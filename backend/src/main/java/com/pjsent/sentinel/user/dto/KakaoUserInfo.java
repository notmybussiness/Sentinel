package com.pjsent.sentinel.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Kakao 사용자 정보 DTO
 * Kakao API에서 받은 사용자 정보를 매핑
 */
@Getter
@Setter
public class KakaoUserInfo {
    
    private Long id;
    
    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;
    
    @Getter
    @Setter
    public static class KakaoAccount {
        private String email;
        private Profile profile;
        
        @Getter
        @Setter
        public static class Profile {
            private String nickname;
            
            @JsonProperty("profile_image_url")
            private String profileImageUrl;
        }
    }
}
