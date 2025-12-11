#!/usr/bin/env python3
"""
Phase 4: Scheduler Interval 비교 분석
30초 vs 180초(3분) 주기 비교
"""

import pandas as pd
import numpy as np
from pathlib import Path
import sys

# UTF-8 출력
sys.stdout.reconfigure(encoding='utf-8')

def load_portfolio_metrics(base_path, interval_name):
    """Portfolio 관련 메트릭 로드"""
    avg_rt = pd.read_csv(f"{base_path}/avg_response_time.csv")
    p95_rt = pd.read_csv(f"{base_path}/p95_response_time.csv")
    tps_data = pd.read_csv(f"{base_path}/tps.csv")

    # Portfolio 엔드포인트 필터링
    portfolio_avg = avg_rt[avg_rt['labels'].str.contains('/api/v1/portfolios', na=False)]
    portfolio_p95 = p95_rt[p95_rt['labels'].str.contains('/api/v1/portfolios', na=False)]
    portfolio_tps = tps_data[tps_data['labels'].str.contains('/api/v1/portfolios', na=False)]

    # GET /api/v1/portfolios만 추출 (목록 조회)
    list_avg = portfolio_avg[
        (portfolio_avg['labels'].str.contains('method=GET')) &
        (portfolio_avg['labels'].str.contains('uri=/api/v1/portfolios,')) |
        (portfolio_avg['labels'].str.contains('uri=/api/v1/portfolios$'))
    ].copy()

    list_p95 = portfolio_p95[
        portfolio_p95['labels'].str.contains('uri=/api/v1/portfolios,') |
        portfolio_p95['labels'].str.contains('uri=/api/v1/portfolios$')
    ].copy()

    # 값 변환
    list_avg['value'] = pd.to_numeric(list_avg['value'], errors='coerce')
    list_p95['value'] = pd.to_numeric(list_p95['value'], errors='coerce')
    portfolio_tps['value'] = pd.to_numeric(portfolio_tps['value'], errors='coerce')

    # NaN 제거
    list_avg = list_avg.dropna(subset=['value'])
    list_p95 = list_p95.dropna(subset=['value'])
    portfolio_tps = portfolio_tps.dropna(subset=['value'])

    return {
        'name': interval_name,
        'avg_rt': list_avg,
        'p95_rt': list_p95,
        'tps': portfolio_tps
    }

def load_system_metrics(base_path):
    """시스템 메트릭 로드"""
    cpu = pd.read_csv(f"{base_path}/cpu_usage.csv")
    threads = pd.read_csv(f"{base_path}/active_threads.csv")
    hikari_active = pd.read_csv(f"{base_path}/hikaricp_active.csv")
    hikari_pending = pd.read_csv(f"{base_path}/hikaricp_pending.csv")

    # 값 변환
    for df in [cpu, threads, hikari_active, hikari_pending]:
        df['value'] = pd.to_numeric(df['value'], errors='coerce')

    return {
        'cpu': cpu.dropna(subset=['value']),
        'threads': threads.dropna(subset=['value']),
        'hikari_active': hikari_active.dropna(subset=['value']),
        'hikari_pending': hikari_pending.dropna(subset=['value'])
    }

def print_section(title):
    print(f"\n{'='*80}")
    print(f"  {title}")
    print('='*80)

def print_comparison(metric_name, exp30s, exp180s, unit='ms', lower_is_better=True):
    """두 실험 비교 출력"""
    diff = exp180s - exp30s
    diff_pct = (diff / exp30s) * 100 if exp30s > 0 else 0

    if lower_is_better:
        emoji = "✅" if diff < 0 else "❌"
        improvement = "개선" if diff < 0 else "악화"
    else:
        emoji = "✅" if diff > 0 else "❌"
        improvement = "개선" if diff > 0 else "악화"

    print(f"\n{metric_name}:")
    print(f"  30초 주기:  {exp30s:>8.2f} {unit}")
    print(f"  180초 주기: {exp180s:>8.2f} {unit}")
    print(f"  차이:       {diff:>8.2f} {unit} ({diff_pct:+.1f}%) {emoji} {improvement}")

def analyze():
    print_section("Phase 4: Scheduler Interval 비교 분석")
    print("📊 30초 주기 vs 180초(3분) 주기")

    base_dir = Path("scripts/phase4_scheduler_impact/results")

    # 데이터 로드
    print("\n📁 데이터 로딩 중...")
    exp30s = load_portfolio_metrics(base_dir / "scheduler30s", "30초 주기")
    exp180s = load_portfolio_metrics(base_dir / "scheduler180s", "180초 주기")

    sys30s = load_system_metrics(base_dir / "scheduler30s")
    sys180s = load_system_metrics(base_dir / "scheduler180s")

    print(f"  ✅ 30초 실험: {len(exp30s['avg_rt'])} 데이터 포인트")
    print(f"  ✅ 180초 실험: {len(exp180s['avg_rt'])} 데이터 포인트")

    # ==================== Portfolio 응답시간 분석 ====================
    print_section("1️⃣  Portfolio 응답시간 (GET /api/v1/portfolios)")

    if len(exp30s['avg_rt']) > 0 and len(exp180s['avg_rt']) > 0:
        # 평균 응답시간
        avg_30s = exp30s['avg_rt']['value'].mean()
        avg_180s = exp180s['avg_rt']['value'].mean()
        print_comparison("평균 응답시간", avg_30s, avg_180s, 'ms')

        # 중앙값
        median_30s = exp30s['avg_rt']['value'].median()
        median_180s = exp180s['avg_rt']['value'].median()
        print_comparison("중앙값", median_30s, median_180s, 'ms')

        # 최소/최대
        min_30s = exp30s['avg_rt']['value'].min()
        min_180s = exp180s['avg_rt']['value'].min()
        print_comparison("최소값", min_30s, min_180s, 'ms')

        max_30s = exp30s['avg_rt']['value'].max()
        max_180s = exp180s['avg_rt']['value'].max()
        print_comparison("최대값", max_30s, max_180s, 'ms')

        # 표준편차 (안정성 지표)
        std_30s = exp30s['avg_rt']['value'].std()
        std_180s = exp180s['avg_rt']['value'].std()
        print(f"\n안정성 (표준편차):")
        print(f"  30초 주기:  {std_30s:>8.2f} ms")
        print(f"  180초 주기: {std_180s:>8.2f} ms")
        emoji = "✅" if std_180s < std_30s else "❌"
        print(f"  → 180초가 {'더 안정적' if std_180s < std_30s else '덜 안정적'} {emoji}")
    else:
        print("⚠️  Portfolio 데이터가 없습니다!")

    # ==================== P95 응답시간 ====================
    print_section("2️⃣  P95 응답시간")

    if len(exp30s['p95_rt']) > 0 and len(exp180s['p95_rt']) > 0:
        p95_30s = exp30s['p95_rt']['value'].mean()
        p95_180s = exp180s['p95_rt']['value'].mean()
        print_comparison("P95 평균", p95_30s, p95_180s, 'ms')

        p95_max_30s = exp30s['p95_rt']['value'].max()
        p95_max_180s = exp180s['p95_rt']['value'].max()
        print_comparison("P95 최대값", p95_max_30s, p95_max_180s, 'ms')

        # 목표 달성 여부
        print(f"\n🎯 목표 달성 여부 (P95 < 150ms):")
        print(f"  30초 주기:  {p95_30s:.2f}ms → {'✅ 달성' if p95_30s < 150 else '❌ 미달성'}")
        print(f"  180초 주기: {p95_180s:.2f}ms → {'✅ 달성' if p95_180s < 150 else '❌ 미달성'}")

    # ==================== TPS ====================
    print_section("3️⃣  처리량 (TPS)")

    if len(exp30s['tps']) > 0 and len(exp180s['tps']) > 0:
        tps_30s = exp30s['tps']['value'].mean()
        tps_180s = exp180s['tps']['value'].mean()
        print_comparison("평균 TPS", tps_30s, tps_180s, 'req/s', lower_is_better=False)

        tps_max_30s = exp30s['tps']['value'].max()
        tps_max_180s = exp180s['tps']['value'].max()
        print_comparison("최대 TPS", tps_max_30s, tps_max_180s, 'req/s', lower_is_better=False)

    # ==================== 시스템 메트릭 ====================
    print_section("4️⃣  시스템 리소스")

    # CPU
    cpu_30s = sys30s['cpu']['value'].mean()
    cpu_180s = sys180s['cpu']['value'].mean()
    print_comparison("평균 CPU 사용률", cpu_30s, cpu_180s, '%')

    # Active Threads
    threads_30s = sys30s['threads']['value'].mean()
    threads_180s = sys180s['threads']['value'].mean()
    print_comparison("평균 활성 스레드", threads_30s, threads_180s, '개')

    # HikariCP Active Connections
    hikari_30s = sys30s['hikari_active']['value'].mean()
    hikari_180s = sys180s['hikari_active']['value'].mean()
    print_comparison("평균 DB 활성 연결", hikari_30s, hikari_180s, '개')

    # HikariCP Pending
    pending_30s = sys30s['hikari_pending']['value'].mean()
    pending_180s = sys180s['hikari_pending']['value'].mean()
    print_comparison("평균 DB 대기 연결", pending_30s, pending_180s, '개')

    # ==================== Scheduler 영향 분석 ====================
    print_section("5️⃣  Scheduler 영향 분석")

    # 30초 주기: 8분 실험 중 약 16번 실행
    # 180초 주기: 17분 실험 중 약 5-6번 실행
    print("\n실험 중 Scheduler 실행 횟수:")
    print("  30초 주기:  약 16번 (매 30초마다)")
    print("  180초 주기: 약 5-6번 (매 3분마다)")

    # 응답시간 변동성으로 Scheduler 충돌 추정
    if len(exp30s['avg_rt']) > 0 and len(exp180s['avg_rt']) > 0:
        cv_30s = (exp30s['avg_rt']['value'].std() / exp30s['avg_rt']['value'].mean()) * 100
        cv_180s = (exp180s['avg_rt']['value'].std() / exp180s['avg_rt']['value'].mean()) * 100

        print(f"\n응답시간 변동계수 (CV):")
        print(f"  30초 주기:  {cv_30s:.2f}%")
        print(f"  180초 주기: {cv_180s:.2f}%")
        print(f"  → 180초가 {abs(cv_180s - cv_30s):.2f}%p {'더 안정적' if cv_180s < cv_30s else '덜 안정적'}")

    # ==================== 최종 결론 ====================
    print_section("📊 최종 결론")

    if len(exp30s['avg_rt']) > 0 and len(exp180s['avg_rt']) > 0:
        avg_improvement = ((avg_30s - avg_180s) / avg_30s) * 100
        p95_improvement = ((p95_30s - p95_180s) / p95_30s) * 100 if len(exp30s['p95_rt']) > 0 else 0

        print(f"\n✅ 180초(3분) 주기 채택 시 기대효과:")
        print(f"  • 평균 응답시간: {avg_improvement:+.1f}% 개선")
        if len(exp30s['p95_rt']) > 0:
            print(f"  • P95 응답시간: {p95_improvement:+.1f}% 개선")
        print(f"  • 응답 안정성: {abs(cv_180s - cv_30s):.1f}%p 개선")
        print(f"  • Scheduler 실행 횟수: 약 70% 감소")

        print(f"\n🎯 권장사항:")
        if avg_180s < avg_30s and (len(exp30s['p95_rt']) == 0 or p95_180s < p95_30s):
            print("  ✅ 180초(3분) 주기 채택 권장")
            print("  이유:")
            print("    - 응답시간 개선")
            print("    - 안정성 향상")
            print("    - Scheduler 부하 감소")
        elif avg_180s < 100 and (len(exp180s['p95_rt']) == 0 or p95_180s < 150):
            print("  ✅ 180초(3분) 주기 충분")
            print("  이유:")
            print("    - 목표 응답시간 달성")
            print("    - Read/Write 분리 효과 확인")
        else:
            print("  ⚠️  추가 최적화 필요")
            print("  문제:")
            print(f"    - 평균 응답시간: {avg_180s:.2f}ms (목표: <100ms)")
            if len(exp180s['p95_rt']) > 0:
                print(f"    - P95: {p95_180s:.2f}ms (목표: <150ms)")
    else:
        print("⚠️  데이터 부족으로 결론 도출 불가")

    print("\n" + "="*80)
    print("✅ 분석 완료!")
    print("="*80 + "\n")

if __name__ == "__main__":
    try:
        analyze()
    except Exception as e:
        print(f"❌ 에러 발생: {e}")
        import traceback
        traceback.print_exc()
