import { StreamingAdapter } from './StreamingAdapter';
import { CryptoPriceDto } from '@/types';
import { post } from '@/lib/api/client';

/**
 * Long Polling Adapter
 * Fallback 용도: 모든 브라우저 지원, 간단한 구현
 */
export class LongPollingAdapter implements StreamingAdapter {
  private intervalId: NodeJS.Timeout | null = null;
  private messageCallback: ((price: CryptoPriceDto) => void) | null = null;
  private connectionCallback: ((connected: boolean) => void) | null = null;
  private errorCallback: ((error: Error) => void) | null = null;

  connect(symbols: string[], baseCurrency: string): void {
    console.log('🔄 Long Polling 시작');

    this.connectionCallback?.(true);

    // 2초마다 API 호출
    this.intervalId = setInterval(async () => {
      try {
        const prices = await post<CryptoPriceDto[]>('/api/v1/crypto/prices', {
          symbols,
          baseCurrency,
        });

        prices.forEach((price) => this.messageCallback?.(price));

      } catch (error) {
        console.error('❌ Long Polling 실패:', error);
        this.connectionCallback?.(false);
        this.errorCallback?.(error as Error);
      }
    }, 2000);
  }

  disconnect(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
      this.connectionCallback?.(false);
      console.log('🔌 Long Polling 종료');
    }
  }

  onMessage(callback: (price: CryptoPriceDto) => void): void {
    this.messageCallback = callback;
  }

  onConnectionChange(callback: (connected: boolean) => void): void {
    this.connectionCallback = callback;
  }

  onError(callback: (error: Error) => void): void {
    this.errorCallback = callback;
  }

  getName(): string {
    return 'LongPolling';
  }

  getUpdateInterval(): number {
    return 2000; // 2초
  }
}
