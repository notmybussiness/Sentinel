package com.pjsent.sentinel.crypto.controller;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.streaming.StreamingService;
import com.pjsent.sentinel.crypto.streaming.WebSocketMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 암호화폐 실시간 스트리밍 통합 컨트롤러
 *
 * Adapter Pattern을 사용하여 런타임에 스트리밍 방식 전환 가능:
 * - SSE (Server-Sent Events) - 권장 ✅
 * - WebSocket - 고성능 ⚡
 * - LongPolling - Fallback 🔄
 *
 * 사용 예시:
 * - SSE: GET /api/v1/crypto/stream/prices?symbols=BTC,ETH&method=SSE
 * - WebSocket: GET
 * /api/v1/crypto/stream/prices?symbols=BTC,ETH&method=WebSocket
 * - LongPolling: GET
 * /api/v1/crypto/stream/prices?symbols=BTC,ETH&method=LongPolling
 */
@RestController
@RequestMapping("/api/v1/crypto/stream")
@Tag(name = "Crypto Streaming", description = "암호화폐 실시간 스트리밍 API")
@Slf4j
@RequiredArgsConstructor
public class CryptoStreamController {

        // Maximum connection duration to prevent zombie connections (5 minutes)
        private static final Duration MAX_CONNECTION_DURATION = Duration.ofMinutes(5);
        // Heartbeat interval for keep-alive
        private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(10);

        // Spring이 자동으로 모든 StreamingService 구현체를 주입
        private final List<StreamingService> streamingServices;
        private final WebSocketMetrics metrics;

        /**
         * 실시간 가격 스트리밍 (SSE)
         *
         * @param symbols      암호화폐 심볼 (쉼표로 구분, 예: BTC,ETH,XRP)
         * @param baseCurrency 기준 통화 (기본값: KRW)
         * @param method       스트리밍 방식 (SSE, WebSocket, LongPolling, 기본값: SSE)
         * @return 실시간 가격 데이터 스트림 (ServerSentEvent)
         */
        @GetMapping(value = "/prices", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        @Operation(summary = "실시간 가격 스트리밍", description = "SSE를 통해 실시간 암호화폐 가격을 스트리밍합니다. method 파라미터로 방식 선택 가능.")
        public Flux<ServerSentEvent<CryptoPriceDto>> streamPrices(
                        @Parameter(description = "암호화폐 심볼 (쉼표로 구분)", example = "BTC,ETH,XRP") @RequestParam String symbols,

                        @Parameter(description = "기준 통화", example = "KRW") @RequestParam(defaultValue = "KRW") String baseCurrency,

                        @Parameter(description = "스트리밍 방식 (SSE, WebSocket, LongPolling)", example = "SSE") @RequestParam(defaultValue = "SSE") String method) {
                log.info("🔵 스트리밍 요청 수신. 심볼: {}, 기준 통화: {}, 방식: {}", symbols, baseCurrency, method);

                // 심볼 목록 파싱
                List<String> symbolList = Arrays.stream(symbols.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());

                if (symbolList.isEmpty()) {
                        log.warn("⚠️ 심볼 목록이 비어있음");
                        return Flux.error(new IllegalArgumentException("심볼은 필수입니다."));
                }

                // StreamingService 선택 (Adapter Pattern)
                StreamingService service = findStreamingService(method);

                log.info("✅ {} 스트리밍 시작. 심볼: {}, 기준 통화: {}", service.getStreamingMethod(), symbolList, baseCurrency);

                // Connection lifecycle tracking
                final long connectionStartTime = System.currentTimeMillis();

                // 1. Price Stream (Upstream)
                Flux<ServerSentEvent<CryptoPriceDto>> priceStream = service.startStreaming(symbolList, baseCurrency)
                                .timeout(Duration.ofSeconds(60)) // Upstream Timeout (60s)
                                .onBackpressureDrop(dropped -> {
                                        log.debug("⚠️ Backpressure: 데이터 drop - {}", dropped.getSymbol());
                                        metrics.recordMessageDropped();
                                })
                                .map(price -> ServerSentEvent.<CryptoPriceDto>builder()
                                                .event("price-update")
                                                .data(price)
                                                .build());

                // 2. Heartbeat Stream - keep-alive for zombie connection detection
                Flux<ServerSentEvent<CryptoPriceDto>> heartbeatStream = Flux.interval(HEARTBEAT_INTERVAL)
                                .map(tick -> ServerSentEvent.<CryptoPriceDto>builder()
                                                .event("heartbeat")
                                                .comment("keep-alive:" + tick)
                                                .build());

                // 3. Merge & Apply connection lifecycle management
                return Flux.merge(priceStream, heartbeatStream)
                                // Max connection duration to prevent eternal zombie connections
                                .take(MAX_CONNECTION_DURATION)
                                // Track connection start
                                .doOnSubscribe(sub -> {
                                        metrics.recordConnectionStart();
                                        log.info("📡 {} 구독 시작. 클라이언트 연결됨. Active: {}",
                                                method, metrics.getActiveConnections().get());
                                })
                                // Track connection end with duration
                                .doFinally(signal -> {
                                        long durationMs = System.currentTimeMillis() - connectionStartTime;
                                        metrics.recordConnectionEnd(durationMs);
                                        log.info("🔴 {} 구독 종료. Signal: {}, Duration: {}ms, Active: {}",
                                                method, signal, durationMs, metrics.getActiveConnections().get());
                                })
                                // Error handling
                                .doOnError(error -> {
                                        metrics.recordError();
                                        log.error("❌ {} 스트리밍 오류: {}", method, error.getMessage());
                                })
                                // Cancel handling for client disconnect
                                .doOnCancel(() -> {
                                        log.info("🔌 {} 클라이언트 연결 해제 감지", method);
                                });
        }

        /**
         * 사용 가능한 스트리밍 방식 조회
         *
         * @return 스트리밍 방식 목록 및 상세 정보
         */
        @GetMapping("/methods")
        @Operation(summary = "스트리밍 방식 목록 조회", description = "사용 가능한 스트리밍 방식과 상세 정보를 반환합니다.")
        public Map<String, Object> getAvailableMethods() {
                log.info("📋 스트리밍 방식 조회 요청");

                List<Map<String, Object>> methods = streamingServices.stream()
                                .map(service -> {
                                        Map<String, Object> map = Map.of(
                                                        "name", service.getStreamingMethod(),
                                                        "available", service.isAvailable(),
                                                        "useCase", service.getRecommendedUseCase(),
                                                        "updateInterval", service.getUpdateIntervalMs() + "ms",
                                                        "latency", service.getExpectedLatencyMs() + "ms");
                                        return (Map<String, Object>) map;
                                })
                                .collect(Collectors.toList());

                return Map.of(
                                "methods", methods,
                                "recommended", "SSE",
                                "count", methods.size());
        }

        /**
         * 스트리밍 서비스 상태 확인
         *
         * @return 서비스 상태 정보
         */
        @GetMapping("/status")
        @Operation(summary = "스트리밍 서비스 상태", description = "모든 스트리밍 서비스의 상태를 확인합니다.")
        public Map<String, Object> getStatus() {
                log.info("🔍 스트리밍 서비스 상태 확인");

                long availableCount = streamingServices.stream()
                                .filter(StreamingService::isAvailable)
                                .count();

                return Map.of(
                                "totalServices", streamingServices.size(),
                                "availableServices", availableCount,
                                "services", streamingServices.stream()
                                                .map(service -> Map.of(
                                                                "name", service.getStreamingMethod(),
                                                                "status", service.isAvailable() ? "UP" : "DOWN"))
                                                .collect(Collectors.toList()),
                                "metrics", metrics.getStats());
        }

        /**
         * StreamingService 선택 (Adapter Pattern)
         *
         * @param method 요청된 방식 (SSE, WebSocket, LongPolling)
         * @return 선택된 StreamingService
         */
        private StreamingService findStreamingService(String method) {
                // 1. 요청된 방식 찾기
                StreamingService requestedService = streamingServices.stream()
                                .filter(service -> service.getStreamingMethod().equalsIgnoreCase(method))
                                .filter(StreamingService::isAvailable)
                                .findFirst()
                                .orElse(null);

                if (requestedService != null) {
                        log.info("✅ 요청된 방식 사용: {}", method);
                        return requestedService;
                }

                // 2. Fallback: SSE 찾기
                log.warn("⚠️ 방식 {}를 찾을 수 없음. SSE로 Fallback", method);
                StreamingService sseService = streamingServices.stream()
                                .filter(service -> "SSE".equalsIgnoreCase(service.getStreamingMethod()))
                                .filter(StreamingService::isAvailable)
                                .findFirst()
                                .orElse(null);

                if (sseService != null) {
                        return sseService;
                }

                // 3. 마지막 Fallback: 사용 가능한 아무 서비스나
                log.warn("⚠️ SSE도 없음. 사용 가능한 첫 번째 서비스 사용");
                return streamingServices.stream()
                                .filter(StreamingService::isAvailable)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("사용 가능한 스트리밍 서비스가 없습니다."));
        }
}
