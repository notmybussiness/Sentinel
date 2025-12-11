#!/usr/bin/env python3
"""
exp8 Tomcat200 + HikariCP50 결과 분석
"""
import pandas as pd
import numpy as np
from pathlib import Path

def load_csv(filename):
    """CSV 파일 로드"""
    try:
        df = pd.read_csv(filename)
        return df
    except FileNotFoundError:
        print(f"⚠️  {filename} 파일 없음")
        return None

def analyze_status_codes(df):
    """HTTP 상태 코드 분석"""
    if df is None or df.empty:
        return None

    # labels에서 status 추출
    df['status'] = df['labels'].str.extract(r'status=(\d+)')

    # 전체 요청 수 계산
    total_requests = df.groupby('status')['value'].sum()

    return total_requests

def analyze_hikaricp(active_df, pending_df, usage_df):
    """HikariCP 메트릭 분석"""
    results = {}

    if active_df is not None and not active_df.empty:
        results['active_avg'] = active_df['value'].mean()
        results['active_max'] = active_df['value'].max()
        results['active_min'] = active_df['value'].min()

    if pending_df is not None and not pending_df.empty:
        results['pending_avg'] = pending_df['value'].mean()
        results['pending_max'] = pending_df['value'].max()
        results['pending_sum'] = pending_df['value'].sum()

    if usage_df is not None and not usage_df.empty:
        results['usage_avg'] = usage_df['value'].mean()
        results['usage_max'] = usage_df['value'].max()

    return results

def analyze_performance(tps_df, avg_rt_df):
    """성능 메트릭 분석"""
    results = {}

    if tps_df is not None and not tps_df.empty:
        results['tps_avg'] = tps_df['value'].mean()
        results['tps_max'] = tps_df['value'].max()
        results['total_requests'] = tps_df['value'].sum() * 15  # 15초 간격

    if avg_rt_df is not None and not avg_rt_df.empty:
        # NaN이 아닌 값만 필터링
        valid_rt = avg_rt_df[avg_rt_df['value'].notna()]['value']
        if not valid_rt.empty:
            results['avg_rt_mean'] = valid_rt.mean()
            results['avg_rt_max'] = valid_rt.max()
            results['avg_rt_min'] = valid_rt.min()

    return results

def analyze_resources(cpu_df, threads_df, memory_df):
    """리소스 사용량 분석"""
    results = {}

    if cpu_df is not None and not cpu_df.empty:
        results['cpu_avg'] = cpu_df['value'].mean()
        results['cpu_max'] = cpu_df['value'].max()

    if threads_df is not None and not threads_df.empty:
        results['threads_avg'] = threads_df['value'].mean()
        results['threads_max'] = threads_df['value'].max()

    if memory_df is not None and not memory_df.empty:
        # Heap 메모리만 필터
        heap_df = memory_df[memory_df['labels'].str.contains('area=heap', na=False)]
        if not heap_df.empty:
            results['heap_avg_mb'] = heap_df['value'].mean() / 1024 / 1024
            results['heap_max_mb'] = heap_df['value'].max() / 1024 / 1024

    return results

def main():
    base_dir = Path('.')

    print("=" * 80)
    print("exp8: Tomcat 200 Platform Threads + HikariCP 50 분석")
    print("=" * 80)
    print()

    # CSV 로드
    status_df = load_csv(base_dir / 'status_code_distribution.csv')
    tps_df = load_csv(base_dir / 'tps.csv')
    avg_rt_df = load_csv(base_dir / 'avg_response_time.csv')
    cpu_df = load_csv(base_dir / 'cpu_usage.csv')
    threads_df = load_csv(base_dir / 'active_threads.csv')
    memory_df = load_csv(base_dir / 'jvm_memory.csv')
    hikari_active_df = load_csv(base_dir / 'hikaricp_active.csv')
    hikari_pending_df = load_csv(base_dir / 'hikaricp_pending.csv')
    hikari_usage_df = load_csv(base_dir / 'hikaricp_usage_percent.csv')

    # 1. HTTP 상태 코드 분석
    print("📊 1. HTTP Status Code Distribution")
    print("-" * 80)
    if status_df is not None:
        status_totals = analyze_status_codes(status_df)
        if status_totals is not None:
            total = status_totals.sum()
            for status, count in status_totals.sort_index().items():
                pct = (count / total * 100) if total > 0 else 0
                emoji = "❌" if status == '403' else ("⚠️" if status.startswith('5') else "✅")
                print(f"  {emoji} {status}: {count:,.0f} ({pct:.1f}%)")

            # Success rate 계산
            success = status_totals.get('200', 0)
            success_rate = (success / total * 100) if total > 0 else 0
            print(f"\n  Success Rate: {success_rate:.2f}%")
    print()

    # 2. HikariCP 분석
    print("🔗 2. HikariCP Connection Pool")
    print("-" * 80)
    hikari_stats = analyze_hikaricp(hikari_active_df, hikari_pending_df, hikari_usage_df)
    if hikari_stats:
        print(f"  Active Connections:")
        print(f"    - Average: {hikari_stats.get('active_avg', 0):.1f}")
        print(f"    - Max:     {hikari_stats.get('active_max', 0):.0f} / 50")
        print(f"    - Min:     {hikari_stats.get('active_min', 0):.0f}")
        print()
        print(f"  Pending Requests:")
        print(f"    - Average: {hikari_stats.get('pending_avg', 0):.1f}")
        print(f"    - Max:     {hikari_stats.get('pending_max', 0):.0f}")
        print()
        print(f"  Pool Usage:")
        print(f"    - Average: {hikari_stats.get('usage_avg', 0):.1f}%")
        print(f"    - Max:     {hikari_stats.get('usage_max', 0):.1f}%")

        # 병목 판단
        if hikari_stats.get('usage_max', 0) > 90:
            print("\n  ⚠️  Pool 사용률 > 90% → Pool Size 증가 필요")
        elif hikari_stats.get('pending_max', 0) > 10:
            print("\n  ⚠️  Pending > 10 → Pool 병목 발생")
        else:
            print("\n  ✅ Pool 상태 양호")
    print()

    # 3. 성능 메트릭
    print("⚡ 3. Performance Metrics")
    print("-" * 80)
    perf_stats = analyze_performance(tps_df, avg_rt_df)
    if perf_stats:
        print(f"  Throughput:")
        print(f"    - Average TPS: {perf_stats.get('tps_avg', 0):.1f} req/s")
        print(f"    - Max TPS:     {perf_stats.get('tps_max', 0):.1f} req/s")
        print()
        print(f"  Response Time:")
        print(f"    - Average: {perf_stats.get('avg_rt_mean', 0):.1f} ms")
        print(f"    - Max:     {perf_stats.get('avg_rt_max', 0):.1f} ms")
        print(f"    - Min:     {perf_stats.get('avg_rt_min', 0):.1f} ms")
    print()

    # 4. 리소스 사용량
    print("💻 4. Resource Usage")
    print("-" * 80)
    resource_stats = analyze_resources(cpu_df, threads_df, memory_df)
    if resource_stats:
        print(f"  CPU:")
        print(f"    - Average: {resource_stats.get('cpu_avg', 0):.1f}%")
        print(f"    - Max:     {resource_stats.get('cpu_max', 0):.1f}%")
        print()
        print(f"  Threads:")
        print(f"    - Average: {resource_stats.get('threads_avg', 0):.0f}")
        print(f"    - Max:     {resource_stats.get('threads_max', 0):.0f}")
        print()
        print(f"  JVM Heap Memory:")
        print(f"    - Average: {resource_stats.get('heap_avg_mb', 0):.1f} MB")
        print(f"    - Max:     {resource_stats.get('heap_max_mb', 0):.1f} MB")
    print()

    # 5. 종합 평가
    print("=" * 80)
    print("📋 Summary & Recommendations")
    print("=" * 80)

    # 403 에러 확인
    if status_df is not None:
        status_totals = analyze_status_codes(status_df)
        if status_totals is not None and '403' in status_totals.index:
            error_403 = status_totals.get('403', 0)
            total = status_totals.sum()
            error_403_pct = (error_403 / total * 100) if total > 0 else 0

            if error_403_pct > 50:
                print("❌ CRITICAL: 403 Forbidden 에러가 50% 이상")
                print("   → JWT 토큰 문제 또는 인증 설정 확인 필요")
                print()

    # Pool 병목 확인
    if hikari_stats:
        if hikari_stats.get('pending_max', 0) > 0:
            print("⚠️  WARNING: Connection Pool Pending 발생")
            print(f"   → Pending Max: {hikari_stats.get('pending_max', 0):.0f}")
            print("   → HikariCP Pool Size 증가 고려")
        else:
            print("✅ Connection Pool 병목 없음")
        print()

    print("=" * 80)

if __name__ == "__main__":
    main()
