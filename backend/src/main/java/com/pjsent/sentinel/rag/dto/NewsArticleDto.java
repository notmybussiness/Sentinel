package com.pjsent.sentinel.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 뉴스 기사 DTO
 * Python RAG API Response의 newsArticles[] 요소
 *
 * @see <a href="file://.claude/specs/API_NEWS_RAG.md">API_NEWS_RAG.md</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticleDto {

    /**
     * Milvus 뉴스 ID
     */
    @JsonProperty("newsId")
    private Long newsId;

    /**
     * 뉴스 제목
     */
    @JsonProperty("title")
    private String title;

    /**
     * 뉴스 요약 (원문 청킹된 텍스트 or LLM 요약)
     */
    @JsonProperty("summary")
    private String summary;

    /**
     * 기사 발행일시 (Format: YYYY-MM-DD HH:mm)
     */
    @JsonProperty("publishedAt")
    private String publishedAt;

    /**
     * 기사 원문 링크
     */
    @JsonProperty("url")
    private String url;

    /**
     * 검색어와의 유사도 (0~1, 높을수록 관련성 높음)
     */
    @JsonProperty("relevanceScore")
    private Double relevanceScore;

    /**
     * 뉴스 감성 분석 결과
     * POSITIVE: 긍정
     * NEGATIVE: 부정
     * NEUTRAL: 중립
     */
    @JsonProperty("sentiment")
    private String sentiment;
}
