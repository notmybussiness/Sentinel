package com.pjsent.sentinel.crypto.streaming;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 스트리밍 구현
 *
 * 장점:
 * - 최고 성능 (레이턴시 <10ms)
 * - 양방향 통신 (Full-duplex)
 * - Binary 지원
 *
 * 단점:
 * - 복잡한 구현 (연결 관리, 재연결)
 * - 프록시 호환성 문제
 * - 디버깅 어려움
 *
 * 권장 사용 사례:
 * - 실시간 트레이딩
 * - 고빈도 업데이트 (<100ms)
 *
 * TODO: Upbit WebSocket 실제 연결 구현 (Phase 2)
 *       현재는 polling 방식으로 동작 (SSE와 유사)
 */
@Service("webSocketStreamingService")
@Slf4j
@RequiredArgsConstructor
public class WebSocketStreamingService implements StreamingService {

    private final CryptoDataService cryptoDataService;

    @Value("${crypto.market.upbit.websocket-url:wss://api.upbit.com/websocket/v1}")
    private String websocketUrl;

    @Value("${crypto.streaming.websocket.enabled:false}")
    private boolean enabled;

    // WebSocket에서 수신한 실시간 가격 캐시
    // TODO: 실제 WebSocket 연결 시 여기에 데이터 저장
    private final Map<String, CryptoPriceDto> priceCache = new ConcurrentHashMap<>();

    @Override
    public Flux<CryptoPriceDto> startStreaming(List<String> symbols, String baseCurrency) {
        log.info("⚡ WebSocket 스트리밍 시작 (현재: Polling 방식). 심볼: {}, 기준 통화: {}", symbols, baseCurrency);

        if (!enabled) {
            log.warn("WebSocket이 비활성화되어 있습니다. SSE 사용을 권장합니다.");
        }

        // TODO: 실제 Upbit WebSocket 연결
        // subscribeToUpbitWebSocket(symbols, baseCurrency);

        // 현재는 polling 방식으로 동작 (빠른 업데이트: 500ms)
        return Flux.interval(Duration.ofMillis(500)) // 500ms마다 (SSE보다 2배 빠름)
                .onBackpressureDrop()
                .flatMap(tick -> {
                    try {
                        // 배치 가격 조회
                        List<CryptoPriceDto> prices = cryptoDataService.getBatchCryptoPrices(symbols, baseCurrency);

                        if (prices == null || prices.isEmpty()) {
                            log.warn("WebSocket: 가격 데이터가 비어있음. Tick: {}", tick);
                            return Flux.empty();
                        }

                        // 캐시 업데이트 (나중에 실제 WebSocket 구현 시 사용)
                        prices.forEach(price -> {
                            String key = price.getBaseCurrency() + "-" + price.getSymbol();
                            priceCache.put(key, price);
                        });

                        log.debug("WebSocket: {} 개 가격 업데이트. Tick: {}", prices.size(), tick);
                        return Flux.fromIterable(prices);

                    } catch (Exception e) {
                        log.error("WebSocket: 가격 조회 실패. Tick: {}, 오류: {}", tick, e.getMessage());
                        return Flux.empty();
                    }
                })
                .filter(Objects::nonNull)
                .doOnSubscribe(sub -> log.info("WebSocket 구독 시작. 심볼: {}", symbols))
                .doOnCancel(() -> {
                    log.info("WebSocket 구독 취소. 심볼: {}", symbols);
                    // TODO: WebSocket 연결 해제
                })
                .doOnComplete(() -> log.info("WebSocket 구독 완료. 심볼: {}", symbols))
                .doOnError(error -> log.error("WebSocket 오류: {}", error.getMessage(), error));
    }

    /**
     * 캐시에서 가격 조회 (실제 WebSocket 구현 시 사용)
     */
    public CryptoPriceDto getCachedPrice(String symbol, String baseCurrency) {
        String key = baseCurrency + "-" + symbol;
        return priceCache.get(key);
    }

    /**
     * TODO: Upbit WebSocket 구독
     * Phase 2에서 구현 예정
     */
    private void subscribeToUpbitWebSocket(List<String> symbols, String baseCurrency) {
        log.warn("TODO: Upbit WebSocket 구독 구현 필요. URL: {}", websocketUrl);
        // 실제 구현 시:
        // 1. WebSocketClient 생성
        // 2. WebSocket 연결
        // 3. 구독 메시지 전송 (JSON)
        // 4. Ticker 메시지 수신
        // 5. priceCache 업데이트
    }

    @Override
    public String getStreamingMethod() {
        return "WebSocket";
    }

    @Override
    public boolean isAvailable() {
        return enabled; // application.yml에서 설정
    }

    @Override
    public String getRecommendedUseCase() {
        return "실시간 트레이딩, 고빈도 업데이트 (<100ms) ⚡";
    }

    @Override
    public long getUpdateIntervalMs() {
        return 500; // 500ms (SSE보다 2배 빠름)
    }

    @Override
    public long getExpectedLatencyMs() {
        return 10; // 평균 10ms (실제 WebSocket 연결 시)
    }
}
