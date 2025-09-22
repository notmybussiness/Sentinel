package com.pjsent.sentinel.market.service.provider;
import java.util.List;
import com.pjsent.sentinel.market.dto.StockPriceDto;

public interface MarketDataProvider {

    // 시장데이터 가격 가져오기
    StockPriceDto getMarketData(String symbol);
    boolean isAvailable();
    // api 제공해주는 곳의 이름
    String getProviderName();

     // Time Series 메서드 (기본값 제공)
    default List<StockPriceDto> getTimeSeriesData(String symbol, String interval) {
        throw new UnsupportedOperationException("Time Series 데이터를 지원하지 않습니다.");
    }
    
    default List<StockPriceDto> getHistoricalData(String symbol, int days) {
        throw new UnsupportedOperationException("Historical 데이터를 지원하지 않습니다.");
    }
      
    // 지원 여부 확인
    default boolean supportsTimeSeries() {
        return false;
    }

    default boolean supportsHistoricalData() {
        return false;
    }

} 
