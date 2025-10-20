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

// 시장 지수 (Mock 데이터)
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
    name: 'Dow Jones',
    symbol: 'DJI',
    value: 34567.89,
    change: 123.45,
    changePercent: 0.36,
    timestamp: new Date().toISOString(),
    isMock: true,
  },
  {
    name: 'Bitcoin',
    symbol: 'BTC',
    value: 95234567, // ₩95,234,567
    change: 1234567,
    changePercent: 1.31,
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

// 암호화폐 Mock 데이터
export const mockCryptoAssets = [
  {
    symbol: 'BTC',
    name: 'Bitcoin',
    price: 95234567,
    change: 1234567,
    changePercent: 1.31,
    marketCap: '1.8조 KRW',
    volume24h: '32.5조 KRW',
    isMock: true,
  },
  {
    symbol: 'ETH',
    name: 'Ethereum',
    price: 5123456,
    change: -123456,
    changePercent: -2.35,
    marketCap: '615조 KRW',
    volume24h: '18.2조 KRW',
    isMock: true,
  },
  {
    symbol: 'XRP',
    name: 'Ripple',
    price: 3456,
    change: 123,
    changePercent: 3.69,
    marketCap: '193조 KRW',
    volume24h: '12.8조 KRW',
    isMock: true,
  },
  {
    symbol: 'ADA',
    name: 'Cardano',
    price: 1234,
    change: 45,
    changePercent: 3.78,
    marketCap: '43.2조 KRW',
    volume24h: '5.1조 KRW',
    isMock: true,
  },
  {
    symbol: 'SOL',
    name: 'Solana',
    price: 234567,
    change: 12345,
    changePercent: 5.56,
    marketCap: '97.8조 KRW',
    volume24h: '8.9조 KRW',
    isMock: true,
  },
];

// 주식 Mock 데이터
export const mockStockAssets = [
  {
    symbol: 'AAPL',
    name: 'Apple Inc.',
    price: 180000,
    change: 4500,
    changePercent: 2.56,
    marketCap: '3.2조 USD',
    volume: '5천만주',
    isMock: true,
  },
  {
    symbol: 'MSFT',
    name: 'Microsoft Corp.',
    price: 330000,
    change: -3960,
    changePercent: -1.18,
    marketCap: '2.8조 USD',
    volume: '3천만주',
    isMock: true,
  },
  {
    symbol: 'GOOGL',
    name: 'Alphabet Inc.',
    price: 145000,
    change: 2900,
    changePercent: 2.04,
    marketCap: '1.8조 USD',
    volume: '2.5천만주',
    isMock: true,
  },
  {
    symbol: 'TSLA',
    name: 'Tesla Inc.',
    price: 250000,
    change: -12500,
    changePercent: -4.76,
    marketCap: '795조 USD',
    volume: '1.2억주',
    isMock: true,
  },
  {
    symbol: 'NVDA',
    name: 'NVIDIA Corp.',
    price: 520000,
    change: 15600,
    changePercent: 3.09,
    marketCap: '1.3조 USD',
    volume: '4천만주',
    isMock: true,
  },
];

// 채권 Mock 데이터
export const mockBondAssets = [
  {
    symbol: 'AGG',
    name: 'iShares Core U.S. Aggregate Bond ETF',
    price: 98.45,
    change: -0.23,
    changePercent: -0.23,
    yield: '4.25%',
    duration: '6.2년',
    isMock: true,
  },
  {
    symbol: 'BND',
    name: 'Vanguard Total Bond Market ETF',
    price: 72.80,
    change: 0.15,
    changePercent: 0.21,
    yield: '4.50%',
    duration: '6.5년',
    isMock: true,
  },
  {
    symbol: 'TLT',
    name: 'iShares 20+ Year Treasury Bond ETF',
    price: 89.23,
    change: -1.45,
    changePercent: -1.60,
    yield: '4.85%',
    duration: '17.8년',
    isMock: true,
  },
  {
    symbol: 'SHY',
    name: 'iShares 1-3 Year Treasury Bond ETF',
    price: 82.15,
    change: 0.05,
    changePercent: 0.06,
    yield: '4.95%',
    duration: '1.9년',
    isMock: true,
  },
];

// 금/원자재 Mock 데이터
export const mockCommodityAssets = [
  {
    symbol: 'GLD',
    name: 'SPDR Gold Shares',
    price: 185.50,
    change: 2.30,
    changePercent: 1.26,
    underlying: '금 1온스',
    volume: '850만주',
    isMock: true,
  },
  {
    symbol: 'SLV',
    name: 'iShares Silver Trust',
    price: 23.45,
    change: -0.45,
    changePercent: -1.88,
    underlying: '은 1온스',
    volume: '2천만주',
    isMock: true,
  },
  {
    symbol: 'USO',
    name: 'United States Oil Fund',
    price: 78.90,
    change: 3.20,
    changePercent: 4.23,
    underlying: 'WTI 원유',
    volume: '1.5천만주',
    isMock: true,
  },
  {
    symbol: 'DBA',
    name: 'Invesco DB Agriculture Fund',
    price: 19.85,
    change: 0.15,
    changePercent: 0.76,
    underlying: '농산물 선물',
    volume: '50만주',
    isMock: true,
  },
];

// 자산 검색 결과 (통합)
export const mockAssetSearchResults = [
  // 주식
  { symbol: 'AAPL', name: 'Apple Inc.', type: 'STOCK', price: 180000, change: 4500, changePercent: 2.56 },
  { symbol: 'MSFT', name: 'Microsoft Corp.', type: 'STOCK', price: 330000, change: -3960, changePercent: -1.18 },
  { symbol: 'GOOGL', name: 'Alphabet Inc.', type: 'STOCK', price: 145000, change: 2900, changePercent: 2.04 },
  { symbol: 'TSLA', name: 'Tesla Inc.', type: 'STOCK', price: 250000, change: -12500, changePercent: -4.76 },
  { symbol: 'NVDA', name: 'NVIDIA Corp.', type: 'STOCK', price: 520000, change: 15600, changePercent: 3.09 },

  // 암호화폐
  { symbol: 'BTC', name: 'Bitcoin', type: 'CRYPTO', price: 95234567, change: 1234567, changePercent: 1.31 },
  { symbol: 'ETH', name: 'Ethereum', type: 'CRYPTO', price: 5123456, change: -123456, changePercent: -2.35 },
  { symbol: 'XRP', name: 'Ripple', type: 'CRYPTO', price: 3456, change: 123, changePercent: 3.69 },

  // 채권
  { symbol: 'AGG', name: 'U.S. Aggregate Bond ETF', type: 'BOND', price: 98.45, change: -0.23, changePercent: -0.23 },
  { symbol: 'BND', name: 'Total Bond Market ETF', type: 'BOND', price: 72.80, change: 0.15, changePercent: 0.21 },

  // 금/원자재
  { symbol: 'GLD', name: 'Gold Shares', type: 'COMMODITY', price: 185.50, change: 2.30, changePercent: 1.26 },
  { symbol: 'SLV', name: 'Silver Trust', type: 'COMMODITY', price: 23.45, change: -0.45, changePercent: -1.88 },
];