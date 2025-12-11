'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LayoutDashboard, TrendingUp, PieChart, Brain, Settings, History } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

const NAV_ITEMS = [
    { name: 'Dashboard', href: '/', icon: LayoutDashboard },
    { name: 'Market', href: '/market', icon: TrendingUp },
    { name: 'Portfolio', href: '/portfolio', icon: PieChart },
    { name: 'AI Analysis', href: '/ai', icon: Brain },
    { name: 'Price History', href: '/price-history', icon: History },
    { name: 'Settings', href: '/settings', icon: Settings },
];

export function Sidebar() {
    const pathname = usePathname();

    return (
        <div className="flex h-screen w-64 flex-col border-r border-[#1E293B] bg-[#0B0F19] text-white">
            <div className="flex h-16 items-center px-6">
                <div className="flex items-center gap-2">
                    <div className="h-8 w-8 rounded-lg bg-indigo-600 flex items-center justify-center font-bold">S</div>
                    <span className="text-lg font-bold">Sentinel</span>
                </div>
            </div>

            <nav className="flex-1 space-y-1 px-3 py-4">
                {NAV_ITEMS.map((item) => {
                    const isActive = pathname === item.href;
                    return (
                        <Link
                            key={item.href}
                            href={item.href}
                            className={cn(
                                "group flex items-center px-3 py-2 text-sm font-medium rounded-md transition-colors",
                                isActive
                                    ? "bg-indigo-600/10 text-indigo-400"
                                    : "text-gray-400 hover:text-white hover:bg-[#1E293B]"
                            )}
                        >
                            <item.icon className={cn("mr-3 h-5 w-5 flex-shrink-0", isActive ? "text-indigo-400" : "text-gray-500 group-hover:text-white")} />
                            {item.name}
                        </Link>
                    );
                })}
            </nav>

            <div className="p-4 border-t border-[#1E293B]">
                <div className="flex items-center">
                    <div className="h-8 w-8 rounded-full bg-gray-700"></div>
                    <div className="ml-3">
                        <p className="text-sm font-medium">User</p>
                        <p className="text-xs text-gray-500">View Profile</p>
                    </div>
                </div>
            </div>
        </div>
    );
}
