package com.pjsent.sentinel.market.service.factory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import com.pjsent.sentinel.market.service.provider.MarketDataProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class MarketDataProviderFactory {

    private static final Map<String, Integer> KR_QUOTE_PRIORITY = Map.of(
            "koreainvestment", 1,
            "finnhub", 2,
            "alphavantage", 3,
            "yahoofinance", 4
    );

    private static final Map<String, Integer> GLOBAL_QUOTE_PRIORITY = Map.of(
            "finnhub", 1,
            "alphavantage", 2,
            "yahoofinance", 3,
            "koreainvestment", 4
    );

    private static final Map<String, Integer> SEARCH_PRIORITY = Map.of(
            "alphavantage", 1,
            "koreainvestment", 2,
            "finnhub", 3,
            "yahoofinance", 4
    );

    private final List<MarketDataProvider> providers;

    public List<MarketDataProvider> getAvailableProviders() {
        List<MarketDataProvider> availableProviders = providers.stream()
                .filter(MarketDataProvider::isAvailable)
                .collect(Collectors.toList());
        AnnotationAwareOrderComparator.sort(availableProviders);

        log.debug("Available providers: {}, all providers: {}",
                availableProviders.size(), providers.size());

        return availableProviders;
    }

    public List<MarketDataProvider> getQuoteProviders(String symbol) {
        Map<String, Integer> priority = isKoreanSymbol(symbol) ? KR_QUOTE_PRIORITY : GLOBAL_QUOTE_PRIORITY;

        return getAvailableProviders().stream()
                .filter(provider -> provider.supportsSymbol(symbol))
                .sorted(Comparator.comparingInt(provider -> priorityValue(priority, provider)))
                .toList();
    }

    public List<MarketDataProvider> getSearchProviders() {
        return getAvailableProviders().stream()
                .filter(MarketDataProvider::supportsSearch)
                .sorted(Comparator.comparingInt(provider -> priorityValue(SEARCH_PRIORITY, provider)))
                .toList();
    }

    public MarketDataProvider getPrimaryProvider() {
        return getAvailableProviders().stream()
                .findFirst()
                .orElse(null);
    }

    public MarketDataProvider getProvider(String providerName) {
        String normalizedName = normalizeProviderName(providerName);

        List<MarketDataProvider> sortedProviders = new ArrayList<>(providers);
        AnnotationAwareOrderComparator.sort(sortedProviders);

        return sortedProviders.stream()
                .filter(provider -> normalizeProviderName(provider.getProviderName()).equals(normalizedName))
                .filter(MarketDataProvider::isAvailable)
                .findFirst()
                .orElse(null);
    }

    public void logProviderStatus() {
        log.info("=== Market Data Provider Status ===");
        providers.forEach(provider -> {
            String status = provider.isAvailable() ? "AVAILABLE" : "UNAVAILABLE";
            log.info("{}: {}", provider.getProviderName(), status);
        });
        log.info("===================================");
    }

    public boolean hasAvailableProvider() {
        return !getAvailableProviders().isEmpty();
    }

    private int priorityValue(Map<String, Integer> priorityMap, MarketDataProvider provider) {
        return priorityMap.getOrDefault(normalizeProviderName(provider.getProviderName()), Integer.MAX_VALUE);
    }

    private String normalizeProviderName(String providerName) {
        if (providerName == null) {
            return "";
        }
        return providerName.replaceAll("\\s+", "").toLowerCase();
    }

    private boolean isKoreanSymbol(String symbol) {
        return symbol != null && symbol.matches("\\d{6}");
    }
}
