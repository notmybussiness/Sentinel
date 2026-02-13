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
@DisplayName("AlphaVantageProvider tests")
class AlphaVantageProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private StockMarketProperties properties;
    private AlphaVantageProvider alphaVantageProvider;

    @BeforeEach
    void setUp() {
        properties = new StockMarketProperties();
        properties.getAlphavantage().setApiKey("test-api-key");
        properties.getAlphavantage().setBaseUrl("https://www.alphavantage.co/query");
        properties.getAlphavantage().setEnabled(true);

        alphaVantageProvider = new AlphaVantageProvider(restTemplate, properties);
    }

    @Test
    @DisplayName("returns true when provider is available")
    void shouldReturnTrueWhenProviderIsAvailable() {
        assertTrue(alphaVantageProvider.isAvailable());
    }

    @Test
    @DisplayName("returns false when API key is missing")
    void shouldReturnFalseWhenApiKeyIsMissing() {
        properties.getAlphavantage().setApiKey("");

        assertFalse(alphaVantageProvider.isAvailable());
    }

    @Test
    @DisplayName("returns provider name")
    void shouldReturnCorrectProviderName() {
        assertEquals("AlphaVantage", alphaVantageProvider.getProviderName());
    }

    @Test
    @DisplayName("supports global symbols only for quote path")
    void shouldSupportOnlyGlobalSymbols() {
        assertTrue(alphaVantageProvider.supportsSymbol("AAPL"));
        assertFalse(alphaVantageProvider.supportsSymbol("005930"));
    }

    @Test
    @DisplayName("returns stock data when API response is valid")
    void shouldGetStockDataSuccessfullyWhenValidResponse() {
        String symbol = "AAPL";
        Map<String, Object> mockResponse = createMockAlphaVantageResponse();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(responseEntity);

        StockPriceDto result = alphaVantageProvider.getMarketData(symbol);

        assertNotNull(result);
        assertEquals(symbol, result.getSymbol());
        assertEquals(150.25, result.getPrice());
        assertEquals(149.50, result.getOpen());
        assertEquals(151.00, result.getHigh());
        assertEquals(148.75, result.getLow());
        assertEquals(149.00, result.getClose());
        assertEquals(1.25, result.getChange());
        assertEquals(0.84, result.getChangePercent(), 0.01);
        assertEquals("2024-01-15", result.getLastTradingDay());
        assertEquals("AlphaVantage", result.getProvider());
    }

    @Test
    @DisplayName("throws exception when API call fails")
    void shouldThrowExceptionWhenApiCallFails() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("API call failed"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> alphaVantageProvider.getMarketData("AAPL"));

        assertTrue(exception.getMessage().contains("AlphaVantage API"));
    }

    @Test
    @DisplayName("throws exception when response data is invalid")
    void shouldThrowExceptionWhenInvalidResponseData() {
        Map<String, Object> invalidResponse = new HashMap<>();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(invalidResponse, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(responseEntity);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> alphaVantageProvider.getMarketData("AAPL"));

        assertTrue(exception.getMessage().contains("response"));
    }

    private Map<String, Object> createMockAlphaVantageResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> globalQuote = new HashMap<>();

        globalQuote.put("01. symbol", "AAPL");
        globalQuote.put("02. open", "149.50");
        globalQuote.put("03. high", "151.00");
        globalQuote.put("04. low", "148.75");
        globalQuote.put("05. price", "150.25");
        globalQuote.put("07. latest trading day", "2024-01-15");
        globalQuote.put("08. previous close", "149.00");
        globalQuote.put("09. change", "1.25");
        globalQuote.put("10. change percent", "0.84%");

        response.put("Global Quote", globalQuote);
        return response;
    }
}
