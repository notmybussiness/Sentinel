'use client';

import { CryptoList } from '@/components/market/crypto-list';
import { MarketOverview } from '@/components/dashboard/market-overview';

export default function MarketPage() {
    return (
        <div className="space-y-8">
            <div>
                <h1 className="text-3xl font-bold">Market Overview</h1>
                <p className="text-gray-400 mt-1">Real-time prices and market analysis.</p>
            </div>

            <MarketOverview />

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <CryptoList />
                {/* Placeholder for Stock List */}
                <div className="h-96 rounded-xl border border-[#1E293B] bg-[#151B2B] p-6 flex items-center justify-center text-gray-500">
                    Stock Market Winners & Losers (Coming Soon)
                </div>
            </div>
        </div>
    );
}
