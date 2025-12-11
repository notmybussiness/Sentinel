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
 * SSE (Server-Sent Events) 스트리밍 구현
 *
 * 장점:
 * - 간단한 구현 (EventSource API)
 * - 자동 재연결 (브라우저 처리)
 * - HTTP 기반 (프록시 호환성 우수)
 * - 디버깅 쉬움 (Chrome DevTools)
 *
 * 단점:
 * - 단방향 (Server → Client만 가능)
 * - Text only (Binary 불가)
 *
 * 권장 사용 사례:
 * - 실시간 가격 업데이트
 * - 실시간 대시보드
 * - 실시간 알림
 */
@Service("sseStreamingService")
@Slf4j
@RequiredArgsConstructor
public class SSEStreamingService implements StreamingService {

    private final CryptoDataService cryptoDataService;

    @Override
    public Flux<CryptoPriceDto> startStreaming(List<String> symbols, String baseCurrency) {
        log.info("✅ SSE 스트리밍 시작. 심볼: {}, 기준 통화: {}", symbols, baseCurrency);

        return Flux.interval(Duration.ofSeconds(1)) // 1초마다 업데이트
                .onBackpressureDrop() // 백프레셔 처리: 과부하 시 이전 데이터 버림
                .flatMap(tick -> {
                    try {
                        // 배치 가격 조회 (CryptoDataService 사용)
                        List<CryptoPriceDto> prices = cryptoDataService.getBatchCryptoPrices(symbols, baseCurrency);

                        if (prices == null || prices.isEmpty()) {
                            log.warn("SSE: 가격 데이터가 비어있음. Tick: {}", tick);
                            return Flux.empty();
                        }

                        log.debug("SSE: {} 개 가격 업데이트. Tick: {}", prices.size(), tick);
                        return Flux.fromIterable(prices);

                    } catch (Exception e) {
                        log.error("SSE: 가격 조회 실패. Tick: {}, 오류: {}", tick, e.getMessage());
                        return Flux.empty(); // 에러 시 빈 스트림 반환
                    }
                })
                .filter(Objects::nonNull)
                .doOnSubscribe(sub -> log.info("SSE 구독 시작. 심볼: {}", symbols))
                .doOnCancel(() -> log.info("SSE 구독 취소. 심볼: {}", symbols))
                .doOnComplete(() -> log.info("SSE 구독 완료. 심볼: {}", symbols))
                .doOnError(error -> log.error("SSE 오류: {}", error.getMessage(), error));
    }

    @Override
    public String getStreamingMethod() {
        return "SSE";
    }

    @Override
    public boolean isAvailable() {
        return true; // SSE는 항상 사용 가능
    }

    @Override
    public String getRecommendedUseCase() {
        return "실시간 가격 업데이트, 대시보드 (권장 ✅)";
    }

    @Override
    public long getUpdateIntervalMs() {
        return 1000; // 1초
    }

    @Override
    public long getExpectedLatencyMs() {
        return 50; // 평균 50ms
    }
}
