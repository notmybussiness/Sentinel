import React from 'react'
import { Badge } from '../ui/Badge'

interface MockDataBadgeProps {
  show?: boolean
  source?: string
  className?: string
}

/**
 * MockDataBadge 컴포넌트
 *
 * Mock 데이터를 사용하는 컴포넌트에 표시하는 뱃지
 * 개발 중 실제 API와 Mock 데이터를 쉽게 구분하기 위함
 *
 * @example
 * ```tsx
 * <MockDataBadge show={true} source="frontend_reference/mock_portfolios.json" />
 * ```
 */
export function MockDataBadge({
  show = true,
  source,
  className,
}: MockDataBadgeProps) {
  if (!show) return null

  return (
    <Badge
      variant="mock"
      className={className}
      title={source ? `Mock 데이터 출처: ${source}` : 'Mock 데이터'}
    >
      <span className="flex items-center gap-1">
        <svg
          width="12"
          height="12"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="16" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12.01" y2="8" />
        </svg>
        Mock
      </span>
    </Badge>
  )
}