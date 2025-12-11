package com.pjsent.sentinel.market.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 종목 검색 결과 DTO
 * AlphaVantage Symbol Search API 응답을 표준화
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {

    /**
     * 종목 심볼 (예: AAPL, GOOGL)
     */
    private String symbol;

    /**
     * 종목 이름 (예: Apple Inc.)
     */
    private String name;

    /**
     * 거래소/지역 (예: United States, NASDAQ)
     */
    private String exchange;

    /**
     * 자산 유형 (예: Equity, ETF)
     */
    private String type;
}
