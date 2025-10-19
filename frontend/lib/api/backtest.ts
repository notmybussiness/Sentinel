import apiClient from './client'
import type { BacktestRequest, BacktestResponse } from '@/types'

/**
 * 백테스팅 API Client
 * 포트폴리오 백테스팅 및 성과 분석
 */

const BACKTEST_BASE_URL = '/api/v1/backtest'

/**
 * 백테스팅 실행
 *
 * @param request 백테스팅 요청
 * @returns 백테스팅 결과
 */
export async function runBacktest(request: BacktestRequest): Promise<BacktestResponse> {
  const response = await apiClient.post<BacktestResponse>(`${BACKTEST_BASE_URL}/run`, request)
  return response.data
}

/**
 * 백테스팅 서비스 상태 확인
 *
 * @returns 서비스 상태
 */
export async function getBacktestStatus(): Promise<{
  available: boolean
  provider: string
  supportedFrequencies: string[]
  maxDateRange: string
}> {
  const response = await apiClient.get(`${BACKTEST_BASE_URL}/status`)
  return response.data
}

/**
 * 리밸런싱 빈도 레이블 매핑
 */
export const rebalanceFrequencyLabels: Record<BacktestRequest['rebalanceFrequency'], string> = {
  NONE: '없음',
  MONTHLY: '월별',
  QUARTERLY: '분기별',
  YEARLY: '연별',
}

/**
 * 날짜 유효성 검증
 *
 * @param startDate 시작일 (YYYY-MM-DD)
 * @param endDate 종료일 (YYYY-MM-DD)
 * @returns 유효 여부
 */
export function validateDateRange(startDate: string, endDate: string): boolean {
  const start = new Date(startDate)
  const end = new Date(endDate)

  if (isNaN(start.getTime()) || isNaN(end.getTime())) {
    return false
  }

  if (start >= end) {
    return false
  }

  // Maximum 10 years
  const maxYears = 10
  const yearsDiff = (end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24 * 365.25)
  if (yearsDiff > maxYears) {
    return false
  }

  return true
}

/**
 * 날짜 범위 계산 (기본값 제공)
 *
 * @param period 기간 (1y, 2y, 3y, 5y)
 * @returns 시작일과 종료일
 */
export function getDefaultDateRange(period: '1y' | '2y' | '3y' | '5y'): {
  startDate: string
  endDate: string
} {
  const end = new Date()
  const start = new Date()

  switch (period) {
    case '1y':
      start.setFullYear(end.getFullYear() - 1)
      break
    case '2y':
      start.setFullYear(end.getFullYear() - 2)
      break
    case '3y':
      start.setFullYear(end.getFullYear() - 3)
      break
    case '5y':
      start.setFullYear(end.getFullYear() - 5)
      break
  }

  return {
    startDate: start.toISOString().split('T')[0],
    endDate: end.toISOString().split('T')[0],
  }
}

export default {
  runBacktest,
  getBacktestStatus,
}
