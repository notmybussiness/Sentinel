package com.pjsent.sentinel.rebalancing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 포트폴리오 상태 DTO (시뮬레이션용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioState {

    private Double totalValue;
    private List<HoldingState> holdings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldingState {
        private String symbol;
        private Double shares;
        private Double value;
        private Double weight;      // %
    }
}
