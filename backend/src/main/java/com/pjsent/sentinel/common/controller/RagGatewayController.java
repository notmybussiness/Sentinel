package com.pjsent.sentinel.common.controller;

import com.pjsent.sentinel.common.config.RagServiceConfig;
import com.pjsent.sentinel.common.service.FallbackService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * RAG 서비스 Gateway Controller
 * 
 * Python RAG 서비스로 요청을 프록시하며:
 * - Circuit Breaker로 장애 전파 방지
 * - Rate Limiting으로 과부하 방지
 * - Fallback으로 장애 시 적절한 응답 제공
 */
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
@Slf4j
public class RagGatewayController {

    private final RagServiceConfig ragConfig;
    private final FallbackService fallbackService;
    private final WebClient.Builder webClientBuilder;

    /**
     * RAG 질의 엔드포인트
     * POST /api/v1/rag/query
     */
    @PostMapping("/query")
    @CircuitBreaker(name = "rag", fallbackMethod = "queryFallback")
    @RateLimiter(name = "rag")
    public Mono<ResponseEntity<Object>> query(@RequestBody Map<String, Object> request) {
        if (!ragConfig.isEnabled()) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "RAG service is disabled")));
        }

        log.info("Proxying RAG query request");

        return webClientBuilder.build()
                .post()
                .uri(ragConfig.getUrl() + "/query")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .timeout(Duration.ofSeconds(ragConfig.getTimeout()))
                .map(response -> ResponseEntity.ok().body(response))
                .doOnSuccess(r -> log.info("RAG query completed successfully"))
                .doOnError(e -> log.error("RAG query failed: {}", e.getMessage()));
    }

    /**
     * RAG 채팅 엔드포인트
     * POST /api/v1/rag/chat
     */
    @PostMapping("/chat")
    @CircuitBreaker(name = "rag", fallbackMethod = "chatFallback")
    @RateLimiter(name = "rag")
    public Mono<ResponseEntity<Object>> chat(@RequestBody Map<String, Object> request) {
        if (!ragConfig.isEnabled()) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "RAG service is disabled")));
        }

        log.info("Proxying RAG chat request");

        return webClientBuilder.build()
                .post()
                .uri(ragConfig.getUrl() + "/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .timeout(Duration.ofSeconds(ragConfig.getTimeout()))
                .map(response -> ResponseEntity.ok().body(response))
                .doOnSuccess(r -> log.info("RAG chat completed successfully"));
    }

    /**
     * RAG 서비스 헬스체크
     * GET /api/v1/rag/health
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<Object>> health() {
        if (!ragConfig.isEnabled()) {
            return Mono.just(ResponseEntity.ok().body(Map.of(
                    "status", "disabled",
                    "message", "RAG service is disabled")));
        }

        return webClientBuilder.build()
                .get()
                .uri(ragConfig.getUrl() + ragConfig.getHealthPath())
                .retrieve()
                .bodyToMono(Object.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> ResponseEntity.ok().body(Map.of(
                        "status", "healthy",
                        "rag_service", response)))
                .onErrorReturn(ResponseEntity.ok().body(Map.of(
                        "status", "unhealthy",
                        "message", "RAG service is not responding")));
    }

    // ========== Fallback Methods ==========

    @SuppressWarnings("unused")
    private Mono<ResponseEntity<Object>> queryFallback(Map<String, Object> request, Throwable t) {
        log.warn("RAG query fallback triggered: {}", t.getMessage());
        ResponseEntity<?> fallbackResponse = fallbackService.ragFallback("/query");
        return Mono.just(ResponseEntity.status(fallbackResponse.getStatusCode())
                .body(fallbackResponse.getBody()));
    }

    @SuppressWarnings("unused")
    private Mono<ResponseEntity<Object>> chatFallback(Map<String, Object> request, Throwable t) {
        log.warn("RAG chat fallback triggered: {}", t.getMessage());
        ResponseEntity<?> fallbackResponse = fallbackService.ragFallback("/chat");
        return Mono.just(ResponseEntity.status(fallbackResponse.getStatusCode())
                .body(fallbackResponse.getBody()));
    }
}
