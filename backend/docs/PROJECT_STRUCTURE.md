# 📐 PROJECT STRUCTURE

> **Last Updated**: 2025-11-25  
> **Project**: Sentinel Backend (Spring Boot 3.5.5 + Java 21)  
> **Architecture**: Layered DDD (Domain-Driven Design)

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Framework** | Spring Boot 3.5.5, Java 21 |
| **Persistence** | Spring Data JPA, QueryDSL |
| **Database** | PostgreSQL, MySQL, H2 (dev) |
| **Cache** | Redis, Caffeine (Local) |
| **Security** | Spring Security, OAuth2 (Kakao), JWT |
| **Resilience** | Resilience4j (Circuit Breaker, Retry) |
| **Reactive** | Spring WebFlux, Reactor |
| **API Docs** | SpringDoc OpenAPI 3 (Swagger) |
| **Monitoring** | Actuator, Prometheus |
| **Testing** | JUnit 5, Testcontainers |

---

## 📦 Domain Packages (11 Domains)

```
com.pjsent.sentinel/
├── 📊 ai/                    # AI Analysis (Gemini)
├── 🔄 backtest/              # Backtesting Engine
├── 🔧 common/                # Cross-cutting Concerns
├── ⚙️  config/               # Configuration
├── 🪙 crypto/                # Cryptocurrency Data
├── 🚨 exception/             # Global Exception Handling
├── 📈 market/                # Stock Market Data
├── 💼 portfolio/             # Portfolio Management
├── 📉 pricehistory/          # Historical Price Data
├── ⚖️  rebalancing/          # Portfolio Rebalancing
└── 👤 user/                  # Authentication & Users
```

---

## 🏛️ Layered Architecture

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

## 🗂️ Domain Details

### 1️⃣ AI Domain (`ai/`)
**Purpose**: Gemini AI-based portfolio analysis  
**Controllers**: `AiController`  
**Services**: `GeminiService`, `PortfolioAnalysisService`  
**Key Features**:
- Portfolio AI analysis (risk, diversification)
- Investment recommendations
- Gemini API integration

---

### 2️⃣ Backtest Domain (`backtest/`)
**Purpose**: Historical portfolio performance simulation  
**Controllers**: `BacktestController`  
**Services**: `HistoricalDataService`  
**Key Features**:
- Strategy backtesting (NONE, MONTHLY, QUARTERLY, YEARLY)
- 10-year historical data support (AlphaVantage)

---

### 3️⃣ Common Domain (`common/`)
**Purpose**: Infrastructure & utilities  
**Controllers**: `HealthController`, `DevController`, `CustomErrorController`  
**Key Features**:
- Health check (`/api/health`)
- Dev JWT token generation
- Custom error page handling

---

### 4️⃣ Config Domain (`config/`)
**Purpose**: Application configuration  
**Files**:
- `CacheConfig.java` - Redis + Caffeine cache setup
- `DataInitializer.java` - Initial data seeding
- `PerfTestSecurityConfig.java` - Perf profile security config

---

### 5️⃣ Crypto Domain (`crypto/`)
**Purpose**: Cryptocurrency real-time & historical data  
**Controllers**: `CryptoDataController`, `CryptoStreamController`  
**Services**: `CryptoDataService`  
**Streaming**: `SSEStreamingService`, `WebSocketStreamingService`, `LongPollingStreamingService`  
**Key Features**:
- Real-time crypto price streaming (SSE/WebSocket/LongPolling)
- Batch price retrieval
- Trending coins (by volume)
- Symbol search
- Historical candlestick data

⚠️ **SRE Note**: High-concurrency streaming with Adapter Pattern for method selection

---

### 6️⃣ Market Domain (`market/`)
**Purpose**: Stock market data (AlphaVantage)  
**Controllers**: `MarketDataController`  
**Services**: `MarketDataService`  
**Repository**: `MarketDataRepository`  
**Key Features**:
- Single/batch stock price retrieval
- Market indices (S&P 500, NASDAQ, DOW, KOSPI)
- Symbol search

⚠️ **SRE Note**: Batch API prone to N+1 calls. Recommend caching for repeated symbols.

---

### 7️⃣ Portfolio Domain (`portfolio/`)
**Purpose**: User portfolio & holdings management  
**Controllers**: `PortfolioController`  
**Services**: `PortfolioService`  
**Repositories**: `PortfolioRepository`, `PortfolioHoldingRepository`  
**Key Features**:
- CRUD operations for portfolios
- CRUD operations for holdings
- Portfolio recalculation (current prices)
- ✅ **Phase 3**: EntityGraph로 N+1 problem 해결
- ✅ **Phase 4a**: Read/Write 분리 (Scheduler로 가격 업데이트)

✅ **SRE Note**: EntityGraph 적용 완료. 조회 시 DB만 사용 (응답시간 <100ms).

---

### 8️⃣ PriceHistory Domain (`pricehistory/`)
**Purpose**: Time-series price data storage & retrieval  
**Controllers**: `PriceHistoryController`  
**Services**: `PriceHistoryService`, `CryptoDataCollectorService`, `IndexDataCollectorService`  
**Repository**: `PriceHistoryRepository`  
**Key Features**:
- Chart data (time range queries)
- Latest price lookup (single/batch)
- Asset type filtering (STOCK, ETF, CRYPTO, INDEX)

⚠️ **SRE Note**: Chart queries may be slow without DB indexing on `(symbol, timestamp)`.

---

### 9️⃣ Rebalancing Domain (`rebalancing/`)
**Purpose**: Portfolio rebalancing recommendations  
**Controllers**: `RebalancingController`  
**Services**: `RebalancingService`  
**Key Features**:
- Strategy-based recommendations (EQUAL_WEIGHT, TARGET_ALLOCATION, etc.)
- Simulation (what-if analysis)

---

### 🔟 User Domain (`user/`)
**Purpose**: Authentication & user management  
**Controllers**: `AuthController`, `PerfTestAuthController`  
**Services**: `AuthService`, `JwtService`, `KakaoOAuthService`  
**Repositories**: `UserRepository`, `UserSessionRepository`  
**Key Features**:
- Kakao OAuth2 login
- JWT token generation & refresh
- Dev login (testing)
- Perf test token bulk generation (500 users)

⚠️ **SRE Note**: `PerfTestAuthController` only active in `perf` profile. Never use in production!

---

## 🔥 Hotspots (Performance-Critical Files)

| File | Lines | Concern | Recommendation |
|------|-------|---------|----------------|
| [`CryptoDataController`](file:///C:/Users/zetto/Desktop/Sentinel/backend/src/main/java/com/pjsent/sentinel/crypto/controller/CryptoDataController.java) | 294 | Batch API calls | ⚡ Use Caffeine cache (1m TTL) |
| [`CryptoStreamController`](file:///C:/Users/zetto/Desktop/Sentinel/backend/src/main/java/com/pjsent/sentinel/crypto/controller/CryptoStreamController.java) | 186 | SSE/WebSocket streaming | 🔍 Monitor memory under high concurrency |
| [`PortfolioController`](file:///C:/Users/zetto/Desktop/Sentinel/backend/src/main/java/com/pjsent/sentinel/portfolio/controller/PortfolioController.java) | 165 | `recalculatePortfolio` | 🔄 Move to async job (Virtual Thread) |
| [`MarketDataController`](file:///C:/Users/zetto/Desktop/Sentinel/backend/src/main/java/com/pjsent/sentinel/market/controller/MarketDataController.java) | 246 | AlphaVantage rate limits | ⏱️ Request throttling via Resilience4j |
| [`PerfTestAuthController`](file:///C:/Users/zetto/Desktop/Sentinel/backend/src/main/java/com/pjsent/sentinel/user/controller/PerfTestAuthController.java) | 159 | Bulk token generation | ⚠️ Profile-gated (safe) |

---

## 📊 Statistics

| Metric | Count |
|--------|-------|
| **Total Domains** | 11 |
| **Total Controllers** | 13 |
| **Total Services** | 17 |
| **Total Repositories** | 6 |
| **API Endpoints** | ~50+ |

---

## 🎯 Architecture Principles

1. ✅ **Layered DDD**: Clear separation (Controller → Service → Repository)
2. ✅ **Spring Boot 3 + Java 21**: Modern features (Records, Virtual Threads)
3. ✅ **Resilience4j**: Circuit Breaker for external APIs
4. ✅ **Caching**: Redis (distributed) + Caffeine (local)
5. ✅ **Reactive Streaming**: WebFlux for real-time crypto prices
6. ✅ **Security**: OAuth2 (Kakao) + JWT
7. ✅ **Observability**: Actuator + Prometheus

---

## 📝 Next Steps

### Completed ✅
- [x] ~~Add `@Cacheable` annotations to MarketDataService~~ (Phase 2)
- [x] ~~Implement `k6` stress tests for batch APIs~~ (Phase 1-3)
- [x] ~~Solve Portfolio N+1 problem~~ (Phase 3: EntityGraph)
- [x] ~~Separate Read/Write in PortfolioService~~ (Phase 4a: Scheduler)

### Planned 🚀
- [ ] Add database indexes for `PriceHistory(symbol, timestamp)`
- [ ] Create API rate limiting via Resilience4j
- [ ] Apply batch caching for Crypto/Market multi-symbol queries
- [ ] Async processing for `recalculatePortfolio` (Virtual Threads)
- [ ] Document database schema (ER diagram)
