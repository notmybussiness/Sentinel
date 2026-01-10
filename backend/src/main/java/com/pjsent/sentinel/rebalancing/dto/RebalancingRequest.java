package com.pjsent.sentinel.rebalancing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 리밸런싱 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalancingRequest {

    @NotNull(message = "Portfolio ID is required")
    private Long portfolioId;

    @NotNull(message = "Rebalancing strategy is required")
    private RebalancingStrategy strategy;

    @Min(value = 0, message = "Threshold percent must be at least 0")
    @Max(value = 100, message = "Threshold percent must be at most 100")
    @Builder.Default
    private Double thresholdPercent = 5.0;

    @Builder.Default
    private Boolean considerTaxes = false;

    /**
     * TARGET_ALLOCATION 전략용: 종목별 목표 비중 (%)
     * 예: {"AAPL": 30.0, "GOOGL": 40.0, "BTC": 30.0}
     * 합계가 100%가 되어야 함
     */
    private Map<String, Double> targetAllocations;

    /**
     * RISK_PARITY, MOMENTUM 전략용: 변동성/수익률 계산 기간 (일)
     * 기본값: 30일
     */
    @Min(value = 7, message = "Lookback period must be at least 7 days")
    @Max(value = 365, message = "Lookback period must be at most 365 days")
    @Builder.Default
    private Integer lookbackDays = 30;
}
