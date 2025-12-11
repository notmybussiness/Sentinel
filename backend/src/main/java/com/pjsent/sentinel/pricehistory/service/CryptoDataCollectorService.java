package com.pjsent.sentinel.pricehistory.service;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
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
 * 암호화폐 데이터 수집 서비스
 *
 * 주요 암호화폐 데이터를 주기적으로 수집하여 저장합니다.
 *
 * ⚠️ 성능 테스트 시 비활성화 (perf 프로파일 제외)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!perf")
public class CryptoDataCollectorService {

    private final CryptoDataService cryptoDataService;
    private final PriceHistoryService priceHistoryService;

    /**
     * 추적할 주요 암호화폐 목록
     */
    private static final List<String> MAJOR_CRYPTOS = Arrays.asList(
        "BTC",   // Bitcoin
        "ETH",   // Ethereum
        "BNB",   // Binance Coin
        "XRP",   // Ripple
        "ADA",   // Cardano
        "SOL",   // Solana
        "DOGE",  // Dogecoin
        "DOT",   // Polkadot
        // "MATIC", // Polygon - Upbit에서 더 이상 지원 안 함 (2025-11-06)
        "AVAX"   // Avalanche
    );

    /**
     * 암호화폐 데이터 수집 (매 5분마다)
     */
    @Scheduled(fixedRate = 300000, initialDelay = 15000) // 5분마다, 15초 후 시작
    public void collectCryptoData() {
        log.info("Starting scheduled crypto data collection");

        try {
            for (String symbol : MAJOR_CRYPTOS) {
                collectCrypto(symbol, "KRW");
                collectCrypto(symbol, "USD");

                // API Rate Limit 방지 (0.3초 대기)
                Thread.sleep(300);
            }

            log.info("Crypto data collection completed successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Crypto data collection interrupted");
        } catch (Exception e) {
            log.error("Error during crypto data collection: {}", e.getMessage(), e);
        }
    }

    /**
     * 즉시 데이터 수집 (수동 호출용)
     */
    public void collectNow() {
        log.info("Manual crypto data collection triggered");
        collectCryptoData();
    }

    /**
     * 특정 암호화폐 데이터 수집
     */
    public void collectCrypto(String symbol, String baseCurrency) {
        try {
            log.debug("Collecting crypto data for: {} ({})", symbol, baseCurrency);

            CryptoPriceDto priceData = cryptoDataService.getCryptoPrice(symbol, baseCurrency);

            if (priceData == null || priceData.getPrice() <= 0) {
                log.warn("Invalid crypto price data received for: {} ({})", symbol, baseCurrency);
                return;
            }

            PriceHistory priceHistory = createPriceHistory(symbol, priceData, baseCurrency);
            priceHistoryService.savePriceData(priceHistory);

            log.debug("Successfully collected crypto data for: {} ({}) - Price: {}",
                symbol, baseCurrency, priceData.getPrice());

        } catch (Exception e) {
            log.error("Failed to collect crypto data for: {} ({}) - Error: {}",
                symbol, baseCurrency, e.getMessage());
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * PriceHistory 엔티티 생성
     */
    private PriceHistory createPriceHistory(String symbol, CryptoPriceDto priceData, String baseCurrency) {
        LocalDateTime timestamp = LocalDateTime.now();

        return PriceHistory.builder()
            .symbol(symbol)
            .assetType(AssetType.CRYPTO)
            .baseCurrency(baseCurrency)
            .timestamp(timestamp)
            .open(BigDecimal.valueOf(priceData.getOpenPrice()))
            .high(BigDecimal.valueOf(priceData.getHighPrice()))
            .low(BigDecimal.valueOf(priceData.getLowPrice()))
            .close(BigDecimal.valueOf(priceData.getPrice()))
            .volume(BigDecimal.valueOf(priceData.getVolume()))
            .build();
    }
}
