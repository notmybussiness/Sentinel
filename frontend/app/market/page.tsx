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
import { mockMarketIndices } from '@/lib/mockData';
import { searchSymbol } from '@/lib/api/market';
import type { AssetClass, AssetSearchResult } from '@/types';

export default function MarketPage() {
  const [selectedAsset, setSelectedAsset] = useState<AssetClass | 'ALL'>('ALL');
  const [searchResults, setSearchResults] = useState<AssetSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  const handleSearch = async (query: string) => {
    // 검색어가 비어있으면 결과 초기화
    if (!query || query.trim().length === 0) {
      setSearchResults([]);
      setSearchError(null);
      return;
    }

    // 최소 2글자 이상 입력 시 검색
    if (query.trim().length < 2) {
      return;
    }

    setIsSearching(true);
    setSearchError(null);

    try {
      // 실제 API 호출
      const results = await searchSymbol(query.trim());

      // API 응답을 AssetSearchResult 타입으로 변환
      const formattedResults: AssetSearchResult[] = results.map(result => ({
        symbol: result.symbol,
        name: result.name,
        exchange: result.exchange,
        type: 'STOCK' as AssetClass, // AlphaVantage는 주로 주식 데이터
        price: 0, // 검색 결과에는 가격 없음
        change: 0,
        changePercent: 0,
      }));

      setSearchResults(formattedResults);
    } catch (error) {
      console.error('종목 검색 실패:', error);
      setSearchError('종목 검색에 실패했습니다. 다시 시도해주세요.');
      setSearchResults([]);
    } finally {
      setIsSearching(false);
    }
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
              {/* 로딩 상태 */}
              {isSearching ? (
                <div className="text-center py-8 text-text-tertiary">
                  <div className="animate-pulse">검색 중...</div>
                </div>
              ) : /* 에러 상태 */
              searchError ? (
                <div className="text-center py-8">
                  <div className="text-error mb-2">⚠️ {searchError}</div>
                  <button
                    onClick={() => setSearchError(null)}
                    className="text-brand-primary text-sm hover:underline"
                  >
                    다시 시도
                  </button>
                </div>
              ) : /* 검색 결과 없음 */
              filteredResults.length === 0 ? (
                <div className="text-center py-8 text-text-tertiary">
                  검색어를 입력하여 종목을 찾아보세요
                </div>
              ) : (
                /* 검색 결과 표시 */
                filteredResults.map((asset) => (
                  <Card
                    key={asset.symbol}
                    onClick={() => alert(`${asset.name} 상세 보기 (준비 중)`)}
                    padding="sm"
                    className="cursor-pointer hover:bg-background-tertiary transition-colors relative"
                  >
                    <span className="absolute top-2 right-2 px-2 py-0.5 bg-green-500/20 text-green-400 text-micro rounded-4">
                      ● 실시간
                    </span>
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