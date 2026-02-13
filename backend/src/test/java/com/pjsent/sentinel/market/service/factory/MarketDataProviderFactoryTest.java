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
                new Order1Provider("KoreaInvestment", false, true, true, false),
                new Order2Provider("AlphaVantage", false, true, false, true)
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

    @Test
    @DisplayName("getQuoteProviders prioritizes KoreaInvestment for KR symbols")
    void getQuoteProviders_prioritizesKoreaInvestment_forKrSymbols() {
        MarketDataProviderFactory factory = createFactoryWithPolicyProviders();

        List<MarketDataProvider> providers = factory.getQuoteProviders("005930");

        assertThat(providers)
                .extracting(MarketDataProvider::getProviderName)
                .containsExactly("KoreaInvestment", "YahooFinance");
    }

    @Test
    @DisplayName("getQuoteProviders prioritizes Finnhub for global symbols")
    void getQuoteProviders_prioritizesFinnhub_forGlobalSymbols() {
        MarketDataProviderFactory factory = createFactoryWithPolicyProviders();

        List<MarketDataProvider> providers = factory.getQuoteProviders("AAPL");

        assertThat(providers)
                .extracting(MarketDataProvider::getProviderName)
                .containsExactly("Finnhub", "AlphaVantage", "YahooFinance");
    }

    @Test
    @DisplayName("getSearchProviders filters by search capability and prioritizes AlphaVantage")
    void getSearchProviders_filtersAndPrioritizes() {
        MarketDataProviderFactory factory = createFactoryWithPolicyProviders();

        List<MarketDataProvider> providers = factory.getSearchProviders();

        assertThat(providers)
                .extracting(MarketDataProvider::getProviderName)
                .containsExactly("AlphaVantage", "KoreaInvestment");
    }

    private MarketDataProviderFactory createFactoryWithOneUnavailableProvider() {
        return new MarketDataProviderFactory(List.of(
                new Order4Provider("Finnhub", true, false, true, true),
                new Order2Provider("AlphaVantage", false, true, true, true),
                new Order3Provider("YahooFinance", true, false, true, true),
                new Order1Provider("KoreaInvestment", true, true, true, true)
        ));
    }

    private MarketDataProviderFactory createFactoryWithPolicyProviders() {
        return new MarketDataProviderFactory(List.of(
                new Order4Provider("Finnhub", true, false, false, true),
                new Order2Provider("AlphaVantage", true, true, false, true),
                new Order3Provider("YahooFinance", true, false, true, true),
                new Order1Provider("KoreaInvestment", true, true, true, false)
        ));
    }

    private abstract static class StubProvider implements MarketDataProvider {
        private final String providerName;
        private final boolean available;
        private final boolean supportsSearch;
        private final boolean supportsKrSymbol;
        private final boolean supportsGlobalSymbol;

        protected StubProvider(String providerName, boolean available, boolean supportsSearch,
                boolean supportsKrSymbol, boolean supportsGlobalSymbol) {
            this.providerName = providerName;
            this.available = available;
            this.supportsSearch = supportsSearch;
            this.supportsKrSymbol = supportsKrSymbol;
            this.supportsGlobalSymbol = supportsGlobalSymbol;
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

        @Override
        public boolean supportsSearch() {
            return supportsSearch;
        }

        @Override
        public boolean supportsSymbol(String symbol) {
            return symbol != null && symbol.matches("\\d{6}") ? supportsKrSymbol : supportsGlobalSymbol;
        }
    }

    @Order(1)
    private static final class Order1Provider extends StubProvider {
        private Order1Provider(String providerName, boolean available, boolean supportsSearch,
                boolean supportsKrSymbol, boolean supportsGlobalSymbol) {
            super(providerName, available, supportsSearch, supportsKrSymbol, supportsGlobalSymbol);
        }
    }

    @Order(2)
    private static final class Order2Provider extends StubProvider {
        private Order2Provider(String providerName, boolean available, boolean supportsSearch,
                boolean supportsKrSymbol, boolean supportsGlobalSymbol) {
            super(providerName, available, supportsSearch, supportsKrSymbol, supportsGlobalSymbol);
        }
    }

    @Order(3)
    private static final class Order3Provider extends StubProvider {
        private Order3Provider(String providerName, boolean available, boolean supportsSearch,
                boolean supportsKrSymbol, boolean supportsGlobalSymbol) {
            super(providerName, available, supportsSearch, supportsKrSymbol, supportsGlobalSymbol);
        }
    }

    @Order(4)
    private static final class Order4Provider extends StubProvider {
        private Order4Provider(String providerName, boolean available, boolean supportsSearch,
                boolean supportsKrSymbol, boolean supportsGlobalSymbol) {
            super(providerName, available, supportsSearch, supportsKrSymbol, supportsGlobalSymbol);
        }
    }
}
