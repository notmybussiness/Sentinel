package com.pjsent.sentinel.rag.service;

import com.pjsent.sentinel.rag.config.NewsRagConfig;
import com.pjsent.sentinel.rag.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * NewsRagService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewsRagServiceTest {

    @Mock
    private NewsRagConfig newsRagConfig;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private NewsRagService newsRagService;

    @BeforeEach
    void setUp() {
        newsRagService = new NewsRagService(newsRagConfig, webClientBuilder);
    }

    @Nested
    @DisplayName("searchNews - 뉴스 검색")
    class SearchNews {

        @Test
        @DisplayName("RAG 비활성화 시 빈 응답 반환")
        void should_ReturnEmptyResponse_When_RagDisabled() {
            // Given
            when(newsRagConfig.isAvailable()).thenReturn(false);
            NewsSearchRequest request = createRequest("삼성전자");

            // When
            NewsSearchResponse response = newsRagService.searchNews(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getQuery()).isEqualTo("삼성전자");
            assertThat(response.getNewsArticles()).isEmpty();
            assertThat(response.getAnalysis().getOverallSentiment()).isEqualTo("NEUTRAL");
            assertThat(response.getMetadata().getTotalMatches()).isEqualTo(0);

            verify(webClientBuilder, never()).build();
        }

        @Test
        @DisplayName("RAG 활성화 시 정상 응답 반환")
        void should_ReturnResponse_When_RagEnabled() {
            // Given
            setupMockWebClient();
            when(newsRagConfig.isAvailable()).thenReturn(true);
            when(newsRagConfig.getSearchUrl()).thenReturn("http://localhost:8000/api/news/search");
            when(newsRagConfig.getTimeout()).thenReturn(30000);

            NewsSearchRequest request = createRequest("삼성전자 반도체");
            NewsSearchResponse expectedResponse = createMockResponse("삼성전자 반도체");

            when(responseSpec.bodyToMono(NewsSearchResponse.class))
                    .thenReturn(Mono.just(expectedResponse));

            // When
            NewsSearchResponse response = newsRagService.searchNews(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getQuery()).isEqualTo("삼성전자 반도체");
            assertThat(response.getNewsArticles()).hasSize(2);
            assertThat(response.getAnalysis().getOverallSentiment()).isEqualTo("POSITIVE");
            assertThat(response.getMetadata().getTotalMatches()).isEqualTo(10);
        }

        @Test
        @DisplayName("Python RAG API 응답이 null인 경우 빈 응답 반환")
        void should_ReturnEmptyResponse_When_ApiResponseIsNull() {
            // Given
            setupMockWebClient();
            when(newsRagConfig.isAvailable()).thenReturn(true);
            when(newsRagConfig.getSearchUrl()).thenReturn("http://localhost:8000/api/news/search");
            when(newsRagConfig.getTimeout()).thenReturn(30000);

            NewsSearchRequest request = createRequest("테스트");

            when(responseSpec.bodyToMono(NewsSearchResponse.class))
                    .thenReturn(Mono.empty());

            // When
            NewsSearchResponse response = newsRagService.searchNews(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getQuery()).isEqualTo("테스트");
            assertThat(response.getNewsArticles()).isEmpty();
        }

        @Test
        @DisplayName("WebClientResponseException 발생 시 예외 전파")
        void should_ThrowException_When_WebClientResponseException() {
            // Given
            setupMockWebClient();
            when(newsRagConfig.isAvailable()).thenReturn(true);
            when(newsRagConfig.getSearchUrl()).thenReturn("http://localhost:8000/api/news/search");
            when(newsRagConfig.getTimeout()).thenReturn(30000);

            NewsSearchRequest request = createRequest("테스트");

            WebClientResponseException exception = mock(WebClientResponseException.class);
            when(exception.getStatusCode()).thenReturn(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
            when(exception.getResponseBodyAsString()).thenReturn("Server error");

            when(responseSpec.bodyToMono(NewsSearchResponse.class))
                    .thenReturn(Mono.error(exception));

            // When & Then
            assertThatThrownBy(() -> newsRagService.searchNews(request))
                    .isInstanceOf(WebClientResponseException.class);
        }

        @Test
        @DisplayName("일반 예외 발생 시 RuntimeException 래핑")
        void should_WrapException_When_GeneralException() {
            // Given
            setupMockWebClient();
            when(newsRagConfig.isAvailable()).thenReturn(true);
            when(newsRagConfig.getSearchUrl()).thenReturn("http://localhost:8000/api/news/search");
            when(newsRagConfig.getTimeout()).thenReturn(30000);

            NewsSearchRequest request = createRequest("테스트");

            when(responseSpec.bodyToMono(NewsSearchResponse.class))
                    .thenReturn(Mono.error(new RuntimeException("Connection refused")));

            // When & Then
            assertThatThrownBy(() -> newsRagService.searchNews(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to call Python RAG API");
        }
    }

    @Nested
    @DisplayName("searchNewsFallback - Circuit Breaker 폴백")
    class SearchNewsFallback {

        @Test
        @DisplayName("폴백 시 빈 응답 반환")
        void should_ReturnEmptyResponse_When_Fallback() {
            // Given
            NewsSearchRequest request = createRequest("삼성전자");
            Exception exception = new RuntimeException("Circuit breaker open");

            // When
            NewsSearchResponse response = newsRagService.searchNewsFallback(request, exception);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getQuery()).isEqualTo("삼성전자");
            assertThat(response.getNewsArticles()).isEmpty();
            assertThat(response.getAnalysis()).isNotNull();
            assertThat(response.getAnalysis().getOverallSentiment()).isEqualTo("NEUTRAL");
            assertThat(response.getAnalysis().getSentimentDistribution().getNeutral()).isEqualTo(1.0);
            assertThat(response.getMetadata().getTotalMatches()).isEqualTo(0);
            assertThat(response.getMetadata().getReturnedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("폴백 응답의 분석 결과는 기본값")
        void should_HaveDefaultAnalysis_When_Fallback() {
            // Given
            NewsSearchRequest request = createRequest("비트코인");
            Exception exception = new RuntimeException("Timeout");

            // When
            NewsSearchResponse response = newsRagService.searchNewsFallback(request, exception);

            // Then
            NewsAnalysisDto analysis = response.getAnalysis();
            assertThat(analysis.getKeyTopics()).isEmpty();
            assertThat(analysis.getRiskFactors()).isEmpty();
            assertThat(analysis.getOpportunities()).isEmpty();
            assertThat(analysis.getRecommendedStocks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isAvailable - 서비스 가용성 확인")
    class IsAvailable {

        @Test
        @DisplayName("설정이 활성화된 경우 true 반환")
        void should_ReturnTrue_When_ConfigEnabled() {
            // Given
            when(newsRagConfig.isAvailable()).thenReturn(true);

            // When
            boolean result = newsRagService.isAvailable();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("설정이 비활성화된 경우 false 반환")
        void should_ReturnFalse_When_ConfigDisabled() {
            // Given
            when(newsRagConfig.isAvailable()).thenReturn(false);

            // When
            boolean result = newsRagService.isAvailable();

            // Then
            assertThat(result).isFalse();
        }
    }

    /**
     * WebClient Mock 설정
     */
    @SuppressWarnings("unchecked")
    private void setupMockWebClient() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        // bodyToMono 결과는 각 테스트에서 설정
    }

    /**
     * 테스트용 요청 생성
     */
    private NewsSearchRequest createRequest(String query) {
        return NewsSearchRequest.builder()
                .query(query)
                .topK(5)
                .build();
    }

    /**
     * 테스트용 Mock 응답 생성
     */
    private NewsSearchResponse createMockResponse(String query) {
        return NewsSearchResponse.builder()
                .query(query)
                .newsArticles(List.of(
                        NewsArticleDto.builder()
                                .title("삼성전자 3nm 공정 양산")
                                .publishedAt("2024-12-20T09:00:00Z")
                                .url("https://example.com/news/1")
                                .summary("삼성전자가 3nm 공정 양산을 시작한다...")
                                .sentiment("POSITIVE")
                                .relevanceScore(0.95)
                                .build(),
                        NewsArticleDto.builder()
                                .title("반도체 업황 회복")
                                .publishedAt("2024-12-19T14:30:00Z")
                                .url("https://example.com/news/2")
                                .summary("글로벌 반도체 업황이 회복세...")
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
                        .keyTopics(List.of("3nm 공정", "반도체 업황"))
                        .riskFactors(List.of("중국 규제"))
                        .opportunities(List.of("AI 수요 증가"))
                        .recommendedStocks(List.of())
                        .build())
                .metadata(NewsMetadataDto.builder()
                        .totalMatches(10)
                        .returnedCount(2)
                        .searchTimeMs(150)
                        .build())
                .build();
    }
}
