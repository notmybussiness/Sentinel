package com.pjsent.sentinel.portfolio.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 리밸런싱 추천안 DTO
 * 포트폴리오 리밸런싱 분석 결과와 추천 액션을 포함
 */
@Data
@Builder
public class RebalancingRecommendationDto {

    /**
     * 추천안 ID
     */
    private String recommendationId;

    /**
     * 포트폴리오 ID
     */
    private Long portfolioId;

    /**
     * 사용된 전략명
     */
    private String strategyName;

    /**
     * 리밸런싱 필요 여부
     */
    private Boolean rebalancingNeeded;

    /**
     * 전체 포트폴리오 편차율 (%)
     */
    private Double totalDeviationPercent;

    /**
     * 현재 자산 배분 (symbol -> current percentage)
     */
    private Map<String, Double> currentAllocation;

    /**
     * 목표 자산 배분 (symbol -> target percentage)
     */
    private Map<String, Double> targetAllocation;

    /**
     * 자산별 편차 정보 (symbol -> deviation percentage)
     */
    private Map<String, Double> deviations;

    /**
     * 리밸런싱 액션 목록
     */
    private List<RebalancingActionDto> actions;

    /**
     * 예상 거래 비용
     */
    private BigDecimal estimatedTransactionCost;

    /**
     * 세금 영향 분석
     */
    private TaxImpactDto taxImpact;

    /**
     * 추천안 생성 시각
     */
    private LocalDateTime createdAt;

    /**
     * 다음 리뷰 예정일
     */
    private LocalDateTime nextReviewDate;

    /**
     * 전략별 상세 정보
     */
    private Map<String, Object> strategyDetails;

    /**
     * 리밸런싱 우선순위 (1=높음, 5=낮음)
     */
    private Integer priority;

    /**
     * 추가 메모
     */
    private String notes;

    /**
     * 리밸런싱 액션 DTO
     */
    @Data
    @Builder
    public static class RebalancingActionDto {
        /**
         * 액션 타입 (BUY, SELL, HOLD)
         */
        private ActionType actionType;

        /**
         * 종목 심볼
         */
        private String symbol;

        /**
         * 현재 수량
         */
        private BigDecimal currentQuantity;

        /**
         * 목표 수량
         */
        private BigDecimal targetQuantity;

        /**
         * 변경 수량 (+ 매수, - 매도)
         */
        private BigDecimal quantityChange;

        /**
         * 현재 가격
         */
        private BigDecimal currentPrice;

        /**
         * 예상 거래 금액
         */
        private BigDecimal estimatedAmount;

        /**
         * 현재 비중 (%)
         */
        private Double currentWeight;

        /**
         * 목표 비중 (%)
         */
        private Double targetWeight;

        /**
         * 편차 (%)
         */
        private Double deviation;

        /**
         * 우선순위
         */
        private Integer priority;
    }

    /**
     * 세금 영향 분석 DTO
     */
    @Data
    @Builder
    public static class TaxImpactDto {
        /**
         * 예상 양도소득세
         */
        private BigDecimal estimatedCapitalGainsTax;

        /**
         * 세금 효율적 매도 후보
         */
        private List<String> taxEfficientSellCandidates;

        /**
         * 손실 실현 기회
         */
        private List<String> taxLossHarvestingOpportunities;
    }

    /**
     * 액션 타입 열거형
     */
    public enum ActionType {
        BUY("매수"),
        SELL("매도"),
        HOLD("보유");

        private final String description;

        ActionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}