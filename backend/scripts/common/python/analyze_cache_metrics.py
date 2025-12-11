#!/usr/bin/env python3
"""
Phase 3b Cache Validation - 결과 분석 스크립트

k6 JSON 결과를 분석하여 캐시 효과를 측정합니다.

Usage:
    python analyze_cache_metrics.py results/phase3b_cache_validation/experiment/summary.json
"""

import json
import sys
from pathlib import Path
from typing import Dict, Any


def load_k6_results(json_path: str) -> Dict[str, Any]:
    """k6 JSON 결과 파일 로드"""
    with open(json_path, 'r', encoding='utf-8') as f:
        return json.load(f)


def analyze_cache_metrics(data: Dict[str, Any]) -> Dict[str, Any]:
    """캐시 메트릭 분석"""
    metrics = data.get('metrics', {})

    # Market Cache 분석
    market_hit_rate = metrics.get('market_cache_hit_rate', {})
    market_response = metrics.get('market_response_time', {})

    # Crypto Cache 분석
    crypto_hit_rate = metrics.get('crypto_cache_hit_rate', {})
    crypto_response = metrics.get('crypto_response_time', {})

    # HTTP 전체 메트릭
    http_duration = metrics.get('http_req_duration', {})
    http_failed = metrics.get('http_req_failed', {})

    # API 호출 수
    api_calls = metrics.get('api_calls_total', {})

    analysis = {
        'market': {
            'cache_hit_rate': market_hit_rate.get('rate', 0) * 100,
            'avg_response_time': market_response.get('avg', 0),
            'p95_response_time': market_response.get('p(95)', 0),
            'p99_response_time': market_response.get('p(99)', 0),
        },
        'crypto': {
            'cache_hit_rate': crypto_hit_rate.get('rate', 0) * 100,
            'avg_response_time': crypto_response.get('avg', 0),
            'p95_response_time': crypto_response.get('p(95)', 0),
            'p99_response_time': crypto_response.get('p(99)', 0),
        },
        'overall': {
            'avg_response_time': http_duration.get('avg', 0),
            'p95_response_time': http_duration.get('p(95)', 0),
            'p99_response_time': http_duration.get('p(99)', 0),
            'error_rate': http_failed.get('rate', 0) * 100,
            'total_requests': api_calls.get('count', 0),
        }
    }

    return analysis


def format_report(analysis: Dict[str, Any]) -> str:
    """분석 결과를 마크다운 형식으로 포맷"""
    report = []

    report.append("# Phase 3b: Cache Validation - Analysis Results\n")
    report.append("## Market Cache (1분 TTL)\n")
    report.append(f"- **Cache Hit Rate**: {analysis['market']['cache_hit_rate']:.2f}%")
    report.append(f"- **Avg Response Time**: {analysis['market']['avg_response_time']:.2f}ms")
    report.append(f"- **P95 Response Time**: {analysis['market']['p95_response_time']:.2f}ms")
    report.append(f"- **P99 Response Time**: {analysis['market']['p99_response_time']:.2f}ms")
    report.append("")

    # 판정
    market_hit_rate = analysis['market']['cache_hit_rate']
    if market_hit_rate >= 80:
        report.append(f"✅ **Market Cache Hit Rate**: {market_hit_rate:.1f}% (목표: >80%)")
    else:
        report.append(f"⚠️ **Market Cache Hit Rate**: {market_hit_rate:.1f}% (목표: >80%, 미달)")

    report.append("\n## Crypto Cache (10초 TTL)\n")
    report.append(f"- **Cache Hit Rate**: {analysis['crypto']['cache_hit_rate']:.2f}%")
    report.append(f"- **Avg Response Time**: {analysis['crypto']['avg_response_time']:.2f}ms")
    report.append(f"- **P95 Response Time**: {analysis['crypto']['p95_response_time']:.2f}ms")
    report.append(f"- **P99 Response Time**: {analysis['crypto']['p99_response_time']:.2f}ms")
    report.append("")

    crypto_hit_rate = analysis['crypto']['cache_hit_rate']
    if crypto_hit_rate >= 70:
        report.append(f"✅ **Crypto Cache Hit Rate**: {crypto_hit_rate:.1f}% (목표: >70%)")
    else:
        report.append(f"⚠️ **Crypto Cache Hit Rate**: {crypto_hit_rate:.1f}% (목표: >70%, 미달)")

    report.append("\n## Overall Performance\n")
    report.append(f"- **Total Requests**: {int(analysis['overall']['total_requests']):,}")
    report.append(f"- **Avg Response Time**: {analysis['overall']['avg_response_time']:.2f}ms")
    report.append(f"- **P95 Response Time**: {analysis['overall']['p95_response_time']:.2f}ms")
    report.append(f"- **Error Rate**: {analysis['overall']['error_rate']:.2f}%")
    report.append("")

    # 종합 판정
    report.append("\n## 종합 평가\n")

    success_criteria = [
        market_hit_rate >= 80,
        crypto_hit_rate >= 70,
        analysis['overall']['p95_response_time'] < 100,
        analysis['overall']['error_rate'] < 1.0,
    ]

    if all(success_criteria):
        report.append("### ✅ 모든 성공 기준 달성")
        report.append("\n**결론**: Cache TTL 재조정(Market 1분, Crypto 10초)이 효과적입니다.")
    else:
        report.append("### ⚠️ 일부 기준 미달")
        report.append("\n**개선 필요 사항**:")

        if market_hit_rate < 80:
            report.append(f"- Market Cache Hit Rate: {market_hit_rate:.1f}% (목표: >80%)")
        if crypto_hit_rate < 70:
            report.append(f"- Crypto Cache Hit Rate: {crypto_hit_rate:.1f}% (목표: >70%)")
        if analysis['overall']['p95_response_time'] >= 100:
            report.append(f"- P95 Response Time: {analysis['overall']['p95_response_time']:.1f}ms (목표: <100ms)")
        if analysis['overall']['error_rate'] >= 1.0:
            report.append(f"- Error Rate: {analysis['overall']['error_rate']:.2f}% (목표: <1%)")

    return '\n'.join(report)


def main():
    if len(sys.argv) < 2:
        print("Usage: python analyze_cache_metrics.py <k6_results.json>")
        print("Example: python analyze_cache_metrics.py results/phase3b_cache_validation/experiment/summary.json")
        sys.exit(1)

    json_path = sys.argv[1]

    if not Path(json_path).exists():
        print(f"❌ File not found: {json_path}")
        sys.exit(1)

    print(f"📊 Analyzing: {json_path}")
    print("=" * 60)

    # 결과 로드 및 분석
    data = load_k6_results(json_path)
    analysis = analyze_cache_metrics(data)

    # 리포트 생성
    report = format_report(analysis)
    print(report)

    # 파일로 저장
    output_dir = Path(json_path).parent
    output_file = output_dir / 'analysis_report.md'

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(report)

    print("\n" + "=" * 60)
    print(f"✅ Analysis saved to: {output_file}")


if __name__ == '__main__':
    main()
