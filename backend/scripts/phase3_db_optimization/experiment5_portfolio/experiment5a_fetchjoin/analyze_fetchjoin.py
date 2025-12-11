#!/usr/bin/env python3
"""
Experiment 5a (Fetch Join) vs Baseline 비교 분석
"""

import pandas as pd
import numpy as np
from pathlib import Path

# 경로 설정
BASE_DIR = Path(__file__).parent
BASELINE_FILE = BASE_DIR.parent / "experiment05" / "all_metrics.csv"
FETCHJOIN_DIR = BASE_DIR / "fetchjoin_result"

def load_baseline():
    """Baseline 데이터 로드 및 파싱"""
    df = pd.read_csv(BASELINE_FILE)

    # P95 응답 시간 추출
    p95_cols = [col for col in df.columns if 'p95_response_time' in col]
    p95_values = []

    for col in p95_cols:
        values = df[col].dropna()
        if len(values) > 0:
            p95_values.extend(values.tolist())

    # 평균 응답 시간 추출
    avg_cols = [col for col in df.columns if 'avg_response_time' in col]
    avg_values = []

    for col in avg_cols:
        values = df[col].dropna()
        if len(values) > 0:
            avg_values.extend(values.tolist())

    # Status Code 분포 추출
    status_cols = [col for col in df.columns if 'status_code_distribution' in col]
    status_200 = []
    status_500 = []

    for col in status_cols:
        if '200' in col:
            status_200.extend(df[col].dropna().tolist())
        elif '500' in col:
            status_500.extend(df[col].dropna().tolist())

    return {
        'p95': p95_values,
        'avg': avg_values,
        'status_200': status_200,
        'status_500': status_500
    }

def load_fetchjoin():
    """Fetch Join 데이터 로드"""
    p95_df = pd.read_csv(FETCHJOIN_DIR / "p95_response_time.csv")
    avg_df = pd.read_csv(FETCHJOIN_DIR / "avg_response_time.csv")
    status_df = pd.read_csv(FETCHJOIN_DIR / "status_code_distribution.csv")

    # P95 응답 시간 (전체 API)
    p95_values = p95_df['value'].dropna().tolist()

    # 평균 응답 시간
    avg_values = avg_df['value'].dropna().tolist()

    # Status Code 분포
    status_200 = status_df[status_df['labels'].str.contains('200', na=False)]['value'].tolist()
    status_500 = status_df[status_df['labels'].str.contains('500', na=False)]['value'].tolist()

    return {
        'p95': p95_values,
        'avg': avg_values,
        'status_200': status_200,
        'status_500': status_500
    }

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
    print("=" * 70)
    print("Experiment 5a: Fetch Join vs Baseline 성능 비교")
    print("=" * 70)
    print()

    # 데이터 로드
    print("📊 데이터 로딩 중...")
    baseline = load_baseline()
    fetchjoin = load_fetchjoin()
    print()

    # 1. P95 응답 시간
    print("=" * 70)
    print("1️⃣  P95 응답 시간 (ms)")
    print("=" * 70)

    baseline_p95 = np.mean(baseline['p95']) if baseline['p95'] else 0
    fetchjoin_p95 = np.mean(fetchjoin['p95']) if fetchjoin['p95'] else 0
    improvement_p95 = ((baseline_p95 - fetchjoin_p95) / baseline_p95 * 100) if baseline_p95 > 0 else 0

    print(f"Baseline:   {baseline_p95:8.2f} ms")
    print(f"Fetch Join: {fetchjoin_p95:8.2f} ms")
    print(f"개선율:     {improvement_p95:8.2f}% {'✅' if improvement_p95 > 0 else '❌'}")
    print()

    # 2. 평균 응답 시간
    print("=" * 70)
    print("2️⃣  평균 응답 시간 (ms)")
    print("=" * 70)

    baseline_avg = np.mean(baseline['avg']) if baseline['avg'] else 0
    fetchjoin_avg = np.mean(fetchjoin['avg']) if fetchjoin['avg'] else 0
    improvement_avg = ((baseline_avg - fetchjoin_avg) / baseline_avg * 100) if baseline_avg > 0 else 0

    print(f"Baseline:   {baseline_avg:8.2f} ms")
    print(f"Fetch Join: {fetchjoin_avg:8.2f} ms")
    print(f"개선율:     {improvement_avg:8.2f}% {'✅' if improvement_avg > 0 else '❌'}")
    print()

    # 3. 에러율
    print("=" * 70)
    print("3️⃣  에러율 (%)")
    print("=" * 70)

    baseline_error = calculate_error_rate(baseline['status_200'], baseline['status_500'])
    fetchjoin_error = calculate_error_rate(fetchjoin['status_200'], fetchjoin['status_500'])
    improvement_error = baseline_error - fetchjoin_error

    print(f"Baseline:   {baseline_error:8.2f}%")
    print(f"Fetch Join: {fetchjoin_error:8.2f}%")
    print(f"개선:       {improvement_error:8.2f}% {'✅' if improvement_error > 0 else '❌'}")
    print()

    # 4. 종합 평가
    print("=" * 70)
    print("📋 종합 평가")
    print("=" * 70)

    success_criteria = {
        "P95 < 100ms": fetchjoin_p95 < 100,
        "에러율 < 5%": fetchjoin_error < 5,
        "P95 개선 > 90%": improvement_p95 > 90,
        "에러율 완전 제거": fetchjoin_error == 0
    }

    for criterion, passed in success_criteria.items():
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"{status}  {criterion}")

    print()

    # 5. 상세 통계
    print("=" * 70)
    print("📊 상세 통계")
    print("=" * 70)

    print("\n[P95 응답 시간 분포]")
    if fetchjoin['p95']:
        print(f"  최소값:  {min(fetchjoin['p95']):8.2f} ms")
        print(f"  최대값:  {max(fetchjoin['p95']):8.2f} ms")
        print(f"  중앙값:  {np.median(fetchjoin['p95']):8.2f} ms")
        print(f"  표준편차: {np.std(fetchjoin['p95']):8.2f} ms")

    print("\n[평균 응답 시간 분포]")
    if fetchjoin['avg']:
        print(f"  최소값:  {min(fetchjoin['avg']):8.2f} ms")
        print(f"  최대값:  {max(fetchjoin['avg']):8.2f} ms")
        print(f"  중앙값:  {np.median(fetchjoin['avg']):8.2f} ms")
        print(f"  표준편차: {np.std(fetchjoin['avg']):8.2f} ms")

    print()
    print("=" * 70)
    print("✅ 분석 완료!")
    print("=" * 70)

if __name__ == "__main__":
    main()
