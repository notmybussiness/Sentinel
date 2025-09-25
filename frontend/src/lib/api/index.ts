// API Services Exports
export * from './client';
export * from './auth';
export * from './market';
export * from './portfolio';

// Re-export types for convenience
export type {
  ApiResponse,
  ApiError,
  LoginResponse,
  RefreshTokenRequest,
  User,
  StockPrice,
  ServiceStatusResponse,
  Portfolio,
  PortfolioHolding,
  CreatePortfolioRequest,
  UpdatePortfolioRequest,
  AddHoldingRequest,
  UpdateHoldingRequest,
  TokenStorage,
  ApiClientConfig,
} from '@/types/api';

// Re-export React Query keys
export { QueryKeys } from '@/types/api';

// Centralized API object for easy access
import { authApi } from './auth';
import { marketApi } from './market';
import { portfolioApi } from './portfolio';

export const api = {
  auth: authApi,
  market: marketApi,
  portfolio: portfolioApi,
} as const;