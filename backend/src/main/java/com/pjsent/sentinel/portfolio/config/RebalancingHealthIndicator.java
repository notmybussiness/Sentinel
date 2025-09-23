package com.pjsent.sentinel.portfolio.config;

import com.pjsent.sentinel.market.service.provider.AlphaVantageProvider;
import com.pjsent.sentinel.market.service.provider.FinnhubProvider;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 리밸런싱 시스템 헬스 체크 컨트롤러
 * 외부 API 연결 상태와 핵심 서비스 가용성을 모니터링
 */
@RestController
@RequestMapping("/api/v1/system")
@Slf4j
@RequiredArgsConstructor
public class RebalancingHealthIndicator {

    private final AlphaVantageProvider alphaVantageProvider;
    private final FinnhubProvider finnhubProvider;
    private final PortfolioRepository portfolioRepository;

    @GetMapping("/health/rebalancing")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            Map<String, Object> details = new HashMap<>();

            // 외부 API 상태 확인
            boolean alphaVantageUp = checkAlphaVantageHealth();
            boolean finnhubUp = checkFinnhubHealth();

            details.put("alphaVantage", Map.of(
                "status", alphaVantageUp ? "UP" : "DOWN",
                "available", alphaVantageProvider.isAvailable()
            ));

            details.put("finnhub", Map.of(
                "status", finnhubUp ? "UP" : "DOWN",
                "available", finnhubProvider.isAvailable()
            ));

            // 데이터베이스 연결 확인
            boolean dbUp = checkDatabaseHealth();
            details.put("database", Map.of(
                "status", dbUp ? "UP" : "DOWN"
            ));

            // 전체 시스템 상태 결정
            boolean overallHealthy = (alphaVantageUp || finnhubUp) && dbUp;

            details.put("timestamp", LocalDateTime.now());
            details.put("marketDataProviders", alphaVantageUp && finnhubUp ? "ALL_UP" :
                                             alphaVantageUp || finnhubUp ? "PARTIAL" : "ALL_DOWN");

            if (overallHealthy) {
                details.put("status", "UP");
                details.put("overall", "HEALTHY");
                return ResponseEntity.ok(details);
            } else {
                details.put("status", "DOWN");
                details.put("overall", "UNHEALTHY");
                details.put("issue", "핵심 서비스 사용 불가");
                return ResponseEntity.status(503).body(details);
            }

        } catch (Exception e) {
            log.error("헬스 체크 중 오류 발생: {}", e.getMessage(), e);
            Map<String, Object> errorDetails = Map.of(
                    "status", "ERROR",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.status(500).body(errorDetails);
        }
    }

    private boolean checkAlphaVantageHealth() {
        try {
            return alphaVantageProvider.isAvailable();
        } catch (Exception e) {
            log.warn("AlphaVantage 헬스 체크 실패: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkFinnhubHealth() {
        try {
            return finnhubProvider.isAvailable();
        } catch (Exception e) {
            log.warn("Finnhub 헬스 체크 실패: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkDatabaseHealth() {
        try {
            // 간단한 DB 연결 확인
            portfolioRepository.count();
            return true;
        } catch (Exception e) {
            log.error("데이터베이스 헬스 체크 실패: {}", e.getMessage());
            return false;
        }
    }
}