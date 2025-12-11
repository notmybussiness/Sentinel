package com.pjsent.sentinel.common.event;

import com.pjsent.sentinel.portfolio.entity.PortfolioHolding.AssetType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Price Update Event for EDA
 * Published when a stock or crypto price changes.
 */
public record PriceUpdateEvent(
        String symbol,
        BigDecimal price,
        AssetType assetType,
        LocalDateTime timestamp) {
}
