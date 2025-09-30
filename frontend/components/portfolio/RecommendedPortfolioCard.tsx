"use client";

import React from 'react';
import { Card } from '../ui/Card';
import { PriceDisplay } from '../ui/PriceDisplay';
import { PercentageChange } from '../ui/PercentageChange';
import { Badge } from '../ui/Badge';
import { MockDataBadge } from '../common/MockDataBadge';

interface RecommendedPortfolioCardProps {
  portfolio: {
    id: number;
    name: string;
    description: string;
    totalValue: number;
    annualReturn: number;
    riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
    holdings: Array<{
      symbol: string;
      name: string;
      weight: number;
    }>;
    isMock?: boolean;
  };
  onClick?: () => void;
}

export function RecommendedPortfolioCard({
  portfolio,
  onClick,
}: RecommendedPortfolioCardProps) {
  const riskLevelMap = {
    LOW: { label: '낮음', variant: 'success' as const },
    MEDIUM: { label: '보통', variant: 'warning' as const },
    HIGH: { label: '높음', variant: 'danger' as const },
  };

  const riskInfo = riskLevelMap[portfolio.riskLevel];

  return (
    <Card
      onClick={onClick}
      padding="sm"
      className="h-full hover:shadow-glow transition-shadow cursor-pointer relative"
    >
      {/* Mock 배지 */}
      {portfolio.isMock && (
        <MockDataBadge
          show={true}
          className="absolute top-2 right-2"
          size="sm"
        />
      )}

      {/* 포트폴리오 이름 */}
      <h3 className="text-text-primary font-semibold mb-1.5">
        {portfolio.name}
      </h3>
      <p className="text-text-tertiary text-mini mb-3">
        {portfolio.description}
      </p>

      {/* 통계 */}
      <div className="space-y-2 mb-3">
        <div>
          <div className="text-text-quaternary text-micro mb-0.5">
            예상 자산 가치
          </div>
          <PriceDisplay
            amount={portfolio.totalValue}
            currency="KRW"
            size="md"
          />
        </div>

        <div>
          <div className="text-text-quaternary text-micro mb-0.5">
            연간 수익률
          </div>
          <PercentageChange
            value={portfolio.annualReturn}
            size="sm"
            showArrow={true}
          />
        </div>
      </div>

      {/* 위험 수준 */}
      <div className="mb-3">
        <Badge variant={riskInfo.variant} size="sm">
          위험도: {riskInfo.label}
        </Badge>
      </div>

      {/* 구성 종목 */}
      <div className="border-t border-border-primary pt-2">
        <div className="text-text-quaternary text-micro mb-1.5">구성 종목</div>
        <div className="flex flex-wrap gap-1.5">
          {portfolio.holdings.map((holding) => (
            <div
              key={holding.symbol}
              className="px-1.5 py-0.5 bg-background-tertiary rounded-4 text-micro"
            >
              <span className="text-text-primary font-medium">
                {holding.symbol}
              </span>
              <span className="text-text-quaternary ml-1">
                {holding.weight}%
              </span>
            </div>
          ))}
        </div>
      </div>
    </Card>
  );
}