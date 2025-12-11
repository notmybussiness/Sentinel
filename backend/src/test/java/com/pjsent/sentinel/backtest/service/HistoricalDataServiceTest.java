package com.pjsent.sentinel.backtest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.backtest.dto.HistoricalPriceData;
import com.pjsent.sentinel.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * HistoricalDataService 단위 테스트
 * 과거 가격 데이터 조회 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class HistoricalDataServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private HistoricalDataService historicalDataService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(historicalDataService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(historicalDataService, "baseUrl", "https://www.alphavantage.co/query");
        ReflectionTestUtils.setField(historicalDataService, "apiKey", "test-api-key");
    }

    // ========================================
    // 1. API Integration Tests (5 tests)
    // ========================================

    @Test
    @DisplayName("Successful API call returns parsed data")
    void should_ReturnHistoricalPrices_When_ApiCallSucceeds() {
        // Given
        String validResponse = """
            {
                "Meta Data": {
                    "1. Information": "Daily Prices",
                    "2. Symbol": "AAPL"
                },
                "Time Series (Daily)": {
                    "2023-01-03": {
                        "1. open": "150.00",
                        "2. high": "155.00",
                        "3. low": "149.00",
                        "4. close": "154.00",
                        "5. volume": "1000000"
                    },
                    "2023-01-02": {
                        "1. open": "148.00",
                        "2. high": "152.00",
                        "3. low": "147.00",
                        "4. close": "150.00",
                        "5. volume": "900000"
                    }
                }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(validResponse);

        // When
        List<HistoricalPriceData> result = historicalDataService.getHistoricalPrices(
            "AAPL", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2023, 1, 2)); // Sorted ascending
        assertThat(result.get(0).getClose()).isEqualTo(150.0);
        assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2023, 1, 3));
        assertThat(result.get(1).getClose()).isEqualTo(154.0);
    }

    @Test
    @DisplayName("API error message throws BusinessException")
    void should_ThrowBusinessException_When_ApiReturnsError() {
        // Given
        String errorResponse = """
            {
                "Error Message": "Invalid API call. Please retry or visit the documentation."
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(errorResponse);

        // When & Then
        assertThatThrownBy(() -> historicalDataService.getHistoricalPrices(
            "INVALID", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Error parsing historical data");
    }

    @Test
    @DisplayName("Rate limit exceeded (Note field) throws exception")
    void should_ThrowBusinessException_When_RateLimitExceeded() {
        // Given
        String rateLimitResponse = """
            {
                "Note": "Thank you for using Alpha Vantage! Our standard API call frequency is 5 calls per minute."
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(rateLimitResponse);

        // When & Then
        assertThatThrownBy(() -> historicalDataService.getHistoricalPrices(
            "AAPL", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Error parsing historical data");
    }

    @Test
    @DisplayName("RestClientException wrapped as BusinessException")
    void should_ThrowBusinessException_When_RestClientFails() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenThrow(new RestClientException("Connection timeout"));

        // When & Then
        assertThatThrownBy(() -> historicalDataService.getHistoricalPrices(
            "AAPL", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Failed to fetch historical data");
    }

    @Test
    @DisplayName("Invalid JSON throws BusinessException")
    void should_ThrowBusinessException_When_InvalidJsonResponse() {
        // Given
        String invalidJson = "{ invalid json }";

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(invalidJson);

        // When & Then
        assertThatThrownBy(() -> historicalDataService.getHistoricalPrices(
            "AAPL", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Error parsing historical data");
    }

    // ========================================
    // 2. Parsing Tests (6 tests)
    // ========================================

    @Test
    @DisplayName("Parse AlphaVantage JSON correctly")
    void should_ParseHistoricalData_When_ValidResponse() {
        // Given
        String validResponse = """
            {
                "Time Series (Daily)": {
                    "2023-01-05": {
                        "1. open": "160.00",
                        "2. high": "162.00",
                        "3. low": "159.00",
                        "4. close": "161.50",
                        "5. volume": "1200000"
                    }
                }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(validResponse);

        // When
        List<HistoricalPriceData> result = historicalDataService.getHistoricalPrices(
            "AAPL", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 10));

        // Then
        assertThat(result).hasSize(1);
        HistoricalPriceData data = result.get(0);
        assertThat(data.getDate()).isEqualTo(LocalDate.of(2023, 1, 5));
        assertThat(data.getOpen()).isEqualTo(160.0);
        assertThat(data.getHigh()).isEqualTo(162.0);
        assertThat(data.getLow()).isEqualTo(159.0);
        assertThat(data.getClose()).isEqualTo(161.5);
        assertThat(data.getVolume()).isEqualTo(1200000L);
    }

    @Test
    @DisplayName("Filter dates outside range")
    void should_FilterByDateRange_When_ParsingData() {
        // Given
        String responseWithMultipleDates = """
            {
                "Time Series (Daily)": {
                    "2023-01-10": {
                        "1. open": "150.00",
                        "2. high": "155.00",
                        "3. low": "149.00",
                        "4. close": "154.00",
                        "5. volume": "1000000"
                    },
                    "2023-01-05": {
                        "1. open": "148.00",
                        "2. high": "152.00",
                        "3. low": "147.00",
                        "4. close": "150.00",
                        "5. volume": "900000"
                    },
                    "2022-12-31": {
                        "1. open": "145.00",
                        "2. high": "148.00",
                        "3. low": "144.00",
                        "4. close": "147.00",
                        "5. volume": "800000"
                    }
                }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(responseWithMultipleDates);

        // When: Filter from 2023-01-01 to 2023-01-07
        List<HistoricalPriceData> result = historicalDataService.getHistoricalPrices(
            "AAPL", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 7));

        // Then: Only 2023-01-05 should be included (2023-01-10 and 2022-12-31 excluded)
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2023, 1, 5));
    }

    @Test
    @DisplayName("Sort by date ascending")
    void should_SortByDateAscending_When_ParsingData() {
        // Given
        String unsortedResponse = """
            {
                "Time Series (Daily)": {
                    "2023-01-05": {
                        "1. open": "160.00",
                        "2. high": "162.00",
                        "3. low": "159.00",
                        "4. close": "161.00",
                        "5. volume": "1000000"
                    },
                    "2023-01-02": {
                        "1. open": "150.00",
                        "2. high": "152.00",
                        "3. low": "149.00",
                        "4. close": "151.00",
                        "5. volume": "900000"
                    },
                    "2023-01-03": {
                        "1. open": "152.00",
                        "2. high": "155.00",
                        "3. low": "151.00",
                        "4. close": "154.00",
                        "5. volume": "950000"
                    }
                }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(unsortedResponse);

        // When
        List<HistoricalPriceData> result = historicalDataService.getHistoricalPrices(
            "AAPL", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 10));

        // Then: Should be sorted ascending
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2023, 1, 2));
        assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2023, 1, 3));
        assertThat(result.get(2).getDate()).isEqualTo(LocalDate.of(2023, 1, 5));
    }

    @Test
    @DisplayName("Missing 'Time Series (Daily)' field throws exception")
    void should_ThrowBusinessException_When_MissingTimeSeries() {
        // Given
        String invalidResponse = """
            {
                "Meta Data": {
                    "1. Information": "Daily Prices"
                }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(invalidResponse);

        // When & Then
        assertThatThrownBy(() -> historicalDataService.getHistoricalPrices(
            "AAPL", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Error parsing historical data");
    }

    @Test
    @DisplayName("Parse all OHLCV fields correctly")
    void should_ParseAllFields_When_ValidDayData() {
        // Given
        String completeDataResponse = """
            {
                "Time Series (Daily)": {
                    "2023-01-03": {
                        "1. open": "100.50",
                        "2. high": "105.75",
                        "3. low": "99.25",
                        "4. close": "103.00",
                        "5. volume": "5000000"
                    }
                }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(completeDataResponse);

        // When
        List<HistoricalPriceData> result = historicalDataService.getHistoricalPrices(
            "TEST", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5));

        // Then
        assertThat(result).hasSize(1);
        HistoricalPriceData data = result.get(0);
        assertThat(data.getOpen()).isEqualTo(100.5);
        assertThat(data.getHigh()).isEqualTo(105.75);
        assertThat(data.getLow()).isEqualTo(99.25);
        assertThat(data.getClose()).isEqualTo(103.0);
        assertThat(data.getVolume()).isEqualTo(5000000L);
    }

    @Test
    @DisplayName("Adjusted close uses close price (field 4)")
    void should_UseCloseAsAdjustedClose_When_Parsing() {
        // Given
        String response = """
            {
                "Time Series (Daily)": {
                    "2023-01-03": {
                        "1. open": "100.00",
                        "2. high": "105.00",
                        "3. low": "99.00",
                        "4. close": "103.50",
                        "5. volume": "1000000"
                    }
                }
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class)))
            .thenReturn(response);

        // When
        List<HistoricalPriceData> result = historicalDataService.getHistoricalPrices(
            "TEST", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAdjustedClose()).isEqualTo(103.5); // Same as close
    }

    // ========================================
    // 3. Batch Operations (2 tests)
    // ========================================

    @Test
    @DisplayName("Batch fetch returns map of symbol -> prices")
    void should_ReturnSymbolPriceMap_When_BatchFetch() {
        // Given
        String aaplResponse = """
            {
                "Time Series (Daily)": {
                    "2023-01-03": {
                        "1. open": "150.00",
                        "2. high": "155.00",
                        "3. low": "149.00",
                        "4. close": "154.00",
                        "5. volume": "1000000"
                    }
                }
            }
            """;

        String googlResponse = """
            {
                "Time Series (Daily)": {
                    "2023-01-03": {
                        "1. open": "100.00",
                        "2. high": "105.00",
                        "3. low": "99.00",
                        "4. close": "103.00",
                        "5. volume": "500000"
                    }
                }
            }
            """;

        when(restTemplate.getForObject(contains("AAPL"), eq(String.class)))
            .thenReturn(aaplResponse);
        when(restTemplate.getForObject(contains("GOOGL"), eq(String.class)))
            .thenReturn(googlResponse);

        // When
        Map<String, List<HistoricalPriceData>> result = historicalDataService.getBatchHistoricalPrices(
            List.of("AAPL", "GOOGL"), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get("AAPL")).hasSize(1);
        assertThat(result.get("AAPL").get(0).getClose()).isEqualTo(154.0);
        assertThat(result.get("GOOGL")).hasSize(1);
        assertThat(result.get("GOOGL").get(0).getClose()).isEqualTo(103.0);
    }

    @Test
    @DisplayName("Batch fetch handles partial failures gracefully")
    void should_HandlePartialFailures_When_BatchFetch() {
        // Given
        String aaplResponse = """
            {
                "Time Series (Daily)": {
                    "2023-01-03": {
                        "1. open": "150.00",
                        "2. high": "155.00",
                        "3. low": "149.00",
                        "4. close": "154.00",
                        "5. volume": "1000000"
                    }
                }
            }
            """;

        when(restTemplate.getForObject(contains("AAPL"), eq(String.class)))
            .thenReturn(aaplResponse);
        when(restTemplate.getForObject(contains("INVALID"), eq(String.class)))
            .thenThrow(new RestClientException("Invalid symbol"));

        // When & Then: Should throw exception for any failure
        assertThatThrownBy(() -> historicalDataService.getBatchHistoricalPrices(
            List.of("AAPL", "INVALID"), LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5)))
            .isInstanceOf(BusinessException.class);
    }

    // ========================================
    // 4. Helper Methods (4 tests)
    // ========================================

    @Test
    @DisplayName("getClosePriceOnDate returns exact date match")
    void should_ReturnClosePrice_When_DateMatches() {
        // Given
        List<HistoricalPriceData> prices = List.of(
            createHistoricalPrice(LocalDate.of(2023, 1, 2), 150.0),
            createHistoricalPrice(LocalDate.of(2023, 1, 3), 154.0),
            createHistoricalPrice(LocalDate.of(2023, 1, 4), 158.0)
        );

        // When
        Double result = historicalDataService.getClosePriceOnDate(
            "AAPL", LocalDate.of(2023, 1, 3), prices);

        // Then
        assertThat(result).isEqualTo(154.0);
    }

    @Test
    @DisplayName("getClosePriceOnDate returns null when no match")
    void should_ReturnNull_When_DateNotFound() {
        // Given
        List<HistoricalPriceData> prices = List.of(
            createHistoricalPrice(LocalDate.of(2023, 1, 2), 150.0),
            createHistoricalPrice(LocalDate.of(2023, 1, 4), 158.0)
        );

        // When
        Double result = historicalDataService.getClosePriceOnDate(
            "AAPL", LocalDate.of(2023, 1, 3), prices);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getLatestClosePriceBeforeDate returns most recent price")
    void should_ReturnLatestPrice_When_BeforeDate() {
        // Given
        List<HistoricalPriceData> prices = List.of(
            createHistoricalPrice(LocalDate.of(2023, 1, 2), 150.0),
            createHistoricalPrice(LocalDate.of(2023, 1, 3), 154.0),
            createHistoricalPrice(LocalDate.of(2023, 1, 4), 158.0),
            createHistoricalPrice(LocalDate.of(2023, 1, 5), 160.0)
        );

        // When
        Double result = historicalDataService.getLatestClosePriceBeforeDate(
            LocalDate.of(2023, 1, 4), prices);

        // Then: Should return 158.0 (2023-01-04)
        assertThat(result).isEqualTo(158.0);
    }

    @Test
    @DisplayName("isApiAvailable returns true when API key exists")
    void should_ReturnTrue_When_ApiKeyConfigured() {
        // Given: API key set in setUp()

        // When
        boolean result = historicalDataService.isApiAvailable();

        // Then
        assertThat(result).isTrue();
    }

    // ========================================
    // Helper Methods
    // ========================================

    private HistoricalPriceData createHistoricalPrice(LocalDate date, Double close) {
        return HistoricalPriceData.builder()
            .date(date)
            .open(close - 5.0)
            .high(close + 5.0)
            .low(close - 10.0)
            .close(close)
            .volume(1000000L)
            .adjustedClose(close)
            .build();
    }
}
