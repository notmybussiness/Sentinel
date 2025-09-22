package com.pjsent.sentinel.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.portfolio.dto.*;
import com.pjsent.sentinel.portfolio.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PortfolioController 테스트
 */
@WebMvcTest(PortfolioController.class)
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

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
                .name("테스트 포트폴리오")
                .description("테스트용 포트폴리오")
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
    @DisplayName("포트폴리오 목록 조회 성공")
    void should_ReturnPortfolioList_When_GetPortfolios() throws Exception {
        // Given
        when(portfolioService.getPortfoliosByUserId(userId))
                .thenReturn(List.of(portfolioDto));

        // When & Then
        mockMvc.perform(get("/api/v1/portfolios")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(portfolioId))
                .andExpect(jsonPath("$[0].name").value("테스트 포트폴리오"))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    @DisplayName("특정 포트폴리오 조회 성공")
    void should_ReturnPortfolio_When_GetPortfolio() throws Exception {
        // Given
        when(portfolioService.getPortfolioById(portfolioId, userId))
                .thenReturn(portfolioDto);

        // When & Then
        mockMvc.perform(get("/api/v1/portfolios/{portfolioId}", portfolioId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(portfolioId))
                .andExpect(jsonPath("$.name").value("테스트 포트폴리오"))
                .andExpect(jsonPath("$.totalValue").value(10000));
    }

    @Test
    @DisplayName("포트폴리오 생성 성공")
    void should_CreatePortfolio_When_ValidRequest() throws Exception {
        // Given
        CreatePortfolioRequest request = new CreatePortfolioRequest();
        request.setName("새 포트폴리오");
        request.setDescription("새로운 포트폴리오");

        when(portfolioService.createPortfolio(eq(userId), any(CreatePortfolioRequest.class)))
                .thenReturn(portfolioDto);

        // When & Then
        mockMvc.perform(post("/api/v1/portfolios")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(portfolioId))
                .andExpect(jsonPath("$.name").value("테스트 포트폴리오"));
    }

    @Test
    @DisplayName("포트폴리오 생성 실패 - 유효성 검증 실패")
    void should_ReturnBadRequest_When_InvalidCreateRequest() throws Exception {
        // Given
        CreatePortfolioRequest request = new CreatePortfolioRequest();
        // name이 비어있음

        // When & Then
        mockMvc.perform(post("/api/v1/portfolios")
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("포트폴리오 수정 성공")
    void should_UpdatePortfolio_When_ValidRequest() throws Exception {
        // Given
        UpdatePortfolioRequest request = new UpdatePortfolioRequest();
        request.setName("수정된 포트폴리오");
        request.setDescription("수정된 설명");

        when(portfolioService.updatePortfolio(eq(portfolioId), eq(userId), any(UpdatePortfolioRequest.class)))
                .thenReturn(portfolioDto);

        // When & Then
        mockMvc.perform(put("/api/v1/portfolios/{portfolioId}", portfolioId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(portfolioId));
    }

    @Test
    @DisplayName("포트폴리오 삭제 성공")
    void should_DeletePortfolio_When_ValidPortfolioId() throws Exception {
        // Given
        doNothing().when(portfolioService).deletePortfolio(portfolioId, userId);

        // When & Then
        mockMvc.perform(delete("/api/v1/portfolios/{portfolioId}", portfolioId)
                        .param("userId", userId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("보유 종목 추가 성공")
    void should_AddHolding_When_ValidRequest() throws Exception {
        // Given
        AddHoldingRequest request = new AddHoldingRequest();
        request.setSymbol("AAPL");
        request.setQuantity(new BigDecimal("10"));
        request.setAverageCost(new BigDecimal("150.00"));

        when(portfolioService.addHolding(eq(portfolioId), eq(userId), any(AddHoldingRequest.class)))
                .thenReturn(holdingDto);

        // When & Then
        mockMvc.perform(post("/api/v1/portfolios/{portfolioId}/holdings", portfolioId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    @DisplayName("보유 종목 수정 성공")
    void should_UpdateHolding_When_ValidRequest() throws Exception {
        // Given
        UpdateHoldingRequest request = new UpdateHoldingRequest();
        request.setQuantity(new BigDecimal("20"));
        request.setAverageCost(new BigDecimal("155.00"));

        when(portfolioService.updateHolding(eq(portfolioId), eq(holdingDto.getId()), eq(userId), any(UpdateHoldingRequest.class)))
                .thenReturn(holdingDto);

        // When & Then
        mockMvc.perform(put("/api/v1/portfolios/{portfolioId}/holdings/{holdingId}", 
                        portfolioId, holdingDto.getId())
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"));
    }

    @Test
    @DisplayName("보유 종목 삭제 성공")
    void should_DeleteHolding_When_ValidHoldingId() throws Exception {
        // Given
        doNothing().when(portfolioService).deleteHolding(portfolioId, holdingDto.getId(), userId);

        // When & Then
        mockMvc.perform(delete("/api/v1/portfolios/{portfolioId}/holdings/{holdingId}", 
                        portfolioId, holdingDto.getId())
                        .param("userId", userId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("포트폴리오 재계산 성공")
    void should_RecalculatePortfolio_When_ValidPortfolioId() throws Exception {
        // Given
        when(portfolioService.recalculatePortfolio(portfolioId, userId))
                .thenReturn(portfolioDto);

        // When & Then
        mockMvc.perform(post("/api/v1/portfolios/{portfolioId}/recalculate", portfolioId)
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(portfolioId));
    }
}
