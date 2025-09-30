"use client";

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { getKakaoLoginUrl, devLogin } from '@/lib/api/auth';
import { Spinner } from '@/components/ui/Spinner';

export default function LoginPage() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);

  // Kakao 로그인
  const handleKakaoLogin = () => {
    try {
      const loginUrl = getKakaoLoginUrl();
      window.location.href = loginUrl;
    } catch (error) {
      console.error('Kakao 로그인 오류:', error);
      alert('Kakao 로그인 설정을 확인해주세요.');
    }
  };

  // 개발 모드 로그인
  const handleDevLogin = async () => {
    setIsLoading(true);
    try {
      await devLogin();
      router.push('/');
      router.refresh();
    } catch (error) {
      console.error('개발 모드 로그인 오류:', error);
      alert('개발 모드 로그인에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const isDev = process.env.NEXT_PUBLIC_DEV_MODE === 'true';

  return (
    <div className="min-h-screen bg-background-primary flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-text-primary mb-2">
            Sentinel
          </h1>
          <p className="text-text-tertiary text-regular">
            포트폴리오 관리 및 백테스팅 플랫폼
          </p>
        </div>

        <div className="space-y-3">
          {/* Kakao 로그인 */}
          <Button
            variant="primary"
            size="lg"
            className="w-full bg-[#FEE500] hover:bg-[#FDD835] text-[#000000] font-semibold"
            onClick={handleKakaoLogin}
            disabled={isLoading}
          >
            <svg
              className="w-5 h-5 mr-2"
              viewBox="0 0 24 24"
              fill="currentColor"
            >
              <path d="M12 3C6.477 3 2 6.477 2 10.5c0 2.442 1.512 4.608 3.825 5.985l-.975 3.585c-.075.285.195.54.465.42l4.095-1.95C10.26 18.84 11.115 19 12 19c5.523 0 10-3.477 10-7.5S17.523 3 12 3z" />
            </svg>
            카카오 로그인
          </Button>

          {/* 개발 모드 로그인 */}
          {isDev && (
            <>
              <div className="relative">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-border-primary"></div>
                </div>
                <div className="relative flex justify-center text-mini">
                  <span className="px-2 bg-background-quaternary text-text-quaternary">
                    개발 모드
                  </span>
                </div>
              </div>

              <Button
                variant="secondary"
                size="lg"
                className="w-full"
                onClick={handleDevLogin}
                disabled={isLoading}
              >
                {isLoading ? (
                  <Spinner size="sm" />
                ) : (
                  '개발자 로그인 (테스트)'
                )}
              </Button>
            </>
          )}
        </div>

        <div className="mt-6 text-center">
          <p className="text-text-quaternary text-small">
            로그인하면{' '}
            <a href="#" className="text-brand-primary hover:underline">
              서비스 이용약관
            </a>
            {' '}및{' '}
            <a href="#" className="text-brand-primary hover:underline">
              개인정보 처리방침
            </a>
            에 동의하는 것으로 간주됩니다.
          </p>
        </div>
      </Card>
    </div>
  );
}