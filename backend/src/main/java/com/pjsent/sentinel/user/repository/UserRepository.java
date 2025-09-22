package com.pjsent.sentinel.user.repository;

import com.pjsent.sentinel.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 Repository
 * 사용자 관련 데이터베이스 작업을 담당
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Kakao ID로 사용자 조회
     */
    Optional<User> findByKakaoId(String kakaoId);

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * Kakao ID로 사용자 존재 여부 확인
     */
    boolean existsByKakaoId(String kakaoId);

    /**
     * 이메일로 사용자 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 활성 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();

    /**
     * Kakao ID와 활성 상태로 사용자 조회
     */
    Optional<User> findByKakaoIdAndIsActive(String kakaoId, Boolean isActive);

    /**
     * 이메일과 활성 상태로 사용자 조회
     */
    Optional<User> findByEmailAndIsActive(String email, Boolean isActive);
}