package com.pjsent.sentinel.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐싱 설정 (Phase 7: Caffeine → Redis 전환)
 *
 * API 호출 제한 문제 해결을 위한 Redis 분산 캐시 설정
 * - AlphaVantage: 분당 5회, 일일 100회 제한
 * - Finnhub: 분당 60회 제한
 * - Upbit: 공식 제한 없음 (과도한 요청 시 차단 가능)
 * - Korea Investment: 협약 기준
 *
 * 캐시 전략 (Phase 6 성능 테스트 결과 반영):
 * - stockPrice: 주식 가격 (10초 캐시) ← Service Layer Cache
 * - cryptoPrice: 암호화폐 가격 (1분 캐시)
 * - trendingCoins: 트렌딩 코인 (1분 캐시)
 * - marketIndices: 시장 지수 (1분 캐시)
 * - cryptoSearch/stockSearch: 검색 결과 (3분 캐시)
 * - portfolios: 포트폴리오 (5초 캐시) ← Phase 6 Scalability
 *
 * Redis vs Caffeine:
 * - Caffeine: 0ms latency (로컬 메모리), 단일 WAS 전용
 * - Redis: 0.5-2ms latency (네트워크), 여러 WAS에서 캐시 공유 가능
 *
 * Request Deduplication:
 * - @Cacheable의 sync=true 옵션으로 동시 요청 중복 방지
 * - Redis에서도 동일하게 동작 (첫 요청만 API 호출, 나머지는 대기)
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Redis용 ObjectMapper 설정
     *
     * GenericJackson2JsonRedisSerializer는 기본 ObjectMapper를 사용하므로
     * JavaTimeModule과 타입 정보를 포함한 커스텀 ObjectMapper 필요
     *
     * 설정 내용:
     * - JavaTimeModule: LocalDateTime, LocalDate 등 Java 8 Time API 지원
     * - activateDefaultTyping: 타입 정보를 JSON에 포함하여 역직렬화 지원
     * - WRITE_DATES_AS_TIMESTAMPS 비활성화: ISO-8601 포맷으로 날짜 저장
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 Time API 지원 (LocalDateTime, LocalDate 등)
        mapper.registerModule(new JavaTimeModule());

        // ISO-8601 포맷 사용 (타임스탬프 대신)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 타입 정보 포함 (역직렬화 시 정확한 타입 복원)
        // NON_FINAL → EVERYTHING으로 변경하여 record 타입도 지원
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.EVERYTHING,
            JsonTypeInfo.As.PROPERTY
        );

        log.info("✅ Redis ObjectMapper 초기화 완료 (JavaTimeModule, DefaultTyping.EVERYTHING)");
        return mapper;
    }

    /**
     * Redis 캐시 매니저 설정
     *
     * RedisCacheManager를 사용하여 각 캐시마다 개별 TTL 설정
     * - stockPrice: 10초 (실시간 주식 거래에 대응)
     * - cryptoPrice: 1분 (실시간 암호화폐 거래)
     * - trendingCoins: 1분 (순위 변동이 느림)
     * - marketIndices: 1분 (주요 지수는 안정적)
     * - cryptoSearch: 3분 (검색 결과 변동 적음)
     * - stockSearch: 3분 (검색 결과 변동 적음)
     * - portfolios: 5초 (포트폴리오 조회 고빈도)
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper redisObjectMapper) {
        log.info("=== Redis 캐시 매니저 초기화 (개별 TTL) ===");

        // Default Cache Configuration (커스텀 ObjectMapper 사용)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))  // Default TTL: 1분
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(redisObjectMapper)
                        )
                );

        // Individual Cache Configurations (각 캐시별 TTL)
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("stockPrice", defaultConfig.entryTtl(Duration.ofSeconds(30)));  // 10초
        cacheConfigurations.put("cryptoPrice", defaultConfig.entryTtl(Duration.ofSeconds(30)));  // 1분
        cacheConfigurations.put("trendingCoins", defaultConfig.entryTtl(Duration.ofMinutes(1))); // 1분
        cacheConfigurations.put("marketIndices", defaultConfig.entryTtl(Duration.ofMinutes(15))); // 1분
        cacheConfigurations.put("cryptoSearch", defaultConfig.entryTtl(Duration.ofMinutes(1)));  // 3분
        cacheConfigurations.put("stockSearch", defaultConfig.entryTtl(Duration.ofMinutes(1)));   // 3분
        cacheConfigurations.put("portfolios", defaultConfig.entryTtl(Duration.ofMinutes(1)));   // 1분 (Phase 7: TTL 증가)
        cacheConfigurations.put("historicalData", defaultConfig.entryTtl(Duration.ofDays(7))); // 7일 (과거 데이터는 안정적)

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();

        log.info("✅ Redis 캐시 매니저 초기화 완료. 캐시 개수: {}", cacheConfigurations.size());
        cacheConfigurations.forEach((name, config) ->
                log.info("   - {}: TTL={}", name, config.getTtl())
        );

        return cacheManager;
    }
}
