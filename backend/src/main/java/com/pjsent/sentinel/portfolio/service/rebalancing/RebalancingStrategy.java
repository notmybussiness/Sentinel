package com.pjsent.sentinel.portfolio.service.rebalancing;

import com.pjsent.sentinel.portfolio.dto.RebalancingRecommendationDto;
import com.pjsent.sentinel.portfolio.entity.Portfolio;

import java.util.Map;

/**
 * 포트폴리오 리밸런싱 전략 인터페이스
 * 다양한 리밸런싱 전략을 구현할 수 있는 Strategy Pattern의 기본 인터페이스
 */
public interface RebalancingStrategy {

    /**
     * 전략 이름 반환
     */
    String getStrategyName();

    /**
     * 전략 설명 반환
     */
    String getDescription();

    /**
     * 리밸런싱 필요 여부 확인
     *
     * @param portfolio 현재 포트폴리오
     * @param targetAllocation 목표 자산 배분 (symbol -> target percentage)
     * @return 리밸런싱 필요 여부
     */
    boolean needsRebalancing(Portfolio portfolio, Map<String, Double> targetAllocation);

    /**
     * 리밸런싱 추천안 생성
     *
     * @param portfolio 현재 포트폴리오
     * @param targetAllocation 목표 자산 배분 (symbol -> target percentage)
     * @return 리밸런싱 추천안
     */
    RebalancingRecommendationDto generateRecommendation(Portfolio portfolio, Map<String, Double> targetAllocation);

    /**
     * 전략별 설정값 검증
     *
     * @param configuration 전략 설정값
     * @return 유효성 검증 결과
     */
    boolean validateConfiguration(Map<String, Object> configuration);
}