package com.pjsent.sentinel.backtest.service;

import com.pjsent.sentinel.backtest.dto.HistoricalPriceData;
import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.service.provider.UpbitProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 암호화폐 과거 가격 데이터 조회 서비스
 * Upbit Candles API를 사용하여 일봉 데이터 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoHistoricalDataService {

    private final UpbitProvider upbitProvider;
    private final CacheManager cacheManager;

    /**
     * 특정 암호화폐의 과거 가격 데이터를 조회합니다.
     *
     * Redis 캐시 적용 (7일 TTL):
     * - Cache Key: symbol + "_" + baseCurrency + "_" + startDate + "_" + endDate
     * - sync=true: 동시 요청 중복 방지
     *
     * Circuit Breaker 적용:
     * - upbitApi 인스턴스 사용
     * - Fallback: 캐시된 데이터 반환 시도
     *
     * @param symbol 암호화폐 심볼 (예: BTC, ETH)
     * @param baseCurrency 기준 통화 (예: KRW)
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 과거 가격 데이터 목록
     */
    @CircuitBreaker(name = "upbitApi", fallbackMethod = "getHistoricalPricesFallback")
    @Cacheable(
        value = "cryptoHistoricalData",
        key = "#symbol + '_' + #baseCurrency + '_' + #startDate.toString() + '_' + #endDate.toString()",
        sync = true
    )
    public List<HistoricalPriceData> getHistoricalPrices(
            String symbol, String baseCurrency, LocalDate startDate, LocalDate endDate) {

        log.info("Fetching crypto historical prices for {}-{}: {} to {}",
                baseCurrency, symbol, startDate, endDate);

        // 일 수 계산 (Upbit API는 count 기반)
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // Upbit API 호출 (최대 200일 제한)
        List<CryptoPriceDto> cryptoPrices = upbitProvider.getHistoricalData(symbol, baseCurrency, days);

        if (cryptoPrices == null || cryptoPrices.isEmpty()) {
            log.warn("No historical data returned from Upbit for {}-{}", baseCurrency, symbol);
            return new ArrayList<>();
        }

        // CryptoPriceDto → HistoricalPriceData 변환
        List<HistoricalPriceData> result = cryptoPrices.stream()
                .map(this::convertToHistoricalPriceData)
                .filter(p -> !p.getDate().isBefore(startDate) && !p.getDate().isAfter(endDate))
                .sorted(Comparator.comparing(HistoricalPriceData::getDate))
                .toList();

        log.info("Parsed {} crypto historical price points for {}", result.size(), symbol);
        return result;
    }

    /**
     * CryptoPriceDto를 HistoricalPriceData로 변환
     */
    private HistoricalPriceData convertToHistoricalPriceData(CryptoPriceDto dto) {
        LocalDate date = dto.getTimestamp() != null
                ? dto.getTimestamp().toLocalDate()
                : LocalDate.now();

        return HistoricalPriceData.builder()
                .date(date)
                .open(dto.getOpenPrice())
                .high(dto.getHighPrice())
                .low(dto.getLowPrice())
                .close(dto.getPrice())
                .volume((long) dto.getVolume())
                .adjustedClose(dto.getPrice()) // 암호화폐는 분할이 없으므로 close와 동일
                .build();
    }

    /**
     * Circuit Breaker Fallback 메서드
     */
    @SuppressWarnings("unchecked")
    public List<HistoricalPriceData> getHistoricalPricesFallback(
            String symbol, String baseCurrency, LocalDate startDate, LocalDate endDate, Throwable throwable) {

        log.warn("Circuit Breaker fallback for crypto {}-{} ({} to {}): {}",
                baseCurrency, symbol, startDate, endDate, throwable.getMessage());

        // 캐시에서 데이터 조회 시도
        Cache cache = cacheManager.getCache("cryptoHistoricalData");
        if (cache != null) {
            String cacheKey = symbol + "_" + baseCurrency + "_" + startDate.toString() + "_" + endDate.toString();
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null && cached.get() != null) {
                log.info("Returning cached crypto data for {} from fallback", symbol);
                return (List<HistoricalPriceData>) cached.get();
            }
        }

        log.warn("No cached crypto data available for {}. Returning empty list.", symbol);
        return new ArrayList<>();
    }

    /**
     * Upbit API 사용 가능 여부 확인
     */
    public boolean isApiAvailable() {
        return upbitProvider.isAvailable();
    }
}
