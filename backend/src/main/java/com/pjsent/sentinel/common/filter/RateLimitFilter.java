package com.pjsent.sentinel.common.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate Limiting Filter
 * 
 * 요청 경로에 따라 다른 Rate Limit 정책 적용:
 * - /api/v1/auth/** → auth (10 req/min)
 * - /rag/** → rag (5 req/sec)
 * - /api/v1/crypto/stream → stream (1 req/sec)
 * - 기타 /api/** → api (50 req/sec)
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimitFilter(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String rateLimiterName = determineRateLimiter(path);

        if (rateLimiterName == null) {
            // Rate limit 적용 대상이 아님
            filterChain.doFilter(request, response);
            return;
        }

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(rateLimiterName);

        try {
            RateLimiter.waitForPermission(rateLimiter);
            filterChain.doFilter(request, response);
        } catch (RequestNotPermitted e) {
            log.warn("Rate limit exceeded for {} on path: {} from IP: {}",
                    rateLimiterName, path, getClientIp(request));

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {
                        "error": "Too Many Requests",
                        "message": "Rate limit exceeded. Please slow down.",
                        "retry_after": %d
                    }
                    """.formatted(getRetryAfterSeconds(rateLimiterName)));
        }
    }

    /**
     * 경로에 따른 Rate Limiter 선택
     */
    private String determineRateLimiter(String path) {
        if (path.startsWith("/api/v1/auth/")) {
            return "auth";
        }
        if (path.startsWith("/rag/") || path.startsWith("/api/v1/rag/")) {
            return "rag";
        }
        if (path.contains("/stream") || path.contains("/ws")) {
            return "stream";
        }
        if (path.startsWith("/api/")) {
            return "api";
        }
        // Actuator, Swagger 등은 Rate Limit 미적용
        return null;
    }

    /**
     * Rate Limiter별 Retry-After 시간
     */
    private int getRetryAfterSeconds(String rateLimiterName) {
        return switch (rateLimiterName) {
            case "auth" -> 60; // 1분
            case "rag" -> 10; // 10초
            case "stream" -> 5; // 5초
            default -> 1; // 1초
        };
    }

    /**
     * 클라이언트 IP 추출 (프록시 고려)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 정적 리소스, actuator, swagger는 필터 제외
        return path.startsWith("/static/") ||
                path.startsWith("/actuator/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs");
    }
}
