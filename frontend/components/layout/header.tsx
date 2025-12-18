'use client';

import { useState } from 'react';
import { Bell, Search, LogIn, LogOut, User, CheckCircle, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/lib/auth/auth-context';

export function Header() {
    const { user, isLoading, isAuthenticated, login, devLogin, logout } = useAuth();
    const [loginLoading, setLoginLoading] = useState(false);
    const [showSuccess, setShowSuccess] = useState(false);

    const handleDevLogin = async () => {
        setLoginLoading(true);
        await devLogin();
        setLoginLoading(false);
        setShowSuccess(true);
        setTimeout(() => setShowSuccess(false), 2000);
    };

    return (
        <header className="flex h-16 items-center justify-between border-b border-[#1E293B] bg-[#0B0F19]/50 px-6 backdrop-blur-xl">
            <div className="w-96">
                <div className="relative">
                    <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-gray-500" />
                    <input
                        type="text"
                        placeholder="Search assets..."
                        className="w-full rounded-md bg-[#1E293B] pl-9 py-2 text-sm text-white placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                    />
                </div>
            </div>

            <div className="flex items-center gap-4">
                <Button variant="ghost" size="icon" className="text-gray-400 hover:text-white">
                    <Bell className="h-5 w-5" />
                </Button>

                {isLoading ? (
                    <div className="h-8 w-20 animate-pulse rounded bg-[#1E293B]" />
                ) : isAuthenticated && user ? (
                    <div className="flex items-center gap-3">
                        {user.profileImage ? (
                            <img
                                src={user.profileImage}
                                alt={user.nickname}
                                className="h-8 w-8 rounded-full"
                            />
                        ) : (
                            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-600">
                                <User className="h-4 w-4 text-white" />
                            </div>
                        )}
                        <span className="text-sm text-gray-300">{user.nickname}</span>
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={logout}
                            className="text-gray-400 hover:text-white"
                        >
                            <LogOut className="mr-1 h-4 w-4" />
                            로그아웃
                        </Button>
                    </div>
                ) : (
                    <div className="flex gap-2">
                        <Button
                            onClick={handleDevLogin}
                            variant="outline"
                            className="border-gray-600 text-gray-300 hover:bg-gray-700"
                            disabled={loginLoading}
                        >
                            {loginLoading ? (
                                <Loader2 className="mr-1 h-4 w-4 animate-spin" />
                            ) : null}
                            Dev 로그인
                        </Button>
                        <Button
                            onClick={login}
                            className="bg-[#FEE500] text-[#191919] hover:bg-[#FDD835]"
                            disabled={loginLoading}
                        >
                            <LogIn className="mr-2 h-4 w-4" />
                            카카오 로그인
                        </Button>
                    </div>
                )}

                {/* 로그인 성공 토스트 */}
                {showSuccess && (
                    <div className="fixed top-4 right-4 z-50 flex items-center gap-2 bg-green-600 text-white px-4 py-3 rounded-lg shadow-lg animate-in fade-in slide-in-from-top-2 duration-300">
                        <CheckCircle className="h-5 w-5" />
                        로그인 성공!
                    </div>
                )}
            </div>
        </header>
    );
}
