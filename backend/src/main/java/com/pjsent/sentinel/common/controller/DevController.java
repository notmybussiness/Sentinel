package com.pjsent.sentinel.common.controller;

import com.pjsent.sentinel.user.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 개발용 컨트롤러
 * JWT 토큰 생성 등 개발/테스트용 기능 제공
 */
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
@Slf4j
public class DevController {

    private final JwtService jwtService;

    /**
     * 개발용 JWT 토큰 생성
     */
    @GetMapping("/token")
    public Map<String, Object> generateToken(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "test@example.com") String email) {

        log.info("개발용 JWT 토큰 생성 - 사용자 ID: {}, 이메일: {}", userId, email);

        String accessToken = jwtService.generateAccessToken(userId, email);
        String refreshToken = jwtService.generateRefreshToken(userId, email);

        return Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken,
            "userId", userId,
            "email", email,
            "message", "개발용 토큰이 생성되었습니다."
        );
    }

    /**
     * JWT 토큰 검증
     */
    @GetMapping("/verify")
    public Map<String, Object> verifyToken(@RequestParam String token) {

        log.info("JWT 토큰 검증 요청");

        try {
            boolean isValid = jwtService.validateToken(token);
            if (isValid) {
                Long userId = jwtService.getUserIdFromToken(token);
                String email = jwtService.getEmailFromToken(token);
                boolean isExpired = jwtService.isTokenExpired(token);

                return Map.of(
                    "valid", true,
                    "userId", userId,
                    "email", email,
                    "expired", isExpired,
                    "message", "유효한 토큰입니다."
                );
            } else {
                return Map.of(
                    "valid", false,
                    "message", "유효하지 않은 토큰입니다."
                );
            }
        } catch (Exception e) {
            return Map.of(
                "valid", false,
                "error", e.getMessage(),
                "message", "토큰 검증 중 오류가 발생했습니다."
            );
        }
    }
}