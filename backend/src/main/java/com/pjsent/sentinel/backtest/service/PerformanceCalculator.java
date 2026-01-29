package com.pjsent.sentinel.backtest.service;

import com.pjsent.sentinel.backtest.dto.EquityPoint;
import com.pjsent.sentinel.backtest.dto.PerformanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 백테스팅 성과 지표 계산 서비스
 */
@Slf4j
@Service
public class PerformanceCalculator {

    private static final double RISK_FREE_RATE = 0.02; // 2% 무위험 이자율
    private static final double TRADING_DAYS_PER_YEAR = 252.0;

    /**
     * 모든 성과 지표를 계산합니다.
     *
     * @param equityCurve    포트폴리오 가치 추이
     * @param initialCapital 초기 투자 금액
     * @param startDate      시작일
     * @param endDate        종료일
     * @return 성과 지표
     */
    public PerformanceMetrics calculatePerformance(
            List<EquityPoint> equityCurve,
            double initialCapital,
            LocalDate startDate,
            LocalDate endDate) {

        if (equityCurve == null) {
            throw new IllegalArgumentException("Equity curve cannot be null");
        }

        if (equityCurve.isEmpty()) {
            log.warn("Empty equity curve, returning zero metrics");
            return createZeroMetrics();
        }

        double finalValue = equityCurve.get(equityCurve.size() - 1).getValue();

        // Calculate metrics
        double totalReturn = calculateTotalReturn(initialCapital, finalValue);
        double cagr = calculateCAGR(initialCapital, finalValue, startDate, endDate);
        double sharpeRatio = calculateSharpeRatio(equityCurve);
        double sortinoRatio = calculateSortinoRatio(equityCurve);
        double maxDrawdown = calculateMaxDrawdown(equityCurve);
        double volatility = calculateVolatility(equityCurve);
        double winRate = calculateWinRate(equityCurve);

        return PerformanceMetrics.builder()
                .totalReturn(totalReturn)
                .cagr(cagr)
                .sharpeRatio(sharpeRatio)
                .sortinoRatio(sortinoRatio)
                .maxDrawdown(maxDrawdown)
                .volatility(volatility)
                .winRate(winRate)
                .build();
    }

    /**
     * 총 수익률 계산
     * Total Return (%) = ((Final Value - Initial Capital) / Initial Capital) * 100
     */
    private double calculateTotalReturn(double initialCapital, double finalValue) {
        return ((finalValue - initialCapital) / initialCapital) * 100.0;
    }

    /**
     * CAGR (연평균 성장률) 계산
     * CAGR (%) = (((Final Value / Initial Capital) ^ (1 / Years)) - 1) * 100
     */
    private double calculateCAGR(double initialCapital, double finalValue, LocalDate startDate, LocalDate endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        double years = daysBetween / 365.25;

        if (years <= 0) {
            return 0.0;
        }

        return (Math.pow(finalValue / initialCapital, 1.0 / years) - 1.0) * 100.0;
    }

    /**
     * 샤프 비율 계산
     * Sharpe Ratio = (Average Return - Risk Free Rate) / Standard Deviation
     */
    private double calculateSharpeRatio(List<EquityPoint> equityCurve) {
        List<Double> returns = extractDailyReturns(equityCurve);

        if (returns.isEmpty()) {
            return 0.0;
        }

        double avgReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = calculateStandardDeviation(returns);

        if (stdDev == 0.0) {
            return 0.0;
        }

        // Annualize
        double annualizedReturn = avgReturn * TRADING_DAYS_PER_YEAR;
        double annualizedStdDev = stdDev * Math.sqrt(TRADING_DAYS_PER_YEAR);

        return (annualizedReturn - RISK_FREE_RATE) / annualizedStdDev;
    }

    /**
     * 소르티노 비율 계산
     * Sortino Ratio = (Average Return - Risk Free Rate) / Downside Deviation
     */
    private double calculateSortinoRatio(List<EquityPoint> equityCurve) {
        List<Double> returns = extractDailyReturns(equityCurve);

        if (returns.isEmpty()) {
            return 0.0;
        }

        double avgReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Calculate downside deviation (only negative returns)
        List<Double> negativeReturns = returns.stream()
                .filter(r -> r < 0.0)
                .toList();

        if (negativeReturns.isEmpty()) {
            return Double.MAX_VALUE; // No downside risk
        }

        double downsideDeviation = calculateStandardDeviation(negativeReturns);

        if (downsideDeviation == 0.0) {
            return 0.0;
        }

        // Annualize
        double annualizedReturn = avgReturn * TRADING_DAYS_PER_YEAR;
        double annualizedDownsideDev = downsideDeviation * Math.sqrt(TRADING_DAYS_PER_YEAR);

        return (annualizedReturn - RISK_FREE_RATE) / annualizedDownsideDev;
    }

    /**
     * 최대 낙폭 계산
     * Max Drawdown (%) = ((Trough Value - Peak Value) / Peak Value) * 100
     */
    private double calculateMaxDrawdown(List<EquityPoint> equityCurve) {
        double peak = Double.MIN_VALUE;
        double maxDrawdown = 0.0;

        for (EquityPoint point : equityCurve) {
            double value = point.getValue();

            if (value > peak) {
                peak = value;
            }

            double drawdown = ((value - peak) / peak) * 100.0;
            if (drawdown < maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown;
    }

    /**
     * 변동성 계산 (연율화 표준편차)
     * Volatility (%) = Standard Deviation * sqrt(252)
     */
    private double calculateVolatility(List<EquityPoint> equityCurve) {
        List<Double> returns = extractDailyReturns(equityCurve);

        if (returns.isEmpty()) {
            return 0.0;
        }

        double stdDev = calculateStandardDeviation(returns);
        return stdDev * Math.sqrt(TRADING_DAYS_PER_YEAR) * 100.0;
    }

    /**
     * 승률 계산 (양의 수익률을 기록한 날의 비율)
     * Win Rate (%) = (Number of Positive Days / Total Days) * 100
     */
    private double calculateWinRate(List<EquityPoint> equityCurve) {
        List<Double> returns = extractDailyReturns(equityCurve);

        if (returns.isEmpty()) {
            return 0.0;
        }

        long positiveDays = returns.stream().filter(r -> r > 0.0).count();
        return (positiveDays / (double) returns.size()) * 100.0;
    }

    /**
     * 일일 수익률 추출
     */
    private List<Double> extractDailyReturns(List<EquityPoint> equityCurve) {
        return equityCurve.stream()
                .map(EquityPoint::getDailyReturn)
                .filter(r -> r != null && !r.isNaN())
                .toList();
    }

    /**
     * 표준편차 계산
     */
    private double calculateStandardDeviation(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    /**
     * 제로 메트릭 생성 (데이터 없을 때)
     */
    private PerformanceMetrics createZeroMetrics() {
        return PerformanceMetrics.builder()
                .totalReturn(0.0)
                .cagr(0.0)
                .sharpeRatio(0.0)
                .sortinoRatio(0.0)
                .maxDrawdown(0.0)
                .volatility(0.0)
                .winRate(0.0)
                .build();
    }
}
