package com.pjsent.sentinel.user.controller;

import com.pjsent.sentinel.common.exception.ApiErrorResponse;
import com.pjsent.sentinel.user.dto.LoginResponseDto;
import com.pjsent.sentinel.user.dto.RefreshTokenRequest;
import com.pjsent.sentinel.user.dto.UserDto;
import com.pjsent.sentinel.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth", description = "Authentication and session APIs")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/kakao")
    @Operation(summary = "Get Kakao login URL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login URL retrieved")
    })
    public ResponseEntity<String> getKakaoLoginUrl() {
        log.info("Request Kakao login URL");
        return ResponseEntity.ok(authService.getKakaoLoginUrl());
    }

    @GetMapping("/kakao/callback")
    @Operation(summary = "Handle Kakao OAuth callback")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login succeeded"),
            @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<LoginResponseDto> kakaoCallback(@RequestParam String code) {
        log.info("Handle Kakao callback");
        return ResponseEntity.ok(authService.loginWithKakao(code));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<LoginResponseDto> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Request token refresh");
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout succeeded"),
            @ApiResponse(responseCode = "400", description = "Invalid authorization header", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        log.info("Request logout");
        authService.logout(extractBearerToken(authorization));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user returned"),
            @ApiResponse(responseCode = "400", description = "Invalid authorization header", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader("Authorization") String authorization) {
        log.info("Request current user");
        return ResponseEntity.ok(authService.getCurrentUser(extractBearerToken(authorization)));
    }

    @PostMapping("/dev-login")
    @Operation(summary = "Development login")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dev login succeeded"),
            @ApiResponse(responseCode = "500", description = "Unexpected error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<LoginResponseDto> devLogin() {
        log.info("Request dev login");
        return ResponseEntity.ok(authService.devLogin());
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        return authorization.substring("Bearer ".length());
    }
}
