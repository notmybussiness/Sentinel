package com.pjsent.sentinel.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 뉴스 분석 결과 DTO
 * Python RAG API Response의 analysis 객체
 *
 * @see <a href="file://.claude/specs/API_NEWS_RAG.md">API_NEWS_RAG.md</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsAnalysisDto {

    /**
     * 전체 뉴스의 종합 감성
     * POSITIVE: 긍정
     * NEGATIVE: 부정
     * NEUTRAL: 중립
     */
    @JsonProperty("overallSentiment")
    private String overallSentiment;

    /**
     * 감성 비율 (합: 1.0)
     */
    @JsonProperty("sentimentDistribution")
    private SentimentDistribution sentimentDistribution;

    /**
     * 핵심 키워드 (예: ["3nm 공정", "수출 증가", "AI 칩"])
     */
    @JsonProperty("keyTopics")
    private List<String> keyTopics;

    /**
     * 투자 리스크 요인 (예: ["중국 견제", "글로벌 경기 둔화"])
     */
    @JsonProperty("riskFactors")
    private List<String> riskFactors;

    /**
     * 투자 기회 (예: ["AI 반도체 수요 급증", "정부 지원 확대"])
     */
    @JsonProperty("opportunities")
    private List<String> opportunities;

    /**
     * 추천 종목 (뉴스 기반)
     */
    @JsonProperty("recommendedStocks")
    private List<RecommendedStockDto> recommendedStocks;

    /**
     * 감성 분포 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SentimentDistribution {

        /**
         * 긍정 비율 (0~1)
         */
        @JsonProperty("positive")
        private Double positive;

        /**
         * 부정 비율 (0~1)
         */
        @JsonProperty("negative")
        private Double negative;

        /**
         * 중립 비율 (0~1)
         */
        @JsonProperty("neutral")
        private Double neutral;
    }
}
