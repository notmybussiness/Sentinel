package com.pjsent.sentinel.rebalancing.dto;

import lombok.Getter;

/**
 * 리밸런싱 전략 Enum
 */
@Getter
public enum RebalancingStrategy {
    EQUAL_WEIGHT("균등 비중", "모든 종목을 동일한 비중으로 유지합니다", true),
    TARGET_ALLOCATION("목표 비중", "사용자 지정 목표 비중에 맞춰 조정합니다", false),
    RISK_PARITY("위험 균등", "각 자산의 위험 기여도를 동일하게 유지합니다", false);

    private final String displayName;
    private final String description;
    private final boolean supported;

    RebalancingStrategy(String displayName, String description, boolean supported) {
        this.displayName = displayName;
        this.description = description;
        this.supported = supported;
    }
}
