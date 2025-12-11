package com.pjsent.sentinel.crypto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 암호화폐 검색 결과 DTO
 * 거래소의 암호화폐 검색 결과를 표준화
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CryptoSearchResultDto {

    /**
     * 암호화폐 심볼 (예: BTC, ETH)
     */
    private String symbol;

    /**
     * 암호화폐 이름 (예: Bitcoin, Ethereum)
     */
    private String name;

    /**
     * 한글 이름 (예: 비트코인, 이더리움)
     */
    private String koreanName;

    /**
     * 마켓 코드 (예: KRW-BTC, USDT-BTC)
     */
    private String marketCode;

    /**
     * 기준 통화 (예: KRW, USDT)
     */
    private String baseCurrency;

    /**
     * 거래 가능 여부
     */
    private boolean tradeable;

    /**
     * 거래소 (예: Upbit, Binance)
     */
    private String exchange;
}
