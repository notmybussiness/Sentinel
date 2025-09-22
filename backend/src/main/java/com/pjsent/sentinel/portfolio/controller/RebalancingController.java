package com.pjsent.sentinel.portfolio.controller;

import com.pjsent.sentinel.portfolio.dto.RebalancingRecommendationDto;
import com.pjsent.sentinel.portfolio.service.RebalancingService;
import com.pjsent.sentinel.portfolio.service.rebalancing.RebalancingStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 포트폴리오 리밸런싱 컨트롤러
 * 리밸런싱 전략 및 추천안 관련 API 엔드포인트 제공
 */
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/rebalancing")
@RequiredArgsConstructor
@Slf4j
public class RebalancingController {

    private final RebalancingService rebalancingService;

    /**
     * 리밸런싱 추천안 생성
     *
     * @param portfolioId 포트폴리오 ID
     * @param userId 사용자 ID (JWT에서 추출)
     * @param request 리밸런싱 요청 정보
     * @return 리밸런싱 추천안
     */
    @PostMapping("/recommendation")
    public ResponseEntity<RebalancingRecommendationDto> generateRecommendation(
            @PathVariable Long portfolioId,
            @RequestParam Long userId,
            @RequestBody RebalancingRequest request) {

        log.info("리밸런싱 추천안 생성 API 호출 - 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);

        RebalancingRecommendationDto recommendation = rebalancingService.generateRebalancingRecommendation(
                portfolioId,
                userId,
                request.getTargetAllocation(),
                request.getStrategyName()
        );

        return ResponseEntity.ok(recommendation);
    }

    /**
     * 리밸런싱 필요 여부 확인
     *
     * @param portfolioId 포트폴리오 ID
     * @param userId 사용자 ID
     * @param request 확인 요청 정보
     * @return 리밸런싱 필요 여부
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkRebalancingNeed(
            @PathVariable Long portfolioId,
            @RequestParam Long userId,
            @RequestBody RebalancingCheckRequest request) {

        log.info("리밸런싱 필요 여부 확인 API 호출 - 포트폴리오 ID: {}", portfolioId);

        boolean needed = rebalancingService.isRebalancingNeeded(
                portfolioId,
                userId,
                request.getTargetAllocation(),
                request.getStrategyName()
        );

        Map<String, Object> response = Map.of(
                "portfolioId", portfolioId,
                "rebalancingNeeded", needed,
                "strategy", request.getStrategyName() != null ? request.getStrategyName() : "THRESHOLD_BASED"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 빠른 리밸런싱 분석
     *
     * @param portfolioId 포트폴리오 ID
     * @param userId 사용자 ID
     * @param request 분석 요청 정보
     * @return 간단한 분석 결과
     */
    @PostMapping("/quick-analysis")
    public ResponseEntity<Map<String, Object>> getQuickAnalysis(
            @PathVariable Long portfolioId,
            @RequestParam Long userId,
            @RequestBody QuickAnalysisRequest request) {

        log.info("빠른 리밸런싱 분석 API 호출 - 포트폴리오 ID: {}", portfolioId);

        Map<String, Object> analysis = rebalancingService.getQuickAnalysis(
                portfolioId,
                userId,
                request.getTargetAllocation()
        );

        return ResponseEntity.ok(analysis);
    }

    /**
     * 포트폴리오 특성 기반 전략 추천
     *
     * @param portfolioId 포트폴리오 ID
     * @param userId 사용자 ID
     * @param riskTolerance 위험 허용도 (1-5, 선택사항)
     * @param investmentHorizon 투자 기간 월수 (선택사항)
     * @return 추천 전략 정보
     */
    @GetMapping("/strategy/recommendation")
    public ResponseEntity<Map<String, Object>> getRecommendedStrategy(
            @PathVariable Long portfolioId,
            @RequestParam Long userId,
            @RequestParam(required = false) Integer riskTolerance,
            @RequestParam(required = false) Integer investmentHorizon) {

        log.info("전략 추천 API 호출 - 포트폴리오 ID: {}, 위험허용도: {}, 투자기간: {}",
                portfolioId, riskTolerance, investmentHorizon);

        Map<String, Object> recommendation = rebalancingService.getRecommendedStrategy(
                portfolioId,
                userId,
                riskTolerance,
                investmentHorizon
        );

        return ResponseEntity.ok(recommendation);
    }

    /**
     * 모든 리밸런싱 전략 정보 조회
     *
     * @return 전략 정보 목록
     */
    @GetMapping("/strategies")
    public ResponseEntity<Map<String, RebalancingStrategyFactory.StrategyInfo>> getAllStrategies() {

        log.info("전략 정보 조회 API 호출");

        Map<String, RebalancingStrategyFactory.StrategyInfo> strategies = rebalancingService.getAllStrategyInfos();

        return ResponseEntity.ok(strategies);
    }

    /**
     * 리밸런싱 요청 DTO
     */
    public static class RebalancingRequest {
        private Map<String, Double> targetAllocation;
        private String strategyName;

        // Getters and Setters
        public Map<String, Double> getTargetAllocation() {
            return targetAllocation;
        }

        public void setTargetAllocation(Map<String, Double> targetAllocation) {
            this.targetAllocation = targetAllocation;
        }

        public String getStrategyName() {
            return strategyName;
        }

        public void setStrategyName(String strategyName) {
            this.strategyName = strategyName;
        }
    }

    /**
     * 리밸런싱 확인 요청 DTO
     */
    public static class RebalancingCheckRequest {
        private Map<String, Double> targetAllocation;
        private String strategyName;

        // Getters and Setters
        public Map<String, Double> getTargetAllocation() {
            return targetAllocation;
        }

        public void setTargetAllocation(Map<String, Double> targetAllocation) {
            this.targetAllocation = targetAllocation;
        }

        public String getStrategyName() {
            return strategyName;
        }

        public void setStrategyName(String strategyName) {
            this.strategyName = strategyName;
        }
    }

    /**
     * 빠른 분석 요청 DTO
     */
    public static class QuickAnalysisRequest {
        private Map<String, Double> targetAllocation;

        // Getters and Setters
        public Map<String, Double> getTargetAllocation() {
            return targetAllocation;
        }

        public void setTargetAllocation(Map<String, Double> targetAllocation) {
            this.targetAllocation = targetAllocation;
        }
    }
}