"use client";

import React, { useState } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';
import { Section } from '@/components/ui/Section';
import { Tabs, Tab } from '@/components/ui/Tabs';
import { IndexCard } from '@/components/market/IndexCard';
import { AssetFilter } from '@/components/market/AssetFilter';
import { StockSearchBar } from '@/components/market/StockSearchBar';
import { Card } from '@/components/ui/Card';
import { PriceDisplay } from '@/components/ui/PriceDisplay';
import { PercentageChange } from '@/components/ui/PercentageChange';
import { MockDataBadge } from '@/components/common/MockDataBadge';
import { mockMarketIndices, mockAssetSearchResults } from '@/lib/mockData';
import type { AssetClass } from '@/types';

export default function MarketPage() {
  const [selectedAsset, setSelectedAsset] = useState<AssetClass | 'ALL'>('ALL');
  const [searchResults, setSearchResults] = useState(mockAssetSearchResults);

  const handleSearch = (query: string) => {
    if (!query) {
      setSearchResults(mockAssetSearchResults);
      return;
    }

    const filtered = mockAssetSearchResults.filter(
      (asset) =>
        asset.name.toLowerCase().includes(query.toLowerCase()) ||
        asset.symbol.toLowerCase().includes(query.toLowerCase())
    );
    setSearchResults(filtered);
  };

  const filteredResults =
    selectedAsset === 'ALL'
      ? searchResults
      : searchResults.filter((asset) => asset.type === selectedAsset);

  const tabs: Tab[] = [
    {
      key: 'indices',
      label: '주요 지수',
      content: (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
          {mockMarketIndices.map((index) => (
            <IndexCard key={index.symbol} index={index} />
          ))}
        </div>
      ),
    },
    {
      key: 'fear-greed',
      label: '공포 탐욕 지수',
      content: (
        <div className="text-center py-16">
          <div className="text-6xl mb-4">😨😃</div>
          <h3 className="text-text-primary text-xl font-semibold mb-2">
            준비 중입니다
          </h3>
          <p className="text-text-tertiary">
            공포 탐욕 지수가 곧 제공될 예정입니다
          </p>
        </div>
      ),
    },
    {
      key: 'crypto',
      label: '암호화폐',
      content: (
        <div className="text-center py-16">
          <div className="text-6xl mb-4">₿</div>
          <h3 className="text-text-primary text-xl font-semibold mb-2">
            준비 중입니다
          </h3>
          <p className="text-text-tertiary">
            암호화폐 시세가 곧 제공될 예정입니다
          </p>
        </div>
      ),
    },
  ];

  return (
    <div className="min-h-screen bg-background-primary">
      <PageHeader
        title="시장 데이터"
        subtitle="실시간 시장 정보 및 주요 지표"
      />

      <div className="max-w-6xl mx-auto px-8 py-8 space-y-6">
        {/* 탭 섹션 */}
        <Tabs tabs={tabs} defaultTab="indices" />

        {/* 자산 검색 */}
        <Section
          title="자산 검색"
          subtitle="주식, 암호화폐, 상품 등 다양한 자산 검색"
          noPadding
          background="none"
        >
          <div className="space-y-3">
            {/* 검색바 */}
            <StockSearchBar
              onSearch={handleSearch}
              placeholder="자산명 또는 심볼 검색 (예: Apple, AAPL, Bitcoin)"
            />

            {/* 필터 */}
            <AssetFilter
              selected={selectedAsset}
              onChange={setSelectedAsset}
            />

            {/* 검색 결과 */}
            <div className="space-y-2">
              {filteredResults.length === 0 ? (
                <div className="text-center py-8 text-text-tertiary">
                  검색 결과가 없습니다
                </div>
              ) : (
                filteredResults.map((asset) => (
                  <Card
                    key={asset.symbol}
                    onClick={() => alert(`${asset.name} 상세 보기 (준비 중)`)}
                    padding="sm"
                    className="cursor-pointer hover:bg-background-tertiary transition-colors relative"
                  >
                    <MockDataBadge
                      show={true}
                      className="absolute top-2 right-2"
                      size="sm"
                    />
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="flex items-center gap-2 mb-0.5">
                          <h4 className="text-text-primary font-semibold text-sm">
                            {asset.symbol}
                          </h4>
                          <span className="text-text-tertiary text-mini">
                            {asset.name}
                          </span>
                          <span className="px-1.5 py-0.5 bg-background-tertiary text-text-tertiary text-micro rounded-4">
                            {asset.type}
                          </span>
                        </div>
                      </div>
                      <div className="text-right">
                        <PriceDisplay
                          amount={asset.price}
                          currency={
                            asset.type === 'CRYPTO' ? 'KRW' : 'KRW'
                          }
                          size="md"
                        />
                        <PercentageChange
                          value={asset.change}
                          size="sm"
                        />
                      </div>
                    </div>
                  </Card>
                ))
              )}
            </div>
          </div>
        </Section>
      </div>
    </div>
  );
}