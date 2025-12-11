'use client';

import { useEffect, useState } from 'react';
import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { api } from '@/lib/api/client';
import { PortfolioHistoryPoint } from '@/lib/api/types';

export function PortfolioGrowthChart() {
    const [data, setData] = useState<PortfolioHistoryPoint[]>([]);

    useEffect(() => {
        api.portfolio.getHistory().then(setData);
    }, []);

    return (
        <Card className="bg-[#1E293B]/50 border-none">
            <CardHeader>
                <CardTitle>Total Wealth History</CardTitle>
                <CardDescription>Last 30 days performance</CardDescription>
            </CardHeader>
            <CardContent>
                <div className="h-[300px] w-full">
                    <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={data}>
                            <defs>
                                <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#6366F1" stopOpacity={0.3} />
                                    <stop offset="95%" stopColor="#6366F1" stopOpacity={0} />
                                </linearGradient>
                            </defs>
                            <XAxis
                                dataKey="date"
                                stroke="#64748B"
                                fontSize={12}
                                tickLine={false}
                                axisLine={false}
                                minTickGap={30}
                            />
                            <YAxis
                                stroke="#64748B"
                                fontSize={12}
                                tickLine={false}
                                axisLine={false}
                                tickFormatter={(value) => `$${value / 1000}k`}
                            />
                            <Tooltip
                                contentStyle={{ backgroundColor: '#0F172A', borderColor: '#1E293B', color: '#F8FAFC' }}
                                itemStyle={{ color: '#6366F1' }}
                                formatter={(value: number) => [`$${value.toLocaleString()}`, 'Value']}
                            />
                            <Area
                                type="monotone"
                                dataKey="value"
                                stroke="#6366F1"
                                strokeWidth={2}
                                fillOpacity={1}
                                fill="url(#colorValue)"
                            />
                        </AreaChart>
                    </ResponsiveContainer>
                </div>
            </CardContent>
        </Card>
    );
}
