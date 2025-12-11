package com.pjsent.sentinel.market.service.provider;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.pjsent.sentinel.market.dto.SearchResultDto;
import com.pjsent.sentinel.market.dto.StockPriceDto;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 한국투자증권 Open API Provider
 *
 * 기능:
 * - OAuth 2.0 토큰 발급 및 자동 갱신
 * - 국내 주식 시세 조회
 * - 종목 검색
 *
 * API 문서: https://apiportal.koreainvestment.com
 *
 * 우선순위: 1순위 (Primary Provider)
 */
@Component
@Slf4j
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(1)
public class KoreaInvestmentProvider implements MarketDataProvider {

    private final RestTemplate restTemplate;

    @Value("${stock.market.korea-investment.enabled:true}")
    private boolean enabled;

    @Value("${stock.market.korea-investment.base-url}")
    private String baseUrl;

    @Value("${stock.market.korea-investment.app-key}")
    private String appKey;

    @Value("${stock.market.korea-investment.app-secret}")
    private String appSecret;

    @Value("${stock.market.korea-investment.timeout:10000}")
    private int timeout;

    @Value("${stock.market.korea-investment.token-expiration:86400}")
    private long tokenExpiration;  // 24시간 (초 단위)

    @Value("${stock.market.korea-investment.token-refresh-before:3600}")
    private long tokenRefreshBefore;  // 만료 1시간 전 갱신

    // 토큰 관리 변수
    private String accessToken;
    private Instant tokenExpiryTime;

    // API 엔드포인트
    private static final String TOKEN_ENDPOINT = "/oauth2/tokenP";
    private static final String PRICE_ENDPOINT = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String SEARCH_ENDPOINT = "/uapi/domestic-stock/v1/quotations/search-info";

    // TR ID (거래 ID)
    private static final String TR_ID_PRICE = "FHKST01010100";  // 주식 현재가 시세
    private static final String TR_ID_SEARCH = "CTPF1604R";     // 종목 검색

    @Override
    // @Cacheable(value = "stockPrice", key = "#symbol", sync = true)  // Experiment 4d: Service Layer로 이동
    @CircuitBreaker(name = "kisApi", fallbackMethod = "fallbackGetMarketData")
    @RateLimiter(name = "kisApi")
    @Retry(name = "marketDataApi")
    public StockPriceDto getMarketData(String symbol) {
        if (!isAvailable()) {
            throw new IllegalStateException("한국투자증권 API가 사용 불가능합니다.");
        }

        log.info("한국투자증권에서 {} 종목의 시장 데이터를 가져오는 중", symbol);

        try {
            // 토큰 확인 및 갱신
            ensureValidToken();

            // API 호출
            String url = buildUrl(PRICE_ENDPOINT, Map.of(
                "fid_cond_mrkt_div_code", "J",  // J: 주식, ETF, ETN
                "fid_input_iscd", symbol
            ));

            HttpHeaders headers = createAuthHeaders(TR_ID_PRICE);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.debug("한국투자증권 API 호출 URL: {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parsePriceResponse(symbol, response.getBody());
            } else {
                log.warn("한국투자증권 API 응답이 비정상입니다. 상태코드: {}", response.getStatusCode());
                throw new RuntimeException("한국투자증권 API 응답 오류");
            }

        } catch (Exception e) {
            log.error("한국투자증권 API 호출 중 오류 발생. 종목: {}, 오류: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("한국투자증권 API 호출 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled &&
               appKey != null && !appKey.trim().isEmpty() &&
               appSecret != null && !appSecret.trim().isEmpty();
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
    @Cacheable(value = "stockSearch", key = "#query", sync = true)
    @CircuitBreaker(name = "kisApi", fallbackMethod = "fallbackSearchSymbol")
    @RateLimiter(name = "kisApi")
    public List<SearchResultDto> searchSymbol(String query) {
        if (!isAvailable()) {
            throw new IllegalStateException("한국투자증권 API가 사용 불가능합니다.");
        }

        log.info("한국투자증권에서 종목 검색 중. 키워드: {}", query);

        try {
            // 토큰 확인 및 갱신
            ensureValidToken();

            // API 호출
            String url = buildUrl(SEARCH_ENDPOINT, Map.of(
                "pdno", query,  // 종목코드 또는 종목명
                "prdt_type_cd", "300"  // 주식
            ));

            HttpHeaders headers = createAuthHeaders(TR_ID_SEARCH);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.debug("한국투자증권 검색 API 호출 URL: {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseSearchResponse(response.getBody());
            } else {
                log.warn("한국투자증권 검색 API 응답이 비정상입니다. 상태코드: {}", response.getStatusCode());
                return List.of();
            }

        } catch (Exception e) {
            log.error("한국투자증권 검색 API 호출 중 오류 발생. 키워드: {}, 오류: {}", query, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 유효한 토큰 확인 및 갱신
     */
    private void ensureValidToken() {
        if (accessToken == null || tokenExpiryTime == null || isTokenExpiringSoon()) {
            log.info("토큰 갱신 필요. 새 토큰 발급 중...");
            refreshAccessToken();
        }
    }

    /**
     * 토큰이 곧 만료되는지 확인
     */
    private boolean isTokenExpiringSoon() {
        if (tokenExpiryTime == null) {
            return true;
        }
        long secondsUntilExpiry = tokenExpiryTime.getEpochSecond() - Instant.now().getEpochSecond();
        return secondsUntilExpiry < tokenRefreshBefore;
    }

    /**
     * OAuth 토큰 발급/갱신
     */
    private void refreshAccessToken() {
        try {
            String url = baseUrl + TOKEN_ENDPOINT;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
            );

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            log.debug("한국투자증권 토큰 발급 요청 중...");

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                accessToken = (String) responseBody.get("access_token");
                Integer expiresIn = (Integer) responseBody.get("expires_in");

                if (accessToken == null) {
                    throw new RuntimeException("토큰 발급 실패: access_token이 없습니다.");
                }

                // 토큰 만료 시간 계산 (기본값: 86400초 = 24시간)
                long expirationSeconds = expiresIn != null ? expiresIn : tokenExpiration;
                tokenExpiryTime = Instant.now().plusSeconds(expirationSeconds);

                log.info("한국투자증권 토큰 발급 성공. 만료 시간: {}", tokenExpiryTime);
            } else {
                throw new RuntimeException("토큰 발급 실패. 상태코드: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("토큰 발급 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("토큰 발급 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 인증 헤더 생성
     */
    private HttpHeaders createAuthHeaders(String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        return headers;
    }

    /**
     * URL 생성 (쿼리 파라미터 포함)
     */
    private String buildUrl(String endpoint, Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl + endpoint + "?");

        params.forEach((key, value) -> {
            url.append(key).append("=").append(value).append("&");
        });

        // 마지막 & 제거
        if (url.charAt(url.length() - 1) == '&') {
            url.setLength(url.length() - 1);
        }

        return url.toString();
    }

    /**
     * 가격 조회 응답 파싱
     */
    @SuppressWarnings("unchecked")
    private StockPriceDto parsePriceResponse(String symbol, Map<String, Object> response) {
        try {
            // 한국투자증권 API 응답 구조:
            // {
            //   "rt_cd": "0",  // 성공 여부
            //   "msg_cd": "...",
            //   "msg1": "...",
            //   "output": { 실제 데이터 }
            // }

            String rtCd = (String) response.get("rt_cd");
            if (!"0".equals(rtCd)) {
                String msg1 = (String) response.get("msg1");
                log.warn("한국투자증권 API 오류. 종목: {}, 메시지: {}", symbol, msg1);
                throw new RuntimeException("API 오류: " + msg1);
            }

            Map<String, Object> output = (Map<String, Object>) response.get("output");
            if (output == null || output.isEmpty()) {
                log.warn("한국투자증권 응답에서 output 데이터를 찾을 수 없습니다. 종목: {}", symbol);
                throw new RuntimeException("유효하지 않은 응답 데이터");
            }

            // 응답 필드 파싱
            String priceStr = (String) output.get("stck_prpr");  // 주식 현재가
            String openStr = (String) output.get("stck_oprc");   // 시가
            String highStr = (String) output.get("stck_hgpr");   // 고가
            String lowStr = (String) output.get("stck_lwpr");    // 저가
            String closeStr = (String) output.get("stck_sdpr");  // 전일 종가
            String changeStr = (String) output.get("prdy_vrss");     // 전일 대비
            String changeRateStr = (String) output.get("prdy_ctrt"); // 전일 대비율

            double price = parseDouble(priceStr);
            double open = parseDouble(openStr);
            double high = parseDouble(highStr);
            double low = parseDouble(lowStr);
            double close = parseDouble(closeStr);
            double change = parseDouble(changeStr);
            double changePercent = parseDouble(changeRateStr);

            log.debug("한국투자증권 데이터 파싱 완료. 종목: {}, 가격: {}", symbol, price);

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

        } catch (Exception e) {
            log.error("한국투자증권 응답 파싱 중 오류 발생. 종목: {}, 응답: {}, 오류: {}",
                     symbol, response, e.getMessage(), e);
            throw new RuntimeException("응답 데이터 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 검색 응답 파싱
     */
    @SuppressWarnings("unchecked")
    private List<SearchResultDto> parseSearchResponse(Map<String, Object> response) {
        try {
            String rtCd = (String) response.get("rt_cd");
            if (!"0".equals(rtCd)) {
                String msg1 = (String) response.get("msg1");
                log.warn("한국투자증권 검색 API 오류. 메시지: {}", msg1);
                return List.of();
            }

            List<Map<String, String>> output = (List<Map<String, String>>) response.get("output");

            if (output == null || output.isEmpty()) {
                log.warn("한국투자증권 검색 결과가 없습니다.");
                return List.of();
            }

            List<SearchResultDto> results = new ArrayList<>();

            for (Map<String, String> item : output) {
                String symbol = item.get("pdno");        // 종목코드
                String name = item.get("prdt_name");     // 종목명
                String type = item.get("prdt_type_cd");  // 상품 유형

                if (symbol != null && name != null) {
                    results.add(new SearchResultDto(symbol, name, "KR", type));
                }

                // 최대 10개 결과만 반환
                if (results.size() >= 10) {
                    break;
                }
            }

            log.info("한국투자증권 검색 완료. 결과 수: {}", results.size());
            return results;

        } catch (Exception e) {
            log.error("한국투자증권 검색 응답 파싱 중 오류 발생. 응답: {}, 오류: {}",
                     response, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 문자열을 double로 변환
     */
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

    /**
     * Circuit Breaker Fallback: getMarketData
     * Circuit Breaker가 OPEN 상태일 때 호출됨
     * MarketDataService의 Fallback 루프가 다음 Provider를 시도하도록 예외를 던진다
     */
    private StockPriceDto fallbackGetMarketData(String symbol, Exception e) {
        log.warn("KIS API Circuit Breaker OPEN 또는 Rate Limit 도달. Fallback Provider로 전환됩니다. 심볼: {}, 오류: {}",
                symbol, e.getMessage());
        throw new RuntimeException("KIS API 일시적 사용 불가: " + e.getMessage(), e);
    }

    /**
     * Circuit Breaker Fallback: searchSymbol
     * Circuit Breaker가 OPEN 상태일 때 호출됨
     */
    private List<SearchResultDto> fallbackSearchSymbol(String query, Exception e) {
        log.warn("KIS API Circuit Breaker OPEN 또는 Rate Limit 도달. 검색 실패. 키워드: {}, 오류: {}",
                query, e.getMessage());
        throw new RuntimeException("KIS API 일시적 사용 불가: " + e.getMessage(), e);
    }
}
