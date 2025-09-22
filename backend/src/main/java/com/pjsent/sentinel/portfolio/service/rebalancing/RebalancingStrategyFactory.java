package com.pjsent.sentinel.portfolio.service.rebalancing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 리밸런싱 전략 팩토리
 * 마켓 데이터 프로바이더 팩토리와 유사한 패턴으로 구현
 * 다양한 리밸런싱 전략을 관리하고 선택할 수 있는 팩토리 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RebalancingStrategyFactory {

    private final ThresholdBasedRebalancingStrategy thresholdStrategy;
    private final TimeBasedRebalancingStrategy timeStrategy;
    private final HybridRebalancingStrategy hybridStrategy;

    /**
     * 사용 가능한 모든 전략 목록 반환
     */
    public Map<String, RebalancingStrategy> getAllStrategies() {
        Map<String, RebalancingStrategy> strategies = new HashMap<>();
        strategies.put(thresholdStrategy.getStrategyName(), thresholdStrategy);
        strategies.put(timeStrategy.getStrategyName(), timeStrategy);
        strategies.put(hybridStrategy.getStrategyName(), hybridStrategy);
        return strategies;
    }

    /**
     * 전략명으로 전략 인스턴스 반환
     *
     * @param strategyName 전략명 (THRESHOLD_BASED, TIME_BASED, HYBRID)
     * @return 해당 전략 인스턴스
     * @throws IllegalArgumentException 지원하지 않는 전략명인 경우
     */
    public RebalancingStrategy getStrategy(String strategyName) {
        if (strategyName == null || strategyName.trim().isEmpty()) {
            log.info("전략명이 지정되지 않아 기본 전략(THRESHOLD_BASED) 사용");
            return getDefaultStrategy();
        }

        switch (strategyName.toUpperCase()) {
            case "THRESHOLD_BASED":
                log.debug("임계값 기반 리밸런싱 전략 선택");
                return thresholdStrategy;

            case "TIME_BASED":
                log.debug("시간 기반 리밸런싱 전략 선택");
                return timeStrategy;

            case "HYBRID":
                log.debug("하이브리드 리밸런싱 전략 선택");
                return hybridStrategy;

            default:
                log.warn("지원하지 않는 리밸런싱 전략: {}. 기본 전략 사용", strategyName);
                return getDefaultStrategy();
        }
    }

    /**
     * 기본 전략 반환 (연구 결과에 따라 THRESHOLD_BASED가 가장 효과적)
     */
    public RebalancingStrategy getDefaultStrategy() {
        return thresholdStrategy;
    }

    /**
     * 포트폴리오 특성에 따른 추천 전략 반환
     *
     * @param portfolioValue 포트폴리오 총 가치
     * @param riskTolerance 위험 허용도 (1=보수적, 5=적극적)
     * @param investmentHorizon 투자 기간 (월 단위)
     * @return 추천 전략
     */
    public RebalancingStrategy getRecommendedStrategy(double portfolioValue, int riskTolerance, int investmentHorizon) {
        log.info("포트폴리오 특성 기반 전략 추천 - 가치: {}, 위험허용도: {}, 투자기간: {}개월",
                portfolioValue, riskTolerance, investmentHorizon);

        // 소액 포트폴리오 (1억 미만) + 보수적 성향
        if (portfolioValue < 100_000_000 && riskTolerance <= 2) {
            log.info("소액 포트폴리오 + 보수적 성향 → 시간 기반 전략 추천");
            return timeStrategy;
        }

        // 장기 투자 (5년 이상) + 안정적 관리 선호
        if (investmentHorizon >= 60 && riskTolerance <= 3) {
            log.info("장기 투자 + 안정적 관리 → 하이브리드 전략 추천");
            return hybridStrategy;
        }

        // 적극적 투자 성향 + 시장 반응성 중시
        if (riskTolerance >= 4) {
            log.info("적극적 투자 성향 → 임계값 기반 전략 추천");
            return thresholdStrategy;
        }

        // 기본: 하이브리드 전략 (균형적 접근)
        log.info("기본 추천 → 하이브리드 전략");
        return hybridStrategy;
    }

    /**
     * 전략별 특성 정보 반환
     */
    public Map<String, StrategyInfo> getStrategyInfos() {
        Map<String, StrategyInfo> infos = new HashMap<>();

        infos.put("THRESHOLD_BASED", StrategyInfo.builder()
                .name("임계값 기반")
                .description(thresholdStrategy.getDescription())
                .pros(List.of("시장 변동에 즉시 반응", "효율적인 리스크 관리", "거래 비용 최소화"))
                .cons(List.of("빈번한 모니터링 필요", "변동성 시기 거래 증가"))
                .suitableFor(List.of("적극적 투자자", "시장 반응성 중시", "큰 포트폴리오"))
                .complexity("중간")
                .build());

        infos.put("TIME_BASED", StrategyInfo.builder()
                .name("시간 기반")
                .description(timeStrategy.getDescription())
                .pros(List.of("예측 가능한 일정", "감정적 결정 방지", "간단한 관리"))
                .cons(List.of("시장 타이밍 놓칠 수 있음", "급격한 변동 대응 부족"))
                .suitableFor(List.of("장기 투자자", "간편한 관리 선호", "안정적 성향"))
                .complexity("낮음")
                .build());

        infos.put("HYBRID", StrategyInfo.builder()
                .name("하이브리드")
                .description(hybridStrategy.getDescription())
                .pros(List.of("균형적 접근", "유연한 대응", "최적의 타이밍"))
                .cons(List.of("복잡한 로직", "설정 조정 필요"))
                .suitableFor(List.of("균형적 투자자", "맞춤형 관리 원하는 경우", "중간 규모 포트폴리오"))
                .complexity("높음")
                .build());

        return infos;
    }

    /**
     * 전략 정보 클래스
     */
    public static class StrategyInfo {
        public String name;
        public String description;
        public List<String> pros;
        public List<String> cons;
        public List<String> suitableFor;
        public String complexity;

        public static StrategyInfoBuilder builder() {
            return new StrategyInfoBuilder();
        }

        public static class StrategyInfoBuilder {
            private String name;
            private String description;
            private List<String> pros;
            private List<String> cons;
            private List<String> suitableFor;
            private String complexity;

            public StrategyInfoBuilder name(String name) {
                this.name = name;
                return this;
            }

            public StrategyInfoBuilder description(String description) {
                this.description = description;
                return this;
            }

            public StrategyInfoBuilder pros(List<String> pros) {
                this.pros = pros;
                return this;
            }

            public StrategyInfoBuilder cons(List<String> cons) {
                this.cons = cons;
                return this;
            }

            public StrategyInfoBuilder suitableFor(List<String> suitableFor) {
                this.suitableFor = suitableFor;
                return this;
            }

            public StrategyInfoBuilder complexity(String complexity) {
                this.complexity = complexity;
                return this;
            }

            public StrategyInfo build() {
                StrategyInfo info = new StrategyInfo();
                info.name = this.name;
                info.description = this.description;
                info.pros = this.pros;
                info.cons = this.cons;
                info.suitableFor = this.suitableFor;
                info.complexity = this.complexity;
                return info;
            }
        }
    }

    /**
     * 전략 성능 통계 (향후 확장용)
     */
    public Map<String, Object> getStrategyPerformanceStats(String strategyName) {
        Map<String, Object> stats = new HashMap<>();

        // 향후 실제 사용 데이터를 기반으로 통계 구현
        stats.put("totalRecommendations", 0);
        stats.put("successfulRebalances", 0);
        stats.put("averageImprovement", 0.0);
        stats.put("averageTransactionCost", 0.0);

        return stats;
    }
}