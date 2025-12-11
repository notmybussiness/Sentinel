package com.pjsent.sentinel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 포트폴리오 분석 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAnalysisResponse {

    /**
     * 분석 요약
     */
    private String summary;

    /**
     * 주요 인사이트 (3-5개)
     */
    private List<String> insights;

    /**
     * 투자 제안 (3-5개)
     */
    private List<String> recommendations;

    /**
     * 리스크 수준 (LOW, MEDIUM, HIGH)
     */
    private RiskLevel riskLevel;

    /**
     * 다각화 점수 (0-100)
     */
    private Double diversificationScore;

    /**
     * 분석 타입
     */
    private PortfolioAnalysisRequest.AnalysisType analysisType;

    /**
     * 분석 생성 시각
     */
    private LocalDateTime analyzedAt;

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}
