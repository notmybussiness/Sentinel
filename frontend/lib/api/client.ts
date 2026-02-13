import { MarketIndex, CryptoPrice, Portfolio, AiAnalysisResult, PortfolioHistoryPoint, AssetAllocation, User, AuthTokens, ChartData, PriceHistoryDto, AssetType, AiAnalysisRequest, AiAnalysisResponse, AiServiceStatus, BacktestRequest, BacktestResponse, BacktestValidationResponse, BacktestStatus, HistoricalPriceData, RebalancingRequest, RebalancingResponse, RebalancingSimulationResponse, StrategyInfoResponse } from './types';
import { MOCK_MARKET_INDICES, MOCK_CRYPTO_PRICES, MOCK_PORTFOLIOS, MOCK_AI_ANALYSIS, MOCK_PORTFOLIO_HISTORY, MOCK_ASSET_ALLOCATION } from './mock-data';
import {
    createPortfolio as createPortfolioApi,
    deletePortfolio as deletePortfolioApi,
    devLogin as devLoginApi,
    getCurrentUser as getCurrentUserApi,
    getKakaoLoginUrl as getKakaoLoginUrlApi,
    getMarketIndices as getMarketIndicesApi,
    getPortfolio as getPortfolioApi,
    getPortfolios as getPortfoliosApi,
    getStockPrice as getStockPriceApi,
    logout as logoutApi,
    recalculatePortfolio as recalculatePortfolioApi,
    searchSymbol as searchSymbolApi,
    updatePortfolio as updatePortfolioApi,
} from './generated/sdk';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

// ?袁⑸뻻嚥?mock delay ?醫? (?癒?춭??筌띾뜆?졿뉩紐껋쟿??곷?
const SIMULATED_DELAY_MS = 800;
function delay<T>(data: T): Promise<T> {
    return new Promise((resolve) => setTimeout(() => resolve(data), SIMULATED_DELAY_MS));
}

// ?醫뤾쿃 揶쏄퉮????λ땾
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

// 嚥≪뮄??袁⑹뜍 (?醫뤾쿃 ????
export function logout() {
    if (typeof window !== 'undefined') {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/';
    }
}

// ?紐꾩쵄 ??삳쐭 ??釉?fetch
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

    // 401 ???醫뤾쿃 揶쏄퉮????뺣즲
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

// Backend MarketIndexDto ??FE MarketIndex 筌띲끋釉?(value ??price)
interface BackendMarketIndex {
    symbol: string;
    name: string;
    value: number;
    change: number;
    changePercent: number;
    timestamp: string;
}

// Backend CryptoPriceDto ??FE CryptoPrice 筌띲끋釉?
interface BackendCryptoPrice {
    symbol: string;
    name: string;
    koreanName: string;
    marketCode: string;
    price: number;
    change: number;
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
        change: item.change || 0, // ?袁⑹뵬 ????揶쎛野?癰궰??
        changePercent: item.changePercent, // ?袁⑹뵬 ????癰궰??뉗ぇ (%)
        volume: item.volume,
        marketCap: item.tradeValue, // tradeValue??marketCap??곗쨮 ??筌?
    };
}

// Backend PortfolioDto ??FE Portfolio 筌띲끋釉?
interface BackendPortfolioHolding {
    id: number;
    symbol: string;
    assetType: 'STOCK' | 'CRYPTO' | 'CASH';
    quantity: number;
    averageCost: number;      // Backend field name
    currentPrice: number;
    marketValue: number;      // Backend field name
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
        cash: 0, // Backend?癒?퐣 癰귢쑬猷???볥궗??? ??놁벉
        holdings: (item.holdings || []).map(h => ({
            id: h.id,
            symbol: h.symbol,
            quantity: h.quantity,
            averagePrice: h.averageCost,     // BE: averageCost ??FE: averagePrice
            currentPrice: h.currentPrice,
            totalValue: h.marketValue,       // BE: marketValue ??FE: totalValue
            profit: h.gainLoss,
            profitRate: h.gainLossPercent,
            assetType: h.assetType,
        })),
        createdAt: item.createdAt,
    };
}

export const api = {
    auth: {
        // 燁삳똻萸??嚥≪뮄???URL????뺤쒔?癒?퐣 揶쎛?紐꾩긾
        getKakaoLoginUrl: async (): Promise<string> => {
            const response = await getKakaoLoginUrlApi();
            return response.data as string;
        },
        // 揶쏆뮆而???癒?짗 嚥≪뮄???
        devLogin: async (): Promise<{ accessToken: string; refreshToken: string; user: User }> => {
            const response = await devLoginApi();
            return response.data as { accessToken: string; refreshToken: string; user: User };
        },
        getMe: async (): Promise<User | null> => {
            try {
                const response = await getCurrentUserApi();
                return response.data as User;
            } catch {
                return null;
            }
        },
        logout: async (): Promise<void> => {
            try {
                await logoutApi();
            } finally {
                logout();
            }
        },
    },
    market: {
        getIndices: async (): Promise<MarketIndex[]> => {
            try {
                const response = await getMarketIndicesApi();
                const data = response.data as BackendMarketIndex[];
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
            const response = await getStockPriceApi(symbol);
            return response.data as MarketIndex;
        },
        search: async (query: string): Promise<MarketIndex[]> => {
            const response = await searchSymbolApi({ query });
            return response.data as MarketIndex[];
        },
        // 疫꿸퀣???紐낆넎?源놁뱽 ?袁る퉸 ?醫? (crypto.getTopCrypto嚥??귐됰뼄?????
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
                const response = await getPortfoliosApi();
                const data = response.data as BackendPortfolio[];
                return data.map(mapPortfolio);
            } catch (error) {
                console.error('Error fetching portfolios:', error);
                return MOCK_PORTFOLIOS;
            }
        },
        getById: async (id: number): Promise<Portfolio | undefined> => {
            try {
                const response = await getPortfolioApi(id);
                if (response.status === 404) return undefined;
                const data = response.data as BackendPortfolio;
                return mapPortfolio(data);
            } catch (error) {
                console.error('Error fetching portfolio:', error);
                return MOCK_PORTFOLIOS.find(p => p.id === id);
            }
        },
        create: async (data: { name: string; description?: string; initialCash?: number }): Promise<Portfolio> => {
            const payload = {
                name: data.name,
                description: data.description,
            };
            const response = await createPortfolioApi(payload);
            const result = response.data as BackendPortfolio;
            return mapPortfolio(result);
        },
        update: async (id: number, data: { name?: string; description?: string }): Promise<Portfolio> => {
            const response = await updatePortfolioApi(id, { name: data.name ?? "", description: data.description });
            const result = response.data as BackendPortfolio;
            return mapPortfolio(result);
        },
        delete: async (id: number): Promise<void> => {
            await deletePortfolioApi(id);
        },
        addHolding: async (portfolioId: number, data: { symbol: string; quantity: number; averagePrice: number; assetType: string }): Promise<Portfolio> => {
            // Backend expects averageCost instead of averagePrice
            const backendData = {
                symbol: data.symbol,
                quantity: data.quantity,
                averageCost: data.averagePrice,
                assetType: data.assetType,
            };
            const response = await fetchWithAuth(`/api/v1/portfolios/${portfolioId}/holdings`, {
                method: 'POST',
                body: JSON.stringify(backendData),
            });
            if (!response.ok) throw new Error('Failed to add holding');
            const result: BackendPortfolio = await response.json();
            return mapPortfolio(result);
        },
        // Holding ??륁젟
        updateHolding: async (portfolioId: number, holdingId: number, data: { quantity?: number; averagePrice?: number }): Promise<Portfolio> => {
            const backendData = {
                quantity: data.quantity,
                averageCost: data.averagePrice,
            };
            const response = await fetchWithAuth(`/api/v1/portfolios/${portfolioId}/holdings/${holdingId}`, {
                method: 'PUT',
                body: JSON.stringify(backendData),
            });
            if (!response.ok) throw new Error('Failed to update holding');
            const result: BackendPortfolio = await response.json();
            return mapPortfolio(result);
        },
        // Holding ????
        deleteHolding: async (portfolioId: number, holdingId: number): Promise<void> => {
            const response = await fetchWithAuth(`/api/v1/portfolios/${portfolioId}/holdings/${holdingId}`, {
                method: 'DELETE',
            });
            if (!response.ok) throw new Error('Failed to delete holding');
        },
        // ?????????????
        recalculate: async (portfolioId: number): Promise<Portfolio> => {
            const response = await recalculatePortfolioApi(portfolioId);
            const result = response.data as BackendPortfolio;
            return mapPortfolio(result);
        },
        // ?袁⑥삋???袁⑹춦 Backend?癒?퐣 ??볥궗??? ??놁벉 - Mock ?醫?
        getHistory: () => delay<PortfolioHistoryPoint[]>(MOCK_PORTFOLIO_HISTORY),
        getAllocation: () => delay<AssetAllocation[]>(MOCK_ASSET_ALLOCATION),
    },
    ai: {
        analyze: async (request: AiAnalysisRequest): Promise<AiAnalysisResponse | null> => {
            try {
                const response = await fetchWithAuth('/api/v1/ai/analyze', {
                    method: 'POST',
                    body: JSON.stringify(request),
                });
                if (!response.ok) {
                    console.error('AI analysis failed');
                    return null;
                }
                return response.json();
            } catch (error) {
                console.error('Error analyzing portfolio:', error);
                return null;
            }
        },
        getStatus: async (): Promise<AiServiceStatus | null> => {
            try {
                const response = await fetch(`${API_URL}/api/v1/ai/status`);
                if (!response.ok) return null;
                return response.json();
            } catch (error) {
                console.error('Error fetching AI status:', error);
                return null;
            }
        },
        // Legacy mock method for backward compatibility
        analyzeMock: (portfolioId: number) => delay<AiAnalysisResult>(MOCK_AI_ANALYSIS),
    },
    priceHistory: {
        getChartData: async (symbol: string, startTime: string, endTime: string): Promise<ChartData | null> => {
            try {
                const response = await fetch(
                    `${API_URL}/api/v1/price-history/chart?symbol=${encodeURIComponent(symbol)}&startTime=${encodeURIComponent(startTime)}&endTime=${encodeURIComponent(endTime)}`
                );
                if (response.status === 404) return null;
                if (!response.ok) throw new Error('Failed to fetch chart data');
                return response.json();
            } catch (error) {
                console.error('Error fetching chart data:', error);
                return null;
            }
        },
        getLatestPrice: async (symbol: string): Promise<PriceHistoryDto | null> => {
            try {
                const response = await fetch(`${API_URL}/api/v1/price-history/latest/${encodeURIComponent(symbol)}`);
                if (response.status === 404) return null;
                if (!response.ok) throw new Error('Failed to fetch latest price');
                return response.json();
            } catch (error) {
                console.error('Error fetching latest price:', error);
                return null;
            }
        },
        getLatestPrices: async (symbols: string[], assetType: AssetType): Promise<PriceHistoryDto[]> => {
            try {
                const response = await fetch(
                    `${API_URL}/api/v1/price-history/latest?symbols=${symbols.join(',')}&assetType=${assetType}`
                );
                if (!response.ok) throw new Error('Failed to fetch latest prices');
                return response.json();
            } catch (error) {
                console.error('Error fetching latest prices:', error);
                return [];
            }
        },
        getSymbolsByAssetType: async (assetType: AssetType): Promise<string[]> => {
            try {
                const response = await fetch(`${API_URL}/api/v1/price-history/symbols?assetType=${assetType}`);
                if (!response.ok) throw new Error('Failed to fetch symbols');
                return response.json();
            } catch (error) {
                console.error('Error fetching symbols:', error);
                return [];
            }
        },
    },
    backtest: {
        // 獄쏄퉲???쎈뱜 ??쎈뻬
        run: async (request: BacktestRequest): Promise<BacktestResponse> => {
            const response = await fetchWithAuth('/api/v1/backtest/run', {
                method: 'POST',
                body: JSON.stringify(request),
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Backtest failed: ${errorText}`);
            }
            return response.json();
        },
        // 獄쏄퉲???쎈뱜 野꺜筌?
        validate: async (request: BacktestRequest): Promise<BacktestValidationResponse> => {
            const response = await fetchWithAuth('/api/v1/backtest/validate', {
                method: 'POST',
                body: JSON.stringify(request),
            });
            if (!response.ok) throw new Error('Validation failed');
            return response.json();
        },
        // ??됰뮞?醫듼봺???怨쀬뵠??鈺곌퀬??
        getHistorical: async (symbol: string, startDate: string, endDate: string): Promise<HistoricalPriceData | null> => {
            try {
                const response = await fetchWithAuth(`/api/v1/backtest/historical/${encodeURIComponent(symbol)}?startDate=${startDate}&endDate=${endDate}`);
                if (response.status === 404) return null;
                if (!response.ok) throw new Error('Failed to fetch historical data');
                return response.json();
            } catch (error) {
                console.error('Error fetching historical data:', error);
                return null;
            }
        },
        // 獄쏄퉲???쎈뱜 ??뺥돩???怨밴묶
        getStatus: async (): Promise<BacktestStatus | null> => {
            try {
                const response = await fetch(`${API_URL}/api/v1/backtest/status`);
                if (!response.ok) return null;
                return response.json();
            } catch (error) {
                console.error('Error fetching backtest status:', error);
                return null;
            }
        },
    },
    rebalancing: {
        // ?귐됯강?怨쀫뼓 ?곕뗄荑?
        getRecommendations: async (request: RebalancingRequest): Promise<RebalancingResponse> => {
            const response = await fetchWithAuth('/api/v1/rebalancing/recommend', {
                method: 'POST',
                body: JSON.stringify(request),
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Rebalancing recommendation failed: ${errorText}`);
            }
            return response.json();
        },
        // ?귐됯강?怨쀫뼓 ?????됱뵠??
        simulate: async (request: RebalancingRequest): Promise<RebalancingSimulationResponse> => {
            const response = await fetchWithAuth('/api/v1/rebalancing/simulate', {
                method: 'POST',
                body: JSON.stringify(request),
            });
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Rebalancing simulation failed: ${errorText}`);
            }
            return response.json();
        },
        // ????揶쎛?館釉??袁⑥셽 筌뤴뫖以?
        getStrategies: async (): Promise<StrategyInfoResponse | null> => {
            try {
                const response = await fetch(`${API_URL}/api/v1/rebalancing/strategies`);
                if (!response.ok) return null;
                return response.json();
            } catch (error) {
                console.error('Error fetching strategies:', error);
                return null;
            }
        },
    },
};



