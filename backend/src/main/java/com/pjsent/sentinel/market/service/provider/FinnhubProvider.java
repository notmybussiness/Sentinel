package com.pjsent.sentinel.market.service.provider;

import java.time.LocalDateTime;
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
public class FinnhubProvider implements MarketDataProvider {
    
    private final RestTemplate restTemplate;
    
    @Value("${stock.market.finnhub.api-key}")
    private String apiKey;
    
    @Value("${stock.market.finnhub.base-url}")
    private String baseUrl;
    
    @Value("${stock.market.finnhub.enabled:true}")
    private boolean enabled;
    
    @Override
    public StockPriceDto getMarketData(String symbol) {
        if (!isAvailable()) {
            throw new IllegalStateException("Finnhub API가 사용 불가능합니다.");
        }
        
        log.info("Finnhub에서 {} 심볼의 시장 데이터를 가져오는 중", symbol);
        
        try {
            String url = buildQuoteUrl(symbol);
            log.debug("Finnhub API 호출 URL: {}", url);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseQuoteResponse(symbol, response.getBody());
            } else {
                log.warn("Finnhub API 응답이 비정상입니다. 상태코드: {}", response.getStatusCode());
                throw new RuntimeException("Finnhub API 응답 오류");
            }
            
        } catch (Exception e) {
            log.error("Finnhub API 호출 중 오류 발생. 심볼: {}, 오류: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Finnhub API 호출 실패: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
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
    
    private String buildQuoteUrl(String symbol) {
        return String.format("%s/quote?symbol=%s&token=%s", 
                           baseUrl, symbol, apiKey);
    }
    

    private StockPriceDto parseQuoteResponse(String symbol, Map<String, Object> response) {
        try {
            // Finnhub 응답 필드 확인
            Object currentPriceObj = response.get("c");
            Object openPriceObj = response.get("o");
            Object highPriceObj = response.get("h");
            Object lowPriceObj = response.get("l");
            Object previousCloseObj = response.get("pc");
            Object timestampObj = response.get("t");
            
            if (currentPriceObj == null) {
                log.warn("Finnhub 응답에서 현재 가격 데이터를 찾을 수 없습니다. 심볼: {}", symbol);
                throw new RuntimeException("유효하지 않은 응답 데이터");
            }
            
            // 숫자 변환
            double currentPrice = parseDouble(currentPriceObj);
            double openPrice = parseDouble(openPriceObj);
            double highPrice = parseDouble(highPriceObj);
            double lowPrice = parseDouble(lowPriceObj);
            double previousClose = parseDouble(previousCloseObj);
            
            // 변화량 계산
            double change = currentPrice - previousClose;
            double changePercent = previousClose != 0 ? (change / previousClose) * 100 : 0.0;
            
            // 타임스탬프 처리 (Unix timestamp)
            LocalDateTime timestamp = LocalDateTime.now();
            if (timestampObj instanceof Number) {
                long unixTimestamp = ((Number) timestampObj).longValue();
                timestamp = LocalDateTime.ofEpochSecond(unixTimestamp, 0, 
                    java.time.ZoneOffset.UTC);
            }
            
            log.debug("Finnhub 데이터 파싱 완료. 심볼: {}, 가격: {}", symbol, currentPrice);
            
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
                    
        } catch (Exception e) {
            log.error("Finnhub 응답 파싱 중 오류 발생. 심볼: {}, 응답: {}, 오류: {}", 
                     symbol, response, e.getMessage(), e);
            throw new RuntimeException("응답 데이터 파싱 실패: " + e.getMessage(), e);
        }
    }
    
    private double parseDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        if (value instanceof String) {
            String strValue = ((String) value).trim();
            if (strValue.isEmpty() || "N/A".equals(strValue)) {
                return 0.0;
            }
            try {
                return Double.parseDouble(strValue);
            } catch (NumberFormatException e) {
                log.warn("숫자 변환 실패: {}", strValue);
                return 0.0;
            }
        }
        
        log.warn("지원하지 않는 데이터 타입: {}", value.getClass().getSimpleName());
        return 0.0;
    }
}
