package com.pjsent.sentinel.market.dto;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceDto {
    private String symbol;
    private double price;
    private double open;
    private double high;
    private double low;
    private double close;
    private double change;
    private double changePercent;
    private String lastTradingDay;
    private LocalDateTime timeStamp;
    private String provider;
}
