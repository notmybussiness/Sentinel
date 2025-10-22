"use client";

import React, { useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { PageHeader } from '@/components/ui/PageHeader';
import { Button } from '@/components/ui/Button';
import { StatCard } from '@/components/ui/StatCard';
import { TradingChart } from '@/components/ui/TradingChart';
import { Card } from '@/components/ui/Card';
import { PriceDisplay } from '@/components/ui/PriceDisplay';
import { PercentageChange } from '@/components/ui/PercentageChange';
import { RebalancingModal } from '@/components/portfolio/RebalancingModal';
import { AddHoldingModal } from '@/components/portfolio/AddHoldingModal';
import { EditHoldingModal } from '@/components/portfolio/EditHoldingModal';
import { PortfolioAnalysisModal } from '@/components/portfolio/PortfolioAnalysisModal';
import { getPortfolio, deleteHolding, type Portfolio, type PortfolioHolding } from '@/lib/api/portfolio';
import { getBatchCryptoPrices, type CryptoPrice } from '@/lib/api/crypto';
import { useAuth } from '@/contexts/AuthContext';

export default function PortfolioDetailPage() {
  const router = useRouter();
  const params = useParams();
  const portfolioId = Number(params.id);
  const queryClient = useQueryClient();
  const { isAuthenticated } = useAuth();

  const [showRebalancing, setShowRebalancing] = useState(false);
  const [showAddHolding, setShowAddHolding] = useState(false);
  const [showEditHolding, setShowEditHolding] = useState(false);
  const [showAiAnalysis, setShowAiAnalysis] = useState(false);
  const [selectedHolding, setSelectedHolding] = useState<PortfolioHolding | null>(null);

  // Fetch portfolio from API
  const {
    data: portfolio,
    isLoading,
    error,
  } = useQuery<Portfolio>({
    queryKey: ['portfolio', portfolioId],
    queryFn: () => getPortfolio(portfolioId),
    enabled: isAuthenticated && !!portfolioId,
    refetchInterval: 60000, // Refresh every minute
  });

  // Holdings의 실시간 가격 조회 (암호화폐 배치 API 사용)
  const symbols = portfolio?.holdings.map((h) => h.symbol) || [];
  const baseCurrency = portfolio?.holdings[0]?.baseCurrency || 'KRW';

  const {
    data: prices,
    isLoading: isPricesLoading,
  } = useQuery<CryptoPrice[]>({
    queryKey: ['crypto-prices', portfolioId, symbols, baseCurrency],
    queryFn: () => getBatchCryptoPrices(symbols, baseCurrency),
    enabled: symbols.length > 0 && !!portfolio,
    refetchInterval: 60000, // 1분마다 자동 갱신
    staleTime: 30000, // 30초 동안은 캐시 사용
  });

  // 가격 맵 생성 (빠른 조회를 위해)
  const priceMap = prices?.reduce((acc, price) => {
    acc[price.symbol] = price.price;
    return acc;
  }, {} as Record<string, number>) || {};

  // Handle edit holding
  const handleEditHolding = (holding: PortfolioHolding) => {
    setSelectedHolding(holding);
    setShowEditHolding(true);
  };

  // Handle delete holding
  const handleDeleteHolding = async (holding: PortfolioHolding) => {
    if (!confirm(`${holding.symbol} 종목을 삭제하시겠습니까?`)) return;

    try {
      await deleteHolding(portfolioId, holding.id);
      // Refresh portfolio data
      queryClient.invalidateQueries({ queryKey: ['portfolio', portfolioId] });
      queryClient.invalidateQueries({ queryKey: ['portfolios'] });
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || '종목 삭제에 실패했습니다');
    }
  };

  // 차트 데이터 생성 (30일 추이)
  const chartData = portfolio ? Array.from({ length: 30 }, (_, i) => {
    // 30일 전부터 현재까지
    const daysAgo = 30 - i;
    const date = new Date();
    date.setDate(date.getDate() - daysAgo);
    const time = date.toISOString().split('T')[0]; // YYYY-MM-DD

    // 가치 계산 (선형 증가 + 노이즈)
    const progress = i / 30;
    const startValue = portfolio.totalCost;
    const endValue = portfolio.totalValue;
    const trend = startValue + (endValue - startValue) * progress;
    const noise = (Math.random() - 0.5) * Math.abs(endValue - startValue) * 0.05;
    const value = Math.max(0, trend + noise);

    return { time, value };
  }) : [];

  // Handle loading state
  if (isLoading) {
    return (
      <div className="min-h-screen bg-background-primary flex items-center justify-center">
        <div className="text-center">
          <div className="text-6xl mb-4 animate-pulse">📊</div>
          <h2 className="text-text-primary text-xl">포트폴리오 불러오는 중...</h2>
        </div>
      </div>
    );
  }

  // Handle error state
  if (error || !portfolio) {
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
              variant="secondary"
              size="md"
              onClick={() => setShowAiAnalysis(true)}
            >
              🤖 AI 분석
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
          />
          <StatCard
            label="투자 원금"
            value={<PriceDisplay amount={portfolio.totalCost} currency="KRW" />}
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
          />
          <StatCard
            label="수익률"
            value={`${portfolio.totalGainLossPercent.toFixed(2)}%`}
            change={portfolio.totalGainLossPercent}
            changePeriod="누적"
          />
        </div>

        {/* 차트 */}
        <Card padding="sm">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-text-primary font-semibold">
              포트폴리오 가치 추이
            </h3>
            <span className="text-text-tertiary text-mini">30일</span>
          </div>
          <TradingChart data={chartData} height={300} type="area" />
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
              onClick={() => setShowAddHolding(true)}
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
                onClick={() => setShowAddHolding(true)}
              >
                종목 추가하기
              </Button>
            </div>
          ) : (
            <div className="space-y-2">
              {portfolio.holdings.map((holding) => {
                // 실시간 가격 사용 (실시간 > DB 가격 > 평단가)
                const realtimePrice = priceMap[holding.symbol] || holding.currentPrice || holding.averageCost;
                const marketValue = realtimePrice * holding.quantity;
                const gainLoss = marketValue - (holding.averageCost * holding.quantity);
                const gainLossPercent = (gainLoss / (holding.averageCost * holding.quantity)) * 100;
                const isRealtime = !!priceMap[holding.symbol];
                const isPriceAvailable = !!(priceMap[holding.symbol] || holding.currentPrice);

                return (
                  <div
                    key={holding.id}
                    className="p-3 bg-background-secondary rounded-8 hover:bg-background-tertiary transition-colors"
                  >
                    <div className="flex items-center justify-between gap-3">
                      {/* 종목 정보 */}
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <h4 className="text-text-primary font-semibold text-sm">
                            {holding.symbol}
                          </h4>
                          {isRealtime && (
                            <span className="text-brand text-mini">● 실시간</span>
                          )}
                          {!isPriceAvailable && (
                            <span className="text-yellow-500 text-mini">● 평단가</span>
                          )}
                          {isPricesLoading && (
                            <span className="text-text-tertiary text-mini animate-pulse">로딩중...</span>
                          )}
                        </div>
                        <div className="flex items-center gap-3 text-mini">
                          <span className="text-text-tertiary">
                            보유: {holding.quantity}주
                          </span>
                          <span className="text-text-tertiary">
                            평단: <PriceDisplay amount={holding.averageCost} currency="KRW" size="sm" />
                          </span>
                          <span className="text-text-tertiary">
                            현재가: <PriceDisplay amount={realtimePrice} currency="KRW" size="sm" />
                          </span>
                        </div>
                      </div>

                      {/* 수익 정보 */}
                      <div className="text-right">
                        <PriceDisplay
                          amount={marketValue}
                          currency="KRW"
                          size="md"
                        />
                        <div className="flex items-center gap-1.5 justify-end mt-0.5">
                          <PriceDisplay
                            amount={Math.abs(gainLoss)}
                            currency="KRW"
                            size="sm"
                            color={gainLoss >= 0 ? 'success' : 'error'}
                          />
                          <PercentageChange
                            value={gainLossPercent}
                            size="sm"
                          />
                        </div>
                      </div>

                      {/* 액션 버튼 */}
                      <div className="flex flex-col gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleEditHolding(holding)}
                        >
                          수정
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeleteHolding(holding)}
                        >
                          삭제
                        </Button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>
      </div>

      {/* 리밸런싱 모달 */}
      {showRebalancing && (
        <RebalancingModal
          portfolioId={portfolioId}
          portfolioName={portfolio.name}
          onClose={() => setShowRebalancing(false)}
        />
      )}

      {/* 종목 추가 모달 */}
      <AddHoldingModal
        isOpen={showAddHolding}
        onClose={() => setShowAddHolding(false)}
        portfolioId={portfolioId}
        onSuccess={() => {
          // Refresh portfolio data after adding holding
          queryClient.invalidateQueries({ queryKey: ['portfolio', portfolioId] });
          queryClient.invalidateQueries({ queryKey: ['portfolios'] });
        }}
      />

      {/* 종목 수정 모달 */}
      <EditHoldingModal
        isOpen={showEditHolding}
        onClose={() => {
          setShowEditHolding(false);
          setSelectedHolding(null);
        }}
        portfolioId={portfolioId}
        holding={selectedHolding}
        onSuccess={() => {
          // Refresh portfolio data after editing holding
          queryClient.invalidateQueries({ queryKey: ['portfolio', portfolioId] });
          queryClient.invalidateQueries({ queryKey: ['portfolios'] });
        }}
      />

      {/* AI 분석 모달 */}
      <PortfolioAnalysisModal
        isOpen={showAiAnalysis}
        onClose={() => setShowAiAnalysis(false)}
        portfolioId={portfolioId}
      />
    </div>
  );
}