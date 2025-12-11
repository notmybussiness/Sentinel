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
    private final PerformanceCalculator performanceCalculator;

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

        // 1. Load portfolio
        Portfolio portfolio = portfolioRepository.findById(request.getPortfolioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));

        // 2. Validate holdings (Stock only)
        List<PortfolioHolding> stockHoldings = portfolio.getHoldings().stream()
                .filter(h -> h.getAssetType() == PortfolioHolding.AssetType.STOCK)
                .toList();

        if (stockHoldings.isEmpty()) {
            throw new IllegalArgumentException("Portfolio has no stock holdings for backtesting");
        }

        log.info("Found {} stock holdings to backtest", stockHoldings.size());

        // 3. Fetch historical data for all holdings
        Map<String, List<HistoricalPriceData>> historicalData = fetchHistoricalData(
                stockHoldings, request.getStartDate(), request.getEndDate());

        // 4. Run simulation
        SimulationResult simulation = runSimulation(
                stockHoldings,
                historicalData,
                request.getInitialCapital(),
                request.getStartDate(),
                request.getEndDate(),
                request.getRebalanceFrequency()
        );

        // 5. Calculate performance metrics
        PerformanceMetrics performance = performanceCalculator.calculatePerformance(
                simulation.getEquityCurve(),
                request.getInitialCapital(),
                request.getStartDate(),
                request.getEndDate()
        );

        // 6. Build response
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
                .build();
    }

    /**
     * 모든 종목의 과거 데이터 조회
     */
    private Map<String, List<HistoricalPriceData>> fetchHistoricalData(
            List<PortfolioHolding> holdings,
            LocalDate startDate,
            LocalDate endDate) {

        List<String> symbols = holdings.stream()
                .map(PortfolioHolding::getSymbol)
                .distinct()
                .toList();

        log.info("Fetching historical data for {} symbols", symbols.size());
        return historicalDataService.getBatchHistoricalPrices(symbols, startDate, endDate);
    }

    /**
     * 백테스팅 시뮬레이션 실행
     */
    private SimulationResult runSimulation(
            List<PortfolioHolding> holdings,
            Map<String, List<HistoricalPriceData>> historicalData,
            double initialCapital,
            LocalDate startDate,
            LocalDate endDate,
            BacktestRequest.RebalanceFrequency rebalanceFrequency) {

        // Initialize portfolio state
        PortfolioState state = initializePortfolio(holdings, historicalData, initialCapital, startDate);

        List<EquityPoint> equityCurve = new ArrayList<>();
        List<RebalanceEvent> rebalanceEvents = new ArrayList<>();

        // Simulate day by day
        LocalDate currentDate = startDate;
        LocalDate nextRebalanceDate = calculateNextRebalanceDate(startDate, rebalanceFrequency);

        double previousValue = initialCapital;

        while (!currentDate.isAfter(endDate)) {
            // Skip weekends
            if (isWeekend(currentDate)) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Check for rebalancing
            if (rebalanceFrequency != BacktestRequest.RebalanceFrequency.NONE &&
                    !currentDate.isBefore(nextRebalanceDate)) {

                RebalanceEvent event = executeRebalancing(state, historicalData, currentDate);
                if (event != null) {
                    rebalanceEvents.add(event);
                }

                nextRebalanceDate = calculateNextRebalanceDate(currentDate, rebalanceFrequency);
            }

            // Calculate portfolio value for the day
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
     */
    private RebalanceEvent executeRebalancing(
            PortfolioState state,
            Map<String, List<HistoricalPriceData>> historicalData,
            LocalDate date) {

        double totalValue = calculatePortfolioValue(state, historicalData, date);
        int numSymbols = state.getPositions().size();
        double targetValuePerHolding = totalValue / numSymbols;

        List<Trade> trades = new ArrayList<>();

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
                trades.add(Trade.builder()
                        .symbol(symbol)
                        .action(action)
                        .quantity(Math.abs(quantityDiff))
                        .price(price)
                        .amount(Math.abs(quantityDiff) * price)
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

        @lombok.Builder
        public SimulationResult(
                List<EquityPoint> equityCurve,
                List<RebalanceEvent> rebalanceEvents,
                List<HoldingSummary> holdingsSummary,
                double finalValue) {
            this.equityCurve = equityCurve;
            this.rebalanceEvents = rebalanceEvents;
            this.holdingsSummary = holdingsSummary;
            this.finalValue = finalValue;
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

        public double getFinalValue() {
            return finalValue;
        }
    }
}
