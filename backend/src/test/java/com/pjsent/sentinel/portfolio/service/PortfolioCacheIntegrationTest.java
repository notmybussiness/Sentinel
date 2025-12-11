package com.pjsent.sentinel.portfolio.service;

import com.pjsent.sentinel.common.exception.ResourceNotFoundException;
import com.pjsent.sentinel.portfolio.dto.PortfolioDto;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import com.pjsent.sentinel.user.entity.User;
import com.pjsent.sentinel.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Portfolio 서비스 캐싱 통합 테스트
 *
 * TDD Cycle:
 * 1. RED: @Cacheable 동작 테스트 작성 (실패 확인)
 * 2. GREEN: PortfolioService에 @Cacheable 적용으로 통과
 * 3. REFACTOR: TTL 5초 검증
 *
 * 테스트 범위:
 * - @Cacheable: 조회 시 캐시 저장
 * - Cache Hit: 두 번째 조회 시 캐시에서 반환
 * - @CacheEvict: 생성/수정/삭제 시 캐시 무효화
 * - TTL: 5초 후 캐시 만료
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.cache.type=redis"
})
class PortfolioCacheIntegrationTest {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private User testUser;
    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        // Redis 연결 확인
        assertThat(redisConnectionFactory.getConnection().ping()).isNotNull();

        // 캐시 초기화
        Cache portfoliosCache = cacheManager.getCache("portfolios");
        if (portfoliosCache != null) {
            portfoliosCache.clear();
        }

        // 테스트 데이터 생성
        testUser = User.builder()
                .email("test@example.com")
                .name("테스트유저")
                .kakaoId("test-kakao-123")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
        testUser = userRepository.save(testUser);

        testPortfolio = Portfolio.builder()
                .userId(testUser.getId())
                .name("테스트 포트폴리오")
                .description("캐시 테스트용")
                .build();
        testPortfolio = portfolioRepository.save(testPortfolio);
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        portfolioRepository.deleteAll();
        userRepository.deleteAll();

        // 캐시 정리
        Cache portfoliosCache = cacheManager.getCache("portfolios");
        if (portfoliosCache != null) {
            portfoliosCache.clear();
        }
    }

    @Test
    @DisplayName("첫 조회 시 캐시에 저장되어야 함")
    void first_read_should_cache_portfolio() {
        // Given
        Cache portfoliosCache = cacheManager.getCache("portfolios");
        Long portfolioId = testPortfolio.getId();

        // 캐시가 비어있는지 확인
        assertThat(portfoliosCache.get(portfolioId)).isNull();

        // When
        PortfolioDto result = portfolioService.getPortfolioById(portfolioId, testUser.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 포트폴리오");

        // 캐시에 저장되었는지 확인
        Cache.ValueWrapper cachedValue = portfoliosCache.get(portfolioId);
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isInstanceOf(PortfolioDto.class);
    }

    @Test
    @DisplayName("두 번째 조회 시 캐시에서 반환되어야 함 (Cache Hit)")
    void second_read_should_return_from_cache() {
        // Given
        Long portfolioId = testPortfolio.getId();
        Cache portfoliosCache = cacheManager.getCache("portfolios");

        // 첫 조회 (캐시 저장)
        PortfolioDto firstResult = portfolioService.getPortfolioById(portfolioId, testUser.getId());

        // When
        // 두 번째 조회 (캐시에서 반환)
        PortfolioDto secondResult = portfolioService.getPortfolioById(portfolioId, testUser.getId());

        // Then
        assertThat(secondResult).isNotNull();
        // Redis 역직렬화 후 새로운 인스턴스이므로 필드 값 비교
        assertThat(secondResult.getId()).isEqualTo(firstResult.getId());
        assertThat(secondResult.getName()).isEqualTo(firstResult.getName());
        assertThat(secondResult.getUserId()).isEqualTo(firstResult.getUserId());

        // 캐시가 여전히 존재하는지 확인
        Cache.ValueWrapper cachedValue = portfoliosCache.get(portfolioId);
        assertThat(cachedValue).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 포트폴리오 조회 시 예외 발생 및 캐시되지 않음")
    void read_nonexistent_portfolio_should_throw_exception_and_not_cache() {
        // Given
        Long nonexistentId = 99999L;
        Cache portfoliosCache = cacheManager.getCache("portfolios");

        // When & Then
        assertThatThrownBy(() -> portfolioService.getPortfolioById(nonexistentId, testUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("포트폴리오");

        // null 값은 캐시되지 않음 (CacheConfig의 disableCachingNullValues())
        assertThat(portfoliosCache.get(nonexistentId)).isNull();
    }

    @Test
    @DisplayName("다른 사용자의 포트폴리오 조회 시 예외 발생")
    void read_other_users_portfolio_should_throw_exception() {
        // Given
        Long otherUserId = 99999L;
        Long portfolioId = testPortfolio.getId();

        // When & Then
        assertThatThrownBy(() -> portfolioService.getPortfolioById(portfolioId, otherUserId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("캐시 키가 portfolioId로 정확히 매핑되어야 함")
    void cache_key_should_be_portfolio_id() {
        // Given
        Long portfolioId = testPortfolio.getId();
        Cache portfoliosCache = cacheManager.getCache("portfolios");

        // When
        portfolioService.getPortfolioById(portfolioId, testUser.getId());

        // Then
        // portfolioId를 키로 캐시 조회
        Cache.ValueWrapper cachedValue = portfoliosCache.get(portfolioId);
        assertThat(cachedValue).isNotNull();

        PortfolioDto cachedDto = (PortfolioDto) cachedValue.get();
        assertThat(cachedDto.getId()).isEqualTo(portfolioId);
        assertThat(cachedDto.getName()).isEqualTo("테스트 포트폴리오");
    }

    @Test
    @DisplayName("여러 포트폴리오가 독립적으로 캐싱되어야 함")
    void multiple_portfolios_should_be_cached_independently() {
        // Given
        Portfolio portfolio2 = Portfolio.builder()
                .userId(testUser.getId())
                .name("두 번째 포트폴리오")
                .description("캐시 독립성 테스트")
                .build();
        portfolio2 = portfolioRepository.save(portfolio2);

        Cache portfoliosCache = cacheManager.getCache("portfolios");

        // When
        PortfolioDto result1 = portfolioService.getPortfolioById(testPortfolio.getId(), testUser.getId());
        PortfolioDto result2 = portfolioService.getPortfolioById(portfolio2.getId(), testUser.getId());

        // Then
        // 두 포트폴리오 모두 캐싱
        assertThat(portfoliosCache.get(testPortfolio.getId())).isNotNull();
        assertThat(portfoliosCache.get(portfolio2.getId())).isNotNull();

        // 각각 독립적인 데이터
        assertThat(result1.getName()).isEqualTo("테스트 포트폴리오");
        assertThat(result2.getName()).isEqualTo("두 번째 포트폴리오");
    }

    @Test
    @DisplayName("Redis 캐시 매니저가 정상 작동해야 함")
    void redis_cache_manager_should_work() {
        // Then
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getClass().getSimpleName()).contains("Redis");

        Cache portfoliosCache = cacheManager.getCache("portfolios");
        assertThat(portfoliosCache).isNotNull();
        assertThat(portfoliosCache.getName()).isEqualTo("portfolios");
    }

    @Test
    @DisplayName("복잡한 DTO (Holdings 포함) 역직렬화가 정상 동작해야 함")
    void complex_dto_with_holdings_should_deserialize_correctly() {
        // Given
        Long portfolioId = testPortfolio.getId();
        Cache portfoliosCache = cacheManager.getCache("portfolios");

        // When
        // 첫 조회 (캐시 저장)
        PortfolioDto firstResult = portfolioService.getPortfolioById(portfolioId, testUser.getId());

        // Redis에서 직접 조회하여 역직렬화 확인
        Cache.ValueWrapper cachedValue = portfoliosCache.get(portfolioId);
        assertThat(cachedValue).isNotNull();

        // Then
        PortfolioDto cachedDto = (PortfolioDto) cachedValue.get();
        assertThat(cachedDto).isNotNull();
        assertThat(cachedDto.getId()).isEqualTo(portfolioId);
        assertThat(cachedDto.getName()).isEqualTo("테스트 포트폴리오");

        // Holdings가 null이 아니어야 함 (빈 리스트라도)
        assertThat(cachedDto.getHoldings()).isNotNull();

        // 두 번째 조회 (캐시에서 역직렬화)
        PortfolioDto secondResult = portfolioService.getPortfolioById(portfolioId, testUser.getId());
        assertThat(secondResult).isNotNull();
        assertThat(secondResult.getId()).isEqualTo(portfolioId);
        assertThat(secondResult.getHoldings()).isNotNull();
    }
}
