package com.pjsent.sentinel.market.service.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.pjsent.sentinel.market.dto.StockPriceDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinnhubProvider 테스트")
class FinnhubProviderTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    @InjectMocks
    private FinnhubProvider finnhubProvider;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(finnhubProvider, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(finnhubProvider, "baseUrl", "https://finnhub.io/api/v1");
        ReflectionTestUtils.setField(finnhubProvider, "enabled", true);
    }
    
    @Test
    @DisplayName("사용 가능한 상태일 때 true를 반환해야 한다")
    void should_ReturnTrue_When_ProviderIsAvailable() {
        // When
        boolean result = finnhubProvider.isAvailable();
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("API 키가 없을 때 false를 반환해야 한다")
    void should_ReturnFalse_When_ApiKeyIsMissing() {
        // Given
        ReflectionTestUtils.setField(finnhubProvider, "apiKey", "");
        
        // When
        boolean result = finnhubProvider.isAvailable();
        
        // Then
        assertFalse(result);
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
    @DisplayName("Time Series와 Historical Data를 지원한다고 반환해야 한다")
    void should_SupportTimeSeriesAndHistoricalData() {
        // When & Then
        assertTrue(finnhubProvider.supportsTimeSeries());
        assertTrue(finnhubProvider.supportsHistoricalData());
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
        assertEquals("Finnhub", result.getProvider());
    }
    
    @Test
    @DisplayName("API가 사용 불가능할 때 예외를 발생시켜야 한다")
    void should_ThrowException_When_ProviderIsNotAvailable() {
        // Given
        ReflectionTestUtils.setField(finnhubProvider, "enabled", false);
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            finnhubProvider.getMarketData("AAPL");
        });
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
    
    @Test
    @DisplayName("null 값이 포함된 응답을 올바르게 처리해야 한다")
    void should_HandleNullValues_When_ResponseContainsNulls() {
        // Given
        String symbol = "AAPL";
        Map<String, Object> responseWithNulls = createMockFinnhubResponseWithNulls();
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseWithNulls, HttpStatus.OK);
        
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(responseEntity);
        
        // When
        StockPriceDto result = finnhubProvider.getMarketData(symbol);
        
        // Then
        assertNotNull(result);
        assertEquals(symbol, result.getSymbol());
        assertEquals(150.25, result.getPrice());
        assertEquals(0.0, result.getOpen()); // null 값은 0.0으로 처리
        assertEquals(0.0, result.getHigh());
        assertEquals(0.0, result.getLow());
        assertEquals(0.0, result.getClose());
        assertEquals(150.25, result.getChange()); // currentPrice - previousClose (150.25 - 0.0)
        assertEquals(0.0, result.getChangePercent()); // previousClose가 0이므로 0.0
    }
    
    private Map<String, Object> createMockFinnhubResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("c", 150.25);  // current price
        response.put("o", 149.50);  // open
        response.put("h", 151.00);  // high
        response.put("l", 148.75);  // low
        response.put("pc", 149.00); // previous close
        response.put("t", 1705276800L); // timestamp
        return response;
    }
    
    private Map<String, Object> createMockFinnhubResponseWithNulls() {
        Map<String, Object> response = new HashMap<>();
        response.put("c", 150.25);  // current price
        response.put("o", null);    // open (null)
        response.put("h", null);    // high (null)
        response.put("l", null);    // low (null)
        response.put("pc", null);   // previous close (null)
        response.put("t", null);    // timestamp (null)
        return response;
    }
}
