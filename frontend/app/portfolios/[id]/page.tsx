"use client";

import React, { useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { PageHeader } from '@/components/ui/PageHeader';
import { Button } from '@/components/ui/Button';
import { StatCard } from '@/components/ui/StatCard';
import { SimpleChart } from '@/components/ui/SimpleChart';
import { Card } from '@/components/ui/Card';
import { PriceDisplay } from '@/components/ui/PriceDisplay';
import { PercentageChange } from '@/components/ui/PercentageChange';
import { MockDataBadge } from '@/components/common/MockDataBadge';
import { RebalancingModal } from '@/components/portfolio/RebalancingModal';
import { mockUserPortfolios, mockRebalancingRecommendations } from '@/lib/mockData';

export default function PortfolioDetailPage() {
  const router = useRouter();
  const params = useParams();
  const portfolioId = Number(params.id);

  const [showRebalancing, setShowRebalancing] = useState(false);

  // 포트폴리오 찾기
  const portfolio = mockUserPortfolios.find((p) => p.id === portfolioId);

  if (!portfolio) {
    return (
      <div className="min-h-screen bg-background-primary flex items-center justify-center">
        <div className="text-center">
          <div className="text-6xl mb-4">📂</div>
          <h2 className="text-text-primary text-2xl font-semibold mb-2">
            포트폴리오를 찾을 수 없습니다
          </h2>
          <Button
            variant="primary"
            onClick={() => router.push('/portfolios')}
          >
            목록으로 돌아가기
          </Button>
        </div>
      </div>
    );
  }

  // Mock 차트 데이터
  const chartData = Array.from({ length: 30 }, (_, i) => {
    const progress = i / 30;
    const trend =
      portfolio.totalCost * (1 + (portfolio.totalGainLossPercent / 100) * progress);
    const noise = (Math.random() - 0.5) * portfolio.totalValue * 0.02;
    return trend + noise;
  });

  return (
    <div className="min-h-screen bg-background-primary">
      <PageHeader
        title={portfolio.name}
        subtitle={portfolio.description}
        actions={
          <>
            <Button
              variant="secondary"
              size="md"
              onClick={() => alert('포트폴리오 편집 (준비 중)')}
            >
              편집
            </Button>
            <Button
              variant="primary"
              size="md"
              onClick={() => setShowRebalancing(true)}
            >
              리밸런싱
            </Button>
          </>
        }
      />

      <div className="max-w-6xl mx-auto px-8 py-8 space-y-6">
        {/* 통계 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          <StatCard
            label="총 자산"
            value={<PriceDisplay amount={portfolio.totalValue} currency="KRW" />}
            isMock={portfolio.isMock}
          />
          <StatCard
            label="투자 원금"
            value={<PriceDisplay amount={portfolio.totalCost} currency="KRW" />}
            isMock={portfolio.isMock}
          />
          <StatCard
            label="총 수익"
            value={
              <PriceDisplay
                amount={Math.abs(portfolio.totalGainLoss)}
                currency="KRW"
                color={portfolio.totalGainLoss >= 0 ? 'success' : 'error'}
              />
            }
            change={portfolio.totalGainLossPercent}
            isMock={portfolio.isMock}
          />
          <StatCard
            label="수익률"
            value={`${portfolio.totalGainLossPercent.toFixed(2)}%`}
            change={portfolio.totalGainLossPercent}
            changePeriod="누적"
            isMock={portfolio.isMock}
          />
        </div>

        {/* 차트 */}
        <Card padding="sm">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-text-primary font-semibold">
              포트폴리오 가치 추이
            </h3>
            {portfolio.isMock && <MockDataBadge show={true} size="sm" />}
          </div>
          <SimpleChart data={chartData} height={160} />
        </Card>

        {/* 보유 종목 */}
        <Card padding="sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-text-primary font-semibold">
              보유 종목
            </h3>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => alert('종목 추가 (준비 중)')}
            >
              + 종목 추가
            </Button>
          </div>

          {portfolio.holdings.length === 0 ? (
            <div className="text-center py-8">
              <div className="text-4xl mb-3">📊</div>
              <p className="text-text-tertiary">
                아직 보유 종목이 없습니다
              </p>
              <Button
                variant="primary"
                size="sm"
                className="mt-4"
                onClick={() => alert('종목 추가 (준비 중)')}
              >
                종목 추가하기
              </Button>
            </div>
          ) : (
            <div className="space-y-2">
              {portfolio.holdings.map((holding) => (
                <div
                  key={holding.id}
                  className="p-3 bg-background-secondary rounded-8 hover:bg-background-tertiary transition-colors"
                >
                  <div className="flex items-center justify-between">
                    {/* 종목 정보 */}
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <h4 className="text-text-primary font-semibold text-sm">
                          {holding.symbol}
                        </h4>
                        <span className="text-text-tertiary text-mini">
                          {holding.name}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 text-mini">
                        <span className="text-text-tertiary">
                          보유: {holding.quantity}주
                        </span>
                        <span className="text-text-tertiary">
                          평단: <PriceDisplay amount={holding.averageCost} currency="KRW" size="sm" />
                        </span>
                        <span className="text-text-tertiary">
                          현재가: <PriceDisplay amount={holding.currentPrice} currency="KRW" size="sm" />
                        </span>
                      </div>
                    </div>

                    {/* 수익 정보 */}
                    <div className="text-right">
                      <PriceDisplay
                        amount={holding.marketValue}
                        currency="KRW"
                        size="md"
                      />
                      <div className="flex items-center gap-1.5 justify-end mt-0.5">
                        <PriceDisplay
                          amount={Math.abs(holding.gainLoss)}
                          currency="KRW"
                          size="sm"
                          color={holding.gainLoss >= 0 ? 'success' : 'error'}
                        />
                        <PercentageChange
                          value={holding.gainLossPercent}
                          size="sm"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      {/* 리밸런싱 모달 */}
      <RebalancingModal
        isOpen={showRebalancing}
        onClose={() => setShowRebalancing(false)}
        recommendations={mockRebalancingRecommendations}
        onExecute={() => {
          alert('리밸런싱 실행! (준비 중)');
          setShowRebalancing(false);
        }}
      />
    </div>
  );
}