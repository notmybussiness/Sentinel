package com.pjsent.sentinel.rebalancing.service;

import com.pjsent.sentinel.common.exception.BusinessException;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.MarketDataService;
import com.pjsent.sentinel.portfolio.dto.PortfolioDto;
import com.pjsent.sentinel.portfolio.dto.PortfolioHoldingDto;
import com.pjsent.sentinel.portfolio.service.PortfolioService;
import com.pjsent.sentinel.pricehistory.dto.ChartDataDto;
import com.pjsent.sentinel.pricehistory.service.PriceHistoryService;
import com.pjsent.sentinel.rebalancing.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 리밸런싱 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RebalancingService {

    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final CryptoDataService cryptoDataService;
    private final PriceHistoryService priceHistoryService;

    private static final Double COMMISSION_RATE = 0.001;  // 0.1%
    private static final Double MIN_COMMISSION = 1.0;
    private static final Double MAX_COMMISSION = 10.0;
    private static final Double TAX_RATE = 0.20;  // 20% capital gains tax

    /**
     * 리밸런싱 추천 생성
     */
    public RebalancingResponse generateRecommendations(Long userId, RebalancingRequest request) {
        log.info("Generating rebalancing recommendations for portfolio {} by user {}", request.getPortfolioId(), userId);

        // 포트폴리오 조회 (PortfolioService를 통해 캡슐화)
        PortfolioDto portfolioDto = portfolioService.getPortfolioById(request.getPortfolioId(), userId);

        // 최소 2개 이상의 holdings 필요
        if (portfolioDto.getHoldings().size() < 2) {
            throw new BusinessException("Portfolio must have at least 2 holdings for rebalancing");
        }

        // 전략 검증
        if (!request.getStrategy().isSupported()) {
            throw new BusinessException("Strategy " + request.getStrategy() + " is not yet supported");
        }

        // 현재 포트폴리오 가치 계산
        double currentValue = calculatePortfolioValue(portfolioDto);

        // 리밸런싱 추천 생성
        List<RebalancingRecommendation> recommendations = new ArrayList<>();
        double totalTransactionCost = 0.0;
        double estimatedTaxImpact = 0.0;
        boolean needsRebalancing = false;

        // 전략별 목표 비중 계산
        Map<String, Double> targetWeights = calculateTargetWeights(request, portfolioDto);

        // 각 holding에 대해 리밸런싱 추천 생성
        for (PortfolioHoldingDto holding : portfolioDto.getHoldings()) {
            double targetWeight = targetWeights.getOrDefault(holding.getSymbol(), 0.0);

            RebalancingRecommendation recommendation = calculateRecommendation(
                    holding, currentValue, targetWeight, request.getThresholdPercent()
            );

            recommendations.add(recommendation);

            if (!recommendation.getAction().equals("HOLD")) {
                needsRebalancing = true;
                totalTransactionCost += calculateCommission(recommendation.getEstimatedAmount());

                // 세금 영향 계산 (매도 시)
                if (request.getConsiderTaxes() && recommendation.getAction().equals("SELL")) {
                    double unrealizedGainLoss = holding.getGainLoss().doubleValue();
                    if (unrealizedGainLoss > 0) {
                        estimatedTaxImpact += unrealizedGainLoss * TAX_RATE;
                    }
                }
            }
        }

        return RebalancingResponse.builder()
                .portfolioId(portfolioDto.getId())
                .portfolioName(portfolioDto.getName())
                .strategy(request.getStrategy().name())
                .currentValue(currentValue)
                .needsRebalancing(needsRebalancing)
                .recommendations(recommendations)
                .totalTransactionCost(totalTransactionCost)
                .estimatedTaxImpact(estimatedTaxImpact)
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 전략별 목표 비중 계산
     */
    private Map<String, Double> calculateTargetWeights(RebalancingRequest request, PortfolioDto portfolio) {
        return switch (request.getStrategy()) {
            case EQUAL_WEIGHT -> calculateEqualWeights(portfolio);
            case TARGET_ALLOCATION -> calculateTargetAllocationWeights(request, portfolio);
            case RISK_PARITY -> calculateRiskParityWeights(portfolio, request.getLookbackDays());
            case MOMENTUM -> calculateMomentumWeights(portfolio, request.getLookbackDays());
            case DIVIDEND_FOCUSED -> throw new BusinessException("DIVIDEND_FOCUSED strategy is not yet implemented");
        };
    }

    /**
     * EQUAL_WEIGHT: 균등 비중 계산
     */
    private Map<String, Double> calculateEqualWeights(PortfolioDto portfolio) {
        int count = portfolio.getHoldings().size();
        double weight = 100.0 / count;
        return portfolio.getHoldings().stream()
                .collect(Collectors.toMap(
                        PortfolioHoldingDto::getSymbol,
                        h -> weight
                ));
    }

    /**
     * TARGET_ALLOCATION: 사용자 지정 목표 비중
     */
    private Map<String, Double> calculateTargetAllocationWeights(RebalancingRequest request, PortfolioDto portfolio) {
        if (request.getTargetAllocations() == null || request.getTargetAllocations().isEmpty()) {
            throw new BusinessException("targetAllocations is required for TARGET_ALLOCATION strategy");
        }

        // 비중 합계 검증 (100% ±1% 허용)
        double totalWeight = request.getTargetAllocations().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        if (Math.abs(totalWeight - 100.0) > 1.0) {
            throw new BusinessException("Target allocations must sum to 100%, current sum: " + totalWeight);
        }

        // 포트폴리오에 있는 종목만 필터링
        Set<String> portfolioSymbols = portfolio.getHoldings().stream()
                .map(PortfolioHoldingDto::getSymbol)
                .collect(Collectors.toSet());

        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Double> entry : request.getTargetAllocations().entrySet()) {
            if (portfolioSymbols.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                log.warn("Symbol {} in targetAllocations not found in portfolio, ignoring", entry.getKey());
            }
        }

        return result;
    }

    /**
     * RISK_PARITY: 변동성 역가중 (위험 균등)
     * 변동성이 낮은 자산에 더 높은 비중 부여
     */
    private Map<String, Double> calculateRiskParityWeights(PortfolioDto portfolio, int lookbackDays) {
        Map<String, Double> volatilities = new HashMap<>();
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(lookbackDays);

        // 각 자산의 변동성 계산
        for (PortfolioHoldingDto holding : portfolio.getHoldings()) {
            try {
                ChartDataDto chartData = priceHistoryService.getChartData(
                        holding.getSymbol(),
                        startTime,
                        endTime
                );

                double volatility = calculateVolatilityFromChart(chartData);
                volatilities.put(holding.getSymbol(), volatility);
                log.debug("Volatility for {}: {}", holding.getSymbol(), volatility);
            } catch (Exception e) {
                log.warn("Failed to calculate volatility for {}, using default", holding.getSymbol());
                volatilities.put(holding.getSymbol(), 0.3);  // 기본 30% 변동성
            }
        }

        // 변동성 역가중치 계산 (1/volatility)
        Map<String, Double> inverseVolatilities = new HashMap<>();
        double sumInverseVol = 0.0;

        for (Map.Entry<String, Double> entry : volatilities.entrySet()) {
            double vol = Math.max(entry.getValue(), 0.01);  // 0으로 나누기 방지
            double inverseVol = 1.0 / vol;
            inverseVolatilities.put(entry.getKey(), inverseVol);
            sumInverseVol += inverseVol;
        }

        // 비중 정규화 (합계 100%)
        Map<String, Double> weights = new HashMap<>();
        for (Map.Entry<String, Double> entry : inverseVolatilities.entrySet()) {
            double weight = (entry.getValue() / sumInverseVol) * 100.0;
            weights.put(entry.getKey(), weight);
        }

        return weights;
    }

    /**
     * MOMENTUM: 최근 수익률 기반 가중치
     * 수익률이 높은 자산에 더 높은 비중 부여
     */
    private Map<String, Double> calculateMomentumWeights(PortfolioDto portfolio, int lookbackDays) {
        Map<String, Double> returns = new HashMap<>();
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(lookbackDays);

        // 각 자산의 수익률 계산
        for (PortfolioHoldingDto holding : portfolio.getHoldings()) {
            try {
                ChartDataDto chartData = priceHistoryService.getChartData(
                        holding.getSymbol(),
                        startTime,
                        endTime
                );

                double momentumReturn = calculateReturnFromChart(chartData);
                returns.put(holding.getSymbol(), momentumReturn);
                log.debug("Momentum return for {}: {}%", holding.getSymbol(), momentumReturn * 100);
            } catch (Exception e) {
                log.warn("Failed to calculate momentum for {}, using 0%", holding.getSymbol());
                returns.put(holding.getSymbol(), 0.0);
            }
        }

        // 음수 수익률 처리: 최소값을 0으로 시프트
        double minReturn = returns.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        Map<String, Double> adjustedReturns = new HashMap<>();
        double sumAdjusted = 0.0;

        for (Map.Entry<String, Double> entry : returns.entrySet()) {
            double adjusted = entry.getValue() - minReturn + 0.01;  // 최소 0.01 보장
            adjustedReturns.put(entry.getKey(), adjusted);
            sumAdjusted += adjusted;
        }

        // 비중 정규화 (합계 100%)
        Map<String, Double> weights = new HashMap<>();
        for (Map.Entry<String, Double> entry : adjustedReturns.entrySet()) {
            double weight = (entry.getValue() / sumAdjusted) * 100.0;
            weights.put(entry.getKey(), weight);
        }

        return weights;
    }

    /**
     * ChartDataDto로부터 변동성 (표준편차) 계산
     */
    private double calculateVolatilityFromChart(ChartDataDto chartData) {
        if (chartData == null || chartData.getData() == null || chartData.getData().size() < 2) {
            return 0.3;  // 기본 변동성
        }

        List<ChartDataDto.DataPoint> data = chartData.getData();

        // 일간 수익률 계산
        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            double prevPrice = data.get(i - 1).getValue().doubleValue();
            double currPrice = data.get(i).getValue().doubleValue();
            if (prevPrice > 0) {
                dailyReturns.add((currPrice - prevPrice) / prevPrice);
            }
        }

        if (dailyReturns.isEmpty()) {
            return 0.3;
        }

        // 표준편차 계산
        double mean = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = dailyReturns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0.0);

        // 연환산 변동성 (일간 변동성 * sqrt(252))
        return Math.sqrt(variance) * Math.sqrt(252);
    }

    /**
     * ChartDataDto로부터 기간 수익률 계산
     */
    private double calculateReturnFromChart(ChartDataDto chartData) {
        if (chartData == null || chartData.getData() == null || chartData.getData().size() < 2) {
            return 0.0;
        }

        List<ChartDataDto.DataPoint> data = chartData.getData();
        double startPrice = data.get(0).getValue().doubleValue();
        double endPrice = data.get(data.size() - 1).getValue().doubleValue();

        if (startPrice <= 0) {
            return 0.0;
        }

        return (endPrice - startPrice) / startPrice;
    }

    /**
     * 리밸런싱 추천 계산 (공통)
     */
    private RebalancingRecommendation calculateRecommendation(
            PortfolioHoldingDto holding,
            double portfolioValue,
            double targetWeight,
            double thresholdPercent
    ) {
        // 현재 가격 조회
        double currentPrice = getCurrentPrice(holding);

        // 현재 비중
        double currentValue = holding.getMarketValue().doubleValue();
        double currentWeight = (currentValue / portfolioValue) * 100.0;

        // 목표 가치
        double targetValue = portfolioValue * targetWeight / 100.0;

        // 목표 주식 수
        double targetShares = targetValue / currentPrice;

        // 거래 수량
        double currentQuantity = holding.getQuantity().doubleValue();
        double quantity = Math.abs(targetShares - currentQuantity);

        // 액션 결정
        String action;
        if (Math.abs(currentWeight - targetWeight) <= thresholdPercent) {
            action = "HOLD";
            quantity = 0.0;
        } else if (targetShares > currentQuantity) {
            action = "BUY";
        } else {
            action = "SELL";
        }

        // 거래 금액
        double estimatedAmount = quantity * currentPrice;

        return RebalancingRecommendation.builder()
                .symbol(holding.getSymbol())
                .currentWeight(currentWeight)
                .targetWeight(targetWeight)
                .currentShares(currentQuantity)
                .targetShares(targetShares)
                .action(action)
                .quantity(quantity)
                .estimatedAmount(estimatedAmount)
                .build();
    }

    /**
     * 리밸런싱 시뮬레이션
     */
    public RebalancingSimulationResponse simulateRebalancing(Long userId, RebalancingRequest request) {
        log.info("Simulating rebalancing for portfolio {} by user {}", request.getPortfolioId(), userId);

        // 포트폴리오 조회 (PortfolioService를 통해 캡슐화)
        PortfolioDto portfolioDto = portfolioService.getPortfolioById(request.getPortfolioId(), userId);

        // 리밸런싱 추천 생성
        RebalancingResponse recommendations = generateRecommendations(userId, request);

        // 시뮬레이션 전 상태
        PortfolioState beforeState = buildPortfolioState(portfolioDto);

        // 거래 내역 생성
        List<Transaction> transactions = new ArrayList<>();
        double totalTransactionCost = 0.0;

        for (RebalancingRecommendation rec : recommendations.getRecommendations()) {
            if (!"HOLD".equals(rec.getAction())) {
                double commission = calculateCommission(rec.getEstimatedAmount());
                totalTransactionCost += commission;

                transactions.add(Transaction.builder()
                        .symbol(rec.getSymbol())
                        .action(rec.getAction())
                        .quantity(rec.getQuantity())
                        .price(rec.getEstimatedAmount() / rec.getQuantity())
                        .amount(rec.getEstimatedAmount())
                        .commission(commission)
                        .build());
            }
        }

        // 시뮬레이션 후 상태 생성
        PortfolioState afterState = buildAfterRebalancingState(
                portfolioDto, recommendations.getRecommendations()
        );

        return RebalancingSimulationResponse.builder()
                .portfolioId(portfolioDto.getId())
                .portfolioName(portfolioDto.getName())
                .beforeRebalancing(beforeState)
                .afterRebalancing(afterState)
                .transactions(transactions)
                .totalTransactionCost(totalTransactionCost)
                .netChange(-totalTransactionCost)
                .build();
    }

    /**
     * 포트폴리오 상태 생성
     */
    private PortfolioState buildPortfolioState(PortfolioDto portfolioDto) {
        double totalValue = calculatePortfolioValue(portfolioDto);

        List<PortfolioState.HoldingState> holdingStates = new ArrayList<>();
        for (PortfolioHoldingDto holding : portfolioDto.getHoldings()) {
            double marketValue = holding.getMarketValue().doubleValue();
            double weight = (marketValue / totalValue) * 100.0;

            holdingStates.add(PortfolioState.HoldingState.builder()
                    .symbol(holding.getSymbol())
                    .shares(holding.getQuantity().doubleValue())
                    .value(marketValue)
                    .weight(weight)
                    .build());
        }

        return PortfolioState.builder()
                .totalValue(totalValue)
                .holdings(holdingStates)
                .build();
    }

    /**
     * 리밸런싱 후 포트폴리오 상태 생성
     */
    private PortfolioState buildAfterRebalancingState(
            PortfolioDto portfolioDto,
            List<RebalancingRecommendation> recommendations
    ) {
        double totalValue = calculatePortfolioValue(portfolioDto);

        List<PortfolioState.HoldingState> holdingStates = new ArrayList<>();
        for (RebalancingRecommendation rec : recommendations) {
            double newShares = rec.getTargetShares();
            double currentPrice = getCurrentPriceBySymbol(portfolioDto, rec.getSymbol());
            double newValue = newShares * currentPrice;
            double newWeight = rec.getTargetWeight();

            holdingStates.add(PortfolioState.HoldingState.builder()
                    .symbol(rec.getSymbol())
                    .shares(newShares)
                    .value(newValue)
                    .weight(newWeight)
                    .build());
        }

        return PortfolioState.builder()
                .totalValue(totalValue)
                .holdings(holdingStates)
                .build();
    }

    /**
     * 현재 가격 조회 (holding 기준)
     */
    private double getCurrentPrice(PortfolioHoldingDto holding) {
        try {
            if ("CRYPTO".equals(holding.getAssetType())) {
                var cryptoPrice = cryptoDataService.getCryptoPrice(
                        holding.getSymbol(),
                        holding.getBaseCurrency()
                );
                return cryptoPrice.getPrice();
            } else {
                StockPriceDto stockPrice = marketDataService.getStockPrice(holding.getSymbol());
                return stockPrice.getPrice();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch current price for {}, using average cost", holding.getSymbol());
            return holding.getAverageCost().doubleValue();
        }
    }

    /**
     * 현재 가격 조회 (symbol 기준)
     */
    private double getCurrentPriceBySymbol(PortfolioDto portfolioDto, String symbol) {
        return portfolioDto.getHoldings().stream()
                .filter(h -> h.getSymbol().equals(symbol))
                .findFirst()
                .map(this::getCurrentPrice)
                .orElse(0.0);
    }

    /**
     * 포트폴리오 가치 계산
     */
    private double calculatePortfolioValue(PortfolioDto portfolioDto) {
        return portfolioDto.getHoldings().stream()
                .mapToDouble(holding -> holding.getMarketValue().doubleValue())
                .sum();
    }

    /**
     * 거래 수수료 계산
     */
    private double calculateCommission(double amount) {
        double commission = amount * COMMISSION_RATE;
        return Math.max(MIN_COMMISSION, Math.min(MAX_COMMISSION, commission));
    }
}
