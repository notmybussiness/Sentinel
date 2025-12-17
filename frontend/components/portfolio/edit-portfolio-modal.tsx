'use client';

import { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { api } from '@/lib/api/client';
import { Portfolio } from '@/lib/api/types';

interface EditPortfolioModalProps {
    isOpen: boolean;
    portfolio: Portfolio | null;
    onClose: () => void;
    onUpdated: () => void;
}

export function EditPortfolioModal({ isOpen, portfolio, onClose, onUpdated }: EditPortfolioModalProps) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (portfolio) {
            setName(portfolio.name);
            setDescription(portfolio.description || '');
        }
    }, [portfolio]);

    if (!isOpen || !portfolio) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!name.trim()) {
            setError('포트폴리오 이름을 입력해주세요.');
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            await api.portfolio.update(portfolio.id, {
                name: name.trim(),
                description: description.trim(),
            });
            onUpdated();
            onClose();
        } catch (err) {
            setError(err instanceof Error ? err.message : '포트폴리오 수정에 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
            <div className="absolute inset-0 bg-black/60" onClick={onClose} />
            <div className="relative z-10 w-full max-w-md bg-[#1E293B] rounded-xl p-6 shadow-xl">
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl font-semibold text-white">포트폴리오 수정</h2>
                    <button onClick={onClose} className="text-gray-400 hover:text-white">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm text-gray-400 mb-2">이름 *</label>
                        <input
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className="w-full px-4 py-3 bg-[#0F172A] border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-gray-400 mb-2">설명</label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={3}
                            className="w-full px-4 py-3 bg-[#0F172A] border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
                        />
                    </div>

                    {error && <div className="text-red-400 text-sm">{error}</div>}

                    <div className="flex gap-3 pt-4">
                        <Button type="button" variant="ghost" onClick={onClose} className="flex-1 text-gray-400">
                            취소
                        </Button>
                        <Button type="submit" disabled={isLoading} className="flex-1 bg-indigo-600 hover:bg-indigo-700">
                            {isLoading ? '저장 중...' : '저장하기'}
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
}
