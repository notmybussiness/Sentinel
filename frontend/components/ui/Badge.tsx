import React from 'react'
import { cn } from '@/lib/utils'

type BadgeVariant = 'default' | 'success' | 'warning' | 'danger' | 'info' | 'mock'

interface BadgeProps {
  children: React.ReactNode
  variant?: BadgeVariant
  className?: string
}

/**
 * Badge 컴포넌트
 *
 * 상태, 카테고리, 레이블을 표시하는 작은 뱃지
 *
 * @example
 * ```tsx
 * <Badge variant="success">활성</Badge>
 * <Badge variant="mock">Mock 데이터</Badge>
 * ```
 */
export function Badge({
  children,
  variant = 'default',
  className,
}: BadgeProps) {
  const variantStyles = {
    default: 'bg-background-quaternary text-text-primary',
    success: 'bg-accent-green/10 text-accent-green',
    warning: 'bg-accent-yellow/10 text-accent-orange',
    danger: 'bg-accent-red/10 text-accent-red',
    info: 'bg-accent-blue/10 text-accent-blue',
    mock: 'bg-accent-indigo/10 text-accent-indigo border border-accent-indigo/20',
  }

  return (
    <span
      className={cn(
        'inline-flex items-center px-2 py-0.5',
        'text-mini font-medium rounded-6',
        variantStyles[variant],
        className
      )}
    >
      {children}
    </span>
  )
}