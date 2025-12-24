// Backtest Types
export type RebalanceFrequency = 'NONE' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY';

export interface BacktestRequest {
    portfolioId: number;
    startDate: string;
    endDate: string;
    initialCapital: number;
    rebalanceFrequency: RebalanceFrequency;
    transactionCostPercent?: number;
}

export interface BacktestPerformance {
    totalReturn: number;
    cagr: number;
    sharpeRatio: number;
    sortinoRatio: number;
    maxDrawdown: number;
    volatility: number;
    winRate: number;
}

export interface EquityCurvePoint {
    date: string;
    value: number;
}

export interface RebalanceEvent {
    date: string;
    type: string;
    details: string;
}

export interface HoldingSummary {
    symbol: string;
    weight: number;
    return: number;
}

export interface BacktestResponse {
    portfolioId: number;
    portfolioName: string;
    startDate: string;
    endDate: string;
    initialCapital: number;
    finalValue: number;
    rebalanceFrequency: RebalanceFrequency;
    transactionCostPercent: number;
    totalTransactionCosts: number;
    costImpactPercent: number;
    performance: BacktestPerformance;
    equityCurve: EquityCurvePoint[];
    rebalanceEvents: RebalanceEvent[];
    holdingsSummary: HoldingSummary[];
    executedAt: string;
}

export interface BacktestValidationResponse {
    valid: boolean;
    errors: string[];
    warnings: string[];
    dataAvailability: Record<string, boolean>;
}

export interface BacktestStatus {
    available: boolean;
    provider: string;
    supportedFrequencies: RebalanceFrequency[];
    maxDateRange: string;
    dataSources: {
        koreanStock: string;
        usStock: string;
        crypto: string;
    };
}

export interface HistoricalPrice {
    date: string;
    open: number;
    high: number;
    low: number;
    close: number;
    volume: number;
    adjustedClose: number;
}

export interface HistoricalPriceData {
    symbol: string;
    startDate: string;
    endDate: string;
    dataPoints: number;
    prices: HistoricalPrice[];
}
