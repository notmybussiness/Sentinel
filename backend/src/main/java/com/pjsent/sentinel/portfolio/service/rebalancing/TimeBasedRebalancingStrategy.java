package com.pjsent.sentinel.portfolio.service.rebalancing;

import com.pjsent.sentinel.portfolio.dto.RebalancingRecommendationDto;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 시간 기반 리밸런싱 전략
 * 정해진 시간 간격(월별, 분기별, 연별)에 따라 리밸런싱을 수행하는 전략
 *
 * 특징:
 * - 예측 가능한 리밸런싱 일정
 * - 감정적 투자 결정 방지
 * - 간단한 관리
 * - 거래 빈도 조절 가능
 */
@Slf4j
@Component
public class TimeBasedRebalancingStrategy implements RebalancingStrategy {

    /**
     * 기본 리밸런싱 주기 (분기별 = 3개월)
     */
    private static final long DEFAULT_REBALANCING_PERIOD_MONTHS = 3;

    /**
     * 최소 편차 임계값 (시간이 되어도 편차가 작으면 리밸런싱하지 않음)
     */
    private static final double MIN_DEVIATION_THRESHOLD = 2.0;

    /**
     * 최소 거래 금액
     */
    private static final BigDecimal MIN_TRADE_AMOUNT = BigDecimal.valueOf(10000);

    @Override
    public String getStrategyName() {
        return "TIME_BASED";
    }

    @Override
    public String getDescription() {
        return "정해진 시간 간격(기본 3개월)에 따라 정기적으로 리밸런싱을 수행합니다. " +
               "예측 가능한 일정으로 감정적 투자 결정을 방지하며, 장기 투자자에게 적합합니다.";
    }

    @Override
    public boolean needsRebalancing(Portfolio portfolio, Map<String, Double> targetAllocation) {
        // 마지막 업데이트 시간 확인
        LocalDateTime lastUpdate = portfolio.getUpdatedAt();
        LocalDateTime now = LocalDateTime.now();

        long monthsSinceLastUpdate = ChronoUnit.MONTHS.between(lastUpdate, now);

        // 리밸런싱 주기가 도래했는지 확인
        if (monthsSinceLastUpdate >= DEFAULT_REBALANCING_PERIOD_MONTHS) {
            // 추가로 최소 편차 임계값 확인 (너무 작은 편차는 무시)
            Map<String, Double> currentAllocation = calculateCurrentAllocation(portfolio);
            double maxDeviation = calculateMaxDeviation(currentAllocation, targetAllocation);

            if (maxDeviation > MIN_DEVIATION_THRESHOLD) {
                log.info("시간 기반 리밸런싱 필요: 마지막 업데이트로부터 {}개월 경과, 최대 편차: {}%",
                        monthsSinceLastUpdate, String.format("%.2f", maxDeviation));
                return true;
            } else {
                log.info("시간 기간은 경과했지만 편차가 미미하여 리밸런싱 불필요: 최대 편차 {}%",
                        String.format("%.2f", maxDeviation));
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
                .sum() / 2;

        // 마지막 업데이트로부터 경과 시간
        long monthsSinceLastUpdate = ChronoUnit.MONTHS.between(portfolio.getUpdatedAt(), LocalDateTime.now());

        // 리밸런싱 액션 생성
        List<RebalancingRecommendationDto.RebalancingActionDto> actions =
                generateRebalancingActions(portfolio, currentAllocation, targetAllocation, deviations);

        // 거래 비용 추정
        BigDecimal estimatedTransactionCost = estimateTransactionCost(actions);

        // 다음 리밸런싱 예정일
        LocalDateTime nextRebalancingDate = LocalDateTime.now().plusMonths(DEFAULT_REBALANCING_PERIOD_MONTHS);

        // 우선순위 (시간 기반이므로 중간 우선순위)
        int priority = 3;

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
                .nextReviewDate(nextRebalancingDate)
                .strategyDetails(Map.of(
                        "rebalancingPeriodMonths", DEFAULT_REBALANCING_PERIOD_MONTHS,
                        "monthsSinceLastUpdate", monthsSinceLastUpdate,
                        "minDeviationThreshold", MIN_DEVIATION_THRESHOLD,
                        "strategy", "time_based"
                ))
                .priority(priority)
                .notes(generateNotes(monthsSinceLastUpdate, totalDeviation, actions.size()))
                .build();
    }

    @Override
    public boolean validateConfiguration(Map<String, Object> configuration) {
        try {
            // 리밸런싱 주기 검증
            if (configuration.containsKey("rebalancingPeriodMonths")) {
                long period = Long.parseLong(configuration.get("rebalancingPeriodMonths").toString());
                if (period < 1 || period > 60) { // 1개월 ~ 5년
                    return false;
                }
            }

            // 최소 편차 임계값 검증
            if (configuration.containsKey("minDeviationThreshold")) {
                double threshold = Double.parseDouble(configuration.get("minDeviationThreshold").toString());
                if (threshold < 0 || threshold > 10) {
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
     * 최대 편차 계산
     */
    private double calculateMaxDeviation(Map<String, Double> currentAllocation,
                                        Map<String, Double> targetAllocation) {
        return calculateDeviations(currentAllocation, targetAllocation)
                .values()
                .stream()
                .mapToDouble(Math::abs)
                .max()
                .orElse(0.0);
    }

    /**
     * 리밸런싱 액션 생성 (모든 자산을 목표 배분으로 조정)
     */
    private List<RebalancingRecommendationDto.RebalancingActionDto> generateRebalancingActions(
            Portfolio portfolio, Map<String, Double> currentAllocation,
            Map<String, Double> targetAllocation, Map<String, Double> deviations) {

        List<RebalancingRecommendationDto.RebalancingActionDto> actions = new ArrayList<>();

        for (String symbol : targetAllocation.keySet()) {
            double deviation = deviations.getOrDefault(symbol, 0.0);

            // 시간 기반이므로 최소 편차 이상인 모든 자산 조정
            if (Math.abs(deviation) > MIN_DEVIATION_THRESHOLD) {
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
                    continue;
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
                        .priority(2) // 시간 기반이므로 높은 우선순위
                        .build());
            }
        }

        // 거래 금액 순으로 정렬 (큰 거래부터)
        return actions.stream()
                .sorted(Comparator.comparing(
                        action -> action.getEstimatedAmount(),
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * 거래 비용 추정
     */
    private BigDecimal estimateTransactionCost(List<RebalancingRecommendationDto.RebalancingActionDto> actions) {
        double commissionRate = 0.0025; // 0.25%

        return actions.stream()
                .map(action -> action.getEstimatedAmount().multiply(BigDecimal.valueOf(commissionRate)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 추천안 메모 생성
     */
    private String generateNotes(long monthsSinceLastUpdate, double totalDeviation, int actionCount) {
        StringBuilder notes = new StringBuilder();

        notes.append(String.format("마지막 리밸런싱으로부터 %d개월 경과, ", monthsSinceLastUpdate));
        notes.append(String.format("총 편차: %.2f%%, ", totalDeviation));
        notes.append(String.format("조정 대상: %d개 종목", actionCount));

        if (monthsSinceLastUpdate >= DEFAULT_REBALANCING_PERIOD_MONTHS * 2) {
            notes.append(" - 정기 리밸런싱 시기가 많이 지났습니다");
        } else {
            notes.append(" - 정기 리밸런싱 시기입니다");
        }

        return notes.toString();
    }
}