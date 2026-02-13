package com.pjsent.sentinel.market.service.provider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.pjsent.sentinel.market.config.StockMarketProperties;
import com.pjsent.sentinel.market.dto.SearchResultDto;
import com.pjsent.sentinel.market.dto.StockPriceDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(1)
public class KoreaInvestmentProvider implements MarketDataProvider {

    private static final String TOKEN_ENDPOINT = "/oauth2/tokenP";
    private static final String PRICE_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String SEARCH_ENDPOINT = "/uapi/domestic-stock/v1/quotations/search-info";

    private static final String TR_ID_PRICE = "FHKST01010100";
    private static final String TR_ID_SEARCH = "CTPF1604R";

    private final RestTemplate restTemplate;
    private final StockMarketProperties properties;

    private String accessToken;
    private Instant tokenExpiryTime;

    @Override
    @CircuitBreaker(name = "kisApi", fallbackMethod = "fallbackGetMarketData")
    @RateLimiter(name = "kisApi")
    @Retry(name = "marketDataApi")
    public StockPriceDto getMarketData(String symbol) {
        if (!isAvailable()) {
            throw new IllegalStateException("KoreaInvestment API is unavailable.");
        }

        try {
            ensureValidToken();

            String url = buildUrl(PRICE_ENDPOINT, Map.of(
                    "fid_cond_mrkt_div_code", "J",
                    "fid_input_iscd", symbol
            ));

            HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(TR_ID_PRICE));
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parsePriceResponse(symbol, response.getBody());
            }
            throw new RuntimeException("KoreaInvestment API response error");

        } catch (Exception e) {
            throw new RuntimeException("KoreaInvestment API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        StockMarketProperties.KoreaInvestment cfg = properties.getKoreaInvestment();
        return cfg.isEnabled()
                && cfg.getAppKey() != null && !cfg.getAppKey().trim().isEmpty()
                && cfg.getAppSecret() != null && !cfg.getAppSecret().trim().isEmpty();
    }

    @Override
    public String getProviderName() {
        return "KoreaInvestment";
    }

    @Override
    public boolean supportsSearch() {
        return true;
    }

    @Override
    public boolean supportsSymbol(String symbol) {
        return symbol != null && symbol.matches("\\d{6}");
    }

    @Override
    @Cacheable(value = "stockSearch", key = "#query", sync = true)
    @CircuitBreaker(name = "kisApi", fallbackMethod = "fallbackSearchSymbol")
    @RateLimiter(name = "kisApi")
    public List<SearchResultDto> searchSymbol(String query) {
        if (!isAvailable()) {
            throw new IllegalStateException("KoreaInvestment API is unavailable.");
        }

        try {
            ensureValidToken();

            String url = buildUrl(SEARCH_ENDPOINT, Map.of(
                    "pdno", query,
                    "prdt_type_cd", "300"
            ));

            HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(TR_ID_SEARCH));
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseSearchResponse(response.getBody());
            }
            return List.of();

        } catch (Exception e) {
            log.warn("KoreaInvestment symbol search failed. query={}, error={}", query, e.getMessage());
            return List.of();
        }
    }

    private void ensureValidToken() {
        if (accessToken == null || tokenExpiryTime == null || isTokenExpiringSoon()) {
            refreshAccessToken();
        }
    }

    private boolean isTokenExpiringSoon() {
        if (tokenExpiryTime == null) {
            return true;
        }
        long secondsUntilExpiry = tokenExpiryTime.getEpochSecond() - Instant.now().getEpochSecond();
        return secondsUntilExpiry < properties.getKoreaInvestment().getTokenRefreshBefore();
    }

    private void refreshAccessToken() {
        try {
            String url = properties.getKoreaInvestment().getBaseUrl() + TOKEN_ENDPOINT;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            Map<String, String> body = Map.of(
                    "grant_type", "client_credentials",
                    "appkey", properties.getKoreaInvestment().getAppKey(),
                    "appsecret", properties.getKoreaInvestment().getAppSecret()
            );

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("Token issue failed. status=" + response.getStatusCode());
            }

            Map<String, Object> responseBody = response.getBody();
            accessToken = (String) responseBody.get("access_token");
            Integer expiresIn = (Integer) responseBody.get("expires_in");
            if (accessToken == null) {
                throw new RuntimeException("Token issue failed: access_token is missing");
            }

            long expirationSeconds = expiresIn != null
                    ? expiresIn
                    : properties.getKoreaInvestment().getTokenExpiration();
            tokenExpiryTime = Instant.now().plusSeconds(expirationSeconds);

        } catch (Exception e) {
            throw new RuntimeException("Token issue failed: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createAuthHeaders(String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", properties.getKoreaInvestment().getAppKey());
        headers.set("appsecret", properties.getKoreaInvestment().getAppSecret());
        headers.set("tr_id", trId);
        return headers;
    }

    private String buildUrl(String endpoint, Map<String, String> params) {
        StringBuilder url = new StringBuilder(properties.getKoreaInvestment().getBaseUrl() + endpoint + "?");
        params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));
        if (url.charAt(url.length() - 1) == '&') {
            url.setLength(url.length() - 1);
        }
        return url.toString();
    }

    @SuppressWarnings("unchecked")
    private StockPriceDto parsePriceResponse(String symbol, Map<String, Object> response) {
        String rtCd = (String) response.get("rt_cd");
        if (!"0".equals(rtCd)) {
            throw new RuntimeException("API error: " + response.get("msg1"));
        }

        Map<String, Object> output = (Map<String, Object>) response.get("output");
        if (output == null || output.isEmpty()) {
            throw new RuntimeException("Invalid response data");
        }

        double price = parseDouble((String) output.get("stck_prpr"));
        double open = parseDouble((String) output.get("stck_oprc"));
        double high = parseDouble((String) output.get("stck_hgpr"));
        double low = parseDouble((String) output.get("stck_lwpr"));
        double close = parseDouble((String) output.get("stck_sdpr"));
        double change = parseDouble((String) output.get("prdy_vrss"));
        double changePercent = parseDouble((String) output.get("prdy_ctrt"));

        return StockPriceDto.builder()
                .symbol(symbol)
                .price(price)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .change(change)
                .changePercent(changePercent)
                .lastTradingDay(LocalDateTime.now().toString())
                .timeStamp(LocalDateTime.now())
                .provider(getProviderName())
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<SearchResultDto> parseSearchResponse(Map<String, Object> response) {
        String rtCd = (String) response.get("rt_cd");
        if (!"0".equals(rtCd)) {
            return List.of();
        }

        List<Map<String, String>> output = (List<Map<String, String>>) response.get("output");
        if (output == null || output.isEmpty()) {
            return List.of();
        }

        List<SearchResultDto> results = new ArrayList<>();
        for (Map<String, String> item : output) {
            String symbol = item.get("pdno");
            String name = item.get("prdt_name");
            String type = item.get("prdt_type_cd");

            if (symbol != null && name != null) {
                results.add(new SearchResultDto(symbol, name, "KR", type));
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

    private StockPriceDto fallbackGetMarketData(String symbol, Exception e) {
        throw new RuntimeException("KIS API unavailable: " + e.getMessage(), e);
    }

    private List<SearchResultDto> fallbackSearchSymbol(String query, Exception e) {
        log.warn("KIS search fallback triggered. query={}, error={}", query, e.getMessage());
        return List.of();
    }
}