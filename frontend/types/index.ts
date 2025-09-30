/**
 * 사용자 정보
 */
export interface User {
  id: number
  kakaoId: string
  nickname: string
  email: string
  profileImage?: string
  createdAt: string
}

/**
 * 인증 응답
 */
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: User
}

/**
 * 포트폴리오 보유 종목
 */
export interface PortfolioHolding {
  id: number
  portfolioId: number
  symbol: string
  quantity: number
  averageCost: number
  currentPrice: number
  marketValue: number
  totalCost: number
  gainLoss: number
  gainLossPercent: number
  createdAt: string
  updatedAt: string
}

/**
 * 포트폴리오
 */
export interface Portfolio {
  id: number
  userId: number
  name: string
  description: string
  totalValue: number
  totalCost: number
  totalGainLoss: number
  totalGainLossPercent: number
  createdAt: string
  updatedAt: string
  holdings: PortfolioHolding[]
}

/**
 * 주식 가격 정보
 */
export interface StockPrice {
  symbol: string
  price: number
  open: number
  high: number
  low: number
  close: number
  change: number
  changePercent: number
  lastTradingDay: string
  timestamp: string
  provider: 'alphavantage' | 'finnhub' | 'yahoo'
}

/**
 * 리밸런싱 권장사항
 */
export interface RebalancingRecommendation {
  symbol: string
  currentWeight: number
  targetWeight: number
  currentShares: number
  targetShares: number
  action: 'BUY' | 'SELL' | 'HOLD'
  quantity: number
  estimatedAmount: number
}

/**
 * 리밸런싱 전략
 */
export type RebalancingStrategy = 'CALENDAR' | 'THRESHOLD' | 'HYBRID'

/**
 * 시장 지수
 */
export interface MarketIndex {
  name: string
  symbol: string
  value: number
  change: number
  changePercent: number
  timestamp: string
}

/**
 * 자산 분류
 */
export type AssetClass = 'STOCK' | 'REAL_ESTATE' | 'CRYPTO' | 'GOLD' | 'BOND' | 'CASH'

/**
 * Mock 데이터 표시용 타입
 */
export interface MockData {
  isMock: boolean
  source?: string
}

/**
 * API 응답 래퍼
 */
export interface ApiResponse<T> {
  data: T
  message?: string
  timestamp: string
}

/**
 * 페이지네이션
 */
export interface Pagination {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * 에러 응답
 */
export interface ApiError {
  message: string
  status: number
  timestamp: string
  path?: string
}