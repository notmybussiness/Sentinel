# 🚀 Next Phase Plan: Batch API Caching

> **Created**: 2025-11-25  
> **Target**: CryptoService & MarketService batch methods

---

## 📊 Current Status

### ✅ Completed Optimizations
- **Phase 1**: Cache experiments
- **Phase 2**: External API optimization (Service layer cache)
- **Phase 3**: DB optimization (EntityGraph)
- **Phase 4a**: Read/Write separation (PortfolioService Scheduler)

### 🎯 Identified Gaps

#### 1. CryptoService - No dedicated batch method
**Current**: No `getBatchPrices()` method exists
```java
// Controller에서 직접 stream 사용
// 개별 getCryptoPrice() 호출 → 이미 Cache 적용됨
```
**Status**: ✅ **문제 없음** (개별 호출이 캐시 사용)  
**Action**: ❌ **작업 불필요**

#### 2. MarketService - Batch uses individual cached calls
**Current**: `getStockPrices()` streams individual `getStockPrice()`
```java
public List<StockPriceDto> getStockPrices(List<String> symbols) {
    return symbols.stream()
            .map(this::getStockPrice)  // ← 이미 @Cacheable
            .collect(Collectors.toList());
}
```
**Status**: ✅ **이미 최적화됨** (각 호출이 Cache 히트)  
**Action**: ❌ **추가 작업 불필요**

---

## 💡 **새로운 다음 Phase 제안**

Squad 분석 결과, **Batch API는 이미 최적화되어 있음**. 따라서 다른 영역으로 방향 전환:

### **Option A: Virtual Threads (비동기 처리)** 🔥 추천!
```java
// PortfolioService.recalculatePortfolio()
@Async
@EnableVirtualThreads
public CompletableFuture<PortfolioDto> recalculatePortfolioAsync(...) {
    // External API 호출을 비동기로 전환
}
```
**목표**:
- Response Time: Blocking → Non-blocking
- 사용자 경험: 즉시 응답 (202 Accepted)
- 백그라운드: Virtual Thread로 처리

**k6 테스트 시나리오**:
```javascript
// Scenario: 동시에 100명이 recalculate 호출
// Before: 순차적으로 처리 → 마지막 사용자는 오래 대기
// After: 즉시 202 반환 → 백그라운드 처리
```

---

### **Option B: DB Indexing (Chart Queries)**
```sql
CREATE INDEX idx_price_history_symbol_time 
ON price_history(symbol, timestamp);

CREATE INDEX idx_portfolio_holding_portfolio_id 
ON portfolio_holding(portfolio_id);
```
**목표**:
- Chart 조회 성능 개선
- Portfolio N+1 완전 방지 (EntityGraph + Index)

---

### **Option C: Rate Limiting (Resilience4j)**
```java
@RateLimiter(name = "alphaVantage", fallbackMethod = "fallbackPrice")
public StockPriceDto getStockPrice(String symbol) {
    // AlphaVantage API 호출 제한
}
```
**목표**:
- External API 호출 제한
- 안정성 향상 (Circuit Breaker)

---

## 🤔 Squad 추천 우선순위

1. **Option A (Virtual Threads)** ⭐⭐⭐⭐⭐
   - 이유: `recalculatePortfolio`가 Hotspot으로 지적됨
   - 난이도: 중
   - 포트폴리오 가치: 높음

2. **Option B (DB Indexing)** ⭐⭐⭐
   - 이유: Chart 조회 성능 개선 필요
   - 난이도: 쉬움
   - 즉각적 효과: 큼

3. **Option C (Rate Limiting)** ⭐⭐
   - 이유: 안정성 향상
   - 난이도: 중
   - Production 필수

---

## 📝 Next Action

사용자가 선택할 옵션에 따라 진행:
- [ ] Option A: Virtual Threads 구현
- [ ] Option B: DB Indexing 적용
- [ ] Option C: Rate Limiting 구현
- [ ] 또는 다른 아이디어...

---

**Squad Leader 코멘트**: "Batch API는 이미 최적화되어 있습니다! 이제 Async Processing이나 DB Indexing으로 넘어가는 게 좋겠습니다." 🚀
