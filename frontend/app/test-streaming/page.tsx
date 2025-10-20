'use client';

import { useState, useEffect } from 'react';
import { useStreamingPrices } from '@/lib/hooks/useStreamingPrices';

export default function TestStreamingPage() {
  const [method, setMethod] = useState<'SSE' | 'LongPolling'>('SSE');
  const [updateCount, setUpdateCount] = useState(0);
  const [startTime, setStartTime] = useState(Date.now());

  const { prices, isConnected, error, currentMethod } = useStreamingPrices({
    symbols: ['BTC', 'ETH', 'XRP'],
    baseCurrency: 'KRW',
    method,
    autoFallback: true,
  });

  // 업데이트 횟수 카운트
  useEffect(() => {
    setUpdateCount((prev) => prev + 1);
  }, [prices]);

  // 평균 업데이트 주기 계산
  const elapsedSeconds = Math.max(1, (Date.now() - startTime) / 1000);
  const updatesPerSecond = (updateCount / elapsedSeconds).toFixed(2);

  // 방식 변경 시 카운터 초기화
  const handleMethodChange = (newMethod: 'SSE' | 'LongPolling') => {
    setMethod(newMethod);
    setUpdateCount(0);
    setStartTime(Date.now());
  };

  return (
    <div className="min-h-screen bg-primary p-8">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-primary mb-2">
          🧪 실시간 스트리밍 성능 비교 테스트
        </h1>
        <p className="text-secondary mb-8">
          Adapter 패턴으로 3가지 방식을 런타임에 전환하여 성능을 비교합니다.
        </p>

        {/* 방식 선택 */}
        <div className="glass-card p-6 mb-6">
          <h2 className="text-xl font-semibold text-primary mb-4">
            📡 스트리밍 방식 선택
          </h2>
          <div className="flex gap-4">
            <button
              onClick={() => handleMethodChange('SSE')}
              className={`px-6 py-3 rounded-12 font-medium transition-all ${
                method === 'SSE'
                  ? 'bg-brand text-primary shadow-glow'
                  : 'bg-secondary/50 text-secondary hover:bg-secondary'
              }`}
            >
              ✅ SSE (권장)
            </button>
            <button
              onClick={() => handleMethodChange('LongPolling')}
              className={`px-6 py-3 rounded-12 font-medium transition-all ${
                method === 'LongPolling'
                  ? 'bg-brand text-primary shadow-glow'
                  : 'bg-secondary/50 text-secondary hover:bg-secondary'
              }`}
            >
              🔄 Long Polling (Fallback)
            </button>
          </div>
        </div>

        {/* 연결 상태 & 성능 메트릭 */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          {/* 연결 상태 */}
          <div className="glass-card p-6">
            <div className="flex items-center gap-3 mb-2">
              <div
                className={`w-4 h-4 rounded-full ${
                  isConnected ? 'bg-success' : 'bg-error'
                }`}
              />
              <h3 className="text-lg font-semibold text-primary">연결 상태</h3>
            </div>
            <p className="text-2xl font-bold text-primary">
              {isConnected ? '연결됨' : '연결 끊김'}
            </p>
            <p className="text-sm text-secondary mt-1">
              방식: <span className="text-brand">{currentMethod}</span>
            </p>
          </div>

          {/* 업데이트 횟수 */}
          <div className="glass-card p-6">
            <h3 className="text-lg font-semibold text-primary mb-2">
              📊 업데이트 횟수
            </h3>
            <p className="text-2xl font-bold text-primary">{updateCount}</p>
            <p className="text-sm text-secondary mt-1">
              평균: {updatesPerSecond}/초
            </p>
          </div>

          {/* 실행 시간 */}
          <div className="glass-card p-6">
            <h3 className="text-lg font-semibold text-primary mb-2">
              ⏱️ 실행 시간
            </h3>
            <p className="text-2xl font-bold text-primary">
              {elapsedSeconds.toFixed(0)}초
            </p>
            <p className="text-sm text-secondary mt-1">
              시작: {new Date(startTime).toLocaleTimeString()}
            </p>
          </div>
        </div>

        {/* 에러 표시 */}
        {error && (
          <div className="glass-card p-4 mb-6 border-error">
            <p className="text-error">❌ {error}</p>
          </div>
        )}

        {/* 실시간 가격 */}
        <div className="glass-card p-6">
          <h2 className="text-xl font-semibold text-primary mb-4">
            💰 실시간 가격
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {['BTC', 'ETH', 'XRP'].map((symbol) => {
              const price = prices.get(symbol);
              return (
                <div key={symbol} className="glass-card p-4">
                  <h3 className="text-lg font-bold text-primary mb-2">
                    {symbol}
                  </h3>
                  {price ? (
                    <>
                      <div className="text-3xl font-bold text-primary mb-1">
                        {price.price.toLocaleString()}원
                      </div>
                      <div
                        className={`text-sm font-semibold ${
                          price.changePercent >= 0
                            ? 'text-success'
                            : 'text-error'
                        }`}
                      >
                        {price.changePercent >= 0 ? '+' : ''}
                        {price.changePercent.toFixed(2)}%
                      </div>
                      <div className="text-xs text-tertiary mt-2">
                        거래량: {price.volume.toFixed(2)}
                      </div>
                      <div className="text-xs text-tertiary">
                        마지막 업데이트:{' '}
                        {new Date(price.timestamp).toLocaleTimeString()}
                      </div>
                    </>
                  ) : (
                    <div className="text-secondary">로딩 중...</div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* 성능 비교 가이드 */}
        <div className="glass-card p-6 mt-6">
          <h2 className="text-xl font-semibold text-primary mb-4">
            📈 성능 비교 가이드
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <h3 className="font-semibold text-brand mb-2">✅ SSE</h3>
              <ul className="space-y-1 text-secondary">
                <li>• 업데이트 주기: 1초</li>
                <li>• 레이턴시: ~50ms</li>
                <li>• 권장: 일반적인 실시간 업데이트</li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold text-brand mb-2">🔄 Long Polling</h3>
              <ul className="space-y-1 text-secondary">
                <li>• 업데이트 주기: 2초</li>
                <li>• 레이턴시: ~200ms</li>
                <li>• 권장: Fallback 용도</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
