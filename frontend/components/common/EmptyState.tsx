import React from 'react'
import { Button } from '../ui/Button'

interface EmptyStateProps {
  icon?: React.ReactNode
  title: string
  description?: string
  actionLabel?: string
  onAction?: () => void
}

/**
 * EmptyState 컴포넌트
 *
 * 데이터가 없을 때 표시하는 빈 상태 UI
 *
 * @example
 * ```tsx
 * <EmptyState
 *   title="포트폴리오가 없습니다"
 *   description="첫 번째 포트폴리오를 만들어보세요"
 *   actionLabel="포트폴리오 생성"
 *   onAction={() => setShowCreateModal(true)}
 * />
 * ```
 */
export function EmptyState({
  icon,
  title,
  description,
  actionLabel,
  onAction,
}: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
      {/* 아이콘 */}
      {icon && (
        <div className="mb-4 text-text-quaternary">
          {icon}
        </div>
      )}

      {/* 제목 */}
      <h3 className="text-lg font-semibold text-text-primary mb-2">
        {title}
      </h3>

      {/* 설명 */}
      {description && (
        <p className="text-regular text-text-tertiary max-w-md mb-6">
          {description}
        </p>
      )}

      {/* 액션 버튼 */}
      {actionLabel && onAction && (
        <Button variant="primary" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  )
}