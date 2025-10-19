"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs, Tab } from '@/components/ui/Tabs';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { PriceDisplay } from '@/components/ui/PriceDisplay';
import { PercentageChange } from '@/components/ui/PercentageChange';
import { SimpleChart } from '@/components/ui/SimpleChart';
import { EmptyState } from '@/components/common/EmptyState';
import { CreatePortfolioModal } from '@/components/portfolio/CreatePortfolioModal';
import { useAuth } from '@/contexts/AuthContext';
import { getPortfolios, type Portfolio } from '@/lib/api/portfolio';

export default function PortfoliosPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isAuthenticated } = useAuth();

  const [showCreatePortfolio, setShowCreatePortfolio] = useState(false);

  // Redirect to login if not authenticated (client-side only)
  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, router]);

  // Fetch portfolios from API
  const {
    data: portfolios = [],
    isLoading,
    error,
  } = useQuery<Portfolio[]>({
    queryKey: ['portfolios'],
    queryFn: getPortfolios,
    enabled: isAuthenticated,
    refetchInterval: 60000, // Refresh every minute
  });

  // Mock 차트 데이터 생성
  const generateChartData = (gainPercent: number) => {
    const baseValue = 100;
    const points = 30;
    const data = [baseValue];

    for (let i = 1; i < points; i++) {
      const progress = i / points;
      const trend = baseValue * (1 + (gainPercent / 100) * progress);
      const noise = (Math.random() - 0.5) * 5;
      data.push(trend + noise);
    }

    return data;
  };

  // Early return while redirecting
  if (!isAuthenticated) {
    return null;
  }

  const tabs: Tab[] = [
    {
      key: 'my',
      label: '내 포트폴리오',
      badge: portfolios.length,
      content: (
        <>
          {portfolios.length === 0 ? (
            <EmptyState
              icon={
                <svg
                  width="64"
                  height="64"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <rect x="2" y="7" width="20" height="14" rx="2" />
                  <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                </svg>
              }
              title="포트폴리오가 없습니다"
              description="첫 번째 포트폴리오를 만들어 투자를 시작하세요"
              actionLabel="포트폴리오 생성"
              onAction={() => setShowCreatePortfolio(true)}
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {portfolios.map((portfolio) => {
                const chartData = generateChartData(
                  portfolio.totalGainLossPercent
                );

                return (
                  <Card
                    key={portfolio.id}
                    onClick={() =>
                      router.push(`/portfolios/${portfolio.id}`)
                    }
                    padding="sm"
                    className="cursor-pointer hover:shadow-glow transition-shadow relative"
                  >
                    {/* 포트폴리오 이름 */}
                    <div className="mb-3">
                      <h3 className="text-text-primary text-lg font-semibold mb-0.5">
                        {portfolio.name}
                      </h3>
                      <p className="text-text-tertiary text-mini">
                        {portfolio.description}
                      </p>
                    </div>

                    {/* 차트 */}
                    <div className="mb-3">
                      <SimpleChart data={chartData} height={60} />
                    </div>

                    {/* 통계 */}
                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <div className="text-text-quaternary text-micro mb-0.5">
                          총 자산
                        </div>
                        <PriceDisplay
                          amount={portfolio.totalValue}
                          currency="KRW"
                          size="md"
                        />
                      </div>
                      <div>
                        <div className="text-text-quaternary text-micro mb-0.5">
                          총 수익
                        </div>
                        <div className="flex flex-col gap-0.5">
                          <PriceDisplay
                            amount={Math.abs(portfolio.totalGainLoss)}
                            currency="KRW"
                            size="sm"
                            color={
                              portfolio.totalGainLoss >= 0
                                ? 'success'
                                : 'error'
                            }
                          />
                          <PercentageChange
                            value={portfolio.totalGainLossPercent}
                            size="sm"
                          />
                        </div>
                      </div>
                    </div>

                    {/* 구성 종목 수 */}
                    <div className="mt-3 pt-3 border-t border-border-primary">
                      <span className="text-text-tertiary text-mini">
                        보유 종목: {portfolio.holdings.length}개
                      </span>
                    </div>
                  </Card>
                );
              })}
            </div>
          )}
        </>
      ),
    },
    {
      key: 'recommended',
      label: '추천 포트폴리오',
      content: (
        <div className="text-center py-16">
          <div className="text-6xl mb-4">🎯</div>
          <h3 className="text-text-primary text-xl font-semibold mb-2">
            준비 중입니다
          </h3>
          <p className="text-text-tertiary">
            전문가 추천 포트폴리오가 곧 제공될 예정입니다
          </p>
        </div>
      ),
    },
    {
      key: 'templates',
      label: '템플릿',
      content: (
        <div className="text-center py-16">
          <div className="text-6xl mb-4">📋</div>
          <h3 className="text-text-primary text-xl font-semibold mb-2">
            준비 중입니다
          </h3>
          <p className="text-text-tertiary">
            다양한 포트폴리오 템플릿이 곧 제공될 예정입니다
          </p>
        </div>
      ),
    },
  ];

  return (
    <div className="min-h-screen bg-background-primary">
      <PageHeader
        title="포트폴리오"
        subtitle="나의 투자 포트폴리오를 관리하세요"
        actions={
          <Button
            variant="primary"
            size="md"
            onClick={() => setShowCreatePortfolio(true)}
          >
            + 새 포트폴리오
          </Button>
        }
      />

      <div className="max-w-6xl mx-auto px-8 py-8">
        <Tabs tabs={tabs} defaultTab="my" />
      </div>

      {/* 포트폴리오 생성 모달 */}
      <CreatePortfolioModal
        isOpen={showCreatePortfolio}
        onClose={() => setShowCreatePortfolio(false)}
        onSuccess={() => {
          // Refresh portfolios list after creating
          queryClient.invalidateQueries({ queryKey: ['portfolios'] });
        }}
      />
    </div>
  );
}