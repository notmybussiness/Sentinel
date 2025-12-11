package com.pjsent.sentinel.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 종목별 최종 요약
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingSummary {

    /**
     * 종목 심볼
     */
    private String symbol;

    /**
     * 최종 보유 수량
     */
    private Double finalQuantity;

    /**
     * 최종 가치
     */
    private Double finalValue;

    /**
     * 최종 비중 (%)
     */
    private Double finalWeight;

    /**
     * 총 수익률 (%)
     */
    private Double totalReturn;
}
