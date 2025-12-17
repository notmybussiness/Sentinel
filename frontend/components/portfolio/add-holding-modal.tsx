'use client';

import { useState } from 'react';
import { X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { api } from '@/lib/api/client';

interface AddHoldingModalProps {
    isOpen: boolean;
    portfolioId: number;
    onClose: () => void;
    onAdded: () => void;
}

export function AddHoldingModal({ isOpen, portfolioId, onClose, onAdded }: AddHoldingModalProps) {
    const [symbol, setSymbol] = useState('');
    const [quantity, setQuantity] = useState('');
    const [averagePrice, setAveragePrice] = useState('');
    const [assetType, setAssetType] = useState<'STOCK' | 'CRYPTO'>('STOCK');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!symbol.trim() || !quantity || !averagePrice) {
            setError('모든 필드를 입력해주세요.');
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            await api.portfolio.addHolding(portfolioId, {
                symbol: symbol.trim().toUpperCase(),
                quantity: parseFloat(quantity),
                averagePrice: parseFloat(averagePrice),
                assetType,
            });
            setSymbol('');
            setQuantity('');
            setAveragePrice('');
            onAdded();
            onClose();
        } catch (err) {
            setError(err instanceof Error ? err.message : '종목 추가에 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
            <div className="absolute inset-0 bg-black/60" onClick={onClose} />
            <div className="relative z-10 w-full max-w-md bg-[#1E293B] rounded-xl p-6 shadow-xl">
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl font-semibold text-white">종목 추가</h2>
                    <button onClick={onClose} className="text-gray-400 hover:text-white">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm text-gray-400 mb-2">자산 유형</label>
                        <div className="flex gap-2">
                            <button
                                type="button"
                                onClick={() => setAssetType('STOCK')}
                                className={`flex-1 py-2 rounded-lg text-sm font-medium transition-colors ${
                                    assetType === 'STOCK'
                                        ? 'bg-indigo-600 text-white'
                                        : 'bg-[#0F172A] text-gray-400 hover:text-white'
                                }`}
                            >
                                주식
                            </button>
                            <button
                                type="button"
                                onClick={() => setAssetType('CRYPTO')}
                                className={`flex-1 py-2 rounded-lg text-sm font-medium transition-colors ${
                                    assetType === 'CRYPTO'
                                        ? 'bg-indigo-600 text-white'
                                        : 'bg-[#0F172A] text-gray-400 hover:text-white'
                                }`}
                            >
                                암호화폐
                            </button>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm text-gray-400 mb-2">심볼 *</label>
                        <input
                            type="text"
                            value={symbol}
                            onChange={(e) => setSymbol(e.target.value)}
                            placeholder={assetType === 'STOCK' ? 'AAPL, NVDA...' : 'BTC, ETH...'}
                            className="w-full px-4 py-3 bg-[#0F172A] border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-gray-400 mb-2">수량 *</label>
                        <input
                            type="number"
                            step="any"
                            value={quantity}
                            onChange={(e) => setQuantity(e.target.value)}
                            placeholder="10"
                            className="w-full px-4 py-3 bg-[#0F172A] border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-gray-400 mb-2">평균 매수가 *</label>
                        <input
                            type="number"
                            step="any"
                            value={averagePrice}
                            onChange={(e) => setAveragePrice(e.target.value)}
                            placeholder="150.00"
                            className="w-full px-4 py-3 bg-[#0F172A] border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        />
                    </div>

                    {error && <div className="text-red-400 text-sm">{error}</div>}

                    <div className="flex gap-3 pt-4">
                        <Button type="button" variant="ghost" onClick={onClose} className="flex-1 text-gray-400">
                            취소
                        </Button>
                        <Button type="submit" disabled={isLoading} className="flex-1 bg-indigo-600 hover:bg-indigo-700">
                            {isLoading ? '추가 중...' : '추가하기'}
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
}
