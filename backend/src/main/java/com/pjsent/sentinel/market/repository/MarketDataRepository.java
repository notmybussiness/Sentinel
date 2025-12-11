package com.pjsent.sentinel.market.repository;

import com.pjsent.sentinel.market.entity.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 시장 데이터 Repository
 * 시장 데이터 캐싱을 위한 데이터베이스 작업을 담당
 */
@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    /**
     * 심볼로 최신 시장 데이터 조회
     */
    Optional<MarketData> findTopBySymbolOrderByTimestampDesc(String symbol);

    /**
     * 심볼과 데이터 소스로 최신 시장 데이터 조회
     */
    Optional<MarketData> findTopBySymbolAndDataSourceOrderByTimestampDesc(String symbol, String dataSource);

    /**
     * 심볼로 시장 데이터 목록 조회 (최신순)
     */
    List<MarketData> findBySymbolOrderByTimestampDesc(String symbol);

    /**
     * 데이터 소스로 시장 데이터 목록 조회
     */
    List<MarketData> findByDataSourceOrderByTimestampDesc(String dataSource);

    /**
     * 특정 시간 이후의 시장 데이터 조회
     */
    List<MarketData> findBySymbolAndTimestampAfterOrderByTimestampDesc(String symbol, LocalDateTime timestamp);

    /**
     * 오래된 캐시 데이터 삭제 (15분 이전 데이터)
     */
    @Query("DELETE FROM MarketData m WHERE m.timestamp < :cutoffTime")
    void deleteOldCacheData(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 심볼로 캐시된 데이터 개수 조회
     */
    long countBySymbol(String symbol);

    /**
     * 데이터 소스별 캐시된 데이터 개수 조회
     */
    long countByDataSource(String dataSource);
}
