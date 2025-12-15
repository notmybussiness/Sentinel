package com.pjsent.sentinel.backtest.service;

import com.pjsent.sentinel.backtest.dto.HistoricalPriceData;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 한국 주식 과거 가격 데이터 조회 서비스
 * KIS (한국투자증권) FHKST03010100 API를 사용하여 일봉 데이터 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisHistoricalDataService {

    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;

    @Value("${stock.market.korea-investment.base-url}")
    private String baseUrl;

    @Value("${stock.market.korea-investment.app-key}")
    private String appKey;

    @Value("${stock.market.korea-investment.app-secret}")
    private String appSecret;

    // 토큰 관리
    private String accessToken;
    private Instant tokenExpiryTime;

    // API 엔드포인트 및 TR ID
    private static final String TOKEN_ENDPOINT = "/oauth2/tokenP";
    private static final String DAILY_PRICE_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
    private static final String TR_ID_DAILY_PRICE = "FHKST03010100";  // 국내주식 기간별 시세 (일봉)

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 특정 한국 주식의 과거 가격 데이터를 조회합니다.
     *
     * Redis 캐시 적용 (7일 TTL):
     * - Cache Key: symbol + "_" + startDate + "_" + endDate
     * - sync=true: 동시 요청 중복 방지
     *
     * Circuit Breaker 적용:
     * - kisApi 인스턴스 사용 (기존 KoreaInvestmentProvider와 공유)
     * - Fallback: 캐시된 데이터 반환 시도
     *
     * @param symbol 종목 코드 (예: 005930 삼성전자)
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 과거 가격 데이터 목록
     */
    @CircuitBreaker(name = "kisApi", fallbackMethod = "getHistoricalPricesFallback")
    @Cacheable(
        value = "kisHistoricalData",
        key = "#symbol + '_' + #startDate.toString() + '_' + #endDate.toString()",
        sync = true
    )
    public List<HistoricalPriceData> getHistoricalPrices(
            String symbol, LocalDate startDate, LocalDate endDate) {

        log.info("Fetching KIS historical prices for {}: {} to {}", symbol, startDate, endDate);

        ensureValidToken();

        // KIS 일봉 API 호출
        String url = buildDailyPriceUrl(symbol, startDate, endDate);
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        log.debug("KIS Daily Price API URL: {}", url);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return parseDailyPriceResponse(response.getBody(), startDate, endDate);
        } else {
            log.warn("KIS API response error. Status: {}", response.getStatusCode());
            throw new RuntimeException("KIS API response error");
        }
    }

    /**
     * 일봉 조회 URL 생성
     */
    private String buildDailyPriceUrl(String symbol, LocalDate startDate, LocalDate endDate) {
        return String.format("%s%s?fid_cond_mrkt_div_code=J&fid_input_iscd=%s&fid_input_date_1=%s&fid_input_date_2=%s&fid_period_div_code=D&fid_org_adj_prc=0",
                baseUrl, DAILY_PRICE_ENDPOINT, symbol,
                startDate.format(DATE_FORMATTER), endDate.format(DATE_FORMATTER));
    }

    /**
     * 인증 헤더 생성
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", TR_ID_DAILY_PRICE);
        return headers;
    }

    /**
     * 토큰 유효성 확인 및 갱신
     */
    private void ensureValidToken() {
        if (accessToken == null || tokenExpiryTime == null || Instant.now().isAfter(tokenExpiryTime.minusSeconds(3600))) {
            refreshAccessToken();
        }
    }

    /**
     * OAuth 토큰 발급/갱신
     */
    private void refreshAccessToken() {
        String url = baseUrl + TOKEN_ENDPOINT;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        Map<String, String> body = Map.of(
            "grant_type", "client_credentials",
            "appkey", appKey,
            "appsecret", appSecret
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            accessToken = (String) responseBody.get("access_token");
            Integer expiresIn = (Integer) responseBody.get("expires_in");

            if (accessToken == null) {
                throw new RuntimeException("Token issuance failed: no access_token");
            }

            tokenExpiryTime = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 86400);
            log.info("KIS token issued. Expires at: {}", tokenExpiryTime);
        } else {
            throw new RuntimeException("Token issuance failed: " + response.getStatusCode());
        }
    }

    /**
     * 일봉 응답 파싱
     *
     * KIS API 응답 구조:
     * {
     *   "rt_cd": "0",
     *   "output2": [
     *     { "stck_bsop_date": "20231215", "stck_oprc": "70000", ... }
     *   ]
     * }
     */
    @SuppressWarnings("unchecked")
    private List<HistoricalPriceData> parseDailyPriceResponse(
            Map<String, Object> response, LocalDate startDate, LocalDate endDate) {

        String rtCd = (String) response.get("rt_cd");
        if (!"0".equals(rtCd)) {
            String msg1 = (String) response.get("msg1");
            log.warn("KIS API error: {}", msg1);
            throw new RuntimeException("KIS API error: " + msg1);
        }

        List<Map<String, Object>> output2 = (List<Map<String, Object>>) response.get("output2");
        if (output2 == null || output2.isEmpty()) {
            log.warn("No daily price data in KIS response");
            return new ArrayList<>();
        }

        List<HistoricalPriceData> result = new ArrayList<>();

        for (Map<String, Object> dayData : output2) {
            try {
                String dateStr = (String) dayData.get("stck_bsop_date");
                if (dateStr == null || dateStr.isEmpty()) continue;

                LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);

                // 날짜 범위 필터링
                if (date.isBefore(startDate) || date.isAfter(endDate)) continue;

                double open = parseDouble((String) dayData.get("stck_oprc"));
                double high = parseDouble((String) dayData.get("stck_hgpr"));
                double low = parseDouble((String) dayData.get("stck_lwpr"));
                double close = parseDouble((String) dayData.get("stck_clpr"));
                long volume = parseLong((String) dayData.get("acml_vol"));

                HistoricalPriceData priceData = HistoricalPriceData.builder()
                        .date(date)
                        .open(open)
                        .high(high)
                        .low(low)
                        .close(close)
                        .volume(volume)
                        .adjustedClose(close) // 수정주가 (현재는 종가와 동일)
                        .build();

                result.add(priceData);
            } catch (Exception e) {
                log.warn("Failed to parse day data: {}", e.getMessage());
            }
        }

        // 날짜 오름차순 정렬
        result.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        log.info("Parsed {} KIS historical price points", result.size());
        return result;
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) return 0L;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Circuit Breaker Fallback 메서드
     */
    @SuppressWarnings("unchecked")
    public List<HistoricalPriceData> getHistoricalPricesFallback(
            String symbol, LocalDate startDate, LocalDate endDate, Throwable throwable) {

        log.warn("Circuit Breaker fallback for KIS {} ({} to {}): {}",
                symbol, startDate, endDate, throwable.getMessage());

        // 캐시에서 데이터 조회 시도
        Cache cache = cacheManager.getCache("kisHistoricalData");
        if (cache != null) {
            String cacheKey = symbol + "_" + startDate.toString() + "_" + endDate.toString();
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null && cached.get() != null) {
                log.info("Returning cached KIS data for {} from fallback", symbol);
                return (List<HistoricalPriceData>) cached.get();
            }
        }

        log.warn("No cached KIS data available for {}. Returning empty list.", symbol);
        return new ArrayList<>();
    }

    /**
     * KIS API 사용 가능 여부 확인
     */
    public boolean isApiAvailable() {
        return appKey != null && !appKey.isEmpty()
                && appSecret != null && !appSecret.isEmpty();
    }
}
