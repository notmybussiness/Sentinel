'use client';

import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { api } from '@/lib/api/client';
import { User } from '@/lib/api/types';

interface AuthContextType {
    user: User | null;
    isLoading: boolean;
    isAuthenticated: boolean;
    login: () => Promise<void>;
    logout: () => void;
    refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const initAuth = async () => {
            const accessToken = localStorage.getItem('accessToken');
            if (accessToken) {
                const userData = await api.auth.getMe();
                setUser(userData);
            }
            setIsLoading(false);
        };
        initAuth();
    }, []);

    const login = async () => {
        try {
            const kakaoUrl = await api.auth.getKakaoLoginUrl();
            window.location.href = kakaoUrl;
        } catch (error) {
            console.error('Failed to get Kakao login URL:', error);
        }
    };

    const handleLogout = async () => {
        await api.auth.logout();
        setUser(null);
    };

    const refreshUser = async () => {
        const userData = await api.auth.getMe();
        setUser(userData);
    };

    return (
        <AuthContext.Provider value={{ user, isLoading, isAuthenticated: !!user, login, logout: handleLogout, refreshUser }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within AuthProvider');
    return context;
}
