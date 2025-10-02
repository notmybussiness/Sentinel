/**
 * Market Data API 클라이언트
 *
 * 시장 데이터 관련 API 호출 함수들
 */

import { get } from './client';

// ========== 타입 정의 ==========

/**
 * 시장 지수 데이터
 *
 * 백엔드 MarketIndexDto와 매핑
 */
export interface MarketIndex {
  symbol: string;           // 지수 심볼 (예: ^GSPC)
  name: string;             // 지수 이름 (예: S&P 500)
  value: number;            // 현재 지수 값
  change: number;           // 전일 대비 변동
  changePercent: number;    // 변동률 (%)
  timestamp: string | number[];  // 조회 시각 (LocalDateTime 배열 형태로 올 수 있음)
}

/**
 * 주식 가격 데이터
 *
 * 백엔드 StockPriceDto와 매핑
 */
export interface StockPrice {
  symbol: string;           // 종목 심볼
  price: number;            // 현재 가격
  open: number;             // 시가
  high: number;             // 고가
  low: number;              // 저가
  close: number;            // 종가
  change: number;           // 전일 대비 변동
  changePercent: number;    // 변동률 (%)
  lastTradingDay: string;   // 마지막 거래일
  timeStamp: string | number[];  // 조회 시각
  provider: string;         // 데이터 제공자 (AlphaVantage, Finnhub 등)
}

/**
 * 종목 검색 결과
 *
 * 백엔드 SearchResultDto와 매핑
 */
export interface SearchResult {
  symbol: string;           // 종목 심볼 (예: AAPL)
  name: string;             // 회사명 (예: Apple Inc.)
  exchange: string;         // 거래소/지역 (예: United States)
  type: string;             // 자산 유형 (예: Equity, ETF)
}

// ========== API 함수 ==========

/**
 * 주요 시장 지수 조회
 *
 * 엔드포인트: GET /api/v1/market/indices
 *
 * 반환 데이터:
 * - S&P 500 (^GSPC)
 * - NASDAQ (^IXIC)
 * - DOW (^DJI)
 * - KOSPI (^KS11)
 *
 * @returns 시장 지수 배열
 *
 * @example
 * const indices = await getMarketIndices();
 * console.log(indices[0].name); // "S&P 500"
 */
export async function getMarketIndices(): Promise<MarketIndex[]> {
  return get<MarketIndex[]>('/api/v1/market/indices');
}

/**
 * 단일 종목 가격 조회
 *
 * 엔드포인트: GET /api/v1/market/price/{symbol}
 *
 * @param symbol 종목 심볼 (예: AAPL, MSFT)
 * @returns 종목 가격 정보
 *
 * @example
 * const applePrice = await getStockPrice('AAPL');
 * console.log(applePrice.price); // 180.50
 */
export async function getStockPrice(symbol: string): Promise<StockPrice> {
  return get<StockPrice>(`/api/v1/market/price/${symbol}`);
}

/**
 * 여러 종목 가격 조회 (개별 호출)
 *
 * 현재는 각 심볼별로 개별 API 호출
 * 나중에 POST /prices (배치 조회)로 최적화 예정
 *
 * @param symbols 종목 심볼 배열
 * @returns 종목 가격 배열
 *
 * @example
 * const prices = await getStockPrices(['AAPL', 'MSFT', 'GOOGL']);
 * console.log(prices.length); // 3
 */
export async function getStockPrices(symbols: string[]): Promise<StockPrice[]> {
  // TODO: 나중에 POST /api/v1/market/prices 배치 API로 최적화
  const promises = symbols.map(symbol => getStockPrice(symbol));
  return Promise.all(promises);
}
