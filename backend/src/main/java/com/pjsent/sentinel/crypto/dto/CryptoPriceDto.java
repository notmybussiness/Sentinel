package com.pjsent.sentinel.crypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 암호화폐 가격 정보 DTO
 * Upbit, Binance 등 다양한 거래소의 가격 데이터를 표준화
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoPriceDto {

    /**
     * 암호화폐 심볼 (예: BTC, ETH, XRP)
     */
    private String symbol;

    /**
     * 기준 통화 (예: KRW, USDT, BTC)
     */
    private String baseCurrency;

    /**
     * 마켓 코드 (예: KRW-BTC, BTCUSDT)
     */
    private String marketCode;

    /**
     * 현재 가격
     */
    private double price;

    /**
     * 시가 (당일 첫 거래 가격)
     */
    private double openPrice;

    /**
     * 고가 (24시간 최고 가격)
     */
    private double highPrice;

    /**
     * 저가 (24시간 최저 가격)
     */
    private double lowPrice;

    /**
     * 거래량 (24시간)
     */
    private double volume;

    /**
     * 가격 변화량 (절댓값)
     */
    private double change;

    /**
     * 가격 변화율 (%)
     */
    private double changePercent;

    /**
     * 거래대금 (24시간)
     */
    private double tradeValue;

    /**
     * 타임스탬프
     */
    private LocalDateTime timestamp;

    /**
     * 데이터 제공자 (예: Upbit, Binance)
     */
    private String provider;
}
