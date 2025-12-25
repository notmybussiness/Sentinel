package com.pjsent.sentinel.rag.service;

import com.pjsent.sentinel.rag.config.NewsRagConfig;
import com.pjsent.sentinel.rag.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;

/**
 * Python RAG API 호출 서비스
 * 뉴스 검색 및 분석을 위한 외부 RAG 시스템 연동
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsRagService {

    private final NewsRagConfig newsRagConfig;
    private final WebClient.Builder webClientBuilder;

    private static final String CIRCUIT_BREAKER_NAME = "pythonRag";

    /**
     * 뉴스 검색 및 분석 요청
     *
     * @param request 검색 요청 DTO
     * @return 검색 결과 및 분석
     */
    @Cacheable(value = "newsSearch", key = "#request.query + '_' + (#request.topK ?: 5)", sync = true)
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "searchNewsFallback")
    public NewsSearchResponse searchNews(NewsSearchRequest request) {
        if (!newsRagConfig.isAvailable()) {
            log.warn("Python RAG API가 비활성화되었습니다");
            return createEmptyResponse(request.getQuery());
        }

        log.debug("Python RAG API 호출 시작: query = {}", request.getQuery());

        try {
            WebClient webClient = webClientBuilder.build();

            NewsSearchResponse response = webClient.post()
                    .uri(newsRagConfig.getSearchUrl())
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(NewsSearchResponse.class)
                    .timeout(Duration.ofMillis(newsRagConfig.getTimeout()))
                    .block();

            if (response == null) {
                log.error("Python RAG API 응답이 비어있습니다");
                return createEmptyResponse(request.getQuery());
            }

            log.debug("Python RAG API 응답 성공: {} articles found",
                    response.getNewsArticles() != null ? response.getNewsArticles().size() : 0);

            return response;

        } catch (WebClientResponseException e) {
            log.error("Python RAG API HTTP 에러: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Python RAG API 호출 실패", e);
            throw new RuntimeException("Failed to call Python RAG API: " + e.getMessage(), e);
        }
    }

    /**
     * Circuit Breaker 폴백 메서드
     * RAG 서비스 장애 시 빈 결과 반환
     */
    public NewsSearchResponse searchNewsFallback(NewsSearchRequest request, Exception e) {
        log.warn("Python RAG API 폴백 실행: query={}, error={}", request.getQuery(), e.getMessage());
        return createEmptyResponse(request.getQuery());
    }

    /**
     * 빈 응답 생성 (폴백용)
     */
    private NewsSearchResponse createEmptyResponse(String query) {
        return NewsSearchResponse.builder()
                .query(query)
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
                        .searchTimeMs(0)
                        .build())
                .build();
    }

    /**
     * RAG API 사용 가능 여부 확인
     */
    public boolean isAvailable() {
        return newsRagConfig.isAvailable();
    }
}
