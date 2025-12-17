'use client';

import { useState, useEffect } from 'react';
import { api } from '@/lib/api/client';
import { ChartData, AssetType } from '@/lib/api/types';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart } from 'recharts';
import { ArrowUp, ArrowDown, TrendingUp, Calendar, Search } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

const PERIODS = [
    { label: '1W', days: 7 },
    { label: '1M', days: 30 },
    { label: '3M', days: 90 },
    { label: '6M', days: 180 },
    { label: '1Y', days: 365 },
];

const ASSET_TYPES: { label: string; value: AssetType }[] = [
    { label: 'Stock', value: 'STOCK' },
    { label: 'Crypto', value: 'CRYPTO' },
    { label: 'ETF', value: 'ETF' },
    { label: 'Index', value: 'INDEX' },
];

export default function PriceHistoryPage() {
    const [assetType, setAssetType] = useState<AssetType>('INDEX');
    const [symbols, setSymbols] = useState<string[]>([]);
    const [selectedSymbol, setSelectedSymbol] = useState<string>('');
    const [selectedPeriod, setSelectedPeriod] = useState(PERIODS[2]); // 3M default
    const [chartData, setChartData] = useState<ChartData | null>(null);
    const [loading, setLoading] = useState(false);
    const [symbolSearch, setSymbolSearch] = useState('');

    // Fetch available symbols when asset type changes
    useEffect(() => {
        async function fetchSymbols() {
            const data = await api.priceHistory.getSymbolsByAssetType(assetType);
            setSymbols(data);
            if (data.length > 0 && !data.includes(selectedSymbol)) {
                setSelectedSymbol(data[0]);
            }
        }
        fetchSymbols();
    }, [assetType]);

    // Fetch chart data when symbol or period changes
    useEffect(() => {
        if (!selectedSymbol) return;

        async function fetchChartData() {
            setLoading(true);
            const endTime = new Date().toISOString();
            const startTime = new Date(Date.now() - selectedPeriod.days * 24 * 60 * 60 * 1000).toISOString();

            const data = await api.priceHistory.getChartData(selectedSymbol, startTime, endTime);
            setChartData(data);
            setLoading(false);
        }
        fetchChartData();
    }, [selectedSymbol, selectedPeriod]);

    const filteredSymbols = symbols.filter(s =>
        s.toLowerCase().includes(symbolSearch.toLowerCase())
    );

    const formatPrice = (value: number) => {
        if (value >= 1000000) return `${(value / 1000000).toFixed(2)}M`;
        if (value >= 1000) return `${(value / 1000).toFixed(1)}K`;
        return value.toFixed(2);
    };

    const formatDate = (dateStr: string) => {
        const date = new Date(dateStr);
        return `${date.getMonth() + 1}/${date.getDate()}`;
    };

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-3xl font-bold">Price History</h1>
                <p className="text-gray-400 mt-1">Historical price charts and data analysis</p>
            </div>

            {/* Controls */}
            <div className="flex flex-wrap gap-4">
                {/* Asset Type Selector */}
                <div className="flex bg-[#1E293B] rounded-lg p-1">
                    {ASSET_TYPES.map((type) => (
                        <button
                            key={type.value}
                            onClick={() => setAssetType(type.value)}
                            className={cn(
                                "px-4 py-2 rounded-md text-sm font-medium transition-colors",
                                assetType === type.value
                                    ? "bg-indigo-600 text-white"
                                    : "text-gray-400 hover:text-white"
                            )}
                        >
                            {type.label}
                        </button>
                    ))}
                </div>

                {/* Period Selector */}
                <div className="flex bg-[#1E293B] rounded-lg p-1">
                    {PERIODS.map((period) => (
                        <button
                            key={period.label}
                            onClick={() => setSelectedPeriod(period)}
                            className={cn(
                                "px-4 py-2 rounded-md text-sm font-medium transition-colors",
                                selectedPeriod.label === period.label
                                    ? "bg-indigo-600 text-white"
                                    : "text-gray-400 hover:text-white"
                            )}
                        >
                            {period.label}
                        </button>
                    ))}
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                {/* Symbol List */}
                <Card className="bg-[#1E293B]/50 border-none lg:col-span-1">
                    <CardHeader className="pb-3">
                        <CardTitle className="text-lg flex items-center gap-2">
                            <TrendingUp className="w-5 h-5" />
                            Symbols
                        </CardTitle>
                        <div className="relative mt-2">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                            <input
                                type="text"
                                placeholder="Search..."
                                value={symbolSearch}
                                onChange={(e) => setSymbolSearch(e.target.value)}
                                className="w-full pl-9 pr-3 py-2 bg-[#0B0F19] border border-[#1E293B] rounded-lg text-sm focus:outline-none focus:border-indigo-500"
                            />
                        </div>
                    </CardHeader>
                    <CardContent className="max-h-[400px] overflow-y-auto">
                        {filteredSymbols.length === 0 ? (
                            <p className="text-gray-500 text-sm text-center py-4">No symbols available</p>
                        ) : (
                            <div className="space-y-1">
                                {filteredSymbols.map((symbol) => (
                                    <button
                                        key={symbol}
                                        onClick={() => setSelectedSymbol(symbol)}
                                        className={cn(
                                            "w-full text-left px-3 py-2 rounded-lg text-sm transition-colors",
                                            selectedSymbol === symbol
                                                ? "bg-indigo-600 text-white"
                                                : "hover:bg-[#0B0F19] text-gray-300"
                                        )}
                                    >
                                        {symbol}
                                    </button>
                                ))}
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* Chart & Statistics */}
                <div className="lg:col-span-3 space-y-6">
                    {/* Statistics Cards */}
                    {chartData?.statistics && (
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                            <Card className="bg-[#1E293B]/50 border-none">
                                <CardContent className="p-4">
                                    <p className="text-xs text-gray-400">Change</p>
                                    <div className={cn(
                                        "flex items-center gap-1 text-lg font-bold",
                                        chartData.statistics.changePercent >= 0 ? "text-green-500" : "text-red-500"
                                    )}>
                                        {chartData.statistics.changePercent >= 0 ? <ArrowUp className="w-4 h-4" /> : <ArrowDown className="w-4 h-4" />}
                                        {Math.abs(chartData.statistics.changePercent).toFixed(2)}%
                                    </div>
                                </CardContent>
                            </Card>
                            <Card className="bg-[#1E293B]/50 border-none">
                                <CardContent className="p-4">
                                    <p className="text-xs text-gray-400">High</p>
                                    <p className="text-lg font-bold text-green-400">
                                        {formatPrice(chartData.statistics.highestPrice)}
                                    </p>
                                </CardContent>
                            </Card>
                            <Card className="bg-[#1E293B]/50 border-none">
                                <CardContent className="p-4">
                                    <p className="text-xs text-gray-400">Low</p>
                                    <p className="text-lg font-bold text-red-400">
                                        {formatPrice(chartData.statistics.lowestPrice)}
                                    </p>
                                </CardContent>
                            </Card>
                            <Card className="bg-[#1E293B]/50 border-none">
                                <CardContent className="p-4">
                                    <p className="text-xs text-gray-400">Avg Volume</p>
                                    <p className="text-lg font-bold">
                                        {formatPrice(chartData.statistics.averageVolume)}
                                    </p>
                                </CardContent>
                            </Card>
                        </div>
                    )}

                    {/* Chart */}
                    <Card className="bg-[#1E293B]/50 border-none">
                        <CardHeader>
                            <CardTitle className="flex items-center justify-between">
                                <span className="flex items-center gap-2">
                                    <Calendar className="w-5 h-5" />
                                    {selectedSymbol || 'Select a symbol'}
                                </span>
                                {chartData && (
                                    <span className="text-sm text-gray-400">
                                        {chartData.data.length} data points
                                    </span>
                                )}
                            </CardTitle>
                        </CardHeader>
                        <CardContent>
                            {loading ? (
                                <div className="h-[400px] flex items-center justify-center">
                                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-500"></div>
                                </div>
                            ) : !chartData || chartData.data.length === 0 ? (
                                <div className="h-[400px] flex items-center justify-center text-gray-500">
                                    {selectedSymbol ? 'No data available for this period' : 'Select a symbol to view chart'}
                                </div>
                            ) : (
                                <ResponsiveContainer width="100%" height={400}>
                                    <AreaChart data={chartData.data}>
                                        <defs>
                                            <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                                                <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3}/>
                                                <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                                            </linearGradient>
                                        </defs>
                                        <CartesianGrid strokeDasharray="3 3" stroke="#1E293B" />
                                        <XAxis
                                            dataKey="time"
                                            tickFormatter={formatDate}
                                            stroke="#64748b"
                                            tick={{ fill: '#64748b', fontSize: 12 }}
                                        />
                                        <YAxis
                                            tickFormatter={formatPrice}
                                            stroke="#64748b"
                                            tick={{ fill: '#64748b', fontSize: 12 }}
                                            domain={['auto', 'auto']}
                                        />
                                        <Tooltip
                                            contentStyle={{
                                                backgroundColor: '#1E293B',
                                                border: 'none',
                                                borderRadius: '8px',
                                                color: '#fff'
                                            }}
                                            labelFormatter={(value) => new Date(value).toLocaleDateString()}
                                            formatter={(value: number) => [formatPrice(value), 'Price']}
                                        />
                                        <Area
                                            type="monotone"
                                            dataKey="value"
                                            stroke="#6366f1"
                                            strokeWidth={2}
                                            fillOpacity={1}
                                            fill="url(#colorValue)"
                                        />
                                    </AreaChart>
                                </ResponsiveContainer>
                            )}
                        </CardContent>
                    </Card>
                </div>
            </div>
        </div>
    );
}
