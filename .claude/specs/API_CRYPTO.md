# 🪙 Crypto API Specification

## Base Information
- **Domain**: `/api/v1/crypto`
- **Authentication**: Optional (rate limit differs)
- **Rate Limit**:
  - Authenticated: 1000 requests/hour
  - Anonymous: 100 requests/hour
- **Primary Provider**: Upbit (No API Key Required)
- **Fallback Provider**: Binance (No API Key Required)

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
  "timestamp": "2025-10-17T14:30:00Z",
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
      "timestamp": "2025-10-17T14:30:00Z"
    },
    {
      "symbol": "ETH",
      "name": "Ethereum",
      "price": 3500000,
      "change": -50000,
      "changePercent": -1.41,
      "baseCurrency": "KRW",
      "timestamp": "2025-10-17T14:30:00Z"
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
      "timestamp": "2025-10-01T00:00:00Z",
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
  "timestamp": "2025-10-17T14:30:00Z"
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
data: {"symbol":"BTC","price":55000000,"changePercent":2.23,"timestamp":"2025-10-17T14:30:00Z"}

event: price-update
data: {"symbol":"ETH","price":3500000,"changePercent":1.85,"timestamp":"2025-10-17T14:30:01Z"}
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

### Provider Priority
1. **Upbit** (Primary - KRW Base)
   - No API Key Required ✅
   - Rate: 600 requests/minute (IP-based)
   - WebSocket: Unlimited connections
   - Use: KRW-based crypto prices, Korean market

2. **Binance** (Fallback - USD Base)
   - No API Key Required ✅
   - Rate: 1200 weight/minute
   - WebSocket: 300 connections/5min
   - Use: USD-based crypto prices, global market

### Provider Factory Pattern
```java
CryptoDataProvider provider = cryptoProviderFactory.getProvider(baseCurrency);
// baseCurrency == "KRW" → UpbitProvider
// baseCurrency == "USD" → BinanceProvider
```

---

## Implementation Status

### ✅ Implemented (Phase 4)
- Week 1: Backend REST API
  - Single/batch price fetching
  - Search functionality
  - Trending cryptocurrencies
  - Historical data endpoint
  - Service status endpoint

- Week 2: Adapter Pattern Streaming
  - SSEStreamingService (권장, 1초 간격)
  - WebSocketStreamingService (고성능, 500ms)
  - LongPollingStreamingService (Fallback, 2초)
  - Auto-fallback (SSE → LongPolling)
  - Performance comparison dashboard

- Week 3: Portfolio Integration
  - Portfolio Crypto Holdings
  - AddCryptoHoldingModal
  - Asset type discrimination (STOCK/CRYPTO)
  - Multi-currency support (KRW/USD)

### 🔄 In Progress (Phase 4 Week 4)
- Manual testing
- E2E test automation
- API documentation finalization

---

## Backend Implementation

**Controller**:
- `backend/src/main/java/com/pjsent/sentinel/crypto/controller/CryptoDataController.java`
- `backend/src/main/java/com/pjsent/sentinel/crypto/controller/CryptoStreamController.java`

**Service**:
- `backend/src/main/java/com/pjsent/sentinel/crypto/service/CryptoDataService.java`
- `backend/src/main/java/com/pjsent/sentinel/crypto/streaming/SSEStreamingService.java`
- `backend/src/main/java/com/pjsent/sentinel/crypto/streaming/WebSocketStreamingService.java`
- `backend/src/main/java/com/pjsent/sentinel/crypto/streaming/LongPollingStreamingService.java`

**Providers**:
- `backend/src/main/java/com/pjsent/sentinel/crypto/service/provider/UpbitProvider.java`
- `backend/src/main/java/com/pjsent/sentinel/crypto/service/provider/BinanceProvider.java` (준비 완료)

**Factory**:
- `backend/src/main/java/com/pjsent/sentinel/crypto/service/factory/CryptoProviderFactory.java`

**DTOs**:
- `backend/src/main/java/com/pjsent/sentinel/crypto/dto/CryptoPriceDto.java`
- `backend/src/main/java/com/pjsent/sentinel/crypto/dto/CryptoSearchResultDto.java`
- `backend/src/main/java/com/pjsent/sentinel/crypto/dto/HistoricalDataDto.java`

---

## Frontend Integration

**API Client**: `frontend/lib/api/crypto.ts`
- `getCryptoPrice(symbol, baseCurrency)`
- `getBatchCryptoPrices(symbols, baseCurrency)`
- `searchCrypto(query, limit)`
- `getTrendingCryptos(baseCurrency, limit)`
- `getCryptoHistorical(symbol, baseCurrency, interval, count)`
- `getCryptoStatus()`

**Streaming Adapters**: `frontend/lib/streaming/`
- `SSEAdapter.ts` - EventSource 기반 (자동 재연결)
- `LongPollingAdapter.ts` - setInterval 기반
- `StreamingAdapter.ts` - 인터페이스

**Hooks**: `frontend/lib/hooks/`
- `useStreamingPrices.ts` - Auto-fallback 지원 (SSE → LongPolling)

**Components**:
- `frontend/components/crypto/TrendingCryptoCard.tsx`
- `frontend/components/portfolio/AddCryptoHoldingModal.tsx`

**Test Pages**:
- `frontend/app/test-streaming/page.tsx` - 실시간 성능 비교 대시보드

---

## Configuration

**Backend (`application.yml`)**:
```yaml
crypto:
  market:
    upbit:
      enabled: true
      base-url: https://api.upbit.com/v1
      websocket-url: wss://api.upbit.com/websocket/v1
      timeout: 10000

    binance:
      enabled: true
      base-url: https://api.binance.com/api/v3
      timeout: 10000

  streaming:
    websocket:
      enabled: false  # Phase 4 Week 2에서 활성화
```

**No API Keys Required!** ✅
- Upbit: Public API only
- Binance: Public API only

---

## Error Handling

### Common Errors

**400 Bad Request**:
```json
{
  "error": "INVALID_SYMBOL",
  "message": "Invalid cryptocurrency symbol: INVALID"
}
```

**404 Not Found**:
```json
{
  "error": "CRYPTO_NOT_FOUND",
  "message": "Cryptocurrency not found: XYZ"
}
```

**500 Internal Server Error**:
```json
{
  "error": "PROVIDER_ERROR",
  "message": "All crypto data providers are unavailable"
}
```

**503 Service Unavailable**:
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded, try again later"
}
```

---

## Testing

### Manual Testing
1. **Price Lookup**: http://localhost:8080/api/v1/crypto/price/BTC?baseCurrency=KRW
2. **Search**: http://localhost:8080/api/v1/crypto/search?query=bitcoin
3. **Trending**: http://localhost:8080/api/v1/crypto/trending?limit=5
4. **Status**: http://localhost:8080/api/v1/crypto/status
5. **Streaming**: http://localhost:3000/test-streaming

### E2E Tests (Playwright)
- `frontend/e2e/crypto-holdings.spec.ts` (8 tests)
- Portfolio crypto addition flow
- Search functionality
- Currency selection (KRW/USD)

---

**Last Updated**: 2025-10-17
**Status**: Phase 4 - 90% Complete (Week 3 완료)
**Next**: Week 4 - Testing & Documentation
