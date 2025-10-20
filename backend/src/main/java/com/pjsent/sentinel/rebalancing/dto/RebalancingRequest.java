package com.pjsent.sentinel.rebalancing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
