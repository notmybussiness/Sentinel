import apiClient from './client';

/**
 * 차트 데이터 포인트
 */
export interface ChartDataPoint {
  time: string;
  value: number;
  open?: number;
  high?: number;
  low?: number;
  volume?: number;
}

/**
 * 차트 통계
 */
export interface ChartStatistics {
  changePercent: number;
  changeAmount: number;
  highestPrice: number;
  lowestPrice: number;
  averageVolume: number;
}

/**
 * 차트 데이터 응답
 */
export interface ChartData {
  symbol: string;
  baseCurrency: string;
  data: ChartDataPoint[];
  statistics: ChartStatistics;
}

/**
 * 가격 이력 DTO
 */
export interface PriceHistoryDto {
  id: number;
  symbol: string;
  assetType: 'STOCK' | 'CRYPTO' | 'ETF' | 'INDEX';
  baseCurrency: string;
  timestamp: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

/**
 * 차트 데이터 조회
 *
 * GET /api/v1/price-history/chart
 */
export async function getChartData(
  symbol: string,
  startTime: string,
  endTime: string
): Promise<ChartData> {
  const response = await apiClient.get<ChartData>('/price-history/chart', {
    params: { symbol, startTime, endTime },
  });
  return response.data;
}

/**
 * 최신 가격 조회 (단일 심볼)
 *
 * GET /api/v1/price-history/latest/{symbol}
 */
export async function getLatestPrice(symbol: string): Promise<PriceHistoryDto> {
  const response = await apiClient.get<PriceHistoryDto>(`/price-history/latest/${symbol}`);
  return response.data;
}

/**
 * 최신 가격 조회 (복수 심볼)
 *
 * GET /api/v1/price-history/latest
 */
export async function getLatestPrices(
  symbols: string[],
  assetType: 'STOCK' | 'CRYPTO' | 'ETF' | 'INDEX'
): Promise<PriceHistoryDto[]> {
  const response = await apiClient.get<PriceHistoryDto[]>('/price-history/latest', {
    params: {
      symbols: symbols.join(','),
      assetType,
    },
  });
  return response.data;
}

/**
 * 자산 유형별 심볼 목록 조회
 *
 * GET /api/v1/price-history/symbols
 */
export async function getSymbolsByAssetType(
  assetType: 'STOCK' | 'CRYPTO' | 'ETF' | 'INDEX'
): Promise<string[]> {
  const response = await apiClient.get<string[]>('/price-history/symbols', {
    params: { assetType },
  });
  return response.data;
}
