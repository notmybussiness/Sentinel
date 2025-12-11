package com.pjsent.sentinel.crypto.streaming;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ========================================================================
 * WebSocket Streaming Service (Real Upbit Integration)
 * ========================================================================
 * 
 * [역할]
 * - Upbit WebSocket을 통한 실시간 암호화폐 가격 스트리밍 (Upstream)
 * - 클라이언트에게 Flux<CryptoPriceDto>로 실시간 데이터 제공
 * 
 * [구조]
 * Upbit WebSocket ──► UpbitWebSocketClient ──► 이 Service ──► Controller ──►
 * Client
 * 
 * [전략]
 * - Upstream은 WebSocket만 사용 (Polling Fallback 불필요)
 * - 연결 실패 시: Retry (지수 백오프) → 최종 실패 시 캐시 값 반환
 * 
 * [장점]
 * - 최고 성능 (레이턴시 <10ms)
 * - 양방향 통신 (Full-duplex)
 * - 실시간 이벤트 기반
 * 
 * [권장 사용 사례]
 * - 실시간 트레이딩
 * - 고빈도 업데이트 (<100ms)
 */
@Service("webSocketStreamingService")
@Slf4j
@RequiredArgsConstructor
public class WebSocketStreamingService implements StreamingService {

    private final UpbitWebSocketClient upbitWebSocketClient;

    @Value("${crypto.streaming.websocket.enabled:true}")
    private boolean enabled;

    // WebSocket에서 수신한 실시간 가격 캐시
    private final Map<String, CryptoPriceDto> priceCache = new ConcurrentHashMap<>();

    // ========================================================================
    // Main Streaming Method
    // ========================================================================

    @Override
    public Flux<CryptoPriceDto> startStreaming(List<String> symbols, String baseCurrency) {
        log.info("⚡ WebSocket 스트리밍 시작. 심볼: {}, 기준 통화: {}", symbols, baseCurrency);

        if (!enabled) {
            log.warn("⚠️ WebSocket이 비활성화되어 있습니다. 캐시된 데이터 반환.");
            return getCachedPricesAsFlux(symbols, baseCurrency);
        }

        // 🔥 실제 Upbit WebSocket 연결!
        return upbitWebSocketClient.connect(symbols)
                .doOnNext(this::updateCache)
                .doOnSubscribe(sub -> log.info("🔌 Upbit WebSocket 구독 시작. 심볼: {}", symbols))
                .doOnCancel(() -> log.info("🔌 Upbit WebSocket 구독 취소. 심볼: {}", symbols))
                .doOnComplete(() -> log.info("🔌 Upbit WebSocket 스트림 완료. 심볼: {}", symbols))
                .doOnError(error -> log.error("❌ Upbit WebSocket 오류: {}", error.getMessage()))
                // 에러 시 재연결 (최대 5회, 지수 백오프)
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(signal -> log.warn("🔄 WebSocket 재연결 시도 #{}", signal.totalRetries() + 1)))
                // 최종 실패 시 캐시 값 반환 (Graceful Degradation)
                .onErrorResume(error -> {
                    log.error("❌ WebSocket 완전 실패. 캐시 값 반환: {}", error.getMessage());
                    return getCachedPricesAsFlux(symbols, baseCurrency);
                });
    }

    // ========================================================================
    // Cache Management
    // ========================================================================

    /**
     * 가격 캐시 업데이트
     */
    private void updateCache(CryptoPriceDto price) {
        if (price != null && price.getSymbol() != null) {
            String key = price.getBaseCurrency() + "-" + price.getSymbol();
            priceCache.put(key, price);
        }
    }

    /**
     * 캐시에서 가격 조회 (단일)
     */
    public CryptoPriceDto getCachedPrice(String symbol, String baseCurrency) {
        String key = baseCurrency + "-" + symbol;
        return priceCache.get(key);
    }

    /**
     * 캐시에서 복수 심볼의 가격을 Flux로 반환 (Fallback용)
     */
    private Flux<CryptoPriceDto> getCachedPricesAsFlux(List<String> symbols, String baseCurrency) {
        return Flux.fromIterable(symbols)
                .map(symbol -> getCachedPrice(symbol, baseCurrency))
                .filter(price -> price != null);
    }

    // ========================================================================
    // StreamingService Interface Implementation
    // ========================================================================

    @Override
    public String getStreamingMethod() {
        return "WebSocket";
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public String getRecommendedUseCase() {
        return "실시간 트레이딩, 고빈도 업데이트 (<100ms) ⚡";
    }

    @Override
    public long getUpdateIntervalMs() {
        return 0; // WebSocket은 이벤트 기반이므로 interval 없음
    }

    @Override
    public long getExpectedLatencyMs() {
        return 10; // 평균 10ms
    }
}
