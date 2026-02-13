package com.pjsent.sentinel.market.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.pjsent.sentinel.common.event.PriceUpdateEvent;
import com.pjsent.sentinel.market.dto.MarketIndexDto;
import com.pjsent.sentinel.market.dto.SearchResultDto;
import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.producer.MarketPriceProducer;
import com.pjsent.sentinel.market.service.factory.MarketDataProviderFactory;
import com.pjsent.sentinel.market.service.provider.MarketDataProvider;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding.AssetType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MarketDataService {

    private final MarketDataProviderFactory providerFactory;
    private final MarketPriceProducer marketPriceProducer;
    private final Clock clock;

    @Autowired
    public MarketDataService(MarketDataProviderFactory providerFactory,
            MarketPriceProducer marketPriceProducer) {
        this(providerFactory, marketPriceProducer, Clock.systemDefaultZone());
    }

    public MarketDataService(MarketDataProviderFactory providerFactory,
            MarketPriceProducer marketPriceProducer,
            Clock clock) {
        this.providerFactory = providerFactory;
        this.marketPriceProducer = marketPriceProducer;
        this.clock = clock;
    }

    @Cacheable(value = "stockPrice", key = "#symbol", sync = true)
    public StockPriceDto getStockPrice(String symbol) {
        log.info("Request stock price. symbol={}", symbol);

        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol is required.");
        }

        List<MarketDataProvider> quoteProviders = providerFactory.getQuoteProviders(symbol);
        if (quoteProviders.isEmpty()) {
            throw new RuntimeException("No available market data providers.");
        }

        Exception lastException = null;

        for (MarketDataProvider provider : quoteProviders) {
            try {
                StockPriceDto result = provider.getMarketData(symbol);
                if (result != null && result.getPrice() > 0) {
                    return result;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Provider failed. provider={}, symbol={}, error={}",
                        provider.getProviderName(), symbol, e.getMessage());
            }
        }

        throw new RuntimeException("All market data providers failed. symbol=" + symbol, lastException);
    }

    public StockPriceDto refreshStockPriceAndPublish(String symbol) {
        StockPriceDto result = getStockPrice(symbol);

        try {
            marketPriceProducer.publishPriceUpdate(new PriceUpdateEvent(
                    symbol,
                    java.math.BigDecimal.valueOf(result.getPrice()),
                    AssetType.STOCK,
                    LocalDateTime.now(clock)));
        } catch (Exception e) {
            log.error("Failed to publish price update event. symbol={}", symbol, e);
        }

        return result;
    }

    public List<StockPriceDto> getStockPrices(List<String> symbols) {
        return symbols.stream()
                .map(this::getStockPrice)
                .collect(Collectors.toList());
    }

    public String getProviderStatus() {
        providerFactory.logProviderStatus();
        return "Provider status logged.";
    }

    public boolean isServiceAvailable() {
        return providerFactory.hasAvailableProvider();
    }

    @Cacheable(value = "marketIndices", sync = true)
    public List<MarketIndexDto> getMarketIndices() {
        List<String> indexSymbols = List.of("^GSPC", "^IXIC", "^DJI", "^KS11");

        return indexSymbols.parallelStream()
                .map(this::fetchIndexData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private MarketIndexDto fetchIndexData(String symbol) {
        try {
            StockPriceDto price = getStockPrice(symbol);

            return MarketIndexDto.builder()
                    .symbol(symbol)
                    .name(getIndexName(symbol))
                    .value(price.getPrice())
                    .change(price.getChange())
                    .changePercent(price.getChangePercent())
                    .timestamp(LocalDateTime.now(clock))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch index data. symbol={}, error={}", symbol, e.getMessage());
            return null;
        }
    }

    private String getIndexName(String symbol) {
        return switch (symbol) {
            case "^GSPC" -> "S&P 500";
            case "^IXIC" -> "NASDAQ";
            case "^DJI" -> "DOW";
            case "^KS11" -> "KOSPI";
            default -> symbol;
        };
    }

    public List<SearchResultDto> searchSymbol(String query) {
        log.info("Search symbol. query={}", query);

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query is required.");
        }

        List<MarketDataProvider> searchProviders = providerFactory.getSearchProviders();
        for (MarketDataProvider provider : searchProviders) {
            try {
                List<SearchResultDto> results = provider.searchSymbol(query);
                if (results != null && !results.isEmpty()) {
                    return results;
                }
            } catch (Exception e) {
                log.warn("Search failed. provider={}, query={}, error={}",
                        provider.getProviderName(), query, e.getMessage());
            }
        }

        return List.of();
    }
}
