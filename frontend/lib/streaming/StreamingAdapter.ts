import { CryptoPriceDto } from '@/types';

/**
 * 스트리밍 어댑터 인터페이스
 * WebSocket, SSE, Long Polling을 추상화
 *
 * Adapter Pattern을 사용하여 다양한 스트리밍 방식을 동일한 인터페이스로 제공
 */
export interface StreamingAdapter {
  /**
   * 스트리밍 연결
   * @param symbols 암호화폐 심볼 목록
   * @param baseCurrency 기준 통화
   */
  connect(symbols: string[], baseCurrency: string): void;

  /**
   * 연결 해제
   */
  disconnect(): void;

  /**
   * 메시지 수신 콜백 등록
   * @param callback 가격 업데이트 시 호출될 함수
   */
  onMessage(callback: (price: CryptoPriceDto) => void): void;

  /**
   * 연결 상태 변화 콜백
   * @param callback 연결 상태 변경 시 호출될 함수
   */
  onConnectionChange(callback: (connected: boolean) => void): void;

  /**
   * 에러 콜백
   * @param callback 에러 발생 시 호출될 함수
   */
  onError(callback: (error: Error) => void): void;

  /**
   * 어댑터 이름
   * @returns "SSE", "WebSocket", "LongPolling"
   */
  getName(): string;

  /**
   * 업데이트 주기 (밀리초)
   * @returns 업데이트 간격 (ms)
   */
  getUpdateInterval(): number;
}
