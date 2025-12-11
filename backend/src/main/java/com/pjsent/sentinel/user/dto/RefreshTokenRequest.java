package com.pjsent.sentinel.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 토큰 갱신 요청 DTO
 * 리프레시 토큰으로 액세스 토큰 갱신 요청
 */
@Getter
@Setter
public class RefreshTokenRequest {
    
    @NotBlank(message = "리프레시 토큰은 필수입니다")
    private String refreshToken;
}
