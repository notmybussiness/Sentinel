package com.pjsent.sentinel.crypto.service.provider;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.dto.CryptoSearchResultDto;
import com.pjsent.sentinel.crypto.dto.TrendingCoinDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Upbit 거래소 API Provider
 * - Public API 사용 (API Key 불필요)
 * - 한국 최대 거래소
 * - 실시간 WebSocket 지원
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UpbitProvider implements CryptoDataProvider {

    private final RestTemplate restTemplate;

    @Value("${crypto.market.upbit.base-url:https://api.upbit.com/v1}")
    private String baseUrl;

    @Value("${crypto.market.upbit.enabled:true}")
    private boolean enabled;

    @Value("${crypto.market.upbit.timeout:10000}")
    private int timeout;

    @Override
    @Cacheable(value = "cryptoPrice", key = "#symbol + '_' + #baseCurrency", sync = true)
    public CryptoPriceDto getCryptoPrice(String symbol, String baseCurrency) {
        if (!isAvailable()) {
            throw new IllegalStateException("Upbit API가 사용 불가능합니다.");
        }

        log.info("Upbit에서 {} 암호화폐 가격 조회 중. 기준 통화: {}", symbol, baseCurrency);

        try {
            String marketCode = buildMarketCode(baseCurrency, symbol);
            String url = String.format("%s/ticker?markets=%s", baseUrl, marketCode);
            log.debug("Upbit API 호출 URL: {}", url);

            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get(0);
                return parseTickerData(symbol, baseCurrency, marketCode, data);
            } else {
                log.warn("Upbit API 응답이 비정상입니다. 상태코드: {}", response.getStatusCode());
                throw new RuntimeException("Upbit API 응답 오류");
            }

        } catch (Exception e) {
            log.error("Upbit API 호출 중 오류 발생. 심볼: {}, 오류: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Upbit API 호출 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CryptoPriceDto> getBatchCryptoPrices(List<String> symbols, String baseCurrency) {
        if (!isAvailable()) {
            throw new IllegalStateException("Upbit API가 사용 불가능합니다.");
        }

        log.info("Upbit에서 배치 가격 조회 중. 개수: {}, 기준 통화: {}", symbols.size(), baseCurrency);

        try {
            String markets = symbols.stream()
                    .map(symbol -> buildMarketCode(baseCurrency, symbol))
                    .collect(Collectors.joining(","));

            String url = String.format("%s/ticker?markets=%s", baseUrl, markets);
            log.debug("Upbit API 배치 호출 URL: {}", url);

            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.getBody();

                return dataList.stream()
                        .map(data -> {
                            String marketCode = (String) data.get("market");
                            String symbol = extractSymbol(marketCode);
                            return parseTickerData(symbol, baseCurrency, marketCode, data);
                        })
                        .collect(Collectors.toList());
            } else {
                log.warn("Upbit API 배치 응답이 비정상입니다. 상태코드: {}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("Upbit API 배치 호출 중 오류 발생. 오류: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Cacheable(value = "cryptoSearch", key = "#query", sync = true)
    public List<CryptoSearchResultDto> searchCrypto(String query) {
        if (!isAvailable()) {
            throw new IllegalStateException("Upbit API가 사용 불가능합니다.");
        }

        log.info("Upbit에서 암호화폐 검색 중. 키워드: {}", query);

        try {
            // 전체 마켓 리스트 조회
            String url = String.format("%s/market/all", baseUrl);
            log.debug("Upbit Market All API 호출 URL: {}", url);

            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> markets = (List<Map<String, Object>>) response.getBody();

                String lowerQuery = query.toLowerCase();

                return markets.stream()
                        .filter(market -> {
                            String marketCode = (String) market.get("market");
                            String koreanName = (String) market.get("korean_name");
                            String englishName = (String) market.get("english_name");

                            return marketCode != null && (
                                    marketCode.toLowerCase().contains(lowerQuery) ||
                                    (koreanName != null && koreanName.toLowerCase().contains(lowerQuery)) ||
                                    (englishName != null && englishName.toLowerCase().contains(lowerQuery))
                            );
                        })
                        .limit(20)
                        .map(this::parseMarketData)
                        .collect(Collectors.toList());
            } else {
                log.warn("Upbit Market All API 응답이 비정상입니다. 상태코드: {}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("Upbit 검색 API 호출 중 오류 발생. 키워드: {}, 오류: {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Cacheable(value = "trendingCoins", key = "#baseCurrency + '_' + #limit", sync = true)
    public List<TrendingCoinDto> getTrendingCoins(String baseCurrency, int limit) {
        if (!isAvailable()) {
            throw new IllegalStateException("Upbit API가 사용 불가능합니다.");
        }

        log.info("Upbit에서 트렌딩 코인 조회 중. 기준 통화: {}, 개수: {}", baseCurrency, limit);

        try {
            // 1. 마켓 리스트 조회 (특정 기준 통화만)
            String marketUrl = String.format("%s/market/all", baseUrl);
            ResponseEntity<List> marketResponse = restTemplate.getForEntity(marketUrl, List.class);

            if (marketResponse.getStatusCode() != HttpStatus.OK || marketResponse.getBody() == null) {
                log.warn("Upbit Market All API 응답이 비정상입니다.");
                return Collections.emptyList();
            }

            List<Map<String, Object>> markets = (List<Map<String, Object>>) marketResponse.getBody();
            List<String> targetMarkets = markets.stream()
                    .map(m -> (String) m.get("market"))
                    .filter(market -> market.startsWith(baseCurrency + "-"))
                    .collect(Collectors.toList());

            // 2. 모든 마켓의 Ticker 정보 조회
            String marketsString = String.join(",", targetMarkets);
            String tickerUrl = String.format("%s/ticker?markets=%s", baseUrl, marketsString);
            ResponseEntity<List> tickerResponse = restTemplate.getForEntity(tickerUrl, List.class);

            if (tickerResponse.getStatusCode() != HttpStatus.OK || tickerResponse.getBody() == null) {
                log.warn("Upbit Ticker API 응답이 비정상입니다.");
                return Collections.emptyList();
            }

            List<Map<String, Object>> tickers = (List<Map<String, Object>>) tickerResponse.getBody();

            // 3. 거래대금 기준 정렬
            List<TrendingCoinDto> trending = tickers.stream()
                    .map(ticker -> {
                        String marketCode = (String) ticker.get("market");
                        String symbol = extractSymbol(marketCode);

                        // 마켓 정보에서 한글/영문명 찾기
                        Optional<Map<String, Object>> marketInfo = markets.stream()
                                .filter(m -> marketCode.equals(m.get("market")))
                                .findFirst();

                        String koreanName = marketInfo.map(m -> (String) m.get("korean_name")).orElse("");
                        String englishName = marketInfo.map(m -> (String) m.get("english_name")).orElse(symbol);

                        double tradePrice = parseDouble(ticker.get("trade_price"));
                        double changeRate = parseDouble(ticker.get("signed_change_rate")) * 100; // %로 변환
                        double accTradeVolume = parseDouble(ticker.get("acc_trade_volume_24h"));
                        double accTradePrice = parseDouble(ticker.get("acc_trade_price_24h"));

                        return TrendingCoinDto.builder()
                                .symbol(symbol)
                                .name(englishName)
                                .koreanName(koreanName)
                                .marketCode(marketCode)
                                .price(tradePrice)
                                .changePercent(changeRate)
                                .volume(accTradeVolume)
                                .tradeValue(accTradePrice)
                                .baseCurrency(baseCurrency)
                                .build();
                    })
                    .sorted(Comparator.comparingDouble(TrendingCoinDto::getTradeValue).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            // 4. 순위 설정
            for (int i = 0; i < trending.size(); i++) {
                trending.get(i).setRank(i + 1);
            }

            log.info("Upbit 트렌딩 코인 조회 완료. 결과 수: {}", trending.size());
            return trending;

        } catch (Exception e) {
            log.error("Upbit 트렌딩 코인 조회 중 오류 발생. 오류: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<CryptoPriceDto> getHistoricalData(String symbol, String baseCurrency, int days) {
        if (!isAvailable()) {
            throw new IllegalStateException("Upbit API가 사용 불가능합니다.");
        }

        log.info("Upbit에서 {} 과거 데이터 조회 중. 기간: {}일, 기준 통화: {}", symbol, days, baseCurrency);

        try {
            String marketCode = buildMarketCode(baseCurrency, symbol);
            String url = String.format("%s/candles/days?market=%s&count=%d", baseUrl, marketCode, days);
            log.debug("Upbit Candles API 호출 URL: {}", url);

            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> candles = (List<Map<String, Object>>) response.getBody();

                return candles.stream()
                        .map(candle -> parseCandleData(symbol, baseCurrency, marketCode, candle))
                        .collect(Collectors.toList());
            } else {
                log.warn("Upbit Candles API 응답이 비정상입니다. 상태코드: {}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("Upbit 과거 데이터 조회 중 오류 발생. 심볼: {}, 오류: {}", symbol, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "Upbit";
    }

    @Override
    public boolean supportsSearch() {
        return true;
    }

    @Override
    public boolean supportsTrending() {
        return true;
    }

    @Override
    public boolean supportsHistoricalData() {
        return true;
    }

    // ==================== Helper Methods ====================

    /**
     * 마켓 코드 생성 (예: KRW-BTC)
     */
    private String buildMarketCode(String baseCurrency, String symbol) {
        return String.format("%s-%s", baseCurrency.toUpperCase(), symbol.toUpperCase());
    }

    /**
     * 마켓 코드에서 심볼 추출 (예: KRW-BTC → BTC)
     */
    private String extractSymbol(String marketCode) {
        String[] parts = marketCode.split("-");
        return parts.length > 1 ? parts[1] : marketCode;
    }

    /**
     * Ticker 데이터 파싱
     */
    private CryptoPriceDto parseTickerData(String symbol, String baseCurrency, String marketCode, Map<String, Object> data) {
        double tradePrice = parseDouble(data.get("trade_price"));
        double openingPrice = parseDouble(data.get("opening_price"));
        double highPrice = parseDouble(data.get("high_price"));
        double lowPrice = parseDouble(data.get("low_price"));
        double change = parseDouble(data.get("signed_change_price"));
        double changeRate = parseDouble(data.get("signed_change_rate")) * 100; // %로 변환
        double accTradeVolume = parseDouble(data.get("acc_trade_volume_24h"));
        double accTradePrice = parseDouble(data.get("acc_trade_price_24h"));

        return CryptoPriceDto.builder()
                .symbol(symbol)
                .baseCurrency(baseCurrency)
                .marketCode(marketCode)
                .price(tradePrice)
                .openPrice(openingPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .volume(accTradeVolume)
                .change(change)
                .changePercent(changeRate)
                .tradeValue(accTradePrice)
                .timestamp(LocalDateTime.now())
                .provider(getProviderName())
                .build();
    }

    /**
     * Candle 데이터 파싱
     */
    private CryptoPriceDto parseCandleData(String symbol, String baseCurrency, String marketCode, Map<String, Object> candle) {
        double openingPrice = parseDouble(candle.get("opening_price"));
        double highPrice = parseDouble(candle.get("high_price"));
        double lowPrice = parseDouble(candle.get("low_price"));
        double tradePrice = parseDouble(candle.get("trade_price"));
        double candleAccTradeVolume = parseDouble(candle.get("candle_acc_trade_volume"));
        double candleAccTradePrice = parseDouble(candle.get("candle_acc_trade_price"));

        // 날짜 파싱
        String candleDateTimeUtc = (String) candle.get("candle_date_time_utc");
        LocalDateTime timestamp = candleDateTimeUtc != null
                ? ZonedDateTime.parse(candleDateTimeUtc).toLocalDateTime()
                : LocalDateTime.now();

        return CryptoPriceDto.builder()
                .symbol(symbol)
                .baseCurrency(baseCurrency)
                .marketCode(marketCode)
                .price(tradePrice)
                .openPrice(openingPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .volume(candleAccTradeVolume)
                .tradeValue(candleAccTradePrice)
                .change(0.0)
                .changePercent(0.0)
                .timestamp(timestamp)
                .provider(getProviderName())
                .build();
    }

    /**
     * Market 데이터 파싱
     */
    private CryptoSearchResultDto parseMarketData(Map<String, Object> market) {
        String marketCode = (String) market.get("market");
        String koreanName = (String) market.get("korean_name");
        String englishName = (String) market.get("english_name");

        String[] parts = marketCode.split("-");
        String baseCurrency = parts[0];
        String symbol = parts.length > 1 ? parts[1] : marketCode;

        return CryptoSearchResultDto.builder()
                .symbol(symbol)
                .name(englishName)
                .koreanName(koreanName)
                .marketCode(marketCode)
                .baseCurrency(baseCurrency)
                .tradeable(true)
                .exchange("Upbit")
                .build();
    }

    /**
     * Object를 double로 변환
     */
    private double parseDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("숫자 변환 실패: {}", value);
            return 0.0;
        }
    }
}
