# 💼 Portfolio API Specification

## Base Information
- **Domain**: `/api/v1/portfolios`
- **Authentication**: Required (JWT Bearer Token)
- **Rate Limit**: 1000 requests/hour

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
    "updatedAt": "2025-10-01T10:00:00Z"
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
      "symbol": "AAPL",
      "name": "Apple Inc.",
      "quantity": 100,
      "averageCost": 150000,
      "currentPrice": 180000,
      "totalValue": 18000000,
      "gainLoss": 3000000,
      "gainLossPercentage": 20.0
    }
  ],
  "createdAt": "2025-01-10T00:00:00Z",
  "updatedAt": "2025-10-01T10:00:00Z"
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
  "createdAt": "2025-10-01T12:00:00Z",
  "updatedAt": "2025-10-01T12:00:00Z"
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

**Request**:
```json
{
  "symbol": "TSLA",
  "quantity": 50,
  "averageCost": 200000
}
```

**Response**: `201 Created`
```json
{
  "id": 5,
  "symbol": "TSLA",
  "name": "Tesla Inc.",
  "quantity": 50,
  "averageCost": 200000,
  "currentPrice": 220000,
  "totalValue": 11000000,
  "gainLoss": 1000000,
  "gainLossPercentage": 10.0
}
```

---

### 7. Update Holding
**Endpoint**: `PUT /api/v1/portfolios/{portfolioId}/holdings/{holdingId}`

**Request**:
```json
{
  "quantity": 60,
  "averageCost": 195000
}
```

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
  "updatedAt": "2025-10-01T14:30:00Z"
}
```

---

## Implementation Status

### ✅ Implemented
- Full CRUD for portfolios
- Holdings management
- Real-time value calculation
- User authorization checks

### 🚧 Pending
- Portfolio history tracking
- Performance benchmarking
- Risk metrics calculation
- Dividend tracking

---

## Backend Implementation

**Controller**: `backend/src/main/java/com/pjsent/sentinel/portfolio/controller/PortfolioController.java`

**Service**: `backend/src/main/java/com/pjsent/sentinel/portfolio/service/PortfolioService.java`

**Entities**:
- `backend/src/main/java/com/pjsent/sentinel/portfolio/entity/Portfolio.java`
- `backend/src/main/java/com/pjsent/sentinel/portfolio/entity/PortfolioHolding.java`

---

**Last Updated**: 2025-10-01