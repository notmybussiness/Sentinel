package com.pjsent.sentinel.portfolio.service.rebalancing;

import com.pjsent.sentinel.portfolio.dto.RebalancingRecommendationDto;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 임계값 기반 리밸런싱 전략
 * 자산 배분이 목표치에서 설정된 임계값 이상 벗어날 때 리밸런싱을 권장하는 전략
 *
 * 연구 결과에 따르면 가장 효과적인 리밸런싱 전략으로 평가됨
 * - 5% 임계값: 보수적 접근 (거래 빈도 낮음)
 * - 10% 임계값: 균형적 접근 (거래 빈도 중간)
 * - 15% 임계값: 적극적 접근 (거래 빈도 높음)
 */
@Slf4j
@Component
public class ThresholdBasedRebalancingStrategy implements RebalancingStrategy {

    /**
     * 기본 임계값 (5%)
     */
    private static final double DEFAULT_THRESHOLD = 5.0;

    /**
     * 최소 거래 금액 (소액 거래 방지)
     */
    private static final BigDecimal MIN_TRADE_AMOUNT = BigDecimal.valueOf(10000); // 1만원

    @Override
    public String getStrategyName() {
        return "THRESHOLD_BASED";
    }

    @Override
    public String getDescription() {
        return "자산 배분이 목표치에서 설정된 임계값(기본 5%) 이상 벗어날 때 리밸런싱을 권장합니다. " +
               "시장 변동에 즉시 반응하며 거래 비용을 최소화하는 효율적인 전략입니다.";
    }

    @Override
    public boolean needsRebalancing(Portfolio portfolio, Map<String, Double> targetAllocation) {
        if (portfolio.getTotalValue() == null || portfolio.getTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        Map<String, Double> currentAllocation = calculateCurrentAllocation(portfolio);
        double threshold = DEFAULT_THRESHOLD;

        // 각 자산별로 임계값 확인
        for (String symbol : targetAllocation.keySet()) {
            double currentWeight = currentAllocation.getOrDefault(symbol, 0.0);
            double targetWeight = targetAllocation.get(symbol);
            double deviation = Math.abs(currentWeight - targetWeight);

            if (deviation > threshold) {
                log.info("리밸런싱 필요 감지: {} - 현재: {}%, 목표: {}%, 편차: {}%",
                        symbol, String.format("%.2f", currentWeight),
                        String.format("%.2f", targetWeight), String.format("%.2f", deviation));
                return true;
            }
        }

        return false;
    }

    @Override
    public RebalancingRecommendationDto generateRecommendation(Portfolio portfolio, Map<String, Double> targetAllocation) {
        Map<String, Double> currentAllocation = calculateCurrentAllocation(portfolio);
        Map<String, Double> deviations = calculateDeviations(currentAllocation, targetAllocation);

        // 총 편차 계산
        double totalDeviation = deviations.values().stream()
                .mapToDouble(Math::abs)
                .sum() / 2; // 편차 합의 절반 (매수/매도가 상쇄되므로)

        // 리밸런싱 액션 생성
        List<RebalancingRecommendationDto.RebalancingActionDto> actions =
                generateRebalancingActions(portfolio, currentAllocation, targetAllocation, deviations);

        // 거래 비용 추정
        BigDecimal estimatedTransactionCost = estimateTransactionCost(actions);

        // 우선순위 계산 (편차가 클수록 높은 우선순위)
        int priority = calculatePriority(totalDeviation);

        return RebalancingRecommendationDto.builder()
                .recommendationId(UUID.randomUUID().toString())
                .portfolioId(portfolio.getId())
                .strategyName(getStrategyName())
                .rebalancingNeeded(needsRebalancing(portfolio, targetAllocation))
                .totalDeviationPercent(totalDeviation)
                .currentAllocation(currentAllocation)
                .targetAllocation(targetAllocation)
                .deviations(deviations)
                .actions(actions)
                .estimatedTransactionCost(estimatedTransactionCost)
                .createdAt(LocalDateTime.now())
                .nextReviewDate(LocalDateTime.now().plusWeeks(2)) // 2주 후 재검토
                .strategyDetails(Map.of(
                        "threshold", DEFAULT_THRESHOLD,
                        "minTradeAmount", MIN_TRADE_AMOUNT,
                        "strategy", "threshold_based"
                ))
                .priority(priority)
                .notes(generateNotes(totalDeviation, actions.size()))
                .build();
    }

    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        try {
            // 임계값 검증
            if (configuration.containsKey("threshold")) {
                double threshold = Double.parseDouble(configuration.get("threshold").toString());
                if (threshold < 1.0 || threshold > 50.0) {
                    return false;
                }
            }

            // 최소 거래 금액 검증
            if (configuration.containsKey("minTradeAmount")) {
                BigDecimal minAmount = new BigDecimal(configuration.get("minTradeAmount").toString());
                if (minAmount.compareTo(BigDecimal.ZERO) <= 0) {
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
     * 현재 자산 배분 계산
     */
    private Map<String, Double> calculateCurrentAllocation(Portfolio portfolio) {
        Map<String, Double> allocation = new HashMap<>();

        if (portfolio.getTotalValue() == null || portfolio.getTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            return allocation;
        }

        for (PortfolioHolding holding : portfolio.getHoldings()) {
            if (holding.getMarketValue() != null) {
                double weight = holding.getMarketValue()
                        .divide(portfolio.getTotalValue(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                allocation.put(holding.getSymbol(), weight);
            }
        }

        return allocation;
    }

    /**
     * 편차 계산
     */
    private Map<String, Double> calculateDeviations(Map<String, Double> currentAllocation,
                                                    Map<String, Double> targetAllocation) {
        Map<String, Double> deviations = new HashMap<>();

        Set<String> allSymbols = new HashSet<>();
        allSymbols.addAll(currentAllocation.keySet());
        allSymbols.addAll(targetAllocation.keySet());

        for (String symbol : allSymbols) {
            double current = currentAllocation.getOrDefault(symbol, 0.0);
            double target = targetAllocation.getOrDefault(symbol, 0.0);
            deviations.put(symbol, current - target);
        }

        return deviations;
    }

    /**
     * 리밸런싱 액션 생성
     */
    private List<RebalancingRecommendationDto.RebalancingActionDto> generateRebalancingActions(
            Portfolio portfolio, Map<String, Double> currentAllocation,
            Map<String, Double> targetAllocation, Map<String, Double> deviations) {

        List<RebalancingRecommendationDto.RebalancingActionDto> actions = new ArrayList<>();

        for (String symbol : targetAllocation.keySet()) {
            double deviation = deviations.getOrDefault(symbol, 0.0);

            // 임계값 이상의 편차만 처리
            if (Math.abs(deviation) > DEFAULT_THRESHOLD) {
                PortfolioHolding holding = portfolio.getHoldings().stream()
                        .filter(h -> h.getSymbol().equals(symbol))
                        .findFirst()
                        .orElse(null);

                BigDecimal currentQuantity = holding != null ? holding.getQuantity() : BigDecimal.ZERO;
                BigDecimal currentPrice = holding != null ? holding.getCurrentPrice() : BigDecimal.ZERO;

                // 목표 금액 계산
                BigDecimal targetAmount = portfolio.getTotalValue()
                        .multiply(BigDecimal.valueOf(targetAllocation.get(symbol)))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // 목표 수량 계산
                BigDecimal targetQuantity = currentPrice.compareTo(BigDecimal.ZERO) > 0
                        ? targetAmount.divide(currentPrice, 6, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                BigDecimal quantityChange = targetQuantity.subtract(currentQuantity);

                // 최소 거래 금액 검증
                BigDecimal tradeAmount = quantityChange.abs().multiply(currentPrice);
                if (tradeAmount.compareTo(MIN_TRADE_AMOUNT) < 0) {
                    continue; // 소액 거래는 제외
                }

                RebalancingRecommendationDto.ActionType actionType;
                if (quantityChange.compareTo(BigDecimal.ZERO) > 0) {
                    actionType = RebalancingRecommendationDto.ActionType.BUY;
                } else if (quantityChange.compareTo(BigDecimal.ZERO) < 0) {
                    actionType = RebalancingRecommendationDto.ActionType.SELL;
                } else {
                    actionType = RebalancingRecommendationDto.ActionType.HOLD;
                }

                actions.add(RebalancingRecommendationDto.RebalancingActionDto.builder()
                        .actionType(actionType)
                        .symbol(symbol)
                        .currentQuantity(currentQuantity)
                        .targetQuantity(targetQuantity)
                        .quantityChange(quantityChange)
                        .currentPrice(currentPrice)
                        .estimatedAmount(tradeAmount)
                        .currentWeight(currentAllocation.getOrDefault(symbol, 0.0))
                        .targetWeight(targetAllocation.get(symbol))
                        .deviation(deviation)
                        .priority(calculateActionPriority(Math.abs(deviation)))
                        .build());
            }
        }

        // 편차 크기 순으로 정렬
        return actions.stream()
                .sorted(Comparator.comparing(action -> Math.abs(action.getDeviation()), Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * 거래 비용 추정
     */
    private BigDecimal estimateTransactionCost(List<RebalancingRecommendationDto.RebalancingActionDto> actions) {
        // 기본 거래 수수료: 0.25%
        double commissionRate = 0.0025;

        return actions.stream()
                .map(action -> action.getEstimatedAmount().multiply(BigDecimal.valueOf(commissionRate)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 전체 우선순위 계산
     */
    private int calculatePriority(double totalDeviation) {
        if (totalDeviation > 20) return 1; // 매우 높음
        if (totalDeviation > 15) return 2; // 높음
        if (totalDeviation > 10) return 3; // 중간
        if (totalDeviation > 5) return 4;  // 낮음
        return 5; // 매우 낮음
    }

    /**
     * 액션별 우선순위 계산
     */
    private int calculateActionPriority(double deviation) {
        if (deviation > 15) return 1; // 매우 높음
        if (deviation > 10) return 2; // 높음
        if (deviation > 7) return 3;  // 중간
        if (deviation > 5) return 4;  // 낮음
        return 5; // 매우 낮음
    }

    /**
     * 추천안 메모 생성
     */
    private String generateNotes(double totalDeviation, int actionCount) {
        StringBuilder notes = new StringBuilder();

        notes.append(String.format("총 편차: %.2f%%, ", totalDeviation));
        notes.append(String.format("조정 대상: %d개 종목", actionCount));

        if (totalDeviation > 15) {
            notes.append(" - 즉시 리밸런싱 권장");
        } else if (totalDeviation > 10) {
            notes.append(" - 리밸런싱 검토 필요");
        } else {
            notes.append(" - 선택적 리밸런싱");
        }

        return notes.toString();
    }
}