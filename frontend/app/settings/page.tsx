'use client';

import { useState, useEffect } from 'react';
import { api, logout } from '@/lib/api/client';
import { User } from '@/lib/api/types';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { User as UserIcon, LogOut, Bell, Shield, Moon, Globe, Mail, Settings2 } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

export default function SettingsPage() {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchUser() {
            const userData = await api.auth.getMe();
            setUser(userData);
            setLoading(false);
        }
        fetchUser();
    }, []);

    const handleLogout = async () => {
        if (confirm('Are you sure you want to log out?')) {
            await api.auth.logout();
        }
    };

    if (loading) {
        return (
            <div className="flex h-[60vh] items-center justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-500"></div>
            </div>
        );
    }

    return (
        <div className="space-y-6 max-w-4xl mx-auto">
            <div>
                <h1 className="text-3xl font-bold flex items-center gap-3">
                    <Settings2 className="w-8 h-8 text-indigo-500" />
                    Settings
                </h1>
                <p className="text-gray-400 mt-1">Manage your account and preferences</p>
            </div>

            {/* User Profile */}
            <Card className="bg-[#1E293B]/50 border-none">
                <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                        <UserIcon className="w-5 h-5" />
                        Profile
                    </CardTitle>
                    <CardDescription>Your account information</CardDescription>
                </CardHeader>
                <CardContent>
                    {user ? (
                        <div className="flex items-center gap-6">
                            <div className="w-20 h-20 rounded-full bg-indigo-500/20 flex items-center justify-center overflow-hidden">
                                {user.profileImage ? (
                                    <img src={user.profileImage} alt={user.nickname} className="w-full h-full object-cover" />
                                ) : (
                                    <UserIcon className="w-10 h-10 text-indigo-500" />
                                )}
                            </div>
                            <div className="space-y-1">
                                <h3 className="text-xl font-bold">{user.nickname}</h3>
                                <p className="text-gray-400 flex items-center gap-2">
                                    <Mail className="w-4 h-4" />
                                    {user.email}
                                </p>
                                <p className="text-gray-500 text-sm">User ID: {user.id}</p>
                            </div>
                        </div>
                    ) : (
                        <div className="text-center py-8">
                            <UserIcon className="w-12 h-12 mx-auto text-gray-600 mb-3" />
                            <p className="text-gray-500">Not logged in</p>
                            <button
                                onClick={() => window.location.href = '/'}
                                className="mt-4 px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium transition-colors"
                            >
                                Go to Login
                            </button>
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* Settings Sections */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Notifications (Coming Soon) */}
                <Card className="bg-[#1E293B]/50 border-none opacity-60">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Bell className="w-5 h-5" />
                            Notifications
                        </CardTitle>
                        <CardDescription>Coming soon</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-4">
                            <SettingRow label="Price Alerts" description="Get notified when prices change" disabled />
                            <SettingRow label="Portfolio Updates" description="Daily portfolio summary" disabled />
                            <SettingRow label="AI Insights" description="New analysis available" disabled />
                        </div>
                    </CardContent>
                </Card>

                {/* Display (Coming Soon) */}
                <Card className="bg-[#1E293B]/50 border-none opacity-60">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Moon className="w-5 h-5" />
                            Display
                        </CardTitle>
                        <CardDescription>Coming soon</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-4">
                            <SettingRow label="Dark Mode" description="Always use dark theme" disabled checked />
                            <SettingRow label="Language" description="English" disabled />
                            <SettingRow label="Currency" description="USD" disabled />
                        </div>
                    </CardContent>
                </Card>

                {/* Privacy (Coming Soon) */}
                <Card className="bg-[#1E293B]/50 border-none opacity-60">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Shield className="w-5 h-5" />
                            Privacy & Security
                        </CardTitle>
                        <CardDescription>Coming soon</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-4">
                            <SettingRow label="Two-Factor Auth" description="Add extra security" disabled />
                            <SettingRow label="Session Timeout" description="Auto-logout after inactivity" disabled />
                        </div>
                    </CardContent>
                </Card>

                {/* Data & API (Coming Soon) */}
                <Card className="bg-[#1E293B]/50 border-none opacity-60">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Globe className="w-5 h-5" />
                            Data & API
                        </CardTitle>
                        <CardDescription>Coming soon</CardDescription>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-4">
                            <SettingRow label="Real-time Data" description="Enable live price updates" disabled checked />
                            <SettingRow label="Export Data" description="Download your portfolio data" disabled />
                        </div>
                    </CardContent>
                </Card>
            </div>

            {/* Logout */}
            {user && (
                <Card className="bg-red-950/20 border border-red-900/30">
                    <CardContent className="py-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <h3 className="font-bold text-red-400">Log Out</h3>
                                <p className="text-gray-500 text-sm">Sign out of your account</p>
                            </div>
                            <button
                                onClick={handleLogout}
                                className="flex items-center gap-2 px-6 py-3 bg-red-600 hover:bg-red-700 text-white rounded-lg font-medium transition-colors"
                            >
                                <LogOut className="w-5 h-5" />
                                Log Out
                            </button>
                        </div>
                    </CardContent>
                </Card>
            )}
        </div>
    );
}

function SettingRow({
    label,
    description,
    disabled = false,
    checked = false
}: {
    label: string;
    description: string;
    disabled?: boolean;
    checked?: boolean;
}) {
    return (
        <div className={cn(
            "flex items-center justify-between py-2",
            disabled && "cursor-not-allowed"
        )}>
            <div>
                <p className="font-medium">{label}</p>
                <p className="text-gray-500 text-sm">{description}</p>
            </div>
            <div className={cn(
                "w-12 h-6 rounded-full relative transition-colors",
                checked ? "bg-indigo-600" : "bg-gray-700",
                disabled && "opacity-50"
            )}>
                <div className={cn(
                    "w-5 h-5 rounded-full bg-white absolute top-0.5 transition-all",
                    checked ? "right-0.5" : "left-0.5"
                )} />
            </div>
        </div>
    );
}
