"use client";

import React, { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Modal, ModalFooter } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { searchSymbol, type SearchResult } from '@/lib/api/market';
import { addHolding, type AddHoldingRequest } from '@/lib/api/portfolio';

interface AddHoldingModalProps {
  isOpen: boolean;
  onClose: () => void;
  portfolioId: number;
  onSuccess: () => void;
}

/**
 * AddHoldingModal 컴포넌트
 *
 * Holdings 추가를 위한 Modal
 * - Symbol Search 통합
 * - 수량/평균가 입력
 * - API 연동
 */
export function AddHoldingModal({
  isOpen,
  onClose,
  portfolioId,
  onSuccess,
}: AddHoldingModalProps) {
  // Form state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSymbol, setSelectedSymbol] = useState<SearchResult | null>(null);
  const [quantity, setQuantity] = useState('');
  const [averageCost, setAverageCost] = useState('');

  // Search state
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  // Validation errors
  const [errors, setErrors] = useState<{ [key: string]: string }>({});

  // Reset form when modal closes
  useEffect(() => {
    if (!isOpen) {
      setSearchQuery('');
      setSelectedSymbol(null);
      setQuantity('');
      setAverageCost('');
      setSearchResults([]);
      setErrors({});
      setSearchError(null);
    }
  }, [isOpen]);

  // Debounced symbol search
  useEffect(() => {
    if (!searchQuery || searchQuery.length < 1) {
      setSearchResults([]);
      return;
    }

    const timer = setTimeout(async () => {
      setIsSearching(true);
      setSearchError(null);
      try {
        const results = await searchSymbol(searchQuery);
        setSearchResults(results);
      } catch (error) {
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
    onError: (error: any) => {
      setErrors({
        submit: error.response?.data?.message || '종목 추가에 실패했습니다',
      });
    },
  });

  // Handle symbol selection
  const handleSelectSymbol = (result: SearchResult) => {
    setSelectedSymbol(result);
    setSearchQuery('');
    setSearchResults([]);
    setErrors({});
  };

  // Validate form
  const validate = (): boolean => {
    const newErrors: { [key: string]: string } = {};

    if (!selectedSymbol) {
      newErrors.symbol = '종목을 선택해주세요';
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
      symbol: selectedSymbol!.symbol,
      quantity: parseFloat(quantity),
      averageCost: parseFloat(averageCost),
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="종목 추가" size="md">
      <div className="space-y-4">
        {/* Symbol Search Section */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            종목 검색
          </label>

          {/* Selected Symbol Display */}
          {selectedSymbol ? (
            <div className="p-3 bg-background-secondary rounded-8 flex items-center justify-between">
              <div>
                <div className="font-semibold text-text-primary">
                  {selectedSymbol.symbol}
                </div>
                <div className="text-mini text-text-tertiary">
                  {selectedSymbol.name}
                </div>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setSelectedSymbol(null)}
              >
                변경
              </Button>
            </div>
          ) : (
            <>
              {/* Search Input */}
              <Input
                type="text"
                placeholder="종목 심볼 또는 이름을 입력하세요 (예: AAPL, Apple)"
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
                        key={result.symbol}
                        onClick={() => handleSelectSymbol(result)}
                        className="w-full p-3 text-left hover:bg-background-tertiary transition-colors border-b border-border-primary last:border-0"
                      >
                        <div className="font-semibold text-text-primary">
                          {result.symbol}
                        </div>
                        <div className="text-mini text-text-tertiary">
                          {result.name} - {result.exchange}
                        </div>
                      </button>
                    ))
                  )}
                </div>
              )}
            </>
          )}
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
            step="0.01"
          />
        </div>

        {/* Average Cost Input */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            평균 매수가 (KRW)
          </label>
          <Input
            type="number"
            placeholder="평균 매수가를 입력하세요"
            value={averageCost}
            onChange={(e) => setAverageCost(e.target.value)}
            error={errors.averageCost}
            min="0"
            step="100"
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
          disabled={isSubmitting || !selectedSymbol}
        >
          {isSubmitting ? '추가 중...' : '종목 추가'}
        </Button>
      </ModalFooter>
    </Modal>
  );
}
