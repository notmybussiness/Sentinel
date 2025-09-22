package com.pjsent.sentinel.portfolio.service.rebalancing;

import com.pjsent.sentinel.portfolio.dto.RebalancingRecommendationDto;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 하이브리드 리밸런싱 전략
 * 시간 기반과 임계값 기반 전략을 조합한 전략
 *
 * 로직:
 * 1. 정기 검토 주기(기본 3개월)마다 리밸런싱 검토
 * 2. 검토 시점에서 임계값(기본 3%) 이상 편차가 있으면 리밸런싱 실행
 * 3. 정기 검토 주기 사이에도 큰 편차(기본 10%) 발생 시 즉시 리밸런싱
 *
 * 장점:
 * - 시간 기반의 예측 가능성과 임계값 기반의 반응성 결합
 * - 거래 빈도와 비용의 균형
 * - 시장 상황에 따른 유연한 대응
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridRebalancingStrategy implements RebalancingStrategy {

    private final ThresholdBasedRebalancingStrategy thresholdStrategy;
    private final TimeBasedRebalancingStrategy timeStrategy;

    /**
     * 정기 검토 주기 (개월)
     */
    private static final long REVIEW_PERIOD_MONTHS = 3;

    /**
     * 정기 검토 시 최소 임계값 (%)
     */
    private static final double REGULAR_THRESHOLD = 3.0;

    /**
     * 응급 개입 임계값 (%)
     */
    private static final double EMERGENCY_THRESHOLD = 10.0;

    @Override
    public String getStrategyName() {
        return "HYBRID";
    }

    @Override
    public String getDescription() {
        return "시간 기반과 임계값 기반 전략을 조합한 하이브리드 전략입니다. " +
               "정기 검토(3개월)와 응급 개입(10% 편차) 기준을 모두 활용하여 " +
               "안정성과 반응성의 균형을 제공합니다.";
    }

    @Override
    public boolean needsRebalancing(Portfolio portfolio, Map<String, Double> targetAllocation) {
        // 1. 응급 상황 확인 (큰 편차 발생)
        if (isEmergencyRebalancingNeeded(portfolio, targetAllocation)) {
            log.info("하이브리드 전략: 응급 리밸런싱 필요 (임계값 {}% 초과)", EMERGENCY_THRESHOLD);
            return true;
        }

        // 2. 정기 검토 시점 확인
        if (isRegularReviewTime(portfolio)) {
            // 정기 검토 시점에서 최소 임계값 확인
            if (isRegularRebalancingNeeded(portfolio, targetAllocation)) {
                log.info("하이브리드 전략: 정기 리밸런싱 필요 (정기 검토 시점, 임계값 {}% 초과)", REGULAR_THRESHOLD);
                return true;
            }
        }

        return false;
    }

    @Override
    public RebalancingRecommendationDto generateRecommendation(Portfolio portfolio, Map<String, Double> targetAllocation) {
        // 어떤 조건으로 리밸런싱이 필요한지 판단
        boolean isEmergency = isEmergencyRebalancingNeeded(portfolio, targetAllocation);
        boolean isRegular = isRegularReviewTime(portfolio) && isRegularRebalancingNeeded(portfolio, targetAllocation);

        RebalancingRecommendationDto recommendation;

        if (isEmergency) {
            // 응급 상황: 임계값 기반 전략 사용
            recommendation = thresholdStrategy.generateRecommendation(portfolio, targetAllocation);

            // 하이브리드 전략 정보로 업데이트
            recommendation = updateForHybridStrategy(recommendation, "EMERGENCY",
                    "큰 편차로 인한 응급 리밸런싱");
        } else if (isRegular) {
            // 정기 검토: 시간 기반 전략 사용
            recommendation = timeStrategy.generateRecommendation(portfolio, targetAllocation);

            // 하이브리드 전략 정보로 업데이트
            recommendation = updateForHybridStrategy(recommendation, "REGULAR",
                    "정기 검토에 따른 리밸런싱");
        } else {
            // 리밸런싱 불필요한 경우의 기본 추천안
            recommendation = createNoRebalancingRecommendation(portfolio, targetAllocation);
        }

        return recommendation;
    }

    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        try {
            // 정기 검토 주기 검증
            if (configuration.containsKey("reviewPeriodMonths")) {
                long period = Long.parseLong(configuration.get("reviewPeriodMonths").toString());
                if (period < 1 || period > 12) {
                    return false;
                }
            }

            // 정기 임계값 검증
            if (configuration.containsKey("regularThreshold")) {
                double threshold = Double.parseDouble(configuration.get("regularThreshold").toString());
                if (threshold < 1.0 || threshold > 10.0) {
                    return false;
                }
            }

            // 응급 임계값 검증
            if (configuration.containsKey("emergencyThreshold")) {
                double threshold = Double.parseDouble(configuration.get("emergencyThreshold").toString());
                if (threshold < 5.0 || threshold > 30.0) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("Configuration validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 응급 리밸런싱 필요 여부 확인
     */
    private boolean isEmergencyRebalancingNeeded(Portfolio portfolio, Map<String, Double> targetAllocation) {
        Map<String, Double> currentAllocation = calculateCurrentAllocation(portfolio);

        for (String symbol : targetAllocation.keySet()) {
            double currentWeight = currentAllocation.getOrDefault(symbol, 0.0);
            double targetWeight = targetAllocation.get(symbol);
            double deviation = Math.abs(currentWeight - targetWeight);

            if (deviation > EMERGENCY_THRESHOLD) {
                return true;
            }
        }

        return false;
    }

    /**
     * 정기 검토 시점 여부 확인
     */
    private boolean isRegularReviewTime(Portfolio portfolio) {
        LocalDateTime lastUpdate = portfolio.getUpdatedAt();
        LocalDateTime now = LocalDateTime.now();

        long monthsSinceLastUpdate = ChronoUnit.MONTHS.between(lastUpdate, now);
        return monthsSinceLastUpdate >= REVIEW_PERIOD_MONTHS;
    }

    /**
     * 정기 리밸런싱 필요 여부 확인
     */
    private boolean isRegularRebalancingNeeded(Portfolio portfolio, Map<String, Double> targetAllocation) {
        Map<String, Double> currentAllocation = calculateCurrentAllocation(portfolio);

        for (String symbol : targetAllocation.keySet()) {
            double currentWeight = currentAllocation.getOrDefault(symbol, 0.0);
            double targetWeight = targetAllocation.get(symbol);
            double deviation = Math.abs(currentWeight - targetWeight);

            if (deviation > REGULAR_THRESHOLD) {
                return true;
            }
        }

        return false;
    }

    /**
     * 현재 자산 배분 계산
     */
    private Map<String, Double> calculateCurrentAllocation(Portfolio portfolio) {
        // ThresholdBasedRebalancingStrategy의 로직 재사용
        return Map.of(); // 실제 구현에서는 계산 로직 포함
    }

    /**
     * 하이브리드 전략 정보로 추천안 업데이트
     */
    private RebalancingRecommendationDto updateForHybridStrategy(
            RebalancingRecommendationDto originalRecommendation,
            String triggerType,
            String reason) {

        Map<String, Object> strategyDetails = new HashMap<>(originalRecommendation.getStrategyDetails());
        strategyDetails.put("strategy", "hybrid");
        strategyDetails.put("triggerType", triggerType);
        strategyDetails.put("reason", reason);
        strategyDetails.put("reviewPeriodMonths", REVIEW_PERIOD_MONTHS);
        strategyDetails.put("regularThreshold", REGULAR_THRESHOLD);
        strategyDetails.put("emergencyThreshold", EMERGENCY_THRESHOLD);

        return RebalancingRecommendationDto.builder()
                .recommendationId(originalRecommendation.getRecommendationId())
                .portfolioId(originalRecommendation.getPortfolioId())
                .strategyName(getStrategyName())
                .rebalancingNeeded(originalRecommendation.getRebalancingNeeded())
                .totalDeviationPercent(originalRecommendation.getTotalDeviationPercent())
                .currentAllocation(originalRecommendation.getCurrentAllocation())
                .targetAllocation(originalRecommendation.getTargetAllocation())
                .deviations(originalRecommendation.getDeviations())
                .actions(originalRecommendation.getActions())
                .estimatedTransactionCost(originalRecommendation.getEstimatedTransactionCost())
                .taxImpact(originalRecommendation.getTaxImpact())
                .createdAt(originalRecommendation.getCreatedAt())
                .nextReviewDate(LocalDateTime.now().plusMonths(REVIEW_PERIOD_MONTHS))
                .strategyDetails(strategyDetails)
                .priority(originalRecommendation.getPriority())
                .notes(reason + " - " + originalRecommendation.getNotes())
                .build();
    }

    /**
     * 리밸런싱 불필요 시 기본 추천안 생성
     */
    private RebalancingRecommendationDto createNoRebalancingRecommendation(
            Portfolio portfolio, Map<String, Double> targetAllocation) {

        return RebalancingRecommendationDto.builder()
                .recommendationId("NO_REBALANCING_" + System.currentTimeMillis())
                .portfolioId(portfolio.getId())
                .strategyName(getStrategyName())
                .rebalancingNeeded(false)
                .totalDeviationPercent(0.0)
                .currentAllocation(Map.of())
                .targetAllocation(targetAllocation)
                .deviations(Map.of())
                .actions(List.of())
                .estimatedTransactionCost(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .nextReviewDate(LocalDateTime.now().plusMonths(REVIEW_PERIOD_MONTHS))
                .strategyDetails(Map.of(
                        "strategy", "hybrid",
                        "triggerType", "NONE",
                        "reason", "리밸런싱 불필요"
                ))
                .priority(5)
                .notes("현재 포트폴리오 상태가 양호하여 리밸런싱이 필요하지 않습니다.")
                .build();
    }
}