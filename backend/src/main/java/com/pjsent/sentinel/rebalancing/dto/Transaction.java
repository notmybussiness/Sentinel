package com.pjsent.sentinel.rebalancing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 거래 내역 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private String symbol;
    private String action;          // BUY, SELL
    private Double quantity;
    private Double price;
    private Double amount;
    private Double commission;
}
