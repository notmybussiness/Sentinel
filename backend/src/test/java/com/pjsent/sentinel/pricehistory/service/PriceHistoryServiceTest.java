package com.pjsent.sentinel.pricehistory.service;

import com.pjsent.sentinel.pricehistory.dto.ChartDataDto;
import com.pjsent.sentinel.pricehistory.dto.PriceHistoryDto;
import com.pjsent.sentinel.pricehistory.entity.PriceHistory;
import com.pjsent.sentinel.pricehistory.entity.PriceHistory.AssetType;
import com.pjsent.sentinel.pricehistory.repository.PriceHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PriceHistoryService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private PriceHistoryService priceHistoryService;

    private PriceHistory samplePriceHistory;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        samplePriceHistory = createPriceHistory("AAPL", AssetType.STOCK, now);
    }

    @Nested
    @DisplayName("savePriceData - 가격 데이터 저장")
    class SavePriceData {

        @Test
        @DisplayName("새로운 데이터 저장 성공")
        void should_SaveData_When_NotExists() {
            // Given
            when(priceHistoryRepository.existsBySymbolAndTimestamp(
                    samplePriceHistory.getSymbol(),
                    samplePriceHistory.getTimestamp()
            )).thenReturn(false);
            when(priceHistoryRepository.save(any(PriceHistory.class)))
                    .thenReturn(samplePriceHistory);

            // When
            PriceHistory result = priceHistoryService.savePriceData(samplePriceHistory);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSymbol()).isEqualTo("AAPL");
            verify(priceHistoryRepository, times(1)).save(samplePriceHistory);
        }

        @Test
        @DisplayName("중복 데이터 저장 시 null 반환")
        void should_ReturnNull_When_DuplicateExists() {
            // Given
            when(priceHistoryRepository.existsBySymbolAndTimestamp(
                    samplePriceHistory.getSymbol(),
                    samplePriceHistory.getTimestamp()
            )).thenReturn(true);

            // When
            PriceHistory result = priceHistoryService.savePriceData(samplePriceHistory);

            // Then
            assertThat(result).isNull();
            verify(priceHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("savePriceDataBatch - 배치 저장")
    class SavePriceDataBatch {

        @Test
        @DisplayName("모든 데이터가 새로운 경우 전체 저장")
        void should_SaveAll_When_AllNew() {
            // Given
            List<PriceHistory> batch = List.of(
                    createPriceHistory("AAPL", AssetType.STOCK, now),
                    createPriceHistory("GOOGL", AssetType.STOCK, now),
                    createPriceHistory("MSFT", AssetType.STOCK, now)
            );

            when(priceHistoryRepository.existsBySymbolAndTimestamp(anyString(), any(LocalDateTime.class)))
                    .thenReturn(false);
            when(priceHistoryRepository.saveAll(anyList())).thenReturn(batch);

            // When
            List<PriceHistory> result = priceHistoryService.savePriceDataBatch(batch);

            // Then
            assertThat(result).hasSize(3);
            verify(priceHistoryRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("모든 데이터가 중복인 경우 빈 리스트 반환")
        void should_ReturnEmpty_When_AllDuplicates() {
            // Given
            List<PriceHistory> batch = List.of(
                    createPriceHistory("AAPL", AssetType.STOCK, now),
                    createPriceHistory("GOOGL", AssetType.STOCK, now)
            );

            when(priceHistoryRepository.existsBySymbolAndTimestamp(anyString(), any(LocalDateTime.class)))
                    .thenReturn(true);

            // When
            List<PriceHistory> result = priceHistoryService.savePriceDataBatch(batch);

            // Then
            assertThat(result).isEmpty();
            verify(priceHistoryRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("일부만 새로운 경우 새로운 것만 저장")
        void should_SaveOnlyNew_When_SomeDuplicates() {
            // Given
            List<PriceHistory> batch = List.of(
                    createPriceHistory("AAPL", AssetType.STOCK, now),
                    createPriceHistory("GOOGL", AssetType.STOCK, now)
            );

            when(priceHistoryRepository.existsBySymbolAndTimestamp(eq("AAPL"), any(LocalDateTime.class)))
                    .thenReturn(true);
            when(priceHistoryRepository.existsBySymbolAndTimestamp(eq("GOOGL"), any(LocalDateTime.class)))
                    .thenReturn(false);
            when(priceHistoryRepository.saveAll(anyList()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            List<PriceHistory> result = priceHistoryService.savePriceDataBatch(batch);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSymbol()).isEqualTo("GOOGL");
        }
    }

    @Nested
    @DisplayName("getChartData - 차트 데이터 조회")
    class GetChartData {

        @Test
        @DisplayName("데이터가 있는 경우 ChartDataDto 반환")
        void should_ReturnChartData_When_DataExists() {
            // Given
            LocalDateTime startTime = now.minusDays(7);
            LocalDateTime endTime = now;

            List<PriceHistory> priceHistories = List.of(
                    createPriceHistoryWithOHLCV("AAPL", now.minusDays(2),
                            new BigDecimal("170"), new BigDecimal("175"),
                            new BigDecimal("169"), new BigDecimal("173"), new BigDecimal("10000000")),
                    createPriceHistoryWithOHLCV("AAPL", now.minusDays(1),
                            new BigDecimal("173"), new BigDecimal("178"),
                            new BigDecimal("172"), new BigDecimal("176"), new BigDecimal("12000000")),
                    createPriceHistoryWithOHLCV("AAPL", now,
                            new BigDecimal("176"), new BigDecimal("180"),
                            new BigDecimal("175"), new BigDecimal("178"), new BigDecimal("15000000"))
            );

            when(priceHistoryRepository.findBySymbolAndTimeRange("AAPL", startTime, endTime))
                    .thenReturn(priceHistories);

            // When
            ChartDataDto result = priceHistoryService.getChartData("AAPL", startTime, endTime);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSymbol()).isEqualTo("AAPL");
            assertThat(result.getData()).hasSize(3);
            assertThat(result.getStatistics()).isNotNull();
            assertThat(result.getStatistics().getHighestPrice())
                    .isEqualByComparingTo(new BigDecimal("180"));
            assertThat(result.getStatistics().getLowestPrice())
                    .isEqualByComparingTo(new BigDecimal("169"));
        }

        @Test
        @DisplayName("데이터가 없는 경우 null 반환")
        void should_ReturnNull_When_NoData() {
            // Given
            LocalDateTime startTime = now.minusDays(7);
            LocalDateTime endTime = now;

            when(priceHistoryRepository.findBySymbolAndTimeRange("UNKNOWN", startTime, endTime))
                    .thenReturn(List.of());

            // When
            ChartDataDto result = priceHistoryService.getChartData("UNKNOWN", startTime, endTime);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("통계 계산 - 변동률 계산")
        void should_CalculateChangePercent_Correctly() {
            // Given
            LocalDateTime startTime = now.minusDays(1);
            LocalDateTime endTime = now;

            List<PriceHistory> priceHistories = List.of(
                    createPriceHistoryWithOHLCV("AAPL", now.minusDays(1),
                            new BigDecimal("100"), new BigDecimal("105"),
                            new BigDecimal("99"), new BigDecimal("100"), new BigDecimal("1000000")),
                    createPriceHistoryWithOHLCV("AAPL", now,
                            new BigDecimal("100"), new BigDecimal("110"),
                            new BigDecimal("100"), new BigDecimal("105"), new BigDecimal("1200000"))
            );

            when(priceHistoryRepository.findBySymbolAndTimeRange("AAPL", startTime, endTime))
                    .thenReturn(priceHistories);

            // When
            ChartDataDto result = priceHistoryService.getChartData("AAPL", startTime, endTime);

            // Then
            assertThat(result.getStatistics().getChangePercent())
                    .isEqualByComparingTo(new BigDecimal("5.0000")); // (105-100)/100 * 100 = 5%
            assertThat(result.getStatistics().getChangeAmount())
                    .isEqualByComparingTo(new BigDecimal("5"));
        }
    }

    @Nested
    @DisplayName("getLatestPrice - 최신 가격 조회")
    class GetLatestPrice {

        @Test
        @DisplayName("데이터가 있는 경우 DTO 반환")
        void should_ReturnDto_When_DataExists() {
            // Given
            when(priceHistoryRepository.findLatestBySymbol("AAPL"))
                    .thenReturn(Optional.of(samplePriceHistory));

            // When
            PriceHistoryDto result = priceHistoryService.getLatestPrice("AAPL");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSymbol()).isEqualTo("AAPL");
            assertThat(result.getAssetType()).isEqualTo(AssetType.STOCK);
        }

        @Test
        @DisplayName("데이터가 없는 경우 null 반환")
        void should_ReturnNull_When_NoData() {
            // Given
            when(priceHistoryRepository.findLatestBySymbol("UNKNOWN"))
                    .thenReturn(Optional.empty());

            // When
            PriceHistoryDto result = priceHistoryService.getLatestPrice("UNKNOWN");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getLatestPrices - 복수 최신 가격 조회")
    class GetLatestPrices {

        @Test
        @DisplayName("여러 심볼의 최신 가격 조회")
        void should_ReturnMultiplePrices() {
            // Given
            List<String> symbols = List.of("AAPL", "GOOGL", "MSFT");
            List<PriceHistory> priceHistories = List.of(
                    createPriceHistory("AAPL", AssetType.STOCK, now),
                    createPriceHistory("GOOGL", AssetType.STOCK, now),
                    createPriceHistory("MSFT", AssetType.STOCK, now)
            );

            when(priceHistoryRepository.findLatestBySymbolsAndAssetType(symbols, AssetType.STOCK))
                    .thenReturn(priceHistories);

            // When
            List<PriceHistoryDto> result = priceHistoryService.getLatestPrices(symbols, AssetType.STOCK);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(PriceHistoryDto::getSymbol)
                    .containsExactly("AAPL", "GOOGL", "MSFT");
        }

        @Test
        @DisplayName("CRYPTO 타입 조회")
        void should_ReturnCryptoPrices() {
            // Given
            List<String> symbols = List.of("BTC", "ETH");
            List<PriceHistory> priceHistories = List.of(
                    createPriceHistory("BTC", AssetType.CRYPTO, now),
                    createPriceHistory("ETH", AssetType.CRYPTO, now)
            );

            when(priceHistoryRepository.findLatestBySymbolsAndAssetType(symbols, AssetType.CRYPTO))
                    .thenReturn(priceHistories);

            // When
            List<PriceHistoryDto> result = priceHistoryService.getLatestPrices(symbols, AssetType.CRYPTO);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(dto -> dto.getAssetType() == AssetType.CRYPTO);
        }
    }

    @Nested
    @DisplayName("cleanupOldData - 오래된 데이터 정리")
    class CleanupOldData {

        @Test
        @DisplayName("오래된 데이터 삭제 호출")
        void should_CallDeleteMethod() {
            // Given
            LocalDateTime beforeDate = now.minusDays(30);
            doNothing().when(priceHistoryRepository).deleteByTimestampBefore(beforeDate);

            // When
            priceHistoryService.cleanupOldData(beforeDate);

            // Then
            verify(priceHistoryRepository, times(1)).deleteByTimestampBefore(beforeDate);
        }
    }

    @Nested
    @DisplayName("getSymbolsByAssetType - 자산 유형별 심볼 조회")
    class GetSymbolsByAssetType {

        @Test
        @DisplayName("STOCK 타입 심볼 목록 반환")
        void should_ReturnStockSymbols() {
            // Given
            when(priceHistoryRepository.findDistinctSymbolsByAssetType(AssetType.STOCK))
                    .thenReturn(List.of("AAPL", "GOOGL", "MSFT"));

            // When
            List<String> result = priceHistoryService.getSymbolsByAssetType(AssetType.STOCK);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).contains("AAPL", "GOOGL", "MSFT");
        }

        @Test
        @DisplayName("ETF 타입 심볼 목록 반환")
        void should_ReturnEtfSymbols() {
            // Given
            when(priceHistoryRepository.findDistinctSymbolsByAssetType(AssetType.ETF))
                    .thenReturn(List.of("SPY", "QQQ"));

            // When
            List<String> result = priceHistoryService.getSymbolsByAssetType(AssetType.ETF);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).contains("SPY", "QQQ");
        }
    }

    // ========== Helper Methods ==========

    private PriceHistory createPriceHistory(String symbol, AssetType assetType, LocalDateTime timestamp) {
        return PriceHistory.builder()
                .id((long) symbol.hashCode())
                .symbol(symbol)
                .assetType(assetType)
                .baseCurrency("USD")
                .timestamp(timestamp)
                .open(new BigDecimal("170.00"))
                .high(new BigDecimal("175.00"))
                .low(new BigDecimal("169.00"))
                .close(new BigDecimal("173.50"))
                .volume(new BigDecimal("10000000"))
                .build();
    }

    private PriceHistory createPriceHistoryWithOHLCV(String symbol, LocalDateTime timestamp,
                                                      BigDecimal open, BigDecimal high,
                                                      BigDecimal low, BigDecimal close,
                                                      BigDecimal volume) {
        return PriceHistory.builder()
                .id((long) (symbol.hashCode() + timestamp.hashCode()))
                .symbol(symbol)
                .assetType(AssetType.STOCK)
                .baseCurrency("USD")
                .timestamp(timestamp)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(volume)
                .build();
    }
}
