package com.pjsent.sentinel.pricehistory.controller;

import com.pjsent.sentinel.pricehistory.dto.ChartDataDto;
import com.pjsent.sentinel.pricehistory.dto.PriceHistoryDto;
import com.pjsent.sentinel.pricehistory.entity.PriceHistory.AssetType;
import com.pjsent.sentinel.pricehistory.service.PriceHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/price-history")
@RequiredArgsConstructor
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    /**
     * 차트 데이터 조회
     *
     * GET /api/v1/price-history/chart?symbol=AAPL&startTime=2024-01-01T00:00:00&endTime=2024-12-31T23:59:59
     */
    @GetMapping("/chart")
    public ResponseEntity<ChartDataDto> getChartData(
        @RequestParam String symbol,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("Fetching chart data: symbol={}, startTime={}, endTime={}",
            symbol, startTime, endTime);

        ChartDataDto chartData = priceHistoryService.getChartData(symbol, startTime, endTime);

        if (chartData == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(chartData);
    }

    /**
     * 최신 가격 조회 (단일 심볼)
     *
     * GET /api/v1/price-history/latest/AAPL
     */
    @GetMapping("/latest/{symbol}")
    public ResponseEntity<PriceHistoryDto> getLatestPrice(@PathVariable String symbol) {
        log.info("Fetching latest price: symbol={}", symbol);

        PriceHistoryDto latestPrice = priceHistoryService.getLatestPrice(symbol);

        if (latestPrice == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(latestPrice);
    }

    /**
     * 최신 가격 조회 (복수 심볼)
     *
     * GET /api/v1/price-history/latest?symbols=AAPL,GOOGL,MSFT&assetType=STOCK
     */
    @GetMapping("/latest")
    public ResponseEntity<List<PriceHistoryDto>> getLatestPrices(
        @RequestParam List<String> symbols,
        @RequestParam AssetType assetType
    ) {
        log.info("Fetching latest prices: symbols={}, assetType={}", symbols, assetType);

        List<PriceHistoryDto> latestPrices = priceHistoryService.getLatestPrices(symbols, assetType);

        return ResponseEntity.ok(latestPrices);
    }

    /**
     * 자산 유형별 심볼 목록 조회
     *
     * GET /api/v1/price-history/symbols?assetType=ETF
     */
    @GetMapping("/symbols")
    public ResponseEntity<List<String>> getSymbolsByAssetType(@RequestParam AssetType assetType) {
        log.info("Fetching symbols by asset type: {}", assetType);

        List<String> symbols = priceHistoryService.getSymbolsByAssetType(assetType);

        return ResponseEntity.ok(symbols);
    }
}
