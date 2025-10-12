package com.pjsent.sentinel.crypto.service;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.dto.CryptoSearchResultDto;
import com.pjsent.sentinel.crypto.dto.TrendingCoinDto;
import com.pjsent.sentinel.crypto.service.factory.CryptoDataProviderFactory;
import com.pjsent.sentinel.crypto.service.provider.CryptoDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 암호화폐 데이터 서비스
 * 여러 거래소 Provider를 통해 암호화폐 가격 데이터를 가져오는 서비스
 * Fallback 전략: Upbit (Primary) → Binance (Fallback)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CryptoDataService {

    private final CryptoDataProviderFactory providerFactory;

    /**
     * 단일 암호화폐 가격 조회
     * @param symbol 암호화폐 심볼 (예: BTC, ETH)
     * @param baseCurrency 기준 통화 (예: KRW, USDT)
     * @return 가격 정보
     */
    public CryptoPriceDto getCryptoPrice(String symbol, String baseCurrency) {
        log.info("암호화폐 가격 조회 요청. 심볼: {}, 기준 통화: {}", symbol, baseCurrency);

        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("심볼은 필수입니다.");
        }

        if (baseCurrency == null || baseCurrency.trim().isEmpty()) {
            throw new IllegalArgumentException("기준 통화는 필수입니다.");
        }

        Exception lastException = null;

        // Fallback 전략: 사용 가능한 모든 Provider 순차 시도
        for (CryptoDataProvider provider : providerFactory.getAllProviders()) {
            if (!provider.isAvailable()) {
                log.debug("Provider {} 사용 불가능. 스킵", provider.getProviderName());
                continue;
            }

            try {
                log.debug("Provider {}로 시도 중. 심볼: {}, 기준 통화: {}",
                        provider.getProviderName(), symbol, baseCurrency);

                CryptoPriceDto result = provider.getCryptoPrice(symbol, baseCurrency);

                if (result != null && result.getPrice() > 0) {
                    log.info("암호화폐 가격 조회 성공. 심볼: {}, 가격: {}, Provider: {}",
                            symbol, result.getPrice(), provider.getProviderName());
                    return result;
                } else {
                    log.warn("Provider {}에서 유효하지 않은 데이터 반환. 심볼: {}",
                            provider.getProviderName(), symbol);
                }

            } catch (Exception e) {
                log.warn("Provider {} 실패. 심볼: {}, 오류: {}",
                        provider.getProviderName(), symbol, e.getMessage());
                lastException = e;
            }
        }

        log.error("모든 Provider 실패. 심볼: {}, 기준 통화: {}", symbol, baseCurrency);
        throw new RuntimeException("암호화폐 가격 조회 실패: " + symbol, lastException);
    }

    /**
     * 배치 가격 조회
     * @param symbols 암호화폐 심볼 목록
     * @param baseCurrency 기준 통화
     * @return 가격 정보 목록
     */
    public List<CryptoPriceDto> getBatchCryptoPrices(List<String> symbols, String baseCurrency) {
        log.info("배치 가격 조회 요청. 개수: {}, 기준 통화: {}", symbols.size(), baseCurrency);

        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("심볼 목록은 필수입니다.");
        }

        CryptoDataProvider provider = providerFactory.getAvailableProvider();

        try {
            // Provider가 배치 API를 지원하면 사용
            List<CryptoPriceDto> results = provider.getBatchCryptoPrices(symbols, baseCurrency);

            if (results != null && !results.isEmpty()) {
                log.info("배치 가격 조회 성공. 조회 개수: {}/{}, Provider: {}",
                        results.size(), symbols.size(), provider.getProviderName());
                return results;
            }

        } catch (Exception e) {
            log.warn("배치 가격 조회 실패. Provider: {}, 오류: {}. 개별 조회로 전환",
                    provider.getProviderName(), e.getMessage());
        }

        // Fallback: 개별 조회
        return symbols.stream()
                .map(symbol -> {
                    try {
                        return getCryptoPrice(symbol, baseCurrency);
                    } catch (Exception e) {
                        log.warn("개별 가격 조회 실패. 심볼: {}", symbol);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 암호화폐 검색
     * @param query 검색어 (심볼 또는 이름)
     * @return 검색 결과 목록
     */
    public List<CryptoSearchResultDto> searchCrypto(String query) {
        log.info("암호화폐 검색 요청. 키워드: {}", query);

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어는 필수입니다.");
        }

        try {
            CryptoDataProvider provider = providerFactory.getSearchProvider();
            List<CryptoSearchResultDto> results = provider.searchCrypto(query);

            log.info("검색 성공. 키워드: {}, 결과 수: {}, Provider: {}",
                    query, results.size(), provider.getProviderName());

            return results;

        } catch (Exception e) {
            log.error("검색 실패. 키워드: {}, 오류: {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * 트렌딩 암호화폐 조회 (거래대금 상위)
     * @param baseCurrency 기준 통화
     * @param limit 조회 개수
     * @return 트렌딩 암호화폐 목록
     */
    public List<TrendingCoinDto> getTrendingCoins(String baseCurrency, int limit) {
        log.info("트렌딩 암호화폐 조회 요청. 기준 통화: {}, 개수: {}", baseCurrency, limit);

        if (baseCurrency == null || baseCurrency.trim().isEmpty()) {
            baseCurrency = "KRW"; // 기본값
        }

        if (limit <= 0 || limit > 50) {
            limit = 10; // 기본값
        }

        try {
            CryptoDataProvider provider = providerFactory.getTrendingProvider();
            List<TrendingCoinDto> results = provider.getTrendingCoins(baseCurrency, limit);

            log.info("트렌딩 암호화폐 조회 성공. 결과 수: {}, Provider: {}",
                    results.size(), provider.getProviderName());

            return results;

        } catch (Exception e) {
            log.error("트렌딩 암호화폐 조회 실패. 기준 통화: {}, 오류: {}", baseCurrency, e.getMessage());
            return List.of();
        }
    }

    /**
     * 과거 데이터 조회 (캔들 스틱)
     * @param symbol 암호화폐 심볼
     * @param baseCurrency 기준 통화
     * @param days 조회 일수
     * @return 과거 가격 데이터 목록
     */
    public List<CryptoPriceDto> getHistoricalData(String symbol, String baseCurrency, int days) {
        log.info("과거 데이터 조회 요청. 심볼: {}, 기준 통화: {}, 일수: {}", symbol, baseCurrency, days);

        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("심볼은 필수입니다.");
        }

        if (baseCurrency == null || baseCurrency.trim().isEmpty()) {
            throw new IllegalArgumentException("기준 통화는 필수입니다.");
        }

        if (days <= 0 || days > 365) {
            throw new IllegalArgumentException("조회 일수는 1~365일 사이여야 합니다.");
        }

        try {
            CryptoDataProvider provider = providerFactory.getHistoricalProvider();
            List<CryptoPriceDto> results = provider.getHistoricalData(symbol, baseCurrency, days);

            log.info("과거 데이터 조회 성공. 심볼: {}, 결과 수: {}, Provider: {}",
                    symbol, results.size(), provider.getProviderName());

            return results;

        } catch (Exception e) {
            log.error("과거 데이터 조회 실패. 심볼: {}, 오류: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    /**
     * 서비스 상태 확인
     * @return Provider 상태 정보
     */
    public String getServiceStatus() {
        List<CryptoDataProvider> providers = providerFactory.getAllProviders();

        StringBuilder status = new StringBuilder("Crypto Data Service Status:\n");

        for (CryptoDataProvider provider : providers) {
            status.append(String.format("  - %s: %s%n",
                    provider.getProviderName(),
                    provider.isAvailable() ? "Available" : "Unavailable"));
        }

        String result = status.toString();
        log.info(result);

        return result;
    }

    /**
     * 서비스 사용 가능 여부
     * @return 사용 가능한 Provider가 있으면 true
     */
    public boolean isServiceAvailable() {
        return providerFactory.getAllProviders().stream()
                .anyMatch(CryptoDataProvider::isAvailable);
    }
}
