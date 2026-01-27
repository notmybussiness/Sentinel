package com.pjsent.sentinel.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting 및 Circuit Breaker 설정
 * 
 * Rate Limits (DAU 100 기준):
 * - API 일반: 50 req/sec, burst 100
 * - Auth: 10 req/min (브루트포스 방지)
 * - RAG: 5 req/sec (AI 비용 고려)
 * - Streaming: 1 req/sec (WebSocket 연결)
 */
@Configuration
public class RateLimitConfig {

    /**
     * 일반 API Rate Limiter
     * 50 requests per second, refresh every second
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig apiConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(50)
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        RateLimiterConfig authConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        RateLimiterConfig ragConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofMillis(1000))
                .build();

        RateLimiterConfig streamConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        registry.rateLimiter("api", apiConfig);
        registry.rateLimiter("auth", authConfig);
        registry.rateLimiter("rag", ragConfig);
        registry.rateLimiter("stream", streamConfig);

        return registry;
    }

    /**
     * Circuit Breaker 설정
     * - Backend CB: 일반 API용
     * - RAG CB: AI 서비스용 (더 관대한 설정)
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig backendConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(80)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowSize(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();

        CircuitBreakerConfig ragConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(90)
                .slowCallDurationThreshold(Duration.ofSeconds(10)) // RAG는 더 오래 걸림
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(5)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("backend", backendConfig);
        registry.circuitBreaker("rag", ragConfig);

        return registry;
    }
}
