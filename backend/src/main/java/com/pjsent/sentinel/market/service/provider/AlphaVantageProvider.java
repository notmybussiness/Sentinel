package com.pjsent.sentinel.market.service.provider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.pjsent.sentinel.market.dto.StockPriceDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class AlphaVantageProvider implements MarketDataProvider {
    
    private final RestTemplate restTemplate;
    
    @Value("${stock.market.alphavantage.api-key}")
    private String apiKey;
    
    @Value("${stock.market.alphavantage.base-url}")
    private String baseUrl;
    
    @Value("${stock.market.alphavantage.enabled:true}")
    private boolean enabled;
    
    private static final String QUOTE_FUNCTION = "GLOBAL_QUOTE";

    
    @Override
    public StockPriceDto getMarketData(String symbol) {
        if (!isAvailable()) {
            throw new IllegalStateException("AlphaVantage API가 사용 불가능합니다.");
        }
        
        log.info("AlphaVantage에서 {} 심볼의 시장 데이터를 가져오는 중", symbol);
        
        try {
            String url = buildQuoteUrl(symbol);
            log.debug("AlphaVantage API 호출 URL: {}", url);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseQuoteResponse(symbol, response.getBody());
            } else {
                log.warn("AlphaVantage API 응답이 비정상입니다. 상태코드: {}", response.getStatusCode());
                throw new RuntimeException("AlphaVantage API 응답 오류");
            }
            
        } catch (Exception e) {
            log.error("AlphaVantage API 호출 중 오류 발생. 심볼: {}, 오류: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("AlphaVantage API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
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
    
    private String buildQuoteUrl(String symbol) {
        return String.format("%s?function=%s&symbol=%s&apikey=%s", 
                           baseUrl, QUOTE_FUNCTION, symbol, apiKey);
    }
    
    @SuppressWarnings("unchecked")
    private StockPriceDto parseQuoteResponse(String symbol, Map<String, Object> response) {
        try {
            Map<String, Object> globalQuote = (Map<String, Object>) response.get("Global Quote");
            
            if (globalQuote == null || globalQuote.isEmpty()) {
                log.warn("AlphaVantage 응답에서 Global Quote 데이터를 찾을 수 없습니다. 심볼: {}", symbol);
                throw new RuntimeException("유효하지 않은 응답 데이터");
            }
            
            // AlphaVantage 응답 필드명 (문자열로 접근)
            String priceStr = (String) globalQuote.get("05. price");
            String openStr = (String) globalQuote.get("02. open");
            String highStr = (String) globalQuote.get("03. high");
            String lowStr = (String) globalQuote.get("04. low");
            String closeStr = (String) globalQuote.get("08. previous close");
            String changeStr = (String) globalQuote.get("09. change");
            String changePercentStr = (String) globalQuote.get("10. change percent");
            String lastTradingDayStr = (String) globalQuote.get("07. latest trading day");
            
            // 문자열을 숫자로 변환
            double price = parseDouble(priceStr);
            double open = parseDouble(openStr);
            double high = parseDouble(highStr);
            double low = parseDouble(lowStr);
            double close = parseDouble(closeStr);
            double change = parseDouble(changeStr);
            double changePercent = parseDouble(changePercentStr.replace("%", ""));
            
            log.debug("AlphaVantage 데이터 파싱 완료. 심볼: {}, 가격: {}", symbol, price);
            
            return StockPriceDto.builder()
                    .symbol(symbol)
                    .price(price)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .change(change)
                    .changePercent(changePercent)
                    .lastTradingDay(lastTradingDayStr)
                    .timeStamp(LocalDateTime.now())
                    .provider(getProviderName())
                    .build();
                    
        } catch (Exception e) {
            log.error("AlphaVantage 응답 파싱 중 오류 발생. 심볼: {}, 응답: {}, 오류: {}", 
                     symbol, response, e.getMessage(), e);
            throw new RuntimeException("응답 데이터 파싱 실패: " + e.getMessage(), e);
        }
    }
    
    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty() || "N/A".equals(value)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: {}", value);
            return 0.0;
        }
    }
}
