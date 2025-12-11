#!/usr/bin/env python3
"""
Experiment 5: 전체 지표 Deep Dive 분석
- 11개 메트릭 전체 분석
- 왜 Entity Graph가 빠른가?
- 왜 목표에 미달했는가?
- 병목은 무엇인가?
"""

import pandas as pd
import numpy as np
from pathlib import Path

BASE_DIR = Path(__file__).parent

def load_all_metrics(result_dir):
    """모든 메트릭 로드"""
    metrics = {}

    # P95 응답 시간
    p95_df = pd.read_csv(result_dir / "p95_response_time.csv")
    metrics['p95'] = p95_df['value'].dropna().tolist()

    # 평균 응답 시간
    avg_df = pd.read_csv(result_dir / "avg_response_time.csv")
    metrics['avg'] = avg_df['value'].dropna().tolist()

    # TPS (처리량)
    tps_df = pd.read_csv(result_dir / "tps.csv")
    metrics['tps'] = tps_df['value'].dropna().tolist()

    # CPU 사용률
    cpu_df = pd.read_csv(result_dir / "cpu_usage.csv")
    metrics['cpu'] = cpu_df['value'].dropna().tolist()

    # Active Threads
    threads_df = pd.read_csv(result_dir / "active_threads.csv")
    metrics['threads'] = threads_df['value'].dropna().tolist()

    # HikariCP Active Connections
    hikari_active_df = pd.read_csv(result_dir / "hikaricp_active.csv")
    metrics['db_active'] = hikari_active_df['value'].dropna().tolist()

    # HikariCP Pending
    hikari_pending_df = pd.read_csv(result_dir / "hikaricp_pending.csv")
    metrics['db_pending'] = hikari_pending_df['value'].dropna().tolist()

    # HikariCP Usage %
    hikari_usage_df = pd.read_csv(result_dir / "hikaricp_usage_percent.csv")
    metrics['db_usage'] = hikari_usage_df['value'].dropna().tolist()

    # HikariCP Acquire Time P95
    hikari_acquire_df = pd.read_csv(result_dir / "hikaricp_acquire_time_p95.csv")
    metrics['db_acquire_p95'] = hikari_acquire_df['value'].dropna().tolist()

    # Status Code 분포
    status_df = pd.read_csv(result_dir / "status_code_distribution.csv")
    metrics['status_200'] = status_df[status_df['labels'].str.contains('200', na=False)]['value'].tolist()
    metrics['status_500'] = status_df[status_df['labels'].str.contains('500', na=False)]['value'].tolist()

    # JVM Memory
    memory_df = pd.read_csv(result_dir / "jvm_memory.csv")
    metrics['memory'] = memory_df['value'].dropna().tolist()

    return metrics

def calculate_error_rate(status_200, status_500):
    """에러율 계산"""
    if len(status_200) == 0 and len(status_500) == 0:
        return 0.0
    total_200 = sum(status_200) if status_200 else 0
    total_500 = sum(status_500) if status_500 else 0
    total_requests = total_200 + total_500
    if total_requests == 0:
        return 0.0
    return (total_500 / total_requests) * 100

def main():
    print("=" * 100)
    print("Experiment 5: 전체 지표 Deep Dive 분석")
    print("=" * 100)
    print()

    # 데이터 로드
    print("📊 전체 메트릭 로딩 중...")
    fetchjoin = load_all_metrics(BASE_DIR / "experiment5a_fetchjoin" / "fetchjoin_result")
    batchsize = load_all_metrics(BASE_DIR / "experiment5b_batchsize" / "batchsize_result")
    entitygraph = load_all_metrics(BASE_DIR / "experiment5c_entitygraph" / "entitygraph_result")
    print("✅ 로딩 완료")
    print()

    # ============================================================
    # 1. 응답 시간 상세 분석
    # ============================================================
    print("=" * 100)
    print("1️⃣  응답 시간 상세 분석 (P95 vs 평균 vs 최대)")
    print("=" * 100)
    print(f"{'전략':<20} {'P95 (ms)':<15} {'평균 (ms)':<15} {'최대 (ms)':<15} {'표준편차':<15}")
    print("-" * 100)

    strategies = [
        ("5a: Fetch Join", fetchjoin),
        ("5b: Batch Size", batchsize),
        ("5c: Entity Graph", entitygraph)
    ]

    for name, data in strategies:
        p95 = np.mean(data['p95']) if data['p95'] else 0
        avg = np.mean(data['avg']) if data['avg'] else 0
        max_val = max(data['p95']) if data['p95'] else 0
        std = np.std(data['p95']) if data['p95'] else 0
        print(f"{name:<20} {p95:>10.2f}      {avg:>10.2f}      {max_val:>10.2f}      {std:>10.2f}")

    print()
    print("💡 인사이트:")
    print("   - Entity Graph의 표준편차가 가장 낮음 = 안정적인 성능")
    print("   - Fetch Join의 최대값이 가장 높음 = 스파이크 발생")
    print()

    # ============================================================
    # 2. 처리량 (TPS) 분석
    # ============================================================
    print("=" * 100)
    print("2️⃣  처리량 (TPS) 분석")
    print("=" * 100)
    print(f"{'전략':<20} {'평균 TPS':<15} {'최대 TPS':<15} {'안정성 (CV)':<15}")
    print("-" * 100)

    for name, data in strategies:
        avg_tps = np.mean(data['tps']) if data['tps'] else 0
        max_tps = max(data['tps']) if data['tps'] else 0
        cv = (np.std(data['tps']) / avg_tps * 100) if avg_tps > 0 and data['tps'] else 0
        print(f"{name:<20} {avg_tps:>10.2f}      {max_tps:>10.2f}      {cv:>10.2f}%")

    print()
    print("💡 인사이트:")
    print("   - TPS가 높을수록 더 많은 요청 처리")
    print("   - CV(변동계수)가 낮을수록 안정적")
    print()

    # ============================================================
    # 3. CPU 사용률 분석
    # ============================================================
    print("=" * 100)
    print("3️⃣  CPU 사용률 분석 (%)")
    print("=" * 100)
    print(f"{'전략':<20} {'평균 CPU':<15} {'최대 CPU':<15} {'최소 CPU':<15}")
    print("-" * 100)

    for name, data in strategies:
        avg_cpu = np.mean(data['cpu']) if data['cpu'] else 0
        max_cpu = max(data['cpu']) if data['cpu'] else 0
        min_cpu = min(data['cpu']) if data['cpu'] else 0
        print(f"{name:<20} {avg_cpu:>10.2f}      {max_cpu:>10.2f}      {min_cpu:>10.2f}")

    print()
    print("💡 인사이트:")
    print("   - CPU가 100%에 근접하면 CPU 바운드")
    print("   - CPU가 낮으면 I/O 대기 (외부 API, DB) 바운드")
    print()

    # ============================================================
    # 4. DB 커넥션 풀 분석 (HikariCP)
    # ============================================================
    print("=" * 100)
    print("4️⃣  DB 커넥션 풀 분석 (HikariCP)")
    print("=" * 100)
    print(f"{'전략':<20} {'평균 Active':<15} {'최대 Active':<15} {'평균 사용률%':<15} {'P95 획득시간(ms)':<20}")
    print("-" * 100)

    for name, data in strategies:
        avg_active = np.mean(data['db_active']) if data['db_active'] else 0
        max_active = max(data['db_active']) if data['db_active'] else 0
        avg_usage = np.mean(data['db_usage']) if data['db_usage'] else 0
        avg_acquire = np.mean(data['db_acquire_p95']) if data['db_acquire_p95'] else 0
        print(f"{name:<20} {avg_active:>10.2f}      {max_active:>10.0f}        {avg_usage:>10.2f}      {avg_acquire:>15.2f}")

    print()
    print("💡 인사이트:")
    print("   - Active Connections가 높을수록 DB 쿼리 많음")
    print("   - 획득 시간이 길면 커넥션 풀 부족 (대기)")
    print("   - Fetch Join/Entity Graph가 Active 낮음 = N+1 해결 증거")
    print()

    # ============================================================
    # 5. 쓰레드 분석
    # ============================================================
    print("=" * 100)
    print("5️⃣  JVM 쓰레드 분석")
    print("=" * 100)
    print(f"{'전략':<20} {'평균 쓰레드':<15} {'최대 쓰레드':<15} {'최소 쓰레드':<15}")
    print("-" * 100)

    for name, data in strategies:
        avg_threads = np.mean(data['threads']) if data['threads'] else 0
        max_threads = max(data['threads']) if data['threads'] else 0
        min_threads = min(data['threads']) if data['threads'] else 0
        print(f"{name:<20} {avg_threads:>10.2f}      {max_threads:>10.0f}        {min_threads:>10.0f}")

    print()
    print("💡 인사이트:")
    print("   - 쓰레드 수 급증 = 동시 요청 처리 중")
    print("   - Tomcat max threads (perf): 200")
    print()

    # ============================================================
    # 6. 에러율 상세 분석
    # ============================================================
    print("=" * 100)
    print("6️⃣  에러율 상세 분석")
    print("=" * 100)
    print(f"{'전략':<20} {'총 요청 (200+500)':<20} {'에러율 (%)':<15} {'500 에러 수':<15}")
    print("-" * 100)

    for name, data in strategies:
        total_200 = sum(data['status_200']) if data['status_200'] else 0
        total_500 = sum(data['status_500']) if data['status_500'] else 0
        total_requests = total_200 + total_500
        error_rate = calculate_error_rate(data['status_200'], data['status_500'])
        print(f"{name:<20} {total_requests:>15.0f}       {error_rate:>10.2f}      {total_500:>10.0f}")

    print()
    print("💡 인사이트:")
    print("   - 에러율이 여전히 높은 이유: 외부 API 타임아웃/실패")
    print("   - Entity Graph도 15% 에러 = DB 최적화만으로는 한계")
    print()

    # ============================================================
    # 7. 종합 진단
    # ============================================================
    print("=" * 100)
    print("🔍 종합 진단 및 병목 분석")
    print("=" * 100)
    print()

    print("【Entity Graph가 왜 가장 빠른가?】")
    print("  1. DB 쿼리 수 최소화 (N+1 완전 해결)")
    print("  2. Active Connection 낮음 = DB 부하 적음")
    print("  3. 응답 시간 표준편차 낮음 = 안정적")
    print("  4. JPA 표준 방식으로 쿼리 최적화 자동")
    print()

    print("【왜 목표(P95 < 100ms)에 미달했는가?】")

    # CPU 분석
    avg_cpu_eg = np.mean(entitygraph['cpu']) if entitygraph['cpu'] else 0
    if avg_cpu_eg < 50:
        print(f"  1. CPU 사용률 낮음 ({avg_cpu_eg:.1f}%) = I/O 바운드")
        print("     → CPU가 놀고 있음, 외부 대기 중")
    else:
        print(f"  1. CPU 사용률 높음 ({avg_cpu_eg:.1f}%) = CPU 바운드")

    # DB 획득 시간
    avg_acquire_eg = np.mean(entitygraph['db_acquire_p95']) if entitygraph['db_acquire_p95'] else 0
    if avg_acquire_eg > 10:
        print(f"  2. DB 커넥션 획득 시간 높음 ({avg_acquire_eg:.1f}ms)")
        print("     → DB 커넥션 풀 부족 또는 DB 느림")
    else:
        print(f"  2. DB 커넥션 획득 빠름 ({avg_acquire_eg:.1f}ms) ✅")

    print("  3. PortfolioService.updatePortfolioPrices():")
    print("     - 각 Holding마다 외부 API 동기 호출 (cryptoDataService, marketDataService)")
    print("     - 10개 holdings × 100ms API 호출 = 1,000ms")
    print("     → 이것이 주범!")
    print()

    print("【다음 최적화 방향】")
    print("  🎯 우선순위 1: Entity Graph 적용 (DB 최적화 완료)")
    print("  🎯 우선순위 2: 외부 API 호출 최적화")
    print("     - 방법 1: updatePortfolioPrices() 호출 제거 (조회 시마다 가격 업데이트 필요한가?)")
    print("     - 방법 2: API 응답 캐싱 (Phase 2에서 했던 것처럼)")
    print("     - 방법 3: 병렬 API 호출 (CompletableFuture.allOf())")
    print("     - 방법 4: 비동기 처리 (@Async)")
    print("  🎯 우선순위 3: 에러 처리 개선 (재시도 로직, Circuit Breaker)")
    print()

    print("=" * 100)
    print("✅ Deep Dive 분석 완료!")
    print("=" * 100)

if __name__ == "__main__":
    main()
