import React from 'react'
import { formatCurrency, formatNumber, formatPercent, getChangeColorClass } from '@/lib/utils'
import type { PortfolioHolding } from '@/types'

interface HoldingItemProps {
  holding: PortfolioHolding
  onEdit?: () => void
  onDelete?: () => void
}

/**
 * HoldingItem 컴포넌트
 *
 * 포트폴리오 보유 종목 목록의 개별 아이템
 *
 * @example
 * ```tsx
 * <HoldingItem
 *   holding={holding}
 *   onEdit={() => handleEdit(holding.id)}
 *   onDelete={() => handleDelete(holding.id)}
 * />
 * ```
 */
export function HoldingItem({
  holding,
  onEdit,
  onDelete,
}: HoldingItemProps) {
  const changeColor = getChangeColorClass(holding.gainLossPercent)

  return (
    <div className="p-4 border border-border-primary rounded-8 hover:bg-background-secondary transition-colors">
      <div className="flex items-start justify-between">
        {/* 종목 정보 */}
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <h4 className="text-lg font-semibold text-text-primary">
              {holding.symbol}
            </h4>
            <span className="text-small text-text-tertiary">
              {formatNumber(holding.quantity, 0)}주
            </span>
          </div>

          <div className="grid grid-cols-4 gap-4 text-small">
            {/* 평균 매수가 */}
            <div>
              <p className="text-text-tertiary mb-1">평균 매수가</p>
              <p className="font-medium text-text-primary">
                {formatCurrency(holding.averageCost)}
              </p>
            </div>

            {/* 현재가 */}
            <div>
              <p className="text-text-tertiary mb-1">현재가</p>
              <p className="font-medium text-text-primary">
                {formatCurrency(holding.currentPrice)}
              </p>
            </div>

            {/* 평가 금액 */}
            <div>
              <p className="text-text-tertiary mb-1">평가 금액</p>
              <p className="font-medium text-text-primary">
                {formatCurrency(holding.marketValue)}
              </p>
            </div>

            {/* 손익 */}
            <div>
              <p className="text-text-tertiary mb-1">손익</p>
              <p className={`font-semibold ${changeColor}`}>
                {formatCurrency(holding.gainLoss)}
                <span className="text-mini ml-1">
                  ({formatPercent(holding.gainLossPercent)})
                </span>
              </p>
            </div>
          </div>
        </div>

        {/* 액션 버튼 */}
        {(onEdit || onDelete) && (
          <div className="flex items-center gap-2 ml-4">
            {onEdit && (
              <button
                onClick={onEdit}
                className="p-2 text-text-tertiary hover:text-text-primary hover:bg-background-tertiary rounded-6 transition-colors"
                title="수정"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
              </button>
            )}
            {onDelete && (
              <button
                onClick={onDelete}
                className="p-2 text-text-tertiary hover:text-accent-red hover:bg-accent-red/10 rounded-6 transition-colors"
                title="삭제"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  )
}