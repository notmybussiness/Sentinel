package com.pjsent.sentinel.pricehistory.service;

import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.MarketDataService;
import com.pjsent.sentinel.pricehistory.entity.PriceHistory;
import com.pjsent.sentinel.pricehistory.entity.PriceHistory.AssetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 인덱스 및 ETF 데이터 수집 서비스
 *
 * 주요 인덱스(S&P 500, NASDAQ 등)와 유명 ETF 데이터를 주기적으로 수집하여 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexDataCollectorService {

    private final MarketDataService marketDataService;
    private final PriceHistoryService priceHistoryService;

    /**
     * 추적할 주요 인덱스 목록
     */
    private static final List<IndexInfo> MAJOR_INDICES = Arrays.asList(
        new IndexInfo("^GSPC", "S&P 500", AssetType.INDEX),
        new IndexInfo("^IXIC", "NASDAQ Composite", AssetType.INDEX),
        new IndexInfo("^DJI", "Dow Jones", AssetType.INDEX),
        new IndexInfo("^RUT", "Russell 2000", AssetType.INDEX)
    );

    /**
     * 추적할 주요 ETF 목록
     */
    private static final List<IndexInfo> MAJOR_ETFS = Arrays.asList(
        new IndexInfo("SPY", "S&P 500 ETF", AssetType.ETF),
        new IndexInfo("QQQ", "NASDAQ 100 ETF", AssetType.ETF),
        new IndexInfo("VTI", "Vanguard Total Stock Market ETF", AssetType.ETF),
        new IndexInfo("IWM", "Russell 2000 ETF", AssetType.ETF),
        new IndexInfo("DIA", "Dow Jones ETF", AssetType.ETF),
        new IndexInfo("VOO", "Vanguard S&P 500 ETF", AssetType.ETF),
        new IndexInfo("VEA", "Vanguard FTSE Developed Markets ETF", AssetType.ETF),
        new IndexInfo("VWO", "Vanguard FTSE Emerging Markets ETF", AssetType.ETF)
    );

    /**
     * 인덱스 및 ETF 데이터 수집 (매 10분마다)
     */
    @Scheduled(fixedRate = 600000, initialDelay = 10000) // 10분마다, 10초 후 시작
    public void collectIndexAndEtfData() {
        log.info("Starting scheduled index and ETF data collection");

        try {
            // 인덱스 데이터 수집
            collectData(MAJOR_INDICES);

            // ETF 데이터 수집
            collectData(MAJOR_ETFS);

            log.info("Index and ETF data collection completed successfully");
        } catch (Exception e) {
            log.error("Error during index/ETF data collection: {}", e.getMessage(), e);
        }
    }

    /**
     * 즉시 데이터 수집 (수동 호출용)
     */
    public void collectNow() {
        log.info("Manual data collection triggered");
        collectIndexAndEtfData();
    }

    /**
     * 특정 심볼의 데이터 수집
     */
    public void collectSymbol(String symbol, AssetType assetType) {
        try {
            log.info("Collecting data for symbol: {} (type: {})", symbol, assetType);

            StockPriceDto priceData = marketDataService.getStockPrice(symbol);

            if (priceData == null || priceData.getPrice() <= 0) {
                log.warn("Invalid price data received for symbol: {}", symbol);
                return;
            }

            PriceHistory priceHistory = createPriceHistory(symbol, priceData, assetType);
            priceHistoryService.savePriceData(priceHistory);

            log.info("Successfully collected data for symbol: {} - Price: {}",
                symbol, priceData.getPrice());

        } catch (Exception e) {
            log.error("Failed to collect data for symbol: {} - Error: {}",
                symbol, e.getMessage());
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * 여러 심볼의 데이터 수집
     */
    private void collectData(List<IndexInfo> indexInfos) {
        for (IndexInfo indexInfo : indexInfos) {
            try {
                collectSymbol(indexInfo.symbol, indexInfo.assetType);

                // API Rate Limit 방지 (0.5초 대기)
                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Data collection interrupted");
                break;
            }
        }
    }

    /**
     * PriceHistory 엔티티 생성
     */
    private PriceHistory createPriceHistory(String symbol, StockPriceDto priceData, AssetType assetType) {
        LocalDateTime timestamp = LocalDateTime.now();

        return PriceHistory.builder()
            .symbol(symbol)
            .assetType(assetType)
            .baseCurrency("USD")
            .timestamp(timestamp)
            .open(BigDecimal.valueOf(priceData.getOpen()))
            .high(BigDecimal.valueOf(priceData.getHigh()))
            .low(BigDecimal.valueOf(priceData.getLow()))
            .close(BigDecimal.valueOf(priceData.getPrice()))
            .volume(null)  // StockPriceDto에 volume 필드 없음
            .build();
    }

    /**
     * 인덱스 정보 내부 클래스
     */
    private static class IndexInfo {
        final String symbol;
        final String name;
        final AssetType assetType;

        IndexInfo(String symbol, String name, AssetType assetType) {
            this.symbol = symbol;
            this.name = name;
            this.assetType = assetType;
        }
    }
}
