package com.pjsent.sentinel.rebalancing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 리밸런싱 추천 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalancingRecommendation {

    private String symbol;
    private Double currentWeight;       // %
    private Double targetWeight;        // %
    private Double currentShares;
    private Double targetShares;
    private String action;              // BUY, SELL, HOLD
    private Double quantity;
    private Double estimatedAmount;     // USD

    public enum Action {
        BUY, SELL, HOLD
    }
}
