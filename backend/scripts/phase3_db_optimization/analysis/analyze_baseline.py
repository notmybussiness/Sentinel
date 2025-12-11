#!/usr/bin/env python3
"""
Baseline 실험 결과 분석

사용법:
    python analyze_baseline.py baseline_metrics/all_metrics.csv
"""

import pandas as pd
import sys

def analyze_baseline(csv_file):
    print("=" * 60)
    print("Baseline 실험 결과 분석")
    print("=" * 60)
    print()

    # CSV 로드
    df = pd.read_csv(csv_file)
    df['timestamp'] = pd.to_datetime(df['timestamp'])

    print(f"📊 데이터 범위")
    print(f"   시작: {df['timestamp'].min()}")
    print(f"   종료: {df['timestamp'].max()}")
    print(f"   데이터 포인트: {len(df)}개")
    print(f"   메트릭 수: {len(df.columns) - 1}개")
    print()

    # TPS 분석
    tps_cols = [col for col in df.columns if 'tps[' in col.lower()]
    if tps_cols:
        print("📈 TPS (Requests per Second)")
        for col in tps_cols[:5]:  # 상위 5개만
            if df[col].notna().any():
                avg_tps = df[col].mean()
                max_tps = df[col].max()
                print(f"   {col[:60]:60s}: Avg {avg_tps:6.2f}, Max {max_tps:6.2f}")
        print()

    # 응답 시간 분석
    print("⏱️  평균 응답 시간 (Average Response Time)")
    rt_cols = [col for col in df.columns if 'avg_response_time' in col.lower()]
    for col in rt_cols[:5]:
        if df[col].notna().any():
            avg_rt = df[col].mean()
            max_rt = df[col].max()
            print(f"   {col[:60]:60s}: Avg {avg_rt:7.2f}ms, Max {max_rt:7.2f}ms")
    print()

    # P95 응답 시간 분석
    print("📊 P95 응답 시간 (95th Percentile)")
    p95_cols = [col for col in df.columns if 'p95_response_time' in col.lower()]
    for col in p95_cols:
        if df[col].notna().any():
            avg_p95 = df[col].mean()
            max_p95 = df[col].max()
            print(f"   {col[:60]:60s}: Avg {avg_p95:7.2f}ms, Max {max_p95:7.2f}ms")
    print()

    # CPU & 메모리
    print("💻 시스템 리소스")
    cpu_col = [col for col in df.columns if 'cpu_usage' in col.lower()]
    if cpu_col and df[cpu_col[0]].notna().any():
        print(f"   CPU 사용률: Avg {df[cpu_col[0]].mean():5.1f}%, Max {df[cpu_col[0]].max():5.1f}%")

    thread_col = [col for col in df.columns if 'active_threads' in col.lower()]
    if thread_col and df[thread_col[0]].notna().any():
        print(f"   활성 스레드: Avg {df[thread_col[0]].mean():5.0f}, Max {df[thread_col[0]].max():5.0f}")
    print()

    # HikariCP 분석
    print("🔌 HikariCP Connection Pool")
    hikari_active = [col for col in df.columns if 'hikaricp_active' in col.lower() and 'percent' not in col.lower()]
    if hikari_active and df[hikari_active[0]].notna().any():
        print(f"   Active Connections: Avg {df[hikari_active[0]].mean():5.1f}, Max {df[hikari_active[0]].max():5.0f}")

    hikari_pending = [col for col in df.columns if 'hikaricp_pending' in col.lower()]
    if hikari_pending and df[hikari_pending[0]].notna().any():
        print(f"   Pending Connections: Avg {df[hikari_pending[0]].mean():5.1f}, Max {df[hikari_pending[0]].max():5.0f}")

    hikari_usage = [col for col in df.columns if 'hikaricp_usage_percent' in col.lower()]
    if hikari_usage and df[hikari_usage[0]].notna().any():
        print(f"   Pool Usage: Avg {df[hikari_usage[0]].mean():5.1f}%, Max {df[hikari_usage[0]].max():5.1f}%")

    hikari_acquire = [col for col in df.columns if 'hikaricp_acquire_time' in col.lower()]
    if hikari_acquire and df[hikari_acquire[0]].notna().any():
        print(f"   Acquire Time (P95): Avg {df[hikari_acquire[0]].mean():5.2f}ms, Max {df[hikari_acquire[0]].max():5.2f}ms")
    print()

    # 에러율 분석
    print("🚨 에러율")
    status_cols = [col for col in df.columns if 'status_code' in col.lower()]
    if status_cols:
        total_requests = 0
        error_requests = 0

        for col in status_cols:
            if df[col].notna().any():
                req_count = df[col].sum()
                total_requests += req_count

                if '500' in col or '4xx' in col or '5xx' in col:
                    error_requests += req_count

        if total_requests > 0:
            error_rate = (error_requests / total_requests) * 100
            print(f"   총 요청: {total_requests:,.0f}")
            print(f"   에러: {error_requests:,.0f}")
            print(f"   에러율: {error_rate:.2f}%")
    print()

    print("=" * 60)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("사용법: python analyze_baseline.py baseline_metrics/all_metrics.csv")
        sys.exit(1)

    analyze_baseline(sys.argv[1])
