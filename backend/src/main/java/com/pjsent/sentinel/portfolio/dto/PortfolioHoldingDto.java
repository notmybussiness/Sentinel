package com.pjsent.sentinel.portfolio.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 포트폴리오 보유 종목 DTO
 * 포트폴리오 보유 종목 정보를 전송하기 위한 데이터 전송 객체
 */
@Getter
@Builder
public class PortfolioHoldingDto {
    private Long id;
    private Long portfolioId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averageCost;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    private BigDecimal totalCost;
    private BigDecimal gainLoss;
    private BigDecimal gainLossPercent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
