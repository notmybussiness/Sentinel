package com.pjsent.sentinel.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 엔티티
 * Kakao OAuth2를 통한 사용자 인증 정보를 관리
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "kakao_id", unique = true)
    private String kakaoId;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserSession> sessions = new ArrayList<>();

    @Builder
    public User(String kakaoId, String email, String name, String profileImageUrl) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 사용자 정보 업데이트
     */
    public void updateUserInfo(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 계정 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 계정 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 사용자 세션 추가
     */
    public void addSession(UserSession session) {
        sessions.add(session);
        session.setUser(this);
    }

    /**
     * 사용자 세션 제거
     */
    public void removeSession(UserSession session) {
        sessions.remove(session);
        session.setUser(null);
    }
}