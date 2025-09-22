# Market Data Domain API Reference

## Core APIs

### GET /api/v1/market/price/{symbol}
**Purpose**: 단일 주식 가격 조회  
**When**: 포트폴리오 재계산, 개별 주식 조회 시  
**Example**: `/api/v1/market/price/AAPL`  
**Response**: StockPriceDto

### GET /api/v1/market/prices?symbols=AAPL,MSFT,GOOGL
**Purpose**: 다중 주식 가격 조회  
**When**: 포트폴리오 전체 재계산 시  
**Response**: StockPriceDto 배열

### GET /api/v1/market/status
**Purpose**: 서비스 상태 확인  
**When**: 헬스체크, 디버깅 시  
**Response**: Provider 상태 정보

## Data Structure
```java
StockPriceDto {
  symbol,           // "AAPL"
  price,           // 229.72
  change,          // 1.22
  changePercent,   // 0.53
  lastTradingDay,  // "2024-01-15"
  timeStamp,       // "2024-01-15T23:37:33"
  provider         // "AlphaVantage"
}
```

## Key Features
- **Fallback Strategy**: AlphaVantage → Finnhub 자동 전환
- **Rate Limits**: AlphaVantage 5calls/min, Finnhub 60calls/min
- **Error Handling**: Provider 실패 시 자동 다음 Provider 시도