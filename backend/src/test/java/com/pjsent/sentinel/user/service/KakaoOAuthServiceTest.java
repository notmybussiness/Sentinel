package com.pjsent.sentinel.user.service;

import com.pjsent.sentinel.user.dto.KakaoTokenResponse;
import com.pjsent.sentinel.user.dto.KakaoUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * KakaoOAuthService 단위 테스트
 *
 * TDD Cycle:
 * [RED] 테스트 먼저 작성 → Kakao API 호출 미구현 시 실패
 * [GREEN] KakaoOAuthService 구현으로 통과
 * [REFACTOR] 에러 핸들링 개선
 *
 * 테스트 범위:
 * - Kakao 로그인 URL 생성
 * - 인증 코드 → 액세스 토큰 교환
 * - 액세스 토큰 → 사용자 정보 조회
 * - 리프레시 토큰 → 액세스 토큰 갱신
 * - 에러 시나리오 처리
 */
@ExtendWith(MockitoExtension.class)
class KakaoOAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private KakaoOAuthService kakaoOAuthService;

    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_REDIRECT_URI = "http://localhost:8080/api/v1/auth/kakao/callback";
    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @BeforeEach
    void setUp() {
        // @Value 필드에 테스트 값 주입
        ReflectionTestUtils.setField(kakaoOAuthService, "clientId", TEST_CLIENT_ID);
        ReflectionTestUtils.setField(kakaoOAuthService, "clientSecret", TEST_CLIENT_SECRET);
        ReflectionTestUtils.setField(kakaoOAuthService, "redirectUri", TEST_REDIRECT_URI);
    }

    @Test
    @DisplayName("Kakao 로그인 URL이 올바른 형식으로 생성되어야 함")
    void get_kakao_login_url_should_return_valid_url() {
        // When
        String loginUrl = kakaoOAuthService.getKakaoLoginUrl();

        // Then
        assertThat(loginUrl).isNotNull();
        assertThat(loginUrl).contains("https://kauth.kakao.com/oauth/authorize");
        assertThat(loginUrl).contains("client_id=" + TEST_CLIENT_ID);
        assertThat(loginUrl).contains("redirect_uri=" + TEST_REDIRECT_URI);
        assertThat(loginUrl).contains("response_type=code");
        assertThat(loginUrl).contains("scope=profile_nickname account_email profile_image");
    }

    @Test
    @DisplayName("유효한 코드로 토큰 교환 시 KakaoTokenResponse가 반환되어야 함")
    void exchange_code_for_token_should_return_token_response() {
        // Given
        String authCode = "test-auth-code";
        KakaoTokenResponse mockResponse = new KakaoTokenResponse();
        mockResponse.setAccessToken("test-access-token");
        mockResponse.setTokenType("bearer");
        mockResponse.setRefreshToken("test-refresh-token");
        mockResponse.setExpiresIn(21599L);

        when(restTemplate.postForObject(
                eq(KAKAO_TOKEN_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenReturn(mockResponse);

        // When
        KakaoTokenResponse response = kakaoOAuthService.exchangeCodeForToken(authCode);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("test-access-token");
        assertThat(response.getTokenType()).isEqualTo("bearer");
        assertThat(response.getRefreshToken()).isEqualTo("test-refresh-token");
        assertThat(response.getExpiresIn()).isEqualTo(21599L);
    }

    @Test
    @DisplayName("토큰 교환 시 null 응답이 오면 RuntimeException 발생")
    void exchange_code_for_token_should_throw_exception_when_response_is_null() {
        // Given
        String authCode = "test-auth-code";

        when(restTemplate.postForObject(
                eq(KAKAO_TOKEN_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.exchangeCodeForToken(authCode))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kakao 토큰 교환 실패");
    }

    @Test
    @DisplayName("토큰 교환 시 accessToken이 null이면 RuntimeException 발생")
    void exchange_code_for_token_should_throw_exception_when_access_token_is_null() {
        // Given
        String authCode = "test-auth-code";
        KakaoTokenResponse mockResponse = new KakaoTokenResponse();
        mockResponse.setAccessToken(null); // accessToken이 null

        when(restTemplate.postForObject(
                eq(KAKAO_TOKEN_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.exchangeCodeForToken(authCode))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kakao 토큰 교환 실패");
    }

    @Test
    @DisplayName("토큰 교환 시 RestClientException 발생하면 RuntimeException으로 래핑")
    void exchange_code_for_token_should_wrap_rest_client_exception() {
        // Given
        String authCode = "test-auth-code";

        when(restTemplate.postForObject(
                eq(KAKAO_TOKEN_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenThrow(new RestClientException("Network error"));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.exchangeCodeForToken(authCode))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kakao 토큰 교환 실패");
    }

    @Test
    @DisplayName("유효한 액세스 토큰으로 사용자 정보 조회 성공")
    void get_user_info_should_return_kakao_user_info() {
        // Given
        String accessToken = "test-access-token";

        KakaoUserInfo mockUserInfo = new KakaoUserInfo();
        mockUserInfo.setId(12345L);

        KakaoUserInfo.KakaoAccount kakaoAccount = new KakaoUserInfo.KakaoAccount();
        kakaoAccount.setEmail("test@kakao.com");

        KakaoUserInfo.KakaoAccount.Profile profile = new KakaoUserInfo.KakaoAccount.Profile();
        profile.setNickname("테스트유저");
        profile.setProfileImageUrl("https://example.com/profile.jpg");

        kakaoAccount.setProfile(profile);
        mockUserInfo.setKakaoAccount(kakaoAccount);

        when(restTemplate.exchange(
                eq(KAKAO_USER_INFO_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(KakaoUserInfo.class)
        )).thenReturn(ResponseEntity.ok(mockUserInfo));

        // When
        KakaoUserInfo userInfo = kakaoOAuthService.getUserInfo(accessToken);

        // Then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.getId()).isEqualTo(12345L);
        assertThat(userInfo.getKakaoAccount()).isNotNull();
        assertThat(userInfo.getKakaoAccount().getEmail()).isEqualTo("test@kakao.com");
        assertThat(userInfo.getKakaoAccount().getProfile()).isNotNull();
        assertThat(userInfo.getKakaoAccount().getProfile().getNickname()).isEqualTo("테스트유저");
        assertThat(userInfo.getKakaoAccount().getProfile().getProfileImageUrl())
                .isEqualTo("https://example.com/profile.jpg");
    }

    @Test
    @DisplayName("사용자 정보 조회 시 null 응답이 오면 RuntimeException 발생")
    void get_user_info_should_throw_exception_when_response_is_null() {
        // Given
        String accessToken = "test-access-token";

        when(restTemplate.exchange(
                eq(KAKAO_USER_INFO_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(KakaoUserInfo.class)
        )).thenReturn(ResponseEntity.ok(null));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.getUserInfo(accessToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kakao 사용자 정보 조회 실패");
    }

    @Test
    @DisplayName("사용자 정보 조회 시 id가 null이면 RuntimeException 발생")
    void get_user_info_should_throw_exception_when_id_is_null() {
        // Given
        String accessToken = "test-access-token";
        KakaoUserInfo mockUserInfo = new KakaoUserInfo();
        mockUserInfo.setId(null); // id가 null

        when(restTemplate.exchange(
                eq(KAKAO_USER_INFO_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(KakaoUserInfo.class)
        )).thenReturn(ResponseEntity.ok(mockUserInfo));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.getUserInfo(accessToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kakao 사용자 정보 조회 실패");
    }

    @Test
    @DisplayName("사용자 정보 조회 시 RestClientException 발생하면 RuntimeException으로 래핑")
    void get_user_info_should_wrap_rest_client_exception() {
        // Given
        String accessToken = "test-access-token";

        when(restTemplate.exchange(
                eq(KAKAO_USER_INFO_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(KakaoUserInfo.class)
        )).thenThrow(new RestClientException("Network error"));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.getUserInfo(accessToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kakao 사용자 정보 조회 실패");
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 액세스 토큰 갱신 성공")
    void refresh_access_token_should_return_new_token() {
        // Given
        String refreshToken = "test-refresh-token";
        KakaoTokenResponse mockResponse = new KakaoTokenResponse();
        mockResponse.setAccessToken("new-access-token");
        mockResponse.setTokenType("bearer");
        mockResponse.setExpiresIn(21599L);

        when(restTemplate.postForObject(
                eq(KAKAO_TOKEN_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenReturn(mockResponse);

        // When
        KakaoTokenResponse response = kakaoOAuthService.refreshAccessToken(refreshToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getTokenType()).isEqualTo("bearer");
        assertThat(response.getExpiresIn()).isEqualTo(21599L);
    }

    @Test
    @DisplayName("액세스 토큰 갱신 시 null 응답이 오면 RuntimeException 발생")
    void refresh_access_token_should_throw_exception_when_response_is_null() {
        // Given
        String refreshToken = "test-refresh-token";

        when(restTemplate.postForObject(
                eq(KAKAO_TOKEN_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.refreshAccessToken(refreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kakao 액세스 토큰 갱신 실패");
    }

    @Test
    @DisplayName("액세스 토큰 갱신 시 accessToken이 null이면 RuntimeException 발생")
    void refresh_access_token_should_throw_exception_when_access_token_is_null() {
        // Given
        String refreshToken = "test-refresh-token";
        KakaoTokenResponse mockResponse = new KakaoTokenResponse();
        mockResponse.setAccessToken(null);

        when(restTemplate.postForObject(
                eq(KAKAO_TOKEN_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.refreshAccessToken(refreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kakao 액세스 토큰 갱신 실패");
    }

    @Test
    @DisplayName("액세스 토큰 갱신 시 RestClientException 발생하면 RuntimeException으로 래핑")
    void refresh_access_token_should_wrap_rest_client_exception() {
        // Given
        String refreshToken = "test-refresh-token";

        when(restTemplate.postForObject(
                eq(KAKAO_TOKEN_URL),
                any(HttpEntity.class),
                eq(KakaoTokenResponse.class)
        )).thenThrow(new RestClientException("Network error"));

        // When & Then
        assertThatThrownBy(() -> kakaoOAuthService.refreshAccessToken(refreshToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Kakao 액세스 토큰 갱신 실패");
    }
}
