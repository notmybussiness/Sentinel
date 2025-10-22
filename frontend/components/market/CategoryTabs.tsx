"use client";

import React from 'react';

export type MarketCategory = 'hot' | 'new' | 'gainer' | 'volume';

interface CategoryTabsProps {
  /** 현재 선택된 카테고리 */
  activeCategory: MarketCategory;
  /** 카테고리 변경 핸들러 */
  onCategoryChange: (category: MarketCategory) => void;
}

interface CategoryConfig {
  id: MarketCategory;
  label: string;
  icon: string;
}

const categories: CategoryConfig[] = [
  { id: 'hot', label: 'Hot', icon: '🔥' },
  { id: 'new', label: 'New', icon: '🆕' },
  { id: 'gainer', label: 'Top Gainer', icon: '📈' },
  { id: 'volume', label: 'Top Volume', icon: '💰' },
];

/**
 * Binance 스타일 카테고리 탭
 *
 * Hot, New, Top Gainer, Top Volume 카테고리 전환
 */
export function CategoryTabs({ activeCategory, onCategoryChange }: CategoryTabsProps) {
  return (
    <div className="flex items-center gap-2 overflow-x-auto">
      {categories.map((category) => {
        const isActive = activeCategory === category.id;

        return (
          <button
            key={category.id}
            onClick={() => onCategoryChange(category.id)}
            className={`
              flex items-center gap-2 px-4 py-2 rounded-8
              whitespace-nowrap transition-all
              ${isActive
                ? 'bg-brand text-background-primary font-semibold'
                : 'bg-background-secondary text-text-secondary hover:bg-background-tertiary'
              }
            `}
          >
            <span className="text-base">{category.icon}</span>
            <span className="text-sm">{category.label}</span>
          </button>
        );
      })}
    </div>
  );
}
