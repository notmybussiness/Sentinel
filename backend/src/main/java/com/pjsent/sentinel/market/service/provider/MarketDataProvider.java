package com.pjsent.sentinel.market.service.provider;

import java.util.List;

import com.pjsent.sentinel.market.dto.SearchResultDto;
import com.pjsent.sentinel.market.dto.StockPriceDto;

public interface MarketDataProvider {

    StockPriceDto getMarketData(String symbol);

    boolean isAvailable();

    String getProviderName();

    default List<StockPriceDto> getTimeSeriesData(String symbol, String interval) {
        throw new UnsupportedOperationException("Time Series data is not supported.");
    }

    default List<StockPriceDto> getHistoricalData(String symbol, int days) {
        throw new UnsupportedOperationException("Historical data is not supported.");
    }

    default boolean supportsTimeSeries() {
        return false;
    }

    default boolean supportsHistoricalData() {
        return false;
    }

    default List<SearchResultDto> searchSymbol(String query) {
        throw new UnsupportedOperationException("Symbol search is not supported.");
    }

    default boolean supportsSearch() {
        return false;
    }

    default boolean supportsSymbol(String symbol) {
        return true;
    }
}
