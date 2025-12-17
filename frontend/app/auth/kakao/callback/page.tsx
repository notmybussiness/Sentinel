'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense } from 'react';
import { CheckCircle } from 'lucide-react';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

function KakaoCallbackContent() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const [error, setError] = useState<string | null>(null);
    const [success, setSuccess] = useState(false);
    const [nickname, setNickname] = useState<string>('');

    useEffect(() => {
        const code = searchParams.get('code');
        const errorParam = searchParams.get('error');

        if (errorParam) {
            setError('카카오 로그인이 취소되었습니다.');
            setTimeout(() => router.push('/'), 2000);
            return;
        }

        if (!code) {
            setError('인증 코드가 없습니다.');
            setTimeout(() => router.push('/'), 2000);
            return;
        }

        // 백엔드 callback 호출하여 토큰 획득
        const exchangeCode = async () => {
            try {
                const response = await fetch(`${API_URL}/api/v1/auth/kakao/callback?code=${code}`);

                if (!response.ok) {
                    throw new Error('인증에 실패했습니다.');
                }

                const data = await response.json();

                // 토큰 저장
                localStorage.setItem('accessToken', data.accessToken);
                localStorage.setItem('refreshToken', data.refreshToken);

                // 유저 정보 저장 (선택)
                if (data.user) {
                    localStorage.setItem('user', JSON.stringify(data.user));
                    setNickname(data.user.nickname || '');
                }

                // 성공 상태 표시
                setSuccess(true);

                // 1.5초 후 메인으로 리다이렉트
                setTimeout(() => {
                    router.push('/');
                    router.refresh(); // 페이지 새로고침으로 AuthContext 갱신
                }, 1500);
            } catch (err) {
                setError(err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.');
                setTimeout(() => router.push('/'), 3000);
            }
        };

        exchangeCode();
    }, [searchParams, router]);

    if (error) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-[#0B0F19]">
                <div className="text-center">
                    <div className="mb-4 text-red-500 text-lg">{error}</div>
                    <div className="text-gray-400 text-sm">잠시 후 메인으로 이동합니다...</div>
                </div>
            </div>
        );
    }

    if (success) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-[#0B0F19]">
                <div className="text-center">
                    <div className="mb-4">
                        <CheckCircle className="h-16 w-16 text-green-500 mx-auto" />
                    </div>
                    <div className="text-white text-xl font-semibold">로그인 성공!</div>
                    {nickname && (
                        <div className="text-indigo-400 text-lg mt-2">환영합니다, {nickname}님</div>
                    )}
                    <div className="text-gray-400 text-sm mt-3">잠시 후 메인으로 이동합니다...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="flex min-h-screen items-center justify-center bg-[#0B0F19]">
            <div className="text-center">
                <div className="mb-4">
                    <div className="h-12 w-12 animate-spin rounded-full border-4 border-indigo-500 border-t-transparent mx-auto"></div>
                </div>
                <div className="text-white text-lg">로그인 처리 중...</div>
                <div className="text-gray-400 text-sm mt-2">잠시만 기다려주세요</div>
            </div>
        </div>
    );
}

export default function KakaoCallbackPage() {
    return (
        <Suspense fallback={
            <div className="flex min-h-screen items-center justify-center bg-[#0B0F19]">
                <div className="h-12 w-12 animate-spin rounded-full border-4 border-indigo-500 border-t-transparent"></div>
            </div>
        }>
            <KakaoCallbackContent />
        </Suspense>
    );
}
