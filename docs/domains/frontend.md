# Frontend Domain Reference

## Page Structure

### (auth) Route Group
- **login/**: Kakao OAuth2 로그인 페이지
- **When**: 미인증 사용자 접근 시

### (dashboard) Route Group  
- **dashboard/**: 포트폴리오 개요, 시장 스냅샷
- **portfolios/**: 포트폴리오 CRUD 관리 페이지
- **market/**: 시장 데이터 조회 페이지

## Key Components

### PortfolioCard
**Purpose**: 포트폴리오 카드 표시  
**When**: 대시보드, 포트폴리오 목록에서 사용  
**Features**: 손익 상태 색상, 호버 애니메이션

### StockPrice  
**Purpose**: 실시간 주식 가격 표시  
**When**: 시장 데이터 페이지, 포트폴리오 상세에서 사용  
**Features**: 가격 변동 애니메이션, 트렌드 아이콘

### PageTransition
**Purpose**: 페이지 전환 애니메이션  
**When**: 모든 페이지 이동 시 적용

## State Management (Zustand)

### Portfolio Store
- `portfolios[]`: 포트폴리오 목록
- `selectedPortfolio`: 현재 선택된 포트폴리오
- `fetchPortfolios()`: API 호출
- `createPortfolio()`: 새 포트폴리오 생성

### Auth Store  
- `user`: 현재 사용자 정보
- `isAuthenticated`: 인증 상태
- `login()`, `logout()`: 인증 관리

## Tech Stack
- **Next.js 14**: App Router, TypeScript
- **Styling**: Tailwind CSS + shadcn/ui  
- **Animation**: Framer Motion
- **State**: Zustand