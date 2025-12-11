# 📊 Market Data API Specification

## Base Information
- **Domain**: `/api/v1/market`
- **Authentication**: Optional (rate limit differs)
- **Rate Limit**:
  - Authenticated: 1000 requests/hour
  - Anonymous: 100 requests/hour

---

## Endpoints

### 1. Get Single Stock Price
**Endpoint**: `GET /api/v1/market/price/{symbol}`

**Parameters**:
- `symbol` (path): Stock ticker symbol (e.g., AAPL, TSLA)

**Response**: `200 OK`
```json
{
  "symbol": "AAPL",
  "price": 180.50,
  "change": 2.30,
  "changePercent": 1.29,
  "timestamp": "2025-10-01T14:30:00Z",
  "currency": "USD"
}
```

---

### 2. Get Multiple Stock Prices
**Endpoint**: `POST /api/v1/market/prices`

**Request**:
```json
{
  "symbols": ["AAPL", "TSLA", "GOOGL", "MSFT"]
}
```

**Response**: `200 OK`
```json
{
  "prices": [
    {
      "symbol": "AAPL",
      "price": 180.50,
      "change": 2.30,
      "changePercent": 1.29,
      "timestamp": "2025-10-01T14:30:00Z"
    },
    {
      "symbol": "TSLA",
      "price": 250.75,
      "change": -5.20,
      "changePercent": -2.03,
      "timestamp": "2025-10-01T14:30:00Z"
    }
  ]
}
```

---

### 3. Get Market Indices
**Endpoint**: `GET /api/v1/market/indices`

**Response**: `200 OK`
```json
{
  "indices": [
    {
      "symbol": "^GSPC",
      "name": "S&P 500",
      "value": 4500.25,
      "change": 15.30,
      "changePercent": 0.34,
      "timestamp": "2025-10-01T14:30:00Z"
    },
    {
      "symbol": "^IXIC",
      "name": "NASDAQ",
      "value": 14200.50,
      "change": -22.15,
      "changePercent": -0.16,
      "timestamp": "2025-10-01T14:30:00Z"
    }
  ]
}
```

---

### 4. Search Assets
**Endpoint**: `GET /api/v1/market/search`

**Query Parameters**:
- `q` (required): Search query
- `type` (optional): Asset type (stock, crypto, commodity, etf)
- `limit` (optional): Results limit (default: 10, max: 50)

**Example**: `GET /api/v1/market/search?q=apple&type=stock&limit=5`

**Response**: `200 OK`
```json
{
  "results": [
    {
      "symbol": "AAPL",
      "name": "Apple Inc.",
      "type": "stock",
      "exchange": "NASDAQ",
      "currency": "USD"
    },
    {
      "symbol": "AAPL.L",
      "name": "Apple Inc. (London)",
      "type": "stock",
      "exchange": "LSE",
      "currency": "GBP"
    }
  ]
}
```

---

### 5. Get Fear & Greed Index
**Endpoint**: `GET /api/v1/market/fear-greed`

**Response**: `200 OK`
```json
{
  "value": 65,
  "sentiment": "Greed",
  "timestamp": "2025-10-01T00:00:00Z",
  "components": {
    "momentum": 70,
    "volume": 60,
    "volatility": 55,
    "marketBreadth": 65,
    "putCallRatio": 70
  }
}
```

---

### 6. Get Trending Assets
**Endpoint**: `GET /api/v1/market/trending`

**Query Parameters**:
- `type` (optional): Asset type filter
- `limit` (optional): Results limit (default: 10)

**Response**: `200 OK`
```json
{
  "trending": [
    {
      "symbol": "TSLA",
      "name": "Tesla Inc.",
      "price": 250.75,
      "changePercent": 5.20,
      "volume": 45000000,
      "trendScore": 95
    }
  ]
}
```

---

## Data Providers

### Provider Priority
1. **AlphaVantage** (Primary)
   - Rate: 5 calls/minute
   - Use: Stock prices, historical data

2. **Finnhub** (Fallback)
   - Rate: 60 calls/minute
   - Use: Real-time quotes, search

3. **Yahoo Finance** (Backup)
   - Rate: Unlimited
   - Use: Emergency fallback

### Circuit Breaker
- Automatic provider switching on failure
- 15-minute cache TTL
- Async processing with thread pools

---

## Implementation Status

### ✅ Implemented
- Single/batch price fetching
- Provider failover system
- Caching layer (Redis)
- Rate limiting

### 🚧 Pending
- Market indices endpoint
- Fear & Greed Index integration
- Asset search functionality
- Trending assets algorithm
- Historical data API
- Real-time WebSocket updates

---

## Backend Implementation

**Controller**: `backend/src/main/java/com/pjsent/sentinel/market/controller/MarketController.java`

**Service**: `backend/src/main/java/com/pjsent/sentinel/market/service/MarketDataService.java`

**Providers**:
- `backend/src/main/java/com/pjsent/sentinel/market/provider/AlphaVantageProvider.java`
- `backend/src/main/java/com/pjsent/sentinel/market/provider/FinnhubProvider.java`

**Factory**: `backend/src/main/java/com/pjsent/sentinel/market/factory/MarketDataProviderFactory.java`

---

**Last Updated**: 2025-10-01