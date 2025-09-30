# 📦 Frontend Module Structure

## Core Directories

### `app/` - Next.js 14 App Router

Pages using App Router convention with TypeScript and "use client" directives.

**Structure**:
```
app/
├── layout.tsx              # Root layout with AuthProvider
├── page.tsx                # Homepage
├── login/page.tsx          # Login page
├── auth/
│   └── callback/page.tsx   # OAuth callback handler
├── portfolios/
│   ├── page.tsx            # Portfolio list
│   └── [id]/page.tsx       # Portfolio detail
├── lab/page.tsx            # Backtesting lab
├── market/page.tsx         # Market data
├── components-preview/page.tsx  # Component showcase
├── globals.css             # Global styles + Tailwind
├── fonts/                  # Geist fonts
└── favicon.ico
```

---

### `lib/` - Utilities & Logic

Core application logic, API clients, and utilities.

#### **API Module** (`lib/api/`)

**client.ts**: Axios-based API client
```typescript
Features:
- Base URL configuration
- JWT token management (localStorage)
- Auto token refresh on 401
- Request/response interceptors
- Error handling

Functions:
- get<T>(url, requireAuth?)
- post<T>(url, data, requireAuth?)
- put<T>(url, data, requireAuth?)
- delete<T>(url, requireAuth?)
- setTokens(access, refresh)
- clearTokens()
- getAccessToken()
```

**auth.ts**: Authentication API
```typescript
Functions:
- getKakaoLoginUrl(): string
- handleKakaoCallback(code): Promise<LoginResponse>
- logout(): Promise<void>
- getCurrentUser(): User | null
- isAuthenticated(): boolean
- devLogin(): Promise<LoginResponse>
```

---

#### **Mock Data** (`lib/mockData.ts`)

Centralized mock data for development and testing.

**Data Sets**:
```typescript
// Recommended portfolios for carousel (5)
mockRecommendedPortfolios: RecommendedPortfolio[]

// Market indices (4): S&P500, NASDAQ, DOW, KOSPI
mockMarketIndices: MarketIndex[]

// User portfolios with holdings (2)
mockUserPortfolios: Portfolio[]

// Rebalancing recommendations (3)
mockRebalancingRecommendations: RebalancingRecommendation[]

// Asset search results (5)
mockAssetSearchResults: AssetSearchResult[]
```

---

#### **Utilities** (`lib/utils.ts`)

Helper functions and utilities.

```typescript
// Tailwind class name merger
cn(...classes: ClassValue[]): string

// Price formatters
formatPrice(amount: number, currency?: string): string
formatPercentage(value: number): string

// Date formatters
formatDate(date: string | Date): string
```

---

### `contexts/` - React Context

Global state management using React Context API.

#### **AuthContext** (`contexts/AuthContext.tsx`)

**State**:
```typescript
{
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
```

**Methods**:
```typescript
login(code: string): Promise<void>
devLogin(): Promise<void>
logout(): Promise<void>
```

**Provider**:
```tsx
<AuthProvider>
  {children}
</AuthProvider>
```

**Hook**:
```typescript
const { user, isAuthenticated, login, logout } = useAuth();
```

---

### `components/` - React Components

Organized by category (see COMPONENTS.md for full details).

**Structure**:
```
components/
├── ui/              # Base UI components (Card, Button, etc.)
├── layout/          # Layout components (Header)
├── portfolio/       # Portfolio-specific components
├── market/          # Market data components
└── common/          # Shared utilities (MockDataBadge, EmptyState)
```

---

### `types/` - TypeScript Types

Shared TypeScript interfaces and types.

**Structure** (`types/index.ts`):
```typescript
// User & Auth
export interface User { ... }
export interface LoginResponse { ... }

// Portfolio
export interface Portfolio { ... }
export interface PortfolioHolding { ... }

// Market
export interface MarketIndex { ... }
export interface StockPrice { ... }

// UI
export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
export type CardPadding = 'none' | 'sm' | 'md' | 'lg';
```

---

## Configuration Files

### **next.config.mjs**
Next.js configuration with:
- Experimental app directory
- Image optimization
- Environment variables

### **tailwind.config.ts**
Tailwind CSS configuration with:
- Custom color palette
- Extended spacing scale
- Custom animations
- Font families (Geist, Geist Mono)

### **tsconfig.json**
TypeScript configuration with:
- Path aliases (@/*)
- Strict mode enabled
- App Router support

### **postcss.config.mjs**
PostCSS with Tailwind CSS processing

### **.env.local** (gitignored)
Environment variables:
```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_KAKAO_CLIENT_ID=...
NEXT_PUBLIC_KAKAO_REDIRECT_URI=...
NEXT_PUBLIC_DEV_MODE=true
```

---

## Module Dependencies

### Core Dependencies
- `next@14.2.23`: React framework
- `react@18`: UI library
- `typescript@5`: Type safety
- `tailwindcss@3.4`: Styling

### Additional Libraries
- `axios`: HTTP client
- `clsx`: Class name utility
- `tailwind-merge`: Tailwind class merger
- `lucide-react`: Icon library
- `framer-motion`: Animations (optional)

---

## Import Patterns

### Path Aliases
```typescript
// Components
import { Card } from '@/components/ui/Card';

// Utils
import { cn, formatPrice } from '@/lib/utils';

// Contexts
import { useAuth } from '@/contexts/AuthContext';

// Types
import type { User, Portfolio } from '@/types';

// Mock Data
import { mockUserPortfolios } from '@/lib/mockData';
```

### Component Imports
```typescript
// Named exports for utilities
import { cn, formatPrice } from '@/lib/utils';

// Default exports for components
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
```

---

## Code Organization Best Practices

### Component Structure
```typescript
// 1. Imports
import React from 'react';
import { cn } from '@/lib/utils';

// 2. Type definitions
interface ComponentProps {
  // ...
}

// 3. Component definition
export default function Component({ ...props }: ComponentProps) {
  // 4. State and hooks
  const [state, setState] = useState();

  // 5. Handlers
  const handleClick = () => {};

  // 6. Effects
  useEffect(() => {}, []);

  // 7. Render
  return <div>...</div>;
}
```

### File Naming
- Components: PascalCase (e.g., `Button.tsx`)
- Utilities: camelCase (e.g., `formatPrice.ts`)
- Pages: lowercase (e.g., `page.tsx`, `layout.tsx`)
- Types: PascalCase (e.g., `User.ts`, `index.ts`)

---

## Module Checklist

When adding new modules:
- [ ] Create in appropriate directory
- [ ] Add TypeScript interfaces
- [ ] Export from index file if needed
- [ ] Add JSDoc comments
- [ ] Update this documentation
- [ ] Add unit tests (if applicable)

---

**Last Updated**: 2025-10-01
**Framework**: Next.js 14 App Router
**Language**: TypeScript 5