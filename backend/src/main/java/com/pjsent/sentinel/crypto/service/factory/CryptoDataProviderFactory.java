package com.pjsent.sentinel.crypto.service.factory;

import com.pjsent.sentinel.crypto.service.provider.CryptoDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 암호화폐 데이터 제공자 팩토리
 * Primary Provider (Upbit) → Fallback Provider (Binance) 전략 구현
 */
@Slf4j
@Component
public class CryptoDataProviderFactory {

    private final List<CryptoDataProvider> providers;

    public CryptoDataProviderFactory(List<CryptoDataProvider> providers) {
        this.providers = providers;
        log.info("CryptoDataProviderFactory initialized with {} providers", providers.size());
        providers.forEach(p -> log.info("  - {}", p.getProviderName()));
    }

    /**
     * 사용 가능한 첫 번째 Provider 반환
     * Priority: Upbit → Binance
     * @return 사용 가능한 Provider
     * @throws IllegalStateException 모든 Provider 사용 불가능
     */
    public CryptoDataProvider getAvailableProvider() {
        return providers.stream()
                .filter(CryptoDataProvider::isAvailable)
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No available crypto data provider");
                    return new IllegalStateException("사용 가능한 암호화폐 데이터 제공자가 없습니다.");
                });
    }

    /**
     * 특정 기능을 지원하는 Provider 반환
     * @param featureCheck 기능 확인 함수
     * @return 기능을 지원하는 Provider
     * @throws IllegalStateException 해당 기능을 지원하는 Provider 없음
     */
    public CryptoDataProvider getProviderWithFeature(ProviderFeatureCheck featureCheck) {
        return providers.stream()
                .filter(CryptoDataProvider::isAvailable)
                .filter(featureCheck::supports)
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No provider supports the requested feature");
                    return new IllegalStateException("해당 기능을 지원하는 암호화폐 데이터 제공자가 없습니다.");
                });
    }

    /**
     * 검색 기능을 지원하는 Provider 반환
     */
    public CryptoDataProvider getSearchProvider() {
        return getProviderWithFeature(CryptoDataProvider::supportsSearch);
    }

    /**
     * 트렌딩 기능을 지원하는 Provider 반환
     */
    public CryptoDataProvider getTrendingProvider() {
        return getProviderWithFeature(CryptoDataProvider::supportsTrending);
    }

    /**
     * 과거 데이터 기능을 지원하는 Provider 반환
     */
    public CryptoDataProvider getHistoricalProvider() {
        return getProviderWithFeature(CryptoDataProvider::supportsHistoricalData);
    }

    /**
     * 모든 등록된 Provider 목록 반환
     */
    public List<CryptoDataProvider> getAllProviders() {
        return providers;
    }

    /**
     * Provider 기능 확인 함수형 인터페이스
     */
    @FunctionalInterface
    public interface ProviderFeatureCheck {
        boolean supports(CryptoDataProvider provider);
    }
}
