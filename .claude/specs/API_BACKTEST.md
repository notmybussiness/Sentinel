# Backtest API Specification

> **Last Updated**: 2025-12-14
> **Status**: ✅ 구현 완료 (100%)

---

## Overview

백테스팅 엔진은 과거 데이터를 기반으로 포트폴리오 전략을 시뮬레이션하고 성과를 평가합니다.

### 핵심 기능
1. ✅ 과거 주식/암호화폐 가격 데이터 조회
2. ✅ 포트폴리오 가치 시뮬레이션
3. ✅ 리밸런싱 전략 적용 (없음, 월별, 분기별, 연별)
4. ✅ 성과 지표 계산 (Sharpe, Sortino, Max Drawdown, CAGR 등)
5. ✅ 거래 비용 모델링 (Transaction Cost)

### 데이터 소스 아키텍처
```
                    HistoricalDataFacade
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
    ┌─────────┐      ┌─────────┐      ┌─────────┐
    │  KIS    │      │ Alpha   │      │  Upbit  │
    │(한국주식)│      │Vantage  │      │(암호화폐)│
    │ 005930  │      │ AAPL    │      │  BTC    │
    │ 50req/s │      │ 5req/m  │      │600req/m │
    └─────────┘      └─────────┘      └─────────┘
```

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
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "initialCapital": 10000000,
  "rebalanceFrequency": "QUARTERLY",
  "transactionCostPercent": 0.001
}
```

**Request Fields**
| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `portfolioId` | Long | ✅ | - | 백테스팅할 포트폴리오 ID |
| `startDate` | String | ✅ | - | 시작일 (YYYY-MM-DD) |
| `endDate` | String | ✅ | - | 종료일 (YYYY-MM-DD) |
| `initialCapital` | Double | ✅ | - | 초기 투자 금액 |
| `rebalanceFrequency` | Enum | ✅ | - | NONE, MONTHLY, QUARTERLY, YEARLY |
| `transactionCostPercent` | Double | ❌ | 0.001 | 거래 비용 비율 (0.1% = 0.001) |

**Response**
```json
{
  "portfolioId": 1,
  "portfolioName": "삼성전자 포트폴리오",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "initialCapital": 10000000,
  "finalValue": 10523450,
  "rebalanceFrequency": "QUARTERLY",
  "transactionCostPercent": 0.001,
  "totalTransactionCosts": 12345.67,
  "costImpactPercent": 0.12,
  "performance": {
    "totalReturn": 5.23,
    "cagr": 5.23,
    "sharpeRatio": 0.85,
    "sortinoRatio": 1.12,
    "maxDrawdown": -8.45,
    "volatility": 15.67,
    "winRate": 52.3
  },
  "equityCurve": [...],
  "rebalanceEvents": [...],
  "holdingsSummary": [...],
  "executedAt": "2025-12-14T12:34:56"
}
```

---

### 2. Get Historical Prices

특정 종목의 과거 가격 데이터를 조회합니다.

**Request**
```http
GET /api/v1/backtest/historical/{symbol}?startDate=2024-01-01&endDate=2024-12-31
Authorization: Bearer {token}
```

**라우팅 규칙**
| 심볼 패턴 | Provider | 예시 |
|-----------|----------|------|
| 6자리 숫자 | KIS (한국투자증권) | 005930, 035720 |
| 영문 | AlphaVantage | AAPL, GOOGL |

**Response (삼성전자 005930 예시)**
```json
{
  "symbol": "005930",
  "startDate": "2024-11-01",
  "endDate": "2024-12-13",
  "dataPoints": 31,
  "prices": [
    {
      "date": "2024-11-01",
      "open": 59000,
      "high": 59600,
      "low": 58100,
      "close": 58300,
      "volume": 19083180,
      "adjustedClose": 58300
    },
    ...
  ]
}
```

---

### 3. Validate Backtest Parameters

백테스팅 실행 전 파라미터 유효성 및 데이터 가용성을 검증합니다.

**Request**
```http
POST /api/v1/backtest/validate
Content-Type: application/json
Authorization: Bearer {token}

{
  "portfolioId": 1,
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "initialCapital": 10000000,
  "rebalanceFrequency": "QUARTERLY"
}
```

**Response**
```json
{
  "valid": true,
  "errors": [],
  "warnings": [
    "Some holdings may have limited historical data"
  ],
  "dataAvailability": {
    "005930": true,
    "AAPL": true,
    "BTC": true
  }
}
```

---

### 4. Get Backtest Status

백테스팅 서비스 상태를 확인합니다.

**Request**
```http
GET /api/v1/backtest/status
```

**Response**
```json
{
  "available": true,
  "provider": "HistoricalDataFacade",
  "supportedFrequencies": ["NONE", "MONTHLY", "QUARTERLY", "YEARLY"],
  "maxDateRange": "10 years",
  "dataSources": {
    "koreanStock": "KIS (한국투자증권)",
    "usStock": "AlphaVantage",
    "crypto": "Upbit"
  }
}
```

---

## Data Sources

### 1. KIS (한국투자증권) - 한국 주식

```
API: FHKST03010100 (국내주식 기간별 시세)
Rate Limit: ~50 req/s
Cache TTL: 7일
```

**지원 종목**: 코스피/코스닥 전종목 (6자리 종목코드)
- 005930 (삼성전자)
- 035720 (카카오)
- 000660 (SK하이닉스)

### 2. AlphaVantage - 미국 주식

```
API: TIME_SERIES_DAILY
Rate Limit: 5 req/min, 100 req/day
Cache TTL: 7일
```

**지원 종목**: NYSE/NASDAQ 전종목
- AAPL, GOOGL, MSFT, TSLA

### 3. Upbit - 암호화폐

```
API: /candles/days
Rate Limit: 600 req/min
Cache TTL: 7일
```

**지원 종목**: Upbit 상장 암호화폐
- BTC, ETH, XRP, SOL

---

## Performance Metrics

| 지표 | 공식 | 설명 |
|------|------|------|
| **Total Return** | `(Final - Initial) / Initial × 100` | 총 수익률 (%) |
| **CAGR** | `((Final/Initial)^(1/Years) - 1) × 100` | 연평균 성장률 |
| **Sharpe Ratio** | `(Return - RiskFree) / StdDev` | 위험 대비 수익률 |
| **Sortino Ratio** | `(Return - RiskFree) / DownsideDev` | 하방 위험 대비 수익률 |
| **Max Drawdown** | `(Trough - Peak) / Peak × 100` | 최대 낙폭 |
| **Volatility** | `StdDev × sqrt(252)` | 연환산 변동성 |
| **Win Rate** | `Positive Days / Total Days × 100` | 승률 |

---

## Transaction Cost Model

### 거래 비용 계산
```java
// 각 거래(BUY/SELL)마다 비용 적용
double cost = tradeAmount × transactionCostPercent;
portfolioValue -= cost;
```

### 비용 영향 보고
```json
{
  "totalTransactionCosts": 45678.90,
  "costImpactPercent": 0.46
}
```

---

## Implementation Status

### ✅ 완료 (100%)
- [x] POST /run - 백테스팅 실행
- [x] GET /historical/{symbol} - 과거 데이터 조회
- [x] POST /validate - 파라미터 검증
- [x] GET /status - 서비스 상태

### 데이터 소스
- [x] KIS 한국 주식 (KisHistoricalDataService)
- [x] AlphaVantage 미국 주식 (HistoricalDataService)
- [x] Upbit 암호화폐 (CryptoHistoricalDataService)

### 기능
- [x] Stock + Crypto 혼합 백테스팅
- [x] Transaction Cost 모델링
- [x] Circuit Breaker (kisApi, alphaVantageApi, upbitApi)
- [x] Redis 캐시 (7일 TTL)

---

## Backend Implementation

**Controller**
```
backtest/controller/BacktestController.java
```

**Services**
```
backtest/service/BacktestEngine.java
backtest/service/HistoricalDataFacade.java      ← Routing
backtest/service/KisHistoricalDataService.java  ← Korean Stock
backtest/service/HistoricalDataService.java     ← US Stock
backtest/service/CryptoHistoricalDataService.java ← Crypto
backtest/service/PerformanceCalculator.java
```

**DTOs**
```
backtest/dto/BacktestRequest.java
backtest/dto/BacktestResponse.java
backtest/dto/BacktestValidationResponse.java
backtest/dto/HistoricalPriceData.java
backtest/dto/PerformanceMetrics.java
```

---

## Cache Configuration

| Cache Name | TTL | 용도 |
|------------|-----|------|
| `kisHistoricalData` | 7일 | KIS 한국 주식 일봉 |
| `historicalData` | 7일 | AlphaVantage 미국 주식 |
| `cryptoHistoricalData` | 7일 | Upbit 암호화폐 일봉 |

---

## Error Handling

| Status | Error | Description |
|--------|-------|-------------|
| 400 | `INVALID_DATE_RANGE` | 시작일 >= 종료일 |
| 400 | `RANGE_TOO_LONG` | 기간 > 10년 |
| 404 | `PORTFOLIO_NOT_FOUND` | 포트폴리오 없음 |
| 503 | `DATA_UNAVAILABLE` | API 장애 |

---

**Last Updated**: 2025-12-14
**Maintainer**: Claude Code
