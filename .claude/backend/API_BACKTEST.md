# Backtest API Specification

> **Phase 6**: 백테스팅 엔진 API 스펙
>
> **Last Updated**: 2025-10-18

---

## Overview

백테스팅 엔진은 과거 데이터를 기반으로 포트폴리오 전략을 시뮬레이션하고 성과를 평가합니다.

### 핵심 기능
1. 과거 주식 가격 데이터 조회 (AlphaVantage TIME_SERIES_DAILY)
2. 포트폴리오 가치 시뮬레이션
3. 리밸런싱 전략 적용 (없음, 월별, 분기별, 연별)
4. 성과 지표 계산 (Sharpe Ratio, Sortino Ratio, Max Drawdown, Total Return, CAGR)

---

## Endpoints

### 1. Run Backtest

포트폴리오에 대한 백테스팅을 실행합니다.

**Request**
```http
POST /api/v1/backtest/run
Content-Type: application/json
Authorization: Bearer {token}

{
  "portfolioId": 1,
  "startDate": "2020-01-01",
  "endDate": "2023-12-31",
  "initialCapital": 10000.00,
  "rebalanceFrequency": "QUARTERLY"
}
```

**Request Fields**
- `portfolioId` (Long, required): 백테스팅할 포트폴리오 ID
- `startDate` (String, required): 시작일 (YYYY-MM-DD)
- `endDate` (String, required): 종료일 (YYYY-MM-DD)
- `initialCapital` (Double, required): 초기 투자 금액 (USD)
- `rebalanceFrequency` (String, required): 리밸런싱 빈도
  - `NONE`: 리밸런싱 없음
  - `MONTHLY`: 월별
  - `QUARTERLY`: 분기별
  - `YEARLY`: 연별

**Response**
```json
{
  "portfolioId": 1,
  "portfolioName": "Tech Portfolio",
  "startDate": "2020-01-01",
  "endDate": "2023-12-31",
  "initialCapital": 10000.00,
  "finalValue": 15234.56,
  "rebalanceFrequency": "QUARTERLY",
  "performance": {
    "totalReturn": 52.35,
    "cagr": 11.23,
    "sharpeRatio": 1.45,
    "sortinoRatio": 1.87,
    "maxDrawdown": -15.67,
    "volatility": 18.45,
    "winRate": 62.5
  },
  "equityCurve": [
    {
      "date": "2020-01-01",
      "value": 10000.00,
      "dailyReturn": 0.00,
      "cumulativeReturn": 0.00
    },
    {
      "date": "2020-01-02",
      "value": 10123.45,
      "dailyReturn": 1.23,
      "cumulativeReturn": 1.23
    }
  ],
  "rebalanceEvents": [
    {
      "date": "2020-04-01",
      "reason": "QUARTERLY_REBALANCE",
      "trades": [
        {
          "symbol": "AAPL",
          "action": "BUY",
          "quantity": 5,
          "price": 150.25,
          "amount": 751.25
        }
      ]
    }
  ],
  "holdingsSummary": [
    {
      "symbol": "AAPL",
      "finalQuantity": 25,
      "finalValue": 3750.00,
      "finalWeight": 24.62,
      "totalReturn": 45.23
    }
  ],
  "executedAt": "2025-10-18T12:34:56"
}
```

**Response Fields**
- `portfolioId`: 포트폴리오 ID
- `portfolioName`: 포트폴리오 이름
- `startDate`: 백테스팅 시작일
- `endDate`: 백테스팅 종료일
- `initialCapital`: 초기 투자 금액
- `finalValue`: 최종 포트폴리오 가치
- `rebalanceFrequency`: 적용된 리밸런싱 빈도
- `performance`: 성과 지표
  - `totalReturn`: 총 수익률 (%)
  - `cagr`: 연평균 성장률 (%)
  - `sharpeRatio`: 샤프 비율 (위험 대비 수익률)
  - `sortinoRatio`: 소르티노 비율 (하방 위험 대비 수익률)
  - `maxDrawdown`: 최대 낙폭 (%)
  - `volatility`: 변동성 (표준편차, %)
  - `winRate`: 승률 (%)
- `equityCurve`: 포트폴리오 가치 추이 (일별)
  - `date`: 날짜
  - `value`: 포트폴리오 가치
  - `dailyReturn`: 일일 수익률 (%)
  - `cumulativeReturn`: 누적 수익률 (%)
- `rebalanceEvents`: 리밸런싱 이벤트 목록
  - `date`: 리밸런싱 날짜
  - `reason`: 리밸런싱 사유
  - `trades`: 거래 내역
- `holdingsSummary`: 종목별 최종 요약
  - `symbol`: 종목 심볼
  - `finalQuantity`: 최종 보유 수량
  - `finalValue`: 최종 가치
  - `finalWeight`: 최종 비중 (%)
  - `totalReturn`: 총 수익률 (%)
- `executedAt`: 백테스팅 실행 시각

**Error Responses**
```json
{
  "message": "Portfolio not found",
  "status": 404,
  "timestamp": "2025-10-18T12:34:56"
}
```

```json
{
  "message": "Invalid date range: start date must be before end date",
  "status": 400,
  "timestamp": "2025-10-18T12:34:56"
}
```

```json
{
  "message": "Historical data unavailable for symbol AAPL",
  "status": 503,
  "timestamp": "2025-10-18T12:34:56"
}
```

---

### 2. Get Historical Prices

특정 종목의 과거 가격 데이터를 조회합니다.

**Request**
```http
GET /api/v1/backtest/historical/{symbol}?startDate=2020-01-01&endDate=2023-12-31
Authorization: Bearer {token}
```

**Query Parameters**
- `startDate` (String, required): 시작일 (YYYY-MM-DD)
- `endDate` (String, required): 종료일 (YYYY-MM-DD)

**Response**
```json
{
  "symbol": "AAPL",
  "startDate": "2020-01-01",
  "endDate": "2023-12-31",
  "dataPoints": 1008,
  "prices": [
    {
      "date": "2020-01-01",
      "open": 150.25,
      "high": 152.50,
      "low": 149.80,
      "close": 151.75,
      "volume": 12345678,
      "adjustedClose": 151.75
    }
  ]
}
```

---

### 3. Validate Backtest Parameters

백테스팅 파라미터를 검증합니다.

**Request**
```http
POST /api/v1/backtest/validate
Content-Type: application/json
Authorization: Bearer {token}

{
  "portfolioId": 1,
  "startDate": "2020-01-01",
  "endDate": "2023-12-31",
  "initialCapital": 10000.00,
  "rebalanceFrequency": "QUARTERLY"
}
```

**Response**
```json
{
  "valid": true,
  "warnings": [
    "Holdings contain crypto assets which may have limited historical data"
  ],
  "dataAvailability": {
    "AAPL": true,
    "GOOGL": true,
    "BTC": false
  }
}
```

---

## Performance Metrics Formulas

### 1. Total Return
```
Total Return (%) = ((Final Value - Initial Capital) / Initial Capital) * 100
```

### 2. CAGR (Compound Annual Growth Rate)
```
CAGR (%) = (((Final Value / Initial Capital) ^ (1 / Years)) - 1) * 100
```

### 3. Sharpe Ratio
```
Sharpe Ratio = (Average Return - Risk Free Rate) / Standard Deviation of Returns
```
- Risk Free Rate: 2% (assumed)
- Higher is better (>1.0 is good, >2.0 is excellent)

### 4. Sortino Ratio
```
Sortino Ratio = (Average Return - Risk Free Rate) / Downside Deviation
```
- Downside Deviation: Standard deviation of negative returns only
- Higher is better

### 5. Max Drawdown
```
Max Drawdown (%) = ((Trough Value - Peak Value) / Peak Value) * 100
```
- Most negative value during the period
- Lower is better (less negative)

### 6. Volatility
```
Volatility (%) = Standard Deviation of Daily Returns * sqrt(252)
```
- Annualized volatility
- Lower is better (more stable)

### 7. Win Rate
```
Win Rate (%) = (Number of Positive Return Days / Total Trading Days) * 100
```

---

## Data Sources

### AlphaVantage TIME_SERIES_DAILY
```
https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=AAPL&outputsize=full&apikey=YOUR_KEY
```

**Response Example**
```json
{
  "Meta Data": {
    "1. Information": "Daily Prices (open, high, low, close) and Volumes",
    "2. Symbol": "AAPL",
    "3. Last Refreshed": "2023-12-31"
  },
  "Time Series (Daily)": {
    "2023-12-31": {
      "1. open": "150.25",
      "2. high": "152.50",
      "3. low": "149.80",
      "4. close": "151.75",
      "5. volume": "12345678"
    }
  }
}
```

**Rate Limits**
- Free tier: 5 API calls per minute, 100 per day
- Consider caching historical data

---

## Implementation Notes

### 1. Historical Data Caching
- Cache historical price data in database or Redis
- Update cache daily for recent data
- Reduces API calls and improves performance

### 2. Rebalancing Logic
```
1. Calculate target weights (e.g., equal weight)
2. Calculate current weights based on market values
3. For each holding:
   - If current weight > target + threshold: SELL
   - If current weight < target - threshold: BUY
4. Execute trades
```

### 3. Transaction Costs
- Consider adding transaction cost modeling (e.g., 0.1% per trade)
- Affects realistic performance expectations

### 4. Dividend Handling
- Use adjusted close prices to account for dividends
- AlphaVantage provides adjusted close in TIME_SERIES_DAILY_ADJUSTED

### 5. Crypto Support
- Phase 6 focuses on stock backtesting
- Crypto backtesting deferred to Phase 7

---

## Error Handling

### Common Errors
1. **Portfolio Not Found** (404)
2. **Invalid Date Range** (400)
   - Start date >= End date
   - Date range > 10 years
3. **Historical Data Unavailable** (503)
   - API rate limit exceeded
   - Symbol not found
4. **Insufficient Data Points** (400)
   - Date range too short (<30 days)
5. **Invalid Rebalance Frequency** (400)

---

## Testing Strategy

### Unit Tests
- PerformanceCalculator: Test each metric formula
- BacktestEngine: Test rebalancing logic
- HistoricalDataService: Mock API responses

### Integration Tests
- End-to-end backtest execution
- Verify equity curve calculations
- Validate performance metrics

### Performance Tests
- Large portfolios (>50 holdings)
- Long time ranges (5+ years)
- Frequent rebalancing (monthly)

---

**Last Updated**: 2025-10-18
**Status**: 📝 Specification Complete
**Next**: Backend Implementation
