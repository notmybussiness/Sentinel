package com.pjsent.sentinel.backtest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.backtest.dto.HistoricalPriceData;
import com.pjsent.sentinel.common.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 과거 가격 데이터 조회 서비스
 * AlphaVantage TIME_SERIES_DAILY API 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricalDataService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Value("${stock.market.alphavantage.base-url}")
    private String baseUrl;

    @Value("${stock.market.alphavantage.api-key}")
    private String apiKey;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 특정 종목의 과거 가격 데이터를 조회합니다.
     *
     * Redis 캐시 적용 (7일 TTL):
     * - Cache Key: symbol + "_" + startDate + "_" + endDate
     * - sync=true: 동시 요청 중복 방지 (request deduplication)
     * - AlphaVantage API 제한: 5 calls/min, 100 calls/day
     *
     * Circuit Breaker 적용:
     * - alphaVantageApi 인스턴스 사용
     * - 50% 실패율 시 Circuit Open
     * - 15초 후 Half-Open 상태로 전환
     * - Fallback: 캐시된 데이터 반환 시도
     *
     * @param symbol 종목 심볼
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 과거 가격 데이터 목록
     */
    @CircuitBreaker(name = "alphaVantageApi", fallbackMethod = "getHistoricalPricesFallback")
    @Cacheable(
        value = "historicalData",
        key = "#symbol + '_' + #startDate.toString() + '_' + #endDate.toString()",
        sync = true
    )
    public List<HistoricalPriceData> getHistoricalPrices(String symbol, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching historical prices for {}: {} to {}", symbol, startDate, endDate);

        // AlphaVantage TIME_SERIES_DAILY API 호출
        String url = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=full&apikey=%s",
                baseUrl, symbol, apiKey);

        try {
            String response = restTemplate.getForObject(url, String.class);
            return parseHistoricalData(response, startDate, endDate);
        } catch (RestClientException e) {
            log.error("Failed to fetch historical data for {}: {}", symbol, e.getMessage());
            throw new BusinessException("Failed to fetch historical data for " + symbol);
        } catch (Exception e) {
            log.error("Error parsing historical data for {}: {}", symbol, e.getMessage());
            throw new BusinessException("Error parsing historical data for " + symbol);
        }
    }

    /**
     * Circuit Breaker Fallback 메서드
     *
     * Circuit이 열려있거나 API 호출 실패 시 호출됩니다.
     * 캐시에서 관련 데이터를 찾아 반환하고, 없으면 빈 리스트를 반환합니다.
     *
     * @param symbol 종목 심볼
     * @param startDate 시작일
     * @param endDate 종료일
     * @param throwable 원인 예외
     * @return 캐시된 데이터 또는 빈 리스트
     */
    @SuppressWarnings("unchecked")
    public List<HistoricalPriceData> getHistoricalPricesFallback(
            String symbol, LocalDate startDate, LocalDate endDate, Throwable throwable) {

        log.warn("Circuit Breaker fallback for {} ({} to {}): {}",
                symbol, startDate, endDate, throwable.getMessage());

        // 캐시에서 데이터 조회 시도
        Cache cache = cacheManager.getCache("historicalData");
        if (cache != null) {
            String cacheKey = symbol + "_" + startDate.toString() + "_" + endDate.toString();
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null && cached.get() != null) {
                log.info("Returning cached data for {} from fallback", symbol);
                return (List<HistoricalPriceData>) cached.get();
            }
        }

        // 캐시에 데이터가 없으면 빈 리스트 반환
        log.warn("No cached data available for {}. Returning empty list.", symbol);
        return new ArrayList<>();
    }

    /**
     * AlphaVantage API 응답을 파싱하여 과거 가격 데이터로 변환합니다.
     *
     * @param jsonResponse API 응답 (JSON)
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 과거 가격 데이터 목록
     */
    private List<HistoricalPriceData> parseHistoricalData(String jsonResponse, LocalDate startDate, LocalDate endDate) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            // Error handling
            if (root.has("Error Message")) {
                String errorMessage = root.get("Error Message").asText();
                throw new BusinessException("AlphaVantage API error: " + errorMessage);
            }

            if (root.has("Note")) {
                String note = root.get("Note").asText();
                log.warn("AlphaVantage API rate limit: {}", note);
                throw new BusinessException("API rate limit exceeded. Please try again later.");
            }

            // Parse time series data
            JsonNode timeSeriesNode = root.get("Time Series (Daily)");
            if (timeSeriesNode == null) {
                throw new BusinessException("Invalid API response: missing time series data");
            }

            List<HistoricalPriceData> prices = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = timeSeriesNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                LocalDate date = LocalDate.parse(entry.getKey(), DATE_FORMATTER);

                // Filter by date range
                if (date.isBefore(startDate) || date.isAfter(endDate)) {
                    continue;
                }

                JsonNode dayData = entry.getValue();

                HistoricalPriceData priceData = HistoricalPriceData.builder()
                        .date(date)
                        .open(dayData.get("1. open").asDouble())
                        .high(dayData.get("2. high").asDouble())
                        .low(dayData.get("3. low").asDouble())
                        .close(dayData.get("4. close").asDouble())
                        .volume(dayData.get("5. volume").asLong())
                        .adjustedClose(dayData.get("4. close").asDouble()) // Use close as adjusted close
                        .build();

                prices.add(priceData);
            }

            // Sort by date ascending
            prices.sort((a, b) -> a.getDate().compareTo(b.getDate()));

            log.info("Parsed {} historical price points", prices.size());
            return prices;

        } catch (Exception e) {
            log.error("Error parsing AlphaVantage response: {}", e.getMessage());
            throw new BusinessException("Failed to parse historical data");
        }
    }

    /**
     * 여러 종목의 과거 가격 데이터를 배치로 조회합니다.
     *
     * @param symbols 종목 심볼 목록
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 종목별 과거 가격 데이터 맵
     */
    public Map<String, List<HistoricalPriceData>> getBatchHistoricalPrices(
            List<String> symbols, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching historical prices for {} symbols", symbols.size());

        return symbols.stream()
                .collect(Collectors.toMap(
                        symbol -> symbol,
                        symbol -> getHistoricalPrices(symbol, startDate, endDate)
                ));
    }

    /**
     * 특정 날짜의 종가를 조회합니다.
     *
     * @param symbol 종목 심볼
     * @param date 날짜
     * @param historicalPrices 과거 가격 데이터
     * @return 종가 (해당 날짜 데이터가 없으면 null)
     */
    public Double getClosePriceOnDate(String symbol, LocalDate date, List<HistoricalPriceData> historicalPrices) {
        return historicalPrices.stream()
                .filter(p -> p.getDate().equals(date))
                .map(HistoricalPriceData::getClose)
                .findFirst()
                .orElse(null);
    }

    /**
     * 가장 최근 거래일의 종가를 조회합니다.
     *
     * @param date 기준 날짜
     * @param historicalPrices 과거 가격 데이터
     * @return 최근 거래일 종가
     */
    public Double getLatestClosePriceBeforeDate(LocalDate date, List<HistoricalPriceData> historicalPrices) {
        return historicalPrices.stream()
                .filter(p -> p.getDate().isBefore(date) || p.getDate().equals(date))
                .reduce((first, second) -> second) // Get last element
                .map(HistoricalPriceData::getClose)
                .orElse(null);
    }

    /**
     * API 상태를 확인합니다.
     *
     * @return API 사용 가능 여부
     */
    public boolean isApiAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * 특정 종목/기간의 과거 데이터 캐시를 삭제합니다.
     *
     * 사용 사례: AlphaVantage가 조정 종가를 업데이트한 경우 (주식 분할, 배당 등)
     *
     * @param symbol 종목 심볼
     * @param startDate 시작일
     * @param endDate 종료일
     */
    @CacheEvict(
        value = "historicalData",
        key = "#symbol + '_' + #startDate.toString() + '_' + #endDate.toString()"
    )
    public void evictHistoricalDataCache(String symbol, LocalDate startDate, LocalDate endDate) {
        log.info("Evicted historical data cache for {}: {} to {}", symbol, startDate, endDate);
    }

    /**
     * 모든 과거 데이터 캐시를 삭제합니다.
     *
     * 사용 사례: 대량 데이터 업데이트 후 전체 캐시 갱신 필요 시
     */
    @CacheEvict(value = "historicalData", allEntries = true)
    public void evictAllHistoricalDataCache() {
        log.info("Evicted all historical data cache");
    }
}
