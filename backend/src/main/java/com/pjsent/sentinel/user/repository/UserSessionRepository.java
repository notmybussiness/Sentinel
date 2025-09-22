package com.pjsent.sentinel.user.repository;

import com.pjsent.sentinel.user.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 세션 Repository
 * 사용자 세션 관련 데이터베이스 작업을 담당
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    /**
     * 사용자 ID로 활성 세션 조회
     */
    @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId AND s.isActive = true AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 액세스 토큰 해시로 세션 조회
     */
    Optional<UserSession> findByAccessTokenHashAndIsActive(String accessTokenHash, Boolean isActive);

    /**
     * 리프레시 토큰 해시로 세션 조회
     */
    Optional<UserSession> findByRefreshTokenHashAndIsActive(String refreshTokenHash, Boolean isActive);

    /**
     * 사용자 ID로 모든 세션 비활성화
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.user.id = :userId")
    void deactivateAllSessionsByUserId(@Param("userId") Long userId);

    /**
     * 만료된 세션 비활성화
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.expiresAt < :now")
    void deactivateExpiredSessions(@Param("now") LocalDateTime now);

    /**
     * 사용자 ID로 세션 개수 조회
     */
    long countByUserIdAndIsActive(Long userId, Boolean isActive);
}
