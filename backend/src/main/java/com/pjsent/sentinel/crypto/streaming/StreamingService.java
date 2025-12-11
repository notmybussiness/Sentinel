package com.pjsent.sentinel.crypto.streaming;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 실시간 스트리밍 서비스 인터페이스
 * WebSocket, SSE, Long Polling을 추상화하여 런타임에 전환 가능
 *
 * Adapter Pattern을 사용하여 다양한 스트리밍 방식을 동일한 인터페이스로 제공
 */
public interface StreamingService {

    /**
     * 실시간 가격 스트리밍 시작
     *
     * @param symbols 암호화폐 심볼 목록 (예: ["BTC", "ETH", "XRP"])
     * @param baseCurrency 기준 통화 (예: "KRW", "USDT")
     * @return 실시간 가격 데이터 스트림 (Reactive Streams)
     */
    Flux<CryptoPriceDto> startStreaming(List<String> symbols, String baseCurrency);

    /**
     * 스트리밍 방식 이름
     *
     * @return "SSE", "WebSocket", "LongPolling"
     */
    String getStreamingMethod();

    /**
     * 스트리밍 서비스 사용 가능 여부
     *
     * @return true if available
     */
    boolean isAvailable();

    /**
     * 권장 사용 사례
     *
     * @return 사용 사례 설명
     */
    default String getRecommendedUseCase() {
        return "General purpose streaming";
    }

    /**
     * 업데이트 주기 (밀리초)
     *
     * @return 업데이트 간격 (ms)
     */
    default long getUpdateIntervalMs() {
        return 1000; // 기본 1초
    }

    /**
     * 예상 레이턴시 (밀리초)
     *
     * @return 평균 레이턴시 (ms)
     */
    default long getExpectedLatencyMs() {
        return 50; // 기본 50ms
    }
}
