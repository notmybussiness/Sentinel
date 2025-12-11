package com.pjsent.sentinel.market.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.pjsent.sentinel.market.dto.MarketIndexDto;
import com.pjsent.sentinel.market.dto.SearchResultDto;
import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.factory.MarketDataProviderFactory;
import com.pjsent.sentinel.market.service.provider.MarketDataProvider;
import com.pjsent.sentinel.market.producer.MarketPriceProducer;
import com.pjsent.sentinel.common.event.PriceUpdateEvent;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding.AssetType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 시장 데이터 서비스
 * 여러 프로바이더를 통해 주식 가격 데이터를 가져오는 서비스입니다.
 * Fallback 전략을 구현하여 주요 프로바이더가 실패할 경우 대체 프로바이더를 사용합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService {

    private final MarketDataProviderFactory providerFactory;
    private final MarketPriceProducer marketPriceProducer;

    /**
     * 주식 가격 데이터를 가져옵니다.
     * Fallback 전략을 사용하여 여러 프로바이더를 순차적으로 시도합니다.
     *
     * 캐싱: Service 레벨 (Experiment 4d - Service Layer Cache 10s TTL)
     *
     * @param symbol 주식 심볼 (예: AAPL, MSFT)
     * @return 주식 가격 데이터
     * @throws RuntimeException 모든 프로바이더가 실패한 경우
     */
    @Cacheable(value = "stockPrice", key = "#symbol", sync = true)
    public StockPriceDto getStockPrice(String symbol) {
        log.info("주식 가격 데이터 요청. 심볼: {}", symbol);

        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("심볼은 필수입니다.");
        }

        List<MarketDataProvider> availableProviders = providerFactory.getAvailableProviders();

        if (availableProviders.isEmpty()) {
            log.error("사용 가능한 프로바이더가 없습니다.");
            throw new RuntimeException("사용 가능한 시장 데이터 프로바이더가 없습니다.");
        }

        Exception lastException = null;

        for (MarketDataProvider provider : availableProviders) {
            try {
                log.debug("프로바이더 {}로 시도 중. 심볼: {}", provider.getProviderName(), symbol);

                StockPriceDto result = provider.getMarketData(symbol);

                if (result != null && result.getPrice() > 0) {
                    log.info("주식 가격 데이터 조회 성공. 심볼: {}, 가격: {}, 프로바이더: {}",
                            symbol, result.getPrice(), provider.getProviderName());

                    // EDA: Publish Price Update Event
                    try {
                        marketPriceProducer.publishPriceUpdate(new PriceUpdateEvent(
                                symbol,
                                java.math.BigDecimal.valueOf(result.getPrice()),
                                AssetType.STOCK,
                                LocalDateTime.now()));
                    } catch (Exception e) {
                        log.error("Failed to publish price update event for symbol: {}", symbol, e);
                        // Don't fail the request if event publishing fails
                    }

                    return result;
                } else {
                    log.warn("프로바이더 {}에서 유효하지 않은 데이터 반환. 심볼: {}",
                            provider.getProviderName(), symbol);
                }

            } catch (Exception e) {
                log.warn("프로바이더 {} 실패. 심볼: {}, 오류: {}",
                        provider.getProviderName(), symbol, e.getMessage());
                lastException = e;
            }
        }

        log.error("모든 프로바이더 실패. 심볼: {}", symbol);
        throw new RuntimeException("모든 시장 데이터 프로바이더가 실패했습니다. 심볼: " + symbol, lastException);
    }

    /**
     * 여러 심볼의 주식 가격 데이터를 가져옵니다.
     * 
     * @param symbols 주식 심볼 목록
     * @return 주식 가격 데이터 목록
     */
    public List<StockPriceDto> getStockPrices(List<String> symbols) {
        log.info("여러 주식 가격 데이터 요청. 심볼 수: {}", symbols.size());

        return symbols.stream()
                .map(this::getStockPrice)
                .collect(Collectors.toList());
    }

    /**
     * 프로바이더 상태를 확인합니다.
     * 
     * @return 프로바이더 상태 정보
     */
    public String getProviderStatus() {
        providerFactory.logProviderStatus();
        return "프로바이더 상태가 로그에 출력되었습니다.";
    }

    /**
     * 사용 가능한 프로바이더가 있는지 확인합니다.
     *
     * @return 사용 가능한 프로바이더가 있으면 true
     */
    public boolean isServiceAvailable() {
        return providerFactory.hasAvailableProvider();
    }

    /**
     * 주요 시장 지수 조회
     * S&P 500, NASDAQ, DOW, KOSPI 4개 지수의 현재 값을 반환합니다.
     *
     * 동작 방식:
     * 1. 4개의 주요 지수 심볼 정의
     * 2. 각 심볼별로 getStockPrice() 재사용하여 가격 조회
     * 3. StockPriceDto → MarketIndexDto 변환
     * 4. 조회 실패한 지수는 null 반환 후 필터링
     *
     * 캐싱: marketIndices 캐시에 5분간 저장
     *
     * @return 시장 지수 목록 (성공한 것만)
     */
    @Cacheable(value = "marketIndices", sync = true)
    public List<MarketIndexDto> getMarketIndices() {
        log.info("시장 지수 조회 요청");

        // 주요 지수 심볼 정의
        // ^GSPC = S&P 500, ^IXIC = NASDAQ, ^DJI = DOW, ^KS11 = KOSPI
        List<String> indexSymbols = List.of("^GSPC", "^IXIC", "^DJI", "^KS11");

        // 각 지수별로 데이터 조회 후 DTO 변환
        // null인 경우 제외 (조회 실패)
        return indexSymbols.stream()
                .map(this::fetchIndexData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 개별 지수 데이터 조회 및 변환
     *
     * 프로세스:
     * 1. getStockPrice()로 해당 심볼의 가격 데이터 조회
     * 2. StockPriceDto를 MarketIndexDto로 변환
     * 3. 지수 이름 매핑 (심볼 → 한글명)
     *
     * @param symbol 지수 심볼 (예: ^GSPC)
     * @return 시장 지수 DTO (실패 시 null)
     */
    private MarketIndexDto fetchIndexData(String symbol) {
        try {
            // 기존 getStockPrice 메서드 재사용
            StockPriceDto price = getStockPrice(symbol);

            // DTO 변환: StockPriceDto → MarketIndexDto
            return MarketIndexDto.builder()
                    .symbol(symbol)
                    .name(getIndexName(symbol)) // 심볼 → 한글명 매핑
                    .value(price.getPrice()) // 현재 지수 값
                    .change(price.getChange()) // 전일 대비 변동
                    .changePercent(price.getChangePercent()) // 변동률(%)
                    .timestamp(LocalDateTime.now()) // 조회 시각
                    .build();
        } catch (Exception e) {
            // 개별 지수 조회 실패는 warning으로 처리 (전체 실패 아님)
            log.warn("지수 조회 실패. 심볼: {}, 오류: {}", symbol, e.getMessage());
            return null;
        }
    }

    /**
     * 심볼을 한글 지수명으로 변환
     *
     * @param symbol 지수 심볼
     * @return 한글 지수명
     */
    private String getIndexName(String symbol) {
        return switch (symbol) {
            case "^GSPC" -> "S&P 500";
            case "^IXIC" -> "NASDAQ";
            case "^DJI" -> "DOW";
            case "^KS11" -> "KOSPI";
            default -> symbol;
        };
    }

    /**
     * 종목 심볼 검색
     * AlphaVantage Symbol Search API를 사용하여 종목을 검색합니다.
     *
     * 동작 방식:
     * 1. Primary provider (AlphaVantage)의 searchSymbol() 호출
     * 2. 검색 결과를 SearchResultDto 리스트로 변환
     * 3. 최대 10개 결과 반환
     *
     * 참고: 현재는 AlphaVantage만 검색 기능 제공
     * Finnhub는 검색 API가 제한적이므로 fallback 미구현
     *
     * 캐싱: stockSearch 캐시에 3분간 저장 (검색 결과는 자주 변하지 않음)
     *
     * @param query 검색 키워드 (심볼 또는 회사명)
     * @return 검색 결과 목록 (최대 10개)
     */
    // @Cacheable(value = "stockSearch", key = "#query")
    public List<SearchResultDto> searchSymbol(String query) {
        log.info("종목 검색 요청. 키워드: {}", query);

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다.");
        }

        // Primary provider로 검색 시도
        List<MarketDataProvider> providers = providerFactory.getAvailableProviders();

        for (MarketDataProvider provider : providers) {
            try {
                log.debug("프로바이더 {}로 검색 중. 키워드: {}", provider.getProviderName(), query);

                List<SearchResultDto> results = provider.searchSymbol(query);

                if (results != null && !results.isEmpty()) {
                    log.info("검색 성공. 키워드: {}, 결과 수: {}, 프로바이더: {}",
                            query, results.size(), provider.getProviderName());
                    return results;
                }

            } catch (Exception e) {
                log.warn("프로바이더 {} 검색 실패. 키워드: {}, 오류: {}",
                        provider.getProviderName(), query, e.getMessage());
            }
        }

        log.warn("모든 프로바이더 검색 실패. 키워드: {}", query);
        return List.of(); // 빈 리스트 반환 (예외 대신)
    }
}