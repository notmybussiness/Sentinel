package com.pjsent.sentinel.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 카카오 OAuth2 콜백 요청 DTO
 */
@Data
public class KakaoCallbackRequest {

    @NotBlank(message = "Authorization code는 필수입니다")
    private String code;
}