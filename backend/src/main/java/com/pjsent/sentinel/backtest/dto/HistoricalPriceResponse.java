package com.pjsent.sentinel.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 과거 가격 데이터 조회 응답 DTO
 * API_BACKTEST.md 스펙에 정의된 응답 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalPriceResponse {

    /**
     * 종목 심볼
     */
    private String symbol;

    /**
     * 조회 시작일
     */
    private LocalDate startDate;

    /**
     * 조회 종료일
     */
    private LocalDate endDate;

    /**
     * 데이터 포인트 개수
     */
    private Integer dataPoints;

    /**
     * 가격 데이터 목록
     */
    private List<HistoricalPriceData> prices;
}
