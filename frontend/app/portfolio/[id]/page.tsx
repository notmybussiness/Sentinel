'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { ArrowLeft, Plus, Edit2, TrendingUp, TrendingDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { api } from '@/lib/api/client';
import { Portfolio } from '@/lib/api/types';
import { AddHoldingModal } from '@/components/portfolio/add-holding-modal';
import { EditPortfolioModal } from '@/components/portfolio/edit-portfolio-modal';

export default function PortfolioDetailPage() {
    const params = useParams();
    const router = useRouter();
    const portfolioId = Number(params.id);

    const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isAddHoldingOpen, setIsAddHoldingOpen] = useState(false);
    const [isEditOpen, setIsEditOpen] = useState(false);

    const loadPortfolio = async () => {
        setIsLoading(true);
        try {
            const data = await api.portfolio.getById(portfolioId);
            setPortfolio(data || null);
        } catch (err) {
            console.error('Failed to load portfolio:', err);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (portfolioId) {
            loadPortfolio();
        }
    }, [portfolioId]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-indigo-500 border-t-transparent"></div>
            </div>
        );
    }

    if (!portfolio) {
        return (
            <div className="text-center py-12">
                <p className="text-gray-400 mb-4">포트폴리오를 찾을 수 없습니다.</p>
                <Button onClick={() => router.push('/portfolio')}>목록으로</Button>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => router.push('/portfolio')}
                        className="text-gray-400 hover:text-white"
                    >
                        <ArrowLeft className="w-5 h-5" />
                    </Button>
                    <div>
                        <h1 className="text-2xl font-bold text-white">{portfolio.name}</h1>
                        <p className="text-gray-400">{portfolio.description}</p>
                    </div>
                </div>
                <Button
                    variant="outline"
                    onClick={() => setIsEditOpen(true)}
                    className="border-gray-600 text-gray-300"
                >
                    <Edit2 className="w-4 h-4 mr-2" />
                    수정
                </Button>
            </div>

            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Card className="bg-[#1E293B]/50 border-none">
                    <CardContent className="p-6">
                        <p className="text-gray-400 text-sm">총 가치</p>
                        <p className="text-2xl font-bold text-white">${portfolio.totalValue.toLocaleString()}</p>
                    </CardContent>
                </Card>
                <Card className="bg-[#1E293B]/50 border-none">
                    <CardContent className="p-6">
                        <p className="text-gray-400 text-sm">총 수익</p>
                        <p className={`text-2xl font-bold ${portfolio.totalProfit >= 0 ? 'text-green-500' : 'text-red-500'}`}>
                            {portfolio.totalProfit >= 0 ? '+' : ''}${portfolio.totalProfit.toLocaleString()}
                        </p>
                    </CardContent>
                </Card>
                <Card className="bg-[#1E293B]/50 border-none">
                    <CardContent className="p-6">
                        <p className="text-gray-400 text-sm">수익률</p>
                        <p className={`text-2xl font-bold ${portfolio.totalProfitRate >= 0 ? 'text-green-500' : 'text-red-500'}`}>
                            {portfolio.totalProfitRate >= 0 ? '+' : ''}{portfolio.totalProfitRate.toFixed(2)}%
                        </p>
                    </CardContent>
                </Card>
            </div>

            {/* Holdings */}
            <Card className="bg-[#1E293B]/50 border-none">
                <CardHeader className="flex flex-row items-center justify-between">
                    <CardTitle>보유 종목</CardTitle>
                    <Button
                        onClick={() => setIsAddHoldingOpen(true)}
                        className="bg-indigo-600 hover:bg-indigo-700"
                    >
                        <Plus className="w-4 h-4 mr-2" />
                        종목 추가
                    </Button>
                </CardHeader>
                <CardContent>
                    {portfolio.holdings.length === 0 ? (
                        <div className="text-center py-8 text-gray-400">
                            보유 종목이 없습니다. 종목을 추가해주세요.
                        </div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead>
                                    <tr className="text-left text-gray-400 text-sm border-b border-gray-700">
                                        <th className="pb-3">종목</th>
                                        <th className="pb-3">유형</th>
                                        <th className="pb-3 text-right">수량</th>
                                        <th className="pb-3 text-right">평균가</th>
                                        <th className="pb-3 text-right">현재가</th>
                                        <th className="pb-3 text-right">평가금액</th>
                                        <th className="pb-3 text-right">수익</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {portfolio.holdings.map((holding) => (
                                        <tr key={holding.id} className="border-b border-gray-800 text-white">
                                            <td className="py-4 font-medium">{holding.symbol}</td>
                                            <td className="py-4">
                                                <span className={`px-2 py-1 rounded text-xs ${
                                                    holding.assetType === 'CRYPTO'
                                                        ? 'bg-purple-500/20 text-purple-400'
                                                        : 'bg-blue-500/20 text-blue-400'
                                                }`}>
                                                    {holding.assetType === 'CRYPTO' ? '암호화폐' : '주식'}
                                                </span>
                                            </td>
                                            <td className="py-4 text-right">{holding.quantity}</td>
                                            <td className="py-4 text-right">${holding.averagePrice.toLocaleString()}</td>
                                            <td className="py-4 text-right">${holding.currentPrice.toLocaleString()}</td>
                                            <td className="py-4 text-right">${holding.totalValue.toLocaleString()}</td>
                                            <td className="py-4 text-right">
                                                <div className={`flex items-center justify-end gap-1 ${
                                                    holding.profit >= 0 ? 'text-green-500' : 'text-red-500'
                                                }`}>
                                                    {holding.profit >= 0 ? (
                                                        <TrendingUp className="w-4 h-4" />
                                                    ) : (
                                                        <TrendingDown className="w-4 h-4" />
                                                    )}
                                                    <span>
                                                        {holding.profit >= 0 ? '+' : ''}${holding.profit.toLocaleString()}
                                                        ({holding.profitRate.toFixed(2)}%)
                                                    </span>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </CardContent>
            </Card>

            {/* Modals */}
            <AddHoldingModal
                isOpen={isAddHoldingOpen}
                portfolioId={portfolioId}
                onClose={() => setIsAddHoldingOpen(false)}
                onAdded={loadPortfolio}
            />
            <EditPortfolioModal
                isOpen={isEditOpen}
                portfolio={portfolio}
                onClose={() => setIsEditOpen(false)}
                onUpdated={loadPortfolio}
            />
        </div>
    );
}
