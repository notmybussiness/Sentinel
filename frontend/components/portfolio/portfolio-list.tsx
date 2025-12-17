'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Portfolio } from '@/lib/api/types';
import { api } from '@/lib/api/client';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { ArrowUpRight, Wallet, PieChart, Trash2 } from 'lucide-react';
import { CreatePortfolioModal } from './create-portfolio-modal';

export function PortfolioList() {
    const router = useRouter();
    const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [deletingId, setDeletingId] = useState<number | null>(null);

    const loadPortfolios = () => {
        api.portfolio.getAll().then(setPortfolios);
    };

    useEffect(() => {
        loadPortfolios();
    }, []);

    const handleDelete = async (id: number, e: React.MouseEvent) => {
        e.stopPropagation();
        if (!confirm('정말 삭제하시겠습니까?')) return;

        setDeletingId(id);
        try {
            await api.portfolio.delete(id);
            loadPortfolios();
        } catch (err) {
            alert('삭제에 실패했습니다.');
        } finally {
            setDeletingId(null);
        }
    };

    return (
        <>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {portfolios.map((p) => (
                    <Card
                        key={p.id}
                        className="bg-[#1E293B]/50 border-none hover:bg-[#1E293B] transition-colors group cursor-pointer"
                        onClick={() => router.push(`/portfolio/${p.id}`)}
                    >
                        <CardHeader>
                            <div className="flex justify-between items-start">
                                <div className="p-2 bg-indigo-500/10 rounded-lg text-indigo-400 group-hover:bg-indigo-500 group-hover:text-white transition-colors">
                                    <Wallet className="w-6 h-6" />
                                </div>
                                <div className="flex gap-1">
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        className="text-gray-500 hover:text-red-400 h-8 w-8"
                                        onClick={(e) => handleDelete(p.id, e)}
                                        disabled={deletingId === p.id}
                                    >
                                        <Trash2 className="w-4 h-4" />
                                    </Button>
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        className="text-gray-500 hover:text-white h-8 w-8"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            router.push(`/portfolio/${p.id}`);
                                        }}
                                    >
                                        <ArrowUpRight className="w-4 h-4" />
                                    </Button>
                                </div>
                            </div>
                            <CardTitle className="mt-4">{p.name}</CardTitle>
                            <CardDescription>{p.description}</CardDescription>
                        </CardHeader>
                        <CardContent>
                            <div className="space-y-2">
                                <div className="flex justify-between text-sm">
                                    <span className="text-gray-400">Total Value</span>
                                    <span className="font-bold text-white">${p.totalValue.toLocaleString()}</span>
                                </div>
                                <div className="flex justify-between text-sm">
                                    <span className="text-gray-400">Profit</span>
                                    <span className={p.totalProfit >= 0 ? "text-green-500" : "text-red-500"}>
                                        {p.totalProfit >= 0 ? "+" : ""}{p.totalProfit.toLocaleString()} ({p.totalProfitRate}%)
                                    </span>
                                </div>
                                <div className="h-2 w-full bg-gray-700 rounded-full mt-4 overflow-hidden">
                                    <div className="h-full bg-indigo-500" style={{ width: '60%' }}></div>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                ))}

                {/* Create New Card */}
                <Card
                    className="bg-[#1E293B]/20 border border-dashed border-gray-700 hover:border-indigo-500 hover:bg-[#1E293B]/40 transition-colors cursor-pointer flex flex-col items-center justify-center p-6 text-gray-500 hover:text-white"
                    onClick={() => setIsCreateModalOpen(true)}
                >
                    <div className="w-12 h-12 rounded-full bg-gray-800 flex items-center justify-center mb-4">
                        <PieChart className="w-6 h-6" />
                    </div>
                    <p className="font-medium">Create New Portfolio</p>
                </Card>
            </div>

            <CreatePortfolioModal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                onCreated={loadPortfolios}
            />
        </>
    );
}
