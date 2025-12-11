package com.pjsent.sentinel.pricehistory.repository;

import com.pjsent.sentinel.pricehistory.entity.PriceHistory;
import com.pjsent.sentinel.pricehistory.entity.PriceHistory.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    /**
     * 특정 심볼의 시간 범위 내 가격 데이터 조회
     */
    @Query("SELECT ph FROM PriceHistory ph " +
           "WHERE ph.symbol = :symbol " +
           "AND ph.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY ph.timestamp ASC")
    List<PriceHistory> findBySymbolAndTimeRange(
        @Param("symbol") String symbol,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 심볼의 최신 가격 데이터 조회
     */
    @Query("SELECT ph FROM PriceHistory ph " +
           "WHERE ph.symbol = :symbol " +
           "ORDER BY ph.timestamp DESC " +
           "LIMIT 1")
    Optional<PriceHistory> findLatestBySymbol(@Param("symbol") String symbol);

    /**
     * 자산 유형별 최신 가격 데이터 조회 (복수 심볼)
     */
    @Query("SELECT ph FROM PriceHistory ph " +
           "WHERE ph.symbol IN :symbols " +
           "AND ph.assetType = :assetType " +
           "AND ph.timestamp = (" +
           "    SELECT MAX(ph2.timestamp) " +
           "    FROM PriceHistory ph2 " +
           "    WHERE ph2.symbol = ph.symbol" +
           ")")
    List<PriceHistory> findLatestBySymbolsAndAssetType(
        @Param("symbols") List<String> symbols,
        @Param("assetType") AssetType assetType
    );

    /**
     * 특정 시간 이전의 오래된 데이터 삭제 (성능 관리)
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);

    /**
     * 심볼과 타임스탬프로 중복 체크
     */
    boolean existsBySymbolAndTimestamp(String symbol, LocalDateTime timestamp);

    /**
     * 자산 유형별 모든 심볼 조회
     */
    @Query("SELECT DISTINCT ph.symbol FROM PriceHistory ph " +
           "WHERE ph.assetType = :assetType")
    List<String> findDistinctSymbolsByAssetType(@Param("assetType") AssetType assetType);
}
