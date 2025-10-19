import apiClient from './client'
import type { AnalysisRequest, AnalysisResponse, AiServiceStatus } from '@/types'

/**
 * AI 분석 API Client
 * Gemini AI 기반 포트폴리오 분석 및 투자 제안
 */

const AI_BASE_URL = '/api/v1/ai'

/**
 * 포트폴리오 AI 분석 실행
 *
 * @param request 분석 요청 (portfolioId, analysisType)
 * @returns 분석 결과
 */
export async function analyzePortfolio(request: AnalysisRequest): Promise<AnalysisResponse> {
  const response = await apiClient.post<AnalysisResponse>(`${AI_BASE_URL}/analyze`, request)
  return response.data
}

/**
 * AI 서비스 상태 확인
 *
 * @returns 서비스 상태 (available, provider, model, features)
 */
export async function getAiServiceStatus(): Promise<AiServiceStatus> {
  const response = await apiClient.get<AiServiceStatus>(`${AI_BASE_URL}/status`)
  return response.data
}

/**
 * AI 테스트 (개발용)
 *
 * @param message 테스트 메시지
 * @returns AI 응답
 */
export async function testAi(message: string): Promise<{ request: string; response: string }> {
  const response = await apiClient.post<{ request: string; response: string }>(
    `${AI_BASE_URL}/test?message=${encodeURIComponent(message)}`
  )
  return response.data
}

/**
 * 분석 타입별 레이블 매핑
 */
export const analysisTypeLabels: Record<AnalysisRequest['analysisType'], string> = {
  OVERVIEW: '전체 개요',
  DIVERSIFICATION: '다각화 분석',
  RISK: '리스크 분석',
  PERFORMANCE: '성과 분석',
  RECOMMENDATION: '투자 제안',
}

/**
 * 리스크 레벨별 색상 매핑 (Tailwind CSS)
 */
export const riskLevelColors: Record<AnalysisResponse['riskLevel'], string> = {
  LOW: 'text-green-500',
  MEDIUM: 'text-yellow-500',
  HIGH: 'text-red-500',
}

/**
 * 리스크 레벨별 레이블
 */
export const riskLevelLabels: Record<AnalysisResponse['riskLevel'], string> = {
  LOW: '낮음',
  MEDIUM: '중간',
  HIGH: '높음',
}

export default {
  analyzePortfolio,
  getAiServiceStatus,
  testAi,
}
