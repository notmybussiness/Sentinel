package com.pjsent.sentinel.market.service.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.pjsent.sentinel.market.dto.StockPriceDto;

@SpringBootTest
@TestPropertySource(properties = {
    "stock.market.alphavantage.api-key=test-api-key",
    "stock.market.alphavantage.base-url=https://www.alphavantage.co/query",
    "stock.market.alphavantage.enabled=true"
})
@DisplayName("AlphaVantageProvider 통합 테스트")
class AlphaVantageProviderIntegrationTest {
    
    @MockBean
    private RestTemplate restTemplate;
    
    @Autowired
    private AlphaVantageProvider alphaVantageProvider;
    
    @Test
    @DisplayName("사용 가능한 상태일 때 true를 반환해야 한다")
    void should_ReturnTrue_When_ProviderIsAvailable() {
        // When
        boolean result = alphaVantageProvider.isAvailable();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("프로바이더 이름을 올바르게 반환해야 한다")
    void should_ReturnCorrectProviderName() {
        // When
        String providerName = alphaVantageProvider.getProviderName();
        
        // Then
        assertEquals("AlphaVantage", providerName);
    }
    
    @Test
    @DisplayName("유효한 응답으로 주식 데이터를 성공적으로 가져와야 한다")
    void should_GetStockDataSuccessfully_When_ValidResponse() {
        // Given
        String symbol = "AAPL";
        Map<String, Object> mockResponse = createMockAlphaVantageResponse();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(responseEntity);
        
        // When
        StockPriceDto result = alphaVantageProvider.getMarketData(symbol);
        
        // Then
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
    @DisplayName("API 호출 실패 시 예외를 발생시켜야 한다")
    void should_ThrowException_When_ApiCallFails() {
        // Given
        String symbol = "AAPL";
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("API 호출 실패"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            alphaVantageProvider.getMarketData(symbol);
        });
        
        assertTrue(exception.getMessage().contains("AlphaVantage API 호출 실패"));
    }
    
    @Test
    @DisplayName("유효하지 않은 응답 데이터 시 예외를 발생시켜야 한다")
    void should_ThrowException_When_InvalidResponseData() {
        // Given
        String symbol = "AAPL";
        Map<String, Object> invalidResponse = new HashMap<>();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(invalidResponse, HttpStatus.OK);
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(responseEntity);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            alphaVantageProvider.getMarketData(symbol);
        });
        
        assertTrue(exception.getMessage().contains("응답 데이터 파싱 실패"));
    }
    
    private Map<String, Object> createMockAlphaVantageResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> globalQuote = new HashMap<>();
        
        globalQuote.put("01. symbol", "AAPL");
        globalQuote.put("02. open", "149.50");
        globalQuote.put("03. high", "151.00");
        globalQuote.put("04. low", "148.75");
        globalQuote.put("05. price", "150.25");
        globalQuote.put("06. volume", "50000000");
        globalQuote.put("07. latest trading day", "2024-01-15");
        globalQuote.put("08. previous close", "149.00");
        globalQuote.put("09. change", "1.25");
        globalQuote.put("10. change percent", "0.84%");
        
        response.put("Global Quote", globalQuote);
        return response;
    }
}
