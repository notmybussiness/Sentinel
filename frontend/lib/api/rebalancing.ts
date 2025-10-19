import apiClient from './client'
import type {
  RebalancingRequest,
  RebalancingResponse,
  RebalancingSimulationResponse,
  StrategyInfoResponse,
  RebalancingStrategy
} from '@/types'

/**
 * Rebalancing API Client
 * 포트폴리오 리밸런싱 추천 및 시뮬레이션
 */

const REBALANCING_BASE_URL = '/api/v1/rebalancing'

/**
 * 리밸런싱 추천 생성
 *
 * @param request 리밸런싱 요청
 * @returns 리밸런싱 추천 결과
 */
export async function getRebalancingRecommendations(
  request: RebalancingRequest
): Promise<RebalancingResponse> {
  const response = await apiClient.post<RebalancingResponse>(
    `${REBALANCING_BASE_URL}/recommend`,
    request
  )
  return response.data
}

/**
 * 리밸런싱 시뮬레이션
 *
 * @param request 리밸런싱 요청
 * @returns 시뮬레이션 결과
 */
export async function simulateRebalancing(
  request: RebalancingRequest
): Promise<RebalancingSimulationResponse> {
  const response = await apiClient.post<RebalancingSimulationResponse>(
    `${REBALANCING_BASE_URL}/simulate`,
    request
  )
  return response.data
}

/**
 * 지원하는 리밸런싱 전략 목록 조회
 *
 * @returns 전략 목록
 */
export async function getRebalancingStrategies(): Promise<StrategyInfoResponse> {
  const response = await apiClient.get<StrategyInfoResponse>(`${REBALANCING_BASE_URL}/strategies`)
  return response.data
}

/**
 * 리밸런싱 전략 레이블 매핑
 */
export const rebalancingStrategyLabels: Record<RebalancingStrategy, string> = {
  EQUAL_WEIGHT: '균등 비중',
  TARGET_ALLOCATION: '목표 비중',
  RISK_PARITY: '위험 균등',
}

/**
 * 리밸런싱 액션 레이블 매핑
 */
export const rebalancingActionLabels: Record<'BUY' | 'SELL' | 'HOLD', string> = {
  BUY: '매수',
  SELL: '매도',
  HOLD: '유지',
}

/**
 * 리밸런싱 액션 색상 매핑
 */
export const rebalancingActionColors: Record<'BUY' | 'SELL' | 'HOLD', string> = {
  BUY: 'text-success',
  SELL: 'text-error',
  HOLD: 'text-text-tertiary',
}

export default {
  getRebalancingRecommendations,
  simulateRebalancing,
  getRebalancingStrategies,
}
