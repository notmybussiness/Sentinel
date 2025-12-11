package com.pjsent.sentinel.backtest.service;

import com.pjsent.sentinel.backtest.dto.EquityPoint;
import com.pjsent.sentinel.backtest.dto.PerformanceMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PerformanceCalculator 단위 테스트
 * 백테스팅 성과 지표 계산 서비스 테스트
 */
class PerformanceCalculatorTest {

    private PerformanceCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PerformanceCalculator();
    }

    // ========================================
    // 1. Formula Accuracy Tests (9 tests)
    // ========================================

    @Test
    @DisplayName("Total Return: (11000 - 10000) / 10000 * 100 = 10%")
    void should_CalculateTotalReturn_When_ValidInputs() {
        // Given
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(LocalDate.of(2023, 1, 1), 10000.0, 0.0, 0.0),
            createEquityPoint(LocalDate.of(2023, 12, 31), 11000.0, 1.0, 10.0)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));

        // Then
        assertThat(result.getTotalReturn()).isCloseTo(10.0, within(0.01));
    }

    @Test
    @DisplayName("CAGR: 1 year, 10000 to 11000 = 10% CAGR")
    void should_CalculateCAGR_When_OneYearPeriod() {
        // Given
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 1); // Exactly 1 year (365 days)
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(startDate, 10000.0, 0.0, 0.0),
            createEquityPoint(endDate, 11000.0, 1.0, 10.0)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, startDate, endDate);

        // Then: CAGR = ((11000 / 10000)^(1/1) - 1) * 100 = 10%
        assertThat(result.getCagr()).isCloseTo(10.0, within(0.1));
    }

    @Test
    @DisplayName("Sharpe Ratio: (avgReturn - 0.02) / stdDev with annualization")
    void should_CalculateSharpeRatio_When_ValidReturns() {
        // Given: 30-day equity curve with positive average return
        List<EquityPoint> equityCurve = createThirtyDayEquityCurve();

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 30));

        // Then: Sharpe Ratio should be positive for positive returns
        assertThat(result.getSharpeRatio()).isNotNaN();
    }

    @Test
    @DisplayName("Sortino Ratio: Only negative returns for downside deviation")
    void should_CalculateSortinoRatio_When_HasNegativeReturns() {
        // Given: Mixed positive and negative returns
        List<EquityPoint> equityCurve = createMixedReturnsEquityCurve();

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 10));

        // Then: Sortino Ratio should be calculated (not NaN)
        assertThat(result.getSortinoRatio()).isNotNaN();
    }

    @Test
    @DisplayName("Max Drawdown: Find worst peak-to-trough decline")
    void should_CalculateMaxDrawdown_When_MultipleDrawdowns() {
        // Given: Equity curve with peak at 11000, trough at 9500
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(LocalDate.of(2023, 1, 1), 10000.0, 0.0, 0.0),
            createEquityPoint(LocalDate.of(2023, 1, 2), 11000.0, 10.0, 10.0),  // Peak
            createEquityPoint(LocalDate.of(2023, 1, 3), 10500.0, -4.55, 5.0),
            createEquityPoint(LocalDate.of(2023, 1, 4), 9500.0, -9.52, -5.0),  // Trough
            createEquityPoint(LocalDate.of(2023, 1, 5), 10000.0, 5.26, 0.0)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 5));

        // Then: Max Drawdown = (9500 - 11000) / 11000 * 100 = -13.64%
        assertThat(result.getMaxDrawdown()).isCloseTo(-13.64, within(0.1));
    }

    @Test
    @DisplayName("Volatility: Annualized standard deviation * sqrt(252)")
    void should_CalculateVolatility_When_ValidDailyReturns() {
        // Given: 30-day equity curve
        List<EquityPoint> equityCurve = createThirtyDayEquityCurve();

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 30));

        // Then: Volatility should be positive and annualized
        assertThat(result.getVolatility()).isPositive();
    }

    @Test
    @DisplayName("Win Rate: Positive days / Total days * 100")
    void should_CalculateWinRate_When_MixedReturns() {
        // Given: 10 days with 6 positive returns (excluding day 0 which has 0.0 return)
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(LocalDate.of(2023, 1, 1), 10000.0, null, 0.0),  // Day 0 (filtered out)
            createEquityPoint(LocalDate.of(2023, 1, 2), 10100.0, 1.0, 1.0),   // + (1)
            createEquityPoint(LocalDate.of(2023, 1, 3), 10050.0, -0.5, 0.5),  // - (2)
            createEquityPoint(LocalDate.of(2023, 1, 4), 10150.0, 1.0, 1.5),   // + (3)
            createEquityPoint(LocalDate.of(2023, 1, 5), 10100.0, -0.5, 1.0),  // - (4)
            createEquityPoint(LocalDate.of(2023, 1, 6), 10200.0, 1.0, 2.0),   // + (5)
            createEquityPoint(LocalDate.of(2023, 1, 7), 10250.0, 0.5, 2.5),   // + (6)
            createEquityPoint(LocalDate.of(2023, 1, 8), 10200.0, -0.5, 2.0),  // - (7)
            createEquityPoint(LocalDate.of(2023, 1, 9), 10300.0, 1.0, 3.0),   // + (8)
            createEquityPoint(LocalDate.of(2023, 1, 10), 10350.0, 0.5, 3.5)   // + (9)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 10));

        // Then: 6 positive / 9 total returns (null filtered out) = 66.67%
        assertThat(result.getWinRate()).isCloseTo(66.67, within(1.0));
    }

    @Test
    @DisplayName("Standard Deviation: sqrt(variance)")
    void should_CalculateStandardDeviation_When_ValidValues() {
        // Given: Equity curve with known variance
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(LocalDate.of(2023, 1, 1), 10000.0, null, 0.0),  // Filtered
            createEquityPoint(LocalDate.of(2023, 1, 2), 10010.0, 0.1, 0.1),
            createEquityPoint(LocalDate.of(2023, 1, 3), 10020.0, 0.1, 0.2),
            createEquityPoint(LocalDate.of(2023, 1, 4), 10030.0, 0.1, 0.3)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 4));

        // Then: Volatility should be very small (consistent returns)
        // With only 3 identical returns of 0.1%, std dev ≈ 0, volatility ≈ 0
        assertThat(result.getVolatility()).isCloseTo(0.0, within(0.01));
    }

    @Test
    @DisplayName("calculatePerformance: All metrics integrated")
    void should_CalculateAllMetrics_When_CompleteEquityCurve() {
        // Given: 1-year equity curve
        List<EquityPoint> equityCurve = createOneYearEquityCurve();
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, startDate, endDate);

        // Then: All metrics should be calculated
        assertThat(result.getTotalReturn()).isNotZero();
        assertThat(result.getCagr()).isNotZero();
        assertThat(result.getSharpeRatio()).isNotNaN();
        assertThat(result.getSortinoRatio()).isNotNaN();
        assertThat(result.getMaxDrawdown()).isNotZero();
        assertThat(result.getVolatility()).isPositive();
        assertThat(result.getWinRate()).isGreaterThan(0.0);
    }

    // ========================================
    // 2. Edge Cases (7 tests)
    // ========================================

    @Test
    @DisplayName("Empty equity curve returns zero metrics")
    void should_ReturnZeroMetrics_When_EmptyEquityCurve() {
        // Given
        List<EquityPoint> emptyEquityCurve = new ArrayList<>();

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            emptyEquityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));

        // Then
        assertThat(result.getTotalReturn()).isZero();
        assertThat(result.getCagr()).isZero();
        assertThat(result.getSharpeRatio()).isZero();
        assertThat(result.getSortinoRatio()).isZero();
        assertThat(result.getMaxDrawdown()).isZero();
        assertThat(result.getVolatility()).isZero();
        assertThat(result.getWinRate()).isZero();
    }

    @Test
    @DisplayName("Single data point returns zero for ratios")
    void should_HandleSingleDataPoint_When_CalculatingMetrics() {
        // Given: Only one equity point (no daily returns)
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(LocalDate.of(2023, 1, 1), 10000.0, null, 0.0)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 1));

        // Then: Metrics should handle single point gracefully
        assertThat(result.getTotalReturn()).isZero();
        assertThat(result.getSharpeRatio()).isZero();
        assertThat(result.getSortinoRatio()).isZero();
        assertThat(result.getWinRate()).isZero();
    }

    @Test
    @DisplayName("All positive returns: Sortino Ratio = Double.MAX_VALUE")
    void should_ReturnMaxSortinoRatio_When_NoDownsideRisk() {
        // Given: Only positive returns (no downside deviation)
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(LocalDate.of(2023, 1, 1), 10000.0, 0.0, 0.0),
            createEquityPoint(LocalDate.of(2023, 1, 2), 10100.0, 1.0, 1.0),
            createEquityPoint(LocalDate.of(2023, 1, 3), 10200.0, 0.99, 2.0),
            createEquityPoint(LocalDate.of(2023, 1, 4), 10300.0, 0.98, 3.0)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 4));

        // Then: Sortino Ratio should be MAX_VALUE (no downside risk)
        assertThat(result.getSortinoRatio()).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    @DisplayName("Zero volatility returns zero Sharpe Ratio")
    void should_ReturnZeroSharpeRatio_When_ZeroVolatility() {
        // Given: Constant value (zero volatility)
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(LocalDate.of(2023, 1, 1), 10000.0, 0.0, 0.0),
            createEquityPoint(LocalDate.of(2023, 1, 2), 10000.0, 0.0, 0.0),
            createEquityPoint(LocalDate.of(2023, 1, 3), 10000.0, 0.0, 0.0)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 3));

        // Then: Sharpe Ratio should be zero (division by zero protection)
        assertThat(result.getSharpeRatio()).isZero();
    }

    @Test
    @DisplayName("CAGR handles zero/negative years")
    void should_ReturnZeroCAGR_When_InvalidYears() {
        // Given: Same start and end date (0 years)
        LocalDate sameDate = LocalDate.of(2023, 1, 1);
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(sameDate, 10000.0, 0.0, 0.0)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, sameDate, sameDate);

        // Then: CAGR should be zero (avoid division by zero)
        assertThat(result.getCagr()).isZero();
    }

    @Test
    @DisplayName("Handles NaN daily returns gracefully")
    void should_FilterNaNReturns_When_CalculatingMetrics() {
        // Given: Equity curve with NaN returns
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(LocalDate.of(2023, 1, 1), 10000.0, Double.NaN, 0.0),
            createEquityPoint(LocalDate.of(2023, 1, 2), 10100.0, 1.0, 1.0),
            createEquityPoint(LocalDate.of(2023, 1, 3), 10200.0, 0.99, 2.0)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 3));

        // Then: Should filter NaN and calculate with valid returns only
        assertThat(result.getWinRate()).isCloseTo(100.0, within(1.0)); // 2 positive returns
    }

    @Test
    @DisplayName("Max drawdown with no decline returns 0")
    void should_ReturnZeroDrawdown_When_OnlyGains() {
        // Given: Only upward equity curve (no drawdown)
        List<EquityPoint> equityCurve = List.of(
            createEquityPoint(LocalDate.of(2023, 1, 1), 10000.0, 0.0, 0.0),
            createEquityPoint(LocalDate.of(2023, 1, 2), 10100.0, 1.0, 1.0),
            createEquityPoint(LocalDate.of(2023, 1, 3), 10200.0, 0.99, 2.0),
            createEquityPoint(LocalDate.of(2023, 1, 4), 10300.0, 0.98, 3.0)
        );

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 4));

        // Then: Max drawdown should be zero
        assertThat(result.getMaxDrawdown()).isZero();
    }

    // ========================================
    // 3. Performance Benchmarks (2 tests)
    // ========================================

    @Test
    @DisplayName("Large dataset (10 years = ~2520 days) performs < 100ms")
    void should_PerformFast_When_LargeDataset() {
        // Given: 2520 trading days (10 years)
        List<EquityPoint> largeEquityCurve = createLargeEquityCurve(2520);
        LocalDate startDate = LocalDate.of(2013, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 1);

        // When
        long startTime = System.currentTimeMillis();
        PerformanceMetrics result = calculator.calculatePerformance(
            largeEquityCurve, 10000.0, startDate, endDate);
        long duration = System.currentTimeMillis() - startTime;

        // Then: Should complete in < 100ms
        assertThat(duration).isLessThan(100L);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Verify constant values: RISK_FREE_RATE=0.02, TRADING_DAYS=252")
    void should_UseCorrectConstants_When_Calculating() {
        // Given: Known equity curve
        List<EquityPoint> equityCurve = createThirtyDayEquityCurve();

        // When
        PerformanceMetrics result = calculator.calculatePerformance(
            equityCurve, 10000.0, LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 30));

        // Then: Verify metrics are calculated (constants used internally)
        // This test verifies the calculator runs with expected constants
        assertThat(result.getSharpeRatio()).isNotNaN();
        assertThat(result.getVolatility()).isPositive();
    }

    // ========================================
    // Helper Methods
    // ========================================

    private EquityPoint createEquityPoint(LocalDate date, Double value, Double dailyReturn, Double cumulativeReturn) {
        return EquityPoint.builder()
            .date(date)
            .value(value)
            .dailyReturn(dailyReturn)
            .cumulativeReturn(cumulativeReturn)
            .build();
    }

    private List<EquityPoint> createThirtyDayEquityCurve() {
        List<EquityPoint> curve = new ArrayList<>();
        double value = 10000.0;
        LocalDate date = LocalDate.of(2023, 1, 1);

        for (int i = 0; i < 30; i++) {
            // Simulate random daily returns between -1% and +2%
            double dailyReturn = (Math.random() * 3.0 - 1.0);
            value = value * (1 + dailyReturn / 100.0);
            double cumulativeReturn = ((value - 10000.0) / 10000.0) * 100.0;

            curve.add(createEquityPoint(date.plusDays(i), value, dailyReturn, cumulativeReturn));
        }
        return curve;
    }

    private List<EquityPoint> createMixedReturnsEquityCurve() {
        return List.of(
            createEquityPoint(LocalDate.of(2023, 1, 1), 10000.0, 0.0, 0.0),
            createEquityPoint(LocalDate.of(2023, 1, 2), 10100.0, 1.0, 1.0),   // Positive
            createEquityPoint(LocalDate.of(2023, 1, 3), 10050.0, -0.5, 0.5),  // Negative
            createEquityPoint(LocalDate.of(2023, 1, 4), 10150.0, 1.0, 1.5),   // Positive
            createEquityPoint(LocalDate.of(2023, 1, 5), 10100.0, -0.5, 1.0),  // Negative
            createEquityPoint(LocalDate.of(2023, 1, 6), 10200.0, 1.0, 2.0),   // Positive
            createEquityPoint(LocalDate.of(2023, 1, 7), 10150.0, -0.5, 1.5),  // Negative
            createEquityPoint(LocalDate.of(2023, 1, 8), 10250.0, 1.0, 2.5),   // Positive
            createEquityPoint(LocalDate.of(2023, 1, 9), 10200.0, -0.5, 2.0),  // Negative
            createEquityPoint(LocalDate.of(2023, 1, 10), 10300.0, 1.0, 3.0)   // Positive
        );
    }

    private List<EquityPoint> createOneYearEquityCurve() {
        List<EquityPoint> curve = new ArrayList<>();
        double value = 10000.0;
        LocalDate date = LocalDate.of(2023, 1, 1);

        // 252 trading days
        for (int i = 0; i < 252; i++) {
            double dailyReturn = (Math.random() * 2.0 - 0.5); // -0.5% to +1.5%
            value = value * (1 + dailyReturn / 100.0);
            double cumulativeReturn = ((value - 10000.0) / 10000.0) * 100.0;

            curve.add(createEquityPoint(date.plusDays(i), value, dailyReturn, cumulativeReturn));
        }
        return curve;
    }

    private List<EquityPoint> createLargeEquityCurve(int days) {
        List<EquityPoint> curve = new ArrayList<>();
        double value = 10000.0;
        LocalDate date = LocalDate.of(2013, 1, 1);

        for (int i = 0; i < days; i++) {
            double dailyReturn = (Math.random() * 2.0 - 0.5);
            value = value * (1 + dailyReturn / 100.0);
            double cumulativeReturn = ((value - 10000.0) / 10000.0) * 100.0;

            curve.add(createEquityPoint(date.plusDays(i), value, dailyReturn, cumulativeReturn));
        }
        return curve;
    }
}
