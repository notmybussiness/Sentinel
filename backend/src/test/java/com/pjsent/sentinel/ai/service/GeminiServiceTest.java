package com.pjsent.sentinel.ai.service;

import com.pjsent.sentinel.ai.config.GeminiConfig;
import com.pjsent.sentinel.ai.dto.GeminiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GeminiService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private GeminiConfig geminiConfig;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(geminiConfig, webClientBuilder);
    }

    @Nested
    @DisplayName("generateContent - 동기 API 호출")
    class GenerateContent {

        @Test
        @DisplayName("API 비활성화 시 IllegalStateException 발생")
        void should_ThrowException_When_ApiNotAvailable() {
            // Given
            when(geminiConfig.isAvailable()).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> geminiService.generateContent("테스트 프롬프트"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Gemini API is not available");

            verify(webClientBuilder, never()).build();
        }

        @Test
        @DisplayName("정상 응답 반환")
        void should_ReturnResponse_When_ApiCallSucceeds() {
            // Given
            setupMockWebClient();
            when(geminiConfig.isAvailable()).thenReturn(true);
            when(geminiConfig.getApiUrl()).thenReturn("https://api.gemini.test/v1/generate");
            when(geminiConfig.getTimeout()).thenReturn(30000);

            GeminiResponse mockResponse = createMockGeminiResponse("이것은 AI 응답입니다.");

            when(responseSpec.bodyToMono(GeminiResponse.class))
                    .thenReturn(Mono.just(mockResponse));

            // When
            String result = geminiService.generateContent("테스트 프롬프트");

            // Then
            assertThat(result).isEqualTo("이것은 AI 응답입니다.");
        }

        @Test
        @DisplayName("API 응답이 null인 경우 RuntimeException 발생")
        void should_ThrowException_When_ResponseIsNull() {
            // Given
            setupMockWebClient();
            when(geminiConfig.isAvailable()).thenReturn(true);
            when(geminiConfig.getApiUrl()).thenReturn("https://api.gemini.test/v1/generate");
            when(geminiConfig.getTimeout()).thenReturn(30000);

            when(responseSpec.bodyToMono(GeminiResponse.class))
                    .thenReturn(Mono.empty());

            // When & Then
            assertThatThrownBy(() -> geminiService.generateContent("테스트 프롬프트"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Empty response from Gemini API");
        }

        @Test
        @DisplayName("API 호출 실패 시 RuntimeException 발생")
        void should_ThrowException_When_ApiCallFails() {
            // Given
            setupMockWebClient();
            when(geminiConfig.isAvailable()).thenReturn(true);
            when(geminiConfig.getApiUrl()).thenReturn("https://api.gemini.test/v1/generate");
            when(geminiConfig.getTimeout()).thenReturn(30000);

            when(responseSpec.bodyToMono(GeminiResponse.class))
                    .thenReturn(Mono.error(new RuntimeException("Connection refused")));

            // When & Then
            assertThatThrownBy(() -> geminiService.generateContent("테스트 프롬프트"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to call Gemini API");
        }
    }

    @Nested
    @DisplayName("generateContentAsync - 비동기 API 호출")
    class GenerateContentAsync {

        @Test
        @DisplayName("API 비활성화 시 에러 Mono 반환")
        void should_ReturnErrorMono_When_ApiNotAvailable() {
            // Given
            when(geminiConfig.isAvailable()).thenReturn(false);

            // When
            Mono<String> result = geminiService.generateContentAsync("테스트 프롬프트");

            // Then
            assertThatThrownBy(() -> result.block())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Gemini API is not available");
        }

        @Test
        @DisplayName("정상 비동기 응답 반환")
        void should_ReturnMono_When_ApiCallSucceeds() {
            // Given
            setupMockWebClient();
            when(geminiConfig.isAvailable()).thenReturn(true);
            when(geminiConfig.getApiUrl()).thenReturn("https://api.gemini.test/v1/generate");
            when(geminiConfig.getTimeout()).thenReturn(30000);

            GeminiResponse mockResponse = createMockGeminiResponse("비동기 AI 응답");

            Mono<GeminiResponse> responseMono = Mono.just(mockResponse);
            when(responseSpec.bodyToMono(GeminiResponse.class)).thenReturn(responseMono);

            // When
            Mono<String> result = geminiService.generateContentAsync("테스트 프롬프트");

            // Then
            assertThat(result.block()).isEqualTo("비동기 AI 응답");
        }
    }

    @Nested
    @DisplayName("isAvailable - 서비스 가용성 확인")
    class IsAvailable {

        @Test
        @DisplayName("설정이 활성화된 경우 true 반환")
        void should_ReturnTrue_When_ConfigEnabled() {
            // Given
            when(geminiConfig.isAvailable()).thenReturn(true);

            // When
            boolean result = geminiService.isAvailable();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("설정이 비활성화된 경우 false 반환")
        void should_ReturnFalse_When_ConfigDisabled() {
            // Given
            when(geminiConfig.isAvailable()).thenReturn(false);

            // When
            boolean result = geminiService.isAvailable();

            // Then
            assertThat(result).isFalse();
        }
    }

    // ========== Helper Methods ==========

    private void setupMockWebClient() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    private GeminiResponse createMockGeminiResponse(String text) {
        GeminiResponse.Part part = new GeminiResponse.Part();
        part.setText(text);

        GeminiResponse.Content content = new GeminiResponse.Content();
        content.setParts(List.of(part));

        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        candidate.setContent(content);

        GeminiResponse response = new GeminiResponse();
        response.setCandidates(List.of(candidate));

        return response;
    }
}
