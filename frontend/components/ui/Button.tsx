import React from 'react'
import { cn } from '@/lib/utils'

/**
 * 버튼 변형 타입
 * - primary: 주요 액션 (브랜드 색상)
 * - secondary: 보조 액션 (회색 배경)
 * - ghost: 투명 배경 (텍스트만)
 * - danger: 위험한 액션 (빨간색)
 */
type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'

/**
 * 버튼 크기 타입
 */
type ButtonSize = 'sm' | 'md' | 'lg'

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  loading?: boolean
  fullWidth?: boolean
  children: React.ReactNode
}

/**
 * Button 컴포넌트
 *
 * @example
 * ```tsx
 * <Button variant="primary" onClick={handleClick}>
 *   클릭하세요
 * </Button>
 * ```
 */
export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  fullWidth = false,
  disabled,
  className,
  children,
  ...props
}: ButtonProps) {
  // 기본 스타일
  const baseStyles = cn(
    'inline-flex items-center justify-center',
    'font-medium transition-all duration-quick',
    'focus:outline-none focus:ring-2 focus:ring-brand-primary focus:ring-offset-2',
    'disabled:opacity-50 disabled:cursor-not-allowed',
    'active:scale-95'
  )

  // 변형별 스타일
  const variantStyles = {
    primary: 'bg-brand-primary text-brand-text hover:opacity-90',
    secondary: 'bg-background-quaternary text-text-primary hover:bg-background-quinary',
    ghost: 'bg-transparent text-text-primary hover:bg-background-tertiary',
    danger: 'bg-accent-red text-white hover:opacity-90',
  }

  // 크기별 스타일
  const sizeStyles = {
    sm: 'px-3 py-1.5 text-small rounded-6',
    md: 'px-4 py-2 text-regular rounded-8',
    lg: 'px-6 py-3 text-large rounded-12',
  }

  return (
    <button
      className={cn(
        baseStyles,
        variantStyles[variant],
        sizeStyles[size],
        fullWidth && 'w-full',
        className
      )}
      disabled={disabled || loading}
      {...props}
    >
      {loading && (
        <svg
          className="animate-spin -ml-1 mr-2 h-4 w-4"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            className="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            strokeWidth="4"
          />
          <path
            className="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          />
        </svg>
      )}
      {children}
    </button>
  )
}