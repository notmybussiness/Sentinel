package com.pjsent.sentinel.rebalancing.controller;

import com.pjsent.sentinel.rebalancing.dto.*;
import com.pjsent.sentinel.rebalancing.service.RebalancingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 리밸런싱 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rebalancing")
@RequiredArgsConstructor
public class RebalancingController {

    private final RebalancingService rebalancingService;

    /**
     * 리밸런싱 추천 생성
     */
    @PostMapping("/recommend")
    public ResponseEntity<RebalancingResponse> getRecommendations(
            Authentication authentication,
            @Valid @RequestBody RebalancingRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Received rebalancing recommendation request from user {}: {}", userId, request);

        RebalancingResponse response = rebalancingService.generateRecommendations(userId, request);

        log.info("Generated {} recommendations for portfolio {}",
                response.getRecommendations().size(), response.getPortfolioId());

        return ResponseEntity.ok(response);
    }

    /**
     * 리밸런싱 시뮬레이션
     */
    @PostMapping("/simulate")
    public ResponseEntity<RebalancingSimulationResponse> simulateRebalancing(
            Authentication authentication,
            @Valid @RequestBody RebalancingRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        log.info("Received rebalancing simulation request from user {}: {}", userId, request);

        RebalancingSimulationResponse response = rebalancingService.simulateRebalancing(userId, request);

        log.info("Simulated rebalancing for portfolio {}: netChange = {}",
                response.getPortfolioId(), response.getNetChange());

        return ResponseEntity.ok(response);
    }

    /**
     * 지원하는 리밸런싱 전략 목록
     */
    @GetMapping("/strategies")
    public ResponseEntity<StrategyInfoResponse> getStrategies() {
        log.info("Received request for rebalancing strategies");

        List<StrategyInfoResponse.StrategyInfo> strategies = new ArrayList<>();

        for (RebalancingStrategy strategy : RebalancingStrategy.values()) {
            strategies.add(StrategyInfoResponse.StrategyInfo.builder()
                    .name(strategy.name())
                    .displayName(strategy.getDisplayName())
                    .description(strategy.getDescription())
                    .supported(strategy.isSupported())
                    .build());
        }

        StrategyInfoResponse response = StrategyInfoResponse.builder()
                .strategies(strategies)
                .build();

        return ResponseEntity.ok(response);
    }
}
