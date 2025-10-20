package com.pjsent.sentinel.ai.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Gemini AI API 설정
 */
@Getter
@Configuration
public class GeminiConfig {

    @Value("${ai.gemini.enabled:true}")
    private boolean enabled;

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    @Value("${ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${ai.gemini.model:gemini-pro}")
    private String model;

    @Value("${ai.gemini.timeout:30000}")
    private int timeout;

    @Value("${ai.gemini.max-tokens:2048}")
    private int maxTokens;

    @Value("${ai.gemini.temperature:0.7}")
    private double temperature;

    /**
     * Gemini API 엔드포인트 URL 생성
     */
    public String getApiUrl() {
        return String.format("%s/%s:generateContent?key=%s", baseUrl, model, apiKey);
    }

    /**
     * Gemini API 사용 가능 여부 확인
     */
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isEmpty();
    }
}
