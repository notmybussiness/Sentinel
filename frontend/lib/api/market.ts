/**
 * Market Data API 클라이언트
 *
 * 시장 데이터 관련 API 호출 함수들
 */

import { get, post } from './client';

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
 * 각 심볼별로 개별 API 호출 (GET /price/{symbol})
 * 종목 수가 적을 때 사용 (<5개)
 *
 * @param symbols 종목 심볼 배열
 * @returns 종목 가격 배열
 *
 * @example
 * const prices = await getStockPrices(['AAPL', 'MSFT', 'GOOGL']);
 * console.log(prices.length); // 3
 *
 * @deprecated 종목이 5개 이상이면 getBatchPrices() 사용 권장
 */
export async function getStockPrices(symbols: string[]): Promise<StockPrice[]> {
  const promises = symbols.map(symbol => getStockPrice(symbol));
  return Promise.all(promises);
}

/**
 * 배치 가격 조회 (POST 방식)
 *
 * 엔드포인트: POST /api/v1/market/prices
 *
 * 여러 종목의 가격을 한 번의 API 호출로 조회합니다.
 * Portfolio Holdings처럼 많은 종목을 조회할 때 효율적입니다.
 *
 * 장점:
 * - 단일 HTTP 요청 (네트워크 오버헤드 감소)
 * - API Rate Limit에 유리
 * - 일관된 timestamp (동일 시점 데이터)
 *
 * @param symbols 종목 심볼 배열 (최대 50개)
 * @returns 종목 가격 배열
 *
 * @example
 * // Portfolio Holdings 실시간 가격 업데이트
 * const symbols = portfolio.holdings.map(h => h.symbol);
 * const prices = await getBatchPrices(symbols);
 *
 * @throws {Error} 심볼이 50개를 초과하거나 비어있을 경우
 */
export async function getBatchPrices(symbols: string[]): Promise<StockPrice[]> {
  if (symbols.length === 0) {
    return [];
  }

  if (symbols.length > 50) {
    throw new Error('배치 조회는 최대 50개 종목까지 가능합니다.');
  }

  return post<StockPrice[]>('/api/v1/market/prices', symbols);
}

/**
 * 종목 검색
 *
 * 엔드포인트: GET /api/v1/market/search?query={query}
 *
 * 종목 심볼이나 회사명으로 검색합니다.
 * Holdings 추가 시 사용됩니다.
 *
 * @param query 검색 키워드 (예: "apple", "AAPL")
 * @returns 검색 결과 배열 (최대 10개)
 *
 * @example
 * const results = await searchSymbol('apple');
 * console.log(results[0].symbol); // "AAPL"
 * console.log(results[0].name);   // "Apple Inc."
 */
export async function searchSymbol(query: string): Promise<SearchResult[]> {
  if (!query || query.trim().length === 0) {
    return [];
  }

  return get<SearchResult[]>(`/api/v1/market/search?query=${encodeURIComponent(query.trim())}`);
}
