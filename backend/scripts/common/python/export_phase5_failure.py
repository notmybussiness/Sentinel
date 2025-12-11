#!/usr/bin/env python3
"""
Phase 5 DB Failure 메트릭 추출 (2025-11-26 13:56:00 ~ 14:00:00 KST)

사용법:
    python export_phase5_failure.py
    python export_phase5_failure.py --output ./phase5_failure_metrics
"""

import argparse
import requests
import csv
import json
from datetime import datetime, timedelta, timezone
import pandas as pd
import os

PROMETHEUS_URL = "http://192.168.0.5:9090"

# Phase 5 장애 시점
FAILURE_START = "2025-11-26T13:56:00+09:00"
FAILURE_END = "2025-11-26T14:00:00+09:00"

# 장애 진단용 핵심 메트릭 (우선순위 순)
CRITICAL_METRICS = {
    # ========================================
    # Priority 1: HikariCP Connection Pool
    # ========================================
    "hikaricp_active": {
        "query": 'hikaricp_connections_active{pool="SentinelHikariPool"}',
        "description": "활성 DB 연결 수 (Pool Exhaustion 확인)",
        "critical_threshold": {"min": 45, "description": "50개 중 45개 이상이면 Pool 고갈 위험"}
    },
    "hikaricp_idle": {
        "query": 'hikaricp_connections_idle{pool="SentinelHikariPool"}',
        "description": "대기 중인 DB 연결 수",
        "critical_threshold": {"max": 5, "description": "5개 이하면 Pool 부족"}
    },
    "hikaricp_pending": {
        "query": 'hikaricp_connections_pending{pool="SentinelHikariPool"}',
        "description": "대기 중인 Connection 요청 수",
        "critical_threshold": {"min": 10, "description": "10개 이상이면 심각한 병목"}
    },
    "hikaricp_timeout": {
        "query": 'rate(hikaricp_connections_timeout_total{pool="SentinelHikariPool"}[1m])',
        "description": "Connection Timeout 발생률 (per second)",
        "critical_threshold": {"min": 1, "description": "1/sec 이상이면 장애 수준"}
    },
    "hikaricp_usage_percent": {
        "query": 'hikaricp_connections_active{pool="SentinelHikariPool"} / hikaricp_connections_max{pool="SentinelHikariPool"} * 100',
        "description": "Connection Pool 사용률 (%)",
        "critical_threshold": {"min": 90, "description": "90% 이상이면 위험"}
    },
    "hikaricp_acquire_time_p95": {
        "query": 'histogram_quantile(0.95, sum by (le) (rate(hikaricp_connections_acquire_seconds_bucket{pool="SentinelHikariPool"}[1m]))) * 1000',
        "description": "Connection 획득 시간 P95 (ms)",
        "critical_threshold": {"min": 500, "description": "500ms 이상이면 성능 저하"}
    },

    # ========================================
    # Priority 2: HTTP Performance
    # ========================================
    "http_p95_latency_portfolio_get": {
        "query": 'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{uri="/api/v1/portfolios/{id}", method="GET"}[1m]))) * 1000',
        "description": "Portfolio GET API P95 응답 시간 (ms)",
        "critical_threshold": {"min": 500, "description": "Baseline 150ms → 500ms 이상이면 장애"}
    },
    "http_error_rate_5xx": {
        "query": 'rate(http_server_requests_seconds_count{status=~"5..*"}[1m])',
        "description": "HTTP 5xx 에러 발생률 (per second)",
        "critical_threshold": {"min": 5, "description": "5/sec 이상이면 심각"}
    },
    "http_success_rate": {
        "query": 'rate(http_server_requests_seconds_count{status="200"}[1m])',
        "description": "HTTP 200 성공률 (per second)",
        "critical_threshold": {"max": 50, "description": "급락하면 서비스 불가"}
    },

    # ========================================
    # Priority 3: Virtual Threads
    # ========================================
    "jvm_threads_live": {
        "query": 'jvm_threads_live_threads',
        "description": "활성 Thread 수 (Virtual Thread 폭증 확인)",
        "critical_threshold": {"min": 500, "description": "500개 이상이면 Virtual Thread 폭증"}
    },
    "jvm_threads_peak": {
        "query": 'jvm_threads_peak_threads',
        "description": "최대 Thread 수",
        "critical_threshold": {"min": 600, "description": "600개 이상이면 비정상"}
    },
    "jvm_threads_waiting": {
        "query": 'jvm_threads_states_threads{state="waiting"}',
        "description": "대기 중인 Thread 수",
        "critical_threshold": {"min": 300, "description": "300개 이상이면 Thread Starvation"}
    },

    # ========================================
    # Priority 4: JVM Memory & GC
    # ========================================
    "jvm_memory_used_heap": {
        "query": 'jvm_memory_used_bytes{area="heap"}',
        "description": "JVM Heap 메모리 사용량 (bytes)",
        "critical_threshold": {"min": 2_000_000_000, "description": "2GB 이상이면 OOM 위험"}
    },
    "jvm_memory_usage_percent": {
        "query": 'jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100',
        "description": "Heap 메모리 사용률 (%)",
        "critical_threshold": {"min": 85, "description": "85% 이상이면 위험"}
    },
    "jvm_gc_pause_rate": {
        "query": 'rate(jvm_gc_pause_seconds_count[1m])',
        "description": "GC 발생 빈도 (per second)",
        "critical_threshold": {"min": 5, "description": "5/sec 이상이면 GC 압박"}
    },

    # ========================================
    # Priority 5: System Resources
    # ========================================
    "system_cpu_usage": {
        "query": 'system_cpu_usage * 100',
        "description": "시스템 전체 CPU 사용률 (%)",
        "critical_threshold": {"min": 80, "description": "80% 이상이면 리소스 부족"}
    },
    "process_cpu_usage": {
        "query": 'process_cpu_usage * 100',
        "description": "프로세스 CPU 사용률 (%)",
        "critical_threshold": {"min": 200, "description": "200% 이상이면 비정상 (멀티코어)"}
    },

    # ========================================
    # Priority 6: Database Queries
    # ========================================
    "db_query_rate": {
        "query": 'rate(spring_data_repository_invocations_seconds_count[1m])',
        "description": "DB 쿼리 실행률 (per second)",
        "critical_threshold": {"max": 50, "description": "급락하면 Connection Pool 고갈"}
    },
}


def parse_kst_to_utc(kst_str):
    """KST 시간을 UTC로 변환"""
    kst = datetime.fromisoformat(kst_str)
    utc = kst.astimezone(timezone.utc)
    return utc


def query_prometheus_range(query, start, end, step='5s'):
    """Prometheus Range Query 실행"""
    url = f"{PROMETHEUS_URL}/api/v1/query_range"

    params = {
        'query': query,
        'start': start.timestamp(),
        'end': end.timestamp(),
        'step': step
    }

    try:
        response = requests.get(url, params=params, timeout=30)
        response.raise_for_status()

        data = response.json()

        if data['status'] != 'success':
            print(f"    ⚠️  쿼리 실패: {data.get('error', 'Unknown error')}")
            return None

        return data['data']['result']

    except requests.exceptions.RequestException as e:
        print(f"    ❌ HTTP 에러: {e}")
        return None


def analyze_metric(metric_name, results, config):
    """메트릭 분석 및 이상 여부 판단"""
    if not results:
        return {"status": "NO_DATA", "message": "데이터 없음"}

    # 모든 값 추출
    all_values = []
    for result in results:
        values = [float(v[1]) for v in result['values']]
        all_values.extend(values)

    if not all_values:
        return {"status": "NO_DATA", "message": "빈 데이터"}

    # 통계 계산
    avg_value = sum(all_values) / len(all_values)
    max_value = max(all_values)
    min_value = min(all_values)

    # Critical Threshold 체크
    threshold = config.get("critical_threshold", {})
    status = "NORMAL"
    message = "정상"

    if "min" in threshold and max_value >= threshold["min"]:
        status = "CRITICAL"
        message = f"⚠️ {threshold['description']} (Max: {max_value:.2f})"
    elif "max" in threshold and min_value <= threshold["max"]:
        status = "CRITICAL"
        message = f"⚠️ {threshold['description']} (Min: {min_value:.2f})"

    return {
        "status": status,
        "message": message,
        "avg": avg_value,
        "max": max_value,
        "min": min_value,
        "data_points": len(all_values)
    }


def export_to_csv(metric_name, results, output_dir):
    """메트릭을 CSV로 저장"""
    if not results:
        return

    all_data = []

    for result in results:
        metric_labels = result['metric']
        values = result['values']

        # 레이블 문자열 생성
        label_str = ', '.join([f"{k}={v}" for k, v in metric_labels.items() if k != '__name__'])

        for timestamp, value in values:
            all_data.append({
                'timestamp': datetime.fromtimestamp(timestamp).isoformat(),
                'labels': label_str,
                'value': float(value)
            })

    if not all_data:
        return

    df = pd.DataFrame(all_data)
    output_file = f"{output_dir}/{metric_name}.csv"
    df.to_csv(output_file, index=False)


def generate_summary_report(analysis_results, output_dir):
    """장애 분석 요약 리포트 생성"""
    report_path = f"{output_dir}/FAILURE_SUMMARY.md"

    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("# Phase 5 Database Failure Analysis Summary\n\n")
        f.write(f"**분석 시각**: {datetime.now().isoformat()}\n")
        f.write(f"**장애 시각**: {FAILURE_START} ~ {FAILURE_END}\n\n")
        f.write("---\n\n")

        # Critical Issues
        critical_count = sum(1 for r in analysis_results.values() if r['status'] == 'CRITICAL')
        f.write(f"## 🚨 Critical Issues: {critical_count}건\n\n")

        if critical_count > 0:
            f.write("| Metric | Status | Details |\n")
            f.write("|--------|--------|--------|\n")

            for metric_name, result in analysis_results.items():
                if result['status'] == 'CRITICAL':
                    f.write(f"| {metric_name} | ❌ CRITICAL | {result['message']} |\n")

            f.write("\n---\n\n")

        # All Metrics Summary
        f.write("## 📊 All Metrics Summary\n\n")
        f.write("| Metric | Avg | Max | Min | Status |\n")
        f.write("|--------|-----|-----|-----|--------|\n")

        for metric_name, result in analysis_results.items():
            if result['status'] != 'NO_DATA':
                status_icon = "❌" if result['status'] == "CRITICAL" else "✅"
                f.write(f"| {metric_name} | {result['avg']:.2f} | {result['max']:.2f} | {result['min']:.2f} | {status_icon} |\n")

        f.write("\n---\n\n")

        # Root Cause Analysis
        f.write("## 🔍 Root Cause Analysis\n\n")

        hikaricp_issues = sum(1 for k, v in analysis_results.items() if k.startswith('hikaricp') and v['status'] == 'CRITICAL')
        thread_issues = sum(1 for k, v in analysis_results.items() if 'thread' in k and v['status'] == 'CRITICAL')
        memory_issues = sum(1 for k, v in analysis_results.items() if 'memory' in k and v['status'] == 'CRITICAL')

        if hikaricp_issues >= 2:
            f.write("### ✅ Connection Pool Exhaustion (Confirmed)\n")
            f.write("- HikariCP 관련 메트릭에서 2개 이상 이상 징후 발견\n")
            f.write("- **원인**: Virtual Thread (500 VUs) vs. HikariCP Pool (50개) 불균형\n\n")

        if thread_issues >= 2:
            f.write("### ✅ Virtual Thread Overcommit (Confirmed)\n")
            f.write("- Thread 대기 현상 확인\n")
            f.write("- **원인**: Connection 획득 대기로 인한 Thread Starvation\n\n")

        if memory_issues >= 1:
            f.write("### ⚠️ Memory Pressure (Warning)\n")
            f.write("- 메모리 압박 징후 발견\n")
            f.write("- **원인**: 대기 중인 Virtual Thread의 메타데이터 누적\n\n")

    print(f"\n✅ 요약 리포트 생성: {report_path}")


def main():
    parser = argparse.ArgumentParser(
        description='Phase 5 DB Failure 메트릭 추출 (13:56-14:00)'
    )

    parser.add_argument('--output', type=str, default='./phase5_failure_metrics', help='출력 디렉토리')
    parser.add_argument('--step', type=str, default='5s', help='샘플링 간격 (기본: 5s)')

    args = parser.parse_args()

    # 출력 디렉토리 생성
    os.makedirs(args.output, exist_ok=True)

    # 시간 범위 변환
    start_time = parse_kst_to_utc(FAILURE_START)
    end_time = parse_kst_to_utc(FAILURE_END)

    print("=" * 80)
    print("🚨 Phase 5 Database Failure Metrics Export")
    print("=" * 80)
    print(f"🕐 시작 (KST): {FAILURE_START}")
    print(f"🕐 종료 (KST): {FAILURE_END}")
    print(f"🕐 시작 (UTC): {start_time.isoformat()}")
    print(f"🕐 종료 (UTC): {end_time.isoformat()}")
    print(f"⏱️  간격: {args.step}")
    print(f"📊 메트릭: {len(CRITICAL_METRICS)}개")
    print(f"📁 출력: {args.output}")
    print("=" * 80)
    print()

    # 모든 메트릭 추출 및 분석
    analysis_results = {}

    for metric_name, config in CRITICAL_METRICS.items():
        print(f"📊 [{metric_name}]")
        print(f"   {config['description']}")

        results = query_prometheus_range(
            config['query'],
            start_time,
            end_time,
            args.step
        )

        # CSV 저장
        export_to_csv(metric_name, results, args.output)

        # 분석
        analysis = analyze_metric(metric_name, results, config)
        analysis_results[metric_name] = analysis

        # 결과 출력
        if analysis['status'] == 'CRITICAL':
            print(f"   ❌ CRITICAL: {analysis['message']}")
        elif analysis['status'] == 'NO_DATA':
            print(f"   ⚠️  {analysis['message']}")
        else:
            print(f"   ✅ {analysis['message']} (Avg: {analysis['avg']:.2f})")

        print()

    # 요약 리포트 생성
    generate_summary_report(analysis_results, args.output)

    print("=" * 80)
    print("✅ Phase 5 메트릭 추출 완료!")
    print(f"📁 결과 확인: {args.output}/")
    print("=" * 80)


if __name__ == "__main__":
    main()
