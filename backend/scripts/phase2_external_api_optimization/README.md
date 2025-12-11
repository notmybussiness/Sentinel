# Phase 2: External API Optimization

> **Problem → Solution 기록**
>
> **기간**: 2025-11-20 ~ 2025-11-21
> **목표**: Cache Layer 위치 최적화를 통한 극단적 성능 개선

---

## 🔴 Problem: Cache Hit이어도 느린 응답

### Phase 1의 성과와 한계

Phase 1에서 **Cache TTL 최적화**로 500 에러를 완전히 제거했습니다:
- ✅ 500 에러: 827건 → **0건**
- ✅ Cache Hit Rate: **~85%**
- ✅ External API Calls: **-85% 감소**

하지만 **새로운 문제** 발견:

### 발견된 병목

```
Cache Hit이어도 응답 시간이 느리다!
→ Cache가 Provider Level에 위치
→ Service logic 실행 → Provider 선택 → Cache 체크
→ Cache Hit이어도 10-20ms overhead 발생
```

**예상 흐름**:
```java
// 현재 (Provider Cache)
MarketDataService.getStockPrice(symbol)
  → providerFactory.getProvider()      // 5ms
  → provider.getMarketData(symbol)      // 5ms
    → @Cacheable check                  // Cache Hit! 0ms
    → return cached data
  → mapping & return                    // 5ms

총 소요 시간: ~15ms (Cache Hit이어도!)
```

### 목표

- 🎯 Cache Hit 시 **< 1ms** 응답 (Early Return)
- 🎯 평균 응답 시간 **< 50ms** (현재 ~200ms)
- 🎯 P95 **< 100ms** (현재 ~500ms)
- 🎯 500 에러 **0건 유지**

---

## 🔬 Solution 1: Connection Pool 추가 (Experiment 04b)

### 가설
```
"Connection Pool을 추가하면 성능이 개선될 것이다"
```

### 구현

**Before (Experiment 04 Baseline)**:
```java
// Simple HTTP Client (No Connection Pool)
WebClient.create(baseUrl)
  .get()
  .retrieve()
```

**After (Experiment 04b)**:
```java
// Apache HttpClient 5 with Connection Pool
HttpAsyncClient client = HttpAsyncClients.custom()
  .setMaxConnTotal(200)
  .setMaxConnPerRoute(50)
  .build();
```

**설정**:
- maxConnTotal: 200
- maxConnPerRoute: 50
- Provider Cache 유지 (TTL 3분)

### 결과

| Metric | Baseline (04) | With Pool (04b) | Change |
|--------|---------------|-----------------|--------|
| 500 Errors | 1,244 | **3,381** | **+171%** ❌ |
| Avg Response | 1,949ms | ~2,200ms | +13% ❌ |

### 분석

**오히려 더 나빠졌다!**

원인:
1. **maxConnPerRoute 부족** (50개)
   - 200 VUsers → 단일 Lambda URL로 집중
   - 50개로는 부족 → Connection Wait 발생

2. **Provider Cache Lock Contention**
   - `sync=true` + 3분 TTL
   - 많은 스레드가 Cache Lock에서 대기

### 교훈
```
❌ Connection Pool만으로는 불충분
⚠️ maxConnPerRoute 설정이 중요
⚠️ Cache Lock이 새로운 병목
```

---

## 🔬 Solution 2: Connection Pool 증가 (Experiment 04c)

### 가설
```
"Connection Pool을 더 늘리면 해결될 것이다"
```

### 구현

**변경**:
```java
HttpAsyncClient client = HttpAsyncClients.custom()
  .setMaxConnTotal(500)      // 200 → 500
  .setMaxConnPerRoute(200)   // 50 → 200
  .build();
```

**중요**: Backend WAS **재시작** (Cache 초기화)

### 결과

| Metric | 04b (Pool 200/50) | 04c (Pool 500/200) | Change |
|--------|-------------------|-------------------|--------|
| 500 Errors | 3,381 | ~500 | -85% ✅ |
| Avg Response | ~2,200ms | ~2,100ms | -5% |
| Active Threads | N/A | **248/250** | **99% 포화** ❌ |

### 분석

**부분적 개선, 하지만 새로운 문제**

✅ 좋은 점:
- 에러 크게 감소 (3,381 → 500)
- Connection 부족 문제 해결

❌ 나쁜 점:
- **Thread Pool 포화** (248/250)
- 응답 시간 개선 미미
- 여전히 500 에러 발생

**근본 원인**:
```
Provider Cache (sync=true, TTL 3분)
→ 대규모 Cache Miss 동시 발생
→ Lock Contention
→ 모든 스레드가 Lock 대기
→ Thread Pool 고갈
```

### 교훈
```
⚠️ Connection Pool만 늘려도 근본 해결 안 됨
⚠️ Lock Contention이 진짜 병목
✅ Thread Pool 모니터링이 중요
```

---

## ✅ Solution 3: Service Layer Cache (Experiment 04d)

### 핵심 통찰

```
문제: Provider Cache는 너무 늦게 체크된다
해결: Cache를 Service Layer로 이동 → Early Return!
```

### 가설
```
"Cache를 Service Layer로 옮기면
 Service logic을 완전히 스킵할 수 있다"
```

### 구현

**Before (Provider Cache)**:
```java
// MarketDataService
public StockPriceDto getStockPrice(String symbol) {
    // Service logic (5ms)
    MarketDataProvider provider = factory.getProvider();  // (5ms)
    return provider.getMarketData(symbol);  // → Cache here (5ms)
}

// Provider
@Cacheable(value = "stockPrice", ...)  // ← Cache at Provider Level
public StockPriceDto getMarketData(String symbol) { ... }
```

**After (Service Cache)**:
```java
// MarketDataService
@Cacheable(value = "stockPrice", key = "#symbol", sync = true)  // ← Cache at Service Level!
public StockPriceDto getStockPrice(String symbol) {
    // Cache Hit → Early Return (< 1ms)
    // Cache Miss → Execute below
    MarketDataProvider provider = factory.getProvider();
    return provider.getMarketData(symbol);
}

// Provider (Cache removed)
public StockPriceDto getMarketData(String symbol) { ... }
```

**추가 변경**:
```java
// Cache TTL 변경
buildCache("stockPrice", 10, TimeUnit.SECONDS, 1_000)  // 3분 → 10초!
```

**이유**:
- 짧은 TTL로 **Cache Miss 시간적 분산**
- Lock Contention **최소화**
- 금융 데이터에서 10초는 충분히 fresh

### 결과

| Metric | 04c (Provider Cache) | 04d (Service Cache) | Improvement |
|--------|----------------------|---------------------|-------------|
| Avg Response | 2,100ms | **10ms** | **210배** 🚀 |
| P95 | ~4,000ms | **7ms** | **525배** 🚀 |
| TPS | 140 req/s | **480 req/s** | **3.4배** 🚀 |
| 500 Errors | ~500 | **0** | **완벽** ✅ |
| Active Threads | 248/250 (99%) | 123/250 (50%) | **여유** ✅ |

### 분석

**극단적 성공!**

✅ 성과:
- 평균 응답 **210배 개선** (2,100ms → 10ms)
- P95 **525배 개선** (4,000ms → 7ms)
- TPS **3.4배 증가** (140 → 480 req/s)
- 500 에러 **완전 제거**
- Thread Pool **50% 여유**

**왜 이렇게 극적인 개선이?**

1. **Early Return Effect**
   ```
   Cache Hit (85% of requests):
   Provider Cache: ~15ms (Service + Provider overhead)
   Service Cache: < 1ms (Immediate return!)

   개선: 15배!
   ```

2. **Lock Contention 최소화**
   ```
   3분 TTL: 모든 스레드가 동시에 Cache Miss
   10초 TTL: Cache Miss가 시간적으로 분산

   Lock Wait Time: 100ms → < 1ms
   ```

3. **Thread Pool 여유 확보**
   ```
   빠른 처리 → 스레드 빠르게 반환
   → 더 많은 요청 처리 가능
   → TPS 증가
   ```

### 교훈
```
🎯 Cache Layer 위치가 모든 것을 결정한다!
🎯 Early Return = 최고의 성능 최적화
🎯 짧은 TTL이 긴 TTL보다 나을 수 있다 (Lock Contention 관점)
🎯 Connection Pool은 부차적 (Cache 없으면 중요, 있으면 무의미)
```

---

## 📊 최종 성과

### Experiment 비교

| Metric | 04 (Baseline) | 04b (Pool 200/50) | 04c (Pool 500/200) | 04d (Service Cache) |
|--------|---------------|-------------------|-------------------|---------------------|
| Avg Response | 1,949ms | ~2,200ms | ~2,100ms | **10ms** 🚀 |
| P95 | 3,675ms | ~4,500ms | ~4,000ms | **7ms** 🚀 |
| TPS | 106 | ~90 | 140 | **480** 🚀 |
| 500 Errors | 1,244 | 3,381 ❌ | ~500 | **0** ✅ |
| Thread Usage | High | High | 99% | **50%** ✅ |

### 채택된 솔루션

**Experiment 04d (Service Layer Cache + 10초 TTL)**
- 평균 응답 **210배 개선**
- P95 **525배 개선**
- TPS **4.5배 증가**
- 에러 **완벽 제거**

---

## 🎓 핵심 교훈

### 1. Architecture > Tuning (04d vs 04b/04c)

**Bad Approach**:
```
"Connection Pool을 늘리자"
→ 200/50 → 500/200
→ 개선 미미
```

**Good Approach**:
```
"Cache Layer를 옮기자"
→ Provider → Service
→ 210배 개선!
```

**교훈**:
> Architecture 변경 > 설정 튜닝

### 2. Early Return의 위력

```
Provider Cache:
  Request → Service (5ms) → Provider (5ms) → Cache (0ms) → Return
  Total: 10ms

Service Cache:
  Request → Cache (0ms) → Return
  Total: < 1ms

차이: 10배!
```

### 3. TTL 역설: 짧을수록 빠를 수 있다

**Long TTL (3분)**:
```
장점: Cache Hit Rate 높음
단점: Cache Miss 시 대규모 Lock Contention
→ Thread Pool 고갈
```

**Short TTL (10초)**:
```
장점: Lock Contention 최소화
단점: Cache Hit Rate 약간 낮음
→ 하지만 전체 성능은 더 좋다!
```

**이유**:
```
Long TTL: Thundering Herd Problem
→ 3분마다 모든 요청이 동시에 Cache Miss
→ 모든 스레드가 Lock 대기

Short TTL: Gradual Miss
→ Cache Miss가 시간적으로 분산
→ Lock 경합 최소화
```

### 4. Connection Pool은 부차적

| Config | Pool | TPS | Notes |
|--------|------|-----|-------|
| 04b | 200/50 | ~90 | Pool 부족 |
| 04c | 500/200 | 140 | Pool 충분 |
| 04d | 500/200 | **480** | **Cache 효과!** |

**교훈**:
```
Cache 없으면: Connection Pool 중요
Cache 있으면: Connection Pool 무의미 (대부분 Cache Hit)
```

---

## 🚀 다음 단계

### Phase 2의 성과

✅ 평균 응답 **210배 개선** (1,949ms → 10ms)
✅ Service Layer Cache의 위력 검증
✅ TTL 최적화 (3분 → 10초)
✅ 에러 **완벽 제거**

### 그러나 남은 문제

**DB 성능**:
```
현재까지 최적화한 것:
- External API (Cache Layer)
- Connection Pool
- Thread Pool

아직 최적화하지 않은 것:
- Database (N+1 쿼리!)
- DB Connection Pool
- Query 최적화
```

**가설**:
```
"Portfolio CRUD API에도 N+1 쿼리 문제가 있을 것이다"
→ Portfolio → Holdings 연관 관계
→ Lazy Loading으로 추가 쿼리 발생
```

### Phase 3 계획

**목표**:
```
Portfolio CRUD API 최적화
→ N+1 쿼리 해결
→ DB 성능 극대화
```

**실험**:
1. N+1 쿼리 확인 (SQL 로깅)
2. Fetch Join vs Batch Size 비교
3. DB Connection Pool 튜닝
4. 종합 최적화

**예상**:
```
SQL Queries: 10~20 → < 5 per request
Avg Response: 100ms → < 50ms
TPS: 50 → > 200 req/s
```

---

## 📚 관련 문서

- **실험 설계**: (Phase 2에는 별도 DESIGN 문서 없음)
- **결과 분석**: `ANALYSIS.md` ← 사고의 흐름과 다음 단계
- **전체 현황**: `.claude/EXPERIMENT_STATUS.md`

---

**Last Updated**: 2025-11-21
**Status**: 완료 (Phase 3로 진행)
**Key Result**: Cache Layer 위치가 성능의 핵심, 210배 개선 달성
