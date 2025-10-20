package com.pjsent.sentinel.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.ai.dto.PortfolioAnalysisRequest;
import com.pjsent.sentinel.ai.dto.PortfolioAnalysisResponse;
import com.pjsent.sentinel.common.exception.ResourceNotFoundException;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 포트폴리오 AI 분석 서비스
 * Gemini AI를 활용한 포트폴리오 분석 및 투자 제안
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioAnalysisService {

    private final GeminiService geminiService;
    private final PortfolioRepository portfolioRepository;
    private final ObjectMapper objectMapper;

    /**
     * 포트폴리오 분석 실행
     *
     * @param request 분석 요청
     * @return 분석 결과
     */
    @Transactional(readOnly = true)
    public PortfolioAnalysisResponse analyzePortfolio(PortfolioAnalysisRequest request) {
        log.info("포트폴리오 분석 시작: portfolioId={}, type={}",
                request.getPortfolioId(), request.getAnalysisType());

        // 1. 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findById(request.getPortfolioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + request.getPortfolioId()));

        // 2. 프롬프트 생성
        String prompt = buildPrompt(portfolio, request);
        log.debug("프롬프트 생성 완료: length={}", prompt.length());

        // 3. Gemini AI 호출
        String aiResponse = geminiService.generateContent(prompt);
        log.debug("AI 응답 수신: length={}", aiResponse.length());

        // 4. 응답 파싱
        PortfolioAnalysisResponse response = parseAiResponse(aiResponse, request.getAnalysisType());
        response.setAnalyzedAt(LocalDateTime.now());

        log.info("포트폴리오 분석 완료: portfolioId={}", request.getPortfolioId());
        return response;
    }

    /**
     * 포트폴리오 데이터를 기반으로 분석 프롬프트 생성
     */
    private String buildPrompt(Portfolio portfolio, PortfolioAnalysisRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a professional investment advisor analyzing a portfolio.\n\n");

        // Portfolio Summary
        prompt.append(String.format("Portfolio: \"%s\"\n", portfolio.getName()));
        if (portfolio.getDescription() != null && !portfolio.getDescription().isEmpty()) {
            prompt.append(String.format("Description: %s\n", portfolio.getDescription()));
        }
        prompt.append(String.format("Total Value: $%,.2f\n", portfolio.getTotalValue()));
        prompt.append(String.format("Total Cost: $%,.2f\n", portfolio.getTotalCost()));
        prompt.append(String.format("Gain/Loss: %s$%,.2f (%+.2f%%)\n\n",
                portfolio.getTotalGainLoss().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
                portfolio.getTotalGainLoss(),
                portfolio.getTotalGainLossPercent()));

        // Holdings Details
        if (portfolio.getHoldings() != null && !portfolio.getHoldings().isEmpty()) {
            prompt.append("Holdings:\n");
            int index = 1;
            for (PortfolioHolding holding : portfolio.getHoldings()) {
                prompt.append(String.format("%d. %s - %s",
                        index++,
                        holding.getSymbol(),
                        holding.getAssetType()));

                if (holding.getAssetType() == PortfolioHolding.AssetType.CRYPTO) {
                    prompt.append(String.format(" (%s)", holding.getBaseCurrency()));
                }
                prompt.append("\n");

                prompt.append(String.format("   - Quantity: %,.6f\n", holding.getQuantity()));
                prompt.append(String.format("   - Average Cost: $%,.4f\n", holding.getAverageCost()));
                prompt.append(String.format("   - Current Price: $%,.4f\n", holding.getCurrentPrice()));
                prompt.append(String.format("   - Market Value: $%,.2f\n", holding.getMarketValue()));
                prompt.append(String.format("   - Gain/Loss: %s$%,.2f (%+.2f%%)\n",
                        holding.getGainLoss().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
                        holding.getGainLoss(),
                        holding.getGainLossPercent()));
            }
            prompt.append("\n");
        }

        // Analysis Focus
        String focusDescription = switch (request.getAnalysisType()) {
            case OVERVIEW -> "Provide a comprehensive overview of this portfolio's strengths and weaknesses.";
            case DIVERSIFICATION -> "Analyze the diversification of this portfolio across different assets and sectors.";
            case RISK -> "Assess the risk level of this portfolio and identify potential vulnerabilities.";
            case PERFORMANCE -> "Evaluate the performance of this portfolio and individual holdings.";
            case RECOMMENDATION -> "Provide actionable investment recommendations for improving this portfolio.";
        };

        prompt.append(String.format("Analyze this portfolio focusing on: %s\n\n", focusDescription));

        // Response Format
        prompt.append("Provide your analysis in the following JSON format (respond with ONLY valid JSON, no additional text):\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"Brief 2-3 sentence summary of the portfolio\",\n");
        prompt.append("  \"insights\": [\"key insight 1\", \"key insight 2\", \"key insight 3\"],\n");
        prompt.append("  \"recommendations\": [\"recommendation 1\", \"recommendation 2\", \"recommendation 3\"],\n");
        prompt.append("  \"riskLevel\": \"LOW or MEDIUM or HIGH\",\n");
        prompt.append("  \"diversificationScore\": 0-100\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    /**
     * AI 응답을 파싱하여 구조화된 응답으로 변환
     */
    private PortfolioAnalysisResponse parseAiResponse(String aiResponse,
                                                      PortfolioAnalysisRequest.AnalysisType analysisType) {
        try {
            // JSON 부분만 추출 (AI가 추가 텍스트를 포함할 수 있음)
            String jsonString = extractJson(aiResponse);

            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // 응답 파싱
            String summary = jsonNode.has("summary") ? jsonNode.get("summary").asText() : "";

            List<String> insights = new ArrayList<>();
            if (jsonNode.has("insights") && jsonNode.get("insights").isArray()) {
                jsonNode.get("insights").forEach(node -> insights.add(node.asText()));
            }

            List<String> recommendations = new ArrayList<>();
            if (jsonNode.has("recommendations") && jsonNode.get("recommendations").isArray()) {
                jsonNode.get("recommendations").forEach(node -> recommendations.add(node.asText()));
            }

            String riskLevelStr = jsonNode.has("riskLevel") ? jsonNode.get("riskLevel").asText().toUpperCase() : "MEDIUM";
            PortfolioAnalysisResponse.RiskLevel riskLevel;
            try {
                riskLevel = PortfolioAnalysisResponse.RiskLevel.valueOf(riskLevelStr);
            } catch (IllegalArgumentException e) {
                riskLevel = PortfolioAnalysisResponse.RiskLevel.MEDIUM;
            }

            Double diversificationScore = jsonNode.has("diversificationScore") ?
                    jsonNode.get("diversificationScore").asDouble() : 50.0;

            return PortfolioAnalysisResponse.builder()
                    .summary(summary)
                    .insights(insights)
                    .recommendations(recommendations)
                    .riskLevel(riskLevel)
                    .diversificationScore(diversificationScore)
                    .analysisType(analysisType)
                    .build();

        } catch (Exception e) {
            log.error("AI 응답 파싱 실패", e);

            // Fallback: 기본 응답 반환
            return PortfolioAnalysisResponse.builder()
                    .summary("Analysis could not be completed. Please try again.")
                    .insights(List.of("Unable to parse AI response"))
                    .recommendations(List.of("Please check your portfolio data and retry"))
                    .riskLevel(PortfolioAnalysisResponse.RiskLevel.MEDIUM)
                    .diversificationScore(50.0)
                    .analysisType(analysisType)
                    .build();
        }
    }

    /**
     * AI 응답에서 JSON 부분 추출
     */
    private String extractJson(String text) {
        // ```json ... ``` 형식 처리
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.lastIndexOf("```");
            if (start < end) {
                return text.substring(start, end).trim();
            }
        }

        // ``` ... ``` 형식 처리
        if (text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.lastIndexOf("```");
            if (start < end) {
                return text.substring(start, end).trim();
            }
        }

        // { ... } 형식 찾기
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1).trim();
        }

        // 전체 텍스트 반환 (마지막 시도)
        return text.trim();
    }
}
