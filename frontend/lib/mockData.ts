/**
 * Mock 데이터
 *
 * 개발 및 테스트용 모의 데이터
 */

// 추천 포트폴리오
export const mockRecommendedPortfolios = [
  {
    id: 1,
    name: '안정적 배당 포트폴리오',
    description: '높은 배당 수익률의 우량주 중심',
    totalValue: 50000000,
    annualReturn: 8.5,
    riskLevel: 'LOW' as const,
    holdings: [
      { symbol: 'KO', name: '코카콜라', weight: 25 },
      { symbol: 'JNJ', name: '존슨앤존슨', weight: 25 },
      { symbol: 'PG', name: '프록터앤갬블', weight: 25 },
      { symbol: 'VZ', name: '버라이즌', weight: 25 },
    ],
    isMock: true,
  },
  {
    id: 2,
    name: '성장형 기술주 포트폴리오',
    description: '빅테크 중심의 고성장 전략',
    totalValue: 75000000,
    annualReturn: 15.2,
    riskLevel: 'HIGH' as const,
    holdings: [
      { symbol: 'AAPL', name: '애플', weight: 30 },
      { symbol: 'MSFT', name: '마이크로소프트', weight: 25 },
      { symbol: 'GOOGL', name: '구글', weight: 25 },
      { symbol: 'NVDA', name: '엔비디아', weight: 20 },
    ],
    isMock: true,
  },
  {
    id: 3,
    name: '균형 투자 포트폴리오',
    description: '주식과 채권의 균형 잡힌 배분',
    totalValue: 60000000,
    annualReturn: 10.8,
    riskLevel: 'MEDIUM' as const,
    holdings: [
      { symbol: 'SPY', name: 'S&P 500 ETF', weight: 40 },
      { symbol: 'BND', name: '채권 ETF', weight: 40 },
      { symbol: 'GLD', name: '금 ETF', weight: 20 },
    ],
    isMock: true,
  },
  {
    id: 4,
    name: 'ESG 친환경 포트폴리오',
    description: '지속가능한 투자를 위한 ESG 전략',
    totalValue: 45000000,
    annualReturn: 12.3,
    riskLevel: 'MEDIUM' as const,
    holdings: [
      { symbol: 'TSLA', name: '테슬라', weight: 30 },
      { symbol: 'ENPH', name: 'Enphase', weight: 25 },
      { symbol: 'NEE', name: 'NextEra Energy', weight: 25 },
      { symbol: 'ICLN', name: '청정에너지 ETF', weight: 20 },
    ],
    isMock: true,
  },
  {
    id: 5,
    name: '글로벌 분산 포트폴리오',
    description: '전 세계 시장 분산 투자',
    totalValue: 80000000,
    annualReturn: 9.7,
    riskLevel: 'MEDIUM' as const,
    holdings: [
      { symbol: 'VTI', name: '미국 전체 시장', weight: 40 },
      { symbol: 'VEA', name: '선진국 시장', weight: 30 },
      { symbol: 'VWO', name: '이머징 시장', weight: 20 },
      { symbol: 'AGG', name: '종합 채권', weight: 10 },
    ],
    isMock: true,
  },
];

// 시장 인덱스
export const mockMarketIndices = [
  {
    name: 'S&P 500',
    symbol: 'SPX',
    value: 4567.89,
    change: 45.23,
    changePercent: 1.0,
    timestamp: new Date().toISOString(),
    isMock: true,
  },
  {
    name: 'NASDAQ',
    symbol: 'IXIC',
    value: 14234.56,
    change: -23.45,
    changePercent: -0.16,
    timestamp: new Date().toISOString(),
    isMock: true,
  },
  {
    name: 'DOW',
    symbol: 'DJI',
    value: 34567.89,
    change: 0,
    changePercent: 0,
    timestamp: new Date().toISOString(),
    isMock: true,
  },
  {
    name: 'KOSPI',
    symbol: 'KOSPI',
    value: 2567.89,
    change: 12.34,
    changePercent: 0.48,
    timestamp: new Date().toISOString(),
    isMock: true,
  },
];

// 사용자 포트폴리오
export const mockUserPortfolios = [
  {
    id: 101,
    userId: 1,
    name: '장기 투자 포트폴리오',
    description: 'S&P 500 중심의 장기 투자 전략',
    totalValue: 50000000,
    totalCost: 45000000,
    totalGainLoss: 5000000,
    totalGainLossPercent: 11.11,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: new Date().toISOString(),
    holdings: [
      {
        id: 1,
        portfolioId: 101,
        symbol: 'AAPL',
        name: '애플',
        quantity: 100,
        averageCost: 150000,
        currentPrice: 180000,
        marketValue: 18000000,
        totalCost: 15000000,
        gainLoss: 3000000,
        gainLossPercent: 20.0,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: new Date().toISOString(),
      },
      {
        id: 2,
        portfolioId: 101,
        symbol: 'MSFT',
        name: '마이크로소프트',
        quantity: 80,
        averageCost: 300000,
        currentPrice: 330000,
        marketValue: 26400000,
        totalCost: 24000000,
        gainLoss: 2400000,
        gainLossPercent: 10.0,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: new Date().toISOString(),
      },
    ],
    isMock: true,
  },
  {
    id: 102,
    userId: 1,
    name: '배당 수익 포트폴리오',
    description: '안정적인 배당 수익 중심',
    totalValue: 30000000,
    totalCost: 32000000,
    totalGainLoss: -2000000,
    totalGainLossPercent: -6.25,
    createdAt: '2024-02-01T00:00:00Z',
    updatedAt: new Date().toISOString(),
    holdings: [],
    isMock: true,
  },
];

// 리밸런싱 추천
export const mockRebalancingRecommendations = [
  {
    symbol: 'AAPL',
    name: '애플',
    currentWeight: 40,
    targetWeight: 30,
    currentShares: 100,
    targetShares: 75,
    action: 'SELL' as const,
    quantity: 25,
    estimatedAmount: 4500000,
  },
  {
    symbol: 'GOOGL',
    name: '구글',
    currentWeight: 20,
    targetWeight: 30,
    currentShares: 20,
    targetShares: 30,
    action: 'BUY' as const,
    quantity: 10,
    estimatedAmount: 3000000,
  },
  {
    symbol: 'MSFT',
    name: '마이크로소프트',
    currentWeight: 25,
    targetWeight: 25,
    currentShares: 50,
    targetShares: 50,
    action: 'HOLD' as const,
    quantity: 0,
    estimatedAmount: 0,
  },
];

// 자산 검색 결과
export const mockAssetSearchResults = [
  { symbol: 'AAPL', name: '애플', type: 'STOCK', price: 180000, change: 2.5 },
  { symbol: 'MSFT', name: '마이크로소프트', type: 'STOCK', price: 330000, change: 1.2 },
  { symbol: 'BTC', name: '비트코인', type: 'CRYPTO', price: 60000000, change: -3.5 },
  { symbol: 'ETH', name: '이더리움', type: 'CRYPTO', price: 3000000, change: 4.2 },
  { symbol: 'GLD', name: '금 ETF', type: 'COMMODITY', price: 180, change: 0.5 },
];