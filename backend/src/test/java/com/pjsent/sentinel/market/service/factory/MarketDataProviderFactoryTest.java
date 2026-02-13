package com.pjsent.sentinel.market.service.factory;

import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.provider.MarketDataProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MarketDataProviderFactory Unit Tests")
class MarketDataProviderFactoryTest {

    @Test
    @DisplayName("getAvailableProviders sorts providers by @Order and filters unavailable providers")
    void getAvailableProviders_sortsByOrder_andFiltersUnavailable() {
        MarketDataProviderFactory factory = createFactoryWithOneUnavailableProvider();

        List<MarketDataProvider> providers = factory.getAvailableProviders();

        assertThat(providers)
                .extracting(MarketDataProvider::getProviderName)
                .containsExactly("KoreaInvestment", "YahooFinance", "Finnhub");
    }

    @Test
    @DisplayName("getPrimaryProvider returns the highest priority available provider")
    void getPrimaryProvider_returnsFirstAvailableOrderedProvider() {
        MarketDataProviderFactory factory = createFactoryWithOneUnavailableProvider();

        MarketDataProvider primary = factory.getPrimaryProvider();

        assertThat(primary).isNotNull();
        assertThat(primary.getProviderName()).isEqualTo("KoreaInvestment");
    }

    @Test
    @DisplayName("getProvider is case-insensitive and ignores unavailable providers")
    void getProvider_caseInsensitive_andUnavailableFiltered() {
        MarketDataProviderFactory factory = createFactoryWithOneUnavailableProvider();

        MarketDataProvider korea = factory.getProvider("koreainvestment");
        MarketDataProvider alpha = factory.getProvider("AlphaVantage");

        assertThat(korea).isNotNull();
        assertThat(korea.getProviderName()).isEqualTo("KoreaInvestment");
        assertThat(alpha).isNull();
    }

    @Test
    @DisplayName("hasAvailableProvider returns false when all providers are unavailable")
    void hasAvailableProvider_returnsFalse_whenAllUnavailable() {
        MarketDataProviderFactory factory = new MarketDataProviderFactory(List.of(
                new Order1Provider("KoreaInvestment", false),
                new Order2Provider("AlphaVantage", false)
        ));

        assertThat(factory.hasAvailableProvider()).isFalse();
        assertThat(factory.getPrimaryProvider()).isNull();
        assertThat(factory.getAvailableProviders()).isEmpty();
    }

    @Test
    @DisplayName("hasAvailableProvider returns true when at least one provider is available")
    void hasAvailableProvider_returnsTrue_whenAnyAvailable() {
        MarketDataProviderFactory factory = createFactoryWithOneUnavailableProvider();

        assertThat(factory.hasAvailableProvider()).isTrue();
    }

    private MarketDataProviderFactory createFactoryWithOneUnavailableProvider() {
        return new MarketDataProviderFactory(List.of(
                new Order4Provider("Finnhub", true),
                new Order2Provider("AlphaVantage", false),
                new Order3Provider("YahooFinance", true),
                new Order1Provider("KoreaInvestment", true)
        ));
    }

    private abstract static class StubProvider implements MarketDataProvider {
        private final String providerName;
        private final boolean available;

        protected StubProvider(String providerName, boolean available) {
            this.providerName = providerName;
            this.available = available;
        }

        @Override
        public StockPriceDto getMarketData(String symbol) {
            return null;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public String getProviderName() {
            return providerName;
        }
    }

    @Order(1)
    private static final class Order1Provider extends StubProvider {
        private Order1Provider(String providerName, boolean available) {
            super(providerName, available);
        }
    }

    @Order(2)
    private static final class Order2Provider extends StubProvider {
        private Order2Provider(String providerName, boolean available) {
            super(providerName, available);
        }
    }

    @Order(3)
    private static final class Order3Provider extends StubProvider {
        private Order3Provider(String providerName, boolean available) {
            super(providerName, available);
        }
    }

    @Order(4)
    private static final class Order4Provider extends StubProvider {
        private Order4Provider(String providerName, boolean available) {
            super(providerName, available);
        }
    }
}
