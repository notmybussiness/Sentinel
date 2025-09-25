import { apiClient, TokenManager, withRetry } from './client';
import { LoginResponse, RefreshTokenRequest, User } from '@/types/api';

// Authentication API Service
export const authApi = {
  /**
   * Get Kakao login URL
   */
  getKakaoLoginUrl: async (): Promise<string> => {
    const response = await withRetry(
      () => apiClient.get<string>('/api/v1/auth/kakao')
    );
    return response.data;
  },

  /**
   * Handle Kakao OAuth callback
   */
  handleKakaoCallback: async (code: string): Promise<LoginResponse> => {
    const response = await withRetry(
      () => apiClient.get<LoginResponse>(`/api/v1/auth/kakao/callback?code=${code}`)
    );

    // Store tokens after successful login
    const { accessToken, refreshToken, tokenType, expiresIn } = response.data;
    TokenManager.setTokens(accessToken, refreshToken, tokenType, expiresIn);

    return response.data;
  },

  /**
   * Refresh access token
   */
  refreshToken: async (refreshTokenData: RefreshTokenRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/api/v1/auth/refresh', refreshTokenData);

    // Update stored tokens
    const { accessToken, refreshToken, tokenType, expiresIn } = response.data;
    TokenManager.setTokens(accessToken, refreshToken, tokenType, expiresIn);

    return response.data;
  },

  /**
   * Logout user
   */
  logout: async (): Promise<void> => {
    try {
      await apiClient.post('/api/v1/auth/logout');
    } finally {
      // Always clear tokens, even if request fails
      TokenManager.clearTokens();
    }
  },

  /**
   * Get current user information
   */
  getCurrentUser: async (): Promise<User> => {
    const response = await withRetry(
      () => apiClient.get<User>('/api/v1/auth/me')
    );
    return response.data;
  },

  /**
   * Check if user is authenticated
   */
  isAuthenticated: (): boolean => {
    return !TokenManager.isTokenExpired();
  },

  /**
   * Get current user ID from token (client-side only)
   * Note: In a real app, you might decode JWT on client side or get from user context
   */
  getCurrentUserId: (): number | null => {
    // For now, we'll need to get this from the user context or API call
    // This is a placeholder implementation
    return null;
  },
};

// Auth Helper Functions
export const authHelpers = {
  /**
   * Initiate Kakao login process
   */
  initiateKakaoLogin: async (): Promise<void> => {
    try {
      const loginUrl = await authApi.getKakaoLoginUrl();
      window.location.href = loginUrl;
    } catch (error) {
      console.error('Failed to initiate Kakao login:', error);
      throw error;
    }
  },

  /**
   * Handle authentication redirect
   */
  handleAuthRedirect: async (searchParams: URLSearchParams): Promise<LoginResponse | null> => {
    const code = searchParams.get('code');
    const error = searchParams.get('error');

    if (error) {
      console.error('Authentication error:', error);
      throw new Error(`Authentication failed: ${error}`);
    }

    if (code) {
      try {
        return await authApi.handleKakaoCallback(code);
      } catch (error) {
        console.error('Failed to handle auth callback:', error);
        throw error;
      }
    }

    return null;
  },

  /**
   * Logout and redirect
   */
  logoutAndRedirect: async (redirectPath: string = '/auth/login'): Promise<void> => {
    try {
      await authApi.logout();
    } finally {
      window.location.href = redirectPath;
    }
  },

  /**
   * Check authentication status and redirect if needed
   */
  requireAuth: (redirectPath: string = '/auth/login'): boolean => {
    if (!authApi.isAuthenticated()) {
      if (typeof window !== 'undefined') {
        window.location.href = redirectPath;
      }
      return false;
    }
    return true;
  },
};