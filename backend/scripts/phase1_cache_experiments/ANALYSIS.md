# Phase 1: Cache Optimization - Analysis & Thought Process

> **사고의 흐름과 다음 단계 계획**
>
> **기간**: 2025-11-14 ~ 2025-11-20
> **핵심 질문**: "왜 캐싱이 실패했고, 어떻게 성공으로 바꿨나?"

---

## 🧠 사고의 흐름

### Act 1: 문제 발견 (2025-11-14)

**관찰**:
```
부하 테스트 시 500 에러 827건 발생 (2.26%)
→ 간헐적이고 예측 불가
→ 사용자 경험 저하
```

**질문**:
```
Q1: 왜 500 에러가 발생하나?
→ A: 로그 분석 결과 KIS API Rate Limiting

Q2: Rate Limit은 언제 발생하나?
→ A: 동시 요청이 많을 때 (부하 테스트 시)

Q3: 어떻게 해결할 것인가?
→ A: API 호출을 줄여야 한다 → 캐싱!
```

**가설 1 설정**:
> "In-memory cache로 External API 호출을 줄이면
> Rate Limit을 피할 수 있을 것이다"

---

### Act 2: 첫 시도와 실패 (Experiment 01)

**구현**:
```java
@Cacheable(value = "stockPrice", key = "#symbol", sync = true)
public StockPriceDto getMarketData(String symbol) { ... }
```
- TTL: 1분
- 이유: 실시간성 유지 (주식 데이터는 빠르게 변함)

**기대**:
```
Cache Hit Rate 증가 → API 호출 감소 → Rate Limit 회피
→ 500 에러 감소
```

**실제 결과**:
```
500 에러: 827 → 1,140 (37.8% 증가!) ❌
```

**충격!**
```
예상과 정반대 결과...
왜 이런 일이?
```

---

### Act 3: 실패 원인 분석 (Deep Dive)

**가설 재검토**:
```
"캐시가 있으면 요청이 빨라진다"
→ 맞다 (216ms로 개선)

"요청이 빨라지면 더 많은 요청을 처리한다"
→ 맞다 (TPS 증가)

"더 많은 요청 → Rate Limit에 더 빨리 도달!"
→ 이게 문제였다!
```

**깨달음 1**:
```
❌ 캐싱은 만능이 아니다!

캐시의 역설:
- 응답은 빨라졌지만 (Good)
- 더 많은 요청이 몰려서 (Bad)
- Rate Limit에 더 빨리 도달했다 (Worse)
```

**근본 원인**:
```
문제: Rate Limit = 시간당 N개
캐시 TTL 1분 = 매 1분마다 Cache Miss
→ 매 1분마다 API 호출 spike 발생
→ Rate Limit 초과
```

---

### Act 4: 방향 전환 (Experiment 02)

**새로운 사고**:
```
"캐싱으로 안 되면... 실패를 관리하자!"
→ Circuit Breaker + Fallback
```

**가설 2**:
> "실패를 감지하고 대체 API로 전환하면
> 에러는 없어질 것이다"

**구현**:
```java
@CircuitBreaker(name = "kisApi", fallbackMethod = "fallbackGetMarketData")
public StockPriceDto getMarketData(String symbol) { ... }
```
- KIS API 실패 시 → AlphaVantage/Finnhub로 전환

**결과**:
```
500 에러: 827 → 0 ✅ (목표 달성!)
BUT...
응답시간: 582ms → 2,331ms (4배 증가!) ❌
```

**딜레마**:
```
에러는 없앴지만, 성능이 망가졌다...
이건 사용자에게 좋은 경험인가?
```

---

### Act 5: 문제 재정의

**원래 목표**:
```
"500 에러를 없애자"
```

**새로운 목표**:
```
"500 에러도 없고, 성능도 좋게 만들자"
```

**깨달음 2**:
```
✅ 문제 해결 != 새로운 문제 만들기

Circuit Breaker의 함정:
- 에러를 Fallback으로 덮었지만 (Good)
- Fallback API가 느려서 (Bad)
- 97%의 정상 요청도 느려졌다 (Worse)
```

**근본 원인 재분석**:
```
All-or-Nothing Fallback:
Circuit CLOSED → 모든 요청 KIS (빠름)
Circuit OPEN → 모든 요청 Fallback (느림)

문제:
- Circuit Breaker가 너무 민감 (5개 중 3개 실패 시 Open)
- 초기 burst에서 즉시 Open됨
- 그 후 모든 요청이 느린 API로 강제 전환
```

---

### Act 6: 본질로 돌아가기 (Experiment 03)

**질문의 재구성**:
```
Q: 왜 캐싱이 실패했나?
A: TTL이 너무 짧아서 (1분)

Q: 왜 1분을 선택했나?
A: 실시간성을 위해서

Q: 실시간성이 정말 1분이 필요한가?
A: 아니다... 금융 데이터는 3분도 충분하다
```

**깨달음 3**:
```
✅ 제약 조건을 다시 검토하라

Rate Limit = 분당 N개
→ 3분 TTL이면 충분히 회피 가능
→ 금융 데이터의 3분 delay는 허용 가능
  (API latency가 이미 300ms인데, 3분은 큰 차이 없음)
```

**가설 3**:
> "TTL을 3분으로 늘리면
> Rate Limit을 피하면서 성능도 유지할 수 있다"

**구현**:
```java
buildCache("stockPrice", 3, TimeUnit.MINUTES, 1_000)
```

**결과**:
```
500 에러: 827 → 0 ✅
Cache Hit Rate: ~85% ✅
External API Calls: 100% → 15% ✅
응답시간: 유지 ✅
```

**성공!**

---

## 📊 실험 결과 비교 분석

### 정량적 분석

| Metric | Baseline | Exp 01 | Exp 02 | Exp 03 |
|--------|----------|--------|--------|--------|
| 500 Errors | 827 | 1,140 ❌ | 0 ✅ | **0** ✅ |
| Avg Response | 222ms | 216ms | 2,331ms ❌ | **~220ms** ✅ |
| Cache Hit | 0% | ~30% | N/A | **~85%** ✅ |
| External API | 100% | ~70% | ~100% | **~15%** ✅ |
| User Experience | Bad | Worse | Very Bad | **Good** ✅ |

### 정성적 분석

**Experiment 01 (Cache 1min)**:
- ❌ 역효과 (에러 증가)
- ✅ CPU 사용량 감소
- 📝 캐싱의 한계 발견

**Experiment 02 (Circuit Breaker)**:
- ✅ 에러 제거 (목표 달성)
- ❌ 성능 저하 (새로운 문제)
- 📝 Fallback의 함정 발견

**Experiment 03 (TTL 3min)**:
- ✅ 에러 제거
- ✅ 성능 유지
- ✅ 단순한 해결책
- 📝 **승자!**

---

## 🎓 핵심 교훈

### 1. 측정 없이 최적화하지 마라

**Bad**:
```
"캐시를 추가하면 빨라질 거야"
→ 테스트 없이 배포
→ 오히려 에러 증가
```

**Good**:
```
가설 → 실험 → 측정 → 분석
→ 통계적 검증 (t-test)
→ 데이터 기반 의사결정
```

### 2. 해결책의 부작용을 고려하라

**Experiment 01**:
```
캐시 → 요청 집중 → Rate Limit 증가
```

**Experiment 02**:
```
Circuit Breaker → Fallback → 성능 저하
```

**교훈**:
```
한 문제를 풀면 다른 문제가 생긴다
→ 전체적인 영향을 고려해야
```

### 3. 단순함이 최고다

**복잡한 시도**:
```
Experiment 02:
- Circuit Breaker 설정
- Fallback Provider 추가
- Rate Limiter 설정
- Retry 로직
→ 복잡도 증가, 유지보수 어려움
```

**단순한 해결책**:
```
Experiment 03:
- Cache TTL 변경 (1줄)
→ 완벽한 해결
```

### 4. 제약 조건을 다시 검토하라

**초기 가정**:
```
"실시간성을 위해 TTL 1분 필요"
→ 검증 없는 가정
```

**재검토 후**:
```
"금융 데이터는 3분도 충분"
→ API latency(300ms)를 고려하면 3분 delay는 무의미
→ TTL 3분으로 완벽 해결
```

---

## 🚀 다음 단계로 넘어간 이유

### Phase 1의 성과

✅ 500 에러 **완전 제거**
✅ 단순한 해결책 (TTL 조정)
✅ 안정성 확보

### 그러나 발견한 문제

**Cache Layer 위치**:
```
현재: Provider Level Cache
→ Service logic 실행 → Provider 선택 → Cache 체크
→ Cache Hit이어도 Service overhead 발생
```

**가설**:
```
"Cache를 Service Layer로 옮기면
 Service logic을 완전히 스킵할 수 있지 않을까?"
```

**기대 효과**:
```
Provider Cache:
  Service overhead + Cache check = ~10-20ms

Service Cache:
  Early return = < 1ms

예상 개선: 10~20배!
```

### Phase 2 계획

**목표**:
```
Cache Layer 위치 최적화 + Connection Pool 튜닝
→ 극단적 성능 개선 (10ms 이하)
```

**실험**:
1. Mock API 구축 (Lambda)
2. Provider Cache vs Service Cache 비교
3. Connection Pool 튜닝
4. 최종 성능 측정

**예상**:
```
Avg Response: 220ms → < 50ms (4배 이상 개선)
P95: 500ms → < 100ms
TPS: 2배 증가
```

---

## 🤔 남은 질문

### Phase 1에서 해결하지 못한 것

1. **Connection Pool**:
   - 현재 Simple HTTP Client 사용
   - Connection Pool이 성능에 미치는 영향은?

2. **Cache Layer 위치**:
   - Provider vs Service - 실제 차이는?
   - 어디가 최적인가?

3. **TTL 최적값**:
   - 3분이 정말 최적인가?
   - 10초, 1분과 비교는?

→ **Phase 2에서 답을 찾자!**

---

## 📝 개선할 점

### 실험 설계

**좋았던 점**:
- 가설 → 실험 → 측정 → 분석 프로세스
- 통계적 검증 (t-test)
- 실패에서 배움

**아쉬웠던 점**:
- ❌ Baseline 측정 불충분 (Exp 01 전에 제대로 된 Baseline 없음)
- ❌ 한 번에 여러 변수 변경 (Exp 02)
- ❌ 실험 설계 문서 부재

### Phase 2에서 개선할 점

1. **명확한 Baseline 설정**
2. **한 번에 하나씩 변수 변경**
3. **실험 설계 문서 작성** (EXPERIMENT_DESIGN.md)
4. **더 많은 메트릭 수집** (Thread, Memory, DB)

---

## 🔗 Phase 2로의 연결

### Phase 1의 결론

```
문제: External API Rate Limit
해결: Cache TTL 최적화 (3분)
성과: 500 에러 완전 제거

하지만...
Cache Layer 위치 최적화로 더 큰 개선 가능성 발견!
```

### Phase 2의 목표

```
Cache Layer를 Service로 이동
→ Service overhead 제거
→ 극단적 성능 개선 (< 10ms)
```

### 연결 고리

```
Phase 1: "캐싱으로 에러를 해결했다"
Phase 2: "캐시 위치로 성능을 극대화하자"

Phase 1이 없었다면:
- Cache의 중요성을 몰랐을 것
- TTL 최적화를 발견하지 못했을 것
- Service Cache 아이디어가 나오지 않았을 것
```

---

## 📚 관련 문서

- **Problem-Solution**: `README.md`
- **전체 현황**: `.claude/EXPERIMENT_STATUS.md`
- **다음 단계**: `../phase2_external_api_optimization/`

---

**Last Updated**: 2025-11-21
**Status**: Phase 1 완료, Phase 2로 진행
**Key Insight**: "Cache Layer 위치가 성능의 핵심이다"
