'use client';

import { useEffect, useState } from 'react';
import { MarketIndex, CryptoPrice } from '@/lib/api/types';
import { api } from '@/lib/api/client';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowUp, ArrowDown } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

export function MarketOverview() {
    const [indices, setIndices] = useState<MarketIndex[]>([]);
    const [crypto, setCrypto] = useState<CryptoPrice[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchData() {
            const [idxData, cryptoData] = await Promise.all([
                api.market.getIndices(),
                api.market.getTopCrypto()
            ]);
            setIndices(idxData);
            setCrypto(cryptoData.slice(0, 4)); // Show top 4
            setLoading(false);
        }
        fetchData();
    }, []);

    if (loading) {
        return (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
                {[1, 2, 3, 4].map((i) => (
                    <div key={i} className="h-32 animate-pulse rounded-lg bg-[#1E293B]" />
                ))}
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div>
                <h2 className="text-xl font-bold mb-4">Market Indices</h2>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
                    {indices.map((idx) => (
                        <Card key={idx.symbol} className="bg-[#1E293B]/50 border-none">
                            <CardContent className="p-6">
                                <div className="flex justify-between items-start">
                                    <div>
                                        <p className="text-sm font-medium text-gray-400">{idx.name}</p>
                                        <h3 className="text-2xl font-bold mt-2">{idx.price.toLocaleString()}</h3>
                                    </div>
                                    <div className={cn(
                                        "flex items-center px-2 py-1 rounded text-xs font-medium",
                                        idx.change >= 0 ? "bg-green-500/10 text-green-500" : "bg-red-500/10 text-red-500"
                                    )}>
                                        {idx.change >= 0 ? <ArrowUp className="w-3 h-3 mr-1" /> : <ArrowDown className="w-3 h-3 mr-1" />}
                                        {Math.abs(idx.changePercent).toFixed(2)}%
                                    </div>
                                </div>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            </div>

            <div>
                <h2 className="text-xl font-bold mb-4">Top Crypto</h2>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
                    {crypto.map((coin) => (
                        <Card key={coin.symbol} className="bg-[#1E293B]/50 border-none">
                            <CardContent className="p-6">
                                <div className="flex justify-between items-start">
                                    <div>
                                        <p className="text-sm font-medium text-gray-400">{coin.name} ({coin.symbol})</p>
                                        <h3 className="text-2xl font-bold mt-2">${coin.price.toLocaleString()}</h3>
                                    </div>
                                    <div className={cn(
                                        "flex items-center px-2 py-1 rounded text-xs font-medium",
                                        coin.changePercent >= 0 ? "bg-green-500/10 text-green-500" : "bg-red-500/10 text-red-500"
                                    )}>
                                        {coin.changePercent >= 0 ? <ArrowUp className="w-3 h-3 mr-1" /> : <ArrowDown className="w-3 h-3 mr-1" />}
                                        {Math.abs(coin.changePercent).toFixed(2)}%
                                    </div>
                                </div>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            </div>
        </div>
    );
}
