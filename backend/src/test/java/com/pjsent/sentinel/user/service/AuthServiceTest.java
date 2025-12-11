package com.pjsent.sentinel.user.service;

import com.pjsent.sentinel.user.dto.*;
import com.pjsent.sentinel.user.entity.User;
import com.pjsent.sentinel.user.repository.UserRepository;
import com.pjsent.sentinel.user.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService 단위 테스트
 *
 * TDD Cycle:
 * [RED] 테스트 먼저 작성 → 인증 로직 미구현 시 실패
 * [GREEN] AuthService 구현으로 통과
 * [REFACTOR] 에러 핸들링 및 세션 관리 개선
 *
 * 테스트 범위:
 * - Kakao OAuth 로그인 (신규/기존 사용자)
 * - JWT 토큰 갱신 (정상/만료/잘못된 토큰)
 * - 로그아웃 (세션 비활성화)
 * - 현재 사용자 정보 조회
 * - 개발 모드 로그인
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private KakaoOAuthService kakaoOAuthService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private static final String TEST_AUTH_CODE = "test-auth-code";
    private static final String TEST_ACCESS_TOKEN = "test-kakao-access-token";
    private static final String TEST_JWT_ACCESS_TOKEN = "test-jwt-access-token";
    private static final String TEST_JWT_REFRESH_TOKEN = "test-jwt-refresh-token";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "test@kakao.com";
    private static final String TEST_KAKAO_ID = "12345";

    @BeforeEach
    void setUp() {
        // 매 테스트마다 새로운 설정이 필요한 경우 여기서 준비
    }

    @Test
    @DisplayName("[Kakao 로그인 - 신규 사용자] 성공 시 사용자 생성 및 JWT 토큰 반환")
    void kakao_login_with_new_user_should_create_user_and_return_tokens() {
        // Given: Kakao API 응답 준비
        KakaoTokenResponse kakaoTokenResponse = new KakaoTokenResponse();
        kakaoTokenResponse.setAccessToken(TEST_ACCESS_TOKEN);
        kakaoTokenResponse.setExpiresIn(21599L);

        KakaoUserInfo kakaoUserInfo = createMockKakaoUserInfo(TEST_KAKAO_ID, TEST_EMAIL, "테스트유저");

        User newUser = User.builder()
                .kakaoId(TEST_KAKAO_ID)
                .email(TEST_EMAIL)
                .name("테스트유저")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
        ReflectionTestUtils.setField(newUser, "id", TEST_USER_ID); // ID 설정 (save 후 반환된 것처럼)

        // Mocking
        when(kakaoOAuthService.exchangeCodeForToken(TEST_AUTH_CODE)).thenReturn(kakaoTokenResponse);
        when(kakaoOAuthService.getUserInfo(TEST_ACCESS_TOKEN)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(TEST_KAKAO_ID)).thenReturn(Optional.empty()); // 신규 사용자
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL)).thenReturn(TEST_JWT_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_USER_ID, TEST_EMAIL)).thenReturn(TEST_JWT_REFRESH_TOKEN);
        when(jwtService.generateTokenHash(anyString())).thenReturn("token-hash");

        // When
        LoginResponseDto response = authService.loginWithKakao(TEST_AUTH_CODE);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(TEST_JWT_ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(TEST_JWT_REFRESH_TOKEN);
        assertThat(response.getExpiresIn()).isEqualTo(21599L);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(TEST_EMAIL);

        verify(userRepository, times(1)).save(any(User.class)); // 신규 사용자 저장 확인
        verify(userSessionRepository, times(1)).save(any()); // 세션 저장 확인
    }

    @Test
    @DisplayName("[Kakao 로그인 - 기존 사용자] 사용자 정보 업데이트 및 JWT 토큰 반환")
    void kakao_login_with_existing_user_should_update_user_and_return_tokens() {
        // Given
        KakaoTokenResponse kakaoTokenResponse = new KakaoTokenResponse();
        kakaoTokenResponse.setAccessToken(TEST_ACCESS_TOKEN);
        kakaoTokenResponse.setExpiresIn(21599L);

        KakaoUserInfo kakaoUserInfo = createMockKakaoUserInfo(TEST_KAKAO_ID, TEST_EMAIL, "업데이트된이름");

        User existingUser = User.builder()
                .kakaoId(TEST_KAKAO_ID)
                .email(TEST_EMAIL)
                .name("기존이름")
                .profileImageUrl("https://example.com/old-profile.jpg")
                .build();
        ReflectionTestUtils.setField(existingUser, "id", TEST_USER_ID);

        // Mocking
        when(kakaoOAuthService.exchangeCodeForToken(TEST_AUTH_CODE)).thenReturn(kakaoTokenResponse);
        when(kakaoOAuthService.getUserInfo(TEST_ACCESS_TOKEN)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(TEST_KAKAO_ID)).thenReturn(Optional.of(existingUser)); // 기존 사용자
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL)).thenReturn(TEST_JWT_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_USER_ID, TEST_EMAIL)).thenReturn(TEST_JWT_REFRESH_TOKEN);
        when(jwtService.generateTokenHash(anyString())).thenReturn("token-hash");

        // When
        LoginResponseDto response = authService.loginWithKakao(TEST_AUTH_CODE);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(TEST_JWT_ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(TEST_JWT_REFRESH_TOKEN);

        verify(userRepository, times(1)).save(any(User.class)); // 사용자 업데이트 확인
        verify(userSessionRepository, times(1)).save(any()); // 세션 저장 확인
    }

    @Test
    @DisplayName("[Kakao 로그인 - 실패] KakaoOAuthService 예외 발생 시 RuntimeException으로 래핑")
    void kakao_login_should_throw_runtime_exception_when_kakao_service_fails() {
        // Given
        when(kakaoOAuthService.exchangeCodeForToken(TEST_AUTH_CODE))
                .thenThrow(new RuntimeException("Kakao API error"));

        // When & Then
        assertThatThrownBy(() -> authService.loginWithKakao(TEST_AUTH_CODE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("로그인 처리 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("[Kakao 로그인] 이메일이 없는 경우 Kakao ID를 이메일로 사용")
    void kakao_login_should_use_kakao_id_as_email_when_email_is_null() {
        // Given
        KakaoTokenResponse kakaoTokenResponse = new KakaoTokenResponse();
        kakaoTokenResponse.setAccessToken(TEST_ACCESS_TOKEN);
        kakaoTokenResponse.setExpiresIn(21599L);

        KakaoUserInfo kakaoUserInfo = createMockKakaoUserInfo(TEST_KAKAO_ID, null, "테스트유저"); // 이메일 없음

        User newUser = User.builder()
                .kakaoId(TEST_KAKAO_ID)
                .email(TEST_KAKAO_ID + "@kakao.com") // Kakao ID를 이메일로 사용
                .name("테스트유저")
                .build();
        ReflectionTestUtils.setField(newUser, "id", TEST_USER_ID);

        // Mocking
        when(kakaoOAuthService.exchangeCodeForToken(TEST_AUTH_CODE)).thenReturn(kakaoTokenResponse);
        when(kakaoOAuthService.getUserInfo(TEST_ACCESS_TOKEN)).thenReturn(kakaoUserInfo);
        when(userRepository.findByKakaoId(TEST_KAKAO_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(jwtService.generateAccessToken(anyLong(), anyString())).thenReturn(TEST_JWT_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn(TEST_JWT_REFRESH_TOKEN);
        when(jwtService.generateTokenHash(anyString())).thenReturn("token-hash");

        // When
        LoginResponseDto response = authService.loginWithKakao(TEST_AUTH_CODE);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository, times(1)).save(argThat(user ->
                user.getEmail().equals(TEST_KAKAO_ID + "@kakao.com")
        ));
    }

    @Test
    @DisplayName("[토큰 갱신 - 성공] 유효한 refresh token으로 새로운 토큰 발급")
    void refresh_token_should_return_new_tokens_when_valid() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(TEST_JWT_REFRESH_TOKEN);

        User user = User.builder()
                .kakaoId(TEST_KAKAO_ID)
                .email(TEST_EMAIL)
                .name("테스트유저")
                .build();
        ReflectionTestUtils.setField(user, "id", TEST_USER_ID);

        // Mocking
        when(jwtService.validateToken(TEST_JWT_REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.isTokenExpired(TEST_JWT_REFRESH_TOKEN)).thenReturn(false);
        when(jwtService.getUserIdFromToken(TEST_JWT_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);
        when(jwtService.getEmailFromToken(TEST_JWT_REFRESH_TOKEN)).thenReturn(TEST_EMAIL);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(TEST_USER_ID, TEST_EMAIL)).thenReturn("new-refresh-token");
        when(jwtService.generateTokenHash(anyString())).thenReturn("token-hash");

        // When
        LoginResponseDto response = authService.refreshToken(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo(TEST_EMAIL);

        verify(userSessionRepository, times(1)).deactivateAllSessionsByUserId(TEST_USER_ID);
        verify(userSessionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("[토큰 갱신 - 실패] 유효하지 않은 refresh token은 예외 발생")
    void refresh_token_should_throw_exception_when_token_is_invalid() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");

        // Mocking
        when(jwtService.validateToken("invalid-refresh-token")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("유효하지 않은 리프레시 토큰입니다");
    }

    @Test
    @DisplayName("[토큰 갱신 - 실패] 만료된 refresh token은 예외 발생")
    void refresh_token_should_throw_exception_when_token_is_expired() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(TEST_JWT_REFRESH_TOKEN);

        // Mocking
        when(jwtService.validateToken(TEST_JWT_REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.isTokenExpired(TEST_JWT_REFRESH_TOKEN)).thenReturn(true); // 만료됨

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("만료된 리프레시 토큰입니다");
    }

    @Test
    @DisplayName("[토큰 갱신 - 실패] 존재하지 않는 사용자는 예외 발생")
    void refresh_token_should_throw_exception_when_user_not_found() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(TEST_JWT_REFRESH_TOKEN);

        // Mocking
        when(jwtService.validateToken(TEST_JWT_REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.isTokenExpired(TEST_JWT_REFRESH_TOKEN)).thenReturn(false);
        when(jwtService.getUserIdFromToken(TEST_JWT_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty()); // 사용자 없음

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("[로그아웃 - 성공] 사용자의 모든 세션을 비활성화")
    void logout_should_deactivate_all_user_sessions() {
        // Given
        String accessToken = TEST_JWT_ACCESS_TOKEN;

        // Mocking
        when(jwtService.getUserIdFromToken(accessToken)).thenReturn(TEST_USER_ID);

        // When
        authService.logout(accessToken);

        // Then
        verify(userSessionRepository, times(1)).deactivateAllSessionsByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("[로그아웃 - 실패] 잘못된 토큰은 예외 발생")
    void logout_should_throw_exception_when_token_is_invalid() {
        // Given
        String invalidToken = "invalid-token";

        // Mocking
        when(jwtService.getUserIdFromToken(invalidToken)).thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        assertThatThrownBy(() -> authService.logout(invalidToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("로그아웃 처리 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("[현재 사용자 조회 - 성공] 유효한 토큰으로 사용자 정보 반환")
    void get_current_user_should_return_user_dto_when_token_is_valid() {
        // Given
        String accessToken = TEST_JWT_ACCESS_TOKEN;
        User user = User.builder()
                .kakaoId(TEST_KAKAO_ID)
                .email(TEST_EMAIL)
                .name("테스트유저")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
        ReflectionTestUtils.setField(user, "id", TEST_USER_ID);

        // Mocking
        when(jwtService.getUserIdFromToken(accessToken)).thenReturn(TEST_USER_ID);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

        // When
        UserDto userDto = authService.getCurrentUser(accessToken);

        // Then
        assertThat(userDto).isNotNull();
        assertThat(userDto.getId()).isEqualTo(TEST_USER_ID);
        assertThat(userDto.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(userDto.getName()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("[현재 사용자 조회 - 실패] 존재하지 않는 사용자는 예외 발생")
    void get_current_user_should_throw_exception_when_user_not_found() {
        // Given
        String accessToken = TEST_JWT_ACCESS_TOKEN;

        // Mocking
        when(jwtService.getUserIdFromToken(accessToken)).thenReturn(TEST_USER_ID);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.getCurrentUser(accessToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("[개발 모드 로그인 - 신규 사용자] dev@sentinel.com 사용자 생성 및 토큰 반환")
    void dev_login_should_create_dev_user_and_return_tokens_when_user_not_exists() {
        // Given
        User devUser = User.builder()
                .kakaoId("dev-user")
                .email("dev@sentinel.com")
                .name("개발자")
                .build();
        ReflectionTestUtils.setField(devUser, "id", TEST_USER_ID);

        // Mocking
        when(userRepository.findByEmail("dev@sentinel.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(devUser);
        when(jwtService.generateAccessToken(TEST_USER_ID, "dev@sentinel.com")).thenReturn(TEST_JWT_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_USER_ID, "dev@sentinel.com")).thenReturn(TEST_JWT_REFRESH_TOKEN);
        when(jwtService.generateTokenHash(anyString())).thenReturn("token-hash");

        // When
        LoginResponseDto response = authService.devLogin();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(TEST_JWT_ACCESS_TOKEN);
        assertThat(response.getRefreshToken()).isEqualTo(TEST_JWT_REFRESH_TOKEN);
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("dev@sentinel.com");

        verify(userRepository, times(1)).save(any(User.class));
        verify(userSessionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("[개발 모드 로그인 - 기존 사용자] 기존 dev@sentinel.com 사용자로 토큰 반환")
    void dev_login_should_use_existing_dev_user_when_user_exists() {
        // Given
        User existingDevUser = User.builder()
                .kakaoId("dev-user")
                .email("dev@sentinel.com")
                .name("개발자")
                .build();
        ReflectionTestUtils.setField(existingDevUser, "id", TEST_USER_ID);

        // Mocking
        when(userRepository.findByEmail("dev@sentinel.com")).thenReturn(Optional.of(existingDevUser));
        when(jwtService.generateAccessToken(TEST_USER_ID, "dev@sentinel.com")).thenReturn(TEST_JWT_ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(TEST_USER_ID, "dev@sentinel.com")).thenReturn(TEST_JWT_REFRESH_TOKEN);
        when(jwtService.generateTokenHash(anyString())).thenReturn("token-hash");

        // When
        LoginResponseDto response = authService.devLogin();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(TEST_JWT_ACCESS_TOKEN);
        assertThat(response.getUser().getEmail()).isEqualTo("dev@sentinel.com");

        verify(userRepository, never()).save(any(User.class)); // 신규 저장하지 않음
        verify(userSessionRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("[Kakao 로그인 URL 조회] KakaoOAuthService에서 URL 반환")
    void get_kakao_login_url_should_delegate_to_kakao_service() {
        // Given
        String mockUrl = "https://kauth.kakao.com/oauth/authorize?client_id=test";

        when(kakaoOAuthService.getKakaoLoginUrl()).thenReturn(mockUrl);

        // When
        String loginUrl = authService.getKakaoLoginUrl();

        // Then
        assertThat(loginUrl).isEqualTo(mockUrl);
        verify(kakaoOAuthService, times(1)).getKakaoLoginUrl();
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * Mock KakaoUserInfo 생성
     */
    private KakaoUserInfo createMockKakaoUserInfo(String kakaoId, String email, String nickname) {
        KakaoUserInfo kakaoUserInfo = new KakaoUserInfo();
        kakaoUserInfo.setId(Long.valueOf(kakaoId));

        KakaoUserInfo.KakaoAccount kakaoAccount = new KakaoUserInfo.KakaoAccount();
        kakaoAccount.setEmail(email);

        KakaoUserInfo.KakaoAccount.Profile profile = new KakaoUserInfo.KakaoAccount.Profile();
        profile.setNickname(nickname);
        profile.setProfileImageUrl("https://example.com/profile.jpg");

        kakaoAccount.setProfile(profile);
        kakaoUserInfo.setKakaoAccount(kakaoAccount);

        return kakaoUserInfo;
    }
}
