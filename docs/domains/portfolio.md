# Portfolio Domain API Reference

## Core APIs

### GET /api/v1/portfolios?userId={userId}
**Purpose**: 사용자 포트폴리오 목록 조회  
**When**: 대시보드 로드 시  
**Response**: Portfolio 배열

### POST /api/v1/portfolios?userId={userId}
**Purpose**: 새 포트폴리오 생성  
**When**: 사용자가 "포트폴리오 추가" 클릭 시  
**Request**: `{"name": "내 포트폴리오", "description": "..."}`

### GET /api/v1/portfolios/{id}?userId={userId}
**Purpose**: 특정 포트폴리오 상세 조회  
**When**: 포트폴리오 상세페이지 진입 시  
**Response**: Portfolio + Holdings 목록

### PUT /api/v1/portfolios/{id}?userId={userId}
**Purpose**: 포트폴리오 정보 수정  
**When**: 포트폴리오 이름/설명 변경 시

### DELETE /api/v1/portfolios/{id}?userId={userId}
**Purpose**: 포트폴리오 삭제  
**When**: 사용자가 포트폴리오 삭제 확인 시

## Holdings Management

### POST /api/v1/portfolios/{id}/holdings?userId={userId}
**Purpose**: 보유 종목 추가  
**When**: 사용자가 주식을 포트폴리오에 추가 시  
**Request**: `{"symbol": "AAPL", "quantity": 10, "averageCost": 150.00}`

### PUT /api/v1/portfolios/{id}/holdings/{holdingId}?userId={userId}
**Purpose**: 보유 종목 수정  
**When**: 수량이나 평균 단가 변경 시

### DELETE /api/v1/portfolios/{id}/holdings/{holdingId}?userId={userId}
**Purpose**: 보유 종목 제거  
**When**: 주식을 포트폴리오에서 제거 시

### POST /api/v1/portfolios/{id}/recalculate?userId={userId}
**Purpose**: 포트폴리오 재계산  
**When**: 실시간 가격 업데이트 시, 수동 새로고침 시  
**Action**: Market Data Service 호출하여 현재 가격으로 손익 재계산

## Business Logic
- **총 가치**: Σ(수량 × 현재가격)
- **총 비용**: Σ(수량 × 평균단가)  
- **손익**: 총 가치 - 총 비용
- **손익률**: (손익 ÷ 총 비용) × 100

## Key Features
- **Auto-calculation**: 보유 종목 변경 시 자동 손익 재계산
- **Real-time**: Market Data 연동으로 실시간 가격 반영
- **User Isolation**: userId로 사용자별 데이터 보호