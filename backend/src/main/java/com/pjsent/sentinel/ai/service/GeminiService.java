package com.pjsent.sentinel.ai.service;

import com.pjsent.sentinel.ai.config.GeminiConfig;
import com.pjsent.sentinel.ai.dto.GeminiRequest;
import com.pjsent.sentinel.ai.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Gemini AI API 호출 서비스
 * Google Generative AI를 사용한 자연어 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final WebClient.Builder webClientBuilder;

    /**
     * Gemini API에 프롬프트 전송 및 응답 받기
     *
     * @param prompt 전송할 프롬프트
     * @return Gemini AI 응답
     */
    public String generateContent(String prompt) {
        if (!geminiConfig.isAvailable()) {
            log.warn("Gemini API가 비활성화되었거나 API 키가 없습니다");
            throw new IllegalStateException("Gemini API is not available");
        }

        log.debug("Gemini API 호출 시작: prompt length = {}", prompt.length());

        try {
            GeminiRequest request = GeminiRequest.of(prompt);

            WebClient webClient = webClientBuilder.build();

            GeminiResponse response = webClient.post()
                    .uri(geminiConfig.getApiUrl())
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofMillis(geminiConfig.getTimeout()))
                    .block();

            if (response == null || response.getFirstText() == null) {
                log.error("Gemini API 응답이 비어있습니다");
                throw new RuntimeException("Empty response from Gemini API");
            }

            String responseText = response.getFirstText();
            log.debug("Gemini API 응답 성공: response length = {}", responseText.length());

            return responseText;

        } catch (Exception e) {
            log.error("Gemini API 호출 실패", e);
            throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * 비동기 방식으로 Gemini API 호출
     *
     * @param prompt 전송할 프롬프트
     * @return Mono<String> 비동기 응답
     */
    public Mono<String> generateContentAsync(String prompt) {
        if (!geminiConfig.isAvailable()) {
            return Mono.error(new IllegalStateException("Gemini API is not available"));
        }

        log.debug("Gemini API 비동기 호출 시작: prompt length = {}", prompt.length());

        try {
            GeminiRequest request = GeminiRequest.of(prompt);

            WebClient webClient = webClientBuilder.build();

            return webClient.post()
                    .uri(geminiConfig.getApiUrl())
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofMillis(geminiConfig.getTimeout()))
                    .map(response -> {
                        if (response == null || response.getFirstText() == null) {
                            throw new RuntimeException("Empty response from Gemini API");
                        }
                        String responseText = response.getFirstText();
                        log.debug("Gemini API 비동기 응답 성공: response length = {}", responseText.length());
                        return responseText;
                    })
                    .onErrorMap(e -> {
                        log.error("Gemini API 비동기 호출 실패", e);
                        return new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
                    });

        } catch (Exception e) {
            log.error("Gemini API 비동기 호출 실패", e);
            return Mono.error(new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e));
        }
    }

    /**
     * Gemini API 상태 확인
     *
     * @return 사용 가능 여부
     */
    public boolean isAvailable() {
        return geminiConfig.isAvailable();
    }
}
