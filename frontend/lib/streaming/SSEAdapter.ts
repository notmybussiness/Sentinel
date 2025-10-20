import { StreamingAdapter } from './StreamingAdapter';
import { CryptoPriceDto } from '@/types';

/**
 * SSE (Server-Sent Events) Adapter
 * 가장 권장되는 방식: 간단, 자동 재연결, HTTP 기반
 */
export class SSEAdapter implements StreamingAdapter {
  private eventSource: EventSource | null = null;
  private messageCallback: ((price: CryptoPriceDto) => void) | null = null;
  private connectionCallback: ((connected: boolean) => void) | null = null;
  private errorCallback: ((error: Error) => void) | null = null;

  connect(symbols: string[], baseCurrency: string): void {
    const symbolsParam = symbols.join(',');
    const url = `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/crypto/stream/prices?symbols=${symbolsParam}&baseCurrency=${baseCurrency}&method=SSE`;

    console.log('✅ SSE 연결 시작:', url);

    this.eventSource = new EventSource(url);

    this.eventSource.onopen = () => {
      console.log('✅ SSE 연결 성공');
      this.connectionCallback?.(true);
    };

    this.eventSource.addEventListener('price-update', (event) => {
      try {
        const price: CryptoPriceDto = JSON.parse(event.data);
        this.messageCallback?.(price);
      } catch (error) {
        console.error('SSE 메시지 파싱 실패:', error);
      }
    });

    this.eventSource.onerror = (error) => {
      console.error('❌ SSE 연결 오류:', error);
      this.connectionCallback?.(false);
      this.errorCallback?.(new Error('SSE 연결 실패'));
      this.eventSource?.close();
    };
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      console.log('🔌 SSE 연결 종료');
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
    return 'SSE';
  }

  getUpdateInterval(): number {
    return 1000; // 1초
  }
}
