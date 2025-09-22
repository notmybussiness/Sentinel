package com.pjsent.sentinel.portfolio.service;

import com.pjsent.sentinel.common.exception.ResourceNotFoundException;
import com.pjsent.sentinel.portfolio.dto.RebalancingRecommendationDto;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import com.pjsent.sentinel.portfolio.service.rebalancing.RebalancingStrategy;
import com.pjsent.sentinel.portfolio.service.rebalancing.RebalancingStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 포트폴리오 리밸런싱 서비스
 * 다양한 리밸런싱 전략을 사용하여 포트폴리오 최적화 추천을 제공
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RebalancingService {

    private final PortfolioRepository portfolioRepository;
    private final RebalancingStrategyFactory strategyFactory;

    /**
     * 포트폴리오 리밸런싱 추천안 생성
     *
     * @param portfolioId 포트폴리오 ID
     * @param userId 사용자 ID
     * @param targetAllocation 목표 자산 배분 (symbol -> percentage)
     * @param strategyName 사용할 전략명 (선택사항, 기본값: THRESHOLD_BASED)
     * @return 리밸런싱 추천안
     */
    public RebalancingRecommendationDto generateRebalancingRecommendation(
            Long portfolioId,
            Long userId,
            Map<String, Double> targetAllocation,
            String strategyName) {

        log.info("리밸런싱 추천안 생성 시작 - 포트폴리오 ID: {}, 사용자 ID: {}, 전략: {}",
                portfolioId, userId, strategyName);

        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));

        // 목표 배분 검증
        validateTargetAllocation(targetAllocation);

        // 전략 선택
        RebalancingStrategy strategy = strategyFactory.getStrategy(strategyName);

        // 추천안 생성
        RebalancingRecommendationDto recommendation = strategy.generateRecommendation(portfolio, targetAllocation);

        log.info("리밸런싱 추천안 생성 완료 - 추천안 ID: {}, 리밸런싱 필요: {}",
                recommendation.getRecommendationId(), recommendation.getRebalancingNeeded());

        return recommendation;
    }

    /**
     * 포트폴리오 특성 기반 전략 추천
     *
     * @param portfolioId 포트폴리오 ID
     * @param userId 사용자 ID
     * @param riskTolerance 위험 허용도 (1-5)
     * @param investmentHorizon 투자 기간 (월)
     * @return 추천 전략 정보
     */
    public Map<String, Object> getRecommendedStrategy(
            Long portfolioId,
            Long userId,
            Integer riskTolerance,
            Integer investmentHorizon) {

        log.info("전략 추천 요청 - 포트폴리오 ID: {}, 위험허용도: {}, 투자기간: {}",
                portfolioId, riskTolerance, investmentHorizon);

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));

        double portfolioValue = portfolio.getTotalValue() != null
                ? portfolio.getTotalValue().doubleValue()
                : 0.0;

        RebalancingStrategy recommendedStrategy = strategyFactory.getRecommendedStrategy(
                portfolioValue,
                riskTolerance != null ? riskTolerance : 3,
                investmentHorizon != null ? investmentHorizon : 36
        );

        Map<String, Object> result = new HashMap<>();
        result.put("recommendedStrategy", recommendedStrategy.getStrategyName());
        result.put("strategyDescription", recommendedStrategy.getDescription());
        result.put("portfolioValue", portfolioValue);
        result.put("allStrategies", strategyFactory.getStrategyInfos());

        return result;
    }

    /**
     * 리밸런싱 필요 여부 확인
     *
     * @param portfolioId 포트폴리오 ID
     * @param userId 사용자 ID
     * @param targetAllocation 목표 자산 배분
     * @param strategyName 전략명
     * @return 리밸런싱 필요 여부
     */
    public boolean isRebalancingNeeded(
            Long portfolioId,
            Long userId,
            Map<String, Double> targetAllocation,
            String strategyName) {

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));

        validateTargetAllocation(targetAllocation);

        RebalancingStrategy strategy = strategyFactory.getStrategy(strategyName);

        boolean needed = strategy.needsRebalancing(portfolio, targetAllocation);

        log.info("리밸런싱 필요 여부 확인 - 포트폴리오 ID: {}, 필요: {}", portfolioId, needed);

        return needed;
    }

    /**
     * 모든 사용 가능한 전략 정보 조회
     *
     * @return 전략 정보 맵
     */
    public Map<String, RebalancingStrategyFactory.StrategyInfo> getAllStrategyInfos() {
        return strategyFactory.getStrategyInfos();
    }

    /**
     * 빠른 리밸런싱 분석 (간단한 편차 정보만)
     *
     * @param portfolioId 포트폴리오 ID
     * @param userId 사용자 ID
     * @param targetAllocation 목표 자산 배분
     * @return 간단한 분석 결과
     */
    public Map<String, Object> getQuickAnalysis(
            Long portfolioId,
            Long userId,
            Map<String, Double> targetAllocation) {

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));

        validateTargetAllocation(targetAllocation);

        // 현재 배분 계산
        Map<String, Double> currentAllocation = calculateCurrentAllocation(portfolio);

        // 편차 계산
        Map<String, Double> deviations = new HashMap<>();
        double maxDeviation = 0.0;
        String maxDeviationSymbol = "";

        for (String symbol : targetAllocation.keySet()) {
            double current = currentAllocation.getOrDefault(symbol, 0.0);
            double target = targetAllocation.get(symbol);
            double deviation = Math.abs(current - target);

            deviations.put(symbol, current - target);

            if (deviation > maxDeviation) {
                maxDeviation = deviation;
                maxDeviationSymbol = symbol;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("currentAllocation", currentAllocation);
        result.put("targetAllocation", targetAllocation);
        result.put("deviations", deviations);
        result.put("maxDeviation", maxDeviation);
        result.put("maxDeviationSymbol", maxDeviationSymbol);
        result.put("needsAttention", maxDeviation > 5.0);

        return result;
    }

    /**
     * 목표 자산 배분 검증
     */
    private void validateTargetAllocation(Map<String, Double> targetAllocation) {
        if (targetAllocation == null || targetAllocation.isEmpty()) {
            throw new IllegalArgumentException("목표 자산 배분이 설정되지 않았습니다.");
        }

        // 총 배분 비율 확인 (100%에 가까워야 함)
        double totalAllocation = targetAllocation.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (Math.abs(totalAllocation - 100.0) > 1.0) { // 1% 오차 허용
            throw new IllegalArgumentException(
                    String.format("목표 자산 배분의 총합이 100%%가 아닙니다. 현재: %.2f%%", totalAllocation));
        }

        // 개별 배분 비율 확인
        for (Map.Entry<String, Double> entry : targetAllocation.entrySet()) {
            if (entry.getValue() < 0 || entry.getValue() > 100) {
                throw new IllegalArgumentException(
                        String.format("잘못된 배분 비율입니다. %s: %.2f%%", entry.getKey(), entry.getValue()));
            }
        }
    }

    /**
     * 현재 자산 배분 계산
     */
    private Map<String, Double> calculateCurrentAllocation(Portfolio portfolio) {
        Map<String, Double> allocation = new HashMap<>();

        if (portfolio.getTotalValue() == null || portfolio.getTotalValue().doubleValue() == 0) {
            return allocation;
        }

        double totalValue = portfolio.getTotalValue().doubleValue();

        portfolio.getHoldings().forEach(holding -> {
            if (holding.getMarketValue() != null) {
                double weight = (holding.getMarketValue().doubleValue() / totalValue) * 100;
                allocation.put(holding.getSymbol(), weight);
            }
        });

        return allocation;
    }
}