package com.pjsent.sentinel.rebalancing.dto;

import lombok.Getter;

/**
 * 리밸런싱 전략 Enum
 */
@Getter
public enum RebalancingStrategy {
    EQUAL_WEIGHT("균등 비중", "모든 종목을 동일한 비중으로 유지합니다", true),
    TARGET_ALLOCATION("목표 비중", "사용자 지정 목표 비중에 맞춰 조정합니다", true),
    RISK_PARITY("위험 균등", "각 자산의 위험 기여도를 동일하게 유지합니다 (변동성 역가중)", true),
    MOMENTUM("모멘텀", "최근 수익률이 높은 자산에 더 높은 비중을 부여합니다", true),
    DIVIDEND_FOCUSED("배당 중심", "배당 수익률 기반 비중 조정 (준비 중)", false);

    private final String displayName;
    private final String description;
    private final boolean supported;

    RebalancingStrategy(String displayName, String description, boolean supported) {
        this.displayName = displayName;
        this.description = description;
        this.supported = supported;
    }
}
