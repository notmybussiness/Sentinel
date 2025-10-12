/**
 * Crypto API Client
 * 암호화폐 데이터 API 호출 함수 모음
 */

import client from './client';

// ==================== Types ====================

export interface CryptoPrice {
  symbol: string;
  baseCurrency: string;
  marketCode: string;
  price: number;
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  volume: number;
  change: number;
  changePercent: number;
  tradeValue: number;
  timestamp: number[];
  provider: string;
}

export interface TrendingCoin {
  symbol: string;
  name: string;
  koreanName: string;
  marketCode: string;
  price: number;
  changePercent: number;
  volume: number;
  tradeValue: number;
  rank: number;
  baseCurrency: string;
}

export interface CryptoSearchResult {
  symbol: string;
  name: string;
  koreanName: string;
  marketCode: string;
  baseCurrency: string;
  tradeable: boolean;
  exchange: string;
}

export interface BatchPriceRequest {
  symbols: string[];
  baseCurrency?: string;
}

// ==================== API Functions ====================

/**
 * 단일 암호화폐 가격 조회
 * GET /api/v1/crypto/price/{symbol}?baseCurrency=KRW
 */
export const getCryptoPrice = async (
  symbol: string,
  baseCurrency: string = 'KRW'
): Promise<CryptoPrice> => {
  const response = await client.get<CryptoPrice>(
    `/crypto/price/${symbol}`,
    { params: { baseCurrency } }
  );
  return response.data;
};

/**
 * 배치 암호화폐 가격 조회
 * POST /api/v1/crypto/prices
 */
export const getBatchCryptoPrices = async (
  symbols: string[],
  baseCurrency: string = 'KRW'
): Promise<CryptoPrice[]> => {
  const response = await client.post<CryptoPrice[]>('/crypto/prices', {
    symbols,
    baseCurrency,
  });
  return response.data;
};

/**
 * 트렌딩 암호화폐 조회 (거래대금 상위)
 * GET /api/v1/crypto/trending?baseCurrency=KRW&limit=10
 */
export const getTrendingCoins = async (
  baseCurrency: string = 'KRW',
  limit: number = 10
): Promise<TrendingCoin[]> => {
  const response = await client.get<TrendingCoin[]>('/crypto/trending', {
    params: { baseCurrency, limit },
  });
  return response.data;
};

/**
 * 암호화폐 검색
 * GET /api/v1/crypto/search?query=bitcoin
 */
export const searchCrypto = async (
  query: string
): Promise<CryptoSearchResult[]> => {
  const response = await client.get<CryptoSearchResult[]>('/crypto/search', {
    params: { query },
  });
  return response.data;
};

/**
 * 과거 데이터 조회 (캔들 스틱)
 * GET /api/v1/crypto/historical/{symbol}?baseCurrency=KRW&days=30
 */
export const getHistoricalData = async (
  symbol: string,
  baseCurrency: string = 'KRW',
  days: number = 30
): Promise<CryptoPrice[]> => {
  const response = await client.get<CryptoPrice[]>(
    `/crypto/historical/${symbol}`,
    { params: { baseCurrency, days } }
  );
  return response.data;
};

/**
 * 서비스 상태 확인
 * GET /api/v1/crypto/status
 */
export const getCryptoServiceStatus = async (): Promise<{
  available: boolean;
  message: string;
}> => {
  const response = await client.get<{ available: boolean; message: string }>(
    '/crypto/status'
  );
  return response.data;
};

// ==================== Utility Functions ====================

/**
 * 가격 포맷팅 (한국 원화)
 */
export const formatKRW = (price: number): string => {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(price);
};

/**
 * 변동률 포맷팅
 */
export const formatChangePercent = (changePercent: number): string => {
  const sign = changePercent >= 0 ? '+' : '';
  return `${sign}${changePercent.toFixed(2)}%`;
};

/**
 * 거래량 포맷팅 (축약)
 */
export const formatVolume = (volume: number): string => {
  if (volume >= 1_000_000_000) {
    return `${(volume / 1_000_000_000).toFixed(2)}B`;
  }
  if (volume >= 1_000_000) {
    return `${(volume / 1_000_000).toFixed(2)}M`;
  }
  if (volume >= 1_000) {
    return `${(volume / 1_000).toFixed(2)}K`;
  }
  return volume.toFixed(2);
};

/**
 * 거래대금 포맷팅 (억원)
 */
export const formatTradeValue = (value: number): string => {
  const billions = value / 100_000_000; // 억원
  return `${billions.toFixed(0)}억원`;
};
