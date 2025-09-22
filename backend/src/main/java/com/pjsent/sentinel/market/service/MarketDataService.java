package com.pjsent.sentinel.market.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.factory.MarketDataProviderFactory;
import com.pjsent.sentinel.market.service.provider.MarketDataProvider;

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
    
    /**
     * 주식 가격 데이터를 가져옵니다.
     * Fallback 전략을 사용하여 여러 프로바이더를 순차적으로 시도합니다.
     * 
     * @param symbol 주식 심볼 (예: AAPL, MSFT)
     * @return 주식 가격 데이터
     * @throws RuntimeException 모든 프로바이더가 실패한 경우
     */
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
                .collect(java.util.stream.Collectors.toList());
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
}