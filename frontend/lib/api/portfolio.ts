import apiClient from './client';

/**
 * Portfolio Types
 */
export interface Portfolio {
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
  holdings: PortfolioHolding[];
}

export interface PortfolioHolding {
  id: number;
  portfolioId: number;
  symbol: string;
  quantity: number;
  averageCost: number;
  currentPrice: number;
  marketValue: number;
  totalCost: number;
  gainLoss: number;
  gainLossPercent: number;
  assetType: 'STOCK' | 'CRYPTO';
  baseCurrency?: string;  // 'USD' or 'KRW' (crypto only)
  createdAt: string;
  updatedAt: string;
}

export interface CreatePortfolioRequest {
  name: string;
  description?: string;
}

export interface UpdatePortfolioRequest {
  name: string;
  description?: string;
}

export interface AddHoldingRequest {
  symbol: string;
  quantity: number;
  averageCost: number;
  assetType?: 'STOCK' | 'CRYPTO';  // Defaults to 'STOCK' if not provided
  baseCurrency?: string;  // Required for CRYPTO, optional for STOCK
}

export interface UpdateHoldingRequest {
  quantity: number;
  averageCost: number;
  baseCurrency?: string;  // Optional: update base currency for crypto
}

/**
 * Portfolio API Client
 */

/**
 * Get all portfolios for the current user
 * @returns List of portfolios
 */
export async function getPortfolios(): Promise<Portfolio[]> {
  const response = await apiClient.get<Portfolio[]>('/api/v1/portfolios');
  return response.data;
}

/**
 * Get portfolio by ID
 * @param portfolioId - Portfolio ID
 * @returns Portfolio details with holdings
 */
export async function getPortfolio(portfolioId: number): Promise<Portfolio> {
  const response = await apiClient.get<Portfolio>(`/api/v1/portfolios/${portfolioId}`);
  return response.data;
}

/**
 * Create a new portfolio
 * @param request - Portfolio creation request
 * @returns Created portfolio
 */
export async function createPortfolio(request: CreatePortfolioRequest): Promise<Portfolio> {
  const response = await apiClient.post<Portfolio>('/api/v1/portfolios', request);
  return response.data;
}

/**
 * Update portfolio
 * @param portfolioId - Portfolio ID
 * @param request - Portfolio update request
 * @returns Updated portfolio
 */
export async function updatePortfolio(
  portfolioId: number,
  request: UpdatePortfolioRequest
): Promise<Portfolio> {
  const response = await apiClient.put<Portfolio>(
    `/api/v1/portfolios/${portfolioId}`,
    request
  );
  return response.data;
}

/**
 * Delete portfolio
 * @param portfolioId - Portfolio ID
 */
export async function deletePortfolio(portfolioId: number): Promise<void> {
  await apiClient.delete(`/api/v1/portfolios/${portfolioId}`);
}

/**
 * Add holding to portfolio
 * @param portfolioId - Portfolio ID
 * @param request - Holding addition request
 * @returns Created holding
 */
export async function addHolding(
  portfolioId: number,
  request: AddHoldingRequest
): Promise<PortfolioHolding> {
  const response = await apiClient.post<PortfolioHolding>(
    `/api/v1/portfolios/${portfolioId}/holdings`,
    request
  );
  return response.data;
}

/**
 * Update holding
 * @param portfolioId - Portfolio ID
 * @param holdingId - Holding ID
 * @param request - Holding update request
 * @returns Updated holding
 */
export async function updateHolding(
  portfolioId: number,
  holdingId: number,
  request: UpdateHoldingRequest
): Promise<PortfolioHolding> {
  const response = await apiClient.put<PortfolioHolding>(
    `/api/v1/portfolios/${portfolioId}/holdings/${holdingId}`,
    request
  );
  return response.data;
}

/**
 * Delete holding
 * @param portfolioId - Portfolio ID
 * @param holdingId - Holding ID
 */
export async function deleteHolding(portfolioId: number, holdingId: number): Promise<void> {
  await apiClient.delete(`/api/v1/portfolios/${portfolioId}/holdings/${holdingId}`);
}

/**
 * Recalculate portfolio with current market prices
 * @param portfolioId - Portfolio ID
 * @returns Recalculated portfolio
 */
export async function recalculatePortfolio(portfolioId: number): Promise<Portfolio> {
  const response = await apiClient.post<Portfolio>(
    `/api/v1/portfolios/${portfolioId}/recalculate`
  );
  return response.data;
}
