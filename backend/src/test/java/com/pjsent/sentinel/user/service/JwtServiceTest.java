package com.pjsent.sentinel.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtService 단위 테스트
 *
 * TDD Cycle:
 * [RED] 테스트 먼저 작성 → JWT 미구현 시 실패
 * [GREEN] JwtService 구현으로 통과
 * [REFACTOR] 토큰 검증 로직 개선
 *
 * 테스트 범위:
 * - Access Token 생성 및 파싱
 * - Refresh Token 생성 및 파싱
 * - 토큰 유효성 검증
 * - 토큰 만료 여부 확인
 * - 토큰 타입 확인
 * - 사용자 정보 추출
 */
class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET = "test-secret-key-for-jwt-must-be-at-least-256-bits-long";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // @Value 필드에 테스트 값 주입
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L); // 15분
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L); // 7일
    }

    @Test
    @DisplayName("Access Token 생성 시 유효한 JWT 토큰이 반환되어야 함")
    void generate_access_token_should_return_valid_jwt() {
        // When
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);

        // Then
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        assertThat(accessToken.split("\\.")).hasSize(3); // JWT 형식: header.payload.signature
    }

    @Test
    @DisplayName("Refresh Token 생성 시 유효한 JWT 토큰이 반환되어야 함")
    void generate_refresh_token_should_return_valid_jwt() {
        // When
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID, TEST_EMAIL);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Access Token에서 사용자 ID를 추출할 수 있어야 함")
    void get_user_id_from_access_token_should_return_correct_id() {
        // Given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);

        // When
        Long extractedUserId = jwtService.getUserIdFromToken(accessToken);

        // Then
        assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Access Token에서 이메일을 추출할 수 있어야 함")
    void get_email_from_access_token_should_return_correct_email() {
        // Given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);

        // When
        String extractedEmail = jwtService.getEmailFromToken(accessToken);

        // Then
        assertThat(extractedEmail).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Refresh Token에서 사용자 ID를 추출할 수 있어야 함")
    void get_user_id_from_refresh_token_should_return_correct_id() {
        // Given
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID, TEST_EMAIL);

        // When
        Long extractedUserId = jwtService.getUserIdFromToken(refreshToken);

        // Then
        assertThat(extractedUserId).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("유효한 Access Token은 검증 통과해야 함")
    void validate_token_should_return_true_for_valid_access_token() {
        // Given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);

        // When
        boolean isValid = jwtService.validateToken(accessToken);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("유효한 Refresh Token은 검증 통과해야 함")
    void validate_token_should_return_true_for_valid_refresh_token() {
        // Given
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID, TEST_EMAIL);

        // When
        boolean isValid = jwtService.validateToken(refreshToken);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 서명의 토큰은 검증 실패해야 함")
    void validate_token_should_return_false_for_invalid_signature() {
        // Given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);
        String tamperedToken = accessToken.substring(0, accessToken.length() - 10) + "tampered123";

        // When
        boolean isValid = jwtService.validateToken(tamperedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("잘못된 형식의 토큰은 검증 실패해야 함")
    void validate_token_should_return_false_for_malformed_token() {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When
        boolean isValid = jwtService.validateToken(malformedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료되지 않은 토큰은 isTokenExpired가 false를 반환해야 함")
    void is_token_expired_should_return_false_for_valid_token() {
        // Given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);

        // When
        boolean isExpired = jwtService.isTokenExpired(accessToken);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 isTokenExpired가 true를 반환해야 함")
    void is_token_expired_should_return_true_for_expired_token() {
        // Given: 만료 시간을 과거로 설정
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L); // -1초 (과거)
        String expiredToken = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);

        // 원래 설정 복원
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L);

        // When
        boolean isExpired = jwtService.isTokenExpired(expiredToken);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰에 대해 isTokenExpired는 true를 반환해야 함")
    void is_token_expired_should_return_true_for_invalid_token() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isExpired = jwtService.isTokenExpired(invalidToken);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Access Token의 타입은 'access'여야 함")
    void get_token_type_should_return_access_for_access_token() {
        // Given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);

        // When
        String tokenType = jwtService.getTokenType(accessToken);

        // Then
        assertThat(tokenType).isEqualTo("access");
    }

    @Test
    @DisplayName("Refresh Token의 타입은 'refresh'여야 함")
    void get_token_type_should_return_refresh_for_refresh_token() {
        // Given
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID, TEST_EMAIL);

        // When
        String tokenType = jwtService.getTokenType(refreshToken);

        // Then
        assertThat(tokenType).isEqualTo("refresh");
    }

    @Test
    @DisplayName("동일한 토큰에 대해 동일한 해시를 생성해야 함")
    void generate_token_hash_should_return_same_hash_for_same_token() {
        // Given
        String token = "test-token-123";

        // When
        String hash1 = jwtService.generateTokenHash(token);
        String hash2 = jwtService.generateTokenHash(token);

        // Then
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("다른 토큰에 대해 다른 해시를 생성해야 함")
    void generate_token_hash_should_return_different_hash_for_different_tokens() {
        // Given
        String token1 = "test-token-123";
        String token2 = "test-token-456";

        // When
        String hash1 = jwtService.generateTokenHash(token1);
        String hash2 = jwtService.generateTokenHash(token2);

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Access Token과 Refresh Token은 서로 다른 토큰이어야 함")
    void access_token_and_refresh_token_should_be_different() {
        // When
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID, TEST_EMAIL);

        // Then
        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("같은 사용자로 생성한 여러 Access Token은 서로 달라야 함")
    void multiple_access_tokens_for_same_user_should_be_different() {
        // When
        String token1 = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);

        // issuedAt이 초 단위이므로 1초 이상 차이를 둠
        try {
            Thread.sleep(1100); // 1.1초
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("다른 사용자 ID로 생성한 토큰은 다른 userId를 반환해야 함")
    void tokens_with_different_user_ids_should_contain_different_user_ids() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;

        // When
        String token1 = jwtService.generateAccessToken(userId1, TEST_EMAIL);
        String token2 = jwtService.generateAccessToken(userId2, TEST_EMAIL);

        Long extractedUserId1 = jwtService.getUserIdFromToken(token1);
        Long extractedUserId2 = jwtService.getUserIdFromToken(token2);

        // Then
        assertThat(extractedUserId1).isEqualTo(userId1);
        assertThat(extractedUserId2).isEqualTo(userId2);
        assertThat(extractedUserId1).isNotEqualTo(extractedUserId2);
    }

    @Test
    @DisplayName("다른 이메일로 생성한 토큰은 다른 email을 반환해야 함")
    void tokens_with_different_emails_should_contain_different_emails() {
        // Given
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        // When
        String token1 = jwtService.generateAccessToken(TEST_USER_ID, email1);
        String token2 = jwtService.generateAccessToken(TEST_USER_ID, email2);

        String extractedEmail1 = jwtService.getEmailFromToken(token1);
        String extractedEmail2 = jwtService.getEmailFromToken(token2);

        // Then
        assertThat(extractedEmail1).isEqualTo(email1);
        assertThat(extractedEmail2).isEqualTo(email2);
        assertThat(extractedEmail1).isNotEqualTo(extractedEmail2);
    }

    @Test
    @DisplayName("토큰 해시는 null이 아니어야 함")
    void generate_token_hash_should_not_return_null() {
        // Given
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL);

        // When
        String hash = jwtService.generateTokenHash(token);

        // Then
        assertThat(hash).isNotNull();
        assertThat(hash).isNotEmpty();
    }
}
