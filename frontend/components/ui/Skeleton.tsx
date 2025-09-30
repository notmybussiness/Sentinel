import React from 'react'
import { cn } from '@/lib/utils'

interface SkeletonProps {
  className?: string
  variant?: 'text' | 'circular' | 'rectangular'
  width?: string
  height?: string
  animation?: boolean
}

/**
 * Skeleton 컴포넌트
 *
 * 콘텐츠 로딩 중 표시되는 스켈레톤 UI
 *
 * @example
 * ```tsx
 * <Skeleton variant="text" />
 * <Skeleton variant="circular" className="w-12 h-12" />
 * <Skeleton variant="rectangular" className="w-full h-48" />
 * ```
 */
export function Skeleton({
  className,
  variant = 'rectangular',
  width,
  height,
  animation = true,
}: SkeletonProps) {
  const variantStyles = {
    text: 'h-4 rounded-4',
    circular: 'rounded-circle',
    rectangular: 'rounded-8',
  }

  return (
    <div
      className={cn(
        'bg-background-quaternary',
        animation && 'animate-pulse',
        variantStyles[variant],
        className
      )}
      style={{ width, height }}
    />
  )
}

/**
 * SkeletonCard 컴포넌트
 *
 * 카드 형태의 스켈레톤 (재사용 가능한 레이아웃)
 */
export function SkeletonCard() {
  return (
    <div className="p-4 border border-border-primary rounded-12">
      <div className="flex items-center gap-3 mb-4">
        <Skeleton variant="circular" className="w-12 h-12" />
        <div className="flex-1 space-y-2">
          <Skeleton variant="text" className="w-3/4" />
          <Skeleton variant="text" className="w-1/2" />
        </div>
      </div>
      <Skeleton variant="rectangular" className="w-full h-32" />
      <div className="mt-4 space-y-2">
        <Skeleton variant="text" className="w-full" />
        <Skeleton variant="text" className="w-5/6" />
      </div>
    </div>
  )
}