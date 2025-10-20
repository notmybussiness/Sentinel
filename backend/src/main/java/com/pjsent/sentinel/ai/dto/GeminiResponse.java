package com.pjsent.sentinel.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Gemini API 응답 DTO
 * Google Generative AI API 형식에 맞춘 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiResponse {

    private List<Candidate> candidates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        private Content content;
        private String finishReason;
        private Integer index;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    /**
     * 응답에서 첫 번째 텍스트 추출
     */
    public String getFirstText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Candidate firstCandidate = candidates.get(0);
        if (firstCandidate.getContent() == null ||
            firstCandidate.getContent().getParts() == null ||
            firstCandidate.getContent().getParts().isEmpty()) {
            return null;
        }

        return firstCandidate.getContent().getParts().get(0).getText();
    }
}
