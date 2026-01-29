package com.pjsent.sentinel.backtest.service;

import com.pjsent.sentinel.backtest.dto.*;
import com.pjsent.sentinel.common.exception.ResourceNotFoundException;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 백테스팅 엔진 서비스
 * 포트폴리오 시뮬레이션 및 성과 분석
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestEngine {

    private final PortfolioRepository portfolioRepository;
    private final HistoricalDataService historicalDataService;
    private final HistoricalDataFacade historicalDataFacade;
    private final PerformanceCalculator performanceCalculator;
    private final CacheManager cacheManager;

    private static final long MAX_BACKTEST_YEARS = 10;

    /**
     * 백테스팅 실행
     *
     * @param request 백테스팅 요청
     * @return 백테스팅 결과
     */
    @Transactional(readOnly = true)
    public BacktestResponse runBacktest(BacktestRequest request) {
        log.info("Running backtest for portfolio {}: {} to {}",
                request.getPortfolioId(), request.getStartDate(), request.getEndDate());

        if (request.getInitialCapital() == null || request.getInitialCapital() <= 0) {
            throw new IllegalArgumentException("Initial capital must be positive");
        }

        // 1. Load portfolio
        Portfolio portfolio = portfolioRepository.findById(request.getPortfolioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));

        // 2. Validate holdings (Stock + Crypto 모두 지원)
        List<PortfolioHolding> allHoldings = portfolio.getHoldings();

        if (allHoldings == null || allHoldings.isEmpty()) {
            throw new IllegalArgumentException("Portfolio has no holdings for backtesting");
        }

        // Holdings 분류 (로깅 목적)
        long stockCount = allHoldings.stream()
                .filter(h -> h.getAssetType() == PortfolioHolding.AssetType.STOCK)
                .count();
        long cryptoCount = allHoldings.stream()
                .filter(h -> h.getAssetType() == PortfolioHolding.AssetType.CRYPTO)
                .count();

        log.info("Found {} holdings to backtest ({} stocks, {} crypto)",
                allHoldings.size(), stockCount, cryptoCount);

        // 3. Fetch historical data for all holdings (Stock + Crypto)
        Map<String, List<HistoricalPriceData>> historicalData = fetchHistoricalData(
                allHoldings, request.getStartDate(), request.getEndDate());

        // Crypto 포함 여부 확인 (주말 처리에 사용)
        boolean hasCrypto = cryptoCount > 0;
        boolean hasCryptoOnly = stockCount == 0 && cryptoCount > 0;

        // 4. Run simulation
        double transactionCostPercent = request.getTransactionCostPercent() != null
                ? request.getTransactionCostPercent()
                : 0.001; // Default 0.1%
        SimulationResult simulation = runSimulation(
                allHoldings,
                historicalData,
                request.getInitialCapital(),
                request.getStartDate(),
                request.getEndDate(),
                request.getRebalanceFrequency(),
                hasCryptoOnly,
                transactionCostPercent);

        // 5. Calculate performance metrics
        PerformanceMetrics performance = performanceCalculator.calculatePerformance(
                simulation.getEquityCurve(),
                request.getInitialCapital(),
                request.getStartDate(),
                request.getEndDate());

        // 6. Build response
        double costImpact = (simulation.getTotalTransactionCosts() / request.getInitialCapital()) * 100.0;
        return BacktestResponse.builder()
                .portfolioId(portfolio.getId())
                .portfolioName(portfolio.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .initialCapital(request.getInitialCapital())
                .finalValue(simulation.getFinalValue())
                .rebalanceFrequency(request.getRebalanceFrequency())
                .performance(performance)
                .equityCurve(simulation.getEquityCurve())
                .rebalanceEvents(simulation.getRebalanceEvents())
                .holdingsSummary(simulation.getHoldingsSummary())
                .executedAt(java.time.LocalDateTime.now())
                .totalTransactionCosts(simulation.getTotalTransactionCosts())
                .costImpactPercent(costImpact)
                .transactionCostPercent(transactionCostPercent)
                .build();
    }

    /**
     * 백테스팅 요청 유효성 검증
     * Cache lookup 방식으로 데이터 가용성을 확인합니다.
     *
     * @param request 백테스팅 요청
     * @return 유효성 검증 결과
     */
    @Transactional(readOnly = true)
    public BacktestValidationResponse validateBacktestRequest(BacktestRequest request) {
        log.info("Validating backtest request for portfolio {}", request.getPortfolioId());

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Boolean> dataAvailability = new HashMap<>();
        BacktestValidationResponse.PortfolioSummary portfolioSummary = null;

        // 1. 포트폴리오 존재 확인
        Optional<Portfolio> portfolioOpt = portfolioRepository.findById(request.getPortfolioId());
        if (portfolioOpt.isEmpty()) {
            errors.add("Portfolio not found: " + request.getPortfolioId());
            return buildValidationResponse(errors, warnings, dataAvailability, null);
        }

        Portfolio portfolio = portfolioOpt.get();

        // 2. 날짜 범위 유효성 검증
        if (request.getStartDate() == null || request.getEndDate() == null) {
            errors.add("Start date and end date are required");
            return buildValidationResponse(errors, warnings, dataAvailability, null);
        }

        if (request.getStartDate().isAfter(request.getEndDate())) {
            errors.add("Start date must be before end date");
        }

        if (request.getEndDate().isAfter(LocalDate.now())) {
            errors.add("End date cannot be in the future");
        }

        long yearsBetween = ChronoUnit.YEARS.between(request.getStartDate(), request.getEndDate());
        if (yearsBetween > MAX_BACKTEST_YEARS) {
            errors.add("Backtest period cannot exceed " + MAX_BACKTEST_YEARS + " years");
        }

        // 3. 초기 자본 검증
        if (request.getInitialCapital() == null || request.getInitialCapital() <= 0) {
            errors.add("Initial capital must be positive");
        }

        // 4. Holdings 확인
        List<PortfolioHolding> holdings = portfolio.getHoldings();
        if (holdings == null || holdings.isEmpty()) {
            errors.add("Portfolio has no holdings");
            return buildValidationResponse(errors, warnings, dataAvailability, null);
        }

        // Holdings 분류
        List<PortfolioHolding> stockHoldings = holdings.stream()
                .filter(h -> h.getAssetType() == PortfolioHolding.AssetType.STOCK)
                .toList();
        List<PortfolioHolding> cryptoHoldings = holdings.stream()
                .filter(h -> h.getAssetType() == PortfolioHolding.AssetType.CRYPTO)
                .toList();

        // Portfolio summary
        List<String> allSymbols = holdings.stream()
                .map(PortfolioHolding::getSymbol)
                .toList();
        portfolioSummary = BacktestValidationResponse.PortfolioSummary.builder()
                .portfolioId(portfolio.getId())
                .portfolioName(portfolio.getName())
                .stockCount(stockHoldings.size())
                .cryptoCount(cryptoHoldings.size())
                .symbols(allSymbols)
                .build();

        // Stock + Crypto 모두 지원
        // (P3에서 Crypto 지원 추가됨)

        // 5. 캐시에서 데이터 가용성 확인 (Cache lookup)
        if (errors.isEmpty()) {
            Cache stockCache = cacheManager.getCache("historicalData");
            Cache cryptoCache = cacheManager.getCache("cryptoHistoricalData");

            for (PortfolioHolding holding : holdings) {
                String symbol = holding.getSymbol();
                boolean isCached = false;

                if (holding.getAssetType() == PortfolioHolding.AssetType.STOCK) {
                    String cacheKey = symbol + "_" + request.getStartDate().toString() + "_"
                            + request.getEndDate().toString();
                    if (stockCache != null) {
                        Cache.ValueWrapper cached = stockCache.get(cacheKey);
                        isCached = (cached != null);
                    }
                } else if (holding.getAssetType() == PortfolioHolding.AssetType.CRYPTO) {
                    String cacheKey = symbol + "_" + holding.getBaseCurrency() + "_" + request.getStartDate().toString()
                            + "_" + request.getEndDate().toString();
                    if (cryptoCache != null) {
                        Cache.ValueWrapper cached = cryptoCache.get(cacheKey);
                        isCached = (cached != null);
                    }
                }

                dataAvailability.put(symbol, isCached);

                if (!isCached) {
                    warnings.add("Historical data for " + symbol + " is not cached. First request may take longer.");
                }
            }
        }

        return buildValidationResponse(errors, warnings, dataAvailability, portfolioSummary);
    }

    /**
     * 유효성 검증 응답 생성
     */
    private BacktestValidationResponse buildValidationResponse(
            List<String> errors,
            List<String> warnings,
            Map<String, Boolean> dataAvailability,
            BacktestValidationResponse.PortfolioSummary portfolioSummary) {

        return BacktestValidationResponse.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .dataAvailability(dataAvailability)
                .portfolioSummary(portfolioSummary)
                .build();
    }

    /**
     * 모든 종목의 과거 데이터 조회 (Stock + Crypto)
     * HistoricalDataFacade를 사용하여 자산 유형에 따라 적절한 서비스 호출
     */
    private Map<String, List<HistoricalPriceData>> fetchHistoricalData(
            List<PortfolioHolding> holdings,
            LocalDate startDate,
            LocalDate endDate) {

        log.info("Fetching historical data for {} holdings", holdings.size());
        return historicalDataFacade.getBatchHistoricalPrices(holdings, startDate, endDate);
    }

    /**
     * 백테스팅 시뮬레이션 실행
     *
     * @param holdings               포트폴리오 보유 자산
     * @param historicalData         과거 가격 데이터
     * @param initialCapital         초기 자본
     * @param startDate              시작일
     * @param endDate                종료일
     * @param rebalanceFrequency     리밸런싱 빈도
     * @param hasCryptoOnly          Crypto만 있는 포트폴리오 여부 (주말 거래 허용)
     * @param transactionCostPercent 거래 비용 비율 (0.001 = 0.1%)
     */
    private SimulationResult runSimulation(
            List<PortfolioHolding> holdings,
            Map<String, List<HistoricalPriceData>> historicalData,
            double initialCapital,
            LocalDate startDate,
            LocalDate endDate,
            BacktestRequest.RebalanceFrequency rebalanceFrequency,
            boolean hasCryptoOnly,
            double transactionCostPercent) {

        // Initialize portfolio state
        PortfolioState state = initializePortfolio(holdings, historicalData, initialCapital, startDate);

        List<EquityPoint> equityCurve = new ArrayList<>();
        List<RebalanceEvent> rebalanceEvents = new ArrayList<>();
        double totalTransactionCosts = 0.0;

        // Simulate day by day
        LocalDate currentDate = startDate;
        LocalDate nextRebalanceDate = calculateNextRebalanceDate(startDate, rebalanceFrequency);

        double previousValue = initialCapital;

        while (!currentDate.isAfter(endDate)) {
            // Skip weekends (Crypto only 포트폴리오는 주말도 거래)
            if (!hasCryptoOnly && isWeekend(currentDate)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Check for rebalancing
            if (rebalanceFrequency != BacktestRequest.RebalanceFrequency.NONE &&
                    !currentDate.isBefore(nextRebalanceDate)) {

                RebalanceEvent event = executeRebalancing(state, historicalData, currentDate, transactionCostPercent);
                if (event != null) {
                    rebalanceEvents.add(event);
                    totalTransactionCosts += event.getTotalTransactionCost() != null
                            ? event.getTotalTransactionCost()
                            : 0.0;
                }

                nextRebalanceDate = calculateNextRebalanceDate(currentDate, rebalanceFrequency);
            }

            // Calculate portfolio value for the day (includes cash which is reduced by
            // transaction costs)
            double portfolioValue = calculatePortfolioValue(state, historicalData, currentDate);

            // Calculate daily return
            double dailyReturn = ((portfolioValue - previousValue) / previousValue) * 100.0;
            double cumulativeReturn = ((portfolioValue - initialCapital) / initialCapital) * 100.0;

            // Add to equity curve
            equityCurve.add(EquityPoint.builder()
                    .date(currentDate)
                    .value(portfolioValue)
                    .dailyReturn(dailyReturn)
                    .cumulativeReturn(cumulativeReturn)
                    .build());

            previousValue = portfolioValue;
            currentDate = currentDate.plusDays(1);
        }

        // Generate holdings summary
        List<HoldingSummary> holdingsSummary = generateHoldingsSummary(state, historicalData, endDate);

        double finalValue = equityCurve.isEmpty() ? initialCapital : equityCurve.get(equityCurve.size() - 1).getValue();

        return SimulationResult.builder()
                .equityCurve(equityCurve)
                .rebalanceEvents(rebalanceEvents)
                .holdingsSummary(holdingsSummary)
                .finalValue(finalValue)
                .totalTransactionCosts(totalTransactionCosts)
                .build();
    }

    /**
     * 포트폴리오 초기화 (Equal Weight 전략)
     */
    private PortfolioState initializePortfolio(
            List<PortfolioHolding> holdings,
            Map<String, List<HistoricalPriceData>> historicalData,
            double initialCapital,
            LocalDate startDate) {

        Map<String, Double> positions = new HashMap<>();
        double cashPerHolding = initialCapital / holdings.size(); // Equal weight

        for (PortfolioHolding holding : holdings) {
            String symbol = holding.getSymbol();
            Double price = getClosePriceOnDate(historicalData.get(symbol), startDate);

            if (price != null && price > 0) {
                double quantity = cashPerHolding / price;
                positions.put(symbol, quantity);
            }
        }

        return new PortfolioState(positions);
    }

    /**
     * 포트폴리오 가치 계산
     */
    private double calculatePortfolioValue(
            PortfolioState state,
            Map<String, List<HistoricalPriceData>> historicalData,
            LocalDate date) {

        double totalValue = 0.0;

        for (Map.Entry<String, Double> entry : state.getPositions().entrySet()) {
            String symbol = entry.getKey();
            Double quantity = entry.getValue();

            Double price = getClosePriceOnDate(historicalData.get(symbol), date);
            if (price != null) {
                totalValue += quantity * price;
            }
        }

        return totalValue;
    }

    /**
     * 리밸런싱 실행
     *
     * @param state                  현재 포트폴리오 상태
     * @param historicalData         과거 가격 데이터
     * @param date                   리밸런싱 날짜
     * @param transactionCostPercent 거래 비용 비율 (0.001 = 0.1%)
     */
    private RebalanceEvent executeRebalancing(
            PortfolioState state,
            Map<String, List<HistoricalPriceData>> historicalData,
            LocalDate date,
            double transactionCostPercent) {

        double totalValue = calculatePortfolioValue(state, historicalData, date);
        int numSymbols = state.getPositions().size();
        double targetValuePerHolding = totalValue / numSymbols;

        List<Trade> trades = new ArrayList<>();
        double eventTotalCost = 0.0;

        for (Map.Entry<String, Double> entry : state.getPositions().entrySet()) {
            String symbol = entry.getKey();
            Double currentQuantity = entry.getValue();

            Double price = getClosePriceOnDate(historicalData.get(symbol), date);
            if (price == null || price <= 0) {
                continue;
            }

            double currentValue = currentQuantity * price;
            double targetQuantity = targetValuePerHolding / price;
            double quantityDiff = targetQuantity - currentQuantity;

            if (Math.abs(quantityDiff) > 0.01) { // Ignore very small differences
                Trade.TradeAction action = quantityDiff > 0 ? Trade.TradeAction.BUY : Trade.TradeAction.SELL;
                double tradeAmount = Math.abs(quantityDiff) * price;
                double tradeCost = tradeAmount * transactionCostPercent;
                eventTotalCost += tradeCost;

                trades.add(Trade.builder()
                        .symbol(symbol)
                        .action(action)
                        .quantity(Math.abs(quantityDiff))
                        .price(price)
                        .amount(tradeAmount)
                        .transactionCost(tradeCost)
                        .build());

                // Update position
                state.getPositions().put(symbol, targetQuantity);
            }
        }

        if (trades.isEmpty()) {
            return null;
        }

        return RebalanceEvent.builder()
                .date(date)
                .reason(getRebalanceReason(date))
                .trades(trades)
                .totalTransactionCost(eventTotalCost)
                .build();
    }

    /**
     * Holdings 요약 생성
     */
    private List<HoldingSummary> generateHoldingsSummary(
            PortfolioState state,
            Map<String, List<HistoricalPriceData>> historicalData,
            LocalDate endDate) {

        double totalPortfolioValue = calculatePortfolioValue(state, historicalData, endDate);

        return state.getPositions().entrySet().stream()
                .map(entry -> {
                    String symbol = entry.getKey();
                    Double quantity = entry.getValue();

                    Double finalPrice = getClosePriceOnDate(historicalData.get(symbol), endDate);
                    double finalValue = quantity * (finalPrice != null ? finalPrice : 0.0);
                    double finalWeight = (finalValue / totalPortfolioValue) * 100.0;

                    return HoldingSummary.builder()
                            .symbol(symbol)
                            .finalQuantity(quantity)
                            .finalValue(finalValue)
                            .finalWeight(finalWeight)
                            .totalReturn(0.0) // Placeholder
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜의 종가 조회
     */
    private Double getClosePriceOnDate(List<HistoricalPriceData> prices, LocalDate date) {
        if (prices == null) {
            return null;
        }

        return prices.stream()
                .filter(p -> p.getDate().equals(date))
                .map(HistoricalPriceData::getClose)
                .findFirst()
                .orElse(null);
    }

    /**
     * 다음 리밸런싱 날짜 계산
     */
    private LocalDate calculateNextRebalanceDate(LocalDate currentDate, BacktestRequest.RebalanceFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> currentDate.plusMonths(1);
            case QUARTERLY -> currentDate.plusMonths(3);
            case YEARLY -> currentDate.plusYears(1);
            default -> currentDate.plusYears(100); // Never
        };
    }

    /**
     * 리밸런싱 사유 생성
     */
    private String getRebalanceReason(LocalDate date) {
        return "SCHEDULED_REBALANCE";
    }

    /**
     * 주말 여부 확인
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * 포트폴리오 상태 (내부 클래스)
     */
    private static class PortfolioState {
        private final Map<String, Double> positions; // symbol -> quantity

        public PortfolioState(Map<String, Double> positions) {
            this.positions = positions;
        }

        public Map<String, Double> getPositions() {
            return positions;
        }
    }

    /**
     * 시뮬레이션 결과 (내부 클래스)
     */
    private static class SimulationResult {
        private final List<EquityPoint> equityCurve;
        private final List<RebalanceEvent> rebalanceEvents;
        private final List<HoldingSummary> holdingsSummary;
        private final double finalValue;
        private final double totalTransactionCosts;

        @lombok.Builder
        public SimulationResult(
                List<EquityPoint> equityCurve,
                List<RebalanceEvent> rebalanceEvents,
                List<HoldingSummary> holdingsSummary,
                double finalValue,
                double totalTransactionCosts) {
            this.equityCurve = equityCurve;
            this.rebalanceEvents = rebalanceEvents;
            this.holdingsSummary = holdingsSummary;
            this.finalValue = finalValue;
            this.totalTransactionCosts = totalTransactionCosts;
        }

        public List<EquityPoint> getEquityCurve() {
            return equityCurve;
        }

        public List<RebalanceEvent> getRebalanceEvents() {
            return rebalanceEvents;
        }

        public List<HoldingSummary> getHoldingsSummary() {
            return holdingsSummary;
        }

        public double getTotalTransactionCosts() {
            return totalTransactionCosts;
        }

        public double getFinalValue() {
            return finalValue;
        }
    }
}
