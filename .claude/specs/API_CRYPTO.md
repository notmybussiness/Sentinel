# Crypto API Specification

> **Last Updated**: 2025-12-14
> **Status**: ✅ 구현 완료 (100%)

---

## Base Information
- **Domain**: `/api/v1/crypto`
- **Authentication**: Optional (rate limit differs)
- **Rate Limit**:
  - Authenticated: 1000 requests/hour
  - Anonymous: 100 requests/hour
- **Primary Provider**: Upbit (No API Key Required)
- **Fallback Provider**: Binance (No API Key Required)

---

## Provider Architecture

```
                    CryptoDataProviderFactory
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
    ┌─────────┐       ┌─────────┐      ┌─────────┐
    │  Upbit  │       │ Binance │      │ (Future)│
    │(Primary)│       │(Fallback│      │ Provider│
    │ KRW Base│       │USD Base)│      │         │
    │600req/m │       │1200/min │      │         │
    └─────────┘       └─────────┘      └─────────┘
         │                 │
         └────────┬────────┘
                  │
           Circuit Breaker
           (upbitApi, binanceApi)
```

---

## Endpoints

### 1. Get Single Crypto Price
**Endpoint**: `GET /api/v1/crypto/price/{symbol}`

**Parameters**:
- `symbol` (path): Crypto symbol (e.g., BTC, ETH, XRP)
- `baseCurrency` (query, optional): Base currency (KRW, USD) - Default: KRW

**Example**: `GET /api/v1/crypto/price/BTC?baseCurrency=KRW`

**Response**: `200 OK`
```json
{
  "symbol": "BTC",
  "name": "Bitcoin",
  "price": 55000000,
  "change": 1200000,
  "changePercent": 2.23,
  "volume24h": 850000000000,
  "marketCap": 1050000000000000,
  "high24h": 56000000,
  "low24h": 53500000,
  "baseCurrency": "KRW",
  "timestamp": "2025-12-14T14:30:00Z",
  "provider": "upbit"
}
```

---

### 2. Get Multiple Crypto Prices (Batch)
**Endpoint**: `POST /api/v1/crypto/prices`

**Request**:
```json
{
  "symbols": ["BTC", "ETH", "XRP"],
  "baseCurrency": "KRW"
}
```

**Response**: `200 OK`
```json
{
  "prices": [
    {
      "symbol": "BTC",
      "name": "Bitcoin",
      "price": 55000000,
      "change": 1200000,
      "changePercent": 2.23,
      "baseCurrency": "KRW",
      "timestamp": "2025-12-14T14:30:00Z"
    },
    {
      "symbol": "ETH",
      "name": "Ethereum",
      "price": 3500000,
      "change": -50000,
      "changePercent": -1.41,
      "baseCurrency": "KRW",
      "timestamp": "2025-12-14T14:30:00Z"
    }
  ]
}
```

---

### 3. Search Cryptocurrency
**Endpoint**: `GET /api/v1/crypto/search`

**Query Parameters**:
- `query` (required): Search term (e.g., "bitcoin", "BTC")
- `limit` (optional): Results limit (default: 10, max: 50)

**Example**: `GET /api/v1/crypto/search?query=bit&limit=5`

**Response**: `200 OK`
```json
{
  "results": [
    {
      "symbol": "BTC",
      "name": "Bitcoin",
      "baseCurrency": "KRW"
    },
    {
      "symbol": "BCH",
      "name": "Bitcoin Cash",
      "baseCurrency": "KRW"
    }
  ]
}
```

---

### 4. Get Trending Cryptocurrencies
**Endpoint**: `GET /api/v1/crypto/trending`

**Query Parameters**:
- `baseCurrency` (optional): Base currency (KRW, USD) - Default: KRW
- `limit` (optional): Results limit (default: 10, max: 50)

**Example**: `GET /api/v1/crypto/trending?baseCurrency=KRW&limit=5`

**Response**: `200 OK`
```json
{
  "trending": [
    {
      "symbol": "BTC",
      "name": "Bitcoin",
      "price": 55000000,
      "changePercent": 2.23,
      "volume24h": 850000000000,
      "baseCurrency": "KRW"
    },
    {
      "symbol": "ETH",
      "name": "Ethereum",
      "price": 3500000,
      "changePercent": 1.85,
      "volume24h": 320000000000,
      "baseCurrency": "KRW"
    }
  ]
}
```

---

### 5. Get Historical Data
**Endpoint**: `GET /api/v1/crypto/historical/{symbol}`

**Parameters**:
- `symbol` (path): Crypto symbol
- `baseCurrency` (query, optional): Base currency - Default: KRW
- `interval` (query, optional): Time interval (1m, 5m, 15m, 1h, 1d) - Default: 1d
- `count` (query, optional): Number of data points (max: 200) - Default: 30

**Example**: `GET /api/v1/crypto/historical/BTC?baseCurrency=KRW&interval=1d&count=30`

**Response**: `200 OK`
```json
{
  "symbol": "BTC",
  "baseCurrency": "KRW",
  "interval": "1d",
  "data": [
    {
      "timestamp": "2024-12-01T00:00:00Z",
      "open": 53000000,
      "high": 54500000,
      "low": 52800000,
      "close": 54200000,
      "volume": 800000000000
    }
  ]
}
```

---

### 6. Get Service Status
**Endpoint**: `GET /api/v1/crypto/status`

**Response**: `200 OK`
```json
{
  "status": "operational",
  "providers": {
    "upbit": {
      "status": "available",
      "responseTime": 45
    },
    "binance": {
      "status": "available",
      "responseTime": 60
    }
  },
  "timestamp": "2025-12-14T14:30:00Z"
}
```

---

### 7. Real-time Streaming (SSE/WebSocket/LongPolling)
**Endpoint**: `GET /api/v1/crypto/stream/prices`

**Query Parameters**:
- `symbols` (required): Comma-separated crypto symbols (e.g., "BTC,ETH,XRP")
- `method` (optional): Streaming method (SSE, WEBSOCKET, LONG_POLLING) - Default: SSE
- `baseCurrency` (optional): Base currency (KRW, USD) - Default: KRW

**Example**: `GET /api/v1/crypto/stream/prices?symbols=BTC,ETH&method=SSE`

**Response** (SSE - Server-Sent Events):
```
event: price-update
data: {"symbol":"BTC","price":55000000,"changePercent":2.23,"timestamp":"2025-12-14T14:30:00Z"}

event: price-update
data: {"symbol":"ETH","price":3500000,"changePercent":1.85,"timestamp":"2025-12-14T14:30:01Z"}
```

---

### 8. Get Available Streaming Methods
**Endpoint**: `GET /api/v1/crypto/stream/methods`

**Response**: `200 OK`
```json
{
  "methods": [
    {
      "name": "SSE",
      "description": "Server-Sent Events (권장)",
      "interval": "1초",
      "latency": "50ms"
    },
    {
      "name": "WEBSOCKET",
      "description": "WebSocket (고성능)",
      "interval": "500ms",
      "latency": "10ms"
    },
    {
      "name": "LONG_POLLING",
      "description": "Long Polling (Fallback)",
      "interval": "2초",
      "latency": "200ms"
    }
  ]
}
```

---

### 9. Get Streaming Status
**Endpoint**: `GET /api/v1/crypto/stream/status`

**Response**: `200 OK`
```json
{
  "activeConnections": 15,
  "supportedMethods": ["SSE", "WEBSOCKET", "LONG_POLLING"],
  "uptime": 3600,
  "status": "healthy"
}
```

---

## Data Providers

### 1. Upbit (Primary - Order: 1)

```yaml
Provider: UpbitProvider
API: Public API (No Key Required)
Rate Limit: 600 req/min (IP-based)
Supported: KRW 기준 암호화폐

Features:
  - 실시간 시세 조회
  - 일봉/분봉 데이터
  - 종목 검색
  - 트렌딩 코인
  - Circuit Breaker: upbitApi
```

**Configuration**:
```yaml
crypto:
  market:
    upbit:
      enabled: true
      base-url: https://api.upbit.com/v1
      websocket-url: wss://api.upbit.com/websocket/v1
      timeout: 10000
```

### 2. Binance (Fallback - Order: 2)

```yaml
Provider: BinanceProvider
API: Public API (No Key Required)
Rate Limit: 1200 weight/min
Supported: USD/USDT 기준 암호화폐

Features:
  - Global 마켓 데이터
  - Fallback Provider
  - Circuit Breaker: binanceApi
```

---

## Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      upbitApi:
        registerHealthIndicator: true
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 15s
        permittedNumberOfCallsInHalfOpenState: 3

      binanceApi:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

---

## Cache Configuration

| Cache Name | TTL | 용도 |
|------------|-----|------|
| `cryptoPrice` | 30초 | 실시간 시세 |
| `cryptoSearch` | 5분 | 종목 검색 결과 |
| `trendingCoins` | 5분 | 트렌딩 코인 |
| `cryptoHistoricalData` | 7일 | 백테스팅용 일봉 데이터 |

---

## Implementation Status

### ✅ 완료 (100%)
- [x] Single/batch price fetching
- [x] Upbit Provider (KRW 마켓)
- [x] Binance Provider (USD 마켓)
- [x] Search functionality
- [x] Trending cryptocurrencies
- [x] Historical data (일봉)
- [x] SSE Streaming Service
- [x] WebSocket Streaming Service
- [x] LongPolling Streaming Service
- [x] Provider Factory Pattern
- [x] Circuit Breaker (upbitApi)
- [x] Redis Caching
- [x] Backtesting Integration (CryptoHistoricalDataService)

### Streaming 성능
| Method | Interval | Latency | 용도 |
|--------|----------|---------|------|
| SSE | 1초 | 50ms | 권장 (자동 재연결) |
| WebSocket | 500ms | 10ms | 고성능 |
| Long Polling | 2초 | 200ms | Fallback |

---

## Backend Implementation

**Controller**
```
crypto/controller/CryptoDataController.java
crypto/controller/CryptoStreamController.java
```

**Service**
```
crypto/service/CryptoDataService.java
```

**Streaming**
```
crypto/streaming/SSEStreamingService.java
crypto/streaming/WebSocketStreamingService.java
crypto/streaming/LongPollingStreamingService.java
crypto/streaming/StreamingService.java (interface)
crypto/streaming/UpbitWebSocketClient.java
crypto/streaming/WebSocketMetrics.java
```

**Providers**
```
crypto/service/provider/UpbitProvider.java      ← Primary
crypto/service/provider/BinanceProvider.java    ← Fallback
crypto/service/provider/CryptoDataProvider.java ← Interface
```

**Factory**
```
crypto/service/factory/CryptoDataProviderFactory.java
```

**Backtesting Integration**
```
backtest/service/CryptoHistoricalDataService.java
```

**DTOs**
```
crypto/dto/CryptoPriceDto.java
crypto/dto/CryptoSearchResultDto.java
crypto/dto/TrendingCoinDto.java
```

---

## Backtesting Support

암호화폐 백테스팅을 위한 별도 서비스가 구현되어 있습니다.

### CryptoHistoricalDataService

```java
// 사용 예시
@CircuitBreaker(name = "upbitApi", fallbackMethod = "getHistoricalPricesFallback")
@Cacheable(value = "cryptoHistoricalData",
           key = "#symbol + '_' + #baseCurrency + '_' + #startDate + '_' + #endDate",
           sync = true)
public List<HistoricalPriceData> getHistoricalPrices(
        String symbol, String baseCurrency,
        LocalDate startDate, LocalDate endDate)
```

**특징**:
- Upbit Candles API 사용 (`/candles/days`)
- 7일 캐시 TTL
- Circuit Breaker fallback (캐시 데이터 반환)
- CryptoPriceDto → HistoricalPriceData 변환

**HistoricalDataFacade 라우팅**:
```
HistoricalDataFacade
├── 한국 주식 (6자리: 005930) → KisHistoricalDataService
├── 미국 주식 (AAPL, GOOGL)   → HistoricalDataService (AlphaVantage)
└── 암호화폐 (BTC, ETH)       → CryptoHistoricalDataService (Upbit)
```

---

## Error Handling

| Status | Error | Description |
|--------|-------|-------------|
| 400 | `INVALID_SYMBOL` | 잘못된 심볼 |
| 404 | `CRYPTO_NOT_FOUND` | 암호화폐 없음 |
| 500 | `PROVIDER_ERROR` | 모든 Provider 장애 |
| 503 | `RATE_LIMIT_EXCEEDED` | Rate limit 초과 |

---

**Last Updated**: 2025-12-14
**Maintainer**: Claude Code
