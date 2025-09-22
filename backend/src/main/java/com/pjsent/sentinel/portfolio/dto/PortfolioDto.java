package com.pjsent.sentinel.portfolio.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 포트폴리오 DTO
 * 포트폴리오 정보를 전송하기 위한 데이터 전송 객체
 */
@Getter
@Builder
public class PortfolioDto {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private BigDecimal totalValue;
    private BigDecimal totalCost;
    private BigDecimal totalGainLoss;
    private BigDecimal totalGainLossPercent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PortfolioHoldingDto> holdings;
}

