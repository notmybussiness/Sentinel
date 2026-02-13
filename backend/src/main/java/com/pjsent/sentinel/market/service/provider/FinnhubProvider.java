package com.pjsent.sentinel.market.service.provider;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.pjsent.sentinel.market.config.StockMarketProperties;
import com.pjsent.sentinel.market.dto.StockPriceDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(4)
public class FinnhubProvider implements MarketDataProvider {

    private final RestTemplate restTemplate;
    private final StockMarketProperties properties;

    @Override
    public StockPriceDto getMarketData(String symbol) {
        if (!isAvailable()) {
            throw new IllegalStateException("Finnhub API is unavailable.");
        }

        log.info("Fetch market data from Finnhub. symbol={}", symbol);

        try {
            String url = buildQuoteUrl(symbol);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseQuoteResponse(symbol, response.getBody());
            }
            throw new RuntimeException("Finnhub API response error");

        } catch (Exception e) {
            throw new RuntimeException("Finnhub API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return properties.getFinnhub().isEnabled()
                && properties.getFinnhub().getApiKey() != null
                && !properties.getFinnhub().getApiKey().trim().isEmpty();
    }

    @Override
    public String getProviderName() {
        return "Finnhub";
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
    public boolean supportsSymbol(String symbol) {
        return symbol != null && !symbol.matches("\\d{6}");
    }

    private String buildQuoteUrl(String symbol) {
        return String.format("%s/quote?symbol=%s&token=%s",
                properties.getFinnhub().getBaseUrl(),
                symbol,
                properties.getFinnhub().getApiKey());
    }

    private StockPriceDto parseQuoteResponse(String symbol, Map<String, Object> response) {
        Object currentPriceObj = response.get("c");
        Object openPriceObj = response.get("o");
        Object highPriceObj = response.get("h");
        Object lowPriceObj = response.get("l");
        Object previousCloseObj = response.get("pc");
        Object timestampObj = response.get("t");

        if (currentPriceObj == null) {
            throw new RuntimeException("Invalid response data");
        }

        double currentPrice = parseDouble(currentPriceObj);
        double openPrice = parseDouble(openPriceObj);
        double highPrice = parseDouble(highPriceObj);
        double lowPrice = parseDouble(lowPriceObj);
        double previousClose = parseDouble(previousCloseObj);

        double change = currentPrice - previousClose;
        double changePercent = previousClose != 0 ? (change / previousClose) * 100 : 0.0;

        LocalDateTime timestamp = LocalDateTime.now();
        if (timestampObj instanceof Number) {
            long unixTimestamp = ((Number) timestampObj).longValue();
            timestamp = LocalDateTime.ofEpochSecond(unixTimestamp, 0, java.time.ZoneOffset.UTC);
        }

        return StockPriceDto.builder()
                .symbol(symbol)
                .price(currentPrice)
                .open(openPrice)
                .high(highPrice)
                .low(lowPrice)
                .close(previousClose)
                .change(change)
                .changePercent(changePercent)
                .lastTradingDay(timestamp.toLocalDate().toString())
                .timeStamp(timestamp)
                .provider(getProviderName())
                .build();
    }

    private double parseDouble(Object value) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        if (value instanceof String str) {
            String trimmed = str.trim();
            if (trimmed.isEmpty() || "N/A".equals(trimmed)) {
                return 0.0;
            }
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        return 0.0;
    }
}
