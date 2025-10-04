import React, { useState, useEffect, useRef } from 'react'
import { Input } from '../ui/Input'

interface StockSearchBarProps {
  onSearch: (query: string) => void
  placeholder?: string
  debounceMs?: number
}

/**
 * StockSearchBar 컴포넌트
 *
 * 주식/자산 검색 바 (Debounce 지원)
 *
 * @example
 * ```tsx
 * <StockSearchBar
 *   onSearch={(query) => handleSearch(query)}
 *   placeholder="종목명 또는 심볼 검색"
 *   debounceMs={300}
 * />
 * ```
 */
export function StockSearchBar({
  onSearch,
  placeholder = '종목명 또는 심볼 검색 (예: AAPL, Apple)',
  debounceMs = 300,
}: StockSearchBarProps) {
  const [query, setQuery] = useState('')
  const debounceTimerRef = useRef<NodeJS.Timeout | null>(null)

  // Debounce 효과: 입력 후 300ms 뒤에 검색 실행
  useEffect(() => {
    // 이전 타이머 취소
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }

    // 새 타이머 설정
    debounceTimerRef.current = setTimeout(() => {
      onSearch(query)
    }, debounceMs)

    // Cleanup: 컴포넌트 언마운트 시 타이머 정리
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current)
      }
    }
  }, [query, onSearch, debounceMs])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // Enter 키 입력 시 즉시 검색 (debounce 무시)
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current)
    }
    onSearch(query.trim())
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