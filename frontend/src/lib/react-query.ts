import { QueryClient, DefaultOptions } from '@tanstack/react-query';
import { handleApiError, isApiError } from './api/client';

// Default React Query options
const queryConfig: DefaultOptions = {
  queries: {
    // Default stale time (5 minutes)
    staleTime: 5 * 60 * 1000,
    // Default cache time (10 minutes)
    gcTime: 10 * 60 * 1000,
    // Retry configuration
    retry: (failureCount, error) => {
      // Don't retry on 4xx errors (client errors)
      if (isApiError(error) && error.status >= 400 && error.status < 500) {
        return false;
      }
      // Retry up to 3 times for other errors
      return failureCount < 3;
    },
    // Retry delay with exponential backoff
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    // Refetch on window focus for critical data
    refetchOnWindowFocus: (query) => {
      // Only refetch critical queries
      const criticalQueries = ['current-user', 'portfolios', 'market-status'];
      return criticalQueries.some(key =>
        query.queryKey.includes(key)
      );
    },
    // Don't refetch on reconnect for non-critical data
    refetchOnReconnect: 'always',
  },
  mutations: {
    // Retry mutations once
    retry: 1,
    // Mutation error handling
    onError: (error) => {
      const apiError = handleApiError(error);
      console.error('[Mutation Error]', apiError);
    },
  },
};

// Create QueryClient instance
export const queryClient = new QueryClient({
  defaultOptions: queryConfig,
});

// Query invalidation helpers
export const queryInvalidation = {
  /**
   * Invalidate all user-related queries
   */
  invalidateUserQueries: () => {
    queryClient.invalidateQueries({ queryKey: ['current-user'] });
  },

  /**
   * Invalidate portfolio queries for a specific user
   */
  invalidatePortfolioQueries: (userId: number) => {
    queryClient.invalidateQueries({ queryKey: ['portfolios', userId] });
    queryClient.invalidateQueries({
      predicate: (query) =>
        query.queryKey[0] === 'portfolio' &&
        query.queryKey.includes(userId)
    });
  },

  /**
   * Invalidate specific portfolio query
   */
  invalidatePortfolio: (portfolioId: number, userId: number) => {
    queryClient.invalidateQueries({
      queryKey: ['portfolio', portfolioId, userId]
    });
    queryClient.invalidateQueries({
      queryKey: ['portfolio-holdings', portfolioId]
    });
  },

  /**
   * Invalidate market data queries
   */
  invalidateMarketQueries: () => {
    queryClient.invalidateQueries({
      predicate: (query) =>
        query.queryKey[0] === 'stock-price' ||
        query.queryKey[0] === 'stock-prices' ||
        query.queryKey[0] === 'market-status'
    });
  },

  /**
   * Invalidate specific stock price
   */
  invalidateStockPrice: (symbol: string) => {
    queryClient.invalidateQueries({
      queryKey: ['stock-price', symbol]
    });
  },

  /**
   * Clear all cached data (use sparingly)
   */
  clearAllQueries: () => {
    queryClient.clear();
  },
};

// Prefetch helpers
export const queryPrefetch = {
  /**
   * Prefetch user data after login
   */
  prefetchUserData: async (userId: number) => {
    const { api } = await import('./api');

    await Promise.allSettled([
      // Prefetch user portfolios
      queryClient.prefetchQuery({
        queryKey: ['portfolios', userId],
        queryFn: () => api.portfolio.getPortfolios(userId),
        staleTime: 5 * 60 * 1000,
      }),
      // Prefetch market status
      queryClient.prefetchQuery({
        queryKey: ['market-status'],
        queryFn: () => api.market.getServiceStatus(),
        staleTime: 2 * 60 * 1000,
      }),
    ]);
  },

  /**
   * Prefetch common stock prices
   */
  prefetchCommonStocks: async () => {
    const { api } = await import('./api');
    const commonSymbols = ['AAPL', 'MSFT', 'GOOGL', 'TSLA', 'NVDA'];

    await Promise.allSettled(
      commonSymbols.map(symbol =>
        queryClient.prefetchQuery({
          queryKey: ['stock-price', symbol],
          queryFn: () => api.market.getStockPrice(symbol),
          staleTime: 60 * 1000, // 1 minute for stock prices
        })
      )
    );
  },
};

// Error boundary helpers
export const queryErrorHandling = {
  /**
   * Global error handler for queries
   */
  handleQueryError: (error: unknown, queryKey: unknown[]) => {
    const apiError = handleApiError(error);

    // Log error with query context
    console.error('[Query Error]', {
      queryKey,
      error: apiError,
    });

    // Handle specific error types
    if (apiError.status === 401) {
      // Unauthorized - clear user data and redirect
      queryInvalidation.invalidateUserQueries();
      if (typeof window !== 'undefined') {
        window.location.href = '/auth/login';
      }
    } else if (apiError.status >= 500) {
      // Server errors - show global error message
      // You might want to integrate with a toast library here
      console.error('Server error occurred:', apiError.message);
    }

    return apiError;
  },

  /**
   * Check if error is a network error
   */
  isNetworkError: (error: unknown): boolean => {
    return isApiError(error) &&
           (error.status === 0 || error.message.includes('Network Error'));
  },

  /**
   * Check if error is a client error (4xx)
   */
  isClientError: (error: unknown): boolean => {
    return isApiError(error) && error.status >= 400 && error.status < 500;
  },

  /**
   * Check if error is a server error (5xx)
   */
  isServerError: (error: unknown): boolean => {
    return isApiError(error) && error.status >= 500;
  },
};

// Performance optimization helpers
export const queryOptimization = {
  /**
   * Set query data without triggering a refetch
   */
  setQueryData: <T>(queryKey: unknown[], data: T) => {
    queryClient.setQueryData(queryKey, data);
  },

  /**
   * Get cached query data
   */
  getQueryData: <T>(queryKey: unknown[]): T | undefined => {
    return queryClient.getQueryData(queryKey);
  },

  /**
   * Optimistically update query data
   */
  optimisticUpdate: <T>(
    queryKey: unknown[],
    updater: (old: T | undefined) => T
  ) => {
    queryClient.setQueryData(queryKey, updater);
  },

  /**
   * Cancel outgoing queries (useful for cleanup)
   */
  cancelQueries: (queryKey?: unknown[]) => {
    if (queryKey) {
      return queryClient.cancelQueries({ queryKey });
    }
    return queryClient.cancelQueries();
  },
};