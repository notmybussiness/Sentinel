# 📄 Frontend Pages Structure

## Page Routing (Next.js 14 App Router)

### Route Map

```
/                           → Homepage
/login                      → Login page
/auth/callback              → OAuth callback handler
/portfolios                 → Portfolio list
/portfolios/[id]            → Portfolio detail
/lab                        → Backtesting lab
/market                     → Market data
/components-preview         → Component showcase (dev only)
```

---

## Page Details

### 1. **Homepage** (`app/page.tsx`)

**Route**: `/`

**Sections**:
1. Hero Banner
   - Gradient background with CTA
   - "무료로 시작하기" button (unauthenticated only)

2. Market Indices
   - 4 index cards (S&P500, NASDAQ, DOW, KOSPI)
   - Real-time price and change display

3. Recommended Portfolios Carousel
   - 3 cards visible at once
   - Auto-play every 5 seconds
   - Slides 1 card at a time

4. Platform Statistics
   - 4 StatCards: Users, AUM, Avg Returns, Portfolios
   - Mock data badges

5. Key Features
   - 3 feature cards: Portfolio Mgmt, Backtesting, Rebalancing

6. CTA Section (unauthenticated only)
   - Final call-to-action with sign-up button

**State**: None (static with mock data)

**Protected**: No

---

### 2. **Login Page** (`app/login/page.tsx`)

**Route**: `/login`

**Features**:
- Kakao OAuth login button (yellow, visible)
- Developer login button (gray, for testing)
- Auto-redirect if already authenticated

**Authentication Flow**:
1. Click "카카오 로그인"
2. Redirect to Kakao OAuth
3. Return to `/auth/callback?code=...`
4. Process login and redirect to `/`

**Dev Mode**:
- DEV_MODE=true enables developer login
- Bypasses OAuth with mock user

**Protected**: No (redirects if authenticated)

---

### 3. **OAuth Callback** (`app/auth/callback/page.tsx`)

**Route**: `/auth/callback`

**Purpose**: Handle Kakao OAuth callback

**Flow**:
1. Extract `code` from URL params
2. Call `handleKakaoCallback(code)`
3. Store tokens and user data
4. Redirect to homepage

**Error Handling**:
- Invalid code → Show error + retry button
- Server error → Show error message

**Protected**: No

---

### 4. **Portfolio List** (`app/portfolios/page.tsx`)

**Route**: `/portfolios`

**Layout**:
- PageHeader with "Create New" button
- Grid of PortfolioCards (2 columns on desktop)
- Each card shows:
  - Portfolio name
  - SimpleChart (60px height)
  - Total value with PriceDisplay
  - Gain/loss with PercentageChange
  - Mock badge

**Interactions**:
- Click card → Navigate to detail page
- Click "Create New" → Future: Open creation modal

**State**:
```typescript
portfolios: Portfolio[]
isLoading: boolean
```

**Protected**: Yes (requires authentication)

**Mock Data**: `mockUserPortfolios` (2 portfolios)

---

### 5. **Portfolio Detail** (`app/portfolios/[id]/page.tsx`)

**Route**: `/portfolios/[id]`

**Sections**:
1. Header with portfolio name
2. Stats Row (4 StatCards)
   - Total Value, Cost Basis, Gain/Loss, Return %
3. Chart Section
   - SimpleChart (160px height)
   - Time period selector (1W, 1M, 3M, 1Y, ALL)
4. Holdings List
   - Table with columns: Asset, Quantity, Avg Cost, Current Price, Value, Gain/Loss
   - Each row is a HoldingItem component
5. Action Buttons
   - "리밸런싱 추천 보기" button

**Modals**:
- RebalancingModal: Shows BUY/SELL/HOLD recommendations

**State**:
```typescript
portfolio: Portfolio | null
showRebalancing: boolean
selectedPeriod: '1W' | '1M' | '3M' | '1Y' | 'ALL'
```

**Protected**: Yes

**Mock Data**: Find portfolio by ID from `mockUserPortfolios`

---

### 6. **Lab (Backtesting)** (`app/lab/page.tsx`)

**Route**: `/lab`

**Features**:
1. Portfolio Selection
   - Select dropdown with user's portfolios
   - "Load Portfolio" button

2. Backtesting Configuration
   - Start date picker
   - End date picker
   - Initial investment input (number)

3. Strategy Selection (future)
   - Radio buttons for different strategies

4. Results Section (future)
   - Chart showing backtest results
   - Performance metrics table

**Current Implementation**:
- UI only, no backend integration
- Mock portfolio selection works

**State**:
```typescript
selectedPortfolioId: string | null
startDate: string
endDate: string
initialInvestment: number
results: BacktestResult | null
```

**Protected**: Yes

---

### 7. **Market Data** (`app/market/page.tsx`)

**Route**: `/market`

**Sections**:
1. Header with search bar
2. Tabs
   - "주요 지수": Major indices (S&P500, NASDAQ, etc.)
   - "종목 검색": Asset search

3. Tab: 주요 지수
   - Grid of 4 IndexCards
   - Real-time prices and changes

4. Tab: 종목 검색
   - Search input
   - Asset type filter (ALL, STOCK, CRYPTO, COMMODITY, ETF)
   - Results grid with asset cards

**State**:
```typescript
activeTab: 'indices' | 'search'
searchQuery: string
selectedType: AssetType
searchResults: Asset[]
```

**Protected**: No

**Mock Data**:
- Indices: `mockMarketIndices`
- Search: `mockAssetSearchResults`

---

### 8. **Component Preview** (`app/components-preview/page.tsx`)

**Route**: `/components-preview`

**Purpose**: Development tool to showcase all UI components

**Sections**:
- All UI components with different states
- Visual testing for glassmorphism effects
- Color palette display
- Typography samples

**Protected**: No (dev only, not for production)

---

## Page Components Pattern

### Standard Page Structure
```tsx
"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { PageHeader } from '@/components/ui/PageHeader';
import { useAuth } from '@/contexts/AuthContext';

export default function Page() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();

  // Protected page check
  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, router]);

  return (
    <div className="min-h-screen bg-background-primary">
      <div className="max-w-6xl mx-auto px-8 py-6">
        <PageHeader title="Page Title" />
        {/* Content */}
      </div>
    </div>
  );
}
```

---

## Routing Patterns

### Navigation
```typescript
import { useRouter } from 'next/navigation';

const router = useRouter();

// Programmatic navigation
router.push('/portfolios');
router.push(`/portfolios/${id}`);
router.back();

// With query params
router.push('/market?type=stocks');
```

### Links
```tsx
import Link from 'next/link';

<Link href="/portfolios" className="...">
  View Portfolios
</Link>
```

---

## Protected Route Pattern

```typescript
// In page component
const { isAuthenticated, isLoading } = useAuth();

useEffect(() => {
  if (!isLoading && !isAuthenticated) {
    router.push('/login');
  }
}, [isAuthenticated, isLoading, router]);

// Show loading while checking auth
if (isLoading) {
  return <Spinner />;
}

// Don't render content until authenticated
if (!isAuthenticated) {
  return null;
}

return <div>Protected content</div>;
```

---

## Page Checklist

When creating new pages:
- [ ] Add to route map in this document
- [ ] Set up authentication guard if needed
- [ ] Add PageHeader component
- [ ] Use consistent container width (max-w-6xl)
- [ ] Add loading and error states
- [ ] Include Mock badges if using mock data
- [ ] Update navigation in Header component

---

**Last Updated**: 2025-10-01
**Total Pages**: 8 (7 functional + 1 dev tool)
**Protected Pages**: 4 (portfolios, detail, lab, preview)