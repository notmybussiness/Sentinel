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

// AI Analysis Types (Backend API)
export type AiAnalysisType = 'OVERVIEW' | 'DIVERSIFICATION' | 'RISK' | 'PERFORMANCE' | 'RECOMMENDATION';

export interface AiAnalysisRequest {
    portfolioId: number;
    analysisType: AiAnalysisType;
    includeMarketContext?: boolean;
}

export interface AiAnalysisResponse {
    summary: string;
    insights: string[];
    recommendations: string[];
    riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
    diversificationScore: number;
    analysisType: AiAnalysisType;
    analyzedAt: string;
}

export interface AiServiceStatus {
    available: boolean;
    provider: string;
    model: string;
    features: string[];
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

// Price History Types
export type AssetType = 'STOCK' | 'CRYPTO' | 'ETF' | 'INDEX';

export interface ChartDataPoint {
    time: string;
    value: number;
    open?: number;
    high?: number;
    low?: number;
    volume?: number;
}

export interface ChartStatistics {
    changePercent: number;
    changeAmount: number;
    highestPrice: number;
    lowestPrice: number;
    averageVolume: number;
}

export interface ChartData {
    symbol: string;
    baseCurrency: string;
    data: ChartDataPoint[];
    statistics: ChartStatistics;
}

export interface PriceHistoryDto {
    id: number;
    symbol: string;
    assetType: AssetType;
    baseCurrency: string;
    timestamp: string;
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
}
