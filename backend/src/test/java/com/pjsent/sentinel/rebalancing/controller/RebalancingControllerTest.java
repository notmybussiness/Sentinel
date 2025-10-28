package com.pjsent.sentinel.rebalancing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.config.TestSecurityConfig;
import com.pjsent.sentinel.config.WithMockJwtUser;
import com.pjsent.sentinel.rebalancing.dto.*;
import com.pjsent.sentinel.rebalancing.service.RebalancingService;
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
import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RebalancingController 단위 테스트
 */
@WebMvcTest(RebalancingController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-jwt-secret-for-rebalancing-controller-test",
    "kakao.oauth.client-id=test-rebalancing-controller-client-id",
    "kakao.oauth.client-secret=test-rebalancing-controller-client-secret",
    "kakao.oauth.redirect-uri=http://localhost:8080/test/callback"
})
class RebalancingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RebalancingService rebalancingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private KakaoOAuthService kakaoOAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    private RebalancingRequest rebalancingRequest;
    private RebalancingResponse rebalancingResponse;
    private RebalancingSimulationResponse simulationResponse;

    @BeforeEach
    void setUp() {
        rebalancingRequest = RebalancingRequest.builder()
                .portfolioId(1L)
                .strategy(RebalancingStrategy.EQUAL_WEIGHT)
                .thresholdPercent(5.0)
                .considerTaxes(false)
                .build();

        rebalancingResponse = RebalancingResponse.builder()
                .portfolioId(1L)
                .portfolioName("테스트 포트폴리오")
                .strategy("EQUAL_WEIGHT")
                .currentValue(100000.0)
                .needsRebalancing(true)
                .recommendations(new ArrayList<>())
                .totalTransactionCost(500.0)
                .estimatedTaxImpact(0.0)
                .analyzedAt(LocalDateTime.now())
                .build();

        simulationResponse = RebalancingSimulationResponse.builder()
                .portfolioId(1L)
                .portfolioName("테스트 포트폴리오")
                .beforeRebalancing(null)
                .afterRebalancing(null)
                .transactions(new ArrayList<>())
                .totalTransactionCost(500.0)
                .netChange(-500.0)
                .build();
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("리밸런싱 추천 생성 성공")
    void should_ReturnRecommendations_When_GetRecommendations() throws Exception {
        // Given
        when(rebalancingService.generateRecommendations(anyLong(), any(RebalancingRequest.class)))
                .thenReturn(rebalancingResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/rebalancing/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rebalancingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(1))
                .andExpect(jsonPath("$.portfolioName").value("테스트 포트폴리오"))
                .andExpect(jsonPath("$.strategy").value("EQUAL_WEIGHT"))
                .andExpect(jsonPath("$.needsRebalancing").value(true))
                .andExpect(jsonPath("$.totalTransactionCost").value(500.0));

        verify(rebalancingService, times(1)).generateRecommendations(anyLong(), any(RebalancingRequest.class));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("리밸런싱 추천 생성 실패 - 필수 필드 누락")
    void should_ReturnBadRequest_When_MissingRequiredFields() throws Exception {
        // Given
        RebalancingRequest invalidRequest = RebalancingRequest.builder()
                // portfolioId, strategy 누락
                .thresholdPercent(5.0)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/rebalancing/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(rebalancingService, never()).generateRecommendations(anyLong(), any(RebalancingRequest.class));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("리밸런싱 추천 생성 실패 - 잘못된 threshold")
    void should_ReturnBadRequest_When_InvalidThreshold() throws Exception {
        // Given
        RebalancingRequest invalidRequest = RebalancingRequest.builder()
                .portfolioId(1L)
                .strategy(RebalancingStrategy.EQUAL_WEIGHT)
                .thresholdPercent(150.0) // 100을 초과
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/rebalancing/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(rebalancingService, never()).generateRecommendations(anyLong(), any(RebalancingRequest.class));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("리밸런싱 시뮬레이션 성공")
    void should_ReturnSimulationResult_When_SimulateRebalancing() throws Exception {
        // Given
        when(rebalancingService.simulateRebalancing(anyLong(), any(RebalancingRequest.class)))
                .thenReturn(simulationResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/rebalancing/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rebalancingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(1))
                .andExpect(jsonPath("$.portfolioName").value("테스트 포트폴리오"))
                .andExpect(jsonPath("$.totalTransactionCost").value(500.0))
                .andExpect(jsonPath("$.netChange").value(-500.0));

        verify(rebalancingService, times(1)).simulateRebalancing(anyLong(), any(RebalancingRequest.class));
    }

    @Test
    @DisplayName("지원하는 리밸런싱 전략 목록 조회 성공")
    void should_ReturnStrategies_When_GetStrategies() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/rebalancing/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategies").isArray())
                .andExpect(jsonPath("$.strategies.length()").value(RebalancingStrategy.values().length));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("리밸런싱 추천 생성 실패 - 서비스 에러")
    void should_ReturnInternalServerError_When_RecommendationFails() throws Exception {
        // Given
        when(rebalancingService.generateRecommendations(anyLong(), any(RebalancingRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/v1/rebalancing/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rebalancingRequest)))
                .andExpect(status().isInternalServerError());

        verify(rebalancingService, times(1)).generateRecommendations(anyLong(), any(RebalancingRequest.class));
    }

    @Test
    @WithMockJwtUser(userId = 1L)
    @DisplayName("리밸런싱 시뮬레이션 실패 - 서비스 에러")
    void should_ReturnInternalServerError_When_SimulationFails() throws Exception {
        // Given
        when(rebalancingService.simulateRebalancing(anyLong(), any(RebalancingRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/v1/rebalancing/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rebalancingRequest)))
                .andExpect(status().isInternalServerError());

        verify(rebalancingService, times(1)).simulateRebalancing(anyLong(), any(RebalancingRequest.class));
    }
}
