package com.pjsent.sentinel.portfolio.scheduler;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.MarketDataService;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding.AssetType;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 포트폴리오 가격 업데이트 스케줄러
 *
 * Phase 4a: Read/Write 분리
 * - 5분마다 백그라운드에서 모든 포트폴리오의 가격 업데이트
 * - 조회 API는 DB 조회만 수행 → 응답 속도 개선
 * - 외부 API 상태와 독립적으로 안정적인 서비스 제공
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioPriceScheduler {

    private final PortfolioRepository portfolioRepository;
    private final MarketDataService marketDataService;
    private final CryptoDataService cryptoDataService;

    /**
     * 포트폴리오 가격 업데이트 스케줄러 (Fallback)
     * EDA 도입으로 인해 실시간 업데이트는 Kafka 이벤트를 통해 처리됨.
     * 이 스케줄러는 데이터 정합성을 위한 Fallback 용도로 1시간마다 실행.
     * 
     * 실행 주기: 1시간 (fixedRate = 3600000ms)
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 60000)
    @Transactional
    public void updateAllPortfolioPrices() {
        log.info("🚀 [Scheduled] Portfolio Price Update Started");

        try {
            // 모든 포트폴리오 조회 (WithHoldings)
            List<Portfolio> portfolios = portfolioRepository.findAll();

            if (portfolios.isEmpty()) {
                log.info("📭 업데이트할 포트폴리오가 없습니다.");
                return;
            }

            int totalPortfolios = portfolios.size();
            int totalHoldings = 0;
            int successCount = 0;
            int failCount = 0;

            // 각 포트폴리오의 가격 업데이트
            for (Portfolio portfolio : portfolios) {
                try {
                    totalHoldings += portfolio.getHoldings().size();

                    // 포트폴리오의 모든 Holding 가격 업데이트
                    for (PortfolioHolding holding : portfolio.getHoldings()) {
                        try {
                            updateHoldingPrice(holding);
                            successCount++;
                        } catch (Exception e) {
                            failCount++;
                            log.warn("가격 업데이트 실패. Portfolio: {}, Symbol: {}, Error: {}",
                                    portfolio.getId(), holding.getSymbol(), e.getMessage());
                        }
                    }

                    // 포트폴리오 총합 재계산
                    portfolio.recalculate();
                    portfolioRepository.save(portfolio);

                } catch (Exception e) {
                    log.error("포트폴리오 업데이트 실패. Portfolio ID: {}, Error: {}",
                            portfolio.getId(), e.getMessage());
                }
            }

            log.info("✅ [Scheduled] Portfolio Price Update Completed. " +
                    "Portfolios: {}, Holdings: {}, Success: {}, Fail: {}",
                    totalPortfolios, totalHoldings, successCount, failCount);

        } catch (Exception e) {
            log.error("❌ [Scheduled] Portfolio Price Update Failed. Error: {}", e.getMessage(), e);
        }
    }

    /**
     * 개별 Holding 가격 업데이트
     * - 자산 타입에 따라 다른 Service 호출 (Stock / Crypto)
     * - 캐싱된 Service를 호출하므로 빠름
     */
    private void updateHoldingPrice(PortfolioHolding holding) {
        if (holding.getAssetType() == AssetType.CRYPTO) {
            CryptoPriceDto cryptoPrice = cryptoDataService.getCryptoPrice(
                    holding.getSymbol(),
                    holding.getBaseCurrency());
            holding.updateCurrentPrice(BigDecimal.valueOf(cryptoPrice.getPrice()));
        } else {
            StockPriceDto stockPrice = marketDataService.getStockPrice(holding.getSymbol());
            holding.updateCurrentPrice(BigDecimal.valueOf(stockPrice.getPrice()));
        }
    }
}
