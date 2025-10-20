package com.pjsent.sentinel.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 포트폴리오 가치 포인트 (Equity Curve의 한 점)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquityPoint {

    /**
     * 날짜
     */
    private LocalDate date;

    /**
     * 포트폴리오 가치
     */
    private Double value;

    /**
     * 일일 수익률 (%)
     */
    private Double dailyReturn;

    /**
     * 누적 수익률 (%)
     */
    private Double cumulativeReturn;
}
