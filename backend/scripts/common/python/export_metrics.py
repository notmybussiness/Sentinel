#!/usr/bin/env python3
"""
Prometheus 메트릭 데이터를 시간 범위 지정해서 CSV로 추출

사용법:
    python export_metrics.py --start "2025-11-22T06:05:00Z" --end "2025-11-22T06:30:00Z"
    python export_metrics.py --last 1h
    python export_metrics.py --last 30m
"""

import argparse
import requests
import csv
import json
from datetime import datetime, timedelta
import pandas as pd

PROMETHEUS_URL = "http://192.168.0.5:9090"

# Grafana 대시보드에 있는 모든 쿼리
METRICS = {
    "tps": {
        "query": 'rate(http_server_requests_seconds_count{job="sentinel-backend"}[1m])',
        "legend": "method, uri, status"
    },
    "avg_response_time": {
        "query": '(rate(http_server_requests_seconds_sum{job="sentinel-backend"}[1m]) * 1000) / rate(http_server_requests_seconds_count{job="sentinel-backend"}[1m])',
        "legend": "method, uri"
    },
    "cpu_usage": {
        "query": 'process_cpu_usage{job="sentinel-backend"} * 100',
        "legend": "cpu_percent"
    },
    "jvm_memory": {
        "query": 'jvm_memory_used_bytes{job="sentinel-backend"}',
        "legend": "area, id"
    },
    "active_threads": {
        "query": 'jvm_threads_live_threads{job="sentinel-backend"}',
        "legend": "threads"
    },
    "p95_response_time": {
        "query": 'histogram_quantile(0.95, sum by (uri, le) (rate(http_server_requests_seconds_bucket{job="sentinel-backend"}[1m]))) * 1000',
        "legend": "uri"
    },
    "hikaricp_active": {
        "query": 'hikaricp_connections_active{pool="SentinelHikariPool"}',
        "legend": "pool"
    },
    "hikaricp_pending": {
        "query": 'hikaricp_connections_pending{pool="SentinelHikariPool"}',
        "legend": "pool"
    },
    "hikaricp_usage_percent": {
        "query": 'hikaricp_connections_active{pool="SentinelHikariPool"} / hikaricp_connections_max{pool="SentinelHikariPool"} * 100',
        "legend": "usage_percent"
    },
    "hikaricp_acquire_time_p95": {
        "query": 'histogram_quantile(0.95, sum by (le) (rate(hikaricp_connections_acquire_seconds_bucket{pool="SentinelHikariPool"}[1m]))) * 1000',
        "legend": "p95_acquire_ms"
    },
    "status_code_distribution": {
        "query": 'sum by (status) (rate(http_server_requests_seconds_count{job="sentinel-backend"}[1m]))',
        "legend": "status"
    }
}


def parse_time(time_str):
    """시간 문자열 파싱 (ISO 8601 또는 상대 시간)"""
    if time_str.endswith('h'):
        hours = int(time_str[:-1])
        return datetime.utcnow() - timedelta(hours=hours)
    elif time_str.endswith('m'):
        minutes = int(time_str[:-1])
        return datetime.utcnow() - timedelta(minutes=minutes)
    else:
        # ISO 8601 형식
        return datetime.fromisoformat(time_str.replace('Z', '+00:00'))


def query_prometheus_range(query, start, end, step='15s'):
    """Prometheus Range Query 실행"""
    url = f"{PROMETHEUS_URL}/api/v1/query_range"

    params = {
        'query': query,
        'start': start.timestamp(),
        'end': end.timestamp(),
        'step': step
    }

    print(f"  쿼리 중: {query[:80]}...")

    try:
        response = requests.get(url, params=params, timeout=30)
        response.raise_for_status()

        data = response.json()

        if data['status'] != 'success':
            print(f"  ⚠️  쿼리 실패: {data.get('error', 'Unknown error')}")
            return []

        return data['data']['result']

    except requests.exceptions.RequestException as e:
        print(f"  ❌ HTTP 에러: {e}")
        return []


def export_to_csv(metric_name, results, output_dir='./metrics_export'):
    """메트릭 데이터를 CSV로 저장"""
    import os

    os.makedirs(output_dir, exist_ok=True)

    if not results:
        print(f"  ⚠️  {metric_name}: 데이터 없음")
        return

    # 모든 시계열 데이터를 하나의 DataFrame으로 변환
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

    df = pd.DataFrame(all_data)

    output_file = f"{output_dir}/{metric_name}.csv"
    df.to_csv(output_file, index=False)

    print(f"  ✅ {metric_name}: {len(all_data)} 데이터 포인트 → {output_file}")


def export_all_to_single_csv(all_metrics, output_file='./metrics_export/all_metrics.csv'):
    """모든 메트릭을 하나의 CSV로 저장 (Wide Format)"""
    import os

    os.makedirs(os.path.dirname(output_file), exist_ok=True)

    # 타임스탬프별로 데이터 정리
    data_by_time = {}

    for metric_name, results in all_metrics.items():
        if not results:
            continue

        for result in results:
            metric_labels = result['metric']
            values = result['values']

            # 레이블 문자열 생성
            label_str = ', '.join([f"{k}={v}" for k, v in metric_labels.items() if k != '__name__'])
            column_name = f"{metric_name}[{label_str}]" if label_str else metric_name

            for timestamp, value in values:
                ts = datetime.fromtimestamp(timestamp).isoformat()

                if ts not in data_by_time:
                    data_by_time[ts] = {'timestamp': ts}

                data_by_time[ts][column_name] = float(value)

    # DataFrame 생성 및 저장
    df = pd.DataFrame(list(data_by_time.values()))
    df = df.sort_values('timestamp')

    df.to_csv(output_file, index=False)

    print(f"\n✅ 통합 CSV 저장: {output_file} ({len(df)} rows × {len(df.columns)} columns)")


def main():
    parser = argparse.ArgumentParser(
        description='Prometheus 메트릭을 CSV로 추출'
    )

    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--last', type=str, help='상대 시간 (예: 1h, 30m)')
    group.add_argument('--start', type=str, help='시작 시간 (ISO 8601)')

    parser.add_argument('--end', type=str, help='종료 시간 (ISO 8601, --start와 함께 사용)')
    parser.add_argument('--step', type=str, default='15s', help='샘플링 간격 (기본: 15s)')
    parser.add_argument('--output', type=str, default='./metrics_export', help='출력 디렉토리')
    parser.add_argument('--single-file', action='store_true', help='하나의 CSV 파일로 통합')

    args = parser.parse_args()

    # 시간 범위 계산
    if args.last:
        end_time = datetime.utcnow()
        start_time = parse_time(args.last)
    else:
        start_time = parse_time(args.start)
        end_time = parse_time(args.end) if args.end else datetime.utcnow()

    print("=" * 60)
    print("Prometheus 메트릭 Export")
    print("=" * 60)
    print(f"🕐 시작: {start_time.isoformat()}")
    print(f"🕐 종료: {end_time.isoformat()}")
    print(f"⏱️  간격: {args.step}")
    print(f"📊 메트릭: {len(METRICS)}개")
    print(f"📁 출력: {args.output}")
    print("=" * 60)
    print()

    # 모든 메트릭 쿼리
    all_metrics = {}

    for metric_name, config in METRICS.items():
        print(f"📊 {metric_name}")
        results = query_prometheus_range(
            config['query'],
            start_time,
            end_time,
            args.step
        )
        all_metrics[metric_name] = results

        if not args.single_file:
            export_to_csv(metric_name, results, args.output)

        print()

    # 통합 CSV 생성
    if args.single_file:
        export_all_to_single_csv(all_metrics, f"{args.output}/all_metrics.csv")

    print("=" * 60)
    print("✅ 완료!")
    print("=" * 60)


if __name__ == "__main__":
    main()
