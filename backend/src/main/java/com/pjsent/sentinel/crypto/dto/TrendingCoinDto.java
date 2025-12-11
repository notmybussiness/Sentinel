package com.pjsent.sentinel.crypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 트렌딩 암호화폐 DTO
 * 거래량, 변동률 기준 상위 암호화폐 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingCoinDto {

    /**
     * 암호화폐 심볼
     */
    private String symbol;

    /**
     * 암호화폐 이름
     */
    private String name;

    /**
     * 한글 이름
     */
    private String koreanName;

    /**
     * 마켓 코드
     */
    private String marketCode;

    /**
     * 현재 가격
     */
    private double price;

    /**
     * 가격 변화율 (%)
     */
    private double changePercent;

    /**
     * 24시간 거래량
     */
    private double volume;

    /**
     * 24시간 거래대금
     */
    private double tradeValue;

    /**
     * 순위 (거래대금 기준)
     */
    private int rank;

    /**
     * 기준 통화
     */
    private String baseCurrency;
}
