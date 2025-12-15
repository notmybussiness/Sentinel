# Portfolio API Specification

> **Last Updated**: 2025-12-14
> **Status**: ✅ 구현 완료 (100%)

---

## Base Information
- **Domain**: `/api/v1/portfolios`
- **Authentication**: Required (JWT Bearer Token)
- **Rate Limit**: 1000 requests/hour

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   PortfolioController                       │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                    PortfolioService                         │
│  - @Cacheable (Redis)                                       │
│  - @Transactional(readOnly=true) 기본                       │
└──────────┬─────────────────────────────┬────────────────────┘
           │                             │
┌──────────▼──────────┐      ┌───────────▼───────────┐
│PortfolioRepository  │      │ MarketDataService     │
│ @EntityGraph        │      │ CryptoDataService     │
│ (N+1 해결)          │      │ (가격 조회용)         │
└─────────────────────┘      └───────────────────────┘
           │
           ▼
   ┌───────────────────┐
   │  PostgreSQL       │
   └───────────────────┘
```

### Read/Write 분리 아키텍처 (Phase 4a)

```
[조회 요청]                      [가격 업데이트]
     │                                │
     ▼                                ▼
 GET /portfolios/{id}         PortfolioPriceScheduler
     │                          (5분마다 실행)
     ▼                                │
 Redis Cache ──miss──→ DB            │
     │                                │
     └────────────────────────────────┘
         (백그라운드에서 가격 갱신)
```

---

## Endpoints

### 1. List User Portfolios
**Endpoint**: `GET /api/v1/portfolios`

**Headers**:
```
Authorization: Bearer {access_token}
```

**Response**: `200 OK`
```json
[
  {
    "id": 1,
    "name": "안정형 포트폴리오",
    "description": "채권 중심의 안정적인 포트폴리오",
    "totalValue": 50000000,
    "totalCost": 45000000,
    "gainLoss": 5000000,
    "gainLossPercentage": 11.11,
    "createdAt": "2025-01-10T00:00:00Z",
    "updatedAt": "2025-12-14T10:00:00Z"
  }
]
```

---

### 2. Get Portfolio Detail
**Endpoint**: `GET /api/v1/portfolios/{id}`

**Headers**:
```
Authorization: Bearer {access_token}
```

**Response**: `200 OK`
```json
{
  "id": 1,
  "name": "안정형 포트폴리오",
  "description": "채권 중심의 안정적인 포트폴리오",
  "totalValue": 50000000,
  "totalCost": 45000000,
  "gainLoss": 5000000,
  "gainLossPercentage": 11.11,
  "holdings": [
    {
      "id": 1,
      "symbol": "005930",
      "name": "삼성전자",
      "quantity": 100,
      "averageCost": 55000,
      "currentPrice": 56100,
      "totalValue": 5610000,
      "gainLoss": 110000,
      "gainLossPercentage": 2.0,
      "assetType": "STOCK",
      "baseCurrency": null
    },
    {
      "id": 2,
      "symbol": "BTC",
      "name": "Bitcoin",
      "quantity": 0.5,
      "averageCost": 50000000,
      "currentPrice": 55000000,
      "totalValue": 27500000,
      "gainLoss": 2500000,
      "gainLossPercentage": 10.0,
      "assetType": "CRYPTO",
      "baseCurrency": "KRW"
    }
  ],
  "createdAt": "2025-01-10T00:00:00Z",
  "updatedAt": "2025-12-14T10:00:00Z"
}
```

**Errors**:
- `404 Not Found`: Portfolio not found
- `403 Forbidden`: Not authorized to access this portfolio

---

### 3. Create Portfolio
**Endpoint**: `POST /api/v1/portfolios`

**Headers**:
```
Authorization: Bearer {access_token}
Content-Type: application/json
```

**Request**:
```json
{
  "name": "Tech Growth Portfolio",
  "description": "Growth-focused tech stocks"
}
```

**Response**: `201 Created`
```json
{
  "id": 2,
  "name": "Tech Growth Portfolio",
  "description": "Growth-focused tech stocks",
  "totalValue": 0,
  "totalCost": 0,
  "gainLoss": 0,
  "gainLossPercentage": 0,
  "holdings": [],
  "createdAt": "2025-12-14T12:00:00Z",
  "updatedAt": "2025-12-14T12:00:00Z"
}
```

---

### 4. Update Portfolio
**Endpoint**: `PUT /api/v1/portfolios/{id}`

**Request**:
```json
{
  "name": "Updated Name",
  "description": "Updated description"
}
```

**Response**: `200 OK`
```json
{
  "id": 1,
  "name": "Updated Name",
  "description": "Updated description",
  ...
}
```

---

### 5. Delete Portfolio
**Endpoint**: `DELETE /api/v1/portfolios/{id}`

**Response**: `204 No Content`

**Errors**:
- `404 Not Found`: Portfolio not found
- `403 Forbidden`: Not authorized

---

### 6. Add Holding
**Endpoint**: `POST /api/v1/portfolios/{id}/holdings`

**Request (Stock)**:
```json
{
  "symbol": "005930",
  "quantity": 100,
  "averageCost": 55000,
  "assetType": "STOCK"
}
```

**Request (Crypto)**:
```json
{
  "symbol": "BTC",
  "quantity": 1.5,
  "averageCost": 50000000,
  "assetType": "CRYPTO",
  "baseCurrency": "KRW"
}
```

**Response**: `201 Created`
```json
{
  "id": 5,
  "symbol": "005930",
  "name": "삼성전자",
  "quantity": 100,
  "averageCost": 55000,
  "currentPrice": 56100,
  "totalValue": 5610000,
  "gainLoss": 110000,
  "gainLossPercentage": 2.0,
  "assetType": "STOCK",
  "baseCurrency": null
}
```

---

### 7. Update Holding
**Endpoint**: `PUT /api/v1/portfolios/{portfolioId}/holdings/{holdingId}`

**Request**:
```json
{
  "quantity": 60,
  "averageCost": 195000,
  "baseCurrency": "USD"
}
```

**Note**: `baseCurrency` can be updated for CRYPTO assets only

**Response**: `200 OK`

---

### 8. Delete Holding
**Endpoint**: `DELETE /api/v1/portfolios/{portfolioId}/holdings/{holdingId}`

**Response**: `204 No Content`

---

### 9. Recalculate Portfolio Values
**Endpoint**: `POST /api/v1/portfolios/{id}/recalculate`

**Description**: Fetches latest prices and recalculates all portfolio metrics

**Response**: `200 OK`
```json
{
  "id": 1,
  "totalValue": 51000000,
  "gainLoss": 6000000,
  "gainLossPercentage": 13.33,
  "updatedAt": "2025-12-14T14:30:00Z"
}
```

---

## Performance Optimization

### N+1 Query 해결 (Phase 3)

```java
// @EntityGraph로 Holdings와 함께 로딩
@EntityGraph(attributePaths = { "holdings" })
@Query("SELECT p FROM Portfolio p WHERE p.id = :id AND p.userId = :userId")
Optional<Portfolio> findByIdAndUserIdWithHoldings(@Param("id") Long id, @Param("userId") Long userId);
```

**결과**: 11 queries → 2 queries (-82%)

### Read/Write 분리 (Phase 4a)

- **읽기**: DB 조회만 수행 (외부 API 호출 없음)
- **쓰기**: `PortfolioPriceScheduler`가 5분마다 백그라운드에서 가격 갱신

**결과**: 496ms → 347ms (-30%)

### Redis 캐싱 (Phase 7)

```java
@Cacheable(value = "portfolios", key = "#portfolioId")
public PortfolioDto getPortfolioById(Long portfolioId, Long userId)
```

**결과**: Cache Hit 시 DB 커넥션 불필요 → TPS 217 req/s

---

## Cache Configuration

| Cache Name | TTL | 용도 |
|------------|-----|------|
| `portfolios` | 5분 | 포트폴리오 상세 정보 |
| `stockPrice` | 30초 | 주식 현재가 (가격 계산용) |
| `cryptoPrice` | 30초 | 암호화폐 현재가 (가격 계산용) |

---

## Implementation Status

### ✅ 완료 (100%)
- [x] Full CRUD for portfolios
- [x] Holdings management (Stock + Crypto)
- [x] Real-time value calculation
- [x] User authorization checks
- [x] Multi-asset support (STOCK, CRYPTO)
- [x] Multi-currency support (KRW, USD)
- [x] N+1 Query 해결 (@EntityGraph)
- [x] Read/Write 분리 (Scheduler)
- [x] Redis Caching
- [x] 백테스팅 지원 (HistoricalDataFacade 연동)

### 🚧 Pending
- [ ] Portfolio history tracking
- [ ] Performance benchmarking vs Index
- [ ] Risk metrics calculation (VaR, Sharpe)
- [ ] Dividend tracking

---

## Backend Implementation

**Controller**
```
portfolio/controller/PortfolioController.java
```

**Service**
```
portfolio/service/PortfolioService.java
```

**Repository**
```
portfolio/repository/PortfolioRepository.java      ← @EntityGraph
portfolio/repository/PortfolioHoldingRepository.java
```

**Entities**
```
portfolio/entity/Portfolio.java
portfolio/entity/PortfolioHolding.java             ← AssetType enum
```

**DTOs**
```
portfolio/dto/PortfolioDto.java
portfolio/dto/PortfolioHoldingDto.java
portfolio/dto/CreatePortfolioRequest.java
portfolio/dto/UpdatePortfolioRequest.java
portfolio/dto/AddHoldingRequest.java
portfolio/dto/UpdateHoldingRequest.java
```

**Background Jobs**
```
portfolio/scheduler/PortfolioPriceScheduler.java   ← 5분마다 가격 갱신
portfolio/consumer/PortfolioPriceConsumer.java     ← Kafka EDA (Future)
```

---

## Asset Type Support

### PortfolioHolding.AssetType

```java
public enum AssetType {
    STOCK,   // 주식 (한국: 005930, 미국: AAPL)
    CRYPTO   // 암호화폐 (BTC, ETH)
}
```

### 가격 조회 라우팅

```
PortfolioService.updatePortfolioPrices()
├── assetType == STOCK  → MarketDataService.getStockPrice()
└── assetType == CRYPTO → CryptoDataService.getCryptoPrice()
```

---

## Error Handling

| Status | Error | Description |
|--------|-------|-------------|
| 400 | `INVALID_REQUEST` | 잘못된 요청 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 404 | `PORTFOLIO_NOT_FOUND` | 포트폴리오 없음 |
| 404 | `HOLDING_NOT_FOUND` | 보유 종목 없음 |
| 409 | `DUPLICATE_NAME` | 중복된 포트폴리오 이름 |

---

**Last Updated**: 2025-12-14
**Maintainer**: Claude Code
