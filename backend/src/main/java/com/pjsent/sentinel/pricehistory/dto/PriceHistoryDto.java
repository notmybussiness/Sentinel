package com.pjsent.sentinel.pricehistory.dto;

import com.pjsent.sentinel.pricehistory.entity.PriceHistory.AssetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistoryDto {
    private Long id;
    private String symbol;
    private AssetType assetType;
    private String baseCurrency;
    private LocalDateTime timestamp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
}
