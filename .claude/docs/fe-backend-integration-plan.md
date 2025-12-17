# FE-Backend API 연동 계획

> **Created**: 2025-12-15
> **Status**: Phase 1 진행중

---

## 현재 상태 분석

### FE 현재 API 사용 (100% Mock)
| FE 메서드 | 현재 상태 | 매핑할 Backend API |
|-----------|----------|-------------------|
| `api.market.getIndices()` | MOCK | GET /api/v1/market/indices |
| `api.market.getTopCrypto()` | MOCK | GET /api/v1/crypto/trending |
| `api.portfolio.getAll()` | MOCK | GET /api/v1/portfolios |
| `api.portfolio.getById(id)` | MOCK | GET /api/v1/portfolios/{id} |
| `api.portfolio.getHistory()` | MOCK | (계산 필요) |
| `api.portfolio.getAllocation()` | MOCK | (계산 필요) |
| `api.ai.analyze()` | MOCK | POST /api/v1/rebalancing/recommend |

### 미구현 FE 기능 (Backend API 있음)
| Backend API | 설명 | FE 필요 |
|-------------|------|---------|
| GET /api/v1/auth/kakao | OAuth 로그인 URL | 로그인 버튼 |
| POST /api/v1/auth/refresh | 토큰 갱신 | 자동 갱신 |
| GET /api/v1/auth/me | 현재 사용자 | 사용자 정보 표시 |
| POST /api/v1/auth/logout | 로그아웃 | 로그아웃 버튼 |
| POST /api/v1/portfolios | 포트폴리오 생성 | 생성 모달 |
| PUT /api/v1/portfolios/{id} | 포트폴리오 수정 | 수정 기능 |
| DELETE /api/v1/portfolios/{id} | 포트폴리오 삭제 | 삭제 버튼 |
| POST /api/v1/portfolios/{id}/holdings | 보유종목 추가 | 종목 추가 |
| GET /api/v1/crypto/stream/prices | 실시간 SSE | 실시간 가격 |
| POST /api/v1/backtest/run | 백테스트 실행 | 백테스트 UI |

---

## 구현 순서 (우선순위)

### Phase 1: 기반 구축 (필수) ✅ 진행중
1. **환경 설정** ✅ 완료
   - `frontend/.env.local` 생성
   - `NEXT_PUBLIC_API_URL=http://localhost:8080`

2. **API 클라이언트 리팩토링** 🔄 진행중
   - `lib/api/client.ts` → 실제 fetch 호출로 변경
   - 토큰 자동 첨부 (Authorization 헤더)
   - 401 시 토큰 갱신 로직

3. **인증 Context 생성** ⏳ 대기
   - `lib/auth/auth-context.tsx`
   - useAuth() 훅 제공

### Phase 2: 인증 연동
4. **OAuth 로그인**
   - 카카오 로그인 버튼 (Header)
   - Callback 페이지 (`app/auth/kakao/callback/page.tsx`)
   - 토큰 저장 (localStorage)

5. **사용자 UI**
   - Header에 로그인/로그아웃 표시
   - Sidebar에 사용자 정보

### Phase 3: 시장 데이터 연동 (인증 불필요)
6. **암호화폐 API**
   - `api.market.getTopCrypto()` → GET /api/v1/crypto/trending

7. **주식 시장 API**
   - `api.market.getIndices()` → GET /api/v1/market/indices

### Phase 4: 포트폴리오 연동 (인증 필요)
8. **포트폴리오 조회**
   - `api.portfolio.getAll()` → GET /api/v1/portfolios
   - `api.portfolio.getById()` → GET /api/v1/portfolios/{id}

9. **포트폴리오 CRUD**
   - 생성, 수정, 삭제 기능

---

## 수정할 파일

### 새로 생성
- `frontend/.env.local` ✅ 완료
- `frontend/lib/auth/auth-context.tsx`
- `frontend/app/auth/kakao/callback/page.tsx`

### 수정
- `frontend/lib/api/client.ts` (전면 수정)
- `frontend/lib/api/types.ts` (User, AuthTokens 타입 추가)
- `frontend/app/layout.tsx` (AuthProvider 추가)
- `frontend/components/layout/header.tsx` (로그인 버튼)
- `frontend/components/layout/sidebar.tsx` (사용자 정보)

---

## 코드 스니펫

### types.ts에 추가할 타입
```typescript
// Auth Types
export interface User {
    id: number;
    email: string;
    nickname: string;
    profileImage?: string;
}

export interface AuthTokens {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
}
```

### client.ts 전체 교체 코드
```typescript
import { MarketIndex, CryptoPrice, Portfolio, AiAnalysisResult, PortfolioHistoryPoint, AssetAllocation, User, AuthTokens } from './types';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
    const accessToken = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;

    const headers: HeadersInit = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (accessToken) {
        (headers as Record<string, string>)['Authorization'] = `Bearer ${accessToken}`;
    }

    const response = await fetch(`${API_URL}${url}`, { ...options, headers });

    if (response.status === 401 && accessToken) {
        const newToken = await refreshAccessToken();
        if (newToken) {
            (headers as Record<string, string>)['Authorization'] = `Bearer ${newToken}`;
            return fetch(`${API_URL}${url}`, { ...options, headers });
        } else {
            logout();
            throw new Error('Session expired');
        }
    }

    return response;
}

async function refreshAccessToken(): Promise<string | null> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return null;

    try {
        const response = await fetch(`${API_URL}/api/v1/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
        });

        if (!response.ok) return null;

        const data: AuthTokens = await response.json();
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        return data.accessToken;
    } catch {
        return null;
    }
}

export function logout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    window.location.href = '/';
}

export const api = {
    auth: {
        getKakaoLoginUrl: (): string => `${API_URL}/api/v1/auth/kakao`,
        getMe: async (): Promise<User | null> => {
            try {
                const response = await fetchWithAuth('/api/v1/auth/me');
                if (!response.ok) return null;
                return response.json();
            } catch { return null; }
        },
        logout: async (): Promise<void> => {
            try { await fetchWithAuth('/api/v1/auth/logout', { method: 'POST' }); }
            finally { logout(); }
        },
    },

    market: {
        getIndices: async (): Promise<MarketIndex[]> => {
            const response = await fetch(`${API_URL}/api/v1/market/indices`);
            if (!response.ok) throw new Error('Failed to fetch indices');
            return response.json();
        },
        getPrice: async (symbol: string): Promise<MarketIndex> => {
            const response = await fetch(`${API_URL}/api/v1/market/price/${symbol}`);
            if (!response.ok) throw new Error('Failed to fetch price');
            return response.json();
        },
        search: async (query: string): Promise<MarketIndex[]> => {
            const response = await fetch(`${API_URL}/api/v1/market/search?query=${encodeURIComponent(query)}`);
            if (!response.ok) throw new Error('Failed to search');
            return response.json();
        },
    },

    crypto: {
        getTopCrypto: async (): Promise<CryptoPrice[]> => {
            const response = await fetch(`${API_URL}/api/v1/crypto/trending`);
            if (!response.ok) throw new Error('Failed to fetch trending crypto');
            return response.json();
        },
        getPrice: async (symbol: string): Promise<CryptoPrice> => {
            const response = await fetch(`${API_URL}/api/v1/crypto/price/${symbol}`);
            if (!response.ok) throw new Error('Failed to fetch crypto price');
            return response.json();
        },
        search: async (query: string): Promise<CryptoPrice[]> => {
            const response = await fetch(`${API_URL}/api/v1/crypto/search?query=${encodeURIComponent(query)}`);
            if (!response.ok) throw new Error('Failed to search crypto');
            return response.json();
        },
    },

    portfolio: {
        getAll: async (): Promise<Portfolio[]> => {
            const response = await fetchWithAuth('/api/v1/portfolios');
            if (!response.ok) throw new Error('Failed to fetch portfolios');
            return response.json();
        },
        getById: async (id: number): Promise<Portfolio | undefined> => {
            const response = await fetchWithAuth(`/api/v1/portfolios/${id}`);
            if (response.status === 404) return undefined;
            if (!response.ok) throw new Error('Failed to fetch portfolio');
            return response.json();
        },
        create: async (data: { name: string; description?: string; initialCash?: number }): Promise<Portfolio> => {
            const response = await fetchWithAuth('/api/v1/portfolios', {
                method: 'POST',
                body: JSON.stringify(data),
            });
            if (!response.ok) throw new Error('Failed to create portfolio');
            return response.json();
        },
        update: async (id: number, data: { name?: string; description?: string }): Promise<Portfolio> => {
            const response = await fetchWithAuth(`/api/v1/portfolios/${id}`, {
                method: 'PUT',
                body: JSON.stringify(data),
            });
            if (!response.ok) throw new Error('Failed to update portfolio');
            return response.json();
        },
        delete: async (id: number): Promise<void> => {
            const response = await fetchWithAuth(`/api/v1/portfolios/${id}`, { method: 'DELETE' });
            if (!response.ok) throw new Error('Failed to delete portfolio');
        },
        addHolding: async (portfolioId: number, data: { symbol: string; quantity: number; averagePrice: number; assetType: string }): Promise<Portfolio> => {
            const response = await fetchWithAuth(`/api/v1/portfolios/${portfolioId}/holdings`, {
                method: 'POST',
                body: JSON.stringify(data),
            });
            if (!response.ok) throw new Error('Failed to add holding');
            return response.json();
        },
        getHistory: async (): Promise<PortfolioHistoryPoint[]> => [],
        getAllocation: async (): Promise<AssetAllocation[]> => [],
    },

    ai: {
        analyze: async (portfolioId: number): Promise<AiAnalysisResult> => {
            const response = await fetchWithAuth('/api/v1/rebalancing/recommend', {
                method: 'POST',
                body: JSON.stringify({ portfolioId }),
            });
            if (!response.ok) throw new Error('Failed to analyze portfolio');
            return response.json();
        },
    },
};
```

### auth-context.tsx (새 파일)
```typescript
'use client';

import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { api } from '@/lib/api/client';
import { User } from '@/lib/api/types';

interface AuthContextType {
    user: User | null;
    isLoading: boolean;
    isAuthenticated: boolean;
    login: () => void;
    logout: () => void;
    refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const initAuth = async () => {
            const accessToken = localStorage.getItem('accessToken');
            if (accessToken) {
                const userData = await api.auth.getMe();
                setUser(userData);
            }
            setIsLoading(false);
        };
        initAuth();
    }, []);

    const login = () => {
        window.location.href = api.auth.getKakaoLoginUrl();
    };

    const logout = async () => {
        await api.auth.logout();
        setUser(null);
    };

    const refreshUser = async () => {
        const userData = await api.auth.getMe();
        setUser(userData);
    };

    return (
        <AuthContext.Provider value={{ user, isLoading, isAuthenticated: !!user, login, logout, refreshUser }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within AuthProvider');
    return context;
}
```

---

## E2E 테스트 방법

1. Backend 실행: `cd backend && ./gradlew bootRun`
2. Frontend 실행: `cd frontend && npm run dev`
3. 브라우저에서 http://localhost:3000 접속
4. 테스트 항목:
   - [ ] 대시보드 로드 (시장 데이터)
   - [ ] 카카오 로그인
   - [ ] 포트폴리오 목록 조회
   - [ ] 포트폴리오 생성
   - [ ] 로그아웃
