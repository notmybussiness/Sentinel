package com.pjsent.sentinel.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 캐싱 설정
 *
 * API 호출 제한 문제 해결을 위한 Caffeine 캐시 설정
 * - AlphaVantage: 분당 5회, 일일 100회 제한
 * - Finnhub: 분당 60회 제한
 * - Upbit/Binance: 비교적 넉넉함
 *
 * 캐시 전략:
 * - stockPrice: 주식 가격 (5분 캐시)
 * - cryptoPrice: 암호화폐 가격 (1분 캐시 - 실시간성 중요)
 * - trendingCoins: 트렌딩 코인 (3분 캐시)
 * - marketIndices: 시장 지수 (5분 캐시)
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 캐시 매니저 설정
     *
     * Caffeine 기반 캐시 매니저 생성
     * - 메모리 내 캐시
     * - TTL(Time To Live) 기반 만료
     * - 최대 크기 제한
     */
    @Bean
    public CacheManager cacheManager() {
        log.info("=== Caffeine 캐시 매니저 초기화 ===");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());

        // 캐시 이름 등록
        cacheManager.setCacheNames(java.util.List.of(
            "stockPrice",      // 주식 가격 캐시
            "cryptoPrice",     // 암호화폐 가격 캐시
            "trendingCoins",   // 트렌딩 코인 캐시
            "marketIndices",   // 시장 지수 캐시
            "cryptoSearch",    // 암호화폐 검색 캐시
            "stockSearch"      // 주식 검색 캐시
        ));

        log.info("캐시 매니저 초기화 완료. 캐시 개수: {}", cacheManager.getCacheNames().size());
        return cacheManager;
    }

    /**
     * Caffeine 캐시 빌더
     *
     * 기본 설정:
     * - expireAfterWrite: 쓰기 후 3분 만료 (기본값)
     * - maximumSize: 최대 10,000개 항목
     * - recordStats: 통계 기록 활성화
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.MINUTES)  // 기본 3분 TTL
                .maximumSize(10_000)                     // 최대 10,000개
                .recordStats()                           // 캐시 통계 기록
                .evictionListener((key, value, cause) -> {
                    log.debug("캐시 항목 제거. 키: {}, 원인: {}", key, cause);
                });
    }

    /**
     * 주식 가격 캐시 빌더
     *
     * - TTL: 5분 (API 호출 제한 고려)
     * - 최대 크기: 1,000개
     */
    @Bean
    public Caffeine<Object, Object> stockPriceCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1_000)
                .recordStats();
    }

    /**
     * 암호화폐 가격 캐시 빌더
     *
     * - TTL: 1분 (실시간성 중요)
     * - 최대 크기: 2,000개
     */
    @Bean
    public Caffeine<Object, Object> cryptoPriceCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(2_000)
                .recordStats();
    }

    /**
     * 트렌딩 코인 캐시 빌더
     *
     * - TTL: 3분
     * - 최대 크기: 100개
     */
    @Bean
    public Caffeine<Object, Object> trendingCoinsCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .maximumSize(100)
                .recordStats();
    }

    /**
     * 시장 지수 캐시 빌더
     *
     * - TTL: 5분
     * - 최대 크기: 50개
     */
    @Bean
    public Caffeine<Object, Object> marketIndicesCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(50)
                .recordStats();
    }
}
