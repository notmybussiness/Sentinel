package com.pjsent.sentinel.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Fallback 서비스
 * 
 * Circuit Breaker가 Open 상태일 때 캐시된 데이터를 반환하거나
 * 적절한 에러 응답을 제공합니다.
 * 
 * 전략: Cache-First → TTL 만료 시 Error
 */
@Service
@ConditionalOnBean(RedisTemplate.class)
@RequiredArgsConstructor
@Slf4j
public class FallbackService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String FALLBACK_CACHE_PREFIX = "fallback:";
    private static final long DEFAULT_CACHE_TTL_SECONDS = 300; // 5분

    /**
     * 캐시된 응답이 있으면 반환, 없으면 에러 응답
     * 
     * @param endpoint    요청 엔드포인트 (캐시 키로 사용)
     * @param serviceName 서비스 이름 (로깅용)
     * @return 캐시된 응답 또는 503 에러
     */
    public ResponseEntity<?> fallbackWithCache(String endpoint, String serviceName) {
        String cacheKey = FALLBACK_CACHE_PREFIX + endpoint;
        Object cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("Fallback: returning cached response for {} (service: {})", endpoint, serviceName);
            return ResponseEntity.ok()
                    .header("X-Fallback", "cache")
                    .header("X-Fallback-Service", serviceName)
                    .body(cached);
        }

        log.warn("Fallback: no cache available for {} (service: {})", endpoint, serviceName);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("X-Fallback", "error")
                .header("X-Fallback-Service", serviceName)
                .body(Map.of(
                        "error", "Service temporarily unavailable",
                        "service", serviceName,
                        "message", "The service is experiencing issues. Please try again later.",
                        "retry_after", 30));
    }

    /**
     * API 서비스 fallback
     */
    public ResponseEntity<?> apiFallback(String endpoint) {
        return fallbackWithCache(endpoint, "api");
    }

    /**
     * RAG 서비스 fallback
     */
    public ResponseEntity<?> ragFallback(String endpoint) {
        log.warn("RAG service fallback triggered for: {}", endpoint);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("X-Fallback", "error")
                .header("X-Fallback-Service", "rag")
                .body(Map.of(
                        "error", "AI service is busy",
                        "service", "rag",
                        "message", "The AI service is currently unavailable. Please try again later.",
                        "retry_after", 60));
    }

    /**
     * 성공 응답을 캐시에 저장 (다음 fallback 시 사용)
     * 
     * @param endpoint   엔드포인트
     * @param response   저장할 응답
     * @param ttlSeconds TTL (초)
     */
    public void cacheSuccessResponse(String endpoint, Object response, long ttlSeconds) {
        String cacheKey = FALLBACK_CACHE_PREFIX + endpoint;
        redisTemplate.opsForValue().set(cacheKey, response, ttlSeconds, TimeUnit.SECONDS);
        log.debug("Cached response for fallback: {} (TTL: {}s)", endpoint, ttlSeconds);
    }

    /**
     * 기본 TTL로 성공 응답 캐시
     */
    public void cacheSuccessResponse(String endpoint, Object response) {
        cacheSuccessResponse(endpoint, response, DEFAULT_CACHE_TTL_SECONDS);
    }

    /**
     * Market 데이터 fallback (캐시 우선)
     * 시장 데이터는 조금 오래되어도 괜찮음
     */
    public ResponseEntity<?> marketFallback(String endpoint) {
        String cacheKey = FALLBACK_CACHE_PREFIX + "market:" + endpoint;
        Object cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("Market fallback: returning stale data for {}", endpoint);
            return ResponseEntity.ok()
                    .header("X-Fallback", "stale-cache")
                    .header("X-Data-Age", "stale")
                    .body(cached);
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Market data temporarily unavailable",
                        "message", "Unable to fetch real-time market data. Please try again.",
                        "retry_after", 30));
    }

    /**
     * Portfolio fallback (캐시 시도 후 에러)
     */
    public ResponseEntity<?> portfolioFallback(String endpoint, String userId) {
        String cacheKey = FALLBACK_CACHE_PREFIX + "portfolio:" + userId + ":" + endpoint;
        Object cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("Portfolio fallback: returning cached data for user {}", userId);
            return ResponseEntity.ok()
                    .header("X-Fallback", "cache")
                    .body(cached);
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Portfolio service temporarily unavailable",
                        "message", "Unable to load portfolio data. Please try again.",
                        "retry_after", 15));
    }
}
