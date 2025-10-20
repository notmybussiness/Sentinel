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
  assetType: 'STOCK' | 'CRYPTO'
  baseCurrency?: string  // 'USD' or 'KRW' (crypto only)
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
export type RebalancingStrategy = 'EQUAL_WEIGHT' | 'TARGET_ALLOCATION' | 'RISK_PARITY'

/**
 * 리밸런싱 요청
 */
export interface RebalancingRequest {
  portfolioId: number
  strategy: RebalancingStrategy
  thresholdPercent?: number  // Default: 5.0
  considerTaxes?: boolean    // Default: false
}

/**
 * 리밸런싱 응답
 */
export interface RebalancingResponse {
  portfolioId: number
  portfolioName: string
  strategy: string
  currentValue: number
  needsRebalancing: boolean
  recommendations: RebalancingRecommendation[]
  totalTransactionCost: number
  estimatedTaxImpact: number
  analyzedAt: string
}

/**
 * 포트폴리오 상태 (시뮬레이션용)
 */
export interface PortfolioState {
  totalValue: number
  holdings: HoldingState[]
}

export interface HoldingState {
  symbol: string
  shares: number
  value: number
  weight: number  // %
}

/**
 * 거래 트랜잭션
 */
export interface Transaction {
  symbol: string
  action: 'BUY' | 'SELL'
  quantity: number
  price: number
  amount: number
  commission: number
}

/**
 * 리밸런싱 시뮬레이션 응답
 */
export interface RebalancingSimulationResponse {
  portfolioId: number
  portfolioName: string
  beforeRebalancing: PortfolioState
  afterRebalancing: PortfolioState
  transactions: Transaction[]
  totalTransactionCost: number
  netChange: number
}

/**
 * 리밸런싱 전략 정보
 */
export interface StrategyInfo {
  name: string
  displayName: string
  description: string
  supported: boolean
}

export interface StrategyInfoResponse {
  strategies: StrategyInfo[]
}

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
export type AssetClass = 'STOCK' | 'REAL_ESTATE' | 'CRYPTO' | 'GOLD' | 'BOND' | 'CASH' | 'COMMODITY'

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

/**
 * AI 분석 요청
 */
export interface AnalysisRequest {
  portfolioId: number
  analysisType: 'OVERVIEW' | 'DIVERSIFICATION' | 'RISK' | 'PERFORMANCE' | 'RECOMMENDATION'
  includeMarketContext?: boolean
}

/**
 * AI 분석 응답
 */
export interface AnalysisResponse {
  summary: string
  insights: string[]
  recommendations: string[]
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  diversificationScore: number
  analysisType: 'OVERVIEW' | 'DIVERSIFICATION' | 'RISK' | 'PERFORMANCE' | 'RECOMMENDATION'
  analyzedAt: string
}

/**
 * AI 서비스 상태
 */
export interface AiServiceStatus {
  available: boolean
  provider: string
  model: string
  features: string[]
}

/**
 * 백테스팅 요청
 */
export interface BacktestRequest {
  portfolioId: number
  startDate: string  // YYYY-MM-DD
  endDate: string    // YYYY-MM-DD
  initialCapital: number
  rebalanceFrequency: 'NONE' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
}

/**
 * 백테스팅 성과 지표
 */
export interface PerformanceMetrics {
  totalReturn: number       // %
  cagr: number              // %
  sharpeRatio: number
  sortinoRatio: number
  maxDrawdown: number       // %
  volatility: number        // %
  winRate: number           // %
}

/**
 * 포트폴리오 가치 포인트
 */
export interface EquityPoint {
  date: string
  value: number
  dailyReturn: number       // %
  cumulativeReturn: number  // %
}

/**
 * 거래 내역
 */
export interface Trade {
  symbol: string
  action: 'BUY' | 'SELL' | 'HOLD'
  quantity: number
  price: number
  amount: number
}

/**
 * 리밸런싱 이벤트
 */
export interface RebalanceEvent {
  date: string
  reason: string
  trades: Trade[]
}

/**
 * 종목별 최종 요약
 */
export interface HoldingSummary {
  symbol: string
  finalQuantity: number
  finalValue: number
  finalWeight: number     // %
  totalReturn: number     // %
}

/**
 * 백테스팅 응답
 */
export interface BacktestResponse {
  portfolioId: number
  portfolioName: string
  startDate: string
  endDate: string
  initialCapital: number
  finalValue: number
  rebalanceFrequency: 'NONE' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY'
  performance: PerformanceMetrics
  equityCurve: EquityPoint[]
  rebalanceEvents: RebalanceEvent[]
  holdingsSummary: HoldingSummary[]
  executedAt: string
}