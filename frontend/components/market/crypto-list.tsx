'use client';

import { useEffect, useState } from 'react';
import { CryptoPrice } from '@/lib/api/types';
import { api } from '@/lib/api/client';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ArrowUp, ArrowDown } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

export function CryptoList() {
    const [cryptos, setCryptos] = useState<CryptoPrice[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetch() {
            const data = await api.market.getTopCrypto();
            setCryptos(data);
            setLoading(false);
        }
        fetch();
    }, []);

    if (loading) return <div>Loading...</div>;

    return (
        <Card className='bg-[#1E293B]/50 border-none'>
            <CardHeader>
                <CardTitle>Top Cryptocurrencies</CardTitle>
            </CardHeader>
            <CardContent>
                <div className="space-y-4">
                    {cryptos.map((coin) => (
                        <div key={coin.symbol} className="flex items-center justify-between p-4 rounded-lg bg-[#0B0F19] border border-[#1E293B] hover:border-indigo-500/50 transition-colors">
                            <div className="flex items-center gap-4">
                                <div className="w-10 h-10 rounded-full bg-indigo-500/10 flex items-center justify-center font-bold text-indigo-400">
                                    {coin.symbol[0]}
                                </div>
                                <div>
                                    <h4 className="font-bold">{coin.name}</h4>
                                    <p className="text-sm text-gray-500">{coin.symbol}</p>
                                </div>
                            </div>
                            <div className="text-right">
                                <h4 className="font-bold">${coin.price.toLocaleString()}</h4>
                                <div className={cn(
                                    "flex items-center justify-end text-sm font-medium",
                                    coin.changePercent >= 0 ? "text-green-500" : "text-red-500"
                                )}>
                                    {coin.changePercent >= 0 ? <ArrowUp className="w-3 h-3 mr-1" /> : <ArrowDown className="w-3 h-3 mr-1" />}
                                    {Math.abs(coin.changePercent)}%
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </CardContent>
        </Card>
    );
}
