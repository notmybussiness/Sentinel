import React from 'react'
import { cn } from '@/lib/utils'
import type { AssetClass } from '@/types'

interface AssetFilterProps {
  selected: AssetClass | 'ALL'
  onChange: (assetClass: AssetClass | 'ALL') => void
}

const assetClasses: Array<{ value: AssetClass | 'ALL'; label: string; icon: string }> = [
  { value: 'ALL', label: '전체', icon: '🌐' },
  { value: 'STOCK', label: '주식', icon: '📈' },
  { value: 'REAL_ESTATE', label: '부동산', icon: '🏠' },
  { value: 'CRYPTO', label: '가상자산', icon: '₿' },
  { value: 'GOLD', label: '금', icon: '🏆' },
  { value: 'BOND', label: '채권', icon: '📜' },
  { value: 'CASH', label: '현금', icon: '💵' },
]

/**
 * AssetFilter 컴포넌트
 *
 * 자산 분류별 필터 (주식, 부동산, 코인, 금, 채권 등)
 *
 * @example
 * ```tsx
 * <AssetFilter
 *   selected={selectedAsset}
 *   onChange={setSelectedAsset}
 * />
 * ```
 */
export function AssetFilter({ selected, onChange }: AssetFilterProps) {
  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-2">
      {assetClasses.map((asset) => (
        <button
          key={asset.value}
          onClick={() => onChange(asset.value)}
          className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-8',
            'text-small font-medium whitespace-nowrap',
            'transition-all duration-quick',
            'hover:bg-background-tertiary',
            selected === asset.value
              ? 'bg-brand-primary text-brand-text'
              : 'bg-background-secondary text-text-primary'
          )}
        >
          <span>{asset.icon}</span>
          <span>{asset.label}</span>
        </button>
      ))}
    </div>
  )
}