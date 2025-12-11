'use client';

import { Bell, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';

export function Header() {
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
            </div>
        </header>
    );
}
