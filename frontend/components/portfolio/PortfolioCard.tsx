import React from 'react'
import { Card, CardHeader, CardTitle, CardContent } from '../ui/Card'
import { Badge } from '../ui/Badge'
import { formatCurrency, formatPercent, getChangeColorClass } from '@/lib/utils'
import type { Portfolio } from '@/types'

interface PortfolioCardProps {
  portfolio: Portfolio
  onClick?: () => void
  showBadge?: boolean
}

/**
 * PortfolioCard 컴포넌트
 *
 * 포트폴리오 목록에서 사용하는 카드 컴포넌트
 *
 * @example
 * ```tsx
 * <PortfolioCard
 *   portfolio={portfolio}
 *   onClick={() => router.push(`/portfolios/${portfolio.id}`)}
 * />
 * ```
 */
export function PortfolioCard({
  portfolio,
  onClick,
  showBadge = false,
}: PortfolioCardProps) {
  const changeColor = getChangeColorClass(portfolio.totalGainLossPercent)

  return (
    <Card padding="md" shadow="low" hover onClick={onClick}>
      <CardHeader>
        <div className="flex items-start justify-between">
          <CardTitle>{portfolio.name}</CardTitle>
          {showBadge && <Badge variant="success">활성</Badge>}
        </div>
        {portfolio.description && (
          <p className="text-small text-text-tertiary mt-1">
            {portfolio.description}
          </p>
        )}
      </CardHeader>

      <CardContent>
        {/* 총 자산 */}
        <div className="mb-4">
          <p className="text-small text-text-tertiary mb-1">총 자산</p>
          <p className="text-2xl font-semibold text-text-primary">
            {formatCurrency(portfolio.totalValue)}
          </p>
        </div>

        {/* 손익 정보 */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-small text-text-tertiary mb-1">손익 금액</p>
            <p className={`text-lg font-semibold ${changeColor}`}>
              {formatCurrency(portfolio.totalGainLoss)}
            </p>
          </div>
          <div>
            <p className="text-small text-text-tertiary mb-1">수익률</p>
            <p className={`text-lg font-semibold ${changeColor}`}>
              {formatPercent(portfolio.totalGainLossPercent)}
            </p>
          </div>
        </div>

        {/* 보유 종목 수 */}
        <div className="mt-4 pt-4 border-t border-border-primary">
          <p className="text-small text-text-tertiary">
            보유 종목: <span className="font-medium text-text-primary">{portfolio.holdings?.length || 0}개</span>
          </p>
        </div>
      </CardContent>
    </Card>
  )
}