'use client';

import { useState } from 'react';
import { X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { api } from '@/lib/api/client';

interface CreatePortfolioModalProps {
    isOpen: boolean;
    onClose: () => void;
    onCreated: () => void;
}

export function CreatePortfolioModal({ isOpen, onClose, onCreated }: CreatePortfolioModalProps) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!name.trim()) {
            setError('포트폴리오 이름을 입력해주세요.');
            return;
        }

        setIsLoading(true);
        setError(null);

        try {
            await api.portfolio.create({ name: name.trim(), description: description.trim() });
            setName('');
            setDescription('');
            onCreated();
            onClose();
        } catch (err) {
            setError(err instanceof Error ? err.message : '포트폴리오 생성에 실패했습니다.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
            {/* Backdrop */}
            <div className="absolute inset-0 bg-black/60" onClick={onClose} />

            {/* Modal */}
            <div className="relative z-10 w-full max-w-md bg-[#1E293B] rounded-xl p-6 shadow-xl">
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl font-semibold text-white">새 포트폴리오 생성</h2>
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
                            placeholder="예: 성장주 포트폴리오"
                            className="w-full px-4 py-3 bg-[#0F172A] border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-gray-400 mb-2">설명</label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="포트폴리오에 대한 설명 (선택)"
                            rows={3}
                            className="w-full px-4 py-3 bg-[#0F172A] border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
                        />
                    </div>

                    {error && (
                        <div className="text-red-400 text-sm">{error}</div>
                    )}

                    <div className="flex gap-3 pt-4">
                        <Button
                            type="button"
                            variant="ghost"
                            onClick={onClose}
                            className="flex-1 text-gray-400"
                        >
                            취소
                        </Button>
                        <Button
                            type="submit"
                            disabled={isLoading}
                            className="flex-1 bg-indigo-600 hover:bg-indigo-700"
                        >
                            {isLoading ? '생성 중...' : '생성하기'}
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
}
