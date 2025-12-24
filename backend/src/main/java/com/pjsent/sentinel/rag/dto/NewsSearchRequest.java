package com.pjsent.sentinel.rag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 뉴스 검색 요청 DTO
 * Python RAG API Request Body
 *
 * @see <a href="file://.claude/specs/API_NEWS_RAG.md">API_NEWS_RAG.md</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsSearchRequest {

    /**
     * 검색 키워드 (예: "삼성전자 반도체 최신 동향")
     */
    @JsonProperty("query")
    private String query;

    /**
     * 포트폴리오 컨텍스트 (제공 시 더 정확한 검색)
     */
    @JsonProperty("portfolioContext")
    private PortfolioContext portfolioContext;

    /**
     * 검색 필터
     */
    @JsonProperty("filters")
    private SearchFilters filters;

    /**
     * 반환할 뉴스 개수 (기본값: 5, 최대: 20)
     */
    @JsonProperty("topK")
    private Integer topK;

    /**
     * 포트폴리오 컨텍스트 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioContext {

        /**
         * 보유 종목 목록
         */
        @JsonProperty("holdings")
        private List<Holding> holdings;

        /**
         * 관련 섹터 (예: ["반도체", "암호화폐"])
         */
        @JsonProperty("sectors")
        private List<String> sectors;

        /**
         * 포트폴리오 총 가치 (원)
         */
        @JsonProperty("totalValue")
        private Double totalValue;

        /**
         * 보유 종목 내부 클래스
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Holding {

            /**
             * 종목 심볼 (예: "005930.KS", "BTC-KRW")
             */
            @JsonProperty("symbol")
            private String symbol;

            /**
             * 종목명 (예: "삼성전자", "비트코인")
             */
            @JsonProperty("name")
            private String name;

            /**
             * 포트폴리오 내 비중 (0~1)
             */
            @JsonProperty("weight")
            private Double weight;
        }
    }

    /**
     * 검색 필터 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchFilters {

        /**
         * 검색 시작일 (기본값: 7일 전)
         * Format: YYYY-MM-DD
         */
        @JsonProperty("startDate")
        private String startDate;

        /**
         * 검색 종료일 (기본값: 오늘)
         * Format: YYYY-MM-DD
         */
        @JsonProperty("endDate")
        private String endDate;

        /**
         * 최소 유사도 임계값 (기본값: 0.7)
         */
        @JsonProperty("minRelevance")
        private Double minRelevance;
    }
}
