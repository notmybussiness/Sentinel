package com.pjsent.sentinel.rebalancing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 리밸런싱 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalancingResponse {

    private Long portfolioId;
    private String portfolioName;
    private String strategy;
    private Double currentValue;
    private Boolean needsRebalancing;
    private List<RebalancingRecommendation> recommendations;
    private Double totalTransactionCost;
    private Double estimatedTaxImpact;
    private LocalDateTime analyzedAt;
}
