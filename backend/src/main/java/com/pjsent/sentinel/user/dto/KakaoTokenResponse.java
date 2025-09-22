package com.pjsent.sentinel.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Kakao 토큰 응답 DTO
 * Kakao OAuth2 토큰 교환 응답을 매핑
 */
@Getter
@Setter
public class KakaoTokenResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @JsonProperty("scope")
    private String scope;
}
