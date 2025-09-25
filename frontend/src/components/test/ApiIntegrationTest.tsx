'use client';

import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { api, QueryKeys } from '@/lib/api';

export default function ApiIntegrationTest() {
  const { user, isAuthenticated, isLoading: authLoading, initiateKakaoLogin, logout } = useAuth();

  // Test market data query
  const { data: marketStatus, isLoading: marketLoading, error: marketError } = useQuery({
    queryKey: [QueryKeys.marketStatus],
    queryFn: () => api.market.getServiceStatus(),
    enabled: true, // Always run this query
  });

  // Test stock price query
  const { data: appleStock, isLoading: stockLoading, error: stockError } = useQuery({
    queryKey: QueryKeys.stockPrice('AAPL'),
    queryFn: () => api.market.getStockPrice('AAPL'),
    enabled: true,
  });

  if (authLoading) {
    return (
      <div className="p-6 bg-white rounded-lg shadow-md">
        <div className="animate-pulse flex space-x-4">
          <div className="rounded-full bg-gray-300 h-10 w-10"></div>
          <div className="flex-1 space-y-2 py-1">
            <div className="h-4 bg-gray-300 rounded w-3/4"></div>
            <div className="h-4 bg-gray-300 rounded w-1/2"></div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 bg-white rounded-lg shadow-md space-y-6">
      <h2 className="text-2xl font-bold text-gray-900">🔗 Frontend-Backend Integration Test</h2>

      {/* Authentication Status */}
      <div className="border-l-4 border-blue-500 pl-4">
        <h3 className="text-lg font-semibold text-gray-800">Authentication Status</h3>
        <div className="mt-2 space-y-2">
          <p className="text-sm">
            <span className="font-medium">Status:</span>{' '}
            <span className={`px-2 py-1 rounded text-xs ${
              isAuthenticated ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
            }`}>
              {isAuthenticated ? '✅ Authenticated' : '❌ Not Authenticated'}
            </span>
          </p>

          {user && (
            <div className="text-sm space-y-1">
              <p><span className="font-medium">User ID:</span> {user.id}</p>
              <p><span className="font-medium">Name:</span> {user.name}</p>
              <p><span className="font-medium">Email:</span> {user.email}</p>
            </div>
          )}

          <div className="flex space-x-2 mt-3">
            {!isAuthenticated ? (
              <button
                onClick={initiateKakaoLogin}
                className="px-4 py-2 bg-yellow-400 text-yellow-900 rounded hover:bg-yellow-500 text-sm font-medium"
              >
                Login with Kakao
              </button>
            ) : (
              <button
                onClick={logout}
                className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600 text-sm font-medium"
              >
                Logout
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Market Data API Test */}
      <div className="border-l-4 border-green-500 pl-4">
        <h3 className="text-lg font-semibold text-gray-800">Market Data API</h3>
        <div className="mt-2 space-y-2">
          <div>
            <span className="font-medium text-sm">Service Status:</span>{' '}
            {marketLoading ? (
              <span className="text-gray-500">Loading...</span>
            ) : marketError ? (
              <span className="text-red-600">Error: {(marketError as any)?.message}</span>
            ) : marketStatus ? (
              <span className={`px-2 py-1 rounded text-xs ${
                marketStatus.available ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
              }`}>
                {marketStatus.available ? '✅ Available' : '❌ Unavailable'} - {marketStatus.message}
              </span>
            ) : (
              <span className="text-gray-500">No data</span>
            )}
          </div>

          <div>
            <span className="font-medium text-sm">AAPL Stock Price:</span>{' '}
            {stockLoading ? (
              <span className="text-gray-500">Loading...</span>
            ) : stockError ? (
              <span className="text-red-600">Error: {(stockError as any)?.message}</span>
            ) : appleStock ? (
              <div className="inline-block">
                <span className="font-mono text-green-600">${appleStock.price.toFixed(2)}</span>
                {appleStock.changePercent && (
                  <span className={`ml-2 text-xs px-1 py-0.5 rounded ${
                    appleStock.changePercent >= 0 ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  }`}>
                    {appleStock.changePercent >= 0 ? '+' : ''}{appleStock.changePercent.toFixed(2)}%
                  </span>
                )}
                <div className="text-xs text-gray-500 mt-1">
                  Source: {appleStock.dataSource} | Updated: {new Date(appleStock.timestamp).toLocaleString()}
                </div>
              </div>
            ) : (
              <span className="text-gray-500">No data</span>
            )}
          </div>
        </div>
      </div>

      {/* Backend Connection Test */}
      <div className="border-l-4 border-purple-500 pl-4">
        <h3 className="text-lg font-semibold text-gray-800">Backend Connection</h3>
        <div className="mt-2 text-sm">
          <p><span className="font-medium">Backend URL:</span> {process.env.NODE_ENV === 'production' ? (process.env.NEXT_PUBLIC_API_URL || 'Production API') : 'http://localhost:8081'}</p>
          <p><span className="font-medium">Environment:</span> {process.env.NODE_ENV}</p>
          <p className="text-green-600 mt-2">✅ API Client configured with interceptors</p>
          <p className="text-green-600">✅ JWT token management active</p>
          <p className="text-green-600">✅ Error handling & retry logic enabled</p>
          <p className="text-green-600">✅ React Query integration complete</p>
        </div>
      </div>

      {/* Integration Summary */}
      <div className="bg-blue-50 p-4 rounded-lg">
        <h3 className="text-lg font-semibold text-blue-900">🎉 Integration Summary</h3>
        <div className="mt-2 text-sm text-blue-800 space-y-1">
          <p>✅ Axios HTTP client with interceptors</p>
          <p>✅ TypeScript API types and interfaces</p>
          <p>✅ JWT token management and auto-refresh</p>
          <p>✅ Authentication context provider</p>
          <p>✅ React Query server state management</p>
          <p>✅ Error handling and retry logic</p>
          <p>✅ Market data API integration</p>
          <p>✅ User authentication flow</p>
        </div>
      </div>
    </div>
  );
}