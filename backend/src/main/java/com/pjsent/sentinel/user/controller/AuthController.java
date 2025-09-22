package com.pjsent.sentinel.user.controller;

import com.pjsent.sentinel.user.dto.*;
import com.pjsent.sentinel.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 * 사용자 인증 관련 REST API 엔드포인트를 제공
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Kakao 로그인 URL 조회
     */
    @GetMapping("/kakao")
    public ResponseEntity<String> getKakaoLoginUrl() {
        log.info("Kakao 로그인 URL 조회 요청");
        
        String loginUrl = authService.getKakaoLoginUrl();
        
        return ResponseEntity.ok(loginUrl);
    }

    /**
     * Kakao OAuth2 콜백 처리
     */
    @GetMapping("/kakao/callback")
    public ResponseEntity<LoginResponseDto> kakaoCallback(@RequestParam String code) {
        log.info("Kakao OAuth2 콜백 처리. 코드: {}", code);
        
        try {
            LoginResponseDto response = authService.loginWithKakao(code);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Kakao OAuth2 콜백 처리 실패. 코드: {}, 오류: {}", code, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("토큰 갱신 요청");
        
        try {
            LoginResponseDto response = authService.refreshToken(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("토큰 갱신 실패. 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        log.info("로그아웃 요청");
        
        try {
            String accessToken = authorization.replace("Bearer ", "");
            authService.logout(accessToken);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("로그아웃 실패. 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader("Authorization") String authorization) {
        log.info("현재 사용자 정보 조회 요청");
        
        try {
            String accessToken = authorization.replace("Bearer ", "");
            UserDto user = authService.getCurrentUser(accessToken);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("현재 사용자 정보 조회 실패. 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
