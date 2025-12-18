# Learning Log - 2025-12-18

## Kafka Consumer + Concurrent Update Deadlock

### 발견된 문제

**증상**: E2E 테스트 실행 중 PostgreSQL Deadlock 발생

```
ERROR: deadlock detected
  Detail: Process 23118 waits for ShareLock on transaction 14705; blocked by process 23095.
  Process 23095 waits for ShareLock on transaction 14704; blocked by process 23118.
  Where: while updating tuple (0,30) in relation "portfolios"
```

### 원인 분석

**충돌 지점**:
1. **Kafka Consumer** (`PortfolioPriceConsumer`): 가격 업데이트 이벤트 수신 → Portfolio 업데이트
2. **E2E Test**: 포트폴리오 수정 요청 → Portfolio 업데이트

**시나리오**:
```
[T1: Kafka Consumer]           [T2: E2E Test]
UPDATE portfolios              UPDATE portfolios
WHERE symbol='GOOG'            WHERE id=9
    ↓                              ↓
Portfolio ID=9 락 시도          Portfolio ID=9 락 시도
    ↓                              ↓
    ← DEADLOCK DETECTED →
```

### 현재 상태

- **테스트 결과**: E2E 테스트 자체는 **PASSED** (Kafka retry 로직 작동)
- **영향**: 실 서비스에서 동시 업데이트 시 일시적 에러 가능

### 해결 방안 (TODO)

#### Option 1: 낙관적 락 (Optimistic Locking)
```java
@Entity
public class Portfolio {
    @Version
    private Long version;
}
```
- 장점: 충돌 발생 시 예외 → 재시도
- 단점: 재시도 로직 구현 필요

#### Option 2: Kafka Consumer 순차 처리
```java
@KafkaListener(concurrency = "1")  // 단일 Consumer
public void handlePriceUpdate(PriceUpdateEvent event) {
    // 순차 처리로 충돌 방지
}
```
- 장점: 간단
- 단점: 처리량 저하

#### Option 3: 분산 락 (Redis)
```java
@DistributedLock(key = "portfolio:#{#portfolioId}")
public void updatePortfolio(Long portfolioId) {
    // ...
}
```
- 장점: 완전한 동시성 제어
- 단점: 복잡도 증가

### 우선순위

- **현재**: Low (테스트 통과, Kafka retry 정상 작동)
- **Phase 8**: 프로덕션 안정성 작업 시 재검토

---

**관련 파일**:
- `PortfolioPriceConsumer.java`: Kafka Consumer
- `PortfolioService.java`: Portfolio 업데이트 로직
- `frontend/e2e/portfolio-crud.spec.ts`: E2E 테스트
