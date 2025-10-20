"use client";

import React from 'react';
import { Card } from '@/components/ui/Card';
import { StatCard } from '@/components/ui/StatCard';
import { SimpleChart } from '@/components/ui/SimpleChart';
import { PriceDisplay } from '@/components/ui/PriceDisplay';
import type { BacktestResponse } from '@/types';

interface BacktestResultsProps {
  results: BacktestResponse;
}

/**
 * 백테스팅 결과 표시 컴포넌트
 */
export function BacktestResults({ results }: BacktestResultsProps) {
  const { performance, equityCurve, holdingsSummary } = results;

  // Equity Curve 차트 데이터
  const chartData = equityCurve.map((point) => point.value);

  return (
    <div className="space-y-6">
      {/* 기본 정보 */}
      <Card padding="sm">
        <h3 className="text-text-primary font-semibold mb-3">백테스팅 개요</h3>
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <span className="text-text-tertiary">포트폴리오:</span>
            <span className="text-text-primary ml-2">{results.portfolioName}</span>
          </div>
          <div>
            <span className="text-text-tertiary">리밸런싱:</span>
            <span className="text-text-primary ml-2">
              {results.rebalanceFrequency === 'NONE' && '없음'}
              {results.rebalanceFrequency === 'MONTHLY' && '월별'}
              {results.rebalanceFrequency === 'QUARTERLY' && '분기별'}
              {results.rebalanceFrequency === 'YEARLY' && '연별'}
            </span>
          </div>
          <div>
            <span className="text-text-tertiary">시작일:</span>
            <span className="text-text-primary ml-2">{results.startDate}</span>
          </div>
          <div>
            <span className="text-text-tertiary">종료일:</span>
            <span className="text-text-primary ml-2">{results.endDate}</span>
          </div>
        </div>
      </Card>

      {/* 성과 지표 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatCard
          label="총 수익률"
          value={`${performance.totalReturn.toFixed(2)}%`}
          change={performance.totalReturn}
        />
        <StatCard
          label="CAGR"
          value={`${performance.cagr.toFixed(2)}%`}
          change={performance.cagr}
        />
        <StatCard
          label="최대 낙폭"
          value={`${performance.maxDrawdown.toFixed(2)}%`}
          change={performance.maxDrawdown}
        />
        <StatCard
          label="변동성"
          value={`${performance.volatility.toFixed(2)}%`}
        />
      </div>

      {/* 추가 지표 */}
      <div className="grid grid-cols-3 gap-3">
        <Card padding="sm">
          <div className="text-text-tertiary text-sm mb-1">샤프 비율</div>
          <div className="text-text-primary text-2xl font-bold">
            {performance.sharpeRatio.toFixed(2)}
          </div>
        </Card>
        <Card padding="sm">
          <div className="text-text-tertiary text-sm mb-1">소르티노 비율</div>
          <div className="text-text-primary text-2xl font-bold">
            {performance.sortinoRatio.toFixed(2)}
          </div>
        </Card>
        <Card padding="sm">
          <div className="text-text-tertiary text-sm mb-1">승률</div>
          <div className="text-text-primary text-2xl font-bold">
            {performance.winRate.toFixed(1)}%
          </div>
        </Card>
      </div>

      {/* Equity Curve 차트 */}
      <Card padding="sm">
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-text-primary font-semibold">포트폴리오 가치 추이</h3>
          <div className="text-sm text-text-tertiary">
            <PriceDisplay amount={results.initialCapital} currency="USD" size="sm" /> →{' '}
            <PriceDisplay
              amount={results.finalValue}
              currency="USD"
              size="sm"
              color={results.finalValue >= results.initialCapital ? 'success' : 'error'}
            />
          </div>
        </div>
        <SimpleChart data={chartData} height={200} />
      </Card>

      {/* Holdings Summary */}
      <Card padding="sm">
        <h3 className="text-text-primary font-semibold mb-3">종목별 최종 요약</h3>
        <div className="space-y-2">
          {holdingsSummary.map((holding) => (
            <div
              key={holding.symbol}
              className="p-3 bg-background-secondary rounded-8 hover:bg-background-tertiary transition-colors"
            >
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="text-text-primary font-semibold text-sm">
                    {holding.symbol}
                  </div>
                  <div className="text-text-tertiary text-mini mt-0.5">
                    보유: {holding.finalQuantity.toFixed(2)}주 | 비중: {holding.finalWeight.toFixed(1)}%
                  </div>
                </div>
                <div className="text-right">
                  <PriceDisplay amount={holding.finalValue} currency="USD" size="md" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* 분석 시각 */}
      <div className="text-center text-text-tertiary text-mini">
        분석 완료: {new Date(results.executedAt).toLocaleString('ko-KR')}
      </div>
    </div>
  );
}
