'use client';

import { Bell, Search, LogIn, LogOut, User } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/lib/auth/auth-context';

export function Header() {
    const { user, isLoading, isAuthenticated, login, logout } = useAuth();

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
                    <Button
                        onClick={login}
                        className="bg-[#FEE500] text-[#191919] hover:bg-[#FDD835]"
                    >
                        <LogIn className="mr-2 h-4 w-4" />
                        카카오 로그인
                    </Button>
                )}
            </div>
        </header>
    );
}
