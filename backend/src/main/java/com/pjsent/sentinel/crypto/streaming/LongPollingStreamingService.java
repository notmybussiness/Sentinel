package com.pjsent.sentinel.crypto.streaming;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Long Polling 스트리밍 구현
 *
 * 장점:
 * - 모든 브라우저 지원 (IE6도 가능!)
 * - 매우 간단한 구현
 * - Fallback 최적
 *
 * 단점:
 * - 높은 레이턴시 (>100ms)
 * - 서버 부하 (매번 HTTP 요청)
 * - 비효율적 (HTTP 헤더 오버헤드)
 *
 * 권장 사용 사례:
 * - 레거시 브라우저 지원
 * - SSE/WebSocket Fallback
 * - 실시간성이 덜 중요한 경우
 */
@Service("longPollingStreamingService")
@Slf4j
@RequiredArgsConstructor
public class LongPollingStreamingService implements StreamingService {

    private final CryptoDataService cryptoDataService;

    @Override
    public Flux<CryptoPriceDto> startStreaming(List<String> symbols, String baseCurrency) {
        log.warn("🔄 Long Polling 스트리밍 시작 (Fallback 모드). 심볼: {}, 기준 통화: {}", symbols, baseCurrency);
        log.warn("⚠️ Long Polling은 비효율적입니다. SSE 또는 WebSocket 사용을 권장합니다.");

        return Flux.interval(Duration.ofSeconds(2)) // 2초마다 (SSE보다 2배 느림)
                .onBackpressureDrop()
                .flatMap(tick -> {
                    try {
                        // 배치 가격 조회
                        List<CryptoPriceDto> prices = cryptoDataService.getBatchCryptoPrices(symbols, baseCurrency);

                        if (prices == null || prices.isEmpty()) {
                            log.warn("Long Polling: 가격 데이터가 비어있음. Tick: {}", tick);
                            return Flux.empty();
                        }

                        log.debug("Long Polling: {} 개 가격 업데이트. Tick: {}", prices.size(), tick);
                        return Flux.fromIterable(prices);

                    } catch (Exception e) {
                        log.error("Long Polling: 가격 조회 실패. Tick: {}, 오류: {}", tick, e.getMessage());
                        return Flux.empty();
                    }
                })
                .filter(Objects::nonNull)
                .doOnSubscribe(sub -> log.info("Long Polling 구독 시작. 심볼: {}", symbols))
                .doOnCancel(() -> log.info("Long Polling 구독 취소. 심볼: {}", symbols))
                .doOnComplete(() -> log.info("Long Polling 구독 완료. 심볼: {}", symbols))
                .doOnError(error -> log.error("Long Polling 오류: {}", error.getMessage(), error));
    }

    @Override
    public String getStreamingMethod() {
        return "LongPolling";
    }

    @Override
    public boolean isAvailable() {
        return true; // Fallback으로 항상 사용 가능
    }

    @Override
    public String getRecommendedUseCase() {
        return "레거시 브라우저 지원, Fallback 용도 🔄";
    }

    @Override
    public long getUpdateIntervalMs() {
        return 2000; // 2초 (가장 느림)
    }

    @Override
    public long getExpectedLatencyMs() {
        return 200; // 평균 200ms
    }
}
