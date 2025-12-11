package com.pjsent.sentinel.market.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.factory.MarketDataProviderFactory;
import com.pjsent.sentinel.market.service.provider.MarketDataProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataService 테스트")
class MarketDataServiceTest {
    
    @Mock
    private MarketDataProviderFactory providerFactory;
    
    @Mock
    private MarketDataProvider mockProvider;
    
    @InjectMocks
    private MarketDataService marketDataService;
    
    @Test
    @DisplayName("프로바이더가 성공할 때 데이터를 반환해야 한다")
    void should_ReturnData_When_ProviderSucceeds() {
        // Given
        String symbol = "AAPL";
        StockPriceDto expectedData = createMockStockPriceDto(symbol, "TestProvider");
        List<MarketDataProvider> providers = Arrays.asList(mockProvider);
        
        when(providerFactory.getAvailableProviders()).thenReturn(providers);
        when(mockProvider.getMarketData(symbol)).thenReturn(expectedData);
        
        // When
        StockPriceDto result = marketDataService.getStockPrice(symbol);
        
        // Then
        assertNotNull(result);
        assertEquals(symbol, result.getSymbol());
        assertEquals("TestProvider", result.getProvider());
        verify(mockProvider).getMarketData(symbol);
    }
    
    @Test
    @DisplayName("사용 가능한 프로바이더가 없을 때 예외를 발생시켜야 한다")
    void should_ThrowException_When_NoAvailableProviders() {
        // Given
        String symbol = "AAPL";
        when(providerFactory.getAvailableProviders()).thenReturn(Collections.emptyList());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            marketDataService.getStockPrice(symbol);
        });
        
        assertTrue(exception.getMessage().contains("사용 가능한 시장 데이터 프로바이더가 없습니다"));
    }
    
    @Test
    @DisplayName("빈 심볼로 요청할 때 예외를 발생시켜야 한다")
    void should_ThrowException_When_SymbolIsEmpty() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            marketDataService.getStockPrice("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            marketDataService.getStockPrice(null);
        });
    }
    
    @Test
    @DisplayName("서비스 사용 가능 여부를 올바르게 반환해야 한다")
    void should_ReturnCorrectServiceAvailability() {
        // Given
        when(providerFactory.hasAvailableProvider()).thenReturn(true);
        
        // When
        boolean isAvailable = marketDataService.isServiceAvailable();
        
        // Then
        assertTrue(isAvailable);
        verify(providerFactory).hasAvailableProvider();
    }
    
    @Test
    @DisplayName("프로바이더 상태를 올바르게 반환해야 한다")
    void should_ReturnProviderStatus() {
        // Given
        doNothing().when(providerFactory).logProviderStatus();
        
        // When
        String status = marketDataService.getProviderStatus();
        
        // Then
        assertEquals("프로바이더 상태가 로그에 출력되었습니다.", status);
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