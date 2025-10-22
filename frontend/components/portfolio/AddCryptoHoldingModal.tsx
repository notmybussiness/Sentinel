"use client";

import React, { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Modal, ModalFooter } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { searchCrypto, type CryptoSearchResult } from '@/lib/api/crypto';
import { addHolding, type AddHoldingRequest } from '@/lib/api/portfolio';

interface AddCryptoHoldingModalProps {
  isOpen: boolean;
  onClose: () => void;
  portfolioId: number;
  onSuccess: () => void;
}

/**
 * AddCryptoHoldingModal 컴포넌트
 *
 * 암호화폐 Holdings 추가를 위한 Modal
 * - Crypto Symbol Search 통합
 * - 수량/평균가 입력
 * - Base Currency 선택 (KRW/USD)
 * - API 연동
 */
export function AddCryptoHoldingModal({
  isOpen,
  onClose,
  portfolioId,
  onSuccess,
}: AddCryptoHoldingModalProps) {
  // Form state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCrypto, setSelectedCrypto] = useState<CryptoSearchResult | null>(null);
  const [quantity, setQuantity] = useState('');
  const [averageCost, setAverageCost] = useState('');
  const [baseCurrency, setBaseCurrency] = useState<'KRW' | 'USD'>('KRW');

  // Search state
  const [searchResults, setSearchResults] = useState<CryptoSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  // Validation errors
  const [errors, setErrors] = useState<{ [key: string]: string }>({});

  // Reset form when modal closes
  useEffect(() => {
    if (!isOpen) {
      setSearchQuery('');
      setSelectedCrypto(null);
      setQuantity('');
      setAverageCost('');
      setBaseCurrency('KRW');
      setSearchResults([]);
      setErrors({});
      setSearchError(null);
    }
  }, [isOpen]);

  // Debounced crypto search
  useEffect(() => {
    if (!searchQuery || searchQuery.length < 1) {
      setSearchResults([]);
      return;
    }

    const timer = setTimeout(async () => {
      setIsSearching(true);
      setSearchError(null);
      try {
        const results = await searchCrypto(searchQuery);
        setSearchResults(results);
      } catch {
        setSearchError('검색 중 오류가 발생했습니다');
        setSearchResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Add holding mutation
  const { mutate: addHoldingMutation, isLoading: isSubmitting } = useMutation({
    mutationFn: (data: AddHoldingRequest) => addHolding(portfolioId, data),
    onSuccess: () => {
      onSuccess();
      onClose();
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      setErrors({
        submit: err.response?.data?.message || '암호화폐 추가에 실패했습니다',
      });
    },
  });

  // Handle crypto selection
  const handleSelectCrypto = (result: CryptoSearchResult) => {
    setSelectedCrypto(result);
    setSearchQuery('');
    setSearchResults([]);
    setErrors({});
  };

  // Validate form
  const validate = (): boolean => {
    const newErrors: { [key: string]: string } = {};

    if (!selectedCrypto) {
      newErrors.symbol = '암호화폐를 선택해주세요';
    }

    const qty = parseFloat(quantity);
    if (!quantity || isNaN(qty) || qty <= 0) {
      newErrors.quantity = '수량은 0보다 큰 숫자여야 합니다';
    }

    const cost = parseFloat(averageCost);
    if (!averageCost || isNaN(cost) || cost <= 0) {
      newErrors.averageCost = '평균 매수가는 0보다 큰 숫자여야 합니다';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Handle submit
  const handleSubmit = () => {
    if (!validate()) return;

    addHoldingMutation({
      symbol: selectedCrypto!.symbol,
      quantity: parseFloat(quantity),
      averageCost: parseFloat(averageCost),
      assetType: 'CRYPTO',
      baseCurrency: baseCurrency,
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="암호화폐 추가" size="md">
      <div className="space-y-4">
        {/* Crypto Search Section */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            암호화폐 검색
          </label>

          {/* Selected Crypto Display */}
          {selectedCrypto ? (
            <div className="p-3 bg-background-secondary rounded-8 flex items-center justify-between">
              <div>
                <div className="font-semibold text-text-primary">
                  {selectedCrypto.symbol}
                </div>
                <div className="text-mini text-text-tertiary">
                  {selectedCrypto.koreanName} ({selectedCrypto.name})
                </div>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setSelectedCrypto(null)}
              >
                변경
              </Button>
            </div>
          ) : (
            <>
              {/* Search Input */}
              <Input
                type="text"
                placeholder="암호화폐 심볼 또는 이름을 입력하세요 (예: BTC, Bitcoin)"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                error={errors.symbol}
              />

              {/* Search Results */}
              {searchQuery && (
                <div className="mt-2 bg-background-secondary rounded-8 max-h-60 overflow-y-auto">
                  {isSearching ? (
                    <div className="p-4 text-center text-text-tertiary">
                      검색 중...
                    </div>
                  ) : searchError ? (
                    <div className="p-4 text-center text-accent-red">
                      {searchError}
                    </div>
                  ) : searchResults.length === 0 ? (
                    <div className="p-4 text-center text-text-tertiary">
                      검색 결과가 없습니다
                    </div>
                  ) : (
                    searchResults.map((result) => (
                      <button
                        key={result.marketCode}
                        onClick={() => handleSelectCrypto(result)}
                        className="w-full p-3 text-left hover:bg-background-tertiary transition-colors border-b border-border-primary last:border-0"
                      >
                        <div className="font-semibold text-text-primary">
                          {result.symbol}
                        </div>
                        <div className="text-mini text-text-tertiary">
                          {result.koreanName} ({result.name}) - {result.exchange}
                        </div>
                      </button>
                    ))
                  )}
                </div>
              )}
            </>
          )}
        </div>

        {/* Base Currency Selection */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            기준 통화
          </label>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setBaseCurrency('KRW')}
              className={`flex-1 px-4 py-2 rounded-8 border transition-colors ${
                baseCurrency === 'KRW'
                  ? 'bg-brand-primary/20 border-brand-primary text-brand-primary'
                  : 'bg-background-secondary border-border-primary text-text-secondary hover:border-brand-primary/50'
              }`}
            >
              KRW (원화)
            </button>
            <button
              type="button"
              onClick={() => setBaseCurrency('USD')}
              className={`flex-1 px-4 py-2 rounded-8 border transition-colors ${
                baseCurrency === 'USD'
                  ? 'bg-brand-primary/20 border-brand-primary text-brand-primary'
                  : 'bg-background-secondary border-border-primary text-text-secondary hover:border-brand-primary/50'
              }`}
            >
              USD (달러)
            </button>
          </div>
        </div>

        {/* Quantity Input */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            수량
          </label>
          <Input
            type="number"
            placeholder="보유 수량을 입력하세요"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            error={errors.quantity}
            min="0"
            step="0.00000001"
          />
        </div>

        {/* Average Cost Input */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            평균 매수가 ({baseCurrency})
          </label>
          <Input
            type="number"
            placeholder="평균 매수가를 입력하세요"
            value={averageCost}
            onChange={(e) => setAverageCost(e.target.value)}
            error={errors.averageCost}
            min="0"
            step={baseCurrency === 'KRW' ? '100' : '0.01'}
          />
        </div>

        {/* Submit Error */}
        {errors.submit && (
          <div className="p-3 bg-accent-red/10 border border-accent-red rounded-8">
            <p className="text-sm text-accent-red">{errors.submit}</p>
          </div>
        )}
      </div>

      {/* Footer */}
      <ModalFooter>
        <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>
          취소
        </Button>
        <Button
          variant="primary"
          onClick={handleSubmit}
          disabled={isSubmitting || !selectedCrypto}
        >
          {isSubmitting ? '추가 중...' : '암호화폐 추가'}
        </Button>
      </ModalFooter>
    </Modal>
  );
}
