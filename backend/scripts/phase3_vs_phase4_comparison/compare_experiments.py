#!/usr/bin/env python3
"""
Phase 3 vs Phase 4 성능 비교 분석 스크립트

비교 대상:
- Phase 3: GET 요청 시 Inline Update (외부 API 호출)
- Phase 4: Scheduler로 분리 (180초 주기)

사용법:
  python compare_experiments.py \
    --phase3 results/phase3_inline_update \
    --phase4 results/phase4_scheduler_180s \
    --output results/comparison_report.md
"""

import argparse
import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path
from datetime import datetime


def load_metric(base_path: Path, filename: str) -> pd.DataFrame:
    """메트릭 CSV 파일 로드"""
    filepath = base_path / filename
    
    if not filepath.exists():
        print(f"⚠️ 파일 없음: {filepath}")
        return None
    
    df = pd.read_csv(filepath)
    
    # Timestamp를 datetime으로 변환
    if 'timestamp' in df.columns:
        df['timestamp'] = pd.to_datetime(df['timestamp'])
    
    return df


def calculate_stats(df: pd.DataFrame, value_col: str) -> dict:
    """통계 계산"""
    if df is None or df.empty:
        return {
            'mean': None,
            'median': None,
            'min': None,
            'max': None,
            'std': None,
            'p95': None,
            'p99': None
        }
    
    return {
        'mean': df[value_col].mean(),
        'median': df[value_col].median(),
        'min': df[value_col].min(),
        'max': df[value_col].max(),
        'std': df[value_col].std(),
        'p95': df[value_col].quantile(0.95),
        'p99': df[value_col].quantile(0.99)
    }


def compare_metric(phase3_df: pd.DataFrame, phase4_df: pd.DataFrame, 
                   metric_name: str, value_col: str, unit: str) -> dict:
    """두 Phase의 메트릭 비교"""
    phase3_stats = calculate_stats(phase3_df, value_col)
    phase4_stats = calculate_stats(phase4_df, value_col)
    
    # 개선율 계산
    improvement = {}
    for key in ['mean', 'median', 'p95', 'p99', 'std']:
        if phase3_stats[key] is not None and phase4_stats[key] is not None:
            if phase3_stats[key] != 0:
                change_pct = ((phase4_stats[key] - phase3_stats[key]) / phase3_stats[key]) * 100
                improvement[key] = change_pct
            else:
                improvement[key] = None
        else:
            improvement[key] = None
    
    return {
        'metric_name': metric_name,
        'unit': unit,
        'phase3': phase3_stats,
        'phase4': phase4_stats,
        'improvement': improvement
    }


def generate_markdown_report(comparisons: dict, output_path: Path):
    """Markdown 비교 리포트 생성"""
    
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write("# Phase 3 vs Phase 4 비교 분석 결과\n\n")
        f.write(f"> **분석 시간**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write("---\n\n")
        
        # 1. 응답시간 비교
        f.write("## 📊 응답시간 비교\n\n")
        
        for metric_key in ['avg_response', 'p95_response']:
            if metric_key not in comparisons:
                continue
            
            comp = comparisons[metric_key]
            f.write(f"### {comp['metric_name']}\n\n")
            
            f.write("| 통계 | Phase 3 (Inline) | Phase 4 (Scheduler) | 변화 |\n")
            f.write("|------|------------------|---------------------|------|\n")
            
            for stat in ['mean', 'median', 'p95', 'p99']:
                p3_val = comp['phase3'][stat]
                p4_val = comp['phase4'][stat]
                change = comp['improvement'][stat]
                
                if p3_val is None or p4_val is None:
                    f.write(f"| {stat.upper()} | N/A | N/A | N/A |\n")
                else:
                    emoji = "✅" if change < 0 else "❌"
                    f.write(f"| **{stat.upper()}** | {p3_val:.2f} {comp['unit']} | "
                           f"{p4_val:.2f} {comp['unit']} | "
                           f"**{change:+.1f}%** {emoji} |\n")
            
            f.write("\n")
        
        # 2. 안정성 비교 (표준편차)
        f.write("## 🎯 안정성 비교 (표준편차)\n\n")
        
        if 'avg_response' in comparisons:
            comp = comparisons['avg_response']
            p3_std = comp['phase3']['std']
            p4_std = comp['phase4']['std']
            
            if p3_std and p4_std:
                std_change = ((p4_std - p3_std) / p3_std) * 100
                emoji = "✅" if std_change < 0 else "❌"
                
                f.write(f"| Phase | 표준편차 |\n")
                f.write(f"|-------|----------|\n")
                f.write(f"| **Phase 3 (Inline)** | {p3_std:.2f} ms |\n")
                f.write(f"| **Phase 4 (Scheduler)** | {p4_std:.2f} ms |\n")
                f.write(f"| **변화** | **{std_change:+.1f}%** {emoji} |\n\n")
                
                if std_change < -30:
                    f.write("> ✅ **Phase 4가 30% 이상 안정적** - Success Criteria 충족!\n\n")
                else:
                    f.write(f"> ⚠️ 안정성 개선은 {abs(std_change):.1f}%로 제한적\n\n")
        
        # 3. 처리량 비교
        f.write("## 🚀 처리량 비교 (TPS)\n\n")
        
        if 'tps' in comparisons:
            comp = comparisons['tps']
            p3_mean = comp['phase3']['mean']
            p4_mean = comp['phase4']['mean']
            
            if p3_mean and p4_mean:
                tps_change = ((p4_mean - p3_mean) / p3_mean) * 100
                emoji = "✅" if tps_change > 0 else "❌"
                
                f.write(f"| Phase | 평균 TPS |\n")
                f.write(f"|-------|----------|\n")
                f.write(f"| **Phase 3 (Inline)** | {p3_mean:.2f} req/s |\n")
                f.write(f"| **Phase 4 (Scheduler)** | {p4_mean:.2f} req/s |\n")
                f.write(f"| **변화** | **{tps_change:+.1f}%** {emoji} |\n\n")
                
                if tps_change > 50:
                    f.write("> ✅ **Phase 4가 1.5배 이상 높은 처리량** - Success Criteria 충족!\n\n")
                else:
                    f.write(f"> ⚠️ 처리량 개선은 {abs(tps_change):.1f}%로 제한적\n\n")
        
        # 4. 시스템 리소스
        f.write("## 💻 시스템 리소스 비교\n\n")
        
        if 'cpu_usage' in comparisons:
            comp = comparisons['cpu_usage']
            f.write("### CPU 사용률\n\n")
            f.write("| Phase | 평균 CPU | P95 CPU |\n")
            f.write("|-------|----------|----------|\n")
            f.write(f"| Phase 3 | {comp['phase3']['mean']:.2f}% | {comp['phase3']['p95']:.2f}% |\n")
            f.write(f"| Phase 4 | {comp['phase4']['mean']:.2f}% | {comp['phase4']['p95']:.2f}% |\n\n")
        
        if 'active_threads' in comparisons:
            comp = comparisons['active_threads']
            f.write("### Active Threads\n\n")
            f.write("| Phase | 평균 | 최대 |\n")
            f.write("|-------|------|------|\n")
            f.write(f"| Phase 3 | {comp['phase3']['mean']:.2f} | {comp['phase3']['max']:.0f} |\n")
            f.write(f"| Phase 4 | {comp['phase4']['mean']:.2f} | {comp['phase4']['max']:.0f} |\n\n")
            
            if comp['phase3']['max'] > 250 or comp['phase4']['max'] > 250:
                f.write("> 🔥 **스레드 고갈 감지!** (Max Threads에 근접)\n\n")
        
        # 5. 최종 결론
        f.write("---\n\n")
        f.write("## 🎯 최종 결론\n\n")
        
        # Success Criteria 체크
        criteria_met = []
        
        # Criterion 1: 응답시간 2배 이상 빠름
        if 'avg_response' in comparisons:
            comp = comparisons['avg_response']
            if comp['improvement']['mean'] and comp['improvement']['mean'] < -50:
                criteria_met.append("✅ 응답시간 2배 이상 개선")
            else:
                criteria_met.append(f"❌ 응답시간 개선 {abs(comp['improvement']['mean']):.1f}% (목표: 50%)")
        
        # Criterion 2: TPS 1.5배 이상 높음
        if 'tps' in comparisons:
            comp = comparisons['tps']
            if comp['improvement']['mean'] and comp['improvement']['mean'] > 50:
                criteria_met.append("✅ TPS 1.5배 이상 증가")
            else:
                criteria_met.append(f"❌ TPS 개선 {abs(comp['improvement']['mean']):.1f}% (목표: 50%)")
        
        # Criterion 3: 안정성 30% 이상 개선
        if 'avg_response' in comparisons:
            comp = comparisons['avg_response']
            if comp['improvement']['std'] and comp['improvement']['std'] < -30:
                criteria_met.append("✅ 안정성 30% 이상 개선")
            else:
                criteria_met.append(f"⚠️ 안정성 개선 {abs(comp['improvement']['std']):.1f}% (목표: 30%)")
        
        f.write("### Success Criteria 달성도\n\n")
        for criterion in criteria_met:
            f.write(f"- {criterion}\n")
        
        f.write("\n")
        
        success_count = sum(1 for c in criteria_met if c.startswith("✅"))
        total_count = len(criteria_met)
        
        if success_count == total_count:
            f.write("> **✅ 모든 Success Criteria 충족! Scheduler 분리 전략은 성공적입니다.**\n\n")
        elif success_count >= total_count / 2:
            f.write(f"> **⚠️ {success_count}/{total_count} 달성. "
                   f"Scheduler 분리는 효과적이나, 추가 최적화 필요.**\n\n")
        else:
            f.write(f"> **❌ {success_count}/{total_count} 달성. "
                   f"근본 원인은 다른 곳에 있을 가능성 높음.**\n\n")
        
        f.write("### 권장 사항\n\n")
        
        if success_count < total_count:
            f.write("**Phase 5 최적화 우선순위**:\n\n")
            f.write("1. ✅ Java 21 Virtual Threads 활성화 (스레드 고갈 해결)\n")
            f.write("2. ✅ HikariCP max-connections 증가 (20 → 50)\n")
            f.write("3. ⏳ DTO 변환 로직 최적화 (Stream → 직접 for문)\n")
            f.write("4. ⏳ Transaction 범위 최소화\n")
        
        f.write("\n---\n\n")
        f.write(f"**Generated**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")


def main():
    parser = argparse.ArgumentParser(description='Phase 3 vs Phase 4 성능 비교')
    parser.add_argument('--phase3', required=True, help='Phase 3 결과 디렉토리')
    parser.add_argument('--phase4', required=True, help='Phase 4 결과 디렉토리')
    parser.add_argument('--output', default='comparison_report.md', help='출력 파일')
    
    args = parser.parse_args()
    
    phase3_dir = Path(args.phase3)
    phase4_dir = Path(args.phase4)
    output_path = Path(args.output)
    
    # Windows 터미널에서 ANSI 색상 지원 활성화
    import os
    if os.name == 'nt':
        os.system('')
    
    # ANSI 색상 코드 (하얀색)
    WHITE = '\033[97m'
    RESET = '\033[0m'
    
    print(f"{WHITE}📊 Phase 3 vs Phase 4 비교 분석{RESET}")
    print(f"{WHITE}   Phase 3: {phase3_dir}{RESET}")
    print(f"{WHITE}   Phase 4: {phase4_dir}{RESET}")
    print()
    
    # 메트릭 로드
    comparisons = {}
    
    # 1. 평균 응답시간
    print(f"{WHITE}⏱️  평균 응답시간 비교...{RESET}")
    phase3_avg = load_metric(phase3_dir, 'avg_response_time.csv')
    phase4_avg = load_metric(phase4_dir, 'avg_response_time.csv')
    
    if phase3_avg is not None and phase4_avg is not None:
        comparisons['avg_response'] = compare_metric(
            phase3_avg, phase4_avg,
            '평균 응답시간', 'value', 'ms'
        )
    
    # 2. P95 응답시간
    print(f"{WHITE}📈 P95 응답시간 비교...{RESET}")
    phase3_p95 = load_metric(phase3_dir, 'p95_response_time.csv')
    phase4_p95 = load_metric(phase4_dir, 'p95_response_time.csv')
    
    if phase3_p95 is not None and phase4_p95 is not None:
        comparisons['p95_response'] = compare_metric(
            phase3_p95, phase4_p95,
            'P95 응답시간', 'value', 'ms'
        )
    
    # 3. TPS
    print(f"{WHITE}🚀 TPS 비교...{RESET}")
    phase3_tps = load_metric(phase3_dir, 'tps.csv')
    phase4_tps = load_metric(phase4_dir, 'tps.csv')
    
    if phase3_tps is not None and phase4_tps is not None:
        comparisons['tps'] = compare_metric(
            phase3_tps, phase4_tps,
            'TPS (Transactions Per Second)', 'value', 'req/s'
        )
    
    # 4. CPU 사용률
    print(f"{WHITE}💻 CPU 사용률 비교...{RESET}")
    phase3_cpu = load_metric(phase3_dir, 'cpu_usage.csv')
    phase4_cpu = load_metric(phase4_dir, 'cpu_usage.csv')
    
    if phase3_cpu is not None and phase4_cpu is not None:
        comparisons['cpu_usage'] = compare_metric(
            phase3_cpu, phase4_cpu,
            'CPU 사용률', 'value', '%'
        )
    
    # 5. Active Threads
    print(f"{WHITE}🧵 Active Threads 비교...{RESET}")
    phase3_threads = load_metric(phase3_dir, 'active_threads.csv')
    phase4_threads = load_metric(phase4_dir, 'active_threads.csv')
    
    if phase3_threads is not None and phase4_threads is not None:
        comparisons['active_threads'] = compare_metric(
            phase3_threads, phase4_threads,
            'Active Threads', 'value', 'threads'
        )
    
    # 리포트 생성
    print()
    print(f"{WHITE}📝 리포트 생성 중: {output_path}{RESET}")
    generate_markdown_report(comparisons, output_path)
    
    print(f"{WHITE}✅ 비교 분석 완료!{RESET}")
    print(f"{WHITE}   결과: {output_path}{RESET}")
    
    # 간단한 요약 출력
    if 'avg_response' in comparisons:
        comp = comparisons['avg_response']
        change = comp['improvement']['mean']
        
        print()
        print(f"{WHITE}📊 핵심 요약:{RESET}")
        print(f"{WHITE}   Phase 3 평균 응답시간: {comp['phase3']['mean']:.2f} ms{RESET}")
        print(f"{WHITE}   Phase 4 평균 응답시간: {comp['phase4']['mean']:.2f} ms{RESET}")
        
        if change:
            if change < -50:
                print(f"{WHITE}   ✅ Scheduler 분리로 {abs(change):.1f}% 개선! (2배 이상){RESET}")
            elif change < 0:
                print(f"{WHITE}   ⚠️ Scheduler 분리로 {abs(change):.1f}% 개선 (제한적){RESET}")
            else:
                print(f"{WHITE}   ❌ Scheduler 분리가 {change:.1f}% 느려짐{RESET}")


if __name__ == '__main__':
    main()
