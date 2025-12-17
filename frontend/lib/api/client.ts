import { MarketIndex, CryptoPrice, Portfolio, AiAnalysisResult, PortfolioHistoryPoint, AssetAllocation, User, AuthTokens } from './types';
import { MOCK_MARKET_INDICES, MOCK_CRYPTO_PRICES, MOCK_PORTFOLIOS, MOCK_AI_ANALYSIS, MOCK_PORTFOLIO_HISTORY, MOCK_ASSET_ALLOCATION } from './mock-data';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// 임시로 mock delay 유지 (점진적 마이그레이션)
const SIMULATED_DELAY_MS = 800;
function delay<T>(data: T): Promise<T> {
    return new Promise((resolve) => setTimeout(() => resolve(data), SIMULATED_DELAY_MS));
}

// 토큰 갱신 함수
async function refreshAccessToken(): Promise<string | null> {
    const refreshToken = typeof window !== 'undefined' ? localStorage.getItem('refreshToken') : null;
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

// 로그아웃 (토큰 삭제)
export function logout() {
    if (typeof window !== 'undefined') {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/';
    }
}

// 인증 헤더 포함 fetch
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

    // 401 시 토큰 갱신 시도
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

// Backend MarketIndexDto → FE MarketIndex 매핑 (value → price)
interface BackendMarketIndex {
    symbol: string;
    name: string;
    value: number;
    change: number;
    changePercent: number;
    timestamp: string;
}

// Backend CryptoPriceDto → FE CryptoPrice 매핑
interface BackendCryptoPrice {
    symbol: string;
    name: string;
    koreanName: string;
    marketCode: string;
    price: number;
    changePercent: number;
    volume: number;
    tradeValue: number;
    rank: number;
    baseCurrency: string;
}

function mapCryptoPrice(item: BackendCryptoPrice): CryptoPrice {
    return {
        symbol: item.symbol,
        name: item.koreanName || item.name,
        price: item.price,
        change: 0, // Backend에서 제공하지 않음
        changePercent: item.changePercent,
        volume: item.volume,
        marketCap: item.tradeValue, // tradeValue를 marketCap으로 대체
    };
}

// Backend PortfolioDto → FE Portfolio 매핑
interface BackendPortfolioHolding {
    id: number;
    symbol: string;
    assetType: 'STOCK' | 'CRYPTO' | 'CASH';
    quantity: number;
    averagePrice: number;
    currentPrice: number;
    totalValue: number;
    gainLoss: number;
    gainLossPercent: number;
}

interface BackendPortfolio {
    id: number;
    userId: number;
    name: string;
    description: string;
    totalValue: number;
    totalCost: number;
    totalGainLoss: number;
    totalGainLossPercent: number;
    createdAt: string;
    updatedAt: string;
    holdings: BackendPortfolioHolding[];
}

function mapPortfolio(item: BackendPortfolio): Portfolio {
    return {
        id: item.id,
        name: item.name,
        description: item.description || '',
        totalValue: item.totalValue,
        totalProfit: item.totalGainLoss,
        totalProfitRate: item.totalGainLossPercent,
        cash: 0, // Backend에서 별도 제공하지 않음
        holdings: (item.holdings || []).map(h => ({
            id: h.id,
            symbol: h.symbol,
            quantity: h.quantity,
            averagePrice: h.averagePrice,
            currentPrice: h.currentPrice,
            totalValue: h.totalValue,
            profit: h.gainLoss,
            profitRate: h.gainLossPercent,
            assetType: h.assetType,
        })),
        createdAt: item.createdAt,
    };
}

export const api = {
    auth: {
        // 카카오 로그인 URL을 서버에서 가져옴
        getKakaoLoginUrl: async (): Promise<string> => {
            const response = await fetch(`${API_URL}/api/v1/auth/kakao`);
            if (!response.ok) throw new Error('Failed to get Kakao login URL');
            return response.text();
        },
        // 개발용 자동 로그인
        devLogin: async (): Promise<{ accessToken: string; refreshToken: string; user: User }> => {
            const response = await fetch(`${API_URL}/api/v1/auth/dev-login`, { method: 'POST' });
            if (!response.ok) throw new Error('Dev login failed');
            return response.json();
        },
        getMe: async (): Promise<User | null> => {
            try {
                const response = await fetchWithAuth('/api/v1/auth/me');
                if (!response.ok) return null;
                return response.json();
            } catch {
                return null;
            }
        },
        logout: async (): Promise<void> => {
            try {
                await fetchWithAuth('/api/v1/auth/logout', { method: 'POST' });
            } finally {
                logout();
            }
        },
    },
    market: {
        getIndices: async (): Promise<MarketIndex[]> => {
            try {
                const response = await fetch(`${API_URL}/api/v1/market/indices`);
                if (!response.ok) {
                    console.error('Failed to fetch indices, falling back to mock');
                    return MOCK_MARKET_INDICES;
                }
                const data: BackendMarketIndex[] = await response.json();
                // Backend value → FE price 매핑
                return data.map(item => ({
                    symbol: item.symbol,
                    name: item.name,
                    price: item.value,
                    change: item.change,
                    changePercent: item.changePercent,
                }));
            } catch (error) {
                console.error('Error fetching indices:', error);
                return MOCK_MARKET_INDICES;
            }
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
        // 기존 호환성을 위해 유지 (crypto.getTopCrypto로 리다이렉트)
        getTopCrypto: async (): Promise<CryptoPrice[]> => {
            try {
                const response = await fetch(`${API_URL}/api/v1/crypto/trending`);
                if (!response.ok) {
                    console.error('Failed to fetch trending crypto, falling back to mock');
                    return MOCK_CRYPTO_PRICES;
                }
                const data: BackendCryptoPrice[] = await response.json();
                return data.map(mapCryptoPrice);
            } catch (error) {
                console.error('Error fetching trending crypto:', error);
                return MOCK_CRYPTO_PRICES;
            }
        },
    },
    crypto: {
        getTopCrypto: async (): Promise<CryptoPrice[]> => {
            try {
                const response = await fetch(`${API_URL}/api/v1/crypto/trending`);
                if (!response.ok) {
                    console.error('Failed to fetch trending crypto, falling back to mock');
                    return MOCK_CRYPTO_PRICES;
                }
                const data: BackendCryptoPrice[] = await response.json();
                return data.map(mapCryptoPrice);
            } catch (error) {
                console.error('Error fetching trending crypto:', error);
                return MOCK_CRYPTO_PRICES;
            }
        },
        getPrice: async (symbol: string): Promise<CryptoPrice> => {
            const response = await fetch(`${API_URL}/api/v1/crypto/price/${symbol}`);
            if (!response.ok) throw new Error('Failed to fetch crypto price');
            const data: BackendCryptoPrice = await response.json();
            return mapCryptoPrice(data);
        },
        search: async (query: string): Promise<CryptoPrice[]> => {
            const response = await fetch(`${API_URL}/api/v1/crypto/search?query=${encodeURIComponent(query)}`);
            if (!response.ok) throw new Error('Failed to search crypto');
            const data: BackendCryptoPrice[] = await response.json();
            return data.map(mapCryptoPrice);
        },
    },
    portfolio: {
        getAll: async (): Promise<Portfolio[]> => {
            try {
                const response = await fetchWithAuth('/api/v1/portfolios');
                if (!response.ok) {
                    console.error('Failed to fetch portfolios, falling back to mock');
                    return MOCK_PORTFOLIOS;
                }
                const data: BackendPortfolio[] = await response.json();
                return data.map(mapPortfolio);
            } catch (error) {
                console.error('Error fetching portfolios:', error);
                return MOCK_PORTFOLIOS;
            }
        },
        getById: async (id: number): Promise<Portfolio | undefined> => {
            try {
                const response = await fetchWithAuth(`/api/v1/portfolios/${id}`);
                if (response.status === 404) return undefined;
                if (!response.ok) {
                    const p = MOCK_PORTFOLIOS.find(p => p.id === id);
                    return p;
                }
                const data: BackendPortfolio = await response.json();
                return mapPortfolio(data);
            } catch (error) {
                console.error('Error fetching portfolio:', error);
                return MOCK_PORTFOLIOS.find(p => p.id === id);
            }
        },
        create: async (data: { name: string; description?: string; initialCash?: number }): Promise<Portfolio> => {
            const response = await fetchWithAuth('/api/v1/portfolios', {
                method: 'POST',
                body: JSON.stringify(data),
            });
            if (!response.ok) throw new Error('Failed to create portfolio');
            const result: BackendPortfolio = await response.json();
            return mapPortfolio(result);
        },
        update: async (id: number, data: { name?: string; description?: string }): Promise<Portfolio> => {
            const response = await fetchWithAuth(`/api/v1/portfolios/${id}`, {
                method: 'PUT',
                body: JSON.stringify(data),
            });
            if (!response.ok) throw new Error('Failed to update portfolio');
            const result: BackendPortfolio = await response.json();
            return mapPortfolio(result);
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
            const result: BackendPortfolio = await response.json();
            return mapPortfolio(result);
        },
        // 아래는 아직 Backend에서 제공하지 않음 - Mock 유지
        getHistory: () => delay<PortfolioHistoryPoint[]>(MOCK_PORTFOLIO_HISTORY),
        getAllocation: () => delay<AssetAllocation[]>(MOCK_ASSET_ALLOCATION),
    },
    ai: {
        analyze: (portfolioId: number) => delay<AiAnalysisResult>(MOCK_AI_ANALYSIS),
    }
};
