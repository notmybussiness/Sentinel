package com.pjsent.sentinel.rebalancing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 리밸런싱 전략 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyInfoResponse {

    private List<StrategyInfo> strategies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyInfo {
        private String name;
        private String displayName;
        private String description;
        private Boolean supported;
    }
}
