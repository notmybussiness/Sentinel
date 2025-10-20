package com.pjsent.sentinel.backtest.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 백테스팅 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestRequest {

    @NotNull(message = "Portfolio ID is required")
    private Long portfolioId;

    @NotNull(message = "Start date is required")
    @PastOrPresent(message = "Start date must be in the past")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @PastOrPresent(message = "End date must be in the past")
    private LocalDate endDate;

    @NotNull(message = "Initial capital is required")
    @Positive(message = "Initial capital must be positive")
    private Double initialCapital;

    @NotNull(message = "Rebalance frequency is required")
    private RebalanceFrequency rebalanceFrequency;

    /**
     * 리밸런싱 빈도
     */
    public enum RebalanceFrequency {
        NONE,       // 리밸런싱 없음
        MONTHLY,    // 월별
        QUARTERLY,  // 분기별
        YEARLY      // 연별
    }
}
