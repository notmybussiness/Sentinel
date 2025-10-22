package com.pjsent.sentinel.pricehistory.service;

import com.pjsent.sentinel.pricehistory.dto.ChartDataDto;
import com.pjsent.sentinel.pricehistory.dto.PriceHistoryDto;
import com.pjsent.sentinel.pricehistory.entity.PriceHistory;
import com.pjsent.sentinel.pricehistory.entity.PriceHistory.AssetType;
import com.pjsent.sentinel.pricehistory.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 가격 데이터 저장 (중복 체크)
     */
    @Transactional
    public PriceHistory savePriceData(PriceHistory priceHistory) {
        // 중복 체크
        boolean exists = priceHistoryRepository.existsBySymbolAndTimestamp(
            priceHistory.getSymbol(),
            priceHistory.getTimestamp()
        );

        if (exists) {
            log.debug("Price data already exists: {} at {}",
                priceHistory.getSymbol(), priceHistory.getTimestamp());
            return null;
        }

        return priceHistoryRepository.save(priceHistory);
    }

    /**
     * 배치로 가격 데이터 저장
     */
    @Transactional
    public List<PriceHistory> savePriceDataBatch(List<PriceHistory> priceHistories) {
        // 중복 제거 후 저장
        List<PriceHistory> uniqueData = priceHistories.stream()
            .filter(ph -> !priceHistoryRepository.existsBySymbolAndTimestamp(
                ph.getSymbol(), ph.getTimestamp()))
            .toList();

        if (uniqueData.isEmpty()) {
            log.debug("No new price data to save (all duplicates)");
            return List.of();
        }

        return priceHistoryRepository.saveAll(uniqueData);
    }

    /**
     * 차트 데이터 조회
     */
    @Transactional(readOnly = true)
    public ChartDataDto getChartData(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
        List<PriceHistory> priceHistories = priceHistoryRepository.findBySymbolAndTimeRange(
            symbol, startTime, endTime
        );

        if (priceHistories.isEmpty()) {
            log.warn("No price history found for symbol: {} between {} and {}",
                symbol, startTime, endTime);
            return null;
        }

        // 데이터 포인트 변환
        List<ChartDataDto.DataPoint> dataPoints = priceHistories.stream()
            .map(ph -> ChartDataDto.DataPoint.builder()
                .time(ph.getTimestamp().format(ISO_FORMATTER))
                .value(ph.getClose())
                .open(ph.getOpen())
                .high(ph.getHigh())
                .low(ph.getLow())
                .volume(ph.getVolume())
                .build())
            .collect(Collectors.toList());

        // 통계 계산
        ChartDataDto.Statistics statistics = calculateStatistics(priceHistories);

        return ChartDataDto.builder()
            .symbol(symbol)
            .baseCurrency(priceHistories.get(0).getBaseCurrency())
            .data(dataPoints)
            .statistics(statistics)
            .build();
    }

    /**
     * 최신 가격 조회
     */
    @Transactional(readOnly = true)
    public PriceHistoryDto getLatestPrice(String symbol) {
        return priceHistoryRepository.findLatestBySymbol(symbol)
            .map(this::toDto)
            .orElse(null);
    }

    /**
     * 복수 심볼의 최신 가격 조회
     */
    @Transactional(readOnly = true)
    public List<PriceHistoryDto> getLatestPrices(List<String> symbols, AssetType assetType) {
        return priceHistoryRepository.findLatestBySymbolsAndAssetType(symbols, assetType)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * 오래된 데이터 정리 (성능 관리)
     */
    @Transactional
    public void cleanupOldData(LocalDateTime beforeDate) {
        log.info("Cleaning up price history data before: {}", beforeDate);
        priceHistoryRepository.deleteByTimestampBefore(beforeDate);
    }

    /**
     * 자산 유형별 모든 심볼 조회
     */
    @Transactional(readOnly = true)
    public List<String> getSymbolsByAssetType(AssetType assetType) {
        return priceHistoryRepository.findDistinctSymbolsByAssetType(assetType);
    }

    // ========== Private Helper Methods ==========

    /**
     * 통계 계산
     */
    private ChartDataDto.Statistics calculateStatistics(List<PriceHistory> priceHistories) {
        if (priceHistories.isEmpty()) {
            return null;
        }

        BigDecimal firstClose = priceHistories.get(0).getClose();
        BigDecimal lastClose = priceHistories.get(priceHistories.size() - 1).getClose();

        BigDecimal changeAmount = lastClose.subtract(firstClose);
        BigDecimal changePercent = changeAmount
            .divide(firstClose, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        BigDecimal highestPrice = priceHistories.stream()
            .map(PriceHistory::getHigh)
            .filter(h -> h != null)
            .max(BigDecimal::compareTo)
            .orElse(lastClose);

        BigDecimal lowestPrice = priceHistories.stream()
            .map(PriceHistory::getLow)
            .filter(l -> l != null)
            .min(BigDecimal::compareTo)
            .orElse(lastClose);

        BigDecimal averageVolume = priceHistories.stream()
            .map(PriceHistory::getVolume)
            .filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(priceHistories.size()), 2, RoundingMode.HALF_UP);

        return ChartDataDto.Statistics.builder()
            .changePercent(changePercent)
            .changeAmount(changeAmount)
            .highestPrice(highestPrice)
            .lowestPrice(lowestPrice)
            .averageVolume(averageVolume)
            .build();
    }

    /**
     * Entity to DTO 변환
     */
    private PriceHistoryDto toDto(PriceHistory entity) {
        return PriceHistoryDto.builder()
            .id(entity.getId())
            .symbol(entity.getSymbol())
            .assetType(entity.getAssetType())
            .baseCurrency(entity.getBaseCurrency())
            .timestamp(entity.getTimestamp())
            .open(entity.getOpen())
            .high(entity.getHigh())
            .low(entity.getLow())
            .close(entity.getClose())
            .volume(entity.getVolume())
            .build();
    }
}
