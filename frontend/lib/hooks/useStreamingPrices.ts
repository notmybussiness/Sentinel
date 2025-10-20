import { useState, useEffect, useRef } from 'react';
import { CryptoPriceDto } from '@/types';
import { StreamingAdapter } from '../streaming/StreamingAdapter';
import { SSEAdapter } from '../streaming/SSEAdapter';
import { LongPollingAdapter } from '../streaming/LongPollingAdapter';

type StreamingMethod = 'SSE' | 'LongPolling';

interface UseStreamingPricesOptions {
  symbols: string[];
  baseCurrency?: string;
  method?: StreamingMethod;
  autoFallback?: boolean;
}

/**
 * 실시간 암호화폐 가격 스트리밍 Hook
 *
 * @param symbols 암호화폐 심볼 목록
 * @param baseCurrency 기준 통화 (기본값: KRW)
 * @param method 스트리밍 방식 (기본값: SSE)
 * @param autoFallback 자동 Fallback 활성화 (기본값: true)
 *
 * @returns { prices, isConnected, error, currentMethod }
 */
export function useStreamingPrices({
  symbols,
  baseCurrency = 'KRW',
  method = 'SSE',
  autoFallback = true,
}: UseStreamingPricesOptions) {
  const [prices, setPrices] = useState<Map<string, CryptoPriceDto>>(new Map());
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentMethod, setCurrentMethod] = useState<StreamingMethod>(method);

  const adapterRef = useRef<StreamingAdapter | null>(null);

  useEffect(() => {
    if (symbols.length === 0) return;

    // Adapter 생성 (Factory Pattern)
    const createAdapter = (method: StreamingMethod): StreamingAdapter => {
      switch (method) {
        case 'SSE':
          return new SSEAdapter();
        case 'LongPolling':
          return new LongPollingAdapter();
      }
    };

    const connectWithFallback = async (method: StreamingMethod) => {
      const adapter = createAdapter(method);
      adapterRef.current = adapter;
      setCurrentMethod(method);

      adapter.onMessage((price) => {
        setPrices((prev) => {
          const newPrices = new Map(prev);
          newPrices.set(price.symbol, price);
          return newPrices;
        });
      });

      adapter.onConnectionChange((connected) => {
        setIsConnected(connected);

        // 자동 Fallback 로직
        if (!connected && autoFallback && method === 'SSE') {
          console.warn('SSE 연결 실패. Long Polling으로 Fallback');
          setTimeout(() => connectWithFallback('LongPolling'), 3000);
        }
      });

      adapter.onError((error) => {
        setError(error.message);
      });

      adapter.connect(symbols, baseCurrency);
    };

    connectWithFallback(method);

    // Cleanup
    return () => {
      if (adapterRef.current) {
        adapterRef.current.disconnect();
      }
    };
  }, [symbols, baseCurrency, method, autoFallback]);

  return { prices, isConnected, error, currentMethod };
}
