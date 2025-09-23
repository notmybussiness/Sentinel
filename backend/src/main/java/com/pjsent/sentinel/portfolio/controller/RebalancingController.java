package com.pjsent.sentinel.portfolio.controller;

import com.pjsent.sentinel.common.exception.ResourceNotFoundException;
import com.pjsent.sentinel.portfolio.dto.RebalancingRecommendationDto;
import com.pjsent.sentinel.portfolio.service.RebalancingService;
import com.pjsent.sentinel.portfolio.service.rebalancing.RebalancingStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<?> generateRecommendation(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal Long userId,
            @RequestBody RebalancingRequest request) {

        log.info("리밸런싱 추천안 생성 API 호출 - 포트폴리오 ID: {}, 사용자 ID: {}", portfolioId, userId);

        try {
            // 요청 데이터 기본 검증
            if (request.getTargetAllocation() == null || request.getTargetAllocation().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("목표 자산 배분 정보가 필요합니다."));
            }

            RebalancingRecommendationDto recommendation = rebalancingService.generateRebalancingRecommendation(
                    portfolioId,
                    userId,
                    request.getTargetAllocation(),
                    request.getStrategyName()
            );

            return ResponseEntity.ok(recommendation);

        } catch (ResourceNotFoundException e) {
            log.warn("포트폴리오 없음: 포트폴리오 ID: {}, 사용자 ID: {}, 메시지: {}", portfolioId, userId, e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 데이터: 포트폴리오 ID: {}, 메시지: {}", portfolioId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));

        } catch (IllegalStateException e) {
            log.error("시스템 상태 오류: 포트폴리오 ID: {}, 메시지: {}", portfolioId, e.getMessage());
            return ResponseEntity.status(503)
                    .body(createServiceUnavailableResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("예상치 못한 오류 발생: 포트폴리오 ID: {}, 오류: {}", portfolioId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createInternalErrorResponse());
        }
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
            @AuthenticationPrincipal Long userId,
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
            @AuthenticationPrincipal Long userId,
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
            @AuthenticationPrincipal Long userId,
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

    // Error Response Helper Methods

    /**
     * 일반 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String message) {
        return Map.of(
                "error", true,
                "message", message,
                "timestamp", java.time.LocalDateTime.now(),
                "code", "REBALANCING_ERROR"
        );
    }

    /**
     * 서비스 사용 불가 에러 응답 생성
     */
    private Map<String, Object> createServiceUnavailableResponse(String message) {
        return Map.of(
                "error", true,
                "message", message,
                "timestamp", java.time.LocalDateTime.now(),
                "code", "SERVICE_UNAVAILABLE",
                "suggestion", "외부 API 연결 문제이거나 포트폴리오 데이터 문제일 수 있습니다. 잠시 후 다시 시도해주세요."
        );
    }

    /**
     * 내부 서버 에러 응답 생성
     */
    private Map<String, Object> createInternalErrorResponse() {
        return Map.of(
                "error", true,
                "message", "내부 서버 오류가 발생했습니다.",
                "timestamp", java.time.LocalDateTime.now(),
                "code", "INTERNAL_SERVER_ERROR",
                "suggestion", "문제가 지속되면 개발팀에 문의해주세요."
        );
    }
}