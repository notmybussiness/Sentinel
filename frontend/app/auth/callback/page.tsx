"use client";

import React, { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { handleKakaoCallback } from '@/lib/api/auth';
import { FullPageSpinner } from '@/components/ui/Spinner';

export default function AuthCallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const code = searchParams.get('code');
    const errorParam = searchParams.get('error');

    // 에러 파라미터가 있는 경우
    if (errorParam) {
      setError('로그인이 취소되었거나 오류가 발생했습니다.');
      setTimeout(() => {
        router.push('/login');
      }, 2000);
      return;
    }

    // 인증 코드가 없는 경우
    if (!code) {
      setError('인증 코드를 찾을 수 없습니다.');
      setTimeout(() => {
        router.push('/login');
      }, 2000);
      return;
    }

    // Kakao 로그인 처리
    handleLogin(code);
  }, [searchParams, router]);

  const handleLogin = async (code: string) => {
    try {
      await handleKakaoCallback(code);

      // 로그인 성공 - 홈으로 리다이렉트
      router.push('/');
      router.refresh();
    } catch (error) {
      console.error('로그인 처리 오류:', error);
      setError('로그인 처리 중 오류가 발생했습니다.');
      setTimeout(() => {
        router.push('/login');
      }, 2000);
    }
  };

  if (error) {
    return (
      <div className="min-h-screen bg-background-primary flex items-center justify-center">
        <div className="text-center">
          <div className="text-semantic-error text-2xl mb-4">⚠️</div>
          <h2 className="text-text-primary text-xl font-semibold mb-2">
            로그인 실패
          </h2>
          <p className="text-text-tertiary">{error}</p>
          <p className="text-text-quaternary text-small mt-2">
            잠시 후 로그인 페이지로 이동합니다...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background-primary flex items-center justify-center">
      <div className="text-center">
        <FullPageSpinner />
        <p className="text-text-secondary mt-4">로그인 처리 중...</p>
      </div>
    </div>
  );
}