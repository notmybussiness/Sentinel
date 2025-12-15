# Market Data API Specification

> **Last Updated**: 2025-12-14
> **Status**: ✅ 구현 완료 (90%)

---

## Base Information
- **Domain**: `/api/v1/market`
- **Authentication**: Optional (rate limit differs)
- **Primary Provider**: KIS (한국투자증권) - 한국 주식
- **Fallback Providers**: AlphaVantage, Finnhub, Yahoo Finance

---

## Provider Architecture

```
                    MarketDataProviderFactory
                           │
    ┌──────────────────────┼──────────────────────┐
    │                      │                      │
    ▼                      ▼                      ▼
┌─────────┐          ┌─────────┐          ┌─────────┐
│  KIS    │          │ Alpha   │          │Finnhub  │
│ Order:1 │          │Vantage  │          │ Order:3 │
│ 50req/s │          │ Order:2 │          │60req/m  │
│         │          │ 5req/m  │          │         │
└─────────┘          └─────────┘          └─────────┘
     │                    │                    │
     └────────────────────┼────────────────────┘
                          │
                   Circuit Breaker
                   (Auto Failover)
```

---

## Endpoints

### 1. Get Single Stock Price
**Endpoint**: `GET /api/v1/market/price/{symbol}`

**Parameters**:
- `symbol` (path): Stock ticker symbol

**Example**:
- Korean: `GET /api/v1/market/price/005930` (삼성전자)
- US: `GET /api/v1/market/price/AAPL`

**Response**: `200 OK`
```json
{
  "symbol": "005930",
  "price": 56100,
  "open": 55800,
  "high": 56300,
  "low": 55500,
  "close": 55900,
  "change": 200,
  "changePercent": 0.36,
  "lastTradingDay": "2024-12-13",
  "timestamp": "2025-12-14T12:30:00",
  "provider": "KoreaInvestment"
}
```

---

### 2. Get Multiple Stock Prices
**Endpoint**: `POST /api/v1/market/prices`

**Request**:
```json
{
  "symbols": ["005930", "035720", "AAPL", "GOOGL"]
}
```

**Response**: `200 OK`
```json
{
  "prices": [
    {
      "symbol": "005930",
      "price": 56100,
      "changePercent": 0.36,
      "provider": "KoreaInvestment"
    },
    {
      "symbol": "AAPL",
      "price": 180.50,
      "changePercent": 1.29,
      "provider": "AlphaVantage"
    }
  ]
}
```

---

### 3. Search Stocks
**Endpoint**: `GET /api/v1/market/search`

**Query Parameters**:
- `q` (required): Search query
- `limit` (optional): Results limit (default: 10)

**Example**: `GET /api/v1/market/search?q=삼성&limit=5`

**Response**: `200 OK`
```json
{
  "results": [
    {
      "symbol": "005930",
      "name": "삼성전자",
      "region": "KR",
      "type": "300"
    },
    {
      "symbol": "005935",
      "name": "삼성전자우",
      "region": "KR",
      "type": "300"
    }
  ]
}
```

---

### 4. Get Market Indices
**Endpoint**: `GET /api/v1/market/indices`

**Response**: `200 OK`
```json
{
  "indices": [
    {
      "symbol": "KOSPI",
      "name": "코스피",
      "value": 2450.25,
      "change": 15.30,
      "changePercent": 0.63
    },
    {
      "symbol": "KOSDAQ",
      "name": "코스닥",
      "value": 750.50,
      "change": -5.15,
      "changePercent": -0.68
    }
  ]
}
```

---

## Data Providers

### 1. KIS 한국투자증권 (Primary - Order: 1)

```yaml
Provider: KoreaInvestmentProvider
API: Open API
Rate Limit: ~50 req/s
Supported: 한국 주식 (코스피/코스닥)

Features:
  - OAuth 2.0 토큰 자동 갱신
  - 실시간 시세 조회
  - 종목 검색
  - Circuit Breaker: kisApi
```

**Configuration**:
```yaml
stock:
  market:
    korea-investment:
      enabled: true
      base-url: https://openapi.koreainvestment.com:9443
      app-key: ${KIS_APP_KEY}
      app-secret: ${KIS_APP_SECRET}
```

### 2. AlphaVantage (Fallback - Order: 2)

```yaml
Provider: AlphaVantageProvider
API: Global Quote
Rate Limit: 5 req/min, 100 req/day
Supported: 미국 주식 (NYSE, NASDAQ)

Features:
  - Time Series Data
  - Historical Data
  - Symbol Search
  - Circuit Breaker: alphaVantageApi
```

### 3. Finnhub (Fallback - Order: 3)

```yaml
Provider: FinnhubProvider
API: REST API
Rate Limit: 60 req/min
Supported: 미국/유럽 주식

Features:
  - Real-time Quotes
  - Company Profiles
```

### 4. Yahoo Finance (Emergency - Order: 4)

```yaml
Provider: YahooFinanceProvider
Rate Limit: Unlimited (비공식)
Supported: 글로벌 주식

Note: Emergency fallback only
```

---

## Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      kisApi:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 15s

      alphaVantageApi:
        slidingWindowSize: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
```

---

## Cache Configuration

| Cache Name | TTL | 용도 |
|------------|-----|------|
| `stockPrice` | 30초 | 실시간 주가 |
| `stockSearch` | 3분 | 종목 검색 결과 |
| `marketIndices` | 15분 | 시장 지수 |

---

## Implementation Status

### ✅ 완료
- [x] Single/Batch price fetching
- [x] KIS Provider (한국 주식)
- [x] AlphaVantage Provider (미국 주식)
- [x] Finnhub Provider
- [x] Provider Failover (Circuit Breaker)
- [x] Redis Caching
- [x] Stock Search

### 🚧 Pending
- [ ] Market indices endpoint 개선
- [ ] Real-time WebSocket updates
- [ ] Fear & Greed Index

---

## Backend Implementation

**Controller**
```
market/controller/MarketController.java
```

**Service**
```
market/service/MarketDataService.java
```

**Providers**
```
market/service/provider/KoreaInvestmentProvider.java  ← Primary
market/service/provider/AlphaVantageProvider.java
market/service/provider/FinnhubProvider.java
market/service/provider/YahooFinanceProvider.java
```

**Factory**
```
market/service/factory/MarketDataProviderFactory.java
```

---

## Error Handling

| Status | Error | Description |
|--------|-------|-------------|
| 400 | `INVALID_SYMBOL` | 잘못된 종목 코드 |
| 404 | `SYMBOL_NOT_FOUND` | 종목 없음 |
| 503 | `ALL_PROVIDERS_DOWN` | 모든 Provider 장애 |
| 429 | `RATE_LIMIT_EXCEEDED` | Rate limit 초과 |

---

**Last Updated**: 2025-12-14
**Maintainer**: Claude Code
