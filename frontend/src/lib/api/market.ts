import { apiClient, withRetry } from './client';
import { StockPrice, ServiceStatusResponse } from '@/types/api';

// Market Data API Service
export const marketApi = {
  /**
   * Get current price for a single stock
   */
  getStockPrice: async (symbol: string): Promise<StockPrice> => {
    const response = await withRetry(
      () => apiClient.get<StockPrice>(`/api/v1/market/price/${symbol.toUpperCase()}`)
    );
    return response.data;
  },

  /**
   * Get current prices for multiple stocks
   */
  getStockPrices: async (symbols: string[]): Promise<StockPrice[]> => {
    const symbolString = symbols.map(s => s.toUpperCase()).join(',');
    const response = await withRetry(
      () => apiClient.get<StockPrice[]>(`/api/v1/market/prices?symbols=${symbolString}`)
    );
    return response.data;
  },

  /**
   * Get market data service status
   */
  getServiceStatus: async (): Promise<ServiceStatusResponse> => {
    const response = await withRetry(
      () => apiClient.get<ServiceStatusResponse>('/api/v1/market/status')
    );
    return response.data;
  },
};

// Market Data Helper Functions
export const marketHelpers = {
  /**
   * Format stock price for display
   */
  formatPrice: (price: number, currency: string = 'USD'): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(price);
  },

  /**
   * Format percentage change
   */
  formatPercentage: (percentage: number): string => {
    const sign = percentage >= 0 ? '+' : '';
    return `${sign}${percentage.toFixed(2)}%`;
  },

  /**
   * Get color class for price change
   */
  getPriceChangeColor: (changePercent: number): 'text-green-600' | 'text-red-600' | 'text-gray-600' => {
    if (changePercent > 0) return 'text-green-600';
    if (changePercent < 0) return 'text-red-600';
    return 'text-gray-600';
  },

  /**
   * Format large numbers (volume, market cap)
   */
  formatLargeNumber: (number: number): string => {
    if (number >= 1_000_000_000) {
      return `${(number / 1_000_000_000).toFixed(1)}B`;
    }
    if (number >= 1_000_000) {
      return `${(number / 1_000_000).toFixed(1)}M`;
    }
    if (number >= 1_000) {
      return `${(number / 1_000).toFixed(1)}K`;
    }
    return number.toString();
  },

  /**
   * Validate stock symbol format
   */
  isValidSymbol: (symbol: string): boolean => {
    // Basic validation: 1-10 uppercase letters
    const symbolRegex = /^[A-Z]{1,10}$/;
    return symbolRegex.test(symbol.toUpperCase());
  },

  /**
   * Get time since last update
   */
  getTimeSinceUpdate: (timestamp: string): string => {
    const now = new Date();
    const updateTime = new Date(timestamp);
    const diffMs = now.getTime() - updateTime.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMinutes / 60);

    if (diffMinutes < 1) return 'Just now';
    if (diffMinutes < 60) return `${diffMinutes}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;

    return updateTime.toLocaleDateString();
  },

  /**
   * Check if market data is stale (older than 15 minutes during market hours)
   */
  isDataStale: (timestamp: string, thresholdMinutes: number = 15): boolean => {
    const now = new Date();
    const updateTime = new Date(timestamp);
    const diffMs = now.getTime() - updateTime.getTime();
    const diffMinutes = diffMs / (1000 * 60);

    return diffMinutes > thresholdMinutes;
  },

  /**
   * Batch stock price requests to avoid rate limiting
   */
  batchStockPrices: async (
    symbols: string[],
    batchSize: number = 10
  ): Promise<Record<string, StockPrice>> => {
    const results: Record<string, StockPrice> = {};
    const batches: string[][] = [];

    // Split symbols into batches
    for (let i = 0; i < symbols.length; i += batchSize) {
      batches.push(symbols.slice(i, i + batchSize));
    }

    // Process batches with delay to respect rate limits
    for (let i = 0; i < batches.length; i++) {
      try {
        const batchResults = await marketApi.getStockPrices(batches[i]);
        batchResults.forEach(stock => {
          results[stock.symbol] = stock;
        });

        // Add delay between batches to respect rate limits
        if (i < batches.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 1000));
        }
      } catch (error) {
        console.error(`Failed to fetch batch ${i + 1}:`, error);
        // Continue with next batch instead of failing completely
      }
    }

    return results;
  },
};