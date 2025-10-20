package com.pjsent.sentinel.rebalancing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 리밸런싱 시뮬레이션 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalancingSimulationResponse {

    private Long portfolioId;
    private String portfolioName;
    private PortfolioState beforeRebalancing;
    private PortfolioState afterRebalancing;
    private List<Transaction> transactions;
    private Double totalTransactionCost;
    private Double netChange;
}
