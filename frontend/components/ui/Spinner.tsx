import React from 'react'
import { cn } from '@/lib/utils'

interface SpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

/**
 * Spinner 컴포넌트
 *
 * 로딩 상태를 표시하는 애니메이션 스피너
 *
 * @example
 * ```tsx
 * <Spinner size="md" />
 * ```
 */
export function Spinner({ size = 'md', className }: SpinnerProps) {
  const sizeStyles = {
    sm: 'h-4 w-4 border-2',
    md: 'h-8 w-8 border-3',
    lg: 'h-12 w-12 border-4',
  }

  return (
    <div
      className={cn(
        'animate-spin rounded-full border-brand-primary border-t-transparent',
        sizeStyles[size],
        className
      )}
    />
  )
}

/**
 * FullPageSpinner 컴포넌트
 *
 * 전체 페이지를 덮는 로딩 스피너
 */
export function FullPageSpinner() {
  return (
    <div className="fixed inset-0 z-overlay flex items-center justify-center bg-background-primary/80 backdrop-blur-sm">
      <div className="text-center">
        <Spinner size="lg" />
        <p className="mt-4 text-regular text-text-secondary">로딩 중...</p>
      </div>
    </div>
  )
}