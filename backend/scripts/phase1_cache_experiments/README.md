# Phase 1: Cache Optimization Experiments

> **Problem → Solution 기록**
>
> **기간**: 2025-11-14 ~ 2025-11-20
> **목표**: External API Rate Limit으로 인한 500 에러 제거

---

## 🔴 Problem: External API Rate Limiting

### 초기 증상
- `POST /api/v1/market/prices` API에서 **500 에러 빈발**
- 부하 테스트 시 에러율 **2.26%** (36,600건 중 827건)
- 사용자 경험 저하 (간헐적 실패)

### 근본 원인
```
한국투자증권(KIS) API Rate Limiting
→ 동시 요청 과다 시 429 Too Many Requests
→ Backend에서 500 에러로 변환되어 반환
```

### 목표
- ❌ 500 에러 **827건 → 0건** 완전 제거
- ✅ 응답 시간 개선
- ✅ 시스템 안정성 확보

---

## 🔬 Solution 1: Caffeine Cache 도입 (Experiment 01)

### 가설
```
"In-memory cache로 External API 호출을 줄이면 Rate Limit을 피할 수 있다"
```

### 구현
```java
@Cacheable(value = "stockPrice", key = "#symbol", sync = true)
public StockPriceDto getMarketData(String symbol) {
    return kisProvider.getMarketData(symbol);
}
```

**설정**:
- Cache: Caffeine (in-memory)
- TTL: **1분**
- Sync: true (동시 요청 중복 방지)

### 결과

| Metric | Before | After | Change |
|--------|--------|-------|--------|
| Avg Response | 222ms | 216ms | -2.77% |
| CPU Peak | 11.8% | 9.81% | -16.9% |
| **500 Errors** | 827 | **1,140** | **+37.8%** ❌ |

### 분석

**예상과 정반대 결과!**

원인:
1. 실시간 주식 데이터는 **캐싱이 어려움** (1분 TTL로도 빈번한 miss)
2. 캐시로 빨라진 요청 → **Rate Limit에 더 빨리 도달**
3. 역효과: 더 많은 요청이 집중되어 **에러 증가**

### 교훈
```
❌ 캐싱은 만능이 아니다
❌ 실시간 데이터에는 캐싱이 역효과를 낼 수 있다
✅ 병목 원인(Rate Limit)을 직접 해결해야 한다
```

---

## 🔬 Solution 2: Circuit Breaker 적용 (Experiment 02)

### 가설
```
"Circuit Breaker로 실패를 감지하고,
 Fallback Provider로 대체하면 에러가 사라질 것이다"
```

### 구현

**1. Resilience4j 적용**
```java
@CircuitBreaker(name = "kisApi", fallbackMethod = "fallbackGetMarketData")
@RateLimiter(name = "kisApi")
@Retry(name = "marketDataApi")
public StockPriceDto getMarketData(String symbol) { ... }
```

**2. Circuit Breaker 설정**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      kisApi:
        failure-rate-threshold: 50%      # 50% 실패 시 Open
        minimum-number-of-calls: 5       # 5개 요청 후 판단
        wait-duration-in-open-state: 10s
```

**3. Fallback Provider 활성화**
```yaml
alphavantage:
  enabled: true
finnhub:
  enabled: true
```

### 결과

| Metric | Baseline | With CB | Change |
|--------|----------|---------|--------|
| **500 Errors** | 827 | **0** | **-100%** ✅ |
| Avg Response | 582ms | 2,331ms | **+300%** ❌ |
| P95 | 706ms | 5,001ms | +608% ❌ |
| TPS | 61 req/s | 15 req/s | -75% ❌ |

### 분석

**반쪽짜리 성공!**

✅ 좋은 점:
- 500 에러 **완전히 제거** (827 → 0)
- Circuit Breaker가 의도대로 작동
- Fallback Provider로 요청 처리 성공

❌ 나쁜 점:
- 응답시간 **4배 증가** (582ms → 2,331ms)
- 처리량 **75% 감소**
- 사용자 경험 **대폭 악화**

**근본 원인**:
1. **Circuit Breaker가 너무 민감** (5개 중 3개 실패 시 즉시 Open)
2. **All-or-Nothing Fallback** (97%의 정상 요청도 느린 API로 강제 라우팅)
3. **Fallback API 본질적으로 느림** (KIS 300ms vs Fallback 2,000ms)

### 교훈
```
⚠️ 문제 해결 방법이 새로운 문제를 만들 수 있다
⚠️ Circuit Breaker는 세밀한 튜닝이 필요하다
⚠️ Fallback은 "최후의 수단"이어야 한다
```

---

## ✅ Solution 3: Cache TTL 최적화 (Experiment 03)

### 가설
```
"TTL을 늘려 Cache Hit Rate를 높이면
 Rate Limit을 피하면서 성능도 유지할 수 있다"
```

### 구현

**변경사항**:
```java
// Cache TTL 증가
buildCache("stockPrice", 3, TimeUnit.MINUTES, 1_000)  // 1분 → 3분
```

**이유**:
- Rate Limit이 분당 제한이므로 **3분 TTL로 충분히 회피**
- 금융 데이터의 경우 3분 delay는 허용 가능

### 결과

| Metric | Baseline | Optimized | Change |
|--------|----------|-----------|--------|
| **500 Errors** | 827 | **0** | **-100%** ✅ |
| Cache Hit Rate | 0% | ~85% | - |
| External API Calls | 100% | ~15% | **-85%** ✅ |

### 분석

**완벽한 성공!**

✅ 500 에러 **완전 제거**
✅ 성능 유지 (Cache Hit로 빠른 응답)
✅ External API 호출 **85% 감소**

**왜 성공했나?**:
1. 3분 TTL로 **Rate Limit 시간대를 회피**
2. Cache Hit Rate 85%로 **대부분의 요청이 캐시에서 처리**
3. Circuit Breaker 없이도 안정성 확보

### 교훈
```
✅ TTL을 Rate Limit 주기에 맞춰 조정하면 효과적
✅ 실시간성과 안정성의 균형점을 찾는 것이 중요
✅ 단순한 해결책이 가장 효과적일 수 있다
```

---

## 📊 최종 성과

### Experiment 비교

| Metric | Baseline | Exp 01 (Cache 1min) | Exp 02 (CB) | Exp 03 (TTL 3min) |
|--------|----------|---------------------|-------------|-------------------|
| 500 Errors | 827 | 1,140 ❌ | 0 ✅ | **0** ✅ |
| Avg Response | 222ms | 216ms | 2,331ms ❌ | ~220ms ✅ |
| Cache Hit | 0% | ~30% | - | **~85%** ✅ |

### 채택된 솔루션

**Experiment 03 (Cache TTL 3분)**
- 500 에러 완전 제거
- 성능 유지
- 구현 단순 (설정 변경만)

---

## 🎓 핵심 교훈

### 1. 캐싱은 만능이 아니다 (Exp 01)
- 실시간 데이터에는 **역효과** 가능
- TTL이 너무 짧으면 의미 없음

### 2. Circuit Breaker는 신중하게 (Exp 02)
- 기본값은 **일반적인 상황용**
- 세밀한 튜닝 필요
- Fallback의 성능도 고려해야

### 3. 단순함이 최고 (Exp 03)
- 복잡한 로직보다 **TTL 조정**이 효과적
- Rate Limit 주기에 맞춰 설정
- 측정 없이 최적화하지 마라

---

## 🚀 다음 단계

Phase 1에서 **Cache TTL 최적화**로 500 에러를 완전히 제거했지만,
여전히 개선 여지가 있었습니다:

**발견된 문제**:
- Cache가 **Provider Level**에 위치
- Cache Hit이어도 **Service overhead** 발생
- Connection Pool 미적용

**Phase 2로 넘어간 이유**:
> "Cache Layer **위치**를 최적화하면 더 큰 성능 개선이 가능할 것이다"

→ **Phase 2: External API Optimization**으로 진행

---

## 📚 관련 문서

- **실험 설계**: (Phase 1에는 별도 DESIGN 문서 없음)
- **결과 분석**: `ANALYSIS.md` ← 사고의 흐름과 다음 단계
- **전체 현황**: `.claude/EXPERIMENT_STATUS.md`

---

**Last Updated**: 2025-11-21
**Status**: 완료 (Phase 2로 진행)
**Key Result**: 500 에러 완전 제거, TTL 최적화의 중요성 발견
