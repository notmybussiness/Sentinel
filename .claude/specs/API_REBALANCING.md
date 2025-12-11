# API_REBALANCING.md - Rebalancing API Specification

> **Phase 7**: 리밸런싱 알고리즘
> **Last Updated**: 2025-10-18
> **Status**: 🔄 In Progress

---

## Overview

포트폴리오 리밸런싱 추천 및 시뮬레이션 API

**핵심 기능**:
- 리밸런싱 추천 생성 (BUY/SELL/HOLD)
- 3가지 리밸런싱 전략 지원
- 세금 효율성 고려
- 거래 비용 계산

---

## API Endpoints

### 1. 리밸런싱 추천 생성

**Endpoint**: `POST /api/v1/rebalancing/recommend`

**Description**: 포트폴리오의 현재 상태를 분석하여 리밸런싱 추천을 생성합니다.

**Request**:
```json
{
  "portfolioId": 1,
  "strategy": "EQUAL_WEIGHT",
  "thresholdPercent": 5.0,
  "considerTaxes": true
}
```

**Request Fields**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| portfolioId | Long | Yes | 포트폴리오 ID |
| strategy | String | Yes | 리밸런싱 전략 (EQUAL_WEIGHT, TARGET_ALLOCATION, RISK_PARITY) |
| thresholdPercent | Double | No | 리밸런싱 임계값 (%) - 기본값: 5.0 |
| considerTaxes | Boolean | No | 세금 효율성 고려 여부 - 기본값: false |

**Response**:
```json
{
  "portfolioId": 1,
  "portfolioName": "My Tech Portfolio",
  "strategy": "EQUAL_WEIGHT",
  "currentValue": 105000.0,
  "needsRebalancing": true,
  "recommendations": [
    {
      "symbol": "AAPL",
      "currentWeight": 35.5,
      "targetWeight": 25.0,
      "currentShares": 50,
      "targetShares": 35,
      "action": "SELL",
      "quantity": 15,
      "estimatedAmount": 2550.0
    },
    {
      "symbol": "GOOGL",
      "currentWeight": 20.2,
      "targetWeight": 25.0,
      "currentShares": 15,
      "targetShares": 19,
      "action": "BUY",
      "quantity": 4,
      "estimatedAmount": 560.0
    },
    {
      "symbol": "MSFT",
      "currentWeight": 24.8,
      "targetWeight": 25.0,
      "currentShares": 18,
      "targetShares": 18,
      "action": "HOLD",
      "quantity": 0,
      "estimatedAmount": 0.0
    }
  ],
  "totalTransactionCost": 31.10,
  "estimatedTaxImpact": 150.0,
  "analyzedAt": "2025-10-18T14:30:00"
}
```

**Response Fields**:
| Field | Type | Description |
|-------|------|-------------|
| portfolioId | Long | 포트폴리오 ID |
| portfolioName | String | 포트폴리오 이름 |
| strategy | String | 사용된 리밸런싱 전략 |
| currentValue | Double | 현재 포트폴리오 총 가치 (USD) |
| needsRebalancing | Boolean | 리밸런싱 필요 여부 |
| recommendations | Array | 종목별 리밸런싱 추천 |
| totalTransactionCost | Double | 총 거래 비용 (USD) |
| estimatedTaxImpact | Double | 예상 세금 영향 (USD) |
| analyzedAt | String | 분석 시각 (ISO 8601) |

**Recommendation Object**:
| Field | Type | Description |
|-------|------|-------------|
| symbol | String | 종목 심볼 |
| currentWeight | Double | 현재 비중 (%) |
| targetWeight | Double | 목표 비중 (%) |
| currentShares | Double | 현재 보유 주식 수 |
| targetShares | Double | 목표 주식 수 |
| action | String | 추천 액션 (BUY, SELL, HOLD) |
| quantity | Double | 거래 수량 |
| estimatedAmount | Double | 예상 거래 금액 (USD) |

**Error Responses**:
- `400 Bad Request`: Invalid request parameters
- `404 Not Found`: Portfolio not found
- `500 Internal Server Error`: Service error

---

### 2. 리밸런싱 시뮬레이션

**Endpoint**: `POST /api/v1/rebalancing/simulate`

**Description**: 리밸런싱 실행 후의 포트폴리오 상태를 시뮬레이션합니다.

**Request**:
```json
{
  "portfolioId": 1,
  "strategy": "EQUAL_WEIGHT",
  "thresholdPercent": 5.0
}
```

**Response**:
```json
{
  "portfolioId": 1,
  "portfolioName": "My Tech Portfolio",
  "beforeRebalancing": {
    "totalValue": 105000.0,
    "holdings": [
      {
        "symbol": "AAPL",
        "shares": 50,
        "value": 37275.0,
        "weight": 35.5
      }
    ]
  },
  "afterRebalancing": {
    "totalValue": 104968.90,
    "holdings": [
      {
        "symbol": "AAPL",
        "shares": 35,
        "value": 26192.5,
        "weight": 25.0
      }
    ]
  },
  "transactions": [
    {
      "symbol": "AAPL",
      "action": "SELL",
      "quantity": 15,
      "price": 170.0,
      "amount": 2550.0,
      "commission": 10.0
    }
  ],
  "totalTransactionCost": 31.10,
  "netChange": -31.10
}
```

---

### 3. 지원하는 전략 목록

**Endpoint**: `GET /api/v1/rebalancing/strategies`

**Description**: 지원하는 리밸런싱 전략 목록을 반환합니다.

**Response**:
```json
{
  "strategies": [
    {
      "name": "EQUAL_WEIGHT",
      "displayName": "균등 비중",
      "description": "모든 종목을 동일한 비중으로 유지합니다",
      "supported": true
    },
    {
      "name": "TARGET_ALLOCATION",
      "displayName": "목표 비중",
      "description": "사용자 지정 목표 비중에 맞춰 조정합니다",
      "supported": false
    },
    {
      "name": "RISK_PARITY",
      "displayName": "위험 균등",
      "description": "각 자산의 위험 기여도를 동일하게 유지합니다",
      "supported": false
    }
  ]
}
```

---

## Business Logic

### 리밸런싱 필요 여부 판단

```
for each holding:
  weightDiff = abs(currentWeight - targetWeight)
  if weightDiff > thresholdPercent:
    needsRebalancing = true
```

### 목표 주식 수 계산

```
targetShares = (portfolioValue * targetWeight / 100) / currentPrice
```

### 거래 수량 계산

```
quantity = abs(targetShares - currentShares)
action = if targetShares > currentShares then BUY
         else if targetShares < currentShares then SELL
         else HOLD
```

### 거래 비용 계산

```
commission = 0.1% of transaction amount (min $1, max $10)
totalTransactionCost = sum of all commissions
```

### 세금 효율성 고려

**Tax Loss Harvesting**:
- 손실이 있는 종목을 우선적으로 매도
- 단기 자본 이득세 회피 (보유 기간 < 1년)
- 30일 규칙 (Wash Sale Rule) 준수

```java
if (considerTaxes && holding.hasUnrealizedLoss()) {
    // 손실 종목 우선 매도
    prioritize(holding);
}
```

---

## Data Models

### RebalancingRequest
```java
@Data
@Builder
public class RebalancingRequest {
    @NotNull
    private Long portfolioId;

    @NotNull
    private RebalancingStrategy strategy;

    @Min(0)
    @Max(100)
    private Double thresholdPercent = 5.0;

    private Boolean considerTaxes = false;
}
```

### RebalancingResponse
```java
@Data
@Builder
public class RebalancingResponse {
    private Long portfolioId;
    private String portfolioName;
    private String strategy;
    private Double currentValue;
    private Boolean needsRebalancing;
    private List<RebalancingRecommendation> recommendations;
    private Double totalTransactionCost;
    private Double estimatedTaxImpact;
    private LocalDateTime analyzedAt;
}
```

### RebalancingRecommendation
```java
@Data
@Builder
public class RebalancingRecommendation {
    private String symbol;
    private Double currentWeight;
    private Double targetWeight;
    private Double currentShares;
    private Double targetShares;
    private String action; // BUY, SELL, HOLD
    private Double quantity;
    private Double estimatedAmount;
}
```

### RebalancingStrategy (Enum)
```java
public enum RebalancingStrategy {
    EQUAL_WEIGHT("균등 비중"),
    TARGET_ALLOCATION("목표 비중"),
    RISK_PARITY("위험 균등");

    private final String displayName;
}
```

---

## Implementation Notes

### Phase 7.1: Equal Weight Strategy (Week 1)
- ✅ Equal weight calculation
- ✅ BUY/SELL/HOLD recommendations
- ✅ Transaction cost calculation

### Phase 7.2: Tax Efficiency (Week 1)
- ✅ Tax loss harvesting
- ✅ Short-term vs long-term gains
- ✅ Wash sale rule checking

### Phase 7.3: Advanced Strategies (Future)
- ⏳ Target allocation (user-defined weights)
- ⏳ Risk parity (equal risk contribution)
- ⏳ Momentum-based rebalancing

---

## Error Handling

**Validation Errors**:
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid threshold percent: must be between 0 and 100",
  "field": "thresholdPercent",
  "timestamp": "2025-10-18T14:30:00"
}
```

**Business Logic Errors**:
```json
{
  "error": "INSUFFICIENT_DATA",
  "message": "Portfolio must have at least 2 holdings for rebalancing",
  "timestamp": "2025-10-18T14:30:00"
}
```

---

## Rate Limiting

- **Recommend**: 10 requests per minute per user
- **Simulate**: 5 requests per minute per user
- **Strategies**: Unlimited

---

## Testing Scenarios

### Test Case 1: Equal Weight Rebalancing
**Given**: Portfolio with 3 holdings (AAPL 40%, GOOGL 35%, MSFT 25%)
**When**: Request equal weight rebalancing
**Then**: Recommendations should adjust to 33.33% each

### Test Case 2: Below Threshold
**Given**: Portfolio with balanced weights (33%, 33%, 34%)
**When**: Request rebalancing with 5% threshold
**Then**: needsRebalancing = false, no recommendations

### Test Case 3: Tax Loss Harvesting
**Given**: Portfolio with 1 profitable and 1 losing position
**When**: Request rebalancing with considerTaxes = true
**Then**: Prioritize selling losing position

---

**Phase**: 7 - Rebalancing Algorithm
**Priority**: High
**Estimated Effort**: 1 week
