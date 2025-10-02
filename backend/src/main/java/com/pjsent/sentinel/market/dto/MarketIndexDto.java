package com.pjsent.sentinel.market.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 시장 지수 DTO
 * S&P 500, NASDAQ, DOW, KOSPI 등 주요 지수 정보를 전달합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketIndexDto {

    /**
     * 지수 심볼 (예: ^GSPC, ^IXIC, ^DJI, ^KS11)
     */
    private String symbol;

    /**
     * 지수 이름 (예: S&P 500, NASDAQ, DOW, KOSPI)
     */
    private String name;

    /**
     * 현재 지수 값
     */
    private Double value;

    /**
     * 전일 대비 변동 (절대값)
     */
    private Double change;

    /**
     * 전일 대비 변동률 (%)
     */
    private Double changePercent;

    /**
     * 데이터 조회 시각
     */
    private LocalDateTime timestamp;
}
