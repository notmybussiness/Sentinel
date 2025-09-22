package com.pjsent.sentinel.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 세션 엔티티
 * JWT 토큰 세션 정보를 관리
 */
@Entity
@Table(name = "user_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "access_token_hash", nullable = false)
    private String accessTokenHash;

    @Column(name = "refresh_token_hash", nullable = false)
    private String refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserSession(User user, String accessTokenHash, String refreshTokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.accessTokenHash = accessTokenHash;
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
    }

    /**
     * 세션 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 세션 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 세션 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 사용자 설정 (양방향 관계 설정용)
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * 토큰 해시 업데이트
     */
    public void updateTokens(String accessTokenHash, String refreshTokenHash, LocalDateTime expiresAt) {
        this.accessTokenHash = accessTokenHash;
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
    }
}
