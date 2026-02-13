package com.pjsent.sentinel.market.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.config.TestSecurityConfig;
import com.pjsent.sentinel.market.dto.MarketIndexDto;
import com.pjsent.sentinel.market.dto.SearchResultDto;
import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.MarketDataService;
import com.pjsent.sentinel.user.service.JwtService;
import com.pjsent.sentinel.user.service.KakaoOAuthService;

@WebMvcTest(MarketDataController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=test-jwt-secret-for-market-controller-test",
        "kakao.oauth.client-id=test-market-controller-client-id",
        "kakao.oauth.client-secret=test-market-controller-client-secret",
        "kakao.oauth.redirect-uri=http://localhost:8080/test/callback",
        "stock.market.alphavantage.api-key=test-alphavantage-key",
        "stock.market.finnhub.api-key=test-finnhub-key" })
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

        searchResultDto = new SearchResultDto("AAPL", "Apple Inc.", "NASDAQ", "Equity");
    }

    @Test
    @DisplayName("returns stock price")
    void shouldReturnStockPrice() throws Exception {
        when(marketDataService.getStockPrice("AAPL")).thenReturn(stockPriceDto);

        mockMvc.perform(get("/api/v1/market/price/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.price").value(150.00));

        verify(marketDataService, times(1)).getStockPrice("AAPL");
    }

    @Test
    @DisplayName("returns standardized 400 error for invalid symbol")
    void shouldReturnBadRequestErrorBodyForInvalidSymbol() throws Exception {
        when(marketDataService.getStockPrice("INVALID")).thenThrow(new IllegalArgumentException("Invalid symbol"));

        mockMvc.perform(get("/api/v1/market/price/INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/market/price/INVALID"));

        verify(marketDataService, times(1)).getStockPrice("INVALID");
    }

    @Test
    @DisplayName("returns prices from query endpoint")
    void shouldReturnPricesWhenQueryEndpointIsUsed() throws Exception {
        List<StockPriceDto> prices = Arrays.asList(stockPriceDto);
        when(marketDataService.getStockPrices(anyList())).thenReturn(prices);

        mockMvc.perform(get("/api/v1/market/prices").param("symbols", "AAPL,MSFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        verify(marketDataService, times(1)).getStockPrices(anyList());
    }

    @Test
    @DisplayName("returns prices from batch endpoint")
    void shouldReturnBatchPricesWhenBodyEndpointIsUsed() throws Exception {
        List<String> symbols = Arrays.asList("AAPL", "MSFT", "GOOGL");
        List<StockPriceDto> prices = Arrays.asList(stockPriceDto);
        when(marketDataService.getStockPrices(symbols)).thenReturn(prices);

        mockMvc.perform(post("/api/v1/market/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(symbols)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        verify(marketDataService, times(1)).getStockPrices(symbols);
    }

    @Test
    @DisplayName("returns 400 when batch symbols are empty")
    void shouldReturnBadRequestWhenBatchSymbolsAreEmpty() throws Exception {
        List<String> emptySymbols = List.of();

        mockMvc.perform(post("/api/v1/market/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptySymbols)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(marketDataService, never()).getStockPrices(anyList());
    }

    @Test
    @DisplayName("returns 400 when batch symbols exceed limit")
    void shouldReturnBadRequestWhenTooManyBatchSymbols() throws Exception {
        List<String> tooManySymbols = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            tooManySymbols.add("SYMBOL" + i);
        }

        mockMvc.perform(post("/api/v1/market/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tooManySymbols)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(marketDataService, never()).getStockPrices(anyList());
    }

    @Test
    @DisplayName("returns stock price from explicit refresh endpoint")
    void shouldReturnStockPriceWhenRefreshEndpointIsUsed() throws Exception {
        when(marketDataService.refreshStockPriceAndPublish("AAPL")).thenReturn(stockPriceDto);

        mockMvc.perform(post("/api/v1/market/price/AAPL/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.provider").value("AlphaVantage"));

        verify(marketDataService, times(1)).refreshStockPriceAndPublish("AAPL");
    }

    @Test
    @DisplayName("returns market indices")
    void shouldReturnMarketIndices() throws Exception {
        when(marketDataService.getMarketIndices()).thenReturn(Arrays.asList(marketIndexDto));

        mockMvc.perform(get("/api/v1/market/indices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("^GSPC"))
                .andExpect(jsonPath("$[0].name").value("S&P 500"));

        verify(marketDataService, times(1)).getMarketIndices();
    }

    @Test
    @DisplayName("returns standardized 500 error when market indices fail")
    void shouldReturnInternalServerErrorBodyWhenMarketIndicesFail() throws Exception {
        when(marketDataService.getMarketIndices()).thenThrow(new RuntimeException("Failed to fetch indices"));

        mockMvc.perform(get("/api/v1/market/indices"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value("/api/v1/market/indices"));

        verify(marketDataService, times(1)).getMarketIndices();
    }

    @Test
    @DisplayName("returns search results")
    void shouldReturnSearchResults() throws Exception {
        when(marketDataService.searchSymbol("apple")).thenReturn(Arrays.asList(searchResultDto));

        mockMvc.perform(get("/api/v1/market/search").param("query", "apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));

        verify(marketDataService, times(1)).searchSymbol("apple");
    }

    @Test
    @DisplayName("returns standardized 400 error for empty query")
    void shouldReturnBadRequestErrorBodyForEmptyQuery() throws Exception {
        mockMvc.perform(get("/api/v1/market/search").param("query", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(marketDataService, never()).searchSymbol(anyString());
    }

    @Test
    @DisplayName("returns service status")
    void shouldReturnServiceStatus() throws Exception {
        when(marketDataService.isServiceAvailable()).thenReturn(true);
        when(marketDataService.getProviderStatus()).thenReturn("Finnhub (Primary)");

        mockMvc.perform(get("/api/v1/market/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").value("Finnhub (Primary)"));

        verify(marketDataService, times(1)).isServiceAvailable();
        verify(marketDataService, times(1)).getProviderStatus();
    }

    @Test
    @DisplayName("returns standardized 500 error when status check fails")
    void shouldReturnInternalServerErrorBodyWhenStatusCheckFails() throws Exception {
        when(marketDataService.isServiceAvailable()).thenThrow(new RuntimeException("Status check failed"));

        mockMvc.perform(get("/api/v1/market/status"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value("/api/v1/market/status"));

        verify(marketDataService, times(1)).isServiceAvailable();
    }
}
