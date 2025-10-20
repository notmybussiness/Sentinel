package com.pjsent.sentinel.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 과거 가격 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalPriceData {

    /**
     * 날짜
     */
    private LocalDate date;

    /**
     * 시가
     */
    private Double open;

    /**
     * 고가
     */
    private Double high;

    /**
     * 저가
     */
    private Double low;

    /**
     * 종가
     */
    private Double close;

    /**
     * 거래량
     */
    private Long volume;

    /**
     * 조정 종가 (배당, 분할 조정)
     */
    private Double adjustedClose;
}
