package com.pjsent.sentinel.market.service.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

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
    "stock.market.finnhub.api-key=test-api-key",
    "stock.market.finnhub.base-url=https://finnhub.io/api/v1",
    "stock.market.finnhub.enabled=true"
})
@DisplayName("FinnhubProvider 통합 테스트")
class FinnhubProviderIntegrationTest {
    
    @MockBean
    private RestTemplate restTemplate;
    
    @Autowired
    private FinnhubProvider finnhubProvider;
    
    @Test
    @DisplayName("사용 가능한 상태일 때 true를 반환해야 한다")
    void should_ReturnTrue_When_ProviderIsAvailable() {
        // When
        boolean result = finnhubProvider.isAvailable();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("프로바이더 이름을 올바르게 반환해야 한다")
    void should_ReturnCorrectProviderName() {
        // When
        String providerName = finnhubProvider.getProviderName();
        
        // Then
        assertEquals("Finnhub", providerName);
    }
    
    @Test
    @DisplayName("유효한 응답으로 주식 데이터를 성공적으로 가져와야 한다")
    void should_GetStockDataSuccessfully_When_ValidResponse() {
        // Given
        String symbol = "AAPL";
        Map<String, Object> mockResponse = createMockFinnhubResponse();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(responseEntity);
        
        // When
        StockPriceDto result = finnhubProvider.getMarketData(symbol);
        
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
        assertEquals("Finnhub", result.getProvider());
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
            finnhubProvider.getMarketData(symbol);
        });
        
        assertTrue(exception.getMessage().contains("Finnhub API 호출 실패"));
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
            finnhubProvider.getMarketData(symbol);
        });
        
        assertTrue(exception.getMessage().contains("응답 데이터 파싱 실패"));
    }
    
    private Map<String, Object> createMockFinnhubResponse() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("c", 150.25);  // current price
        response.put("o", 149.50);  // open
        response.put("h", 151.00);  // high
        response.put("l", 148.75);  // low
        response.put("pc", 149.00); // previous close
        response.put("d", 1.25);    // change
        response.put("dp", 0.84);   // change percent
        response.put("t", 1705276800); // timestamp
        
        return response;
    }
}
