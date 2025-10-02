'use client';

import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { searchSymbol, SearchResult } from '@/lib/api/market';

interface SymbolSearchInputProps {
  onSelect: (symbol: string, name: string) => void;
  placeholder?: string;
}

/**
 * 종목 검색 입력 컴포넌트
 *
 * 기능:
 * - 실시간 종목 검색 (2자 이상 입력 시)
 * - 검색 결과 드롭다운 표시
 * - 최대 10개 결과
 * - 로딩 및 에러 상태 처리
 *
 * 사용 예시:
 * <SymbolSearchInput
 *   onSelect={(symbol, name) => console.log(symbol, name)}
 *   placeholder="종목 심볼 또는 이름 검색"
 * />
 */
export default function SymbolSearchInput({
  onSelect,
  placeholder = '종목 심볼 또는 이름 검색 (예: AAPL, Apple)'
}: SymbolSearchInputProps) {
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [debouncedQuery, setDebouncedQuery] = useState('');

  // Debounce: 500ms 후에 실제 검색 실행
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query);
    }, 500);

    return () => clearTimeout(timer);
  }, [query]);

  // React Query로 검색 API 호출
  const { data: results, isLoading, error } = useQuery({
    queryKey: ['symbol-search', debouncedQuery],
    queryFn: () => searchSymbol(debouncedQuery),
    enabled: debouncedQuery.length >= 2, // 2자 이상일 때만 검색
    staleTime: 60000, // 1분간 캐시 유지
  });

  const handleSelect = (result: SearchResult) => {
    onSelect(result.symbol, result.name);
    setQuery(''); // 선택 후 입력 필드 초기화
    setIsOpen(false);
  };

  const handleInputChange = (value: string) => {
    setQuery(value);
    setIsOpen(true);
  };

  return (
    <div className="relative">
      {/* 검색 입력 필드 */}
      <input
        type="text"
        value={query}
        onChange={(e) => handleInputChange(e.target.value)}
        onFocus={() => setIsOpen(true)}
        placeholder={placeholder}
        className="w-full px-4 py-3 rounded-lg bg-dark-800/50 border border-primary-500/20
                   text-white placeholder-gray-400
                   focus:outline-none focus:border-primary-500/50
                   transition-all duration-200"
      />

      {/* 로딩 인디케이터 */}
      {isLoading && (
        <div className="absolute right-4 top-1/2 transform -translate-y-1/2">
          <div className="w-5 h-5 border-2 border-primary-500 border-t-transparent
                          rounded-full animate-spin" />
        </div>
      )}

      {/* 검색 결과 드롭다운 */}
      {isOpen && debouncedQuery.length >= 2 && (
        <div className="absolute z-50 w-full mt-2 rounded-lg
                        bg-dark-800 border border-primary-500/20
                        shadow-2xl shadow-primary-500/10
                        max-h-96 overflow-y-auto">

          {/* 에러 상태 */}
          {error && (
            <div className="px-4 py-3 text-red-400 text-sm">
              검색 중 오류가 발생했습니다. 다시 시도해주세요.
            </div>
          )}

          {/* 결과 없음 */}
          {!isLoading && !error && (!results || results.length === 0) && (
            <div className="px-4 py-3 text-gray-400 text-sm">
              검색 결과가 없습니다.
            </div>
          )}

          {/* 검색 결과 목록 */}
          {results && results.length > 0 && (
            <div className="divide-y divide-primary-500/10">
              {results.map((result) => (
                <button
                  key={result.symbol}
                  onClick={() => handleSelect(result)}
                  className="w-full px-4 py-3 text-left
                             hover:bg-primary-500/10
                             transition-all duration-200
                             group"
                >
                  {/* 심볼 */}
                  <div className="font-bold text-white group-hover:text-primary-400
                                  transition-colors duration-200">
                    {result.symbol}
                  </div>

                  {/* 회사명 */}
                  <div className="text-sm text-gray-400 mt-1 line-clamp-1">
                    {result.name}
                  </div>

                  {/* 거래소 및 타입 */}
                  <div className="flex gap-2 mt-1 text-xs text-gray-500">
                    {result.exchange && (
                      <span className="px-2 py-0.5 rounded bg-dark-700">
                        {result.exchange}
                      </span>
                    )}
                    {result.type && (
                      <span className="px-2 py-0.5 rounded bg-dark-700">
                        {result.type}
                      </span>
                    )}
                  </div>
                </button>
              ))}
            </div>
          )}

          {/* 안내 문구 */}
          <div className="px-4 py-2 text-xs text-gray-500 border-t border-primary-500/10">
            최대 10개 결과 표시 • AlphaVantage 제공
          </div>
        </div>
      )}

      {/* 2자 미만 입력 시 안내 */}
      {isOpen && query.length > 0 && query.length < 2 && (
        <div className="absolute z-50 w-full mt-2 px-4 py-3 rounded-lg
                        bg-dark-800 border border-primary-500/20
                        text-gray-400 text-sm">
          최소 2자 이상 입력해주세요.
        </div>
      )}

      {/* 드롭다운 닫기용 백그라운드 */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40"
          onClick={() => setIsOpen(false)}
        />
      )}
    </div>
  );
}
