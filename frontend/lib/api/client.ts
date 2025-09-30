/**
 * API 클라이언트
 *
 * 백엔드 API와 통신하는 기본 클라이언트
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';

interface RequestOptions extends RequestInit {
  requiresAuth?: boolean;
}

/**
 * API 요청 함수
 */
export async function apiRequest<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { requiresAuth = false, headers = {}, ...fetchOptions } = options;

  // 헤더 설정
  const requestHeaders: HeadersInit = {
    'Content-Type': 'application/json',
    ...headers,
  };

  // 인증이 필요한 경우 토큰 추가
  if (requiresAuth) {
    const token = getAccessToken();
    if (token) {
      requestHeaders['Authorization'] = `Bearer ${token}`;
    }
  }

  // 요청 URL
  const url = `${API_BASE_URL}${endpoint}`;

  try {
    const response = await fetch(url, {
      ...fetchOptions,
      headers: requestHeaders,
    });

    // 401 Unauthorized - 토큰 갱신 시도
    if (response.status === 401 && requiresAuth) {
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        // 토큰 갱신 성공 - 재시도
        const newToken = getAccessToken();
        if (newToken) {
          requestHeaders['Authorization'] = `Bearer ${newToken}`;
        }
        const retryResponse = await fetch(url, {
          ...fetchOptions,
          headers: requestHeaders,
        });
        return handleResponse<T>(retryResponse);
      } else {
        // 토큰 갱신 실패 - 로그아웃
        clearTokens();
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
        throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
      }
    }

    return handleResponse<T>(response);
  } catch (error) {
    console.error(`[API Error] ${endpoint}:`, error);
    throw error;
  }
}

/**
 * 응답 처리
 */
async function handleResponse<T>(response: Response): Promise<T> {
  // 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  const contentType = response.headers.get('content-type');
  const isJson = contentType?.includes('application/json');

  // JSON 응답
  if (isJson) {
    const data = await response.json();

    if (!response.ok) {
      throw new ApiError(
        data.message || response.statusText,
        response.status,
        data
      );
    }

    return data;
  }

  // 텍스트 응답
  const text = await response.text();
  if (!response.ok) {
    throw new ApiError(text || response.statusText, response.status);
  }

  return text as unknown as T;
}

/**
 * API 에러 클래스
 */
export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public data?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * 토큰 관리 함수들
 */

export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('access_token');
}

export function getRefreshToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('refresh_token');
}

export function setTokens(accessToken: string, refreshToken: string): void {
  if (typeof window === 'undefined') return;
  localStorage.setItem('access_token', accessToken);
  localStorage.setItem('refresh_token', refreshToken);
}

export function clearTokens(): void {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
  localStorage.removeItem('user');
}

/**
 * 토큰 갱신
 */
async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) return false;

    const data = await response.json();
    setTokens(data.accessToken, data.refreshToken);
    return true;
  } catch (error) {
    console.error('[Token Refresh Error]:', error);
    return false;
  }
}

/**
 * 편의 함수들
 */

export async function get<T>(endpoint: string, requiresAuth = false): Promise<T> {
  return apiRequest<T>(endpoint, { method: 'GET', requiresAuth });
}

export async function post<T>(
  endpoint: string,
  data?: any,
  requiresAuth = false
): Promise<T> {
  return apiRequest<T>(endpoint, {
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined,
    requiresAuth,
  });
}

export async function put<T>(
  endpoint: string,
  data?: any,
  requiresAuth = false
): Promise<T> {
  return apiRequest<T>(endpoint, {
    method: 'PUT',
    body: data ? JSON.stringify(data) : undefined,
    requiresAuth,
  });
}

export async function del<T>(
  endpoint: string,
  requiresAuth = false
): Promise<T> {
  return apiRequest<T>(endpoint, { method: 'DELETE', requiresAuth });
}