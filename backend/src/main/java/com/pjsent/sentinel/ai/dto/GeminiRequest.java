package com.pjsent.sentinel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gemini API 요청 DTO
 * Google Generative AI API 형식에 맞춘 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {

    private List<Content> contents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    /**
     * 간단한 텍스트 프롬프트로 요청 생성
     */
    public static GeminiRequest of(String prompt) {
        return GeminiRequest.builder()
                .contents(List.of(
                        Content.builder()
                                .parts(List.of(
                                        Part.builder()
                                                .text(prompt)
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }
}
