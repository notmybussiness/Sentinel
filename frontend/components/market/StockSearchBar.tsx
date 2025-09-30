import React, { useState } from 'react'
import { Input } from '../ui/Input'

interface StockSearchBarProps {
  onSearch: (query: string) => void
  placeholder?: string
}

/**
 * StockSearchBar 컴포넌트
 *
 * 주식/자산 검색 바
 *
 * @example
 * ```tsx
 * <StockSearchBar
 *   onSearch={(query) => handleSearch(query)}
 *   placeholder="종목명 또는 심볼 검색"
 * />
 * ```
 */
export function StockSearchBar({
  onSearch,
  placeholder = '종목명 또는 심볼 검색 (예: AAPL, Apple)',
}: StockSearchBarProps) {
  const [query, setQuery] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (query.trim()) {
      onSearch(query.trim())
    }
  }

  return (
    <form onSubmit={handleSubmit} className="w-full">
      <Input
        type="search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder={placeholder}
        leftIcon={
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
        }
        rightIcon={
          query && (
            <button
              type="button"
              onClick={() => {
                setQuery('')
                onSearch('')
              }}
              className="text-text-quaternary hover:text-text-primary transition-colors"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          )
        }
      />
    </form>
  )
}