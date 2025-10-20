"use client";

import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { PageHeader } from '@/components/ui/PageHeader';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { BacktestResults } from '@/components/backtest/BacktestResults';
import { getPortfolios } from '@/lib/api/portfolio';
import { runBacktest, getDefaultDateRange, rebalanceFrequencyLabels } from '@/lib/api/backtest';
import type { Portfolio, BacktestRequest, BacktestResponse } from '@/types';
import { useAuth } from '@/contexts/AuthContext';

export default function LabPage() {
  const { isAuthenticated } = useAuth();

  // Form state
  const [selectedPortfolioId, setSelectedPortfolioId] = useState<number | null>(null);
  const [startDate, setStartDate] = useState(getDefaultDateRange('1y').startDate);
  const [endDate, setEndDate] = useState(getDefaultDateRange('1y').endDate);
  const [initialCapital, setInitialCapital] = useState(10000);
  const [rebalanceFrequency, setRebalanceFrequency] = useState<BacktestRequest['rebalanceFrequency']>('QUARTERLY');

  // Results state
  const [backtestResults, setBacktestResults] = useState<BacktestResponse | null>(null);

  // Fetch portfolios
  const { data: portfolios, isLoading: isLoadingPortfolios } = useQuery<Portfolio[]>({
    queryKey: ['portfolios'],
    queryFn: getPortfolios,
    enabled: isAuthenticated,
  });

  // Backtest mutation
  const backtestMutation = useMutation({
    mutationFn: (request: BacktestRequest) => runBacktest(request),
    onSuccess: (data) => {
      setBacktestResults(data);
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || '백테스팅에 실패했습니다');
    },
  });

  // Handle backtest
  const handleRunBacktest = () => {
    if (!selectedPortfolioId) {
      alert('포트폴리오를 선택해주세요');
      return;
    }

    if (!startDate || !endDate) {
      alert('날짜 범위를 입력해주세요');
      return;
    }

    if (initialCapital <= 0) {
      alert('초기 투자 금액을 입력해주세요');
      return;
    }

    backtestMutation.mutate({
      portfolioId: selectedPortfolioId,
      startDate,
      endDate,
      initialCapital,
      rebalanceFrequency,
    });
  };

  // Handle date range preset
  const handleDateRangePreset = (period: '1y' | '2y' | '3y' | '5y') => {
    const range = getDefaultDateRange(period);
    setStartDate(range.startDate);
    setEndDate(range.endDate);
  };

  return (
    <div className="min-h-screen bg-background-primary">
      <PageHeader
        title="백테스팅 Lab"
        subtitle="과거 데이터로 포트폴리오 전략을 시뮬레이션하세요"
      />

      <div className="max-w-6xl mx-auto px-8 py-8 space-y-6">
        {/* 백테스팅 폼 */}
        <Card padding="sm">
          <h3 className="text-text-primary font-semibold mb-4">백테스팅 설정</h3>

          {/* Portfolio Selection */}
          <div className="mb-4">
            <label className="block text-text-primary text-sm font-medium mb-2">
              포트폴리오 선택
            </label>
            {isLoadingPortfolios ? (
              <div className="text-text-tertiary text-sm">포트폴리오 불러오는 중...</div>
            ) : portfolios && portfolios.length > 0 ? (
              <select
                value={selectedPortfolioId || ''}
                onChange={(e) => setSelectedPortfolioId(Number(e.target.value))}
                className="w-full p-3 bg-background-secondary border border-border rounded-8 text-text-primary focus:border-brand focus:outline-none"
              >
                <option value="">포트폴리오 선택</option>
                {portfolios.map((portfolio) => (
                  <option key={portfolio.id} value={portfolio.id}>
                    {portfolio.name} ({portfolio.holdings.length}개 종목)
                  </option>
                ))}
              </select>
            ) : (
              <div className="text-text-tertiary text-sm">
                사용 가능한 포트폴리오가 없습니다
              </div>
            )}
          </div>

          {/* Date Range */}
          <div className="mb-4">
            <label className="block text-text-primary text-sm font-medium mb-2">
              백테스팅 기간
            </label>
            <div className="flex gap-2 mb-2">
              {(['1y', '2y', '3y', '5y'] as const).map((period) => (
                <Button
                  key={period}
                  variant="ghost"
                  size="sm"
                  onClick={() => handleDateRangePreset(period)}
                >
                  {period}
                </Button>
              ))}
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-text-tertiary text-mini mb-1">시작일</label>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full p-2 bg-background-secondary border border-border rounded-8 text-text-primary focus:border-brand focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-text-tertiary text-mini mb-1">종료일</label>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="w-full p-2 bg-background-secondary border border-border rounded-8 text-text-primary focus:border-brand focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Initial Capital */}
          <div className="mb-4">
            <label className="block text-text-primary text-sm font-medium mb-2">
              초기 투자 금액 (USD)
            </label>
            <input
              type="number"
              value={initialCapital}
              onChange={(e) => setInitialCapital(Number(e.target.value))}
              min="100"
              step="100"
              className="w-full p-3 bg-background-secondary border border-border rounded-8 text-text-primary focus:border-brand focus:outline-none"
            />
          </div>

          {/* Rebalance Frequency */}
          <div className="mb-4">
            <label className="block text-text-primary text-sm font-medium mb-2">
              리밸런싱 빈도
            </label>
            <div className="grid grid-cols-4 gap-2">
              {(Object.keys(rebalanceFrequencyLabels) as Array<BacktestRequest['rebalanceFrequency']>).map(
                (freq) => (
                  <button
                    key={freq}
                    onClick={() => setRebalanceFrequency(freq)}
                    className={`p-3 rounded-8 text-sm transition-colors ${
                      rebalanceFrequency === freq
                        ? 'bg-brand/20 border border-brand text-brand'
                        : 'bg-background-secondary border border-transparent text-text-primary hover:bg-background-tertiary'
                    }`}
                  >
                    {rebalanceFrequencyLabels[freq]}
                  </button>
                )
              )}
            </div>
          </div>

          {/* Run Button */}
          <Button
            variant="primary"
            size="lg"
            onClick={handleRunBacktest}
            disabled={backtestMutation.isPending || !selectedPortfolioId}
            className="w-full"
          >
            {backtestMutation.isPending ? '백테스팅 실행 중...' : '백테스팅 실행'}
          </Button>
        </Card>

        {/* Loading State */}
        {backtestMutation.isPending && (
          <Card padding="sm">
            <div className="text-center py-8">
              <div className="text-6xl mb-4 animate-pulse">📊</div>
              <h3 className="text-text-primary text-xl font-semibold mb-2">백테스팅 실행 중...</h3>
              <p className="text-text-tertiary">
                과거 데이터를 분석하고 성과 지표를 계산하고 있습니다
              </p>
            </div>
          </Card>
        )}

        {/* Results */}
        {backtestResults && !backtestMutation.isPending && (
          <BacktestResults results={backtestResults} />
        )}

        {/* Empty State */}
        {!backtestResults && !backtestMutation.isPending && (
          <Card padding="sm">
            <div className="text-center py-12">
              <div className="text-6xl mb-4">🧪</div>
              <h3 className="text-text-primary text-xl font-semibold mb-2">
                백테스팅 Lab에 오신 것을 환영합니다
              </h3>
              <p className="text-text-tertiary mb-4">
                과거 데이터를 기반으로 포트폴리오 전략을 시뮬레이션하고 성과를 분석하세요
              </p>
              <ul className="text-text-secondary text-sm space-y-2 max-w-md mx-auto text-left">
                <li>✓ 과거 주식 가격 데이터 기반 시뮬레이션</li>
                <li>✓ 7가지 성과 지표 (Sharpe, Sortino, Max Drawdown 등)</li>
                <li>✓ 리밸런싱 전략 (월별, 분기별, 연별)</li>
                <li>✓ 포트폴리오 가치 추이 시각화</li>
              </ul>
            </div>
          </Card>
        )}
      </div>
    </div>
  );
}
