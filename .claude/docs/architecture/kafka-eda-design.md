# Kafka Event-Driven Architecture (Phase 7)

> **Created**: 2025-12-03
> **Purpose**: Implement Event-Driven Architecture for Portfolio price updates
> **Expected Improvement**: Response Time 810ms → 10ms (81x)

---

## 🎯 Problem Statement

### Current Architecture (Synchronous)
```
POST /api/v1/portfolios/{id}/holdings
   ↓
PortfolioService.addHolding()
   ↓ (Blocking)
External API Call (MarketDataService, CryptoDataService)
   ↓ (300-400ms)
DB Update (Portfolio + Holding)
   ↓
Response (810ms total)
```

### Issues
1. **Slow Response**: 810ms 평균 응답 시간
2. **Blocking I/O**: 외부 API 호출이 트랜잭션 내부에서 발생
3. **Failure Propagation**: Upbit API 500 에러 → Portfolio API 실패
4. **Resource Waste**: 스레드가 I/O 대기로 블로킹됨

---

## 🚀 Proposed Architecture (Asynchronous)

### Event-Driven Flow
```
POST /api/v1/portfolios/{id}/holdings
   ↓
PortfolioService.addHolding()
   ↓
DB Insert (Holding only)
   ↓
Kafka Producer.send(PortfolioHoldingCreatedEvent)
   ↓
Response (202 Accepted, ~10ms) ← **즉시 응답**

--- (비동기 경계) ---

Kafka Consumer.consume(PortfolioHoldingCreatedEvent)
   ↓ (백그라운드)
External API Call (격리됨)
   ↓
DB Update (Current Price, Total Value)
   ↓
(Optional) Kafka Producer.send(PortfolioUpdatedEvent)
```

### Benefits
1. **Fast Response**: 10ms 응답 (DB 쓰기만)
2. **Fault Isolation**: 외부 API 장애가 Portfolio API에 영향 없음
3. **Retry Logic**: Kafka Consumer에서 자동 재시도
4. **Scalability**: Consumer를 독립적으로 확장 가능

---

## 📐 Architecture Design

### Event Types
```java
// 1. Portfolio Domain Events
public record PortfolioCreatedEvent(
    Long portfolioId,
    Long userId,
    String name,
    LocalDateTime createdAt
) {}

public record PortfolioHoldingCreatedEvent(
    Long portfolioId,
    Long holdingId,
    AssetType assetType,  // STOCK, CRYPTO
    String symbol,
    BigDecimal quantity,
    LocalDateTime createdAt
) {}

public record PortfolioPriceUpdateRequestedEvent(
    Long portfolioId,
    LocalDateTime requestedAt
) {}

// 2. Result Events (Optional, for notification)
public record PortfolioUpdatedEvent(
    Long portfolioId,
    BigDecimal totalValue,
    BigDecimal totalCost,
    BigDecimal profitLoss,
    LocalDateTime updatedAt
) {}
```

### Topics
```yaml
Topics:
  - portfolio.holding.created      # Holding 생성 이벤트
  - portfolio.price-update.requested  # 가격 업데이트 요청
  - portfolio.updated              # 업데이트 완료 (Optional, for WebSocket notification)
```

---

## 🔧 Implementation Plan

### Step 1: Kafka Setup (Docker Compose)
**File**: `docker-compose.yml`

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    depends_on:
      - kafka
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

**Verification**:
```bash
docker-compose up -d
docker-compose ps
# Expected: zookeeper, kafka, kafka-ui (all UP)

# Access Kafka UI
open http://localhost:8090
```

---

### Step 2: Spring Kafka Configuration
**File**: `build.gradle`

```gradle
dependencies {
    implementation 'org.springframework.kafka:spring-kafka'

    // JSON serialization
    implementation 'com.fasterxml.jackson.core:jackson-databind'

    // Testing
    testImplementation 'org.springframework.kafka:spring-kafka-test'
}
```

**File**: `application-perf.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: 1  # Leader acknowledgement (balance between performance and reliability)
      retries: 3
    consumer:
      group-id: sentinel-portfolio-consumer
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      properties:
        spring.json.trusted.packages: com.pjsent.sentinel.portfolio.event
```

**File**: `KafkaConfig.java`

```java
@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic portfolioHoldingCreatedTopic() {
        return TopicBuilder.name("portfolio.holding.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic portfolioPriceUpdateRequestedTopic() {
        return TopicBuilder.name("portfolio.price-update.requested")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic portfolioUpdatedTopic() {
        return TopicBuilder.name("portfolio.updated")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
```

---

### Step 3: Event Producer
**File**: `PortfolioEventProducer.java`

```java
@Component
@Slf4j
public class PortfolioEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PortfolioEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendHoldingCreatedEvent(PortfolioHoldingCreatedEvent event) {
        String key = event.portfolioId().toString();

        kafkaTemplate.send("portfolio.holding.created", key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Sent PortfolioHoldingCreatedEvent: portfolioId={}, holdingId={}",
                                event.portfolioId(), event.holdingId());
                    } else {
                        log.error("Failed to send PortfolioHoldingCreatedEvent: {}", event, ex);
                    }
                });
    }

    public void sendPriceUpdateRequestedEvent(PortfolioPriceUpdateRequestedEvent event) {
        String key = event.portfolioId().toString();

        kafkaTemplate.send("portfolio.price-update.requested", key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Sent PortfolioPriceUpdateRequestedEvent: portfolioId={}",
                                event.portfolioId());
                    } else {
                        log.error("Failed to send PortfolioPriceUpdateRequestedEvent: {}", event, ex);
                    }
                });
    }
}
```

---

### Step 4: Modify PortfolioService (Producer Side)
**File**: `PortfolioService.java`

```java
@Service
@Transactional
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingRepository holdingRepository;
    private final PortfolioEventProducer eventProducer;  // ← 추가

    // ❌ Before: 동기식 (810ms)
    // public PortfolioDto addHolding(Long portfolioId, HoldingCreateRequest request) {
    //     Portfolio portfolio = portfolioRepository.findById(portfolioId)...;
    //     PortfolioHolding holding = holdingRepository.save(...);
    //
    //     // Blocking I/O
    //     updateHoldingPrice(holding);
    //
    //     return convertToDto(portfolio);
    // }

    // ✅ After: 비동기식 (10ms)
    public PortfolioDto addHolding(Long portfolioId, HoldingCreateRequest request) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        // 1. DB에 Holding 저장 (가격 정보 없이)
        PortfolioHolding holding = PortfolioHolding.builder()
                .portfolio(portfolio)
                .assetType(request.assetType())
                .symbol(request.symbol())
                .quantity(request.quantity())
                .averageCost(request.averageCost())
                // currentPrice, totalValue는 null (나중에 업데이트)
                .build();

        PortfolioHolding saved = holdingRepository.save(holding);

        // 2. Kafka 이벤트 발행 (비동기 처리 요청)
        PortfolioHoldingCreatedEvent event = new PortfolioHoldingCreatedEvent(
                portfolioId,
                saved.getId(),
                saved.getAssetType(),
                saved.getSymbol(),
                saved.getQuantity(),
                LocalDateTime.now()
        );
        eventProducer.sendHoldingCreatedEvent(event);

        // 3. 즉시 응답 (202 Accepted)
        return convertToDto(portfolio);  // ← ~10ms
    }
}
```

---

### Step 5: Event Consumer
**File**: `PortfolioEventConsumer.java`

```java
@Component
@Slf4j
public class PortfolioEventConsumer {

    private final PortfolioHoldingRepository holdingRepository;
    private final MarketDataService marketDataService;
    private final CryptoDataService cryptoDataService;

    @KafkaListener(
        topics = "portfolio.holding.created",
        groupId = "sentinel-portfolio-consumer"
    )
    @Transactional
    public void handleHoldingCreatedEvent(PortfolioHoldingCreatedEvent event) {
        log.info("Received PortfolioHoldingCreatedEvent: {}", event);

        try {
            // 1. Holding 조회
            PortfolioHolding holding = holdingRepository.findById(event.holdingId())
                    .orElseThrow(() -> new HoldingNotFoundException(event.holdingId()));

            // 2. 외부 API 호출 (격리됨, 실패해도 Portfolio API에 영향 없음)
            BigDecimal currentPrice = fetchCurrentPrice(holding);

            // 3. DB 업데이트 (currentPrice, totalValue)
            holding.setCurrentPrice(currentPrice);
            holding.setTotalValue(currentPrice.multiply(holding.getQuantity()));
            holdingRepository.save(holding);

            log.info("Updated holding price: holdingId={}, price={}",
                    holding.getId(), currentPrice);

            // 4. (Optional) Portfolio 전체 재계산 및 알림 이벤트 발행
            // recalculatePortfolioTotalValue(event.portfolioId());

        } catch (Exception e) {
            log.error("Failed to process PortfolioHoldingCreatedEvent: {}", event, e);

            // Kafka will retry automatically (max 3 times)
            // After max retries, message goes to DLT (Dead Letter Topic)
            throw e;
        }
    }

    private BigDecimal fetchCurrentPrice(PortfolioHolding holding) {
        return switch (holding.getAssetType()) {
            case STOCK, ETF -> marketDataService.getStockPrice(holding.getSymbol()).getPrice();
            case CRYPTO -> cryptoDataService.getCryptoPrice(holding.getSymbol(), "USD").getPrice();
            case INDEX -> throw new UnsupportedOperationException("Index not tradable");
        };
    }
}
```

---

### Step 6: Error Handling & DLT (Dead Letter Topic)
**File**: `KafkaConfig.java`

```java
@Configuration
@EnableKafka
public class KafkaConfig {

    // ... (previous code)

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name("portfolio.holding.created.dlt")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Error handling with retry and DLT
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate(),
                        (record, ex) -> new TopicPartition(record.topic() + ".dlt", -1)),
                new FixedBackOff(1000L, 3L)  // Retry 3 times with 1s interval
        ));

        return factory;
    }
}
```

---

## 🧪 Testing Strategy

### Unit Test (Producer)
**File**: `PortfolioServiceTest.java`

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"portfolio.holding.created"})
class PortfolioServiceTest {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void addHolding_shouldSendKafkaEvent() {
        // Given
        HoldingCreateRequest request = new HoldingCreateRequest(
                AssetType.STOCK, "AAPL", new BigDecimal("10"), new BigDecimal("150.00")
        );

        // When
        PortfolioDto result = portfolioService.addHolding(1L, request);

        // Then
        assertThat(result).isNotNull();

        // Verify Kafka message sent (check with EmbeddedKafka)
        // ...
    }
}
```

### Integration Test (Consumer)
**File**: `PortfolioEventConsumerTest.java`

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"portfolio.holding.created"})
class PortfolioEventConsumerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PortfolioHoldingRepository holdingRepository;

    @Test
    void consumer_shouldUpdateHoldingPrice_whenEventReceived() throws Exception {
        // Given
        PortfolioHoldingCreatedEvent event = new PortfolioHoldingCreatedEvent(
                1L, 100L, AssetType.STOCK, "AAPL", new BigDecimal("10"), LocalDateTime.now()
        );

        // When
        kafkaTemplate.send("portfolio.holding.created", "1", event).get();

        // Wait for consumer to process
        Thread.sleep(3000);

        // Then
        PortfolioHolding holding = holdingRepository.findById(100L).orElseThrow();
        assertThat(holding.getCurrentPrice()).isNotNull();
        assertThat(holding.getTotalValue()).isNotNull();
    }
}
```

### Performance Test (k6)
**File**: `scripts/phase7_eda/exp11_async_portfolio.js`

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 100 },
    { duration: '1m', target: 500 },
    { duration: '30s', target: 0 },
  ],
};

export default function () {
  const token = __ENV.AUTH_TOKEN;

  // POST /api/v1/portfolios/{id}/holdings
  const payload = JSON.stringify({
    assetType: 'STOCK',
    symbol: 'AAPL',
    quantity: 10,
    averageCost: 150.00,
  });

  const res = http.post(
    `http://localhost:8080/api/v1/portfolios/1/holdings`,
    payload,
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
    }
  );

  check(res, {
    'status is 202 Accepted': (r) => r.status === 202,
    'response time < 50ms': (r) => r.timings.duration < 50,  // ← Target!
  });

  sleep(1);
}
```

**Expected Results**:
- Avg Response Time: **< 20ms** (Phase 4a: 347ms → Phase 7: 10~20ms)
- P95 Response Time: **< 50ms**
- TPS: > 500 req/s
- Error Rate: < 1% (external API failures isolated)

---

## 📊 Success Criteria

### Performance Metrics
| Metric | Before (Phase 6) | Target (Phase 7) | Improvement |
|--------|------------------|------------------|-------------|
| **Avg Response Time** | 347ms | **< 20ms** | -94% |
| **P95 Response Time** | 460ms | **< 50ms** | -89% |
| **TPS** | 483 req/s | **> 500 req/s** | +3% |
| **Error Rate (External API)** | 8.5% | **< 1%** (isolated) | -88% |

### Architecture Quality
- [ ] Portfolio API는 외부 API 장애에 영향받지 않음
- [ ] Kafka Consumer가 재시도 로직을 통해 일시적 장애 극복
- [ ] DLT (Dead Letter Topic)로 영구 실패 메시지 처리
- [ ] Eventual Consistency 허용 가능 (사용자는 5초 내 최신 가격 확인 가능)

---

## 🚨 Risk Analysis

### Risk 1: Eventual Consistency
**문제**: 사용자가 Holding 추가 직후 최신 가격을 볼 수 없음
**해결**:
- Frontend에서 "가격 업데이트 중..." 표시
- WebSocket으로 업데이트 완료 알림 (PortfolioUpdatedEvent)
- 또는 Polling으로 5초마다 재조회

### Risk 2: Kafka Downtime
**문제**: Kafka 장애 시 이벤트 유실
**해결**:
- Fallback: Kafka 전송 실패 시 DB에 "pending" 상태로 저장
- Background Job으로 pending 항목 재처리

### Risk 3: Message Ordering
**문제**: 같은 Portfolio에 대한 이벤트가 순서대로 처리되지 않을 수 있음
**해결**:
- Partition Key를 portfolioId로 설정 → 같은 Portfolio는 항상 같은 Partition
- 같은 Partition 내에서는 순서 보장됨

---

## 📅 Implementation Timeline

### Week 1: Setup & Basic Integration
- [ ] Docker Compose (Kafka + Zookeeper + Kafka UI)
- [ ] Spring Kafka dependency & configuration
- [ ] Event models & topics
- [ ] PortfolioEventProducer

### Week 2: Consumer & Error Handling
- [ ] PortfolioEventConsumer
- [ ] External API integration
- [ ] DLT (Dead Letter Topic)
- [ ] Unit tests

### Week 3: Performance Test & Optimization
- [ ] k6 script (exp11_async_portfolio.js)
- [ ] Performance baseline & comparison
- [ ] Monitoring (Kafka lag, consumer metrics)
- [ ] Documentation update

---

## 🔜 Future Enhancements (Phase 8+)

1. **Kafka Streams**: Real-time aggregation (Portfolio total value)
2. **Event Sourcing**: Store all Portfolio changes as events
3. **CQRS**: Separate Read Model (optimized for queries)
4. **Saga Pattern**: Distributed transactions across multiple domains

---

**Estimated Duration**: 2-3 weeks
**Risk Level**: Medium (new infrastructure component)
**Expected ROI**: High (81x response time improvement + fault isolation)
