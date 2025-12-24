package com.pjsent.sentinel.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 추천 종목 DTO
 * Python RAG API Response의 analysis.recommendedStocks[] 요소
 *
 * @see <a href="file://.claude/specs/API_NEWS_RAG.md">API_NEWS_RAG.md</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedStockDto {

    /**
     * 종목 심볼 (예: "005930.KS")
     */
    @JsonProperty("symbol")
    private String symbol;

    /**
     * 종목명 (예: "삼성전자")
     */
    @JsonProperty("name")
    private String name;

    /**
     * 추천 이유 (예: "3nm 양산 성공으로 수익성 개선 전망")
     */
    @JsonProperty("reason")
    private String reason;

    /**
     * 추천 신뢰도 (0~1)
     */
    @JsonProperty("confidence")
    private Double confidence;
}
