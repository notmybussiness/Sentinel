import { apiClient, withRetry } from './client';
import {
  Portfolio,
  PortfolioHolding,
  CreatePortfolioRequest,
  UpdatePortfolioRequest,
  AddHoldingRequest,
  UpdateHoldingRequest
} from '@/types/api';

// Portfolio API Service
export const portfolioApi = {
  /**
   * Get all portfolios for a user
   */
  getPortfolios: async (userId: number): Promise<Portfolio[]> => {
    const response = await withRetry(
      () => apiClient.get<Portfolio[]>(`/api/v1/portfolios?userId=${userId}`)
    );
    return response.data;
  },

  /**
   * Get a specific portfolio
   */
  getPortfolio: async (portfolioId: number, userId: number): Promise<Portfolio> => {
    const response = await withRetry(
      () => apiClient.get<Portfolio>(`/api/v1/portfolios/${portfolioId}?userId=${userId}`)
    );
    return response.data;
  },

  /**
   * Create a new portfolio
   */
  createPortfolio: async (userId: number, request: CreatePortfolioRequest): Promise<Portfolio> => {
    const response = await apiClient.post<Portfolio>(
      `/api/v1/portfolios?userId=${userId}`,
      request
    );
    return response.data;
  },

  /**
   * Update a portfolio
   */
  updatePortfolio: async (
    portfolioId: number,
    userId: number,
    request: UpdatePortfolioRequest
  ): Promise<Portfolio> => {
    const response = await apiClient.put<Portfolio>(
      `/api/v1/portfolios/${portfolioId}?userId=${userId}`,
      request
    );
    return response.data;
  },

  /**
   * Delete a portfolio
   */
  deletePortfolio: async (portfolioId: number, userId: number): Promise<void> => {
    await apiClient.delete(`/api/v1/portfolios/${portfolioId}?userId=${userId}`);
  },

  /**
   * Add a new holding to portfolio
   */
  addHolding: async (
    portfolioId: number,
    userId: number,
    request: AddHoldingRequest
  ): Promise<PortfolioHolding> => {
    const response = await apiClient.post<PortfolioHolding>(
      `/api/v1/portfolios/${portfolioId}/holdings?userId=${userId}`,
      request
    );
    return response.data;
  },

  /**
   * Update a holding
   */
  updateHolding: async (
    portfolioId: number,
    holdingId: number,
    userId: number,
    request: UpdateHoldingRequest
  ): Promise<PortfolioHolding> => {
    const response = await apiClient.put<PortfolioHolding>(
      `/api/v1/portfolios/${portfolioId}/holdings/${holdingId}?userId=${userId}`,
      request
    );
    return response.data;
  },

  /**
   * Delete a holding
   */
  deleteHolding: async (
    portfolioId: number,
    holdingId: number,
    userId: number
  ): Promise<void> => {
    await apiClient.delete(
      `/api/v1/portfolios/${portfolioId}/holdings/${holdingId}?userId=${userId}`
    );
  },

  /**
   * Recalculate portfolio with current market prices
   */
  recalculatePortfolio: async (portfolioId: number, userId: number): Promise<Portfolio> => {
    const response = await apiClient.post<Portfolio>(
      `/api/v1/portfolios/${portfolioId}/recalculate?userId=${userId}`
    );
    return response.data;
  },
};

// Portfolio Helper Functions
export const portfolioHelpers = {
  /**
   * Calculate total portfolio value
   */
  calculateTotalValue: (holdings: PortfolioHolding[]): number => {
    return holdings.reduce((total, holding) => total + (holding.marketValue || 0), 0);
  },

  /**
   * Calculate total cost basis
   */
  calculateTotalCost: (holdings: PortfolioHolding[]): number => {
    return holdings.reduce((total, holding) => total + holding.totalCost, 0);
  },

  /**
   * Calculate total gain/loss
   */
  calculateTotalGainLoss: (holdings: PortfolioHolding[]): number => {
    const totalValue = portfolioHelpers.calculateTotalValue(holdings);
    const totalCost = portfolioHelpers.calculateTotalCost(holdings);
    return totalValue - totalCost;
  },

  /**
   * Calculate total gain/loss percentage
   */
  calculateTotalGainLossPercent: (holdings: PortfolioHolding[]): number => {
    const totalCost = portfolioHelpers.calculateTotalCost(holdings);
    if (totalCost === 0) return 0;

    const totalGainLoss = portfolioHelpers.calculateTotalGainLoss(holdings);
    return (totalGainLoss / totalCost) * 100;
  },

  /**
   * Calculate asset allocation percentages
   */
  calculateAssetAllocation: (holdings: PortfolioHolding[]): Record<string, number> => {
    const totalValue = portfolioHelpers.calculateTotalValue(holdings);
    if (totalValue === 0) return {};

    return holdings.reduce((allocation, holding) => {
      const percentage = ((holding.marketValue || 0) / totalValue) * 100;
      allocation[holding.symbol] = percentage;
      return allocation;
    }, {} as Record<string, number>);
  },

  /**
   * Get top performers
   */
  getTopPerformers: (holdings: PortfolioHolding[], limit: number = 5): PortfolioHolding[] => {
    return holdings
      .filter(holding => holding.gainLossPercent !== undefined)
      .sort((a, b) => (b.gainLossPercent || 0) - (a.gainLossPercent || 0))
      .slice(0, limit);
  },

  /**
   * Get worst performers
   */
  getWorstPerformers: (holdings: PortfolioHolding[], limit: number = 5): PortfolioHolding[] => {
    return holdings
      .filter(holding => holding.gainLossPercent !== undefined)
      .sort((a, b) => (a.gainLossPercent || 0) - (b.gainLossPercent || 0))
      .slice(0, limit);
  },

  /**
   * Format currency amount
   */
  formatCurrency: (amount: number, currency: string = 'USD'): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  },

  /**
   * Format percentage with color indication
   */
  formatGainLossPercent: (percent: number): { text: string; color: string } => {
    const text = `${percent >= 0 ? '+' : ''}${percent.toFixed(2)}%`;
    const color = percent > 0 ? 'text-green-600' : percent < 0 ? 'text-red-600' : 'text-gray-600';
    return { text, color };
  },

  /**
   * Validate portfolio name
   */
  isValidPortfolioName: (name: string): boolean => {
    return name.trim().length >= 1 && name.trim().length <= 255;
  },

  /**
   * Validate holding data
   */
  validateHolding: (holding: AddHoldingRequest | UpdateHoldingRequest): string[] => {
    const errors: string[] = [];

    if ('symbol' in holding) {
      if (!holding.symbol || holding.symbol.trim().length === 0) {
        errors.push('Symbol is required');
      } else if (!/^[A-Z]{1,10}$/.test(holding.symbol.toUpperCase())) {
        errors.push('Symbol must be 1-10 uppercase letters');
      }
    }

    if ('quantity' in holding && holding.quantity !== undefined) {
      if (holding.quantity <= 0) {
        errors.push('Quantity must be greater than 0');
      }
    }

    if ('averageCost' in holding && holding.averageCost !== undefined) {
      if (holding.averageCost <= 0) {
        errors.push('Average cost must be greater than 0');
      }
    }

    return errors;
  },

  /**
   * Calculate rebalancing suggestions
   */
  calculateRebalancingSuggestions: (
    holdings: PortfolioHolding[],
    targetAllocations: Record<string, number>
  ): Record<string, { current: number; target: number; action: 'buy' | 'sell' | 'hold' }> => {
    const currentAllocations = portfolioHelpers.calculateAssetAllocation(holdings);
    const suggestions: Record<string, { current: number; target: number; action: 'buy' | 'sell' | 'hold' }> = {};

    Object.keys(targetAllocations).forEach(symbol => {
      const current = currentAllocations[symbol] || 0;
      const target = targetAllocations[symbol];
      const diff = current - target;

      suggestions[symbol] = {
        current,
        target,
        action: Math.abs(diff) <= 1 ? 'hold' : diff > 0 ? 'sell' : 'buy'
      };
    });

    return suggestions;
  },

  /**
   * Export portfolio data to CSV
   */
  exportToCsv: (portfolio: Portfolio): string => {
    if (!portfolio.holdings || portfolio.holdings.length === 0) {
      return 'No holdings to export';
    }

    const headers = [
      'Symbol',
      'Quantity',
      'Average Cost',
      'Current Price',
      'Total Cost',
      'Market Value',
      'Gain/Loss',
      'Gain/Loss %'
    ];

    const rows = portfolio.holdings.map(holding => [
      holding.symbol,
      holding.quantity.toString(),
      holding.averageCost.toFixed(2),
      (holding.currentPrice || 0).toFixed(2),
      holding.totalCost.toFixed(2),
      (holding.marketValue || 0).toFixed(2),
      (holding.gainLoss || 0).toFixed(2),
      (holding.gainLossPercent || 0).toFixed(2)
    ]);

    return [headers, ...rows].map(row => row.join(',')).join('\n');
  },
};