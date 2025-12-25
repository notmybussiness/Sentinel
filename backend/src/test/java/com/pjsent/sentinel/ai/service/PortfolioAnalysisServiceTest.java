package com.pjsent.sentinel.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.ai.dto.PortfolioAnalysisRequest;
import com.pjsent.sentinel.ai.dto.PortfolioAnalysisRequest.AnalysisType;
import com.pjsent.sentinel.ai.dto.PortfolioAnalysisResponse;
import com.pjsent.sentinel.common.exception.ResourceNotFoundException;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PortfolioAnalysisService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortfolioAnalysisServiceTest {

    @Mock
    private GeminiService geminiService;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PortfolioAnalysisService portfolioAnalysisService;

    private Portfolio mockPortfolio;
    private PortfolioAnalysisRequest analysisRequest;

    @BeforeEach
    void setUp() {
        mockPortfolio = createMockPortfolio();
        analysisRequest = PortfolioAnalysisRequest.builder()
                .portfolioId(1L)
                .analysisType(AnalysisType.OVERVIEW)
                .build();
    }

    @Nested
    @DisplayName("analyzePortfolio - 포트폴리오 분석")
    class AnalyzePortfolio {

        @Test
        @DisplayName("정상 분석 결과 반환")
        void should_ReturnAnalysisResponse_When_ValidRequest() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(mockPortfolio));

            String mockAiResponse = """
                {
                    "summary": "이 포트폴리오는 기술주 중심으로 구성되어 있습니다.",
                    "insights": ["기술주 비중이 높음", "성장주 위주 구성", "변동성이 큼"],
                    "recommendations": ["방어주 추가 고려", "채권 비중 확대 검토", "정기 리밸런싱 필요"],
                    "riskLevel": "HIGH",
                    "diversificationScore": 45
                }
                """;
            when(geminiService.generateContent(anyString())).thenReturn(mockAiResponse);

            // When
            PortfolioAnalysisResponse response = portfolioAnalysisService.analyzePortfolio(analysisRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSummary()).contains("기술주");
            assertThat(response.getInsights()).hasSize(3);
            assertThat(response.getRecommendations()).hasSize(3);
            assertThat(response.getRiskLevel()).isEqualTo(PortfolioAnalysisResponse.RiskLevel.HIGH);
            assertThat(response.getDiversificationScore()).isEqualTo(45.0);
            assertThat(response.getAnalyzedAt()).isNotNull();

            verify(portfolioRepository, times(1)).findById(1L);
            verify(geminiService, times(1)).generateContent(anyString());
        }

        @Test
        @DisplayName("포트폴리오 없는 경우 ResourceNotFoundException 발생")
        void should_ThrowException_When_PortfolioNotFound() {
            // Given
            when(portfolioRepository.findById(999L)).thenReturn(Optional.empty());

            PortfolioAnalysisRequest request = PortfolioAnalysisRequest.builder()
                    .portfolioId(999L)
                    .analysisType(AnalysisType.RISK)
                    .build();

            // When & Then
            assertThatThrownBy(() -> portfolioAnalysisService.analyzePortfolio(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Portfolio not found");

            verify(geminiService, never()).generateContent(anyString());
        }

        @Test
        @DisplayName("다양한 분석 타입 처리")
        void should_HandleDifferentAnalysisTypes() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(mockPortfolio));

            String mockAiResponse = """
                {
                    "summary": "리스크 분석 결과입니다.",
                    "insights": ["리스크 요인 1"],
                    "recommendations": ["위험 관리 방안"],
                    "riskLevel": "MEDIUM",
                    "diversificationScore": 60
                }
                """;
            when(geminiService.generateContent(anyString())).thenReturn(mockAiResponse);

            PortfolioAnalysisRequest riskRequest = PortfolioAnalysisRequest.builder()
                    .portfolioId(1L)
                    .analysisType(AnalysisType.RISK)
                    .build();

            // When
            PortfolioAnalysisResponse response = portfolioAnalysisService.analyzePortfolio(riskRequest);

            // Then
            assertThat(response.getAnalysisType()).isEqualTo(AnalysisType.RISK);
            assertThat(response.getRiskLevel()).isEqualTo(PortfolioAnalysisResponse.RiskLevel.MEDIUM);
        }

        @Test
        @DisplayName("JSON 마크다운 형식 응답 파싱")
        void should_ParseMarkdownJsonResponse() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(mockPortfolio));

            String mockAiResponse = """
                분석 결과입니다:

                ```json
                {
                    "summary": "마크다운 JSON 응답입니다.",
                    "insights": ["인사이트1", "인사이트2"],
                    "recommendations": ["추천1", "추천2"],
                    "riskLevel": "LOW",
                    "diversificationScore": 80
                }
                ```

                이상입니다.
                """;
            when(geminiService.generateContent(anyString())).thenReturn(mockAiResponse);

            // When
            PortfolioAnalysisResponse response = portfolioAnalysisService.analyzePortfolio(analysisRequest);

            // Then
            assertThat(response.getSummary()).isEqualTo("마크다운 JSON 응답입니다.");
            assertThat(response.getRiskLevel()).isEqualTo(PortfolioAnalysisResponse.RiskLevel.LOW);
            assertThat(response.getDiversificationScore()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("잘못된 JSON 응답 시 기본값 반환")
        void should_ReturnFallback_When_InvalidJsonResponse() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(mockPortfolio));
            when(geminiService.generateContent(anyString()))
                    .thenReturn("This is not a valid JSON response");

            // When
            PortfolioAnalysisResponse response = portfolioAnalysisService.analyzePortfolio(analysisRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSummary()).contains("could not be completed");
            assertThat(response.getRiskLevel()).isEqualTo(PortfolioAnalysisResponse.RiskLevel.MEDIUM);
            assertThat(response.getDiversificationScore()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("빈 포트폴리오 분석")
        void should_AnalyzeEmptyPortfolio() {
            // Given
            Portfolio emptyPortfolio = mock(Portfolio.class);
            when(emptyPortfolio.getName()).thenReturn("Empty Portfolio");
            when(emptyPortfolio.getDescription()).thenReturn(null);
            when(emptyPortfolio.getTotalValue()).thenReturn(BigDecimal.ZERO);
            when(emptyPortfolio.getTotalCost()).thenReturn(BigDecimal.ZERO);
            when(emptyPortfolio.getTotalGainLoss()).thenReturn(BigDecimal.ZERO);
            when(emptyPortfolio.getTotalGainLossPercent()).thenReturn(BigDecimal.ZERO);
            when(emptyPortfolio.getHoldings()).thenReturn(List.of());

            when(portfolioRepository.findById(2L)).thenReturn(Optional.of(emptyPortfolio));

            String mockAiResponse = """
                {
                    "summary": "빈 포트폴리오입니다.",
                    "insights": ["보유 종목이 없음"],
                    "recommendations": ["투자 시작 권장"],
                    "riskLevel": "LOW",
                    "diversificationScore": 0
                }
                """;
            when(geminiService.generateContent(anyString())).thenReturn(mockAiResponse);

            PortfolioAnalysisRequest request = PortfolioAnalysisRequest.builder()
                    .portfolioId(2L)
                    .analysisType(AnalysisType.OVERVIEW)
                    .build();

            // When
            PortfolioAnalysisResponse response = portfolioAnalysisService.analyzePortfolio(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDiversificationScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Gemini API 실패 시 예외 전파")
        void should_PropagateException_When_GeminiApiFails() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(mockPortfolio));
            when(geminiService.generateContent(anyString()))
                    .thenThrow(new RuntimeException("Gemini API 오류"));

            // When & Then
            assertThatThrownBy(() -> portfolioAnalysisService.analyzePortfolio(analysisRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Gemini API");
        }

        @Test
        @DisplayName("유효하지 않은 riskLevel 처리")
        void should_HandleInvalidRiskLevel() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(mockPortfolio));

            String mockAiResponse = """
                {
                    "summary": "테스트",
                    "insights": [],
                    "recommendations": [],
                    "riskLevel": "EXTREME",
                    "diversificationScore": 50
                }
                """;
            when(geminiService.generateContent(anyString())).thenReturn(mockAiResponse);

            // When
            PortfolioAnalysisResponse response = portfolioAnalysisService.analyzePortfolio(analysisRequest);

            // Then
            assertThat(response.getRiskLevel()).isEqualTo(PortfolioAnalysisResponse.RiskLevel.MEDIUM);
        }

        @Test
        @DisplayName("분석 타입별 프롬프트 생성 확인 - DIVERSIFICATION")
        void should_GenerateCorrectPrompt_For_Diversification() {
            // Given
            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(mockPortfolio));

            String mockAiResponse = """
                {
                    "summary": "다각화 분석",
                    "insights": ["섹터 집중"],
                    "recommendations": ["다각화 필요"],
                    "riskLevel": "MEDIUM",
                    "diversificationScore": 30
                }
                """;
            when(geminiService.generateContent(anyString())).thenReturn(mockAiResponse);

            PortfolioAnalysisRequest diversificationRequest = PortfolioAnalysisRequest.builder()
                    .portfolioId(1L)
                    .analysisType(AnalysisType.DIVERSIFICATION)
                    .build();

            // When
            PortfolioAnalysisResponse response = portfolioAnalysisService.analyzePortfolio(diversificationRequest);

            // Then
            assertThat(response.getAnalysisType()).isEqualTo(AnalysisType.DIVERSIFICATION);
            verify(geminiService).generateContent(argThat(prompt ->
                    prompt.toLowerCase().contains("diversification") || prompt.contains("Analyze")));
        }
    }

    // ========== Helper Methods ==========

    private Portfolio createMockPortfolio() {
        // Create mock holdings
        PortfolioHolding appleHolding = mock(PortfolioHolding.class);
        when(appleHolding.getSymbol()).thenReturn("AAPL");
        when(appleHolding.getAssetType()).thenReturn(PortfolioHolding.AssetType.STOCK);
        when(appleHolding.getQuantity()).thenReturn(new BigDecimal("10"));
        when(appleHolding.getAverageCost()).thenReturn(new BigDecimal("150"));
        when(appleHolding.getCurrentPrice()).thenReturn(new BigDecimal("175"));
        when(appleHolding.getMarketValue()).thenReturn(new BigDecimal("1750"));
        when(appleHolding.getGainLoss()).thenReturn(new BigDecimal("250"));
        when(appleHolding.getGainLossPercent()).thenReturn(new BigDecimal("16.67"));

        PortfolioHolding googleHolding = mock(PortfolioHolding.class);
        when(googleHolding.getSymbol()).thenReturn("GOOGL");
        when(googleHolding.getAssetType()).thenReturn(PortfolioHolding.AssetType.STOCK);
        when(googleHolding.getQuantity()).thenReturn(new BigDecimal("5"));
        when(googleHolding.getAverageCost()).thenReturn(new BigDecimal("130"));
        when(googleHolding.getCurrentPrice()).thenReturn(new BigDecimal("140"));
        when(googleHolding.getMarketValue()).thenReturn(new BigDecimal("700"));
        when(googleHolding.getGainLoss()).thenReturn(new BigDecimal("50"));
        when(googleHolding.getGainLossPercent()).thenReturn(new BigDecimal("7.69"));

        PortfolioHolding btcHolding = mock(PortfolioHolding.class);
        when(btcHolding.getSymbol()).thenReturn("BTC");
        when(btcHolding.getAssetType()).thenReturn(PortfolioHolding.AssetType.CRYPTO);
        when(btcHolding.getBaseCurrency()).thenReturn("USD");
        when(btcHolding.getQuantity()).thenReturn(new BigDecimal("0.5"));
        when(btcHolding.getAverageCost()).thenReturn(new BigDecimal("40000"));
        when(btcHolding.getCurrentPrice()).thenReturn(new BigDecimal("45000"));
        when(btcHolding.getMarketValue()).thenReturn(new BigDecimal("22500"));
        when(btcHolding.getGainLoss()).thenReturn(new BigDecimal("2500"));
        when(btcHolding.getGainLossPercent()).thenReturn(new BigDecimal("12.5"));

        // Create mock portfolio
        Portfolio portfolio = mock(Portfolio.class);
        when(portfolio.getName()).thenReturn("Tech Portfolio");
        when(portfolio.getDescription()).thenReturn("기술주 및 암호화폐 포트폴리오");
        when(portfolio.getTotalValue()).thenReturn(new BigDecimal("24950"));
        when(portfolio.getTotalCost()).thenReturn(new BigDecimal("22150"));
        when(portfolio.getTotalGainLoss()).thenReturn(new BigDecimal("2800"));
        when(portfolio.getTotalGainLossPercent()).thenReturn(new BigDecimal("12.64"));
        when(portfolio.getHoldings()).thenReturn(List.of(appleHolding, googleHolding, btcHolding));

        return portfolio;
    }
}
