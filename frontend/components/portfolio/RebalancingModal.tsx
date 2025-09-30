import React from 'react'
import { Modal, ModalFooter } from '../ui/Modal'
import { Button } from '../ui/Button'
import { formatCurrency, formatPercent } from '@/lib/utils'
import type { RebalancingRecommendation } from '@/types'

interface RebalancingModalProps {
  isOpen: boolean
  onClose: () => void
  recommendations: RebalancingRecommendation[]
  onExecute?: () => void
  loading?: boolean
}

/**
 * RebalancingModal 컴포넌트
 *
 * 리밸런싱 권장사항을 팝업으로 표시
 *
 * @example
 * ```tsx
 * <RebalancingModal
 *   isOpen={showRebalancing}
 *   onClose={() => setShowRebalancing(false)}
 *   recommendations={recommendations}
 *   onExecute={handleExecuteRebalancing}
 * />
 * ```
 */
export function RebalancingModal({
  isOpen,
  onClose,
  recommendations,
  onExecute,
  loading = false,
}: RebalancingModalProps) {
  // 매수/매도별 그룹화
  const buyRecommendations = recommendations.filter(r => r.action === 'BUY')
  const sellRecommendations = recommendations.filter(r => r.action === 'SELL')
  const holdRecommendations = recommendations.filter(r => r.action === 'HOLD')

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="리밸런싱 권장사항" size="lg">
      <div className="space-y-6">
        {/* 매도 권장 */}
        {sellRecommendations.length > 0 && (
          <div>
            <h4 className="text-lg font-semibold text-accent-red mb-3">
              🔻 매도 (SELL)
            </h4>
            <div className="space-y-2">
              {sellRecommendations.map((rec, idx) => (
                <RebalancingItem key={idx} recommendation={rec} />
              ))}
            </div>
          </div>
        )}

        {/* 매수 권장 */}
        {buyRecommendations.length > 0 && (
          <div>
            <h4 className="text-lg font-semibold text-accent-green mb-3">
              🔺 매수 (BUY)
            </h4>
            <div className="space-y-2">
              {buyRecommendations.map((rec, idx) => (
                <RebalancingItem key={idx} recommendation={rec} />
              ))}
            </div>
          </div>
        )}

        {/* 유지 */}
        {holdRecommendations.length > 0 && (
          <div>
            <h4 className="text-lg font-semibold text-text-tertiary mb-3">
              ― 유지 (HOLD)
            </h4>
            <div className="space-y-2">
              {holdRecommendations.map((rec, idx) => (
                <RebalancingItem key={idx} recommendation={rec} />
              ))}
            </div>
          </div>
        )}

        {/* 권장사항이 없는 경우 */}
        {recommendations.length === 0 && (
          <div className="text-center py-8 text-text-tertiary">
            <p>현재 리밸런싱이 필요하지 않습니다.</p>
            <p className="text-small mt-2">포트폴리오가 목표 비율에 가깝습니다.</p>
          </div>
        )}
      </div>

      <ModalFooter>
        <Button variant="secondary" onClick={onClose}>
          취소
        </Button>
        {recommendations.length > 0 && onExecute && (
          <Button
            variant="primary"
            onClick={onExecute}
            loading={loading}
          >
            리밸런싱 실행
          </Button>
        )}
      </ModalFooter>
    </Modal>
  )
}

/**
 * RebalancingItem - 개별 리밸런싱 권장 아이템
 */
function RebalancingItem({ recommendation }: { recommendation: RebalancingRecommendation }) {
  const actionColor = {
    BUY: 'text-accent-green',
    SELL: 'text-accent-red',
    HOLD: 'text-text-tertiary',
  }[recommendation.action]

  const actionLabel = {
    BUY: '매수',
    SELL: '매도',
    HOLD: '유지',
  }[recommendation.action]

  return (
    <div className="p-3 bg-background-secondary rounded-8">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-3">
          <span className="font-semibold text-text-primary">
            {recommendation.symbol}
          </span>
          <span className={`text-small font-medium ${actionColor}`}>
            {actionLabel}
          </span>
        </div>
        {recommendation.action !== 'HOLD' && (
          <span className={`font-semibold ${actionColor}`}>
            {formatCurrency(Math.abs(recommendation.estimatedAmount))}
          </span>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4 text-small text-text-tertiary">
        <div>
          <span>현재 비중: </span>
          <span className="font-medium text-text-primary">
            {formatPercent(recommendation.currentWeight)}
          </span>
        </div>
        <div>
          <span>목표 비중: </span>
          <span className="font-medium text-text-primary">
            {formatPercent(recommendation.targetWeight)}
          </span>
        </div>
        {recommendation.action !== 'HOLD' && (
          <>
            <div>
              <span>현재 수량: </span>
              <span className="font-medium text-text-primary">
                {recommendation.currentShares}주
              </span>
            </div>
            <div>
              <span>조정 수량: </span>
              <span className={`font-medium ${actionColor}`}>
                {recommendation.action === 'BUY' ? '+' : '-'}
                {Math.abs(recommendation.quantity)}주
              </span>
            </div>
          </>
        )}
      </div>
    </div>
  )
}