import React from 'react'
import { Card } from '../ui/Card'
import { formatNumber, formatPercent, getChangeColorClass } from '@/lib/utils'
import type { MarketIndex } from '@/types'

interface IndexCardProps {
  index: MarketIndex
}

/**
 * IndexCard 컴포넌트
 *
 * 시장 지수를 표시하는 카드 (S&P 500, NASDAQ 등)
 *
 * @example
 * ```tsx
 * <IndexCard index={{ name: "S&P 500", symbol: "SPX", value: 4500, change: 25, changePercent: 0.56 }} />
 * ```
 */
export function IndexCard({ index }: IndexCardProps) {
  const changeColor = getChangeColorClass(index.changePercent)

  return (
    <Card padding="sm" shadow="low" hover>
      <div className="flex items-start justify-between">
        {/* 지수 정보 */}
        <div>
          <h3 className="text-mini text-text-tertiary mb-0.5">{index.name}</h3>
          <p className="text-xl font-semibold text-text-primary mb-0.5">
            {formatNumber(index.value, 2)}
          </p>
          <div className={`flex items-center gap-1 text-mini font-medium ${changeColor}`}>
            <span>
              {index.change > 0 ? '▲' : index.change < 0 ? '▼' : '―'}
            </span>
            <span>{formatNumber(Math.abs(index.change), 2)}</span>
            <span>({formatPercent(index.changePercent)})</span>
          </div>
        </div>

        {/* 심볼 */}
        <div className="px-1.5 py-0.5 bg-background-tertiary rounded-4">
          <span className="text-mini font-medium text-text-tertiary">
            {index.symbol}
          </span>
        </div>
      </div>
    </Card>
  )
}