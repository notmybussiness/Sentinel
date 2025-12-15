package com.pjsent.sentinel.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 거래 내역
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    /**
     * 종목 심볼
     */
    private String symbol;

    /**
     * 거래 유형
     */
    private TradeAction action;

    /**
     * 수량
     */
    private Double quantity;

    /**
     * 가격
     */
    private Double price;

    /**
     * 총 금액
     */
    private Double amount;

    /**
     * 거래 비용 (거래 금액 × 비용 비율)
     */
    private Double transactionCost;

    /**
     * 거래 유형
     */
    public enum TradeAction {
        BUY,    // 매수
        SELL,   // 매도
        HOLD    // 보유
    }
}
