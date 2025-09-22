package com.pjsent.sentinel.portfolio.repository;

import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 포트폴리오 보유 종목 Repository
 * 포트폴리오 보유 종목 관련 데이터베이스 작업을 담당
 */
@Repository
public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {

    /**
     * 포트폴리오 ID로 보유 종목 목록 조회
     */
    List<PortfolioHolding> findByPortfolioIdOrderBySymbol(Long portfolioId);

    /**
     * 포트폴리오 ID와 심볼로 보유 종목 조회
     */
    Optional<PortfolioHolding> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    /**
     * 포트폴리오 ID로 보유 종목 개수 조회
     */
    long countByPortfolioId(Long portfolioId);

    /**
     * 포트폴리오 ID와 심볼로 보유 종목 존재 여부 확인
     */
    boolean existsByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    /**
     * 포트폴리오 ID로 보유 종목 총 가치 합계 조회
     */
    @Query("SELECT COALESCE(SUM(h.marketValue), 0) FROM PortfolioHolding h WHERE h.portfolio.id = :portfolioId")
    Double getTotalMarketValueByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 포트폴리오 ID로 보유 종목 총 비용 합계 조회
     */
    @Query("SELECT COALESCE(SUM(h.totalCost), 0) FROM PortfolioHolding h WHERE h.portfolio.id = :portfolioId")
    Double getTotalCostByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 포트폴리오 ID로 보유 종목 총 손익 합계 조회
     */
    @Query("SELECT COALESCE(SUM(h.gainLoss), 0) FROM PortfolioHolding h WHERE h.portfolio.id = :portfolioId")
    Double getTotalGainLossByPortfolioId(@Param("portfolioId") Long portfolioId);

    /**
     * 사용자 ID로 모든 보유 종목 조회 (여러 포트폴리오에 걸쳐)
     */
    @Query("SELECT h FROM PortfolioHolding h JOIN h.portfolio p WHERE p.userId = :userId ORDER BY h.symbol")
    List<PortfolioHolding> findByUserId(@Param("userId") UUID userId);

    /**
     * 사용자 ID와 심볼로 보유 종목 조회 (여러 포트폴리오에 걸쳐)
     */
    @Query("SELECT h FROM PortfolioHolding h JOIN h.portfolio p WHERE p.userId = :userId AND h.symbol = :symbol")
    List<PortfolioHolding> findByUserIdAndSymbol(@Param("userId") UUID userId, @Param("symbol") String symbol);
}
