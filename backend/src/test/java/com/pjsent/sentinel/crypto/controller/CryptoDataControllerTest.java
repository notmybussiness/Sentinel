package com.pjsent.sentinel.crypto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.config.TestSecurityConfig;
import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.dto.CryptoSearchResultDto;
import com.pjsent.sentinel.crypto.dto.TrendingCoinDto;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import com.pjsent.sentinel.user.service.JwtService;
import com.pjsent.sentinel.user.service.KakaoOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CryptoDataController 단위 테스트
 */
@WebMvcTest(CryptoDataController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-jwt-secret-for-crypto-controller-test",
    "kakao.oauth.client-id=test-crypto-controller-client-id",
    "kakao.oauth.client-secret=test-crypto-controller-client-secret",
    "kakao.oauth.redirect-uri=http://localhost:8080/test/callback"
})
class CryptoDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CryptoDataService cryptoDataService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private KakaoOAuthService kakaoOAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    private CryptoPriceDto cryptoPriceDto;
    private CryptoSearchResultDto searchResultDto;
    private TrendingCoinDto trendingCoinDto;

    @BeforeEach
    void setUp() {
        cryptoPriceDto = CryptoPriceDto.builder()
                .symbol("BTC")
                .baseCurrency("KRW")
                .marketCode("KRW-BTC")
                .price(50000000.0)
                .openPrice(49000000.0)
                .highPrice(51000000.0)
                .lowPrice(48000000.0)
                .volume(1000.0)
                .change(1000000.0)
                .changePercent(2.04)
                .tradeValue(50000000000.0)
                .timestamp(LocalDateTime.now())
                .provider("Upbit")
                .build();

        searchResultDto = CryptoSearchResultDto.builder()
                .symbol("BTC")
                .name("Bitcoin")
                .koreanName("비트코인")
                .marketCode("KRW-BTC")
                .baseCurrency("KRW")
                .tradeable(true)
                .exchange("Upbit")
                .build();

        trendingCoinDto = TrendingCoinDto.builder()
                .symbol("BTC")
                .name("Bitcoin")
                .koreanName("비트코인")
                .marketCode("KRW-BTC")
                .price(50000000.0)
                .changePercent(2.04)
                .volume(1000.0)
                .tradeValue(50000000000.0)
                .rank(1)
                .baseCurrency("KRW")
                .build();
    }

    @Test
    @DisplayName("단일 암호화폐 가격 조회 성공")
    void should_ReturnCryptoPrice_When_GetCryptoPrice() throws Exception {
        // Given
        when(cryptoDataService.getCryptoPrice("BTC", "KRW")).thenReturn(cryptoPriceDto);

        // When & Then
        mockMvc.perform(get("/api/v1/crypto/price/BTC")
                        .param("baseCurrency", "KRW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.baseCurrency").value("KRW"))
                .andExpect(jsonPath("$.price").value(50000000.0))
                .andExpect(jsonPath("$.changePercent").value(2.04));

        verify(cryptoDataService, times(1)).getCryptoPrice("BTC", "KRW");
    }

    @Test
    @DisplayName("단일 암호화폐 가격 조회 실패 - 잘못된 심볼")
    void should_ReturnBadRequest_When_InvalidSymbol() throws Exception {
        // Given
        when(cryptoDataService.getCryptoPrice("INVALID", "KRW"))
                .thenThrow(new IllegalArgumentException("Invalid symbol"));

        // When & Then
        mockMvc.perform(get("/api/v1/crypto/price/INVALID")
                        .param("baseCurrency", "KRW"))
                .andExpect(status().isBadRequest());

        verify(cryptoDataService, times(1)).getCryptoPrice("INVALID", "KRW");
    }

    @Test
    @DisplayName("배치 암호화폐 가격 조회 성공")
    void should_ReturnBatchPrices_When_ValidRequest() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("symbols", Arrays.asList("BTC", "ETH", "XRP"));
        request.put("baseCurrency", "KRW");

        List<CryptoPriceDto> prices = Arrays.asList(cryptoPriceDto);
        when(cryptoDataService.getBatchCryptoPrices(anyList(), eq("KRW"))).thenReturn(prices);

        // When & Then
        mockMvc.perform(post("/api/v1/crypto/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].symbol").value("BTC"));

        verify(cryptoDataService, times(1)).getBatchCryptoPrices(anyList(), eq("KRW"));
    }

    @Test
    @DisplayName("배치 암호화폐 가격 조회 실패 - 빈 목록")
    void should_ReturnBadRequest_When_EmptySymbolList() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("symbols", Arrays.asList());
        request.put("baseCurrency", "KRW");

        // When & Then
        mockMvc.perform(post("/api/v1/crypto/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cryptoDataService, never()).getBatchCryptoPrices(anyList(), anyString());
    }

    @Test
    @DisplayName("배치 암호화폐 가격 조회 실패 - 너무 많은 심볼 (>100)")
    void should_ReturnBadRequest_When_TooManySymbols() throws Exception {
        // Given
        List<String> tooManySymbols = new java.util.ArrayList<>();
        for (int i = 0; i < 101; i++) {
            tooManySymbols.add("SYMBOL" + i);
        }

        Map<String, Object> request = new HashMap<>();
        request.put("symbols", tooManySymbols);
        request.put("baseCurrency", "KRW");

        // When & Then
        mockMvc.perform(post("/api/v1/crypto/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cryptoDataService, never()).getBatchCryptoPrices(anyList(), anyString());
    }

    @Test
    @DisplayName("암호화폐 검색 성공")
    void should_ReturnSearchResults_When_SearchCrypto() throws Exception {
        // Given
        List<CryptoSearchResultDto> results = Arrays.asList(searchResultDto);
        when(cryptoDataService.searchCrypto("bitcoin")).thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/crypto/search")
                        .param("query", "bitcoin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[0].name").value("Bitcoin"))
                .andExpect(jsonPath("$[0].koreanName").value("비트코인"));

        verify(cryptoDataService, times(1)).searchCrypto("bitcoin");
    }

    @Test
    @DisplayName("암호화폐 검색 실패 - 빈 검색어")
    void should_ReturnBadRequest_When_EmptyQuery() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/crypto/search")
                        .param("query", ""))
                .andExpect(status().isBadRequest());

        verify(cryptoDataService, never()).searchCrypto(anyString());
    }

    @Test
    @DisplayName("트렌딩 암호화폐 조회 성공")
    void should_ReturnTrendingCoins_When_GetTrendingCoins() throws Exception {
        // Given
        List<TrendingCoinDto> trending = Arrays.asList(trendingCoinDto);
        when(cryptoDataService.getTrendingCoins("KRW", 10)).thenReturn(trending);

        // When & Then
        mockMvc.perform(get("/api/v1/crypto/trending")
                        .param("baseCurrency", "KRW")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[0].rank").value(1));

        verify(cryptoDataService, times(1)).getTrendingCoins("KRW", 10);
    }

    @Test
    @DisplayName("트렌딩 암호화폐 조회 실패 - 잘못된 limit (<= 0)")
    void should_ReturnBadRequest_When_InvalidLimitZero() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/crypto/trending")
                        .param("baseCurrency", "KRW")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());

        verify(cryptoDataService, never()).getTrendingCoins(anyString(), anyInt());
    }

    @Test
    @DisplayName("트렌딩 암호화폐 조회 실패 - 잘못된 limit (> 50)")
    void should_ReturnBadRequest_When_InvalidLimitTooLarge() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/crypto/trending")
                        .param("baseCurrency", "KRW")
                        .param("limit", "51"))
                .andExpect(status().isBadRequest());

        verify(cryptoDataService, never()).getTrendingCoins(anyString(), anyInt());
    }

    @Test
    @DisplayName("과거 데이터 조회 성공")
    void should_ReturnHistoricalData_When_GetHistoricalData() throws Exception {
        // Given
        List<CryptoPriceDto> historical = Arrays.asList(cryptoPriceDto);
        when(cryptoDataService.getHistoricalData("BTC", "KRW", 30)).thenReturn(historical);

        // When & Then
        mockMvc.perform(get("/api/v1/crypto/historical/BTC")
                        .param("baseCurrency", "KRW")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].symbol").value("BTC"));

        verify(cryptoDataService, times(1)).getHistoricalData("BTC", "KRW", 30);
    }

    @Test
    @DisplayName("과거 데이터 조회 실패 - 잘못된 days (<= 0)")
    void should_ReturnBadRequest_When_InvalidDaysZero() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/crypto/historical/BTC")
                        .param("baseCurrency", "KRW")
                        .param("days", "0"))
                .andExpect(status().isBadRequest());

        verify(cryptoDataService, never()).getHistoricalData(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("과거 데이터 조회 실패 - 잘못된 days (> 365)")
    void should_ReturnBadRequest_When_InvalidDaysTooLarge() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/crypto/historical/BTC")
                        .param("baseCurrency", "KRW")
                        .param("days", "366"))
                .andExpect(status().isBadRequest());

        verify(cryptoDataService, never()).getHistoricalData(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("서비스 상태 확인 성공")
    void should_ReturnServiceStatus_When_GetServiceStatus() throws Exception {
        // Given
        when(cryptoDataService.isServiceAvailable()).thenReturn(true);
        when(cryptoDataService.getServiceStatus()).thenReturn("Upbit & Binance Available");

        // When & Then
        mockMvc.perform(get("/api/v1/crypto/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").value("Upbit & Binance Available"));

        verify(cryptoDataService, times(1)).isServiceAvailable();
        verify(cryptoDataService, times(1)).getServiceStatus();
    }

    @Test
    @DisplayName("서비스 상태 확인 실패")
    void should_ReturnErrorStatus_When_GetServiceStatusFails() throws Exception {
        // Given
        when(cryptoDataService.isServiceAvailable())
                .thenThrow(new RuntimeException("Status check failed"));

        // When & Then
        mockMvc.perform(get("/api/v1/crypto/status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("서비스 상태 확인 실패"));

        verify(cryptoDataService, times(1)).isServiceAvailable();
    }
}
