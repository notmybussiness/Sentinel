// API Response Types for Sentinel Backend Integration

// Common API Response Structure
export interface ApiResponse<T = any> {
  data: T;
  status: number;
  message?: string;
}

// Error Response
export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
  path?: string;
}

// Auth Types
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface User {
  id: number;
  email: string;
  name: string;
  profileImageUrl?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  kakaoId?: string;
}

// Market Data Types
export interface StockPrice {
  id?: number;
  symbol: string;
  price: number;
  changePercent?: number;
  high24h?: number;
  low24h?: number;
  volume?: number;
  marketCap?: number;
  dataSource: string;
  timestamp: string;
}

export interface ServiceStatusResponse {
  available: boolean;
  message: string;
}

// Portfolio Types
export interface Portfolio {
  id: number;
  name: string;
  description?: string;
  userId: number;
  totalCost: number;
  totalValue: number;
  totalGainLoss: number;
  totalGainLossPercent: number;
  createdAt: string;
  updatedAt: string;
  holdings?: PortfolioHolding[];
}

export interface PortfolioHolding {
  id: number;
  portfolioId: number;
  symbol: string;
  quantity: number;
  averageCost: number;
  totalCost: number;
  currentPrice?: number;
  marketValue?: number;
  gainLoss?: number;
  gainLossPercent?: number;
  createdAt: string;
  updatedAt: string;
}

// Request Types
export interface CreatePortfolioRequest {
  name: string;
  description?: string;
}

export interface UpdatePortfolioRequest {
  name?: string;
  description?: string;
}

export interface AddHoldingRequest {
  symbol: string;
  quantity: number;
  averageCost: number;
}

export interface UpdateHoldingRequest {
  quantity?: number;
  averageCost?: number;
}

// API Client Configuration
export interface ApiClientConfig {
  baseURL: string;
  timeout: number;
  withCredentials: boolean;
}

// Token Storage
export interface TokenStorage {
  accessToken: string | null;
  refreshToken: string | null;
  tokenType: string;
  expiresIn: number;
  expiresAt: number;
}

// API Method Types
export type ApiMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

// Query Keys for React Query
export const QueryKeys = {
  // Auth
  currentUser: 'current-user',

  // Market Data
  stockPrice: (symbol: string) => ['stock-price', symbol],
  stockPrices: (symbols: string[]) => ['stock-prices', symbols.join(',')],
  marketStatus: 'market-status',

  // Portfolio
  portfolios: (userId: number) => ['portfolios', userId],
  portfolio: (portfolioId: number, userId: number) => ['portfolio', portfolioId, userId],
  portfolioHoldings: (portfolioId: number) => ['portfolio-holdings', portfolioId],
} as const;