package com.pjsent.sentinel.portfolio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 보유 종목 수정 요청 DTO
 */
@Getter
@Setter
public class UpdateHoldingRequest {
    
    @NotNull(message = "수량은 필수입니다")
    @DecimalMin(value = "0.000001", message = "수량은 0보다 커야 합니다")
    private BigDecimal quantity;
    
    @NotNull(message = "평균 단가는 필수입니다")
    @DecimalMin(value = "0.0001", message = "평균 단가는 0보다 커야 합니다")
    private BigDecimal averageCost;
}
