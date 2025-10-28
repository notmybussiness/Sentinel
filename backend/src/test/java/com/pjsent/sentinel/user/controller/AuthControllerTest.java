package com.pjsent.sentinel.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.config.TestSecurityConfig;
import com.pjsent.sentinel.user.dto.LoginResponseDto;
import com.pjsent.sentinel.user.dto.RefreshTokenRequest;
import com.pjsent.sentinel.user.dto.UserDto;
import com.pjsent.sentinel.user.service.AuthService;
import com.pjsent.sentinel.user.service.JwtService;
import com.pjsent.sentinel.user.service.KakaoOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 단위 테스트
 */
@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
    "jwt.secret=test-jwt-secret-for-auth-controller-test",
    "kakao.oauth.client-id=test-auth-controller-client-id",
    "kakao.oauth.client-secret=test-auth-controller-client-secret",
    "kakao.oauth.redirect-uri=http://localhost:8080/test/callback"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private KakaoOAuthService kakaoOAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDto userDto;
    private LoginResponseDto loginResponseDto;

    @BeforeEach
    void setUp() {
        userDto = UserDto.builder()
                .id(1L)
                .email("test@example.com")
                .name("테스트 사용자")
                .profileImageUrl("https://example.com/profile.jpg")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        loginResponseDto = LoginResponseDto.builder()
                .accessToken("test-access-token")
                .refreshToken("test-refresh-token")
                .expiresIn(3600L)
                .user(userDto)
                .build();
    }

    @Test
    @DisplayName("Kakao 로그인 URL 조회 성공")
    void should_ReturnKakaoLoginUrl_When_GetKakaoLoginUrl() throws Exception {
        // Given
        String expectedUrl = "https://kauth.kakao.com/oauth/authorize?client_id=test&redirect_uri=http://localhost:8080/callback";
        when(authService.getKakaoLoginUrl()).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/kakao"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));

        verify(authService, times(1)).getKakaoLoginUrl();
    }

    @Test
    @DisplayName("Kakao OAuth2 콜백 처리 성공")
    void should_ReturnLoginResponse_When_KakaoCallbackSuccess() throws Exception {
        // Given
        String code = "test-auth-code";
        when(authService.loginWithKakao(code)).thenReturn(loginResponseDto);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/kakao/callback")
                        .param("code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));

        verify(authService, times(1)).loginWithKakao(code);
    }

    @Test
    @DisplayName("Kakao OAuth2 콜백 처리 실패")
    void should_ReturnUnauthorized_When_KakaoCallbackFails() throws Exception {
        // Given
        String code = "invalid-code";
        when(authService.loginWithKakao(code)).thenThrow(new RuntimeException("Invalid code"));

        // When & Then
        mockMvc.perform(get("/api/v1/auth/kakao/callback")
                        .param("code", code))
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).loginWithKakao(code);
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void should_ReturnNewTokens_When_RefreshTokenValid() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(loginResponseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰")
    void should_ReturnUnauthorized_When_RefreshTokenInvalid() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new RuntimeException("Invalid refresh token"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 빈 리프레시 토큰")
    void should_ReturnBadRequest_When_RefreshTokenEmpty() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        // refreshToken이 null

        // When & Then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void should_ReturnOk_When_LogoutSuccess() throws Exception {
        // Given
        String accessToken = "valid-access-token";
        doNothing().when(authService).logout(accessToken);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        verify(authService, times(1)).logout(accessToken);
    }

    @Test
    @DisplayName("로그아웃 실패")
    void should_ReturnInternalServerError_When_LogoutFails() throws Exception {
        // Given
        String accessToken = "valid-access-token";
        doThrow(new RuntimeException("Logout failed")).when(authService).logout(accessToken);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isInternalServerError());

        verify(authService, times(1)).logout(accessToken);
    }

    @Test
    @DisplayName("현재 사용자 정보 조회 성공")
    void should_ReturnUserInfo_When_GetCurrentUser() throws Exception {
        // Given
        String accessToken = "valid-access-token";
        when(authService.getCurrentUser(accessToken)).thenReturn(userDto);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트 사용자"))
                .andExpect(jsonPath("$.isActive").value(true));

        verify(authService, times(1)).getCurrentUser(accessToken);
    }

    @Test
    @DisplayName("현재 사용자 정보 조회 실패 - 유효하지 않은 토큰")
    void should_ReturnUnauthorized_When_GetCurrentUserWithInvalidToken() throws Exception {
        // Given
        String accessToken = "invalid-access-token";
        when(authService.getCurrentUser(accessToken))
                .thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).getCurrentUser(accessToken);
    }

    @Test
    @DisplayName("개발 모드 로그인 성공")
    void should_ReturnLoginResponse_When_DevLogin() throws Exception {
        // Given
        when(authService.devLogin()).thenReturn(loginResponseDto);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/dev-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.user.id").value(1));

        verify(authService, times(1)).devLogin();
    }

    @Test
    @DisplayName("개발 모드 로그인 실패")
    void should_ReturnInternalServerError_When_DevLoginFails() throws Exception {
        // Given
        when(authService.devLogin()).thenThrow(new RuntimeException("Dev login failed"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/dev-login"))
                .andExpect(status().isInternalServerError());

        verify(authService, times(1)).devLogin();
    }
}
