"use client";

import React from 'react';
import { PriceDisplay } from '@/components/ui/PriceDisplay';
import { PercentageChange } from '@/components/ui/PercentageChange';

export interface MarketCoin {
  /** 심볼 (예: BTC, ETH) */
  symbol: string;
  /** 코인 이름 (예: Bitcoin, Ethereum) */
  name: string;
  /** 현재 가격 */
  price: number;
  /** 24시간 변동률 */
  changePercent: number;
  /** 24시간 거래량 (USD) */
  volume24h?: number;
  /** 시가총액 (USD) */
  marketCap?: number;
  /** 로고 이미지 URL */
  logoUrl?: string;
}

interface MarketCoinCardProps {
  coin: MarketCoin;
  /** 클릭 핸들러 */
  onClick?: () => void;
}

/**
 * Binance 스타일 코인 카드
 *
 * 심볼, 가격, 24h 변동, 거래량 표시
 */
export function MarketCoinCard({ coin, onClick }: MarketCoinCardProps) {
  return (
    <div
      onClick={onClick}
      className="
        p-4 bg-background-secondary rounded-8
        hover:bg-background-tertiary transition-all cursor-pointer
        border border-border-primary
      "
    >
      {/* 상단: 심볼 + 이름 */}
      <div className="flex items-center gap-2 mb-3">
        {coin.logoUrl && (
          <img
            src={coin.logoUrl}
            alt={coin.name}
            className="w-6 h-6 rounded-full"
          />
        )}
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <span className="text-text-primary font-semibold text-sm">
              {coin.symbol}
            </span>
            <span className="text-text-tertiary text-xs">
              {coin.name}
            </span>
          </div>
        </div>
      </div>

      {/* 중단: 가격 */}
      <div className="mb-2">
        <PriceDisplay
          amount={coin.price}
          currency="KRW"
          size="lg"
        />
      </div>

      {/* 하단: 변동률 + 거래량 */}
      <div className="flex items-center justify-between">
        <PercentageChange value={coin.changePercent} size="sm" />

        {coin.volume24h && (
          <div className="text-text-quaternary text-xs">
            Vol: {formatVolume(coin.volume24h)}
          </div>
        )}
      </div>

      {/* 시가총액 (선택적) */}
      {coin.marketCap && (
        <div className="mt-2 pt-2 border-t border-border-primary">
          <div className="flex items-center justify-between text-xs">
            <span className="text-text-tertiary">Market Cap</span>
            <span className="text-text-secondary font-medium">
              {formatMarketCap(coin.marketCap)}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * 거래량 포맷 (K, M, B 단위)
 */
function formatVolume(volume: number): string {
  if (volume >= 1_000_000_000) {
    return `$${(volume / 1_000_000_000).toFixed(2)}B`;
  }
  if (volume >= 1_000_000) {
    return `$${(volume / 1_000_000).toFixed(2)}M`;
  }
  if (volume >= 1_000) {
    return `$${(volume / 1_000).toFixed(2)}K`;
  }
  return `$${volume.toFixed(2)}`;
}

/**
 * 시가총액 포맷 (T, B 단위)
 */
function formatMarketCap(marketCap: number): string {
  if (marketCap >= 1_000_000_000_000) {
    return `$${(marketCap / 1_000_000_000_000).toFixed(2)}T`;
  }
  if (marketCap >= 1_000_000_000) {
    return `$${(marketCap / 1_000_000_000).toFixed(2)}B`;
  }
  if (marketCap >= 1_000_000) {
    return `$${(marketCap / 1_000_000).toFixed(2)}M`;
  }
  return `$${marketCap.toFixed(2)}`;
}
