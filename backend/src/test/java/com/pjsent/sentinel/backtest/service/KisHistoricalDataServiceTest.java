package com.pjsent.sentinel.backtest.service;

import com.pjsent.sentinel.backtest.dto.HistoricalPriceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KIS Historical Data Service 테스트")
class KisHistoricalDataServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private KisHistoricalDataService kisHistoricalDataService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kisHistoricalDataService, "baseUrl", "https://openapi.koreainvestment.com:9443");
        ReflectionTestUtils.setField(kisHistoricalDataService, "appKey", "test-app-key");
        ReflectionTestUtils.setField(kisHistoricalDataService, "appSecret", "test-app-secret");
    }

    @Test
    @DisplayName("토큰 발급 후 일봉 데이터 조회 성공")
    void getHistoricalPrices_success() {
        // Given: 토큰 발급 응답
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-token");
        tokenResponse.put("expires_in", 86400);

        when(restTemplate.exchange(
                contains("/oauth2/tokenP"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(tokenResponse));

        // Given: 일봉 데이터 응답
        Map<String, Object> dailyPriceResponse = new HashMap<>();
        dailyPriceResponse.put("rt_cd", "0");
        dailyPriceResponse.put("output2", List.of(
                Map.of(
                        "stck_bsop_date", "20231215",
                        "stck_oprc", "70000",
                        "stck_hgpr", "71000",
                        "stck_lwpr", "69000",
                        "stck_clpr", "70500",
                        "acml_vol", "10000000"
                ),
                Map.of(
                        "stck_bsop_date", "20231214",
                        "stck_oprc", "69000",
                        "stck_hgpr", "70500",
                        "stck_lwpr", "68500",
                        "stck_clpr", "70000",
                        "acml_vol", "9000000"
                )
        ));

        when(restTemplate.exchange(
                contains("/quotations/inquire-daily-itemchartprice"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(dailyPriceResponse));

        // When
        List<HistoricalPriceData> result = kisHistoricalDataService.getHistoricalPrices(
                "005930",
                LocalDate.of(2023, 12, 14),
                LocalDate.of(2023, 12, 15)
        );

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2023, 12, 14));
        assertThat(result.get(1).getDate()).isEqualTo(LocalDate.of(2023, 12, 15));
        assertThat(result.get(1).getClose()).isEqualTo(70500.0);
    }

    @Test
    @DisplayName("API 오류 응답 시 예외 발생")
    void getHistoricalPrices_apiError() {
        // Given: 토큰 발급 응답
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-token");
        tokenResponse.put("expires_in", 86400);

        when(restTemplate.exchange(
                contains("/oauth2/tokenP"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(tokenResponse));

        // Given: 에러 응답
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("rt_cd", "1");
        errorResponse.put("msg1", "Invalid symbol");

        when(restTemplate.exchange(
                contains("/quotations/inquire-daily-itemchartprice"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(errorResponse));

        // When & Then
        assertThatThrownBy(() -> kisHistoricalDataService.getHistoricalPrices(
                "INVALID",
                LocalDate.of(2023, 12, 1),
                LocalDate.of(2023, 12, 31)
        )).isInstanceOf(RuntimeException.class)
          .hasMessageContaining("KIS API error");
    }

    @Test
    @DisplayName("Fallback: 캐시에서 데이터 반환")
    void getHistoricalPricesFallback_returnsCachedData() {
        // Given
        List<HistoricalPriceData> cachedData = List.of(
                HistoricalPriceData.builder()
                        .date(LocalDate.of(2023, 12, 15))
                        .close(70500.0)
                        .build()
        );

        Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
        when(cacheManager.getCache("kisHistoricalData")).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(cachedData);

        // When
        List<HistoricalPriceData> result = kisHistoricalDataService.getHistoricalPricesFallback(
                "005930",
                LocalDate.of(2023, 12, 1),
                LocalDate.of(2023, 12, 31),
                new RuntimeException("API error")
        );

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClose()).isEqualTo(70500.0);
    }

    @Test
    @DisplayName("Fallback: 캐시 없으면 빈 리스트 반환")
    void getHistoricalPricesFallback_returnsEmptyListWhenNoCachedData() {
        // Given
        when(cacheManager.getCache("kisHistoricalData")).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(null);

        // When
        List<HistoricalPriceData> result = kisHistoricalDataService.getHistoricalPricesFallback(
                "005930",
                LocalDate.of(2023, 12, 1),
                LocalDate.of(2023, 12, 31),
                new RuntimeException("API error")
        );

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isApiAvailable: API 키가 설정되어 있으면 true")
    void isApiAvailable_returnsTrueWhenConfigured() {
        assertThat(kisHistoricalDataService.isApiAvailable()).isTrue();
    }

    @Test
    @DisplayName("isApiAvailable: API 키가 없으면 false")
    void isApiAvailable_returnsFalseWhenNotConfigured() {
        ReflectionTestUtils.setField(kisHistoricalDataService, "appKey", null);
        assertThat(kisHistoricalDataService.isApiAvailable()).isFalse();
    }
}
