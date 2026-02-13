package com.pjsent.sentinel.portfolio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import com.pjsent.sentinel.common.exception.ResourceNotFoundException;
import com.pjsent.sentinel.config.TestSecurityConfig;
import com.pjsent.sentinel.config.WithMockJwtUser;
import com.pjsent.sentinel.portfolio.dto.AddHoldingRequest;
import com.pjsent.sentinel.portfolio.dto.CreatePortfolioRequest;
import com.pjsent.sentinel.portfolio.dto.PortfolioDto;
import com.pjsent.sentinel.portfolio.dto.PortfolioHoldingDto;
import com.pjsent.sentinel.portfolio.dto.UpdateHoldingRequest;
import com.pjsent.sentinel.portfolio.dto.UpdatePortfolioRequest;
import com.pjsent.sentinel.portfolio.service.PortfolioService;
import com.pjsent.sentinel.user.service.JwtService;
import com.pjsent.sentinel.user.service.KakaoOAuthService;

@WebMvcTest(PortfolioController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=test-jwt-secret-for-controller-test",
        "kakao.oauth.client-id=test-controller-client-id",
        "kakao.oauth.client-secret=test-controller-client-secret",
        "kakao.oauth.redirect-uri=http://localhost:8080/test/callback",
        "stock.market.alphavantage.api-key=test-controller-key",
        "stock.market.finnhub.api-key=test-controller-key" })
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private KakaoOAuthService kakaoOAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    private Long userId;
    private Long portfolioId;
    private PortfolioDto portfolioDto;
    private PortfolioHoldingDto holdingDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        portfolioId = 1L;

        portfolioDto = PortfolioDto.builder()
                .id(portfolioId)
                .userId(userId)
                .name("test-portfolio")
                .description("test-description")
                .totalValue(BigDecimal.valueOf(10000))
                .totalCost(BigDecimal.valueOf(9500))
                .totalGainLoss(BigDecimal.valueOf(500))
                .totalGainLossPercent(BigDecimal.valueOf(5.26))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        holdingDto = PortfolioHoldingDto.builder()
                .id(1L)
                .portfolioId(portfolioId)
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .averageCost(new BigDecimal("150.00"))
                .currentPrice(new BigDecimal("160.00"))
                .marketValue(new BigDecimal("1600.00"))
                .totalCost(new BigDecimal("1500.00"))
                .gainLoss(new BigDecimal("100.00"))
                .gainLossPercent(new BigDecimal("6.67"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("returns portfolio list")
    void shouldReturnPortfolioList() throws Exception {
        when(portfolioService.getPortfoliosByUserId(userId)).thenReturn(List.of(portfolioDto));

        mockMvc.perform(get("/api/v1/portfolios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(portfolioId))
                .andExpect(jsonPath("$[0].name").value("test-portfolio"));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("returns single portfolio")
    void shouldReturnPortfolio() throws Exception {
        when(portfolioService.getPortfolioById(portfolioId, userId)).thenReturn(portfolioDto);

        mockMvc.perform(get("/api/v1/portfolios/{portfolioId}", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(portfolioId));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("returns standardized error body when portfolio is not found")
    void shouldReturnStandardizedErrorBodyWhenPortfolioNotFound() throws Exception {
        when(portfolioService.getPortfolioById(portfolioId, userId))
                .thenThrow(new ResourceNotFoundException("portfolio not found"));

        mockMvc.perform(get("/api/v1/portfolios/{portfolioId}", portfolioId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/v1/portfolios/" + portfolioId));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("creates portfolio")
    void shouldCreatePortfolio() throws Exception {
        CreatePortfolioRequest request = new CreatePortfolioRequest();
        request.setName("new-portfolio");
        request.setDescription("new-description");

        when(portfolioService.createPortfolio(eq(userId), any(CreatePortfolioRequest.class))).thenReturn(portfolioDto);

        mockMvc.perform(post("/api/v1/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(portfolioId));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("updates portfolio")
    void shouldUpdatePortfolio() throws Exception {
        UpdatePortfolioRequest request = new UpdatePortfolioRequest();
        request.setName("updated");
        request.setDescription("updated-description");

        when(portfolioService.updatePortfolio(eq(portfolioId), eq(userId), any(UpdatePortfolioRequest.class)))
                .thenReturn(portfolioDto);

        mockMvc.perform(put("/api/v1/portfolios/{portfolioId}", portfolioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(portfolioId));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("deletes portfolio")
    void shouldDeletePortfolio() throws Exception {
        doNothing().when(portfolioService).deletePortfolio(portfolioId, userId);

        mockMvc.perform(delete("/api/v1/portfolios/{portfolioId}", portfolioId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("adds holding")
    void shouldAddHolding() throws Exception {
        AddHoldingRequest request = new AddHoldingRequest();
        request.setSymbol("AAPL");
        request.setQuantity(new BigDecimal("10"));
        request.setAverageCost(new BigDecimal("150.00"));

        when(portfolioService.addHolding(eq(portfolioId), eq(userId), any(AddHoldingRequest.class)))
                .thenReturn(holdingDto);

        mockMvc.perform(post("/api/v1/portfolios/{portfolioId}/holdings", portfolioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("updates holding")
    void shouldUpdateHolding() throws Exception {
        UpdateHoldingRequest request = new UpdateHoldingRequest();
        request.setQuantity(new BigDecimal("20"));
        request.setAverageCost(new BigDecimal("155.00"));

        when(portfolioService.updateHolding(eq(portfolioId), eq(holdingDto.getId()), eq(userId),
                any(UpdateHoldingRequest.class))).thenReturn(holdingDto);

        mockMvc.perform(put("/api/v1/portfolios/{portfolioId}/holdings/{holdingId}", portfolioId, holdingDto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("deletes holding")
    void shouldDeleteHolding() throws Exception {
        doNothing().when(portfolioService).deleteHolding(portfolioId, holdingDto.getId(), userId);

        mockMvc.perform(delete("/api/v1/portfolios/{portfolioId}/holdings/{holdingId}", portfolioId, holdingDto.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("recalculates portfolio")
    void shouldRecalculatePortfolio() throws Exception {
        when(portfolioService.recalculatePortfolio(portfolioId, userId)).thenReturn(portfolioDto);

        mockMvc.perform(post("/api/v1/portfolios/{portfolioId}/recalculate", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(portfolioId));
    }
}
