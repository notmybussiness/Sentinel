# Sentinel Architecture

> 포트폴리오 관리 + 백테스팅 + AI 뉴스 분석 플랫폼의 아키텍처 문서

---

## 1. 프로젝트 구조

### Tech Stack

| Layer | Technology |
|-------|-----------|
| **Framework** | Spring Boot 3.5.5, Java 21 |
| **Persistence** | Spring Data JPA, QueryDSL |
| **Database** | PostgreSQL, Redis |
| **Security** | Spring Security, OAuth2 (Kakao), JWT |
| **Resilience** | Resilience4j (Circuit Breaker, Retry) |
| **Reactive** | Spring WebFlux, Reactor |
| **API Docs** | SpringDoc OpenAPI 3 (Swagger) |
| **Monitoring** | Actuator, Prometheus, Grafana |
| **Testing** | JUnit 5, Testcontainers |

### Domain Packages (11 Domains)

```
com.pjsent.sentinel/
├── ai/                    # AI Analysis (Gemini)
├── backtest/              # Backtesting Engine
├── common/                # Cross-cutting Concerns
├── config/                # Configuration
├── crypto/                # Cryptocurrency Data
├── exception/             # Global Exception Handling
├── market/                # Stock Market Data
├── portfolio/             # Portfolio Management
├── pricehistory/          # Historical Price Data
├── rebalancing/           # Portfolio Rebalancing
└── user/                  # Authentication & Users
```

### Standard Layer Structure (per domain)

```
domain/
├── controller/       # REST API Endpoints
├── service/          # Business Logic
├── repository/       # Data Access (JPA)
├── entity/           # JPA Entities
├── dto/              # Data Transfer Objects
└── exception/        # Domain-specific Exceptions
```

---

## 2. 인프라 구조 (3-Tier)

### Network Topology

| Node | Role | IP Address | Port(s) | Hardware Spec |
|------|------|------------|---------|---------------|
| **Client** | Load Generator (k6) | External (Mac) | - | MacBook Air M1 (8GB RAM) |
| **WAS** | App Server (Spring Boot) | `192.168.0.58` | 8080 | i5-12400F / 16GB RAM |
| **DB/Infra** | Database & Monitoring | `192.168.0.5` | 5432, 9090, 3000 | Ryzen 5 3400G / 12GB RAM |

### Component Details

**WAS Node (`192.168.0.58`)**
- CPU: Intel Core i5-12400F (6C/12T)
- RAM: 16GB
- Role: Spring Boot 3.5.5 Application Server
- Note: Strongest node, CPU usually stays low

**DB/Infra Node (`192.168.0.5`)**
- CPU: AMD Ryzen 5 3400G (4C/8T) - Potential Bottleneck
- RAM: 12GB
- Stack: PostgreSQL 15, Prometheus, Grafana
- Risk: Handles both DB queries AND monitoring ingestion

---

## 3. Event-Driven Architecture (EDA)

### 전체 아키텍처

```
External APIs (AlphaVantage, Yahoo Finance, Finnhub)
        ↓
MarketDataProvider Interface → MarketDataService
        ↓
MarketPriceProducer → Kafka Topic (market-price-updates)
        ↓
PortfolioPriceConsumer → PortfolioService → DB Update
```

### EDA 파일 구조

```
src/main/java/com/pjsent/sentinel/
├── common/event/
│   └── PriceUpdateEvent.java       # 이벤트 메시지 정의
├── market/producer/
│   └── MarketPriceProducer.java    # Kafka 이벤트 발행
├── portfolio/consumer/
│   └── PortfolioPriceConsumer.java # Kafka 이벤트 수신/처리
└── portfolio/scheduler/
    └── PortfolioPriceScheduler.java # Fallback 스케줄러 (1시간 주기)
```

### EDA vs Scheduler 비교

| 구분 | EDA (Kafka) | Scheduler (Fallback) |
|------|-------------|----------------------|
| **트리거** | 가격 변동 시 | 1시간 주기 |
| **지연 시간** | 수 초 이내 | 최대 1시간 |
| **용도** | 실시간 업데이트 | 데이터 정합성 보장 |

---

## 4. 실시간 스트리밍 아키텍처

### Adapter Pattern 구조

```
CryptoStreamController
        ↓
StreamingService Interface
    ├── SSEStreamingService        (권장 - 웹 대시보드)
    ├── WebSocketStreamingService  (고성능 - 트레이딩)
    └── LongPollingStreamingService (Fallback)
        ↓
External API (Upbit)
```

### 스트리밍 방식 비교

| 방식 | 레이턴시 | 장점 | 단점 | 권장 사용처 |
|------|----------|------|------|-------------|
| **SSE** | 50ms | 자동 재연결, 간단한 구현 | 단방향만 | 웹 대시보드 |
| **WebSocket** | 10ms | 양방향, 최고 성능 | 연결 관리 복잡 | 트레이딩 앱 |
| **LongPolling** | 1000ms | 호환성 최고 | 높은 오버헤드 | 레거시 시스템 |

### 스트리밍 파일 구조

```
src/main/java/com/pjsent/sentinel/crypto/
├── controller/
│   └── CryptoStreamController.java    # REST API 엔드포인트
├── streaming/
│   ├── StreamingService.java          # 인터페이스 (Adapter Pattern)
│   ├── SSEStreamingService.java       # SSE 구현
│   ├── WebSocketStreamingService.java # WebSocket 구현
│   └── LongPollingStreamingService.java # Long Polling 구현
└── dto/
    └── CryptoPriceDto.java            # 가격 데이터 DTO
```

### API Endpoints

```bash
# SSE (권장)
curl -N "http://localhost:8080/api/v1/crypto/stream/prices?symbols=BTC,ETH&method=SSE"

# 사용 가능한 방식 조회
curl "http://localhost:8080/api/v1/crypto/stream/methods"

# 서비스 상태 확인
curl "http://localhost:8080/api/v1/crypto/stream/status"
```

---

## 5. Hotspots (Performance-Critical)

| File | Concern | Recommendation |
|------|---------|----------------|
| `CryptoDataController` | Batch API calls | Caffeine cache (1m TTL) |
| `CryptoStreamController` | SSE/WebSocket streaming | Monitor memory under high concurrency |
| `PortfolioController` | `recalculatePortfolio` | Move to async job (Virtual Thread) |
| `MarketDataController` | AlphaVantage rate limits | Request throttling via Resilience4j |
| `PerfTestAuthController` | Bulk token generation | Profile-gated (perf only) |

---

## 6. Architecture Principles

1. **Layered DDD**: Clear separation (Controller → Service → Repository)
2. **Spring Boot 3 + Java 21**: Modern features (Records, Virtual Threads)
3. **Resilience4j**: Circuit Breaker for external APIs
4. **Caching**: Redis (distributed) + Caffeine (local)
5. **Reactive Streaming**: WebFlux for real-time crypto prices
6. **Security**: OAuth2 (Kakao) + JWT
7. **Observability**: Actuator + Prometheus + Grafana

---

**Source**: Consolidated from `backend/docs/` architecture documents
