package com.pjsent.sentinel.rag.controller;

import com.pjsent.sentinel.rag.dto.NewsSearchRequest;
import com.pjsent.sentinel.rag.dto.NewsSearchResponse;
import com.pjsent.sentinel.rag.service.NewsRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * News RAG Controller
 * Python RAG API 연동을 통한 뉴스 검색 및 분석
 *
 * @see <a href="file://.claude/specs/API_NEWS_RAG.md">API_NEWS_RAG.md</a>
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class NewsRagController {

    private final NewsRagService newsRagService;

    /**
     * 뉴스 검색
     *
     * POST /api/news/search
     *
     * @param request 뉴스 검색 요청 (query 필수)
     * @return 뉴스 검색 결과 및 분석
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchNews(@RequestBody NewsSearchRequest request) {
        log.info("뉴스 검색 요청: query={}", request.getQuery());

        // 요청 검증
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(createErrorResponse("INVALID_REQUEST", "query 필드는 필수입니다."));
        }

        // Python RAG API 호출
        NewsSearchResponse response = newsRagService.searchNews(request);

        log.info("뉴스 검색 응답: {} 건의 기사",
                response.getNewsArticles() != null ? response.getNewsArticles().size() : 0);

        return ResponseEntity.ok(response);
    }

    /**
     * RAG 상태 확인
     *
     * GET /api/news/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        boolean available = newsRagService.isAvailable();
        return ResponseEntity.ok(new HealthResponse(
                available ? "healthy" : "unavailable",
                "news-rag-api"
        ));
    }

    /**
     * 에러 응답 생성
     */
    private ErrorResponse createErrorResponse(String error, String message) {
        return new ErrorResponse(error, message, java.time.Instant.now().toString());
    }

    record ErrorResponse(String error, String message, String timestamp) {}
    record HealthResponse(String status, String service) {}
}
