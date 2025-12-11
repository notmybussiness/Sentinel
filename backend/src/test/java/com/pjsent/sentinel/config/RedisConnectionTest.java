package com.pjsent.sentinel.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 연결 및 기본 캐시 동작 테스트
 *
 * RED-GREEN-REFACTOR:
 * [RED] 테스트 먼저 작성 → Redis 미설정 시 실패
 * [GREEN] CacheConfig.java로 구현 → 테스트 통과
 * [REFACTOR] 개별 TTL 설정 검증
 */
@DataRedisTest
@Import({CacheConfig.class, RedisAutoConfiguration.class})
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class RedisConnectionTest {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 캐시 초기화
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }

    @Test
    @DisplayName("[RED → GREEN] Redis 서버 연결 확인")
    void redis_ping_should_return_pong() {
        // Given & When
        RedisConnection connection = redisConnectionFactory.getConnection();

        // Then
        assertThat(connection).isNotNull();
        assertThat(connection.ping()).isNotNull();

        connection.close();
    }

    @Test
    @DisplayName("[GREEN] RedisCacheManager가 정상 생성되어야 함")
    void cache_manager_should_be_redis_type() {
        // Then
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    @DisplayName("[GREEN] 7개의 캐시가 설정되어야 함")
    void seven_caches_should_be_configured() {
        // Given
        String[] expectedCaches = {
            "stockPrice", "cryptoPrice", "trendingCoins",
            "marketIndices", "cryptoSearch", "stockSearch", "portfolios"
        };

        // When & Then
        for (String cacheName : expectedCaches) {
            Cache cache = cacheManager.getCache(cacheName);
            assertThat(cache)
                .as("Cache '%s' should exist", cacheName)
                .isNotNull();
        }
    }

    @Test
    @DisplayName("[GREEN] stockPrice 캐시 저장 및 조회")
    void stockPrice_cache_should_store_and_retrieve() {
        // Given
        Cache cache = cacheManager.getCache("stockPrice");
        String key = "AAPL";
        String value = "150.25";

        // When
        cache.put(key, value);
        Cache.ValueWrapper retrieved = cache.get(key);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get()).isEqualTo(value);
    }

    @Test
    @DisplayName("[GREEN] cryptoPrice 캐시 저장 및 조회")
    void cryptoPrice_cache_should_store_and_retrieve() {
        // Given
        Cache cache = cacheManager.getCache("cryptoPrice");
        String key = "BTC-USD";
        Double value = 45000.0;

        // When
        cache.put(key, value);
        Cache.ValueWrapper retrieved = cache.get(key);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get()).isEqualTo(value);
    }

    @Test
    @DisplayName("[GREEN] portfolios 캐시 저장 및 조회")
    void portfolios_cache_should_store_and_retrieve() {
        // Given
        Cache cache = cacheManager.getCache("portfolios");
        Long key = 1L;
        String value = "My Portfolio";

        // When
        cache.put(key, value);
        Cache.ValueWrapper retrieved = cache.get(key);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get()).isEqualTo(value);
    }

    @Test
    @DisplayName("[GREEN] 캐시 삭제(evict) 동작 확인")
    void cache_evict_should_remove_entry() {
        // Given
        Cache cache = cacheManager.getCache("stockPrice");
        String key = "TSLA";
        String value = "200.50";
        cache.put(key, value);

        // When
        cache.evict(key);

        // Then
        assertThat(cache.get(key)).isNull();
    }

    @Test
    @DisplayName("[GREEN] 캐시 전체 삭제(clear) 동작 확인")
    void cache_clear_should_remove_all_entries() {
        // Given
        Cache cache = cacheManager.getCache("stockPrice");
        cache.put("AAPL", "150.0");
        cache.put("TSLA", "200.0");
        cache.put("GOOGL", "100.0");

        // When
        cache.clear();

        // Then
        assertThat(cache.get("AAPL")).isNull();
        assertThat(cache.get("TSLA")).isNull();
        assertThat(cache.get("GOOGL")).isNull();
    }

    @Test
    @DisplayName("[GREEN] 존재하지 않는 키 조회 시 null 반환")
    void get_nonexistent_key_should_return_null() {
        // Given
        Cache cache = cacheManager.getCache("stockPrice");

        // When
        Cache.ValueWrapper retrieved = cache.get("NONEXISTENT");

        // Then
        assertThat(retrieved).isNull();
    }

    @Test
    @DisplayName("[REFACTOR] 같은 키에 다른 값 저장 시 덮어쓰기")
    void put_same_key_should_overwrite_value() {
        // Given
        Cache cache = cacheManager.getCache("stockPrice");
        String key = "AAPL";

        // When
        cache.put(key, "150.0");
        cache.put(key, "155.0");
        Cache.ValueWrapper retrieved = cache.get(key);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get()).isEqualTo("155.0");
    }
}
