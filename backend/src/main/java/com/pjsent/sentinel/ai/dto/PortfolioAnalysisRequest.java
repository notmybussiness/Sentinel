package com.pjsent.sentinel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 포트폴리오 분석 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAnalysisRequest {

    /**
     * 분석할 포트폴리오 ID
     */
    private Long portfolioId;

    /**
     * 분석 타입
     * OVERVIEW: 전체 개요
     * DIVERSIFICATION: 다각화 분석
     * RISK: 리스크 분석
     * PERFORMANCE: 성과 분석
     * RECOMMENDATION: 투자 제안
     */
    private AnalysisType analysisType;

    /**
     * 추가 옵션: 시장 상황 포함 여부
     */
    private Boolean includeMarketContext;

    public enum AnalysisType {
        OVERVIEW,
        DIVERSIFICATION,
        RISK,
        PERFORMANCE,
        RECOMMENDATION
    }
}
