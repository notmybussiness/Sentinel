"use client";

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter, usePathname } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/Button';

const navigation = [
  { name: '홈', href: '/' },
  { name: '포트폴리오', href: '/portfolios', requiresAuth: true },
  { name: '실험실', href: '/lab', requiresAuth: true },
  { name: '시장 데이터', href: '/market' },
];

export function Header() {
  const router = useRouter();
  const pathname = usePathname();
  const { user, isAuthenticated, logout } = useAuth();
  const [showUserMenu, setShowUserMenu] = useState(false);

  const handleLogout = async () => {
    await logout();
    setShowUserMenu(false);
  };

  return (
    <header className="sticky top-0 z-header bg-background-primary/95 backdrop-blur-sm border-b border-border-primary">
      <div className="max-w-7xl mx-auto px-8 py-4">
        <div className="flex items-center gap-8">
          {/* 로고 */}
          <Link
            href="/"
            className="text-2xl font-bold text-text-primary hover:text-brand-primary transition-colors"
          >
            Sentinel
          </Link>

          {/* 네비게이션 */}
          <nav className="hidden md:flex items-center gap-6 flex-1">
            {navigation.map((item) => {
              // 인증 필요한 메뉴는 로그인 시에만 표시
              if (item.requiresAuth && !isAuthenticated) {
                return null;
              }

              const isActive = pathname === item.href;

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`text-regular font-medium transition-colors ${
                    isActive
                      ? 'text-brand-primary'
                      : 'text-text-secondary hover:text-text-primary'
                  }`}
                >
                  {item.name}
                </Link>
              );
            })}
          </nav>

          {/* 사용자 메뉴 */}
          <div className="flex items-center gap-3">
            {isAuthenticated && user ? (
              <div className="relative">
                <button
                  onClick={() => setShowUserMenu(!showUserMenu)}
                  className="flex items-center gap-2 px-3 py-2 rounded-8 hover:bg-background-tertiary transition-colors"
                >
                  {user.profileImageUrl ? (
                    <img
                      src={user.profileImageUrl}
                      alt={user.name}
                      className="w-8 h-8 rounded-full"
                    />
                  ) : (
                    <div className="w-8 h-8 rounded-full bg-brand-primary/20 flex items-center justify-center text-brand-primary font-semibold">
                      {user.name[0]}
                    </div>
                  )}
                  <span className="text-text-primary font-medium hidden sm:block">
                    {user.nickname || user.name}
                  </span>
                  <svg
                    className={`w-4 h-4 text-text-tertiary transition-transform ${
                      showUserMenu ? 'rotate-180' : ''
                    }`}
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M19 9l-7 7-7-7"
                    />
                  </svg>
                </button>

                {/* 드롭다운 메뉴 */}
                {showUserMenu && (
                  <>
                    <div
                      className="fixed inset-0 z-10"
                      onClick={() => setShowUserMenu(false)}
                    />
                    <div className="absolute right-0 mt-2 w-48 bg-background-quaternary border border-border-primary rounded-8 shadow-high z-20">
                      <div className="p-3 border-b border-border-primary">
                        <p className="text-text-primary font-medium">
                          {user.name}
                        </p>
                        <p className="text-text-quaternary text-small">
                          {user.email}
                        </p>
                      </div>
                      <div className="p-2">
                        <button
                          onClick={() => {
                            router.push('/settings');
                            setShowUserMenu(false);
                          }}
                          className="w-full px-3 py-2 text-left text-text-secondary hover:bg-background-quinary hover:text-text-primary rounded-6 transition-colors"
                        >
                          설정
                        </button>
                        <button
                          onClick={handleLogout}
                          className="w-full px-3 py-2 text-left text-semantic-error hover:bg-background-quinary rounded-6 transition-colors"
                        >
                          로그아웃
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>
            ) : (
              <Button
                variant="primary"
                size="md"
                onClick={() => router.push('/login')}
                className="bg-brand-primary hover:bg-brand-secondary text-background-primary font-semibold"
              >
                카카오 로그인
              </Button>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}