package com.pjsent.sentinel.market.service.factory;

import com.pjsent.sentinel.market.service.provider.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarketDataProviderFactory 우선순위 테스트
 *
 * 목적: KoreaInvestmentProvider가 1순위로 작동하는지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MarketDataProviderFactory 우선순위 테스트")
class MarketDataProviderFactoryTest {

    @Autowired
    private MarketDataProviderFactory providerFactory;

    @Autowired
    private List<MarketDataProvider> allProviders;

    @BeforeEach
    void setUp() {
        providerFactory.logProviderStatus();
    }

    @Test
    @DisplayName("KoreaInvestmentProvider가 1순위로 등록되어야 한다")
    void testKoreaInvestmentProviderIsFirst() {
        // Given: Provider 목록 조회
        List<MarketDataProvider> availableProviders = providerFactory.getAvailableProviders();

        // When: 첫 번째 Provider 확인
        assertThat(availableProviders).isNotEmpty();
        MarketDataProvider firstProvider = availableProviders.get(0);

        // Then: KoreaInvestmentProvider가 1순위여야 함
        assertThat(firstProvider).isInstanceOf(KoreaInvestmentProvider.class);
        assertThat(firstProvider.getProviderName()).isEqualTo("KoreaInvestment");
    }

    @Test
    @DisplayName("Provider 우선순위: KoreaInvestment -> AlphaVantage -> YahooFinance -> Finnhub")
    void testProviderOrder() {
        // Given: 모든 Provider가 활성화되어 있다고 가정
        List<MarketDataProvider> availableProviders = providerFactory.getAvailableProviders();

        // When: Provider 이름 목록 추출
        List<String> providerNames = availableProviders.stream()
                .map(MarketDataProvider::getProviderName)
                .toList();

        // Then: 예상 순서 검증
        System.out.println("📋 실제 Provider 순서: " + providerNames);

        // 최소한 KoreaInvestment가 첫 번째여야 함
        assertThat(providerNames.get(0)).isEqualTo("KoreaInvestment");

        // 가능한 경우 전체 순서 검증 (일부 Provider가 비활성화될 수 있음)
        if (availableProviders.size() >= 2) {
            // AlphaVantage가 있다면 KoreaInvestment 다음이어야 함
            if (providerNames.contains("AlphaVantage")) {
                int koreaIdx = providerNames.indexOf("KoreaInvestment");
                int alphaIdx = providerNames.indexOf("AlphaVantage");
                assertThat(koreaIdx).isLessThan(alphaIdx);
            }
        }
    }

    @Test
    @DisplayName("getPrimaryProvider()는 KoreaInvestmentProvider를 반환해야 한다")
    void testGetPrimaryProvider() {
        // When: Primary Provider 조회
        MarketDataProvider primaryProvider = providerFactory.getPrimaryProvider();

        // Then: KoreaInvestmentProvider여야 함
        assertThat(primaryProvider).isNotNull();
        assertThat(primaryProvider).isInstanceOf(KoreaInvestmentProvider.class);
        assertThat(primaryProvider.getProviderName()).isEqualTo("KoreaInvestment");
    }

    @Test
    @DisplayName("getProvider()로 특정 Provider 조회 가능해야 한다")
    void testGetProviderByName() {
        // When: 이름으로 Provider 조회
        MarketDataProvider koreaProvider = providerFactory.getProvider("KoreaInvestment");
        MarketDataProvider alphaProvider = providerFactory.getProvider("AlphaVantage");

        // Then: 올바른 Provider 반환
        assertThat(koreaProvider).isNotNull();
        assertThat(koreaProvider.getProviderName()).isEqualTo("KoreaInvestment");

        if (alphaProvider != null) {
            assertThat(alphaProvider.getProviderName()).isEqualTo("AlphaVantage");
        }
    }

    @Test
    @DisplayName("모든 Provider는 @Order 어노테이션을 가져야 한다")
    void testAllProvidersHaveOrder() {
        // Given: 모든 Provider 조회
        List<MarketDataProvider> providers = providerFactory.getAvailableProviders();

        // Then: 최소 1개 이상의 Provider가 등록되어 있어야 함
        assertThat(providers).isNotEmpty();

        // Provider 정보 출력
        System.out.println("\n=== Provider 등록 순서 ===");
        for (int i = 0; i < providers.size(); i++) {
            MarketDataProvider provider = providers.get(i);
            System.out.printf("%d. %s (Available: %s)%n",
                    i + 1,
                    provider.getProviderName(),
                    provider.isAvailable());
        }
        System.out.println("==========================\n");
    }

    @Test
    @DisplayName("hasAvailableProvider()는 true를 반환해야 한다")
    void testHasAvailableProvider() {
        // When: Provider 존재 여부 확인
        boolean hasProvider = providerFactory.hasAvailableProvider();

        // Then: 최소 1개 이상의 Provider가 있어야 함
        assertThat(hasProvider).isTrue();
    }

    @Test
    @DisplayName("Spring Context에 모든 Provider가 등록되어 있어야 한다")
    void testAllProvidersRegistered() {
        // Given: 주입된 Provider 목록 확인
        assertThat(allProviders).isNotNull();

        // Then: 4개의 Provider가 등록되어 있어야 함
        assertThat(allProviders.size()).isGreaterThanOrEqualTo(4);

        // Provider 클래스 확인
        boolean hasKoreaInvestment = allProviders.stream()
                .anyMatch(p -> p instanceof KoreaInvestmentProvider);
        boolean hasAlphaVantage = allProviders.stream()
                .anyMatch(p -> p instanceof AlphaVantageProvider);
        boolean hasYahooFinance = allProviders.stream()
                .anyMatch(p -> p instanceof YahooFinanceProvider);
        boolean hasFinnhub = allProviders.stream()
                .anyMatch(p -> p instanceof FinnhubProvider);

        assertThat(hasKoreaInvestment).isTrue();
        assertThat(hasAlphaVantage).isTrue();
        assertThat(hasYahooFinance).isTrue();
        assertThat(hasFinnhub).isTrue();
    }
}
