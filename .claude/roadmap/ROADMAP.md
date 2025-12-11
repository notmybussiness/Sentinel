# Sentinel Performance Optimization Roadmap

> **Last Updated**: 2025-11-24
> **Current Phase**: Phase 3 (DB Optimization)

---

## 🎯 Overview

```
Phase 1-2: Cache Optimization (완료) → 210배 성능 개선
Phase 3: DB Optimization (진행 중) → N+1 해결
Phase 4: Event-Driven (계획) → Kafka 도입
Phase 5: Real-time Communication (선택) → SSE vs WS
Phase 6: Reactive (보류) → WebFlux 부분 도입
```

---

## ✅ Phase 3: DB Optimization (현재)

### 3a. EntityGraph 적용 ✅
- Portfolio 1:N Holdings Fetch Join
- N+1 쿼리 해결

### 3b. Cache 효과 검증 (다음)
**목표**: Service Layer 캐싱 효과 측정
- Market: 5분 → 1분 TTL
- Crypto: 30초 → 10초 TTL

**측정 지표**:
- Avg Response Time
- Cache Hit Rate
- API Calls/min

**소요**: 1~2시간

### 3c. N+1 완전 해결
**목표**: Price 조회 N+1 제거

**방법**: `@BatchSize` 적용
```java
@BatchSize(size = 50)
@OneToOne
private PriceHistory latestPrice;
```

**예상 효과**:
- SQL: 10~20개 → 2~3개
- Response: 100ms → 30ms
- TPS: 50 → 200+

**소요**: 3~5시간

---

## 🔥 Phase 4: Event-Driven Architecture (추천)

### 목표
트랜잭션과 외부 API 분리 → 빠른 응답

### 아키텍처
```
[Controller] → [Service]
                  ↓
              [DB 저장] (10ms)
                  ↓
          [Kafka Event 발행] (1ms)
                  ↓
           [즉시 응답 200 OK]

[Kafka Consumer] (별도 프로세스)
    ↓
[외부 API 호출] (500ms)
[이메일 발송] (300ms)
```

### 구현 단계

**1. Kafka 인프라 (4시간)**
```yaml
# docker-compose.yml
zookeeper, kafka, kafka-ui
```

**2. 트랜잭션 분리 (1일)**
- `PortfolioCreatedEvent`
- `PriceUpdatedEvent`
- `RebalancingRecommendedEvent`

**3. 이벤트 소비자 (1일)**
- 가격 검증
- AI 분석
- 알림 발송

### 예상 효과
- Response: 810ms → **10ms** (81배)
- 장애 격리: ✅
- 재시도: ✅

**소요**: 2~3일

---

## 🌊 Phase 5: Real-time Communication (선택)

### 5a. SSE vs WebSocket 비교 실험
**목표**: 성능 비교 데이터 확보

**측정**:
- Latency (p50, p95, p99)
- Throughput (msg/sec)
- CPU/Memory Usage
- Connection Errors

**k6 테스트**:
```javascript
// 동시 접속 1000명
scenarios: {
  sse_test: { vus: 1000, duration: '5m' },
  ws_test: { vus: 1000, duration: '5m' }
}
```

**소요**: 4~6시간

### 5b. Portfolio 실시간 수익률
```java
@GetMapping("/portfolios/{id}/stream/returns")
public Flux<ServerSentEvent<ReturnDto>> streamReturns() {
    return Flux.interval(Duration.ofSeconds(10))
        .map(tick -> calculateReturns());
}
```

**소요**: 4시간

---

## 🔵 Phase 6: Reactive (보류)

### 판단
- ❌ 전체 WebFlux 전환: ROI 낮음 (3~5일, JPA → R2DBC 재작성)
- ✅ WebClient 부분 도입: HTTP 비동기만 (1~2일)

### 부분 도입 (선택)
```java
// 외부 API만 WebClient 사용
@Service
public class MarketDataService {
    private final WebClient webClient;

    public Mono<StockPrice> getPrice(String symbol) {
        return webClient.get()
            .retrieve()
            .bodyToMono(StockPrice.class)
            .block();  // MVC 호환
    }
}
```

**소요**: 1~2일 (시간 있으면)

---

## 📅 Timeline

### Week 1 (현재)
- ✅ Day 1: Cache 효과 검증 (1~2h)
- ✅ Day 2-3: N+1 완전 해결 (3~5h)

### Week 2
- 🔥 Day 1: Kafka 인프라 (4h)
- 🔥 Day 2-3: Event-Driven 전환 (2일)
- 🔥 Day 4: Portfolio 실시간 스트리밍 (4h)

### Week 3 (선택)
- ⚠️ SSE vs WS 비교 실험 (4~6h)
- ⚠️ WebClient 부분 도입 (1~2일)

---

## 🎓 Portfolio Value

### 어필 포인트
```markdown
# 성능 최적화
- N+1 쿼리 해결: 20개 → 2~3개 (BatchSize)
- API 응답: 100ms → 30ms (3배 개선)
- Cache 전략: Market(1분) vs Crypto(10초)

# Event-Driven Architecture
- Kafka 이벤트 스트리밍
- 트랜잭션 분리: 810ms → 10ms (81배)
- 장애 격리 및 재시도

# 실시간 통신
- SSE vs WebSocket 성능 비교
- Portfolio 실시간 수익률 Push
```

---

## 📚 Documentation

- **실험 상태**: `.claude/EXPERIMENT_STATUS.md`
- **메인 가이드**: `.claude/CLAUDE.md`
- **실험 결과**: `backend/scripts/results/`
  - Phase 1: Cache Experiments
  - Phase 2: External API Optimization
  - Phase 3: DB Optimization

---

## 🚨 Important

**다음 작업 시작 전 반드시 읽기**:
1. `.claude/EXPERIMENT_STATUS.md` - 현재 위치 파악
2. 해당 Phase의 `README.md` - 문제와 해결책
3. 해당 Phase의 `ANALYSIS.md` - 사고의 흐름

**원칙**:
- 불명확한 점은 사용자에게 질문
- 한 번에 하나씩 (변수 분리)
- 측정 필수 (Baseline → 변경 → 비교)

---

**Last Updated**: 2025-11-24
**Next Task**: Cache 효과 검증 (Phase 3b)
