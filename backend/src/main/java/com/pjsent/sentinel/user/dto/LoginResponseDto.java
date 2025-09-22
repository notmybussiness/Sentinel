package com.pjsent.sentinel.user.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 응답 DTO
 * 로그인 성공 시 반환되는 데이터
 */
@Getter
@Builder
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserDto user;
}
