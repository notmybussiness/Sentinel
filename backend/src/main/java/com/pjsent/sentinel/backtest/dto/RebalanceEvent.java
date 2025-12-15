package com.pjsent.sentinel.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 리밸런싱 이벤트
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalanceEvent {

    /**
     * 리밸런싱 날짜
     */
    private LocalDate date;

    /**
     * 리밸런싱 사유
     */
    private String reason;

    /**
     * 거래 목록
     */
    private List<Trade> trades;

    /**
     * 이 리밸런싱 이벤트의 총 거래 비용
     */
    private Double totalTransactionCost;
}
