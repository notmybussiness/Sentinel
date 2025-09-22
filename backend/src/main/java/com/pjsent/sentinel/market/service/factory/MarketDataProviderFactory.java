package com.pjsent.sentinel.market.service.factory;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.pjsent.sentinel.market.service.provider.MarketDataProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MarketDataProvider 팩토리 클래스
 * 사용 가능한 프로바이더들을 관리하고 우선순위에 따라 반환합니다.
 * 
 * 우선순위: AlphaVantage -> Finnhub -> Cache
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MarketDataProviderFactory {
    
    private final List<MarketDataProvider> providers;
    
    /**
     * 사용 가능한 모든 프로바이더를 우선순위 순으로 반환합니다.
     * 
     * @return 사용 가능한 프로바이더 목록
     */
    public List<MarketDataProvider> getAvailableProviders() {
        List<MarketDataProvider> availableProviders = providers.stream()
                .filter(MarketDataProvider::isAvailable)
                .collect(Collectors.toList());
        
        log.debug("사용 가능한 프로바이더 수: {}, 전체 프로바이더 수: {}", 
                 availableProviders.size(), providers.size());
        
        return availableProviders;
    }
    
    /**
     * 첫 번째 사용 가능한 프로바이더를 반환합니다.
     * 
     * @return 첫 번째 사용 가능한 프로바이더 또는 null
     */
    public MarketDataProvider getPrimaryProvider() {
        return getAvailableProviders().stream()
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 특정 이름의 프로바이더를 반환합니다.
     * 
     * @param providerName 프로바이더 이름
     * @return 해당 프로바이더 또는 null
     */
    public MarketDataProvider getProvider(String providerName) {
        return providers.stream()
                .filter(provider -> provider.getProviderName().equalsIgnoreCase(providerName))
                .filter(MarketDataProvider::isAvailable)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 모든 프로바이더의 상태를 로깅합니다.
     */
    public void logProviderStatus() {
        log.info("=== Market Data Provider 상태 ===");
        providers.forEach(provider -> {
            String status = provider.isAvailable() ? "사용 가능" : "사용 불가";
            log.info("{}: {}", provider.getProviderName(), status);
        });
        log.info("================================");
    }
    
    /**
     * 사용 가능한 프로바이더가 있는지 확인합니다.
     * 
     * @return 사용 가능한 프로바이더가 있으면 true
     */
    public boolean hasAvailableProvider() {
        return !getAvailableProviders().isEmpty();
    }
}
