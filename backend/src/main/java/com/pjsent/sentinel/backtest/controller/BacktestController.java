package com.pjsent.sentinel.backtest.controller;

import com.pjsent.sentinel.backtest.dto.BacktestRequest;
import com.pjsent.sentinel.backtest.dto.BacktestResponse;
import com.pjsent.sentinel.backtest.dto.BacktestValidationResponse;
import com.pjsent.sentinel.backtest.dto.HistoricalPriceResponse;
import com.pjsent.sentinel.backtest.dto.HistoricalPriceData;
import com.pjsent.sentinel.backtest.service.BacktestEngine;
import com.pjsent.sentinel.backtest.service.HistoricalDataFacade;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 백테스팅 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestEngine backtestEngine;
    private final HistoricalDataFacade historicalDataFacade;

    /**
     * 백테스팅 실행
     *
     * @param request 백테스팅 요청
     * @return 백테스팅 결과
     */
    @PostMapping("/run")
    public ResponseEntity<BacktestResponse> runBacktest(@Valid @RequestBody BacktestRequest request) {
        log.info("Received backtest request for portfolio: {}", request.getPortfolioId());

        // Validate date range
        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        // Run backtest
        BacktestResponse response = backtestEngine.runBacktest(request);

        log.info("Backtest completed for portfolio {}: Final Value = {}, Total Return = {}%",
                request.getPortfolioId(),
                response.getFinalValue(),
                response.getPerformance().getTotalReturn());

        return ResponseEntity.ok(response);
    }

    /**
     * 백테스팅 요청 유효성 검증
     *
     * 백테스트 실행 전 파라미터 유효성 및 데이터 가용성을 검증합니다.
     * API 호출 없이 캐시 상태만 확인하므로 빠른 응답이 가능합니다.
     *
     * @param request 백테스팅 요청
     * @return 유효성 검증 결과
     */
    @PostMapping("/validate")
    public ResponseEntity<BacktestValidationResponse> validateBacktest(
            @Valid @RequestBody BacktestRequest request) {
        log.info("Validating backtest request for portfolio: {}", request.getPortfolioId());

        BacktestValidationResponse response = backtestEngine.validateBacktestRequest(request);

        log.info("Validation result for portfolio {}: valid={}, errors={}, warnings={}",
                request.getPortfolioId(),
                response.isValid(),
                response.getErrors().size(),
                response.getWarnings().size());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 종목의 과거 가격 데이터 조회
     *
     * API_BACKTEST.md 스펙에 정의된 엔드포인트
     * - Redis 캐시 적용 (7일 TTL)
     * - 한국 주식 (6자리 숫자): KIS API
     * - 미국 주식: AlphaVantage API
     *
     * @param symbol 종목 심볼
     * @param startDate 시작일 (YYYY-MM-DD)
     * @param endDate 종료일 (YYYY-MM-DD)
     * @return 과거 가격 데이터
     */
    @GetMapping("/historical/{symbol}")
    public ResponseEntity<HistoricalPriceResponse> getHistoricalPrices(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        log.info("Fetching historical prices for {}: {} to {}", symbol, startDate, endDate);

        // Validate date range
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        // Validate max date range (10 years as per API spec)
        if (startDate.plusYears(10).isBefore(endDate)) {
            throw new IllegalArgumentException("Date range must not exceed 10 years");
        }

        // Fetch historical data via Facade (routes to KIS or AlphaVantage based on symbol)
        List<HistoricalPriceData> prices = historicalDataFacade.getHistoricalPrices(
                symbol.toUpperCase(),
                PortfolioHolding.AssetType.STOCK,
                "KRW",  // default base currency
                startDate,
                endDate);

        // Build response matching API spec
        HistoricalPriceResponse response = HistoricalPriceResponse.builder()
                .symbol(symbol.toUpperCase())
                .startDate(startDate)
                .endDate(endDate)
                .dataPoints(prices.size())
                .prices(prices)
                .build();

        log.info("Retrieved {} historical price points for {}", prices.size(), symbol);

        return ResponseEntity.ok(response);
    }

    /**
     * 백테스팅 서비스 상태 확인
     *
     * @return 서비스 상태
     */
    @GetMapping("/status")
    public ResponseEntity<BacktestStatusResponse> getStatus() {
        return ResponseEntity.ok(BacktestStatusResponse.builder()
                .available(true)
                .provider("AlphaVantage")
                .supportedFrequencies(new String[]{"NONE", "MONTHLY", "QUARTERLY", "YEARLY"})
                .maxDateRange("10 years")
                .build());
    }

    /**
     * 백테스팅 서비스 상태 응답
     */
    @lombok.Builder
    @lombok.Data
    private static class BacktestStatusResponse {
        private boolean available;
        private String provider;
        private String[] supportedFrequencies;
        private String maxDateRange;
    }
}
