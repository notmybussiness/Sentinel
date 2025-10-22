package com.pjsent.sentinel.pricehistory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 차트 데이터 DTO (Frontend TradingChart용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartDataDto {

    /**
     * 심볼
     */
    private String symbol;

    /**
     * 기준 통화
     */
    private String baseCurrency;

    /**
     * 차트 데이터 포인트 목록
     */
    private List<DataPoint> data;

    /**
     * 통계 정보
     */
    private Statistics statistics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataPoint {
        /**
         * 시간 (ISO 8601 형식 문자열)
         */
        private String time;

        /**
         * 가격 (종가)
         */
        private BigDecimal value;

        /**
         * 시가 (선택적)
         */
        private BigDecimal open;

        /**
         * 고가 (선택적)
         */
        private BigDecimal high;

        /**
         * 저가 (선택적)
         */
        private BigDecimal low;

        /**
         * 거래량 (선택적)
         */
        private BigDecimal volume;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Statistics {
        /**
         * 변동률 (%)
         */
        private BigDecimal changePercent;

        /**
         * 변동 금액
         */
        private BigDecimal changeAmount;

        /**
         * 최고가
         */
        private BigDecimal highestPrice;

        /**
         * 최저가
         */
        private BigDecimal lowestPrice;

        /**
         * 평균 거래량
         */
        private BigDecimal averageVolume;
    }
}
