# Phase 2: External API Optimization - Analysis

> **사고의 흐름과 다음 단계**
>
> **핵심 발견**: Cache Layer 위치가 성능의 모든 것을 결정했다

---

## 🧠 핵심 통찰

### The Breakthrough Moment

**질문**:
```
"Cache Hit이 85%인데 왜 여전히 느린가?"
```

**분석**:
```java
// Request 흐름 분해
MarketDataService.getStockPrice("AAPL")
  → providerFactory.getProvider()      // 5ms (Provider 선택)
  → provider.getMarketData("AAPL")     // 5ms (메서드 호출)
    → @Cacheable check                 // 0ms (Cache Hit!)
    → return cached data
  → mapping & response                 // 5ms

총 시간: 15ms (Cache Hit이어도!)
```

**깨달음**:
```
Cache가 너무 늦게 체크된다!
→ Service logic을 먼저 실행하고 Provider에서 Cache 체크
→ Cache Hit이어도 overhead 발생

해결책:
→ Cache를 Service Layer로 이동
→ Early Return (< 1ms)
```

---

## 📊 실험 여정

### Experiment 04b: Connection Pool (실패)

**가설**: "Connection Pool을 추가하면 빨라질 것이다"

**결과**: 오히려 에러 **171% 증가**!

**왜?**
```
maxConnPerRoute: 50
200 VUsers → 단일 Lambda URL
→ 50개로 부족
→ Connection Wait 발생
```

### Experiment 04c: Pool 증가 (부분 성공)

**가설**: "Pool을 더 늘리면 해결될 것이다"

**결과**: 에러 감소했지만 Thread Pool 포화 (99%)

**왜?**
```
Provider Cache (sync=true, TTL 3분)
→ 3분마다 대규모 Cache Miss 동시 발생
→ Lock Contention
→ Thread Pool 고갈
```

### Experiment 04d: Service Cache (극적 성공!)

**가설**: "Cache를 Service Layer로 옮기면 Early Return으로 극적 개선"

**결과**: **210배 개선**!

**왜?**
```
1. Early Return (< 1ms) vs Provider overhead (15ms)
2. TTL 10초로 Lock Contention 최소화
3. Thread Pool 50% 여유 확보
```

---

## 🎓 핵심 교훈

### 1. Architecture > Tuning

```
Connection Pool 늘리기 (04b, 04c): 부분적 개선
Cache Layer 이동 (04d): 210배 개선!
```

### 2. TTL의 역설

```
Long TTL (3분):
  장점: Cache Hit Rate 높음
  단점: Thundering Herd → Lock Contention

Short TTL (10초):
  장점: Lock Contention 최소화
  단점: Cache Hit Rate 약간 낮음
  → 전체 성능은 더 좋다!
```

### 3. Early Return의 위력

```
Provider Cache: 15ms (overhead 포함)
Service Cache: < 1ms (Early Return)
→ 15배 차이!
```

---

## 🚀 Phase 3로의 연결

### 남은 최적화 대상

```
✅ External API (Cache) - 완료
✅ Connection Pool - 완료
✅ Thread Pool - 여유 확보

❌ Database - 아직!
   - N+1 쿼리
   - DB Connection Pool
   - Query 최적화
```

### Phase 3 가설

```
"Portfolio CRUD API에도 N+1 쿼리가 있을 것이다"
→ Portfolio → Holdings 연관 관계
→ Lazy Loading
→ N+1 문제 예상
```

---

**Last Updated**: 2025-11-21
**Key Result**: Cache Layer 위치 = 성능의 핵심
**Next**: Phase 3 DB Optimization
