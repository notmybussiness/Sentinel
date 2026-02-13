package com.pjsent.sentinel.market.service.provider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.pjsent.sentinel.market.config.StockMarketProperties;
import com.pjsent.sentinel.market.dto.SearchResultDto;
import com.pjsent.sentinel.market.dto.StockPriceDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(2)
public class AlphaVantageProvider implements MarketDataProvider {

    private static final String QUOTE_FUNCTION = "GLOBAL_QUOTE";

    private final RestTemplate restTemplate;
    private final StockMarketProperties properties;

    @Override
    public StockPriceDto getMarketData(String symbol) {
        if (!isAvailable()) {
            throw new IllegalStateException("AlphaVantage API is unavailable.");
        }

        log.info("Fetch market data from AlphaVantage. symbol={}", symbol);

        try {
            String url = buildQuoteUrl(symbol);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseQuoteResponse(symbol, response.getBody());
            }
            throw new RuntimeException("AlphaVantage API response error");

        } catch (Exception e) {
            throw new RuntimeException("AlphaVantage API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return properties.getAlphavantage().isEnabled()
                && properties.getAlphavantage().getApiKey() != null
                && !properties.getAlphavantage().getApiKey().trim().isEmpty();
    }

    @Override
    public String getProviderName() {
        return "AlphaVantage";
    }

    @Override
    public boolean supportsTimeSeries() {
        return true;
    }

    @Override
    public boolean supportsHistoricalData() {
        return true;
    }

    @Override
    public boolean supportsSearch() {
        return true;
    }

    @Override
    public boolean supportsSymbol(String symbol) {
        return symbol != null && !symbol.matches("\\d{6}");
    }

    @Override
    @Cacheable(value = "stockSearch", key = "#query", sync = true)
    public List<SearchResultDto> searchSymbol(String query) {
        if (!isAvailable()) {
            throw new IllegalStateException("AlphaVantage API is unavailable.");
        }

        try {
            String url = buildSearchUrl(query);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseSearchResponse(response.getBody());
            }
            return List.of();

        } catch (Exception e) {
            log.warn("AlphaVantage search failed. query={}, error={}", query, e.getMessage());
            return List.of();
        }
    }

    private String buildQuoteUrl(String symbol) {
        return String.format("%s?function=%s&symbol=%s&apikey=%s",
                properties.getAlphavantage().getBaseUrl(),
                QUOTE_FUNCTION,
                symbol,
                properties.getAlphavantage().getApiKey());
    }

    private String buildSearchUrl(String query) {
        return String.format("%s?function=SYMBOL_SEARCH&keywords=%s&apikey=%s",
                properties.getAlphavantage().getBaseUrl(),
                query,
                properties.getAlphavantage().getApiKey());
    }

    @SuppressWarnings("unchecked")
    private StockPriceDto parseQuoteResponse(String symbol, Map<String, Object> response) {
        Map<String, Object> globalQuote = (Map<String, Object>) response.get("Global Quote");
        if (globalQuote == null || globalQuote.isEmpty()) {
            throw new RuntimeException("Invalid response data");
        }

        double price = parseDouble((String) globalQuote.get("05. price"));
        double open = parseDouble((String) globalQuote.get("02. open"));
        double high = parseDouble((String) globalQuote.get("03. high"));
        double low = parseDouble((String) globalQuote.get("04. low"));
        double close = parseDouble((String) globalQuote.get("08. previous close"));
        double change = parseDouble((String) globalQuote.get("09. change"));
        String changePercentRaw = (String) globalQuote.get("10. change percent");
        double changePercent = parseDouble(changePercentRaw == null ? null : changePercentRaw.replace("%", ""));
        String lastTradingDay = (String) globalQuote.get("07. latest trading day");

        return StockPriceDto.builder()
                .symbol(symbol)
                .price(price)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .change(change)
                .changePercent(changePercent)
                .lastTradingDay(lastTradingDay)
                .timeStamp(LocalDateTime.now())
                .provider(getProviderName())
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<SearchResultDto> parseSearchResponse(Map<String, Object> response) {
        List<Map<String, String>> bestMatches = (List<Map<String, String>>) response.get("bestMatches");
        if (bestMatches == null || bestMatches.isEmpty()) {
            return List.of();
        }

        List<SearchResultDto> results = new ArrayList<>();
        for (Map<String, String> match : bestMatches) {
            String symbol = match.get("1. symbol");
            String name = match.get("2. name");
            String type = match.get("3. type");
            String region = match.get("4. region");

            if (symbol != null && name != null) {
                results.add(new SearchResultDto(symbol, name, region, type));
            }

            if (results.size() >= 10) {
                break;
            }
        }

        return results;
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty() || "N/A".equals(value)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
