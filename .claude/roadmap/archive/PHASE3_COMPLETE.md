# Phase 3: DB N+1 최적화 - 완료 보고서

> **완료일**: 2025-11-25  
> **결과**: Entity Graph 적용, 63.55% 성능 개선

---

## 📊 실험 결과 요약

### 3가지 전략 비교 테스트

| 전략 | P95 (ms) | 평균 (ms) | 에러율 | 순위 |
|------|----------|-----------|--------|------|
| **Baseline** | 2,101 | 1,284 | 27.97% | - |
| **5a: Fetch Join** | 1,515 | 923 | 21.59% | 🥉 #3 |
| **5b: Batch Size** | 1,039 | 574 | 17.64% | 🥈 #2 |
| **5c: Entity Graph** | **766** | **496** | **15.09%** | 🏆 #1 |

### 최종 선택: Entity Graph (JPA)

**성능 개선**:
- ✅ P95: 2,101ms → **766ms** (63.55% 개선)
- ✅ 평균: 1,284ms → **496ms** (61.36% 개선)
- ✅ 에러율: 27.97% → **15.09%** (12.88%p 개선)
- ✅ TPS: 21 → **40 req/s** (1.9배)

**선택 이유**:
- 🥇 3가지 전략 중 최고 성능
- 📉 응답 시간 표준편차 **454ms** (가장 안정적)
- 🎯 JPA 표준 방식 (코드 간결, 유지보수 용이)
- 🔧 코드 수정 최소 (Repository에 `@EntityGraph` 추가만)

---

## 🔥 발견된 진짜 병목

### ⚠️ 목표 미달
- **목표**: P95 < 100ms, 에러율 < 5%
- **실제**: P95 766ms, 에러율 15.09%

### 🔍 원인 분석

**1. CPU 사용률: 11%** (I/O 바운드)
- CPU가 놀고 있음 → 외부 API 대기 중

**2. DB 커넥션 획득 시간: 827ms**
- 외부 API 호출 중 DB 커넥션 점유
- 다른 요청이 커넥션 대기

**3. updatePortfolioPrices() 동기 호출** (주범!)
```java
// PortfolioService.getPortfolio()
Portfolio portfolio = repository.findById(id);
updatePortfolioPrices(portfolio);  // 🔥 여기가 문제!

// Holdings 10개 × API 100ms = 1,000ms
for (Holding holding : portfolio.getHoldings()) {
    cryptoDataService.getPrice(holding);  // 동기
    marketDataService.getPrice(holding);  // 동기
}
```

---

## 🎯 다음 단계: Phase 4

### 목표: updatePortfolioPrices() 최적화

**Option 1: 조회 시 가격 업데이트 제거** (추천)
- 가격 업데이트를 조회 API에서 분리
- Scheduler로 주기적 업데이트 (5분마다)
- 예상 효과: P95 766ms → **< 50ms** (15배)

**Option 2: API 응답 캐싱 강화**
- Phase 2 캐싱 활용
- Cache Hit 시 즉시 반환
- 예상 효과: P95 < 100ms

**Option 3: 병렬 API 호출**
- CompletableFuture.allOf() 활용
- 예상 효과: 1,000ms → 100ms (10배)

**Option 4: 비동기 처리**
- @Async + CompletableFuture
- 즉시 응답 + 백그라운드 업데이트

---

## 📚 관련 문서

- **비교 분석**: `scripts/results/phase3_db_optimization/experiment5_portfolio/compare_all_strategies.py`
- **심층 분석**: `scripts/results/phase3_db_optimization/experiment5_portfolio/deep_analysis.py`
- **코드**: `PortfolioRepository.java:30,44` (EntityGraph 적용 위치)

---

**결론**: DB N+1은 Entity Graph로 해결 완료. 다음 병목은 외부 API 동기 호출.
