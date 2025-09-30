/**
 * 인증 API
 *
 * Kakao OAuth 로그인 및 토큰 관리
 */

import { post, setTokens, clearTokens, getAccessToken } from './client';

export interface User {
  id: number;
  email: string;
  name: string;
  nickname?: string;
  profileImageUrl?: string;
  provider: 'KAKAO';
  createdAt: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

/**
 * Kakao 로그인 URL 생성
 */
export function getKakaoLoginUrl(): string {
  const clientId = process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID;
  const redirectUri = process.env.NEXT_PUBLIC_KAKAO_REDIRECT_URI;

  if (!clientId || !redirectUri) {
    throw new Error('Kakao OAuth 설정이 없습니다.');
  }

  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: 'code',
  });

  return `https://kauth.kakao.com/oauth/authorize?${params.toString()}`;
}

/**
 * Kakao 로그인 콜백 처리
 */
export async function handleKakaoCallback(code: string): Promise<LoginResponse> {
  try {
    const response = await post<LoginResponse>('/api/v1/auth/kakao', { code });

    // 토큰 저장
    setTokens(response.accessToken, response.refreshToken);

    // 사용자 정보 저장
    if (typeof window !== 'undefined') {
      localStorage.setItem('user', JSON.stringify(response.user));
    }

    return response;
  } catch (error) {
    console.error('[Kakao Login Error]:', error);
    throw error;
  }
}

/**
 * 로그아웃
 */
export async function logout(): Promise<void> {
  try {
    // 백엔드에 로그아웃 요청 (선택사항)
    await post('/api/v1/auth/logout', {}, true);
  } catch (error) {
    console.error('[Logout Error]:', error);
  } finally {
    // 로컬 토큰 삭제
    clearTokens();

    // 로그인 페이지로 리다이렉트
    if (typeof window !== 'undefined') {
      window.location.href = '/login';
    }
  }
}

/**
 * 현재 로그인된 사용자 정보 가져오기
 */
export function getCurrentUser(): User | null {
  if (typeof window === 'undefined') return null;

  const userStr = localStorage.getItem('user');
  if (!userStr) return null;

  try {
    return JSON.parse(userStr);
  } catch {
    return null;
  }
}

/**
 * 로그인 상태 확인
 */
export function isAuthenticated(): boolean {
  return !!getAccessToken();
}

/**
 * 개발 모드 로그인 (테스트용)
 */
export async function devLogin(): Promise<LoginResponse> {
  if (process.env.NEXT_PUBLIC_DEV_MODE !== 'true') {
    throw new Error('개발 모드가 아닙니다.');
  }

  // Mock 사용자 데이터
  const mockUser: User = {
    id: 1,
    email: 'dev@sentinel.com',
    name: '개발자',
    nickname: 'Developer',
    profileImageUrl: '',
    provider: 'KAKAO',
    createdAt: new Date().toISOString(),
  };

  const mockResponse: LoginResponse = {
    accessToken: 'dev-access-token-' + Date.now(),
    refreshToken: 'dev-refresh-token-' + Date.now(),
    user: mockUser,
  };

  // 토큰 저장
  setTokens(mockResponse.accessToken, mockResponse.refreshToken);

  // 사용자 정보 저장
  if (typeof window !== 'undefined') {
    localStorage.setItem('user', JSON.stringify(mockResponse.user));
  }

  return mockResponse;
}