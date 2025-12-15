package com.pjsent.sentinel.backtest.service;

import com.pjsent.sentinel.backtest.dto.HistoricalPriceData;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 과거 가격 데이터 통합 Facade
 * 주식(한국/미국)과 암호화폐 데이터를 통합하여 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataFacade {

    private final HistoricalDataService stockService;        // AlphaVantage (미국 주식)
    private final KisHistoricalDataService kisService;       // KIS (한국 주식)
    private final CryptoHistoricalDataService cryptoService; // Upbit (암호화폐)

    // 한국 주식 심볼 패턴: 6자리 숫자 (예: 005930, 035720)
    private static final java.util.regex.Pattern KOREAN_STOCK_PATTERN =
            java.util.regex.Pattern.compile("^\\d{6}$");

    /**
     * 자산 유형에 따라 적절한 서비스에서 과거 가격 데이터를 조회합니다.
     *
     * @param symbol 종목/암호화폐 심볼
     * @param assetType 자산 유형 (STOCK or CRYPTO)
     * @param baseCurrency 기준 통화 (Crypto의 경우 KRW, USD 등)
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 과거 가격 데이터 목록
     */
    public List<HistoricalPriceData> getHistoricalPrices(
            String symbol,
            PortfolioHolding.AssetType assetType,
            String baseCurrency,
            LocalDate startDate,
            LocalDate endDate) {

        return switch (assetType) {
            case STOCK -> {
                if (isKoreanStock(symbol)) {
                    log.debug("Fetching KOREAN STOCK data for {} via KIS", symbol);
                    yield kisService.getHistoricalPrices(symbol, startDate, endDate);
                } else {
                    log.debug("Fetching US STOCK data for {} via AlphaVantage", symbol);
                    yield stockService.getHistoricalPrices(symbol, startDate, endDate);
                }
            }
            case CRYPTO -> {
                log.debug("Fetching CRYPTO data for {}-{}", baseCurrency, symbol);
                yield cryptoService.getHistoricalPrices(symbol, baseCurrency, startDate, endDate);
            }
        };
    }

    /**
     * 한국 주식인지 확인 (6자리 숫자)
     */
    private boolean isKoreanStock(String symbol) {
        return symbol != null && KOREAN_STOCK_PATTERN.matcher(symbol).matches();
    }

    /**
     * 여러 종목/암호화폐의 과거 가격 데이터를 배치로 조회합니다.
     *
     * @param holdings 포트폴리오 보유 자산 목록
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 종목별 과거 가격 데이터 맵
     */
    public Map<String, List<HistoricalPriceData>> getBatchHistoricalPrices(
            List<PortfolioHolding> holdings,
            LocalDate startDate,
            LocalDate endDate) {

        log.info("Fetching historical data for {} holdings", holdings.size());

        Map<String, List<HistoricalPriceData>> result = new HashMap<>();

        for (PortfolioHolding holding : holdings) {
            try {
                List<HistoricalPriceData> prices = getHistoricalPrices(
                        holding.getSymbol(),
                        holding.getAssetType(),
                        holding.getBaseCurrency(),
                        startDate,
                        endDate
                );
                result.put(holding.getSymbol(), prices);
            } catch (Exception e) {
                log.error("Failed to fetch historical data for {}: {}",
                        holding.getSymbol(), e.getMessage());
                result.put(holding.getSymbol(), List.of());
            }
        }

        return result;
    }
}
