"use client";

import React, { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Modal, ModalFooter } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { searchSymbol, type SearchResult } from '@/lib/api/market';
import { searchCrypto, type CryptoSearchResult } from '@/lib/api/crypto';
import { addHolding, type AddHoldingRequest } from '@/lib/api/portfolio';

interface AddHoldingModalProps {
  isOpen: boolean;
  onClose: () => void;
  portfolioId: number;
  onSuccess: () => void;
}

type AssetType = 'STOCK' | 'CRYPTO';
type BaseCurrency = 'KRW' | 'USD';

/**
 * AddHoldingModal 컴포넌트 (통합 버전)
 *
 * 주식 및 암호화폐 Holdings 추가를 위한 통합 Modal
 * - Asset Type 선택 (STOCK / CRYPTO)
 * - Symbol Search 통합 (타입별 API 분기)
 * - 수량/평균가 입력
 * - CRYPTO 선택 시 Base Currency 선택 (KRW/USD)
 * - API 연동
 */
export function AddHoldingModal({
  isOpen,
  onClose,
  portfolioId,
  onSuccess,
}: AddHoldingModalProps) {
  // Asset Type state
  const [assetType, setAssetType] = useState<AssetType>('STOCK');

  // Form state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStock, setSelectedStock] = useState<SearchResult | null>(null);
  const [selectedCrypto, setSelectedCrypto] = useState<CryptoSearchResult | null>(null);
  const [quantity, setQuantity] = useState('');
  const [averageCost, setAverageCost] = useState('');
  const [baseCurrency, setBaseCurrency] = useState<BaseCurrency>('KRW');

  // Search state
  const [stockResults, setStockResults] = useState<SearchResult[]>([]);
  const [cryptoResults, setCryptoResults] = useState<CryptoSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  // Validation errors
  const [errors, setErrors] = useState<{ [key: string]: string }>({});

  // Reset form when modal closes
  useEffect(() => {
    if (!isOpen) {
      setAssetType('STOCK');
      setSearchQuery('');
      setSelectedStock(null);
      setSelectedCrypto(null);
      setQuantity('');
      setAverageCost('');
      setBaseCurrency('KRW');
      setStockResults([]);
      setCryptoResults([]);
      setErrors({});
      setSearchError(null);
    }
  }, [isOpen]);

  // Reset search when asset type changes
  useEffect(() => {
    setSearchQuery('');
    setSelectedStock(null);
    setSelectedCrypto(null);
    setStockResults([]);
    setCryptoResults([]);
    setErrors({});
    setSearchError(null);
  }, [assetType]);

  // Debounced search
  useEffect(() => {
    if (!searchQuery || searchQuery.length < 1) {
      setStockResults([]);
      setCryptoResults([]);
      return;
    }

    const timer = setTimeout(async () => {
      setIsSearching(true);
      setSearchError(null);
      try {
        if (assetType === 'STOCK') {
          const results = await searchSymbol(searchQuery);
          setStockResults(results);
        } else {
          const results = await searchCrypto(searchQuery);
          setCryptoResults(results);
        }
      } catch {
        setSearchError('검색 중 오류가 발생했습니다');
        setStockResults([]);
        setCryptoResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery, assetType]);

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
        submit: err.response?.data?.message || '종목 추가에 실패했습니다',
      });
    },
  });

  // Handle selection
  const handleSelectStock = (result: SearchResult) => {
    setSelectedStock(result);
    setSearchQuery('');
    setStockResults([]);
    setErrors({});
  };

  const handleSelectCrypto = (result: CryptoSearchResult) => {
    setSelectedCrypto(result);
    setSearchQuery('');
    setCryptoResults([]);
    setErrors({});
  };

  // Validate form
  const validate = (): boolean => {
    const newErrors: { [key: string]: string } = {};

    if (assetType === 'STOCK' && !selectedStock) {
      newErrors.symbol = '주식 종목을 선택해주세요';
    }
    if (assetType === 'CRYPTO' && !selectedCrypto) {
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

    const symbol = assetType === 'STOCK'
      ? selectedStock!.symbol
      : selectedCrypto!.symbol;

    const requestData: AddHoldingRequest = {
      symbol,
      quantity: parseFloat(quantity),
      averageCost: parseFloat(averageCost),
      assetType,
    };

    // Add baseCurrency only for CRYPTO
    if (assetType === 'CRYPTO') {
      requestData.baseCurrency = baseCurrency;
    }

    addHoldingMutation(requestData);
  };

  const selectedItem = assetType === 'STOCK' ? selectedStock : selectedCrypto;
  const searchResults = assetType === 'STOCK' ? stockResults : cryptoResults;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="종목 추가" size="md">
      <div className="space-y-4">
        {/* Asset Type Selection */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            종목 타입
          </label>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setAssetType('STOCK')}
              className={`flex-1 px-4 py-2 rounded-8 border transition-colors ${
                assetType === 'STOCK'
                  ? 'bg-brand-primary/20 border-brand-primary text-brand-primary'
                  : 'bg-background-secondary border-border-primary text-text-secondary hover:border-brand-primary/50'
              }`}
            >
              📈 주식
            </button>
            <button
              type="button"
              onClick={() => setAssetType('CRYPTO')}
              className={`flex-1 px-4 py-2 rounded-8 border transition-colors ${
                assetType === 'CRYPTO'
                  ? 'bg-brand-primary/20 border-brand-primary text-brand-primary'
                  : 'bg-background-secondary border-border-primary text-text-secondary hover:border-brand-primary/50'
              }`}
            >
              ₿ 암호화폐
            </button>
          </div>
        </div>

        {/* Search Section */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            {assetType === 'STOCK' ? '주식 종목 검색' : '암호화폐 검색'}
          </label>

          {/* Selected Item Display */}
          {selectedItem ? (
            <div className="p-3 bg-background-secondary rounded-8 flex items-center justify-between">
              <div>
                <div className="font-semibold text-text-primary">
                  {selectedItem.symbol}
                </div>
                <div className="text-mini text-text-tertiary">
                  {assetType === 'STOCK'
                    ? (selectedItem as SearchResult).name
                    : `${(selectedItem as CryptoSearchResult).koreanName} (${(selectedItem as CryptoSearchResult).name})`
                  }
                </div>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSelectedStock(null);
                  setSelectedCrypto(null);
                }}
              >
                변경
              </Button>
            </div>
          ) : (
            <>
              {/* Search Input */}
              <Input
                type="text"
                placeholder={assetType === 'STOCK'
                  ? '종목 심볼 또는 이름을 입력하세요 (예: AAPL, Apple)'
                  : '암호화폐 심볼 또는 이름을 입력하세요 (예: BTC, Bitcoin)'
                }
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
                  ) : assetType === 'STOCK' ? (
                    (searchResults as SearchResult[]).map((result) => (
                      <button
                        key={result.symbol}
                        onClick={() => handleSelectStock(result)}
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
                  ) : (
                    (searchResults as CryptoSearchResult[]).map((result) => (
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

        {/* Base Currency Selection (CRYPTO only) */}
        {assetType === 'CRYPTO' && (
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
        )}

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
            step={assetType === 'CRYPTO' ? '0.00000001' : '0.01'}
          />
        </div>

        {/* Average Cost Input */}
        <div>
          <label className="block text-sm font-medium text-text-primary mb-2">
            평균 매수가 {assetType === 'CRYPTO' ? `(${baseCurrency})` : '(USD)'}
          </label>
          <Input
            type="number"
            placeholder={assetType === 'STOCK' ? '평균 매수가를 입력하세요 (예: 150.50)' : '평균 매수가를 입력하세요'}
            value={averageCost}
            onChange={(e) => setAverageCost(e.target.value)}
            error={errors.averageCost}
            min="0"
            step={assetType === 'CRYPTO' && baseCurrency === 'KRW' ? '100' : '0.01'}
          />
          {assetType === 'STOCK' && (
            <p className="mt-1 text-mini text-text-tertiary">
              💡 주식 가격은 USD(달러)로 입력해주세요
            </p>
          )}
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
          disabled={isSubmitting || !selectedItem}
        >
          {isSubmitting ? '추가 중...' : '종목 추가'}
        </Button>
      </ModalFooter>
    </Modal>
  );
}
