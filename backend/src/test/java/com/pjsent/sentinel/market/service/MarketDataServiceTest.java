package com.pjsent.sentinel.market.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pjsent.sentinel.market.dto.SearchResultDto;
import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.producer.MarketPriceProducer;
import com.pjsent.sentinel.market.service.factory.MarketDataProviderFactory;
import com.pjsent.sentinel.market.service.provider.MarketDataProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService tests")
class MarketDataServiceTest {

    @Mock
    private MarketDataProviderFactory providerFactory;

    @Mock
    private MarketDataProvider mockProvider;

    @Mock
    private MarketDataProvider nonSearchProvider;

    @Mock
    private MarketDataProvider searchProvider;

    @Mock
    private MarketPriceProducer marketPriceProducer;

    @Mock
    private Clock clock;

    @InjectMocks
    private MarketDataService marketDataService;

    @Test
    @DisplayName("returns stock price when provider succeeds")
    void shouldReturnDataWhenProviderSucceeds() {
        String symbol = "AAPL";
        StockPriceDto expectedData = createMockStockPriceDto(symbol, "TestProvider");
        List<MarketDataProvider> providers = Arrays.asList(mockProvider);

        when(providerFactory.getAvailableProviders()).thenReturn(providers);
        when(mockProvider.getMarketData(symbol)).thenReturn(expectedData);

        StockPriceDto result = marketDataService.getStockPrice(symbol);

        assertNotNull(result);
        assertEquals(symbol, result.getSymbol());
        assertEquals("TestProvider", result.getProvider());
        verify(mockProvider).getMarketData(symbol);
        verifyNoInteractions(marketPriceProducer);
    }

    @Test
    @DisplayName("read path must not publish events")
    void shouldNotPublishEventWhenReadPathIsUsed() {
        String symbol = "AAPL";
        StockPriceDto expectedData = createMockStockPriceDto(symbol, "TestProvider");
        List<MarketDataProvider> providers = Arrays.asList(mockProvider);

        when(providerFactory.getAvailableProviders()).thenReturn(providers);
        when(mockProvider.getMarketData(symbol)).thenReturn(expectedData);

        StockPriceDto result = marketDataService.getStockPrice(symbol);

        assertNotNull(result);
        verify(mockProvider, times(1)).getMarketData(symbol);
        verifyNoInteractions(marketPriceProducer);
    }

    @Test
    @DisplayName("explicit refresh path publishes event")
    void shouldPublishEventWhenRefreshPathIsUsed() {
        String symbol = "AAPL";
        StockPriceDto expectedData = createMockStockPriceDto(symbol, "TestProvider");
        List<MarketDataProvider> providers = Arrays.asList(mockProvider);

        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        when(providerFactory.getAvailableProviders()).thenReturn(providers);
        when(mockProvider.getMarketData(symbol)).thenReturn(expectedData);

        StockPriceDto result = marketDataService.refreshStockPriceAndPublish(symbol);

        assertNotNull(result);
        verify(mockProvider, times(1)).getMarketData(symbol);
        verify(marketPriceProducer, times(1)).publishPriceUpdate(any());
    }

    @Test
    @DisplayName("search path must only call providers that support search")
    void shouldUseOnlySupportsSearchProviders() {
        String query = "apple";
        List<SearchResultDto> expected = List.of(new SearchResultDto("AAPL", "Apple Inc", "US", "STOCK"));
        List<MarketDataProvider> providers = Arrays.asList(nonSearchProvider, searchProvider);

        when(providerFactory.getAvailableProviders()).thenReturn(providers);
        when(nonSearchProvider.getProviderName()).thenReturn("NoSearch");
        when(nonSearchProvider.supportsSearch()).thenReturn(false);
        when(searchProvider.getProviderName()).thenReturn("SearchProvider");
        when(searchProvider.supportsSearch()).thenReturn(true);
        when(searchProvider.searchSymbol(query)).thenReturn(expected);

        List<SearchResultDto> result = marketDataService.searchSymbol(query);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
        verify(nonSearchProvider, never()).searchSymbol(any());
        verify(searchProvider, times(1)).searchSymbol(query);
    }

    @Test
    @DisplayName("throws exception when no providers are available")
    void shouldThrowExceptionWhenNoAvailableProviders() {
        String symbol = "AAPL";
        when(providerFactory.getAvailableProviders()).thenReturn(Collections.emptyList());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> marketDataService.getStockPrice(symbol));

        assertTrue(exception.getMessage().contains("시장 데이터 프로바이더가 없습니다"));
    }

    @Test
    @DisplayName("throws exception when symbol is blank")
    void shouldThrowExceptionWhenSymbolIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> marketDataService.getStockPrice(""));
        assertThrows(IllegalArgumentException.class, () -> marketDataService.getStockPrice(null));
    }

    @Test
    @DisplayName("returns service availability from provider factory")
    void shouldReturnCorrectServiceAvailability() {
        when(providerFactory.hasAvailableProvider()).thenReturn(true);

        boolean isAvailable = marketDataService.isServiceAvailable();

        assertTrue(isAvailable);
        verify(providerFactory).hasAvailableProvider();
    }

    @Test
    @DisplayName("returns provider status message")
    void shouldReturnProviderStatus() {
        doNothing().when(providerFactory).logProviderStatus();

        String status = marketDataService.getProviderStatus();

        assertNotNull(status);
        assertTrue(!status.isBlank());
        verify(providerFactory).logProviderStatus();
    }

    private StockPriceDto createMockStockPriceDto(String symbol, String provider) {
        return StockPriceDto.builder()
                .symbol(symbol)
                .price(150.25)
                .open(149.50)
                .high(151.00)
                .low(148.75)
                .close(149.00)
                .change(1.25)
                .changePercent(0.84)
                .lastTradingDay("2024-01-15")
                .provider(provider)
                .build();
    }
}
