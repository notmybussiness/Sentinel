package com.pjsent.sentinel.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 뉴스 검색 응답 DTO
 * Python RAG API Response Body
 *
 * @see <a href="file://.claude/specs/API_NEWS_RAG.md">API_NEWS_RAG.md</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsSearchResponse {

    /**
     * 검색 쿼리 (원본 요청 쿼리)
     */
    @JsonProperty("query")
    private String query;

    /**
     * 뉴스 기사 목록
     */
    @JsonProperty("newsArticles")
    private List<NewsArticleDto> newsArticles;

    /**
     * 뉴스 분석 결과
     */
    @JsonProperty("analysis")
    private NewsAnalysisDto analysis;

    /**
     * 검색 메타데이터
     */
    @JsonProperty("metadata")
    private NewsMetadataDto metadata;
}
