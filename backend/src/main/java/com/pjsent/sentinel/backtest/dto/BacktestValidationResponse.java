package com.pjsent.sentinel.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 백테스팅 유효성 검증 응답 DTO
 * 백테스트 실행 전 파라미터 유효성 및 데이터 가용성을 검증합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestValidationResponse {

    /**
     * 전체 유효성 결과 (모든 검증 통과 시 true)
     */
    private boolean valid;

    /**
     * 검증 오류 목록 (백테스트 실행 불가 사유)
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * 경고 목록 (실행은 가능하나 주의 필요)
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * 종목별 데이터 가용성 (symbol → cached 여부)
     */
    @Builder.Default
    private Map<String, Boolean> dataAvailability = new HashMap<>();

    /**
     * 포트폴리오 정보 요약
     */
    private PortfolioSummary portfolioSummary;

    /**
     * 포트폴리오 요약 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioSummary {
        private Long portfolioId;
        private String portfolioName;
        private int stockCount;
        private int cryptoCount;
        private List<String> symbols;
    }
}
