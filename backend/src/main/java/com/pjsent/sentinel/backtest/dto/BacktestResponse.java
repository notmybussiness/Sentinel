package com.pjsent.sentinel.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 백테스팅 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestResponse {

    /**
     * 포트폴리오 ID
     */
    private Long portfolioId;

    /**
     * 포트폴리오 이름
     */
    private String portfolioName;

    /**
     * 백테스팅 시작일
     */
    private LocalDate startDate;

    /**
     * 백테스팅 종료일
     */
    private LocalDate endDate;

    /**
     * 초기 투자 금액
     */
    private Double initialCapital;

    /**
     * 최종 포트폴리오 가치
     */
    private Double finalValue;

    /**
     * 리밸런싱 빈도
     */
    private BacktestRequest.RebalanceFrequency rebalanceFrequency;

    /**
     * 성과 지표
     */
    private PerformanceMetrics performance;

    /**
     * 포트폴리오 가치 추이 (Equity Curve)
     */
    private List<EquityPoint> equityCurve;

    /**
     * 리밸런싱 이벤트 목록
     */
    private List<RebalanceEvent> rebalanceEvents;

    /**
     * 종목별 최종 요약
     */
    private List<HoldingSummary> holdingsSummary;

    /**
     * 백테스팅 실행 시각
     */
    private LocalDateTime executedAt;
}
