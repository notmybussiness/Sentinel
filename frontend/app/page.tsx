"use client";

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Section } from '@/components/ui/Section';
import { Carousel } from '@/components/ui/Carousel';
import { StatCard } from '@/components/ui/StatCard';
import { MarketCoinCard } from '@/components/market/MarketCoinCard';
import { CategoryTabs, type MarketCategory } from '@/components/market/CategoryTabs';
import { RecommendedPortfolioCard } from '@/components/portfolio/RecommendedPortfolioCard';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/contexts/AuthContext';
import { mockRecommendedPortfolios } from '@/lib/mockData';
import { getTrendingCoins } from '@/lib/api/crypto';
import { useQuery } from '@tanstack/react-query';
import type { TrendingCoinDto } from '@/lib/api/crypto';
import { getBatchPrices } from '@/lib/api/market';
import type { StockPrice } from '@/lib/api/market';
import { PercentageChange } from '@/components/ui/PercentageChange';


export default function HomePage() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const [activeCategory, setActiveCategory] = useState<MarketCategory>('hot');

  /**
   * 트렌딩 암호화폐 데이터 조회 (Hot 카테고리)
   */
  const {
    data: hotCoins,
    isLoading: isHotLoading,
  } = useQuery({
    queryKey: ['hot-coins'],
    queryFn: () => getTrendingCoins('KRW', 10),
    refetchInterval: 60000, // 1분마다 자동 갱신
  });
  const popularEtfs = ['SPY', 'QQQ', 'SCHD', 'VTI', 'IWM', 'AGG'];
  const {
    data: etfPrices,
    isLoading: isEtfLoading,
  } = useQuery({
    queryKey: ['etf-prices'],
    queryFn: () => getBatchPrices(popularEtfs),
    refetchInterval: 60000,
  });

  const etfNames: Record<string, string> = {
    'SPY': 'S&P 500',
    'QQQ': 'NASDAQ-100',
    'SCHD': 'SCHD',
    'VTI': 'Total Market',
    'IWM': 'Russell 2000',
    'AGG': 'US Bond',
  };

  /**
   * TODO: 카테고리별 API 엔드포인트 구현 필요
   * - New: 최근 상장 코인
   * - Top Gainer: 24시간 상승률 상위
   * - Top Volume: 거래량 상위
   *
   * 현재는 Hot 데이터를 재사용
   */
  const displayCoins = hotCoins || [];

  return (
    <div className="min-h-screen bg-background-primary">
      {/* 히어로 섹션 */}
      <div className="max-w-6xl mx-auto px-8 py-6">
        <section className="bg-gradient-brand rounded-12 py-8 px-8 text-center">
          <h1 className="text-3xl font-bold text-background-primary mb-2">
            포트폴리오 관리의 새로운 기준
          </h1>
          <p className="text-background-primary mb-4 opacity-90">
            데이터 기반의 투자 전략으로 더 나은 수익을 만드세요
          </p>
          {!isAuthenticated && (
            <Button
              variant="secondary"
              size="md"
              onClick={() => router.push('/login')}
            >
              무료로 시작하기
            </Button>
          )}
        </section>
      </div>

      <div className="max-w-6xl mx-auto px-8 pb-6 space-y-6">
        {/* 시장 현황 - Binance 스타일 */}
        <Section
          title="시장 현황"
          subtitle="실시간 암호화폐 시장 (Upbit)"
          action={
            <Button
              variant="ghost"
              size="sm"
              onClick={() => router.push('/market')}
            >
              전체 보기 →
            </Button>
          }
          noPadding
          background="none"
        >
          {/* 카테고리 탭 */}
          <div className="mb-4">
            <CategoryTabs
              activeCategory={activeCategory}
              onCategoryChange={setActiveCategory}
            />
          </div>

          {/* 로딩 상태 */}
          {isHotLoading && (
            <div className="text-center py-8 text-text-tertiary">
              데이터를 불러오는 중...
            </div>
          )}

          {/* 코인 그리드 */}
          {!isHotLoading && displayCoins.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
              {displayCoins.map((coin: TrendingCoinDto) => (
                <MarketCoinCard
                  key={coin.marketCode}
                  coin={{
                    symbol: coin.symbol,
                    name: coin.koreanName || coin.name,  // ✅ 수정
                    price: coin.price,                   // ✅ 수정
                    changePercent: coin.changePercent,   // ✅ 수정
                    volume24h: coin.volume,              // ✅ 수정
                  }}
                  onClick={() => console.log('Coin clicked:', coin.symbol)}
                />
              ))}
            </div>
          )}

          {/* 빈 상태 */}
          {!isHotLoading && displayCoins.length === 0 && (
            <div className="text-center py-8 text-text-tertiary">
              데이터가 없습니다.
            </div>
          )}
        </Section>

        {/* 주요 ETF & 인덱스 */}
        <Section
          title="주요 ETF & 인덱스"
          subtitle="미국 시장 주요 지수 및 ETF"
          action={
            <Button
              variant="ghost"
              size="sm"
              onClick={() => router.push('/market')}
            >
              전체 보기 →
            </Button>
          }
          noPadding
          background="none"
        >
          {/* 로딩 상태 */}
          {isEtfLoading && (
            <div className="text-center py-8 text-text-tertiary">
              데이터를 불러오는 중...
            </div>
          )}

          {/* ETF 그리드 */}
          {!isEtfLoading && etfPrices && etfPrices.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {etfPrices.map((etf: StockPrice) => {
                // Safe calculation - previousClose가 없거나 0일 경우 처리
                const previousClose = etf.previousClose || etf.price;
                const changeAmount = etf.price - previousClose;
                const changePercent = previousClose !== 0
                  ? (changeAmount / previousClose) * 100
                  : 0;
                const isPositive = changePercent >= 0;

                return (
                  <div
                    key={etf.symbol}
                    className="bg-background-secondary hover:bg-background-tertiary rounded-8 p-4 cursor-pointer transition-colors border border-background-tertiary"
                    onClick={() => console.log('ETF clicked:', etf.symbol)}
                  >
                    {/* 심볼 & 이름 */}
                    <div className="mb-3">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="text-text-primary font-medium text-sm">
                          {etf.symbol}
                        </span>
                      </div>
                      <span className="text-text-tertiary text-xs">
                        {etfNames[etf.symbol] || etf.symbol}
                      </span>
                    </div>

                    {/* 가격 */}
                    <div className="mb-2">
                      <div className="text-text-primary font-bold text-lg">
                        ${etf.price?.toLocaleString('en-US', {
                          minimumFractionDigits: 2,
                          maximumFractionDigits: 2,
                        }) || '0.00'}
                      </div>
                    </div>

                    {/* 변동률 */}
                    <div className="flex items-center justify-between">
                      <PercentageChange value={changePercent} size="sm" />
                      {previousClose !== etf.price && (
                        <span
                          className={`text-xs font-medium ${
                            isPositive ? 'text-success' : 'text-error'
                          }`}
                        >
                          {isPositive ? '+' : ''}
                          {changeAmount.toFixed(2)}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* 빈 상태 */}
          {!isEtfLoading && (!etfPrices || etfPrices.length === 0) && (
            <div className="text-center py-8 text-text-tertiary">
              데이터가 없습니다.
            </div>
          )}
        </Section>

        {/* 추천 포트폴리오 캐러셀 */}
        <Section
          title="추천 포트폴리오"
          subtitle="전문가가 선별한 투자 전략"
          action={
            <Button
              variant="ghost"
              size="sm"
              onClick={() => router.push('/portfolios')}
            >
              전체 보기 →
            </Button>
          }
          noPadding
          background="none"
        >
          <Carousel
            itemsPerView={3}
            slidesToScroll={1}
            autoplay
            interval={5000}
          >
            {mockRecommendedPortfolios.map((portfolio) => (
              <RecommendedPortfolioCard
                key={portfolio.id}
                portfolio={portfolio}
                onClick={() => alert(`${portfolio.name} 상세 보기`)}
              />
            ))}
          </Carousel>
        </Section>

        {/* 통계 섹션 */}
        <Section
          title="플랫폼 현황"
          subtitle="Sentinel과 함께하는 투자자들"
          noPadding
          background="none"
        >
          <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
            <StatCard
              label="총 사용자"
              value="10,234"
              change={15.3}
              changePeriod="이번 달"
              isMock
            />
            <StatCard
              label="관리 자산"
              value="₩1.2조"
              change={8.7}
              changePeriod="전월 대비"
              isMock
            />
            <StatCard
              label="평균 수익률"
              value="12.5%"
              change={2.1}
              changePeriod="연간"
              isMock
            />
            <StatCard
              label="포트폴리오 수"
              value="4,567"
              change={25.4}
              changePeriod="이번 달"
              isMock
            />
          </div>
        </Section>

        {/* 기능 소개 */}
        <Section
          title="주요 기능"
          subtitle="Sentinel이 제공하는 강력한 도구"
          noPadding
          background="none"
        >
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="p-5 bg-background-secondary rounded-12 border border-border-primary">
              <div className="text-3xl mb-3">📊</div>
              <h3 className="text-text-primary text-lg font-semibold mb-2">
                포트폴리오 관리
              </h3>
              <p className="text-text-tertiary text-sm">
                여러 포트폴리오를 한곳에서 관리하고 실시간으로 수익률을 확인하세요
              </p>
            </div>
            <div className="p-5 bg-background-secondary rounded-12 border border-border-primary">
              <div className="text-3xl mb-3">🧪</div>
              <h3 className="text-text-primary text-lg font-semibold mb-2">
                백테스팅 실험실
              </h3>
              <p className="text-text-tertiary text-sm">
                과거 데이터로 전략을 테스트하고 최적의 투자 방법을 찾아보세요
              </p>
            </div>
            <div className="p-5 bg-background-secondary rounded-12 border border-border-primary">
              <div className="text-3xl mb-3">🎯</div>
              <h3 className="text-text-primary text-lg font-semibold mb-2">
                리밸런싱 추천
              </h3>
              <p className="text-text-tertiary text-sm">
                AI 기반의 리밸런싱 추천으로 포트폴리오를 최적화하세요
              </p>
            </div>
          </div>
        </Section>

        {/* CTA 섹션 */}
        {!isAuthenticated && (
          <Section noPadding background="none">
            <div className="text-center py-10 px-8 bg-background-secondary rounded-12">
              <h2 className="text-xl font-bold text-text-primary mb-2">
                지금 바로 시작하세요
              </h2>
              <p className="text-text-tertiary text-sm mb-5">
                무료로 계정을 만들고 Sentinel의 모든 기능을 사용해보세요
              </p>
              <Button
                variant="primary"
                size="md"
                onClick={() => router.push('/login')}
              >
                무료 가입하기
              </Button>
            </div>
          </Section>
        )}
      </div>
    </div>
  );
}
