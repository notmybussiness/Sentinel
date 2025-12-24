'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { api } from '@/lib/api/client';
import { Portfolio, BacktestRequest, BacktestResponse, RebalanceFrequency } from '@/lib/api/types';
import { ArrowUp, ArrowDown, TrendingUp, Activity, Target, AlertTriangle } from 'lucide-react';
import { cn } from '@/lib/utils/cn';

export default function BacktestPage() {
    const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
    const [selectedPortfolio, setSelectedPortfolio] = useState<number | null>(null);
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<BacktestResponse | null>(null);
    const [error, setError] = useState<string | null>(null);

    // Form state
    const [startDate, setStartDate] = useState('2024-01-01');
    const [endDate, setEndDate] = useState('2024-12-31');
    const [initialCapital, setInitialCapital] = useState(10000000);
    const [rebalanceFrequency, setRebalanceFrequency] = useState<RebalanceFrequency>('QUARTERLY');
    const [transactionCost, setTransactionCost] = useState(0.1);

    useEffect(() => {
        async function loadPortfolios() {
            const data = await api.portfolio.getAll();
            setPortfolios(data);
            if (data.length > 0) {
                setSelectedPortfolio(data[0].id);
            }
        }
        loadPortfolios();
    }, []);

    const runBacktest = async () => {
        if (!selectedPortfolio) {
            setError('Please select a portfolio');
            return;
        }

        setLoading(true);
        setError(null);
        setResult(null);

        try {
            const request: BacktestRequest = {
                portfolioId: selectedPortfolio,
                startDate,
                endDate,
                initialCapital,
                rebalanceFrequency,
                transactionCostPercent: transactionCost / 100,
            };

            const response = await api.backtest.run(request);
            setResult(response);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Backtest failed');
        } finally {
            setLoading(false);
        }
    };

    const formatNumber = (num: number) => num.toLocaleString('ko-KR');
    const formatPercent = (num: number) => `${num >= 0 ? '+' : ''}${num.toFixed(2)}%`;
    const formatCurrency = (num: number) => `₩${formatNumber(Math.round(num))}`;

    return (
        <div className="space-y-8">
            <div>
                <h1 className="text-3xl font-bold">Backtest</h1>
                <p className="text-gray-400 mt-1">Test your portfolio strategy with historical data.</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Configuration Panel */}
                <Card className="bg-[#1E293B]/50 border-none">
                    <CardHeader>
                        <CardTitle>Configuration</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        {/* Portfolio Selection */}
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-2">
                                Portfolio
                            </label>
                            <select
                                value={selectedPortfolio ?? ''}
                                onChange={(e) => setSelectedPortfolio(Number(e.target.value))}
                                className="w-full bg-[#0B0F19] border border-[#1E293B] rounded-lg px-3 py-2 text-white"
                            >
                                <option value="">Select portfolio...</option>
                                {portfolios.map((p) => (
                                    <option key={p.id} value={p.id}>
                                        {p.name} ({p.holdings.length} holdings)
                                    </option>
                                ))}
                            </select>
                        </div>

                        {/* Date Range */}
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-400 mb-2">
                                    Start Date
                                </label>
                                <input
                                    type="date"
                                    value={startDate}
                                    onChange={(e) => setStartDate(e.target.value)}
                                    className="w-full bg-[#0B0F19] border border-[#1E293B] rounded-lg px-3 py-2 text-white"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-400 mb-2">
                                    End Date
                                </label>
                                <input
                                    type="date"
                                    value={endDate}
                                    onChange={(e) => setEndDate(e.target.value)}
                                    className="w-full bg-[#0B0F19] border border-[#1E293B] rounded-lg px-3 py-2 text-white"
                                />
                            </div>
                        </div>

                        {/* Initial Capital */}
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-2">
                                Initial Capital (KRW)
                            </label>
                            <input
                                type="number"
                                value={initialCapital}
                                onChange={(e) => setInitialCapital(Number(e.target.value))}
                                className="w-full bg-[#0B0F19] border border-[#1E293B] rounded-lg px-3 py-2 text-white"
                            />
                        </div>

                        {/* Rebalance Frequency */}
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-2">
                                Rebalancing
                            </label>
                            <select
                                value={rebalanceFrequency}
                                onChange={(e) => setRebalanceFrequency(e.target.value as RebalanceFrequency)}
                                className="w-full bg-[#0B0F19] border border-[#1E293B] rounded-lg px-3 py-2 text-white"
                            >
                                <option value="NONE">None</option>
                                <option value="MONTHLY">Monthly</option>
                                <option value="QUARTERLY">Quarterly</option>
                                <option value="YEARLY">Yearly</option>
                            </select>
                        </div>

                        {/* Transaction Cost */}
                        <div>
                            <label className="block text-sm font-medium text-gray-400 mb-2">
                                Transaction Cost (%)
                            </label>
                            <input
                                type="number"
                                step="0.01"
                                value={transactionCost}
                                onChange={(e) => setTransactionCost(Number(e.target.value))}
                                className="w-full bg-[#0B0F19] border border-[#1E293B] rounded-lg px-3 py-2 text-white"
                            />
                        </div>

                        <Button
                            onClick={runBacktest}
                            disabled={loading || !selectedPortfolio}
                            className="w-full bg-indigo-600 hover:bg-indigo-700"
                        >
                            {loading ? 'Running...' : 'Run Backtest'}
                        </Button>

                        {error && (
                            <div className="p-3 bg-red-500/10 border border-red-500/30 rounded-lg text-red-400 text-sm">
                                {error}
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* Results Panel */}
                <div className="lg:col-span-2 space-y-6">
                    {result ? (
                        <>
                            {/* Summary Cards */}
                            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                <Card className="bg-[#1E293B]/50 border-none">
                                    <CardContent className="p-4">
                                        <div className="flex items-center gap-2 text-gray-400 text-sm mb-1">
                                            <TrendingUp className="w-4 h-4" />
                                            Total Return
                                        </div>
                                        <div className={cn(
                                            "text-2xl font-bold",
                                            result.performance.totalReturn >= 0 ? "text-green-500" : "text-red-500"
                                        )}>
                                            {formatPercent(result.performance.totalReturn)}
                                        </div>
                                    </CardContent>
                                </Card>

                                <Card className="bg-[#1E293B]/50 border-none">
                                    <CardContent className="p-4">
                                        <div className="flex items-center gap-2 text-gray-400 text-sm mb-1">
                                            <Activity className="w-4 h-4" />
                                            CAGR
                                        </div>
                                        <div className={cn(
                                            "text-2xl font-bold",
                                            result.performance.cagr >= 0 ? "text-green-500" : "text-red-500"
                                        )}>
                                            {formatPercent(result.performance.cagr)}
                                        </div>
                                    </CardContent>
                                </Card>

                                <Card className="bg-[#1E293B]/50 border-none">
                                    <CardContent className="p-4">
                                        <div className="flex items-center gap-2 text-gray-400 text-sm mb-1">
                                            <Target className="w-4 h-4" />
                                            Sharpe Ratio
                                        </div>
                                        <div className="text-2xl font-bold text-white">
                                            {result.performance.sharpeRatio.toFixed(2)}
                                        </div>
                                    </CardContent>
                                </Card>

                                <Card className="bg-[#1E293B]/50 border-none">
                                    <CardContent className="p-4">
                                        <div className="flex items-center gap-2 text-gray-400 text-sm mb-1">
                                            <AlertTriangle className="w-4 h-4" />
                                            Max Drawdown
                                        </div>
                                        <div className="text-2xl font-bold text-red-500">
                                            {formatPercent(result.performance.maxDrawdown)}
                                        </div>
                                    </CardContent>
                                </Card>
                            </div>

                            {/* Performance Details */}
                            <Card className="bg-[#1E293B]/50 border-none">
                                <CardHeader>
                                    <CardTitle>Performance Summary</CardTitle>
                                </CardHeader>
                                <CardContent>
                                    <div className="grid grid-cols-2 md:grid-cols-3 gap-6">
                                        <div>
                                            <div className="text-gray-400 text-sm">Initial Capital</div>
                                            <div className="text-xl font-semibold">{formatCurrency(result.initialCapital)}</div>
                                        </div>
                                        <div>
                                            <div className="text-gray-400 text-sm">Final Value</div>
                                            <div className="text-xl font-semibold">{formatCurrency(result.finalValue)}</div>
                                        </div>
                                        <div>
                                            <div className="text-gray-400 text-sm">Profit/Loss</div>
                                            <div className={cn(
                                                "text-xl font-semibold flex items-center gap-1",
                                                result.finalValue >= result.initialCapital ? "text-green-500" : "text-red-500"
                                            )}>
                                                {result.finalValue >= result.initialCapital ?
                                                    <ArrowUp className="w-4 h-4" /> :
                                                    <ArrowDown className="w-4 h-4" />
                                                }
                                                {formatCurrency(result.finalValue - result.initialCapital)}
                                            </div>
                                        </div>
                                        <div>
                                            <div className="text-gray-400 text-sm">Sortino Ratio</div>
                                            <div className="text-xl font-semibold">{result.performance.sortinoRatio.toFixed(2)}</div>
                                        </div>
                                        <div>
                                            <div className="text-gray-400 text-sm">Volatility</div>
                                            <div className="text-xl font-semibold">{formatPercent(result.performance.volatility)}</div>
                                        </div>
                                        <div>
                                            <div className="text-gray-400 text-sm">Win Rate</div>
                                            <div className="text-xl font-semibold">{formatPercent(result.performance.winRate)}</div>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>

                            {/* Transaction Costs */}
                            <Card className="bg-[#1E293B]/50 border-none">
                                <CardHeader>
                                    <CardTitle>Transaction Costs</CardTitle>
                                </CardHeader>
                                <CardContent>
                                    <div className="grid grid-cols-3 gap-6">
                                        <div>
                                            <div className="text-gray-400 text-sm">Total Costs</div>
                                            <div className="text-xl font-semibold">{formatCurrency(result.totalTransactionCosts)}</div>
                                        </div>
                                        <div>
                                            <div className="text-gray-400 text-sm">Cost Rate</div>
                                            <div className="text-xl font-semibold">{(result.transactionCostPercent * 100).toFixed(2)}%</div>
                                        </div>
                                        <div>
                                            <div className="text-gray-400 text-sm">Impact on Return</div>
                                            <div className="text-xl font-semibold text-orange-400">-{formatPercent(result.costImpactPercent)}</div>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>

                            {/* Equity Curve */}
                            {result.equityCurve && result.equityCurve.length > 0 && (
                                <Card className="bg-[#1E293B]/50 border-none">
                                    <CardHeader>
                                        <CardTitle>Equity Curve</CardTitle>
                                    </CardHeader>
                                    <CardContent>
                                        <div className="h-64 flex items-end gap-1">
                                            {result.equityCurve.slice(-50).map((point, idx) => {
                                                const min = Math.min(...result.equityCurve.slice(-50).map(p => p.value));
                                                const max = Math.max(...result.equityCurve.slice(-50).map(p => p.value));
                                                const height = ((point.value - min) / (max - min)) * 100;
                                                return (
                                                    <div
                                                        key={idx}
                                                        className="flex-1 bg-indigo-500/50 hover:bg-indigo-500 rounded-t transition-colors"
                                                        style={{ height: `${Math.max(height, 5)}%` }}
                                                        title={`${point.date}: ${formatCurrency(point.value)}`}
                                                    />
                                                );
                                            })}
                                        </div>
                                        <div className="flex justify-between text-xs text-gray-500 mt-2">
                                            <span>{result.equityCurve[0]?.date}</span>
                                            <span>{result.equityCurve[result.equityCurve.length - 1]?.date}</span>
                                        </div>
                                    </CardContent>
                                </Card>
                            )}

                            {/* Holdings Summary */}
                            {result.holdingsSummary && result.holdingsSummary.length > 0 && (
                                <Card className="bg-[#1E293B]/50 border-none">
                                    <CardHeader>
                                        <CardTitle>Holdings Performance</CardTitle>
                                    </CardHeader>
                                    <CardContent>
                                        <div className="space-y-3">
                                            {result.holdingsSummary.map((holding, idx) => (
                                                <div key={idx} className="flex items-center justify-between p-3 bg-[#0B0F19] rounded-lg">
                                                    <div className="flex items-center gap-3">
                                                        <div className="w-10 h-10 rounded-full bg-indigo-500/10 flex items-center justify-center font-bold text-indigo-400">
                                                            {holding.symbol[0]}
                                                        </div>
                                                        <div>
                                                            <div className="font-medium">{holding.symbol}</div>
                                                            <div className="text-sm text-gray-400">Weight: {formatPercent(holding.weight)}</div>
                                                        </div>
                                                    </div>
                                                    <div className={cn(
                                                        "text-lg font-semibold",
                                                        holding.return >= 0 ? "text-green-500" : "text-red-500"
                                                    )}>
                                                        {formatPercent(holding.return)}
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </CardContent>
                                </Card>
                            )}
                        </>
                    ) : (
                        <Card className="bg-[#1E293B]/50 border-none h-96 flex items-center justify-center">
                            <div className="text-center text-gray-400">
                                <TrendingUp className="w-16 h-16 mx-auto mb-4 opacity-50" />
                                <p className="text-lg">Configure and run a backtest to see results</p>
                                <p className="text-sm mt-2">Select a portfolio, set parameters, and click Run Backtest</p>
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div>
    );
}
