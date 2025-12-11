package com.pjsent.sentinel.crypto.service.provider;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.dto.CryptoSearchResultDto;
import com.pjsent.sentinel.crypto.dto.TrendingCoinDto;

import java.util.List;

/**
 * 암호화폐 데이터 제공자 인터페이스
 * 다양한 거래소 (Upbit, Binance 등)의 데이터를 표준화
 */
public interface CryptoDataProvider {

    /**
     * 단일 암호화폐 가격 조회
     * @param symbol 암호화폐 심볼 (예: BTC, ETH)
     * @param baseCurrency 기준 통화 (예: KRW, USDT)
     * @return 가격 정보
     */
    CryptoPriceDto getCryptoPrice(String symbol, String baseCurrency);

    /**
     * 배치 가격 조회
     * @param symbols 암호화폐 심볼 목록
     * @param baseCurrency 기준 통화
     * @return 가격 정보 목록
     */
    List<CryptoPriceDto> getBatchCryptoPrices(List<String> symbols, String baseCurrency);

    /**
     * 암호화폐 검색
     * @param query 검색어 (심볼 또는 이름)
     * @return 검색 결과 목록
     */
    default List<CryptoSearchResultDto> searchCrypto(String query) {
        throw new UnsupportedOperationException("암호화폐 검색을 지원하지 않습니다.");
    }

    /**
     * 트렌딩 암호화폐 조회 (거래대금 상위)
     * @param baseCurrency 기준 통화
     * @param limit 조회 개수
     * @return 트렌딩 암호화폐 목록
     */
    default List<TrendingCoinDto> getTrendingCoins(String baseCurrency, int limit) {
        throw new UnsupportedOperationException("트렌딩 암호화폐 조회를 지원하지 않습니다.");
    }

    /**
     * 과거 데이터 조회 (캔들 스틱)
     * @param symbol 암호화폐 심볼
     * @param baseCurrency 기준 통화
     * @param days 조회 일수
     * @return 과거 가격 데이터 목록
     */
    default List<CryptoPriceDto> getHistoricalData(String symbol, String baseCurrency, int days) {
        throw new UnsupportedOperationException("과거 데이터 조회를 지원하지 않습니다.");
    }

    /**
     * 제공자 가용성 확인
     * @return 사용 가능 여부
     */
    boolean isAvailable();

    /**
     * 제공자 이름
     * @return 제공자 이름 (예: Upbit, Binance)
     */
    String getProviderName();

    /**
     * 검색 기능 지원 여부
     * @return 검색 지원 여부
     */
    default boolean supportsSearch() {
        return false;
    }

    /**
     * 트렌딩 기능 지원 여부
     * @return 트렌딩 지원 여부
     */
    default boolean supportsTrending() {
        return false;
    }

    /**
     * 과거 데이터 지원 여부
     * @return 과거 데이터 지원 여부
     */
    default boolean supportsHistoricalData() {
        return false;
    }
}
