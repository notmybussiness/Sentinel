import { Portfolio, MarketIndex, CryptoPrice, AiAnalysisResult, PortfolioHistoryPoint, AssetAllocation } from './types';

export const MOCK_MARKET_INDICES: MarketIndex[] = [
    { symbol: 'SPX', name: 'S&P 500', price: 4783.45, change: 23.45, changePercent: 0.49 },
    { symbol: 'NDX', name: 'Nasdaq 100', price: 16832.92, change: -12.30, changePercent: -0.07 },
    { symbol: 'DJI', name: 'Dow Jones', price: 37654.32, change: 112.50, changePercent: 0.30 },
    { symbol: 'KOSPI', name: 'KOSPI', price: 2654.32, change: 15.20, changePercent: 0.58 },
];

export const MOCK_CRYPTO_PRICES: CryptoPrice[] = [
    { symbol: 'BTC', name: 'Bitcoin', price: 64230.50, change: 1250.00, changePercent: 1.98, volume: 34500000000, marketCap: 1200000000000 },
    { symbol: 'ETH', name: 'Ethereum', price: 3450.20, change: -45.30, changePercent: -1.30, volume: 15000000000, marketCap: 400000000000 },
    { symbol: 'SOL', name: 'Solana', price: 145.60, change: 12.40, changePercent: 9.30, volume: 4500000000, marketCap: 65000000000 },
    { symbol: 'XRP', name: 'Ripple', price: 0.62, change: 0.01, changePercent: 1.60, volume: 1200000000, marketCap: 34000000000 },
];

export const MOCK_PORTFOLIOS: Portfolio[] = [
    {
        id: 1,
        name: 'Main Aggressive',
        description: 'High risk, high reward crypto & tech strategy',
        totalValue: 125430.50,
        totalProfit: 25430.50,
        totalProfitRate: 25.43,
        cash: 5000.00,
        createdAt: '2024-01-15T10:00:00Z',
        holdings: [
            { id: 101, symbol: 'BTC', quantity: 0.5, averagePrice: 50000, currentPrice: 64230.50, totalValue: 32115.25, profit: 7115.25, profitRate: 28.46, assetType: 'CRYPTO' },
            { id: 102, symbol: 'NVDA', quantity: 50, averagePrice: 450, currentPrice: 900, totalValue: 45000, profit: 22500, profitRate: 100.00, assetType: 'STOCK' },
            { id: 103, symbol: 'TSLA', quantity: 100, averagePrice: 200, currentPrice: 175, totalValue: 17500, profit: -2500, profitRate: -12.50, assetType: 'STOCK' },
        ]
    },
    {
        id: 2,
        name: 'Safe Dividend',
        description: 'Stable income portfolio',
        totalValue: 54320.00,
        totalProfit: 4320.00,
        totalProfitRate: 8.64,
        cash: 10000.00,
        createdAt: '2024-03-01T10:00:00Z',
        holdings: [
            { id: 201, symbol: 'O', quantity: 200, averagePrice: 55, currentPrice: 58, totalValue: 11600, profit: 600, profitRate: 5.45, assetType: 'STOCK' },
            { id: 202, symbol: 'KO', quantity: 300, averagePrice: 58, currentPrice: 60, totalValue: 18000, profit: 600, profitRate: 3.45, assetType: 'STOCK' },
        ]
    }
];

export const MOCK_AI_ANALYSIS: AiAnalysisResult = {
    portfolioId: 1,
    score: 85,
    riskLevel: 'HIGH',
    summary: 'Your portfolio is heavily weighted towards high-volatility assets (Crypto, Tech). While returns are high, downside risk is significant.',
    recommendations: [
        'Consider reducing exposure to TSLA to mitigate sector risk.',
        'Increase cash reserves or add bond ETFs for stability.',
        'Bitcoin dominance is high; consider diversifying into Ethereum or Solana.'
    ]
};

export const MOCK_PORTFOLIO_HISTORY: PortfolioHistoryPoint[] = Array.from({ length: 30 }, (_, i) => {
    const date = new Date();
    date.setDate(date.getDate() - (29 - i));
    return {
        date: date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
        value: 120000 + (Math.random() * 10000) + (i * 200), // Upward trend
    };
});

export const MOCK_ASSET_ALLOCATION: AssetAllocation[] = [
    { id: 'crypto', label: 'Crypto', value: 45, color: '#6366F1' }, // Indigo
    { id: 'stock', label: 'Stocks', value: 35, color: '#EC4899' },  // Pink
    { id: 'cash', label: 'Cash', value: 20, color: '#10B981' },    // Emerald
];
