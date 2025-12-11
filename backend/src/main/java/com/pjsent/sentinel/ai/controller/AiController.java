package com.pjsent.sentinel.ai.controller;

import com.pjsent.sentinel.ai.dto.PortfolioAnalysisRequest;
import com.pjsent.sentinel.ai.dto.PortfolioAnalysisResponse;
import com.pjsent.sentinel.ai.service.GeminiService;
import com.pjsent.sentinel.ai.service.PortfolioAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 분석 API 컨트롤러
 * Gemini AI 기반 포트폴리오 분석 및 투자 제안
 */
@Slf4j
@Tag(name = "AI", description = "AI 기반 포트폴리오 분석 API")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final PortfolioAnalysisService portfolioAnalysisService;
    private final GeminiService geminiService;

    /**
     * 포트폴리오 AI 분석
     *
     * @param request 분석 요청 (portfolioId, analysisType)
     * @return 분석 결과
     */
    @Operation(summary = "포트폴리오 AI 분석",
              description = "Gemini AI를 활용하여 포트폴리오를 분석하고 투자 제안을 제공합니다.")
    @PostMapping("/analyze")
    public ResponseEntity<PortfolioAnalysisResponse> analyzePortfolio(
            @Parameter(description = "분석 요청 (portfolioId, analysisType 필수)")
            @Valid @RequestBody PortfolioAnalysisRequest request) {

        log.info("AI 분석 요청: portfolioId={}, type={}", request.getPortfolioId(), request.getAnalysisType());

        try {
            PortfolioAnalysisResponse response = portfolioAnalysisService.analyzePortfolio(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("AI 분석 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * AI 서비스 상태 확인
     *
     * @return 서비스 상태
     */
    @Operation(summary = "AI 서비스 상태 확인",
              description = "Gemini AI 서비스의 사용 가능 여부를 확인합니다.")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAiStatus() {
        Map<String, Object> status = new HashMap<>();

        boolean isAvailable = geminiService.isAvailable();
        status.put("available", isAvailable);
        status.put("provider", "Gemini AI");
        status.put("model", "gemini-pro");
        status.put("features", new String[]{
            "Portfolio Analysis",
            "Investment Recommendations",
            "Risk Assessment",
            "Diversification Analysis"
        });

        return ResponseEntity.ok(status);
    }

    /**
     * 간단한 AI 테스트 (개발용)
     *
     * @param message 테스트 메시지
     * @return AI 응답
     */
    @Operation(summary = "AI 테스트 (개발용)",
              description = "Gemini AI API를 간단히 테스트합니다.")
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testAi(@RequestParam String message) {
        log.info("AI 테스트 요청: message={}", message);

        try {
            String response = geminiService.generateContent(message);

            Map<String, String> result = new HashMap<>();
            result.put("request", message);
            result.put("response", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("AI 테스트 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
