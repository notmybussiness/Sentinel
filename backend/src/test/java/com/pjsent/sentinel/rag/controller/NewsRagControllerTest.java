package com.pjsent.sentinel.rag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.config.TestSecurityConfig;
import com.pjsent.sentinel.rag.dto.*;
import com.pjsent.sentinel.rag.service.NewsRagService;
import com.pjsent.sentinel.user.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NewsRagController 단위 테스트
 */
@WebMvcTest(NewsRagController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-jwt-secret-for-rag-controller-test",
    "python-rag.enabled=true",
    "python-rag.base-url=http://localhost:8000",
    "python-rag.timeout=30000"
})
class NewsRagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsRagService newsRagService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private NewsSearchRequest validRequest;
    private NewsSearchResponse mockResponse;

    @BeforeEach
    void setUp() {
        validRequest = NewsSearchRequest.builder()
                .query("삼성전자 반도체")
                .topK(5)
                .build();

        mockResponse = createMockResponse("삼성전자 반도체");
    }

    @Nested
    @DisplayName("POST /api/news/search - 뉴스 검색")
    class SearchNews {

        @Test
        @DisplayName("정상 검색 요청 시 200 OK 반환")
        void should_ReturnOk_When_ValidRequest() throws Exception {
            // Given
            when(newsRagService.searchNews(any(NewsSearchRequest.class))).thenReturn(mockResponse);

            // When & Then
            mockMvc.perform(post("/api/news/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.query").value("삼성전자 반도체"))
                    .andExpect(jsonPath("$.newsArticles").isArray())
                    .andExpect(jsonPath("$.newsArticles.length()").value(2))
                    .andExpect(jsonPath("$.analysis.overallSentiment").value("POSITIVE"))
                    .andExpect(jsonPath("$.metadata.totalMatches").value(10));

            verify(newsRagService, times(1)).searchNews(any(NewsSearchRequest.class));
        }

        @Test
        @DisplayName("포트폴리오 컨텍스트 포함 검색 요청 성공")
        void should_ReturnOk_When_RequestWithPortfolioContext() throws Exception {
            // Given
            NewsSearchRequest requestWithContext = NewsSearchRequest.builder()
                    .query("AI 반도체")
                    .topK(10)
                    .portfolioContext(NewsSearchRequest.PortfolioContext.builder()
                            .holdings(List.of(
                                    NewsSearchRequest.PortfolioContext.Holding.builder()
                                            .symbol("005930.KS")
                                            .name("삼성전자")
                                            .weight(0.3)
                                            .build()
                            ))
                            .sectors(List.of("반도체", "IT"))
                            .totalValue(10000000.0)
                            .build())
                    .build();

            when(newsRagService.searchNews(any(NewsSearchRequest.class)))
                    .thenReturn(createMockResponse("AI 반도체"));

            // When & Then
            mockMvc.perform(post("/api/news/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithContext)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.query").value("AI 반도체"));

            verify(newsRagService, times(1)).searchNews(any(NewsSearchRequest.class));
        }

        @Test
        @DisplayName("필터 포함 검색 요청 성공")
        void should_ReturnOk_When_RequestWithFilters() throws Exception {
            // Given
            NewsSearchRequest requestWithFilters = NewsSearchRequest.builder()
                    .query("비트코인")
                    .topK(5)
                    .filters(NewsSearchRequest.SearchFilters.builder()
                            .startDate("2024-01-01")
                            .endDate("2024-12-31")
                            .minRelevance(0.8)
                            .build())
                    .build();

            when(newsRagService.searchNews(any(NewsSearchRequest.class)))
                    .thenReturn(createMockResponse("비트코인"));

            // When & Then
            mockMvc.perform(post("/api/news/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithFilters)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.query").value("비트코인"));

            verify(newsRagService, times(1)).searchNews(any(NewsSearchRequest.class));
        }

        @Test
        @DisplayName("query 필드가 null인 경우 400 Bad Request 반환")
        void should_ReturnBadRequest_When_QueryIsNull() throws Exception {
            // Given
            NewsSearchRequest invalidRequest = NewsSearchRequest.builder()
                    .query(null)
                    .topK(5)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/news/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message").value("query 필드는 필수입니다."));

            verify(newsRagService, never()).searchNews(any());
        }

        @Test
        @DisplayName("query 필드가 빈 문자열인 경우 400 Bad Request 반환")
        void should_ReturnBadRequest_When_QueryIsEmpty() throws Exception {
            // Given
            NewsSearchRequest invalidRequest = NewsSearchRequest.builder()
                    .query("")
                    .topK(5)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/news/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

            verify(newsRagService, never()).searchNews(any());
        }

        @Test
        @DisplayName("query 필드가 공백만 있는 경우 400 Bad Request 반환")
        void should_ReturnBadRequest_When_QueryIsBlank() throws Exception {
            // Given
            NewsSearchRequest invalidRequest = NewsSearchRequest.builder()
                    .query("   ")
                    .topK(5)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/news/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

            verify(newsRagService, never()).searchNews(any());
        }

        @Test
        @DisplayName("검색 결과가 없는 경우 빈 배열 반환")
        void should_ReturnEmptyArray_When_NoResults() throws Exception {
            // Given
            NewsSearchResponse emptyResponse = NewsSearchResponse.builder()
                    .query("존재하지않는쿼리")
                    .newsArticles(List.of())
                    .analysis(NewsAnalysisDto.builder()
                            .overallSentiment("NEUTRAL")
                            .sentimentDistribution(NewsAnalysisDto.SentimentDistribution.builder()
                                    .positive(0.0)
                                    .negative(0.0)
                                    .neutral(1.0)
                                    .build())
                            .keyTopics(List.of())
                            .riskFactors(List.of())
                            .opportunities(List.of())
                            .recommendedStocks(List.of())
                            .build())
                    .metadata(NewsMetadataDto.builder()
                            .totalMatches(0)
                            .returnedCount(0)
                            .searchTimeMs(50)
                            .build())
                    .build();

            NewsSearchRequest request = NewsSearchRequest.builder()
                    .query("존재하지않는쿼리")
                    .build();

            when(newsRagService.searchNews(any(NewsSearchRequest.class))).thenReturn(emptyResponse);

            // When & Then
            mockMvc.perform(post("/api/news/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newsArticles").isArray())
                    .andExpect(jsonPath("$.newsArticles.length()").value(0))
                    .andExpect(jsonPath("$.metadata.totalMatches").value(0));
        }

        @Test
        @DisplayName("서비스 예외 발생 시 500 Internal Server Error 반환")
        void should_ReturnInternalServerError_When_ServiceThrowsException() throws Exception {
            // Given
            when(newsRagService.searchNews(any(NewsSearchRequest.class)))
                    .thenThrow(new RuntimeException("RAG 서비스 오류"));

            // When & Then
            mockMvc.perform(post("/api/news/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/news/health - 헬스체크")
    class HealthCheck {

        @Test
        @DisplayName("RAG 서비스가 사용 가능한 경우 healthy 반환")
        void should_ReturnHealthy_When_ServiceAvailable() throws Exception {
            // Given
            when(newsRagService.isAvailable()).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/news/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("healthy"))
                    .andExpect(jsonPath("$.service").value("news-rag-api"));

            verify(newsRagService, times(1)).isAvailable();
        }

        @Test
        @DisplayName("RAG 서비스가 사용 불가능한 경우 unavailable 반환")
        void should_ReturnUnavailable_When_ServiceNotAvailable() throws Exception {
            // Given
            when(newsRagService.isAvailable()).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/api/news/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("unavailable"))
                    .andExpect(jsonPath("$.service").value("news-rag-api"));

            verify(newsRagService, times(1)).isAvailable();
        }
    }

    /**
     * 테스트용 Mock 응답 생성
     */
    private NewsSearchResponse createMockResponse(String query) {
        return NewsSearchResponse.builder()
                .query(query)
                .newsArticles(List.of(
                        NewsArticleDto.builder()
                                .title("삼성전자 3nm 공정 양산 본격화")
                                .publishedAt("2024-12-20T09:00:00Z")
                                .url("https://example.com/news/1")
                                .summary("삼성전자가 3nm 공정 양산을 본격화한다...")
                                .sentiment("POSITIVE")
                                .relevanceScore(0.95)
                                .build(),
                        NewsArticleDto.builder()
                                .title("반도체 업황 회복세")
                                .publishedAt("2024-12-19T14:30:00Z")
                                .url("https://example.com/news/2")
                                .summary("글로벌 반도체 업황이 회복세에 접어들었다...")
                                .sentiment("POSITIVE")
                                .relevanceScore(0.88)
                                .build()
                ))
                .analysis(NewsAnalysisDto.builder()
                        .overallSentiment("POSITIVE")
                        .sentimentDistribution(NewsAnalysisDto.SentimentDistribution.builder()
                                .positive(0.7)
                                .negative(0.1)
                                .neutral(0.2)
                                .build())
                        .keyTopics(List.of("3nm 공정", "반도체 업황", "AI 칩"))
                        .riskFactors(List.of("중국 규제"))
                        .opportunities(List.of("AI 수요 증가"))
                        .recommendedStocks(List.of(
                                RecommendedStockDto.builder()
                                        .symbol("005930.KS")
                                        .name("삼성전자")
                                        .reason("3nm 공정 선도")
                                        .build()
                        ))
                        .build())
                .metadata(NewsMetadataDto.builder()
                        .totalMatches(10)
                        .returnedCount(2)
                        .searchTimeMs(150)
                        .build())
                .build();
    }
}
