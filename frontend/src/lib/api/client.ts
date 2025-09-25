import axios, {
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  InternalAxiosRequestConfig
} from 'axios';
import { ApiClientConfig, ApiError, TokenStorage, RefreshTokenRequest } from '@/types/api';

// API Client Configuration
const API_CONFIG: ApiClientConfig = {
  baseURL: process.env.NODE_ENV === 'production'
    ? process.env.NEXT_PUBLIC_API_URL || 'https://api.sentinel.com'
    : 'http://localhost:8081',
  timeout: 10000,
  withCredentials: false,
};

// Token Management Class
class TokenManager {
  private static readonly STORAGE_KEY = 'sentinel_tokens';

  static getTokens(): TokenStorage | null {
    if (typeof window === 'undefined') return null;

    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (!stored) return null;

      const tokens: TokenStorage = JSON.parse(stored);

      // Check if token is expired
      if (tokens.expiresAt && Date.now() > tokens.expiresAt) {
        this.clearTokens();
        return null;
      }

      return tokens;
    } catch (error) {
      console.error('Failed to parse stored tokens:', error);
      this.clearTokens();
      return null;
    }
  }

  static setTokens(
    accessToken: string,
    refreshToken: string,
    tokenType: string = 'Bearer',
    expiresIn: number = 3600
  ): void {
    if (typeof window === 'undefined') return;

    const tokens: TokenStorage = {
      accessToken,
      refreshToken,
      tokenType,
      expiresIn,
      expiresAt: Date.now() + (expiresIn * 1000),
    };

    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(tokens));
    } catch (error) {
      console.error('Failed to store tokens:', error);
    }
  }

  static clearTokens(): void {
    if (typeof window === 'undefined') return;

    try {
      localStorage.removeItem(this.STORAGE_KEY);
    } catch (error) {
      console.error('Failed to clear tokens:', error);
    }
  }

  static getAuthHeader(): string | null {
    const tokens = this.getTokens();
    return tokens ? `${tokens.tokenType} ${tokens.accessToken}` : null;
  }

  static isTokenExpired(): boolean {
    const tokens = this.getTokens();
    return !tokens || (tokens.expiresAt && Date.now() > tokens.expiresAt);
  }
}

// Create Axios Instance
const createApiClient = (): AxiosInstance => {
  const client = axios.create(API_CONFIG);

  // Request Interceptor - Add auth token
  client.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const authHeader = TokenManager.getAuthHeader();
      if (authHeader) {
        config.headers.Authorization = authHeader;
      }

      // Add request timestamp
      config.metadata = { startTime: Date.now() };

      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, {
        headers: config.headers,
        data: config.data,
      });

      return config;
    },
    (error) => {
      console.error('[API Request Error]', error);
      return Promise.reject(error);
    }
  );

  // Response Interceptor - Handle auth and errors
  client.interceptors.response.use(
    (response: AxiosResponse) => {
      const duration = Date.now() - (response.config.metadata?.startTime || 0);

      console.log(`[API Response] ${response.status} ${response.config.method?.toUpperCase()} ${response.config.url}`, {
        duration: `${duration}ms`,
        data: response.data,
      });

      return response;
    },
    async (error) => {
      const originalRequest = error.config;

      // Handle 401 Unauthorized - Token refresh
      if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;

        try {
          const tokens = TokenManager.getTokens();
          if (tokens?.refreshToken) {
            console.log('[API] Attempting token refresh...');

            // Create a separate client for refresh to avoid infinite loops
            const refreshClient = axios.create({ baseURL: API_CONFIG.baseURL });

            const refreshResponse = await refreshClient.post('/api/v1/auth/refresh', {
              refreshToken: tokens.refreshToken,
            } as RefreshTokenRequest);

            const { accessToken, refreshToken, tokenType, expiresIn } = refreshResponse.data;

            TokenManager.setTokens(accessToken, refreshToken, tokenType, expiresIn);

            // Retry original request with new token
            originalRequest.headers.Authorization = `${tokenType} ${accessToken}`;
            return client(originalRequest);
          }
        } catch (refreshError) {
          console.error('[API] Token refresh failed:', refreshError);
          TokenManager.clearTokens();

          // Redirect to login page or trigger auth flow
          if (typeof window !== 'undefined') {
            window.location.href = '/auth/login';
          }
        }
      }

      // Handle other errors
      const apiError: ApiError = {
        status: error.response?.status || 500,
        message: error.response?.data?.message || error.message || 'An unexpected error occurred',
        timestamp: new Date().toISOString(),
        path: error.config?.url,
      };

      console.error('[API Error]', {
        status: apiError.status,
        message: apiError.message,
        url: error.config?.url,
        method: error.config?.method,
      });

      return Promise.reject(apiError);
    }
  );

  return client;
};

// Create singleton instance
export const apiClient = createApiClient();

// Export token manager for external use
export { TokenManager };

// Utility functions
export const isApiError = (error: any): error is ApiError => {
  return error && typeof error.status === 'number' && typeof error.message === 'string';
};

export const handleApiError = (error: unknown): ApiError => {
  if (isApiError(error)) {
    return error;
  }

  return {
    status: 500,
    message: 'An unexpected error occurred',
    timestamp: new Date().toISOString(),
  };
};

// Request retry utility
export const withRetry = async <T>(
  request: () => Promise<T>,
  maxRetries: number = 3,
  delay: number = 1000
): Promise<T> => {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await request();
    } catch (error) {
      if (i === maxRetries - 1) throw error;

      const isRetryableError = isApiError(error) &&
        [408, 429, 500, 502, 503, 504].includes(error.status);

      if (!isRetryableError) throw error;

      console.log(`[API] Retrying request in ${delay}ms... (${i + 1}/${maxRetries})`);
      await new Promise(resolve => setTimeout(resolve, delay * Math.pow(2, i)));
    }
  }

  throw new Error('Max retries exceeded');
};

// Extend AxiosRequestConfig to include metadata
declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    metadata?: {
      startTime: number;
    };
    _retry?: boolean;
  }
}