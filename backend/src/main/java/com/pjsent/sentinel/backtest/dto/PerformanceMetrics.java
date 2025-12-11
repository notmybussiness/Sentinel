package com.pjsent.sentinel.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 백테스팅 성과 지표
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {

    /**
     * 총 수익률 (%)
     */
    private Double totalReturn;

    /**
     * 연평균 성장률 (CAGR, %)
     */
    private Double cagr;

    /**
     * 샤프 비율 (Sharpe Ratio)
     * (평균 수익률 - 무위험 이자율) / 표준편차
     */
    private Double sharpeRatio;

    /**
     * 소르티노 비율 (Sortino Ratio)
     * (평균 수익률 - 무위험 이자율) / 하방 편차
     */
    private Double sortinoRatio;

    /**
     * 최대 낙폭 (Max Drawdown, %)
     */
    private Double maxDrawdown;

    /**
     * 변동성 (연율화 표준편차, %)
     */
    private Double volatility;

    /**
     * 승률 (%)
     * 양의 수익률을 기록한 날의 비율
     */
    private Double winRate;
}
