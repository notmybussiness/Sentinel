package com.pjsent.sentinel.rag.controller;

import com.pjsent.sentinel.rag.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * News RAG Mock Controller
 * Python RAG API Mock 서버 (Phase 1: API 스펙 검증용)
 *
 * @see <a href="file://.claude/specs/API_NEWS_RAG.md">API_NEWS_RAG.md</a>
 * @see <a href="file://.claude/roadmap/NEWS_RAG_INTEGRATION.md">NEWS_RAG_INTEGRATION.md Phase 1</a>
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class MockNewsRagController {

    /**
     * 뉴스 검색 (Mock)
     *
     * POST /api/news/search
     *
     * @param request 뉴스 검색 요청 (query 필수)
     * @return 더미 뉴스 검색 결과 (API_NEWS_RAG.md 스펙 준수)
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchNews(@RequestBody NewsSearchRequest request) {
        log.info("[Mock] 뉴스 검색 요청: {}", request.getQuery());

        // 요청 검증
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(createErrorResponse("INVALID_REQUEST", "query 필드는 필수입니다."));
        }

        // 더미 데이터 생성 (API_NEWS_RAG.md Example Response)
        NewsSearchResponse response = createMockResponse(request.getQuery());

        log.info("[Mock] 뉴스 검색 응답: {} 건의 기사", response.getNewsArticles().size());
        return ResponseEntity.ok(response);
    }

    /**
     * 더미 응답 생성
     * API_NEWS_RAG.md의 Example Response를 기반으로 작성
     */
    private NewsSearchResponse createMockResponse(String query) {
        // 더미 뉴스 기사
        List<NewsArticleDto> articles = Arrays.asList(
                NewsArticleDto.builder()
                        .newsId(12345L)
                        .title("삼성전자, 차세대 3nm 공정 양산 시작")
                        .summary("삼성전자가 3나노미터 공정을 활용한 차세대 반도체 양산을 시작했다. 업계 전문가들은 이번 3nm 공정이 기존 5nm 대비 성능은 23% 향상되고 전력 소비는 45% 감소할 것으로 전망하고 있다.")
                        .publishedAt("2025-12-20 10:30")
                        .url("https://news.naver.com/main/read.nhn?mode=LSD&mid=sec&sid1=001&oid=001&aid=0014234567")
                        .relevanceScore(0.92)
                        .sentiment("POSITIVE")
                        .build(),
                NewsArticleDto.builder()
                        .newsId(12346L)
                        .title("반도체 수출 3개월 연속 증가, AI 칩 수요 급증")
                        .summary("한국 반도체 수출이 3개월 연속 증가하며 회복세를 보이고 있다. 특히 AI 서버용 고성능 칩 수요가 급증하면서 삼성전자와 SK하이닉스의 수주량이 전년 대비 40% 증가했다.")
                        .publishedAt("2025-12-19 14:20")
                        .url("https://news.naver.com/main/read.nhn?mode=LSD&mid=sec&sid1=101&oid=009&aid=0005234567")
                        .relevanceScore(0.85)
                        .sentiment("POSITIVE")
                        .build(),
                NewsArticleDto.builder()
                        .newsId(12347L)
                        .title("미중 반도체 갈등 심화, 한국 기업 리스크 증가")
                        .summary("미국과 중국의 반도체 패권 경쟁이 격화되면서 한국 반도체 기업들이 양국 사이에서 선택의 기로에 놓였다. 전문가들은 지정학적 리스크가 단기 실적에 악영향을 미칠 수 있다고 경고했다.")
                        .publishedAt("2025-12-18 09:15")
                        .url("https://news.naver.com/main/read.nhn?mode=LSD&mid=sec&sid1=101&oid=011&aid=0004234567")
                        .relevanceScore(0.78)
                        .sentiment("NEGATIVE")
                        .build()
        );

        // 더미 감성 분포
        NewsAnalysisDto.SentimentDistribution sentimentDist = NewsAnalysisDto.SentimentDistribution.builder()
                .positive(0.67)
                .negative(0.20)
                .neutral(0.13)
                .build();

        // 더미 추천 종목
        List<RecommendedStockDto> recommendedStocks = Arrays.asList(
                RecommendedStockDto.builder()
                        .symbol("005930.KS")
                        .name("삼성전자")
                        .reason("3nm 양산 성공 및 AI 칩 수요 증가로 수익성 개선 전망")
                        .confidence(0.88)
                        .build(),
                RecommendedStockDto.builder()
                        .symbol("000660.KS")
                        .name("SK하이닉스")
                        .reason("HBM 메모리 시장 독점 및 AI 서버향 수주 급증")
                        .confidence(0.85)
                        .build()
        );

        // 더미 분석 결과
        NewsAnalysisDto analysis = NewsAnalysisDto.builder()
                .overallSentiment("POSITIVE")
                .sentimentDistribution(sentimentDist)
                .keyTopics(Arrays.asList("3nm 공정", "수출 증가", "AI 칩", "미중 갈등", "지정학적 리스크"))
                .riskFactors(Arrays.asList(
                        "미중 반도체 패권 경쟁 심화",
                        "중국 시장 의존도 높음",
                        "글로벌 경기 둔화 우려",
                        "환율 변동성 확대"
                ))
                .opportunities(Arrays.asList(
                        "AI 반도체 수요 급증 (40% 증가)",
                        "3nm 공정 양산 성공으로 기술 우위 확보",
                        "정부 반도체 지원 정책 확대",
                        "차세대 HBM 메모리 독점적 지위"
                ))
                .recommendedStocks(recommendedStocks)
                .build();

        // 더미 메타데이터
        NewsMetadataDto metadata = NewsMetadataDto.builder()
                .totalMatches(15)
                .returnedCount(3)
                .searchTimeMs(120)
                .build();

        // 전체 응답 구성
        return NewsSearchResponse.builder()
                .query(query)
                .newsArticles(articles)
                .analysis(analysis)
                .metadata(metadata)
                .build();
    }

    /**
     * 에러 응답 생성
     */
    private ErrorResponse createErrorResponse(String error, String message) {
        return new ErrorResponse(error, message, java.time.Instant.now().toString());
    }

    /**
     * 에러 응답 DTO (내부 클래스)
     */
    record ErrorResponse(String error, String message, String timestamp) {}
}
