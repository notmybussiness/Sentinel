package com.pjsent.sentinel.portfolio.consumer;

import com.pjsent.sentinel.common.event.PriceUpdateEvent;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioPriceConsumer {

    private final PortfolioRepository portfolioRepository;

    @KafkaListener(topics = "market-price-updates", groupId = "sentinel-group")
    @Transactional
    public void handlePriceUpdate(PriceUpdateEvent event) {
        log.debug("Received price update event: {}", event);

        // Find all portfolios holding this symbol using the optimized query
        List<Portfolio> portfolios = portfolioRepository.findByHoldings_Symbol(event.symbol());

        int updatedCount = 0;
        for (Portfolio portfolio : portfolios) {
            boolean updated = false;
            for (PortfolioHolding holding : portfolio.getHoldings()) {
                if (holding.getSymbol().equals(event.symbol()) && holding.getAssetType() == event.assetType()) {
                    holding.updateCurrentPrice(event.price());
                    updated = true;
                }
            }

            if (updated) {
                portfolio.recalculate();
                portfolioRepository.save(portfolio);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            log.info("Updated {} portfolios for symbol {}", updatedCount, event.symbol());
        }
    }
}
