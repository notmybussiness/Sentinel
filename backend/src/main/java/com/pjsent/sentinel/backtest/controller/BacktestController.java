package com.pjsent.sentinel.backtest.controller;

import com.pjsent.sentinel.backtest.dto.BacktestRequest;
import com.pjsent.sentinel.backtest.dto.BacktestResponse;
import com.pjsent.sentinel.backtest.service.BacktestEngine;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 백테스팅 API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestEngine backtestEngine;

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
