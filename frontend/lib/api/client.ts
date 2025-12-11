import { MarketIndex, CryptoPrice, Portfolio, AiAnalysisResult, PortfolioHistoryPoint, AssetAllocation } from './types';
import { MOCK_MARKET_INDICES, MOCK_CRYPTO_PRICES, MOCK_PORTFOLIOS, MOCK_AI_ANALYSIS, MOCK_PORTFOLIO_HISTORY, MOCK_ASSET_ALLOCATION } from './mock-data';

const SIMULATED_DELAY_MS = 800;

function delay<T>(data: T): Promise<T> {
    return new Promise((resolve) => setTimeout(() => resolve(data), SIMULATED_DELAY_MS));
}

export const api = {
    market: {
        getIndices: () => delay<MarketIndex[]>(MOCK_MARKET_INDICES),
        getTopCrypto: () => delay<CryptoPrice[]>(MOCK_CRYPTO_PRICES),
    },
    portfolio: {
        getAll: () => delay<Portfolio[]>(MOCK_PORTFOLIOS),
        getById: (id: number) => {
            const p = MOCK_PORTFOLIOS.find(p => p.id === id);
            return delay<Portfolio | undefined>(p);
        },
        getHistory: () => delay<PortfolioHistoryPoint[]>(MOCK_PORTFOLIO_HISTORY),
        getAllocation: () => delay<AssetAllocation[]>(MOCK_ASSET_ALLOCATION),
    },
    ai: {
        analyze: (portfolioId: number) => delay<AiAnalysisResult>(MOCK_AI_ANALYSIS),
    }
};
