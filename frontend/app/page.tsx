"use client";

import React from 'react';
import { useRouter } from 'next/navigation';
import { Section } from '@/components/ui/Section';
import { Carousel } from '@/components/ui/Carousel';
import { StatCard } from '@/components/ui/StatCard';
import { TrendingCryptoCard } from '@/components/crypto/TrendingCryptoCard';
import { RecommendedPortfolioCard } from '@/components/portfolio/RecommendedPortfolioCard';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/contexts/AuthContext';
import { mockRecommendedPortfolios, mockMarketIndices } from '@/lib/mockData';
// STOCK API 주석처리 (API 한도 문제로 Crypto 중심 전환) - Mock 데이터 사용
// import { getMarketIndices } from '@/lib/api/market';
import { getTrendingCoins } from '@/lib/api/crypto';
import { useQuery } from '@tanstack/react-query';

export default function HomePage() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();

  /**
   * 시장 지수 데이터 - Mock 데이터 사용 (API 한도 문제로 비활성화)
   *
   * S&P 500, NASDAQ, Dow Jones, Bitcoin 지수 표시
   */
  const indices = mockMarketIndices;
  const isLoading = false;
  const error = null;

  // Bitcoin 실시간 API 주석처리 - Mock 데이터로 대체
  // const {
  //   data: bitcoinPrice,
  //   isLoading: isBitcoinLoading,
  //   error: bitcoinError,
  // } = useQuery({
  //   queryKey: ['bitcoin-price'],
  //   queryFn: () => getCryptoPrice('BTC', 'KRW'),
  //   refetchInterval: 60000, // 1분마다 자동 갱신
  // });

  /**
   * 트렌딩 암호화폐 데이터 조회
   */
  const {
    data: trendingCoins,
    isLoading: isCryptoLoading,
    error: cryptoError,
  } = useQuery({
    queryKey: ['trending-coins'],
    queryFn: () => getTrendingCoins('KRW', 5),
    refetchInterval: 60000, // 1분마다 자동 갱신
  });

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
        {/* 시장 지수 - Mock 데이터 */}
        <Section
          title="시장 현황"
          subtitle="주요 지수 실시간 업데이트"
          noPadding
          background="none"
        >
          {/* 로딩 상태 */}
          {isLoading && (
            <div className="text-center py-8 text-text-tertiary">
              시장 데이터를 불러오는 중...
            </div>
          )}

          {/* 에러 상태 */}
          {error && (
            <div className="text-center py-8 text-error">
              시장 데이터를 불러올 수 없습니다.
            </div>
          )}

          {/* 데이터 표시 - 4개 지수 (Mock) */}
          {!isLoading && !error && indices && (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
              {indices.map((index) => (
                <div
                  key={index.symbol}
                  className="glass-card p-4 rounded-12 hover:bg-background-secondary transition-all cursor-pointer relative"
                >
                  {/* Mock 태그 */}
                  <span className="absolute top-2 right-2 px-2 py-0.5 bg-yellow-500/20 text-yellow-400 text-micro rounded-4">
                    MOCK
                  </span>
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <h3 className="text-text-primary font-semibold text-sm">
                        {index.name}
                      </h3>
                      <p className="text-text-tertiary text-xs">{index.symbol}</p>
                    </div>
                    <div className="text-xl">
                      {index.symbol === 'BTC' ? '₿' : '📊'}
                    </div>
                  </div>
                  <div className="text-text-primary text-xl font-bold mb-1">
                    {index.symbol === 'BTC'
                      ? `₩${index.value.toLocaleString()}`
                      : index.value.toLocaleString()}
                  </div>
                  <div className="flex items-center gap-1">
                    <span
                      className={`text-sm font-semibold ${
                        index.changePercent >= 0
                          ? 'text-success'
                          : 'text-error'
                      }`}
                    >
                      {index.changePercent >= 0 ? '+' : ''}
                      {index.changePercent.toFixed(2)}%
                    </span>
                    <span className="text-text-tertiary text-xs">
                      ({index.changePercent >= 0 ? '+' : ''}
                      {index.symbol === 'BTC'
                        ? `₩${index.change.toLocaleString()}`
                        : index.change.toLocaleString()})
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Section>

        {/* 트렌딩 암호화폐 */}
        <Section
          title="트렌딩 암호화폐"
          subtitle="실시간 거래대금 상위 코인 (Upbit)"
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
          {isCryptoLoading && (
            <div className="text-center py-8 text-text-tertiary">
              암호화폐 데이터를 불러오는 중...
            </div>
          )}

          {/* 에러 상태 */}
          {cryptoError && (
            <div className="text-center py-8 text-error">
              암호화폐 데이터를 불러올 수 없습니다.
            </div>
          )}

          {/* 데이터 표시 */}
          {!isCryptoLoading && !cryptoError && trendingCoins && (
            <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
              {trendingCoins.map((coin) => (
                <TrendingCryptoCard key={coin.marketCode} coin={coin} />
              ))}
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
