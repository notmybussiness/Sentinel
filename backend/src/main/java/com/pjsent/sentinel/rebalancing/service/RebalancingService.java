package com.pjsent.sentinel.rebalancing.service;

import com.pjsent.sentinel.common.exception.BusinessException;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.MarketDataService;
import com.pjsent.sentinel.portfolio.dto.PortfolioDto;
import com.pjsent.sentinel.portfolio.dto.PortfolioHoldingDto;
import com.pjsent.sentinel.portfolio.service.PortfolioService;
import com.pjsent.sentinel.rebalancing.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private static final Double COMMISSION_RATE = 0.001;  // 0.1%
    private static final Double MIN_COMMISSION = 1.0;
    private static final Double MAX_COMMISSION = 10.0;

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

        // Equal Weight 전략
        if (request.getStrategy() == RebalancingStrategy.EQUAL_WEIGHT) {
            int holdingsCount = portfolioDto.getHoldings().size();
            double targetWeight = 100.0 / holdingsCount;

            for (PortfolioHoldingDto holding : portfolioDto.getHoldings()) {
                RebalancingRecommendation recommendation = calculateEqualWeightRecommendation(
                        holding, currentValue, targetWeight, request.getThresholdPercent()
                );

                recommendations.add(recommendation);

                if (!recommendation.getAction().equals("HOLD")) {
                    needsRebalancing = true;
                    totalTransactionCost += calculateCommission(recommendation.getEstimatedAmount());

                    // 세금 영향 계산 (손실 실현 시)
                    if (request.getConsiderTaxes() && recommendation.getAction().equals("SELL")) {
                        double unrealizedGainLoss = holding.getGainLoss().doubleValue();
                        if (unrealizedGainLoss > 0) {
                            // 이익 실현 시 세금 (단순화: 20%)
                            estimatedTaxImpact += unrealizedGainLoss * 0.2;
                        }
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
     * Equal Weight 리밸런싱 추천 계산
     */
    private RebalancingRecommendation calculateEqualWeightRecommendation(
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
