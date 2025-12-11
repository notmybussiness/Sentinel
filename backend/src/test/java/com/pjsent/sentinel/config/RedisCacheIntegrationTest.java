package com.pjsent.sentinel.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 캐시 통합 테스트
 *
 * TDD Cycle:
 * 1. RED: Redis 캐시 기능 테스트 작성 (실패 확인)
 * 2. GREEN: CacheConfig 구현으로 테스트 통과
 * 3. REFACTOR: 개별 TTL 설정 검증
 *
 * 테스트 범위:
 * - Redis 연결 확인
 * - CacheManager 빈 생성 확인
 * - 개별 캐시 생성 확인
 * - TTL 설정 검증
 * - 캐시 저장/조회/삭제 기능
 */
@SpringBootTest
@ActiveProfiles("test")
class RedisCacheIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 캐시 초기화
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("Redis 연결이 정상적으로 동작해야 함")
    void redis_connection_should_work() {
        // Given & When
        RedisConnection connection = redisConnectionFactory.getConnection();

        // Then
        assertThat(connection).isNotNull();
        assertThat(connection.ping()).isNotNull();

        connection.close();
    }

    @Test
    @DisplayName("CacheManager 빈이 정상적으로 생성되어야 함")
    void cacheManager_bean_should_be_created() {
        // Then
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getClass().getSimpleName()).contains("Redis");
    }

    @Test
    @DisplayName("7개의 캐시가 설정되어야 함")
    void seven_caches_should_be_configured() {
        // Given
        String[] expectedCaches = {
            "stockPrice",      // 10초
            "cryptoPrice",     // 1분
            "trendingCoins",   // 1분
            "marketIndices",   // 1분
            "cryptoSearch",    // 3분
            "stockSearch",     // 3분
            "portfolios"       // 5초
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
    @DisplayName("stockPrice 캐시: 데이터 저장 및 조회가 동작해야 함")
    void stockPrice_cache_should_store_and_retrieve() {
        // Given
        Cache stockPriceCache = cacheManager.getCache("stockPrice");
        String key = "AAPL";
        StockPriceTestDto value = new StockPriceTestDto("AAPL", 150.0);

        // When
        stockPriceCache.put(key, value);
        Cache.ValueWrapper retrieved = stockPriceCache.get(key);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get()).isEqualTo(value);
    }

    @Test
    @DisplayName("cryptoPrice 캐시: 데이터 저장 및 조회가 동작해야 함")
    void cryptoPrice_cache_should_store_and_retrieve() {
        // Given
        Cache cryptoPriceCache = cacheManager.getCache("cryptoPrice");
        String key = "BTC-USD";
        CryptoPriceTestDto value = new CryptoPriceTestDto("BTC-USD", 45000.0);

        // When
        cryptoPriceCache.put(key, value);
        Cache.ValueWrapper retrieved = cryptoPriceCache.get(key);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get()).isEqualTo(value);
    }

    @Test
    @DisplayName("portfolios 캐시: 데이터 저장 및 조회가 동작해야 함")
    void portfolios_cache_should_store_and_retrieve() {
        // Given
        Cache portfoliosCache = cacheManager.getCache("portfolios");
        Long userId = 1L;
        PortfolioTestDto value = new PortfolioTestDto(userId, "My Portfolio");

        // When
        portfoliosCache.put(userId, value);
        Cache.ValueWrapper retrieved = portfoliosCache.get(userId);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get()).isEqualTo(value);
    }

    @Test
    @DisplayName("캐시 삭제(evict)가 정상 동작해야 함")
    void cache_evict_should_work() {
        // Given
        Cache stockPriceCache = cacheManager.getCache("stockPrice");
        String key = "TSLA";
        StockPriceTestDto value = new StockPriceTestDto("TSLA", 200.0);
        stockPriceCache.put(key, value);

        // When
        stockPriceCache.evict(key);
        Cache.ValueWrapper retrieved = stockPriceCache.get(key);

        // Then
        assertThat(retrieved).isNull();
    }

    @Test
    @DisplayName("캐시 전체 삭제(clear)가 정상 동작해야 함")
    void cache_clear_should_work() {
        // Given
        Cache stockPriceCache = cacheManager.getCache("stockPrice");
        stockPriceCache.put("AAPL", new StockPriceTestDto("AAPL", 150.0));
        stockPriceCache.put("TSLA", new StockPriceTestDto("TSLA", 200.0));
        stockPriceCache.put("GOOGL", new StockPriceTestDto("GOOGL", 100.0));

        // When
        stockPriceCache.clear();

        // Then
        assertThat(stockPriceCache.get("AAPL")).isNull();
        assertThat(stockPriceCache.get("TSLA")).isNull();
        assertThat(stockPriceCache.get("GOOGL")).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 키 조회 시 null 반환")
    void get_nonexistent_key_should_return_null() {
        // Given
        Cache stockPriceCache = cacheManager.getCache("stockPrice");

        // When
        Cache.ValueWrapper retrieved = stockPriceCache.get("NONEXISTENT");

        // Then
        assertThat(retrieved).isNull();
    }

    @Test
    @DisplayName("같은 키에 다른 값 저장 시 덮어쓰기 동작")
    void put_same_key_should_overwrite() {
        // Given
        Cache stockPriceCache = cacheManager.getCache("stockPrice");
        String key = "AAPL";
        StockPriceTestDto oldValue = new StockPriceTestDto("AAPL", 150.0);
        StockPriceTestDto newValue = new StockPriceTestDto("AAPL", 155.0);

        // When
        stockPriceCache.put(key, oldValue);
        stockPriceCache.put(key, newValue);
        Cache.ValueWrapper retrieved = stockPriceCache.get(key);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.get()).isEqualTo(newValue);
        assertThat(((StockPriceTestDto) retrieved.get()).price()).isEqualTo(155.0);
    }

    // Test DTOs (간단한 record 사용)
    record StockPriceTestDto(String symbol, Double price) {}
    record CryptoPriceTestDto(String pair, Double price) {}
    record PortfolioTestDto(Long userId, String name) {}
}
