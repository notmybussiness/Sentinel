'use client'

import React, { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { Badge } from '@/components/ui/Badge'
import { Modal, ModalFooter } from '@/components/ui/Modal'
import { Spinner, FullPageSpinner } from '@/components/ui/Spinner'
import { Skeleton, SkeletonCard } from '@/components/ui/Skeleton'
import { MockDataBadge } from '@/components/common/MockDataBadge'
import { EmptyState } from '@/components/common/EmptyState'
import { PortfolioCard } from '@/components/portfolio/PortfolioCard'
import { HoldingItem } from '@/components/portfolio/HoldingItem'
import { RebalancingModal } from '@/components/portfolio/RebalancingModal'
import { IndexCard } from '@/components/market/IndexCard'
import { AssetFilter } from '@/components/market/AssetFilter'
import { StockSearchBar } from '@/components/market/StockSearchBar'
import type { Portfolio, PortfolioHolding, RebalancingRecommendation, MarketIndex, AssetClass } from '@/types'

/**
 * 컴포넌트 미리보기 페이지
 *
 * 모든 재사용 가능한 컴포넌트를 한 페이지에서 확인
 */
export default function ComponentsPreviewPage() {
  const [showModal, setShowModal] = useState(false)
  const [showRebalancing, setShowRebalancing] = useState(false)
  const [showSpinner, setShowSpinner] = useState(false)
  const [selectedAsset, setSelectedAsset] = useState<AssetClass | 'ALL'>('ALL')

  // Mock 데이터
  const mockPortfolio: Portfolio = {
    id: 1,
    userId: 1,
    name: '장기 투자 포트폴리오',
    description: 'S&P 500 중심의 장기 투자 전략',
    totalValue: 50000000,
    totalCost: 45000000,
    totalGainLoss: 5000000,
    totalGainLossPercent: 11.11,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2025-09-30T00:00:00Z',
    holdings: [],
  }

  const mockHolding: PortfolioHolding = {
    id: 1,
    portfolioId: 1,
    symbol: 'AAPL',
    quantity: 100,
    averageCost: 150000,
    currentPrice: 180000,
    marketValue: 18000000,
    totalCost: 15000000,
    gainLoss: 3000000,
    gainLossPercent: 20.0,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2025-09-30T00:00:00Z',
  }

  const mockRebalancing: RebalancingRecommendation[] = [
    {
      symbol: 'AAPL',
      currentWeight: 40,
      targetWeight: 30,
      currentShares: 100,
      targetShares: 75,
      action: 'SELL',
      quantity: 25,
      estimatedAmount: 4500000,
    },
    {
      symbol: 'GOOGL',
      currentWeight: 20,
      targetWeight: 30,
      currentShares: 20,
      targetShares: 30,
      action: 'BUY',
      quantity: 10,
      estimatedAmount: 3000000,
    },
  ]

  const mockIndex: MarketIndex = {
    name: 'S&P 500',
    symbol: 'SPX',
    value: 4567.89,
    change: 45.23,
    changePercent: 1.0,
    timestamp: '2025-09-30T15:00:00Z',
  }

  return (
    <div className="min-h-screen bg-background-secondary p-8">
      <div className="max-w-page mx-auto space-y-12">
        {/* 헤더 */}
        <div className="text-center">
          <h1 className="text-4xl font-bold text-text-primary mb-2">
            Sentinel 컴포넌트 라이브러리
          </h1>
          <p className="text-regular text-text-tertiary">
            Linear Design System 기반의 재사용 가능한 컴포넌트들
          </p>
          <MockDataBadge show={true} source="이 페이지는 데모용입니다" className="mt-4" />
        </div>

        {/* Buttons */}
        <Section title="Buttons">
          <div className="flex flex-wrap gap-3">
            <Button variant="primary">Primary</Button>
            <Button variant="secondary">Secondary</Button>
            <Button variant="ghost">Ghost</Button>
            <Button variant="danger">Danger</Button>
            <Button variant="primary" loading>Loading</Button>
            <Button variant="primary" disabled>Disabled</Button>
          </div>
          <div className="flex flex-wrap gap-3 mt-4">
            <Button variant="primary" size="sm">Small</Button>
            <Button variant="primary" size="md">Medium</Button>
            <Button variant="primary" size="lg">Large</Button>
          </div>
        </Section>

        {/* Cards */}
        <Section title="Cards">
          <div className="grid grid-cols-3 gap-4">
            <Card padding="md" shadow="low">
              <CardHeader>
                <CardTitle>기본 카드</CardTitle>
              </CardHeader>
              <CardContent>
                <p>카드 콘텐츠 영역입니다.</p>
              </CardContent>
            </Card>
            <Card padding="lg" shadow="medium" hover>
              <CardHeader>
                <CardTitle>호버 카드</CardTitle>
              </CardHeader>
              <CardContent>
                <p>마우스를 올려보세요!</p>
              </CardContent>
            </Card>
            <Card padding="md" shadow="high">
              <CardHeader>
                <CardTitle>그림자 강조</CardTitle>
              </CardHeader>
              <CardContent>
                <p>높은 그림자 효과</p>
              </CardContent>
            </Card>
          </div>
        </Section>

        {/* Inputs */}
        <Section title="Inputs">
          <div className="space-y-4 max-w-md">
            <Input label="이메일" type="email" placeholder="email@example.com" />
            <Input label="비밀번호" type="password" placeholder="비밀번호" />
            <Input
              label="에러 예시"
              type="text"
              error="유효한 값을 입력하세요"
              placeholder="잘못된 입력"
            />
            <Input
              label="도움말 예시"
              type="text"
              helperText="8자 이상 입력하세요"
              placeholder="비밀번호"
            />
          </div>
        </Section>

        {/* Badges */}
        <Section title="Badges">
          <div className="flex flex-wrap gap-3">
            <Badge variant="default">Default</Badge>
            <Badge variant="success">Success</Badge>
            <Badge variant="warning">Warning</Badge>
            <Badge variant="danger">Danger</Badge>
            <Badge variant="info">Info</Badge>
            <Badge variant="mock">Mock Data</Badge>
          </div>
        </Section>

        {/* Modal */}
        <Section title="Modal">
          <Button variant="primary" onClick={() => setShowModal(true)}>
            모달 열기
          </Button>
          <Modal isOpen={showModal} onClose={() => setShowModal(false)} title="예시 모달">
            <p className="mb-4">모달 콘텐츠 영역입니다.</p>
            <p>ESC 키 또는 외부 클릭으로 닫을 수 있습니다.</p>
            <ModalFooter>
              <Button variant="secondary" onClick={() => setShowModal(false)}>
                취소
              </Button>
              <Button variant="primary" onClick={() => setShowModal(false)}>
                확인
              </Button>
            </ModalFooter>
          </Modal>
        </Section>

        {/* Spinners */}
        <Section title="Spinners & Loading">
          <div className="flex items-center gap-6">
            <Spinner size="sm" />
            <Spinner size="md" />
            <Spinner size="lg" />
            <Button variant="primary" onClick={() => {
              setShowSpinner(true)
              setTimeout(() => setShowSpinner(false), 2000)
            }}>
              전체 화면 스피너 (2초)
            </Button>
          </div>
          {showSpinner && <FullPageSpinner />}
        </Section>

        {/* Skeletons */}
        <Section title="Skeletons">
          <div className="space-y-4">
            <Skeleton variant="text" className="w-3/4" />
            <Skeleton variant="text" className="w-1/2" />
            <div className="flex items-center gap-3">
              <Skeleton variant="circular" className="w-12 h-12" />
              <div className="flex-1 space-y-2">
                <Skeleton variant="text" className="w-full" />
                <Skeleton variant="text" className="w-2/3" />
              </div>
            </div>
            <Skeleton variant="rectangular" className="w-full h-48" />
            <SkeletonCard />
          </div>
        </Section>

        {/* Empty State */}
        <Section title="Empty State">
          <EmptyState
            icon={
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="12" />
                <line x1="12" y1="16" x2="12.01" y2="16" />
              </svg>
            }
            title="데이터가 없습니다"
            description="첫 번째 항목을 만들어보세요"
            actionLabel="생성하기"
            onAction={() => alert('생성 액션!')}
          />
        </Section>

        {/* Portfolio Card */}
        <Section title="Portfolio Card">
          <div className="grid grid-cols-2 gap-4">
            <PortfolioCard
              portfolio={mockPortfolio}
              onClick={() => alert('포트폴리오 클릭!')}
              showBadge={true}
            />
            <PortfolioCard
              portfolio={{ ...mockPortfolio, totalGainLoss: -2000000, totalGainLossPercent: -4.44 }}
              onClick={() => alert('포트폴리오 클릭!')}
            />
          </div>
        </Section>

        {/* Holding Item */}
        <Section title="Holding Item">
          <HoldingItem
            holding={mockHolding}
            onEdit={() => alert('수정')}
            onDelete={() => alert('삭제')}
          />
        </Section>

        {/* Rebalancing Modal */}
        <Section title="Rebalancing Modal">
          <Button variant="primary" onClick={() => setShowRebalancing(true)}>
            리밸런싱 모달 열기
          </Button>
          <RebalancingModal
            isOpen={showRebalancing}
            onClose={() => setShowRebalancing(false)}
            recommendations={mockRebalancing}
            onExecute={() => {
              alert('리밸런싱 실행!')
              setShowRebalancing(false)
            }}
          />
        </Section>

        {/* Index Card */}
        <Section title="Market Index Card">
          <div className="grid grid-cols-4 gap-4">
            <IndexCard index={mockIndex} />
            <IndexCard index={{ ...mockIndex, name: 'NASDAQ', symbol: 'IXIC', value: 14234.56, change: -23.45, changePercent: -0.16 }} />
            <IndexCard index={{ ...mockIndex, name: 'DOW', symbol: 'DJI', value: 34567.89, change: 0, changePercent: 0 }} />
            <IndexCard index={{ ...mockIndex, name: 'KOSPI', symbol: 'KOSPI', value: 2567.89, change: 12.34, changePercent: 0.48 }} />
          </div>
        </Section>

        {/* Asset Filter */}
        <Section title="Asset Filter">
          <AssetFilter selected={selectedAsset} onChange={setSelectedAsset} />
          <p className="mt-3 text-small text-text-tertiary">
            선택됨: <strong>{selectedAsset}</strong>
          </p>
        </Section>

        {/* Stock Search Bar */}
        <Section title="Stock Search Bar">
          <StockSearchBar
            onSearch={(query) => alert(`검색: ${query}`)}
            placeholder="종목명 또는 심볼 검색"
          />
        </Section>
      </div>
    </div>
  )
}

// 섹션 래퍼 컴포넌트
function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-background-primary p-6 rounded-16 border border-border-primary">
      <h2 className="text-2xl font-semibold text-text-primary mb-6">{title}</h2>
      {children}
    </div>
  )
}