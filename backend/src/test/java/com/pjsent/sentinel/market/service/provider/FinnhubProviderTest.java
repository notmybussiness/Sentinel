package com.pjsent.sentinel.market.service.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.pjsent.sentinel.market.config.StockMarketProperties;
import com.pjsent.sentinel.market.dto.StockPriceDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinnhubProvider tests")
class FinnhubProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private StockMarketProperties properties;
    private FinnhubProvider finnhubProvider;

    @BeforeEach
    void setUp() {
        properties = new StockMarketProperties();
        properties.getFinnhub().setApiKey("test-api-key");
        properties.getFinnhub().setBaseUrl("https://finnhub.io/api/v1");
        properties.getFinnhub().setEnabled(true);

        finnhubProvider = new FinnhubProvider(restTemplate, properties);
    }

    @Test
    @DisplayName("returns true when provider is available")
    void shouldReturnTrueWhenProviderIsAvailable() {
        assertTrue(finnhubProvider.isAvailable());
    }

    @Test
    @DisplayName("returns false when API key is missing")
    void shouldReturnFalseWhenApiKeyIsMissing() {
        properties.getFinnhub().setApiKey("");

        assertFalse(finnhubProvider.isAvailable());
    }

    @Test
    @DisplayName("returns provider name")
    void shouldReturnCorrectProviderName() {
        assertEquals("Finnhub", finnhubProvider.getProviderName());
    }

    @Test
    @DisplayName("supports global symbols only for quote path")
    void shouldSupportOnlyGlobalSymbols() {
        assertTrue(finnhubProvider.supportsSymbol("AAPL"));
        assertFalse(finnhubProvider.supportsSymbol("005930"));
    }

    @Test
    @DisplayName("returns stock data when API response is valid")
    void shouldGetStockDataSuccessfullyWhenValidResponse() {
        String symbol = "AAPL";
        Map<String, Object> mockResponse = createMockFinnhubResponse();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(responseEntity);

        StockPriceDto result = finnhubProvider.getMarketData(symbol);

        assertNotNull(result);
        assertEquals(symbol, result.getSymbol());
        assertEquals(150.25, result.getPrice());
        assertEquals(149.50, result.getOpen());
        assertEquals(151.00, result.getHigh());
        assertEquals(148.75, result.getLow());
        assertEquals(149.00, result.getClose());
        assertEquals(1.25, result.getChange());
        assertEquals(0.84, result.getChangePercent(), 0.01);
        assertEquals("Finnhub", result.getProvider());
    }

    @Test
    @DisplayName("throws exception when API call fails")
    void shouldThrowExceptionWhenApiCallFails() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("API call failed"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> finnhubProvider.getMarketData("AAPL"));

        assertTrue(exception.getMessage().contains("Finnhub API"));
    }

    @Test
    @DisplayName("throws exception when response data is invalid")
    void shouldThrowExceptionWhenInvalidResponseData() {
        Map<String, Object> invalidResponse = new HashMap<>();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(invalidResponse, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(responseEntity);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> finnhubProvider.getMarketData("AAPL"));

        assertTrue(exception.getMessage().contains("response"));
    }

    private Map<String, Object> createMockFinnhubResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("c", 150.25);
        response.put("o", 149.50);
        response.put("h", 151.00);
        response.put("l", 148.75);
        response.put("pc", 149.00);
        response.put("t", 1705276800L);
        return response;
    }
}
