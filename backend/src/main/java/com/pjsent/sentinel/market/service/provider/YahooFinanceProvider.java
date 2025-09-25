package com.pjsent.sentinel.market.service.provider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.market.dto.StockPriceDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Yahoo Finance API 프로바이더
 * 무료 무제한 API를 통해 실시간 주식 데이터를 제공합니다.
 * 15-20분 지연되지만 안정적이고 글로벌 시장을 지원합니다.
 */
@Component
@Order(1) // 최우선 순위 - AlphaVantage보다 먼저 시도
@Slf4j
public class YahooFinanceProvider implements MarketDataProvider {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Yahoo Finance 전용 RestTemplate과 ObjectMapper를 주입받는 생성자
    public YahooFinanceProvider(
            @Qualifier("yahooRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // Yahoo Finance Chart API 엔드포인트
    private static final String YAHOO_FINANCE_BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";

    // 서비스 가용성 체크용 테스트 심볼
    private static final String HEALTH_CHECK_SYMBOL = "AAPL";

    @Override
    public StockPriceDto getMarketData(String symbol) {
        if (!isAvailable()) {
            throw new IllegalStateException("Yahoo Finance API가 사용 불가능합니다.");
        }

        log.info("Yahoo Finance에서 {} 심볼의 시장 데이터를 가져오는 중", symbol);

        try {
            String url = buildChartUrl(symbol);
            log.debug("Yahoo Finance API 호출 URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseYahooResponse(symbol, response.getBody());
            } else {
                log.warn("Yahoo Finance API 응답이 비정상입니다. 상태코드: {}", response.getStatusCode());
                throw new RuntimeException("Yahoo Finance API 응답 오류");
            }

        } catch (Exception e) {
            log.error("Yahoo Finance API 호출 중 오류 발생. 심볼: {}, 오류: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Yahoo Finance API 호출 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // 간단한 헬스체크 - Apple 주식으로 테스트
            String testUrl = buildChartUrl(HEALTH_CHECK_SYMBOL);
            ResponseEntity<String> response = restTemplate.getForEntity(testUrl, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // 응답에 chart 데이터가 있는지 확인
                return response.getBody().contains("\"chart\"") &&
                       response.getBody().contains("\"result\"");
            }
            return false;

        } catch (Exception e) {
            log.warn("Yahoo Finance 서비스 가용성 체크 실패: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Yahoo Finance";
    }

    @Override
    public boolean supportsTimeSeries() {
        return true;
    }

    @Override
    public boolean supportsHistoricalData() {
        return true;
    }

    /**
     * 심볼을 Yahoo Finance 형식으로 변환
     */
    private String convertToYahooSymbol(String symbol) {
        // 한국 주식: 6자리 숫자 (예: 005930 → 005930.KS)
        if (symbol.matches("\\d{6}")) {
            return symbol + ".KS";
        }
        // 일본 주식: 4자리 숫자 (예: 7203 → 7203.T)
        if (symbol.matches("\\d{4}")) {
            return symbol + ".T";
        }
        // 미국 주식 및 기타: 그대로 사용
        return symbol;
    }

    /**
     * Yahoo Finance Chart API URL 구성
     */
    private String buildChartUrl(String symbol) {
        // 심볼을 Yahoo Finance 형식으로 변환
        String yahooSymbol = convertToYahooSymbol(symbol);
        // 기본적으로 1일 데이터로 최신 가격 정보를 가져옴
        return YAHOO_FINANCE_BASE_URL + yahooSymbol + "?interval=1d&range=1d";
    }

    /**
     * Yahoo Finance 응답을 StockPriceDto로 변환
     */
    private StockPriceDto parseYahooResponse(String symbol, String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // chart.result[0] 경로로 데이터 접근
            JsonNode chartNode = rootNode.path("chart");
            if (chartNode.isMissingNode()) {
                throw new RuntimeException("Yahoo Finance 응답에서 chart 데이터를 찾을 수 없습니다.");
            }

            JsonNode resultArray = chartNode.path("result");
            if (resultArray.isMissingNode() || !resultArray.isArray() || resultArray.size() == 0) {
                throw new RuntimeException("Yahoo Finance 응답에서 result 데이터를 찾을 수 없습니다.");
            }

            JsonNode resultNode = resultArray.get(0);

            // 메타 데이터에서 현재 가격 정보 추출
            JsonNode metaNode = resultNode.path("meta");
            if (metaNode.isMissingNode()) {
                throw new RuntimeException("Yahoo Finance 응답에서 meta 데이터를 찾을 수 없습니다.");
            }

            // 현재 가격과 이전 종가
            double currentPrice = metaNode.path("regularMarketPrice").asDouble(0.0);
            double previousClose = metaNode.path("previousClose").asDouble(0.0);

            if (currentPrice <= 0) {
                throw new RuntimeException("유효하지 않은 가격 데이터: " + currentPrice);
            }

            // OHLC 데이터 추출 (최신 데이터)
            JsonNode timestampArray = resultNode.path("timestamp");
            JsonNode indicatorsNode = resultNode.path("indicators");
            JsonNode quoteNode = indicatorsNode.path("quote").get(0);

            double open = 0.0, high = 0.0, low = 0.0, close = 0.0;
            LocalDateTime lastTradingDateTime = LocalDateTime.now();

            if (!timestampArray.isMissingNode() && timestampArray.isArray() && timestampArray.size() > 0) {
                int lastIndex = timestampArray.size() - 1;

                // 타임스탬프
                long timestamp = timestampArray.get(lastIndex).asLong();
                lastTradingDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()
                );

                // OHLC 데이터
                if (!quoteNode.isMissingNode()) {
                    JsonNode openArray = quoteNode.path("open");
                    JsonNode highArray = quoteNode.path("high");
                    JsonNode lowArray = quoteNode.path("low");
                    JsonNode closeArray = quoteNode.path("close");

                    if (openArray.isArray() && lastIndex < openArray.size()) {
                        open = openArray.get(lastIndex).asDouble(0.0);
                    }
                    if (highArray.isArray() && lastIndex < highArray.size()) {
                        high = highArray.get(lastIndex).asDouble(0.0);
                    }
                    if (lowArray.isArray() && lastIndex < lowArray.size()) {
                        low = lowArray.get(lastIndex).asDouble(0.0);
                    }
                    if (closeArray.isArray() && lastIndex < closeArray.size()) {
                        close = closeArray.get(lastIndex).asDouble(0.0);
                    }
                }
            }

            // 변화량 계산
            double change = currentPrice - previousClose;
            double changePercent = previousClose > 0 ? (change / previousClose) * 100 : 0.0;

            log.debug("Yahoo Finance 데이터 파싱 완료. 심볼: {}, 가격: ${}, 변화: {}%",
                     symbol, currentPrice, String.format("%.2f", changePercent));

            return StockPriceDto.builder()
                    .symbol(symbol)
                    .price(currentPrice)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .change(change)
                    .changePercent(changePercent)
                    .lastTradingDay(lastTradingDateTime.toLocalDate().toString())
                    .timeStamp(lastTradingDateTime)
                    .provider(getProviderName())
                    .build();

        } catch (Exception e) {
            log.error("Yahoo Finance 응답 파싱 중 오류 발생. 심볼: {}, 오류: {}",
                     symbol, e.getMessage(), e);
            throw new RuntimeException("Yahoo Finance 응답 데이터 파싱 실패: " + e.getMessage(), e);
        }
    }
}