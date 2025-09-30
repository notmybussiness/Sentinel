"use client";

import React from 'react';
import { useRouter } from 'next/navigation';
import { Section } from '@/components/ui/Section';
import { Carousel } from '@/components/ui/Carousel';
import { StatCard } from '@/components/ui/StatCard';
import { IndexCard } from '@/components/market/IndexCard';
import { RecommendedPortfolioCard } from '@/components/portfolio/RecommendedPortfolioCard';
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/contexts/AuthContext';
import { mockRecommendedPortfolios, mockMarketIndices } from '@/lib/mockData';

export default function HomePage() {
  const router = useRouter();
  const { isAuthenticated } = useAuth();

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
        {/* 시장 인덱스 */}
        <Section
          title="시장 현황"
          subtitle="주요 지수 실시간 업데이트"
          noPadding
          background="none"
        >
          <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
            {mockMarketIndices.map((index) => (
              <IndexCard key={index.symbol} index={index} />
            ))}
          </div>
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
