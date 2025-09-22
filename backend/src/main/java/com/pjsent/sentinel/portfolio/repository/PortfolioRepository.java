package com.pjsent.sentinel.portfolio.repository;

import com.pjsent.sentinel.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 포트폴리오 Repository
 * 포트폴리오 관련 데이터베이스 작업을 담당
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * 사용자 ID로 포트폴리오 목록 조회
     */
    List<Portfolio> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자 ID와 포트폴리오 ID로 포트폴리오 조회
     */
    Optional<Portfolio> findByIdAndUserId(Long id, Long userId);

    /**
     * 사용자 ID로 포트폴리오 개수 조회
     */
    long countByUserId(Long userId);

    /**
     * 사용자 ID와 포트폴리오 이름으로 포트폴리오 존재 여부 확인
     */
    boolean existsByUserIdAndName(Long userId, String name);

    /**
     * 사용자 ID와 포트폴리오 이름으로 포트폴리오 조회 (이름 중복 체크용)
     */
    Optional<Portfolio> findByUserIdAndName(Long userId, String name);

    /**
     * 사용자 ID로 포트폴리오 총 가치 합계 조회
     */
    @Query("SELECT COALESCE(SUM(p.totalValue), 0) FROM Portfolio p WHERE p.userId = :userId")
    Double getTotalValueByUserId(@Param("userId") Long userId);

    /**
     * 사용자 ID로 포트폴리오 총 손익 합계 조회
     */
    @Query("SELECT COALESCE(SUM(p.totalGainLoss), 0) FROM Portfolio p WHERE p.userId = :userId")
    Double getTotalGainLossByUserId(@Param("userId") Long userId);
}
