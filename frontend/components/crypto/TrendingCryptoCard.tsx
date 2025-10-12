import React from 'react';
import { Card } from '../ui/Card';
import { formatNumber, formatPercent, getChangeColorClass } from '@/lib/utils';
import { formatKRW, formatTradeValue } from '@/lib/api/crypto';
import type { TrendingCoin } from '@/lib/api/crypto';

interface TrendingCryptoCardProps {
  coin: TrendingCoin;
}

/**
 * TrendingCryptoCard 컴포넌트
 *
 * 트렌딩 암호화폐를 표시하는 카드 (거래대금 상위)
 *
 * @example
 * ```tsx
 * <TrendingCryptoCard coin={{ ... }} />
 * ```
 */
export function TrendingCryptoCard({ coin }: TrendingCryptoCardProps) {
  const changeColor = getChangeColorClass(coin.changePercent);

  return (
    <Card padding="sm" shadow="low" hover>
      <div className="flex items-start justify-between">
        {/* 코인 정보 */}
        <div className="flex-1">
          {/* 순위 + 이름 */}
          <div className="flex items-center gap-2 mb-0.5">
            <div className="w-6 h-6 flex items-center justify-center bg-brand/10 rounded-4">
              <span className="text-mini font-bold text-brand">
                {coin.rank}
              </span>
            </div>
            <h3 className="text-sm font-semibold text-text-primary">
              {coin.koreanName}
            </h3>
          </div>

          {/* 가격 */}
          <p className="text-lg font-bold text-text-primary mb-0.5">
            {formatKRW(coin.price)}
          </p>

          {/* 변동률 */}
          <div className={`flex items-center gap-1 text-mini font-medium ${changeColor}`}>
            <span>
              {coin.changePercent > 0 ? '▲' : coin.changePercent < 0 ? '▼' : '―'}
            </span>
            <span>{formatPercent(Math.abs(coin.changePercent))}</span>
          </div>

          {/* 거래대금 */}
          <div className="mt-1 text-mini text-text-tertiary">
            거래대금: {formatTradeValue(coin.tradeValue)}
          </div>
        </div>

        {/* 심볼 */}
        <div className="px-2 py-1 bg-background-tertiary rounded-4">
          <span className="text-sm font-bold text-text-secondary">
            {coin.symbol}
          </span>
        </div>
      </div>
    </Card>
  );
}
