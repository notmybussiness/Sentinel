package com.pjsent.sentinel.backtest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.backtest.dto.BacktestRequest;
import com.pjsent.sentinel.backtest.dto.BacktestResponse;
import com.pjsent.sentinel.backtest.dto.HistoricalPriceResponse;
import com.pjsent.sentinel.backtest.dto.HistoricalPriceData;
import com.pjsent.sentinel.backtest.dto.PerformanceMetrics;
import com.pjsent.sentinel.backtest.service.BacktestEngine;
import com.pjsent.sentinel.backtest.service.HistoricalDataFacade;
import com.pjsent.sentinel.config.TestSecurityConfig;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BacktestController 단위 테스트
 */
@WebMvcTest(BacktestController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-jwt-secret-for-backtest-controller-test",
    "kakao.oauth.client-id=test-backtest-controller-client-id",
    "kakao.oauth.client-secret=test-backtest-controller-client-secret",
    "kakao.oauth.redirect-uri=http://localhost:8080/test/callback"
})
class BacktestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BacktestEngine backtestEngine;

    @MockBean
    private HistoricalDataFacade historicalDataFacade;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private KakaoOAuthService kakaoOAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    private BacktestRequest backtestRequest;
    private BacktestResponse backtestResponse;

    @BeforeEach
    void setUp() {
        backtestRequest = BacktestRequest.builder()
                .portfolioId(1L)
                .startDate(LocalDate.of(2023, 1, 1))
                .endDate(LocalDate.of(2024, 1, 1))
                .initialCapital(10000.0)
                .rebalanceFrequency(BacktestRequest.RebalanceFrequency.MONTHLY)
                .build();

        PerformanceMetrics performanceMetrics = PerformanceMetrics.builder()
                .totalReturn(15.5)
                .cagr(14.2)
                .sharpeRatio(1.25)
                .sortinoRatio(1.45)
                .maxDrawdown(-8.5)
                .volatility(12.3)
                .winRate(65.0)
                .build();

        backtestResponse = BacktestResponse.builder()
                .portfolioId(1L)
                .portfolioName("테스트 포트폴리오")
                .startDate(LocalDate.of(2023, 1, 1))
                .endDate(LocalDate.of(2024, 1, 1))
                .initialCapital(10000.0)
                .finalValue(11550.0)
                .rebalanceFrequency(BacktestRequest.RebalanceFrequency.MONTHLY)
                .performance(performanceMetrics)
                .equityCurve(new ArrayList<>())
                .rebalanceEvents(new ArrayList<>())
                .holdingsSummary(new ArrayList<>())
                .executedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("백테스팅 실행 성공")
    void should_ReturnBacktestResult_When_RunBacktest() throws Exception {
        // Given
        when(backtestEngine.runBacktest(any(BacktestRequest.class)))
                .thenReturn(backtestResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(backtestRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(1))
                .andExpect(jsonPath("$.portfolioName").value("테스트 포트폴리오"))
                .andExpect(jsonPath("$.initialCapital").value(10000.0))
                .andExpect(jsonPath("$.finalValue").value(11550.0))
                .andExpect(jsonPath("$.performance.totalReturn").value(15.5))
                .andExpect(jsonPath("$.performance.cagr").value(14.2))
                .andExpect(jsonPath("$.performance.sharpeRatio").value(1.25))
                .andExpect(jsonPath("$.performance.maxDrawdown").value(-8.5));

        verify(backtestEngine, times(1)).runBacktest(any(BacktestRequest.class));
    }

    @Test
    @DisplayName("백테스팅 실행 실패 - 시작일이 종료일보다 늦음")
    void should_ReturnBadRequest_When_StartDateAfterEndDate() throws Exception {
        // Given
        BacktestRequest invalidRequest = BacktestRequest.builder()
                .portfolioId(1L)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2023, 1, 1))  // 종료일이 시작일보다 빠름
                .initialCapital(10000.0)
                .rebalanceFrequency(BacktestRequest.RebalanceFrequency.MONTHLY)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(backtestEngine, never()).runBacktest(any(BacktestRequest.class));
    }

    @Test
    @DisplayName("백테스팅 실행 실패 - 필수 필드 누락")
    void should_ReturnBadRequest_When_MissingRequiredFields() throws Exception {
        // Given
        BacktestRequest invalidRequest = BacktestRequest.builder()
                .portfolioId(1L)
                // startDate, endDate, initialCapital 누락
                .rebalanceFrequency(BacktestRequest.RebalanceFrequency.MONTHLY)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(backtestEngine, never()).runBacktest(any(BacktestRequest.class));
    }

    @Test
    @DisplayName("백테스팅 서비스 상태 확인 성공")
    void should_ReturnServiceStatus_When_GetStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/backtest/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.provider").value("AlphaVantage"))
                .andExpect(jsonPath("$.supportedFrequencies").isArray())
                .andExpect(jsonPath("$.supportedFrequencies.length()").value(4))
                .andExpect(jsonPath("$.supportedFrequencies[0]").value("NONE"))
                .andExpect(jsonPath("$.supportedFrequencies[1]").value("MONTHLY"))
                .andExpect(jsonPath("$.supportedFrequencies[2]").value("QUARTERLY"))
                .andExpect(jsonPath("$.supportedFrequencies[3]").value("YEARLY"))
                .andExpect(jsonPath("$.maxDateRange").value("10 years"));
    }

    @Test
    @DisplayName("백테스팅 실행 실패 - 서비스 에러")
    void should_ReturnInternalServerError_When_BacktestFails() throws Exception {
        // Given
        when(backtestEngine.runBacktest(any(BacktestRequest.class)))
                .thenThrow(new RuntimeException("Backtest failed"));

        // When & Then
        mockMvc.perform(post("/api/v1/backtest/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(backtestRequest)))
                .andExpect(status().isInternalServerError());

        verify(backtestEngine, times(1)).runBacktest(any(BacktestRequest.class));
    }
}
