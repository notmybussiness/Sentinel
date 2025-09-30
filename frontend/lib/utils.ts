import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

/**
 * Tailwind CSS 클래스 병합 유틸리티
 * 조건부 클래스와 Tailwind 클래스 충돌을 안전하게 처리
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * 숫자를 통화 형식으로 포맷 (한국 원화)
 * @example formatCurrency(1234567) => "₩1,234,567"
 */
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(amount)
}

/**
 * 숫자를 천 단위 구분자로 포맷
 * @example formatNumber(1234567.89) => "1,234,567.89"
 */
export function formatNumber(num: number, decimals: number = 2): string {
  return new Intl.NumberFormat('ko-KR', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(num)
}

/**
 * 퍼센트 포맷 (+/- 기호 포함)
 * @example formatPercent(12.34) => "+12.34%"
 * @example formatPercent(-5.67) => "-5.67%"
 */
export function formatPercent(percent: number, decimals: number = 2): string {
  const formatted = formatNumber(Math.abs(percent), decimals)
  return percent >= 0 ? `+${formatted}%` : `-${formatted}%`
}

/**
 * 날짜를 로컬 형식으로 포맷
 * @example formatDate(new Date()) => "2025년 9월 30일"
 */
export function formatDate(date: Date | string): string {
  const dateObj = typeof date === 'string' ? new Date(date) : date
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(dateObj)
}

/**
 * 상대 시간 포맷 (방금 전, 1분 전, 1시간 전 등)
 */
export function formatRelativeTime(date: Date | string): string {
  const dateObj = typeof date === 'string' ? new Date(date) : date
  const now = new Date()
  const diffInSeconds = Math.floor((now.getTime() - dateObj.getTime()) / 1000)

  if (diffInSeconds < 60) return '방금 전'
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}분 전`
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}시간 전`
  if (diffInSeconds < 2592000) return `${Math.floor(diffInSeconds / 86400)}일 전`

  return formatDate(dateObj)
}

/**
 * 수익률에 따른 색상 클래스 반환 (다크 테마용)
 * @param value - 수익률 값
 * @returns Tailwind CSS 색상 클래스
 */
export function getChangeColorClass(value: number): string {
  if (value > 0) return 'text-accent-green'
  if (value < 0) return 'text-accent-red'
  return 'text-text-tertiary'
}

/**
 * 수익률에 따른 백그라운드 색상 클래스 반환
 */
export function getChangeBgClass(value: number): string {
  if (value > 0) return 'bg-accent-green/10 border-accent-green/20'
  if (value < 0) return 'bg-accent-red/10 border-accent-red/20'
  return 'bg-background-quaternary'
}

/**
 * 수익률에 따른 글로우 효과 클래스 반환
 */
export function getChangeGlowClass(value: number): string {
  if (value > 0) return 'glow-green'
  if (value < 0) return 'glow-red'
  return ''
}

/**
 * API 에러 메시지 추출
 */
export function getErrorMessage(error: unknown): string {
  if (error instanceof Error) return error.message
  if (typeof error === 'string') return error
  return '알 수 없는 오류가 발생했습니다.'
}

/**
 * 큰 숫자를 축약 형식으로 포맷
 * @example formatCompactNumber(1234567) => "1.2M"
 */
export function formatCompactNumber(num: number): string {
  if (num >= 1000000000) {
    return (num / 1000000000).toFixed(1) + 'B'
  }
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}
