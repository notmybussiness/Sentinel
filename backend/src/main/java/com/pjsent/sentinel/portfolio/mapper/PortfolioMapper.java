package com.pjsent.sentinel.portfolio.mapper;

import com.pjsent.sentinel.portfolio.dto.PortfolioDto;
import com.pjsent.sentinel.portfolio.dto.PortfolioHoldingDto;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 포트폴리오 매퍼
 * Portfolio/PortfolioHolding 엔티티를 DTO로 변환
 * 
 * TDD Phase 3:
 * - PortfolioService에서 추출
 * - 독립적 단위 테스트 가능
 * - 다른 서비스에서 재사용 가능
 */
@Component
@Slf4j
public class PortfolioMapper {

    /**
     * Portfolio 엔티티를 DTO로 변환
     * 
     * @param portfolio 변환할 Portfolio 엔티티
     * @return PortfolioDto (null 시 null 반환)
     */
    public PortfolioDto toDto(Portfolio portfolio) {
        if (portfolio == null) {
            return null;
        }

        List<PortfolioHoldingDto> holdingDtos = portfolio.getHoldings() != null
                ? portfolio.getHoldings().stream()
                        .map(this::toHoldingDto)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return PortfolioDto.builder()
                .id(portfolio.getId())
                .userId(portfolio.getUserId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .totalValue(portfolio.getTotalValue())
                .totalCost(portfolio.getTotalCost())
                .totalGainLoss(portfolio.getTotalGainLoss())
                .totalGainLossPercent(portfolio.getTotalGainLossPercent())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .holdings(holdingDtos)
                .build();
    }

    /**
     * PortfolioHolding 엔티티를 DTO로 변환
     * 
     * @param holding 변환할 PortfolioHolding 엔티티
     * @return PortfolioHoldingDto (null 시 null 반환)
     */
    public PortfolioHoldingDto toHoldingDto(PortfolioHolding holding) {
        if (holding == null) {
            return null;
        }

        return PortfolioHoldingDto.builder()
                .id(holding.getId())
                .portfolioId(holding.getPortfolio() != null ? holding.getPortfolio().getId() : null)
                .symbol(holding.getSymbol())
                .quantity(holding.getQuantity())
                .averageCost(holding.getAverageCost())
                .currentPrice(holding.getCurrentPrice())
                .marketValue(holding.getMarketValue())
                .totalCost(holding.getTotalCost())
                .gainLoss(holding.getGainLoss())
                .gainLossPercent(holding.getGainLossPercent())
                .assetType(holding.getAssetType() != null ? holding.getAssetType().name() : null)
                .baseCurrency(holding.getBaseCurrency())
                .createdAt(holding.getCreatedAt())
                .updatedAt(holding.getUpdatedAt())
                .build();
    }

    /**
     * PortfolioHolding 리스트를 DTO 리스트로 변환
     * 
     * @param holdings 변환할 PortfolioHolding 리스트
     * @return PortfolioHoldingDto 리스트
     */
    public List<PortfolioHoldingDto> toHoldingDtoList(List<PortfolioHolding> holdings) {
        if (holdings == null || holdings.isEmpty()) {
            return Collections.emptyList();
        }

        return holdings.stream()
                .map(this::toHoldingDto)
                .collect(Collectors.toList());
    }
}
