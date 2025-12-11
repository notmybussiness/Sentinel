export interface Portfolio {
    id: number;
    name: string;
    description: string;
    totalValue: number;
    totalProfit: number;
    totalProfitRate: number;
    cash: number;
    holdings: PortfolioHolding[];
    createdAt: string;
}

export interface PortfolioHolding {
    id: number;
    symbol: string;
    quantity: number;
    averagePrice: number;
    currentPrice: number;
    totalValue: number;
    profit: number;
    profitRate: number;
    assetType: 'STOCK' | 'CRYPTO' | 'CASH';
}

export interface MarketIndex {
    symbol: string;
    name: string;
    price: number;
    change: number;
    changePercent: number;
}

export interface CryptoPrice {
    symbol: string;
    name: string; // crypto often has name separate
    price: number;
    change: number;
    changePercent: number;
    volume: number;
    marketCap: number;
}

export interface AiAnalysisResult {
    portfolioId: number;
    score: number;
    riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
    summary: string;
    recommendations: string[];
}

export interface PortfolioHistoryPoint {
    date: string;
    value: number;
}

export interface AssetAllocation {
    id: string;
    label: string;
    value: number;
    color: string;
}
