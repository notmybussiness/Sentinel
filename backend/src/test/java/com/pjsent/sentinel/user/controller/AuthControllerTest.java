package com.pjsent.sentinel.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.config.TestSecurityConfig;
import com.pjsent.sentinel.user.dto.LoginResponseDto;
import com.pjsent.sentinel.user.dto.RefreshTokenRequest;
import com.pjsent.sentinel.user.dto.UserDto;
import com.pjsent.sentinel.user.service.AuthService;
import com.pjsent.sentinel.user.service.JwtService;
import com.pjsent.sentinel.user.service.KakaoOAuthService;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=test-jwt-secret-for-auth-controller-test",
        "kakao.oauth.client-id=test-auth-controller-client-id",
        "kakao.oauth.client-secret=test-auth-controller-client-secret",
        "kakao.oauth.redirect-uri=http://localhost:8080/test/callback" })
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
                .name("test-user")
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
    @DisplayName("returns Kakao login URL")
    void shouldReturnKakaoLoginUrl() throws Exception {
        String expectedUrl = "https://kauth.kakao.com/oauth/authorize?client_id=test";
        when(authService.getKakaoLoginUrl()).thenReturn(expectedUrl);

        mockMvc.perform(get("/api/v1/auth/kakao"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));

        verify(authService, times(1)).getKakaoLoginUrl();
    }

    @Test
    @DisplayName("returns login response for Kakao callback")
    void shouldReturnLoginResponseWhenKakaoCallbackSucceeds() throws Exception {
        String code = "test-auth-code";
        when(authService.loginWithKakao(code)).thenReturn(loginResponseDto);

        mockMvc.perform(get("/api/v1/auth/kakao/callback").param("code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));

        verify(authService, times(1)).loginWithKakao(code);
    }

    @Test
    @DisplayName("returns standardized 401 error when Kakao callback fails")
    void shouldReturnUnauthorizedErrorBodyWhenKakaoCallbackFails() throws Exception {
        String code = "invalid-code";
        when(authService.loginWithKakao(code)).thenThrow(new BadCredentialsException("Invalid code"));

        mockMvc.perform(get("/api/v1/auth/kakao/callback").param("code", code))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/kakao/callback"));

        verify(authService, times(1)).loginWithKakao(code);
    }

    @Test
    @DisplayName("refreshes token")
    void shouldRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(loginResponseDto);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"));

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("returns standardized 401 error when refresh token is invalid")
    void shouldReturnUnauthorizedErrorBodyWhenRefreshTokenIsInvalid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid refresh token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"));

        verify(authService, times(1)).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("returns validation error when refresh token is empty")
    void shouldReturnBadRequestWhenRefreshTokenIsEmpty() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"));

        verify(authService, never()).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("logs out")
    void shouldLogout() throws Exception {
        String accessToken = "valid-access-token";
        doNothing().when(authService).logout(accessToken);

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        verify(authService, times(1)).logout(accessToken);
    }

    @Test
    @DisplayName("returns standardized 500 error when logout fails")
    void shouldReturnInternalServerErrorWhenLogoutFails() throws Exception {
        String accessToken = "valid-access-token";
        doThrow(new RuntimeException("Logout failed")).when(authService).logout(accessToken);

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/logout"));

        verify(authService, times(1)).logout(accessToken);
    }

    @Test
    @DisplayName("returns current user")
    void shouldReturnCurrentUser() throws Exception {
        String accessToken = "valid-access-token";
        when(authService.getCurrentUser(accessToken)).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(authService, times(1)).getCurrentUser(accessToken);
    }

    @Test
    @DisplayName("returns standardized 401 error when get current user fails")
    void shouldReturnUnauthorizedErrorBodyWhenGetCurrentUserFails() throws Exception {
        String accessToken = "invalid-access-token";
        when(authService.getCurrentUser(accessToken)).thenThrow(new BadCredentialsException("Invalid token"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"));

        verify(authService, times(1)).getCurrentUser(accessToken);
    }

    @Test
    @DisplayName("supports dev login")
    void shouldReturnLoginResponseWhenDevLoginSucceeds() throws Exception {
        when(authService.devLogin()).thenReturn(loginResponseDto);

        mockMvc.perform(post("/api/v1/auth/dev-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-access-token"));

        verify(authService, times(1)).devLogin();
    }

    @Test
    @DisplayName("returns standardized 500 error when dev login fails")
    void shouldReturnInternalServerErrorWhenDevLoginFails() throws Exception {
        when(authService.devLogin()).thenThrow(new RuntimeException("Dev login failed"));

        mockMvc.perform(post("/api/v1/auth/dev-login"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/dev-login"));

        verify(authService, times(1)).devLogin();
    }
}
