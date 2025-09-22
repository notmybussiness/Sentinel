package com.pjsent.sentinel.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 DTO
 * 사용자 정보를 전송하기 위한 데이터 전송 객체
 */
@Getter
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String name;
    private String profileImageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
