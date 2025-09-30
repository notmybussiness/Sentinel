import React from 'react'
import { cn } from '@/lib/utils'

interface CardProps {
  children: React.ReactNode
  className?: string
  padding?: 'none' | 'sm' | 'md' | 'lg'
  shadow?: 'none' | 'tiny' | 'low' | 'medium' | 'high'
  hover?: boolean
  onClick?: () => void
}

/**
 * Card 컴포넌트
 *
 * 콘텐츠를 담는 기본 카드 컨테이너
 *
 * @example
 * ```tsx
 * <Card padding="md" shadow="medium">
 *   <h3>제목</h3>
 *   <p>내용</p>
 * </Card>
 * ```
 */
export function Card({
  children,
  className,
  padding = 'md',
  shadow = 'low',
  hover = false,
  onClick,
}: CardProps) {
  const paddingStyles = {
    none: '',
    sm: 'p-3',
    md: 'p-4',
    lg: 'p-6',
  }

  const shadowStyles = {
    none: '',
    tiny: 'shadow-tiny',
    low: 'shadow-low',
    medium: 'shadow-medium',
    high: 'shadow-high',
  }

  return (
    <div
      className={cn(
        'bg-background-secondary/80 backdrop-blur-sm rounded-12 border border-border-primary/50',
        paddingStyles[padding],
        shadowStyles[shadow],
        hover && 'transition-all duration-regular hover:shadow-medium hover:scale-[1.02] hover:bg-background-secondary/90',
        onClick && 'cursor-pointer',
        className
      )}
      onClick={onClick}
    >
      {children}
    </div>
  )
}

/**
 * CardHeader 컴포넌트 - 카드 헤더 영역
 */
export function CardHeader({
  children,
  className,
}: {
  children: React.ReactNode
  className?: string
}) {
  return (
    <div className={cn('mb-4', className)}>
      {children}
    </div>
  )
}

/**
 * CardTitle 컴포넌트 - 카드 제목
 */
export function CardTitle({
  children,
  className,
}: {
  children: React.ReactNode
  className?: string
}) {
  return (
    <h3 className={cn('text-lg font-semibold text-text-primary', className)}>
      {children}
    </h3>
  )
}

/**
 * CardContent 컴포넌트 - 카드 본문 영역
 */
export function CardContent({
  children,
  className,
}: {
  children: React.ReactNode
  className?: string
}) {
  return (
    <div className={cn('text-regular text-text-secondary', className)}>
      {children}
    </div>
  )
}