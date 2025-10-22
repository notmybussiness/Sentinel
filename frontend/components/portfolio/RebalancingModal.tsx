"use client";

import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Modal } from '@/components/ui/Modal';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { PriceDisplay } from '@/components/ui/PriceDisplay';
import { getRebalancingRecommendations } from '@/lib/api/rebalancing';
import {
  rebalancingStrategyLabels,
  rebalancingActionLabels,
  rebalancingActionColors,
} from '@/lib/api/rebalancing';
import type {
  RebalancingRequest,
  RebalancingResponse,
  RebalancingStrategy,
} from '@/types';

interface RebalancingModalProps {
  portfolioId: number;
  portfolioName: string;
  onClose: () => void;
}

/**
 * 리밸런싱 추천 모달 (Phase 7)
 * - 리밸런싱 전략 선택
 * - 추천 생성 및 결과 표시
 */
export function RebalancingModal({ portfolioId, portfolioName, onClose }: RebalancingModalProps) {
  const [strategy, setStrategy] = useState<RebalancingStrategy>('EQUAL_WEIGHT');
  const [thresholdPercent, setThresholdPercent] = useState(5.0);
  const [considerTaxes, setConsiderTaxes] = useState(false);
  const [results, setResults] = useState<RebalancingResponse | null>(null);

  const rebalancingMutation = useMutation({
    mutationFn: (request: RebalancingRequest) => getRebalancingRecommendations(request),
    onSuccess: (data) => {
      setResults(data);
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || '리밸런싱 추천 생성에 실패했습니다');
    },
  });

  const handleGenerateRecommendations = () => {
    if (thresholdPercent < 0 || thresholdPercent > 100) {
      alert('임계값은 0~100 사이여야 합니다');
      return;
    }

    rebalancingMutation.mutate({
      portfolioId,
      strategy,
      thresholdPercent,
      considerTaxes,
    });
  };

  return (
    <Modal isOpen={true} onClose={onClose} title="리밸런싱 추천">
      <div className="space-y-4">
        {/* Portfolio Info */}
        <div className="text-sm text-text-tertiary">
          포트폴리오: <span className="text-text-primary font-medium">{portfolioName}</span>
        </div>

        {!results ? (
          /* Configuration Form */
          <>
            {/* Strategy Selection */}
            <div>
              <label className="block text-text-primary text-sm font-medium mb-2">
                리밸런싱 전략
              </label>
              <select
                value={strategy}
                onChange={(e) => setStrategy(e.target.value as RebalancingStrategy)}
                className="w-full p-3 bg-background-secondary border border-border rounded-8 text-text-primary focus:border-brand focus:outline-none"
              >
                {(Object.keys(rebalancingStrategyLabels) as RebalancingStrategy[]).map((s) => (
                  <option key={s} value={s} disabled={s !== 'EQUAL_WEIGHT'}>
                    {rebalancingStrategyLabels[s]} {s !== 'EQUAL_WEIGHT' && '(준비 중)'}
                  </option>
                ))}
              </select>
            </div>

            {/* Threshold */}
            <div>
              <label className="block text-text-primary text-sm font-medium mb-2">
                리밸런싱 임계값 (%)
              </label>
              <input
                type="number"
                value={thresholdPercent}
                onChange={(e) => setThresholdPercent(Number(e.target.value))}
                min="0"
                max="100"
                step="0.1"
                className="w-full p-3 bg-background-secondary border border-border rounded-8 text-text-primary focus:border-brand focus:outline-none"
              />
              <p className="text-mini text-text-tertiary mt-1">
                비중 차이가 이 값보다 크면 리밸런싱을 추천합니다
              </p>
            </div>

            {/* Consider Taxes */}
            <div className="flex items-center">
              <input
                type="checkbox"
                id="considerTaxes"
                checked={considerTaxes}
                onChange={(e) => setConsiderTaxes(e.target.checked)}
                className="mr-2"
              />
              <label htmlFor="considerTaxes" className="text-text-primary text-sm">
                세금 효율성 고려
              </label>
            </div>

            {/* Generate Button */}
            <div className="flex gap-2 pt-2">
              <Button
                variant="secondary"
                size="lg"
                onClick={onClose}
                className="flex-1"
              >
                취소
              </Button>
              <Button
                variant="primary"
                size="lg"
                onClick={handleGenerateRecommendations}
                disabled={rebalancingMutation.isPending}
                className="flex-1"
              >
                {rebalancingMutation.isPending ? '분석 중...' : '추천 생성'}
              </Button>
            </div>
          </>
        ) : (
          /* Results Display */
          <>
            {/* Summary */}
            <Card padding="sm">
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <span className="text-text-tertiary">현재 가치:</span>
                  <PriceDisplay
                    amount={results.currentValue}
                    currency="USD"
                    size="md"
                    className="ml-2"
                  />
                </div>
                <div>
                  <span className="text-text-tertiary">리밸런싱 필요:</span>
                  <span className={`ml-2 font-medium ${results.needsRebalancing ? 'text-brand' : 'text-success'}`}>
                    {results.needsRebalancing ? '예' : '아니오'}
                  </span>
                </div>
                <div>
                  <span className="text-text-tertiary">거래 비용:</span>
                  <PriceDisplay
                    amount={results.totalTransactionCost}
                    currency="USD"
                    size="sm"
                    className="ml-2"
                  />
                </div>
                {considerTaxes && (
                  <div>
                    <span className="text-text-tertiary">예상 세금:</span>
                    <PriceDisplay
                      amount={results.estimatedTaxImpact}
                      currency="USD"
                      size="sm"
                      className="ml-2"
                    />
                  </div>
                )}
              </div>
            </Card>

            {/* Recommendations */}
            {results.needsRebalancing && results.recommendations.length > 0 && (
              <div>
                <h4 className="text-text-primary font-semibold mb-2">추천 사항</h4>
                <div className="space-y-2 max-h-[400px] overflow-y-auto">
                  {results.recommendations.map((rec) => (
                    <Card key={rec.symbol} padding="sm">
                      <div className="flex items-center justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <span className="text-text-primary font-semibold">{rec.symbol}</span>
                            <span
                              className={`text-sm font-medium ${rebalancingActionColors[rec.action]}`}
                            >
                              {rebalancingActionLabels[rec.action]}
                            </span>
                          </div>
                          <div className="text-mini text-text-tertiary mt-1">
                            현재: {rec.currentWeight.toFixed(1)}% → 목표: {rec.targetWeight.toFixed(1)}%
                          </div>
                        </div>
                        <div className="text-right">
                          {rec.action !== 'HOLD' && (
                            <>
                              <div className="text-sm text-text-primary font-medium">
                                {rec.quantity.toFixed(2)}주
                              </div>
                              <PriceDisplay
                                amount={rec.estimatedAmount}
                                currency="USD"
                                size="sm"
                              />
                            </>
                          )}
                        </div>
                      </div>
                    </Card>
                  ))}
                </div>
              </div>
            )}

            {/* No Rebalancing Needed */}
            {!results.needsRebalancing && (
              <div className="text-center py-6">
                <div className="text-4xl mb-2">✅</div>
                <p className="text-text-primary font-medium">리밸런싱이 필요하지 않습니다</p>
                <p className="text-text-tertiary text-sm mt-1">
                  모든 종목이 목표 비중 범위 내에 있습니다
                </p>
              </div>
            )}

            {/* Actions */}
            <div className="flex gap-2 pt-2">
              <Button
                variant="secondary"
                size="lg"
                onClick={() => setResults(null)}
                className="flex-1"
              >
                다시 설정
              </Button>
              <Button
                variant="primary"
                size="lg"
                onClick={onClose}
                className="flex-1"
              >
                확인
              </Button>
            </div>
          </>
        )}
      </div>
    </Modal>
  );
}
