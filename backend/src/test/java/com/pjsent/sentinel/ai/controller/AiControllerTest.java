package com.pjsent.sentinel.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.ai.dto.PortfolioAnalysisRequest;
import com.pjsent.sentinel.ai.dto.PortfolioAnalysisResponse;
import com.pjsent.sentinel.ai.service.GeminiService;
import com.pjsent.sentinel.ai.service.PortfolioAnalysisService;
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

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AiController 단위 테스트
 */
@WebMvcTest(AiController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-jwt-secret-for-ai-controller-test",
    "kakao.oauth.client-id=test-ai-controller-client-id",
    "kakao.oauth.client-secret=test-ai-controller-client-secret",
    "kakao.oauth.redirect-uri=http://localhost:8080/test/callback",
    "ai.gemini.api-key=test-gemini-key"
})
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioAnalysisService portfolioAnalysisService;

    @MockBean
    private GeminiService geminiService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private KakaoOAuthService kakaoOAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    private PortfolioAnalysisRequest analysisRequest;
    private PortfolioAnalysisResponse analysisResponse;

    @BeforeEach
    void setUp() {
        analysisRequest = PortfolioAnalysisRequest.builder()
                .portfolioId(1L)
                .analysisType(PortfolioAnalysisRequest.AnalysisType.OVERVIEW)
                .includeMarketContext(true)
                .build();

        analysisResponse = PortfolioAnalysisResponse.builder()
                .summary("포트폴리오가 잘 다각화되어 있으며, 리스크는 중간 수준입니다.")
                .insights(Arrays.asList(
                    "기술주 비중이 높습니다 (40%)",
                    "채권 편입을 고려해보세요",
                    "현금 비중이 적절합니다"
                ))
                .recommendations(Arrays.asList(
                    "채권 ETF 10% 편입",
                    "배당주 추가 고려",
                    "리밸런싱 권장"
                ))
                .riskLevel(PortfolioAnalysisResponse.RiskLevel.MEDIUM)
                .diversificationScore(75.5)
                .analysisType(PortfolioAnalysisRequest.AnalysisType.OVERVIEW)
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("포트폴리오 AI 분석 성공")
    void should_ReturnAnalysisResult_When_AnalyzePortfolio() throws Exception {
        // Given
        when(portfolioAnalysisService.analyzePortfolio(any(PortfolioAnalysisRequest.class)))
                .thenReturn(analysisResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analysisRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.insights").isArray())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.diversificationScore").value(75.5))
                .andExpect(jsonPath("$.analysisType").value("OVERVIEW"));

        verify(portfolioAnalysisService, times(1)).analyzePortfolio(any(PortfolioAnalysisRequest.class));
    }

    @Test
    @DisplayName("포트폴리오 AI 분석 실패 - 서비스 에러")
    void should_ReturnInternalServerError_When_AnalysisFails() throws Exception {
        // Given
        when(portfolioAnalysisService.analyzePortfolio(any(PortfolioAnalysisRequest.class)))
                .thenThrow(new RuntimeException("Analysis failed"));

        // When & Then
        mockMvc.perform(post("/api/v1/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analysisRequest)))
                .andExpect(status().isInternalServerError());

        verify(portfolioAnalysisService, times(1)).analyzePortfolio(any(PortfolioAnalysisRequest.class));
    }

    @Test
    @DisplayName("AI 서비스 상태 확인 성공 - 사용 가능")
    void should_ReturnAvailableStatus_When_GetAiStatus() throws Exception {
        // Given
        when(geminiService.isAvailable()).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.provider").value("Gemini AI"))
                .andExpect(jsonPath("$.model").value("gemini-pro"))
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.features.length()").value(4));

        verify(geminiService, times(1)).isAvailable();
    }

    @Test
    @DisplayName("AI 서비스 상태 확인 성공 - 사용 불가")
    void should_ReturnUnavailableStatus_When_ServiceUnavailable() throws Exception {
        // Given
        when(geminiService.isAvailable()).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/v1/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.provider").value("Gemini AI"));

        verify(geminiService, times(1)).isAvailable();
    }

    @Test
    @DisplayName("AI 테스트 성공")
    void should_ReturnTestResponse_When_TestAi() throws Exception {
        // Given
        String testMessage = "Hello AI";
        String testResponse = "Hello! How can I help you today?";
        when(geminiService.generateContent(testMessage)).thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/ai/test")
                        .param("message", testMessage))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request").value(testMessage))
                .andExpect(jsonPath("$.response").value(testResponse));

        verify(geminiService, times(1)).generateContent(testMessage);
    }

    @Test
    @DisplayName("AI 테스트 실패")
    void should_ReturnInternalServerError_When_TestAiFails() throws Exception {
        // Given
        String testMessage = "Hello AI";
        when(geminiService.generateContent(testMessage))
                .thenThrow(new RuntimeException("AI service unavailable"));

        // When & Then
        mockMvc.perform(post("/api/v1/ai/test")
                        .param("message", testMessage))
                .andExpect(status().isInternalServerError());

        verify(geminiService, times(1)).generateContent(testMessage);
    }
}
