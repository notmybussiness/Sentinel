import React from 'react'
import { cn } from '@/lib/utils'

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helperText?: string
  leftIcon?: React.ReactNode
  rightIcon?: React.ReactNode
}

/**
 * Input 컴포넌트
 *
 * 라벨, 에러 메시지, 아이콘을 지원하는 입력 필드
 *
 * @example
 * ```tsx
 * <Input
 *   label="이메일"
 *   type="email"
 *   placeholder="email@example.com"
 *   error="유효한 이메일을 입력하세요"
 * />
 * ```
 */
export function Input({
  label,
  error,
  helperText,
  leftIcon,
  rightIcon,
  className,
  ...props
}: InputProps) {
  return (
    <div className="w-full">
      {/* 라벨 */}
      {label && (
        <label className="block mb-1.5 text-small font-medium text-text-primary">
          {label}
        </label>
      )}

      {/* 입력 필드 컨테이너 */}
      <div className="relative">
        {/* 왼쪽 아이콘 */}
        {leftIcon && (
          <div className="absolute left-3 top-1/2 -translate-y-1/2 text-text-tertiary">
            {leftIcon}
          </div>
        )}

        {/* 입력 필드 */}
        <input
          className={cn(
            'w-full px-3 py-2 text-regular',
            'bg-background-primary text-text-primary',
            'border border-border-primary rounded-8',
            'placeholder:text-text-quaternary',
            'transition-all duration-quick',
            'focus:outline-none focus:ring-2 focus:ring-brand-primary focus:border-transparent',
            error && 'border-accent-red focus:ring-accent-red',
            leftIcon && 'pl-10',
            rightIcon && 'pr-10',
            props.disabled && 'opacity-50 cursor-not-allowed bg-background-secondary',
            className
          )}
          {...props}
        />

        {/* 오른쪽 아이콘 */}
        {rightIcon && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2 text-text-tertiary">
            {rightIcon}
          </div>
        )}
      </div>

      {/* 에러 메시지 또는 도움말 */}
      {error && (
        <p className="mt-1.5 text-small text-accent-red">{error}</p>
      )}
      {helperText && !error && (
        <p className="mt-1.5 text-small text-text-tertiary">{helperText}</p>
      )}
    </div>
  )
}