'use client';

import { useEffect, useState } from 'react';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { api } from '@/lib/api/client';
import { AssetAllocation } from '@/lib/api/types';

export function AssetAllocationChart() {
    const [data, setData] = useState<AssetAllocation[]>([]);

    useEffect(() => {
        api.portfolio.getAllocation().then(setData);
    }, []);

    // 총합 계산
    const total = data.reduce((sum, item) => sum + item.value, 0);

    return (
        <Card className="bg-[#1E293B]/50 border-none">
            <CardHeader>
                <CardTitle>Asset Allocation</CardTitle>
                <CardDescription>Current distribution</CardDescription>
            </CardHeader>
            <CardContent>
                <div className="h-[300px] w-full">
                    <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                            <Pie
                                data={data}
                                cx="50%"
                                cy="50%"
                                innerRadius={60}
                                outerRadius={80}
                                paddingAngle={5}
                                dataKey="value"
                                nameKey="label"
                                label={({ value }) => `${total > 0 ? ((value / total) * 100).toFixed(0) : 0}%`}
                                labelLine={false}
                            >
                                {data.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={entry.color} stroke="none" />
                                ))}
                            </Pie>
                            <Tooltip
                                contentStyle={{ backgroundColor: '#0F172A', borderColor: '#1E293B', color: '#F8FAFC' }}
                                formatter={(value: number) => [`${total > 0 ? ((value / total) * 100).toFixed(1) : 0}%`, '비중']}
                            />
                        </PieChart>
                    </ResponsiveContainer>
                </div>
                {/* 커스텀 Legend (글씨색 흰색) */}
                <div className="flex flex-wrap justify-center gap-4 mt-4">
                    {data.map((entry) => (
                        <div key={entry.id} className="flex items-center gap-2">
                            <span
                                className="w-3 h-3 rounded-full"
                                style={{ backgroundColor: entry.color }}
                            />
                            <span className="text-gray-300 text-sm">
                                {entry.label} ({total > 0 ? ((entry.value / total) * 100).toFixed(0) : 0}%)
                            </span>
                        </div>
                    ))}
                </div>
            </CardContent>
        </Card>
    );
}
