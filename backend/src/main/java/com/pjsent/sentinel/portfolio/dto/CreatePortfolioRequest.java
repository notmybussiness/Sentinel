package com.pjsent.sentinel.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 포트폴리오 생성 요청 DTO
 */
@Getter
@Setter
public class CreatePortfolioRequest {
    
    @NotBlank(message = "포트폴리오 이름은 필수입니다")
    @Size(max = 255, message = "포트폴리오 이름은 255자를 초과할 수 없습니다")
    private String name;
    
    @Size(max = 1000, message = "포트폴리오 설명은 1000자를 초과할 수 없습니다")
    private String description;
}
