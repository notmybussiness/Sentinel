package com.pjsent.sentinel.market.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.config.TestSecurityConfig;
import com.pjsent.sentinel.market.dto.MarketIndexDto;
import com.pjsent.sentinel.market.dto.SearchResultDto;
import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.MarketDataService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MarketDataController 단위 테스트
 */
@WebMvcTest(MarketDataController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-jwt-secret-for-market-controller-test",
    "kakao.oauth.client-id=test-market-controller-client-id",
    "kakao.oauth.client-secret=test-market-controller-client-secret",
    "kakao.oauth.redirect-uri=http://localhost:8080/test/callback",
    "stock.market.alphavantage.api-key=test-alphavantage-key",
    "stock.market.finnhub.api-key=test-finnhub-key"
})
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MarketDataService marketDataService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private KakaoOAuthService kakaoOAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    private StockPriceDto stockPriceDto;
    private MarketIndexDto marketIndexDto;
    private SearchResultDto searchResultDto;

    @BeforeEach
    void setUp() {
        stockPriceDto = StockPriceDto.builder()
                .symbol("AAPL")
                .price(150.00)
                .open(148.50)
                .high(151.00)
                .low(148.00)
                .close(150.00)
                .change(1.50)
                .changePercent(1.01)
                .lastTradingDay("2024-01-15")
                .timeStamp(LocalDateTime.now())
                .provider("AlphaVantage")
                .build();

        marketIndexDto = MarketIndexDto.builder()
                .symbol("^GSPC")
                .name("S&P 500")
                .value(4500.00)
                .change(25.50)
                .changePercent(0.57)
                .timestamp(LocalDateTime.now())
                .build();

        searchResultDto = new SearchResultDto(
                "AAPL",
                "Apple Inc.",
                "NASDAQ",
                "Equity"
        );
    }

    @Test
    @DisplayName("단일 주식 가격 조회 성공")
    void should_ReturnStockPrice_When_GetStockPrice() throws Exception {
        // Given
        when(marketDataService.getStockPrice("AAPL")).thenReturn(stockPriceDto);

        // When & Then
        mockMvc.perform(get("/api/v1/market/price/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.price").value(150.00))
                .andExpect(jsonPath("$.change").value(1.50))
                .andExpect(jsonPath("$.changePercent").value(1.01));

        verify(marketDataService, times(1)).getStockPrice("AAPL");
    }

    @Test
    @DisplayName("단일 주식 가격 조회 실패 - 잘못된 심볼")
    void should_ReturnBadRequest_When_InvalidSymbol() throws Exception {
        // Given
        when(marketDataService.getStockPrice("INVALID"))
                .thenThrow(new IllegalArgumentException("Invalid symbol"));

        // When & Then
        mockMvc.perform(get("/api/v1/market/price/INVALID"))
                .andExpect(status().isBadRequest());

        verify(marketDataService, times(1)).getStockPrice("INVALID");
    }

    @Test
    @DisplayName("여러 주식 가격 조회 성공 (Query Parameter)")
    void should_ReturnMultipleStockPrices_When_GetStockPricesWithQuery() throws Exception {
        // Given
        List<StockPriceDto> prices = Arrays.asList(stockPriceDto);
        when(marketDataService.getStockPrices(anyList())).thenReturn(prices);

        // When & Then
        mockMvc.perform(get("/api/v1/market/prices")
                        .param("symbols", "AAPL,MSFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        verify(marketDataService, times(1)).getStockPrices(anyList());
    }

    @Test
    @DisplayName("배치 주식 가격 조회 성공 (Request Body)")
    void should_ReturnBatchPrices_When_ValidRequest() throws Exception {
        // Given
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
        List<StockPriceDto> prices = Arrays.asList(stockPriceDto);
        when(marketDataService.getStockPrices(symbols)).thenReturn(prices);

        // When & Then
        mockMvc.perform(post("/api/v1/market/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(symbols)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        verify(marketDataService, times(1)).getStockPrices(symbols);
    }

    @Test
    @DisplayName("배치 주식 가격 조회 실패 - 빈 목록")
    void should_ReturnBadRequest_When_EmptySymbolList() throws Exception {
        // Given
        List<String> emptySymbols = Arrays.asList();

        // When & Then
        mockMvc.perform(post("/api/v1/market/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptySymbols)))
                .andExpect(status().isBadRequest());

        verify(marketDataService, never()).getStockPrices(anyList());
    }

    @Test
    @DisplayName("배치 주식 가격 조회 실패 - 너무 많은 심볼 (>50)")
    void should_ReturnBadRequest_When_TooManySymbols() throws Exception {
        // Given
        List<String> tooManySymbols = new java.util.ArrayList<>();
        for (int i = 0; i < 51; i++) {
            tooManySymbols.add("SYMBOL" + i);
        }

        // When & Then
        mockMvc.perform(post("/api/v1/market/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooManySymbols)))
                .andExpect(status().isBadRequest());

        verify(marketDataService, never()).getStockPrices(anyList());
    }

    @Test
    @DisplayName("주요 시장 지수 조회 성공")
    void should_ReturnMarketIndices_When_GetMarketIndices() throws Exception {
        // Given
        List<MarketIndexDto> indices = Arrays.asList(marketIndexDto);
        when(marketDataService.getMarketIndices()).thenReturn(indices);

        // When & Then
        mockMvc.perform(get("/api/v1/market/indices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].symbol").value("^GSPC"))
                .andExpect(jsonPath("$[0].name").value("S&P 500"))
                .andExpect(jsonPath("$[0].value").value(4500.00));

        verify(marketDataService, times(1)).getMarketIndices();
    }

    @Test
    @DisplayName("주요 시장 지수 조회 실패")
    void should_ReturnInternalServerError_When_GetMarketIndicesFails() throws Exception {
        // Given
        when(marketDataService.getMarketIndices())
                .thenThrow(new RuntimeException("Failed to fetch indices"));

        // When & Then
        mockMvc.perform(get("/api/v1/market/indices"))
                .andExpect(status().isInternalServerError());

        verify(marketDataService, times(1)).getMarketIndices();
    }

    @Test
    @DisplayName("종목 심볼 검색 성공")
    void should_ReturnSearchResults_When_SearchSymbol() throws Exception {
        // Given
        List<SearchResultDto> results = Arrays.asList(searchResultDto);
        when(marketDataService.searchSymbol("apple")).thenReturn(results);

        // When & Then
        mockMvc.perform(get("/api/v1/market/search")
                        .param("query", "apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].name").value("Apple Inc."))
                .andExpect(jsonPath("$[0].exchange").value("NASDAQ"));

        verify(marketDataService, times(1)).searchSymbol("apple");
    }

    @Test
    @DisplayName("종목 심볼 검색 실패 - 빈 검색어")
    void should_ReturnBadRequest_When_EmptyQuery() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/market/search")
                        .param("query", ""))
                .andExpect(status().isBadRequest());

        verify(marketDataService, never()).searchSymbol(anyString());
    }

    @Test
    @DisplayName("종목 심볼 검색 실패 - 공백 검색어")
    void should_ReturnBadRequest_When_BlankQuery() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/market/search")
                        .param("query", "   "))
                .andExpect(status().isBadRequest());

        verify(marketDataService, never()).searchSymbol(anyString());
    }

    @Test
    @DisplayName("서비스 상태 확인 성공")
    void should_ReturnServiceStatus_When_GetServiceStatus() throws Exception {
        // Given
        when(marketDataService.isServiceAvailable()).thenReturn(true);
        when(marketDataService.getProviderStatus()).thenReturn("AlphaVantage (Primary)");

        // When & Then
        mockMvc.perform(get("/api/v1/market/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").value("AlphaVantage (Primary)"));

        verify(marketDataService, times(1)).isServiceAvailable();
        verify(marketDataService, times(1)).getProviderStatus();
    }

    @Test
    @DisplayName("서비스 상태 확인 실패")
    void should_ReturnErrorStatus_When_GetServiceStatusFails() throws Exception {
        // Given
        when(marketDataService.isServiceAvailable())
                .thenThrow(new RuntimeException("Status check failed"));

        // When & Then
        mockMvc.perform(get("/api/v1/market/status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("서비스 상태 확인 실패"));

        verify(marketDataService, times(1)).isServiceAvailable();
    }
}
