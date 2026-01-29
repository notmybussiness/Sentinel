package com.pjsent.sentinel.rebalancing.service;

import com.pjsent.sentinel.common.exception.BusinessException;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import com.pjsent.sentinel.market.service.MarketDataService;
import com.pjsent.sentinel.portfolio.dto.PortfolioDto;
import com.pjsent.sentinel.portfolio.dto.PortfolioHoldingDto;
import com.pjsent.sentinel.portfolio.service.PortfolioService;
import com.pjsent.sentinel.rebalancing.dto.RebalancingRequest;
import com.pjsent.sentinel.rebalancing.dto.RebalancingResponse;
import com.pjsent.sentinel.rebalancing.dto.RebalancingStrategy;
import com.pjsent.sentinel.pricehistory.dto.ChartDataDto;
import com.pjsent.sentinel.pricehistory.service.PriceHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * RebalancingService 단위 테스트
 *
 * TDD Cycle:
 * [RED] 테스트 먼저 작성 → Rebalancing 로직 미구현 시 실패
 * [GREEN] RebalancingService 구현으로 통과
 * [REFACTOR] Equal Weight 알고리즘 개선
 *
 * 테스트 범위:
 * - generateRecommendations() - Equal Weight 전략
 * - 비즈니스 규칙 검증 (최소 2개 holdings, 지원 전략)
 * - 수수료 계산 로직
 * - 세금 영향 계산
 */
@ExtendWith(MockitoExtension.class)
class RebalancingServiceTest {

        @Mock
        private PortfolioService portfolioService;

        @Mock
        private MarketDataService marketDataService;

        @Mock
        private CryptoDataService cryptoDataService;

        @Mock
        private PriceHistoryService priceHistoryService;

        @InjectMocks
        private RebalancingService rebalancingService;

        private static final Long TEST_USER_ID = 1L;
        private static final Long TEST_PORTFOLIO_ID = 100L;

        // ==================== generateRecommendations() 테스트 ====================

        @Test
        @DisplayName("[성공] Equal Weight 리밸런싱 추천 생성 - 2개 holdings")
        void generateRecommendations_should_return_equal_weight_recommendations() {
                // Given
                RebalancingRequest request = new RebalancingRequest();
                request.setPortfolioId(TEST_PORTFOLIO_ID);
                request.setStrategy(RebalancingStrategy.EQUAL_WEIGHT);
                request.setThresholdPercent(5.0); // 5% threshold
                request.setConsiderTaxes(false);

                PortfolioDto mockPortfolio = createDummyPortfolio(
                                TEST_PORTFOLIO_ID,
                                "테스트 포트폴리오",
                                List.of(
                                                createDummyHolding("AAPL", 10.0, 150.0, 1500.0), // 현재 가치: $1,500
                                                createDummyHolding("TSLA", 5.0, 200.0, 1000.0) // 현재 가치: $1,000
                                ));

                when(portfolioService.getPortfolioById(eq(TEST_PORTFOLIO_ID), eq(TEST_USER_ID)))
                                .thenReturn(mockPortfolio);

                // When
                RebalancingResponse response = rebalancingService.generateRecommendations(TEST_USER_ID, request);

                // Then
                assertThat(response).isNotNull();
                assertThat(response.getPortfolioId()).isEqualTo(TEST_PORTFOLIO_ID);
                assertThat(response.getStrategy()).isEqualTo("EQUAL_WEIGHT");
                assertThat(response.getCurrentValue()).isEqualTo(2500.0); // Total: $2,500
                assertThat(response.getRecommendations()).hasSize(2);

                // Equal Weight: 각 종목 50%씩 (= $1,250)
                // AAPL: 현재 60% ($1,500) → 목표 50% ($1,250) = SELL
                // TSLA: 현재 40% ($1,000) → 목표 50% ($1,250) = BUY
                assertThat(response.getNeedsRebalancing()).isTrue();
                assertThat(response.getTotalTransactionCost()).isGreaterThan(0);
        }

        @Test
        @DisplayName("[실패] Holdings가 2개 미만이면 BusinessException 발생")
        void generateRecommendations_should_throw_exception_when_less_than_2_holdings() {
                // Given
                RebalancingRequest request = new RebalancingRequest();
                request.setPortfolioId(TEST_PORTFOLIO_ID);
                request.setStrategy(RebalancingStrategy.EQUAL_WEIGHT);

                PortfolioDto mockPortfolio = createDummyPortfolio(
                                TEST_PORTFOLIO_ID,
                                "포트폴리오",
                                List.of(createDummyHolding("AAPL", 10.0, 150.0, 1500.0)) // 1개만
                );

                when(portfolioService.getPortfolioById(eq(TEST_PORTFOLIO_ID), eq(TEST_USER_ID)))
                                .thenReturn(mockPortfolio);

                // When & Then
                assertThatThrownBy(() -> rebalancingService.generateRecommendations(TEST_USER_ID, request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("at least 2 holdings");
        }

        @Test
        @DisplayName("[실패] 지원하지 않는 전략이면 BusinessException 발생")
        void generateRecommendations_should_throw_exception_when_strategy_not_supported() {
                // Given
                RebalancingRequest request = new RebalancingRequest();
                request.setPortfolioId(TEST_PORTFOLIO_ID);
                request.setStrategy(RebalancingStrategy.DIVIDEND_FOCUSED); // Not supported

                PortfolioDto mockPortfolio = createDummyPortfolio(
                                TEST_PORTFOLIO_ID,
                                "포트폴리오",
                                List.of(
                                                createDummyHolding("AAPL", 10.0, 150.0, 1500.0),
                                                createDummyHolding("TSLA", 5.0, 200.0, 1000.0)));

                when(portfolioService.getPortfolioById(eq(TEST_PORTFOLIO_ID), eq(TEST_USER_ID)))
                                .thenReturn(mockPortfolio);

                // When & Then
                assertThatThrownBy(() -> rebalancingService.generateRecommendations(TEST_USER_ID, request))
                                .isInstanceOf(BusinessException.class)
                                .hasMessageContaining("not yet supported");
        }

        @Test
        @DisplayName("[HOLD] Threshold 이내이면 리밸런싱 불필요")
        void generateRecommendations_should_recommend_hold_when_within_threshold() {
                // Given
                RebalancingRequest request = new RebalancingRequest();
                request.setPortfolioId(TEST_PORTFOLIO_ID);
                request.setStrategy(RebalancingStrategy.EQUAL_WEIGHT);
                request.setThresholdPercent(10.0); // 10% threshold (큰 값)

                // 거의 균등한 포트폴리오 (각각 49%, 51% = threshold 10% 이내)
                PortfolioDto mockPortfolio = createDummyPortfolio(
                                TEST_PORTFOLIO_ID,
                                "포트폴리오",
                                List.of(
                                                createDummyHolding("AAPL", 10.0, 150.0, 1225.0), // 49%
                                                createDummyHolding("TSLA", 10.0, 127.5, 1275.0) // 51%
                                ));

                when(portfolioService.getPortfolioById(eq(TEST_PORTFOLIO_ID), eq(TEST_USER_ID)))
                                .thenReturn(mockPortfolio);

                // When
                RebalancingResponse response = rebalancingService.generateRecommendations(TEST_USER_ID, request);

                // Then
                assertThat(response.getNeedsRebalancing()).isFalse(); // Threshold 이내
                assertThat(response.getTotalTransactionCost()).isEqualTo(0.0);

                response.getRecommendations().forEach(rec -> assertThat(rec.getAction()).isEqualTo("HOLD"));
        }

        @Test
        @DisplayName("[성공] 세금 고려 옵션 활성화 시 세금 영향 계산")
        void generateRecommendations_should_calculate_tax_impact_when_enabled() {
                // Given
                RebalancingRequest request = new RebalancingRequest();
                request.setPortfolioId(TEST_PORTFOLIO_ID);
                request.setStrategy(RebalancingStrategy.EQUAL_WEIGHT);
                request.setThresholdPercent(5.0);
                request.setConsiderTaxes(true); // 세금 고려

                // Gain이 있는 holding (SELL 시 세금 발생)
                PortfolioHoldingDto holdingWithGain = PortfolioHoldingDto.builder()
                                .symbol("AAPL")
                                .assetType("STOCK")
                                .quantity(new BigDecimal("10.0"))
                                .averageCost(new BigDecimal("150.0"))
                                .marketValue(new BigDecimal("1800.0"))
                                .currentPrice(new BigDecimal("180.0"))
                                .gainLoss(new BigDecimal("300.0")) // $300 이익
                                .baseCurrency("USD")
                                .build();

                PortfolioDto mockPortfolio = createDummyPortfolio(
                                TEST_PORTFOLIO_ID,
                                "포트폴리오",
                                List.of(
                                                holdingWithGain,
                                                createDummyHolding("TSLA", 5.0, 200.0, 700.0)));

                when(portfolioService.getPortfolioById(eq(TEST_PORTFOLIO_ID), eq(TEST_USER_ID)))
                                .thenReturn(mockPortfolio);

                // When
                RebalancingResponse response = rebalancingService.generateRecommendations(TEST_USER_ID, request);

                // Then
                // AAPL은 72% → 50%로 SELL 필요 (이익 실현 → 세금 20%)
                assertThat(response.getEstimatedTaxImpact()).isGreaterThan(0);
        }

        // ==================== 🛠️ Helper Methods ====================

        /**
         * PortfolioDto Mock 데이터 생성
         */
        private PortfolioDto createDummyPortfolio(Long id, String name, List<PortfolioHoldingDto> holdings) {
                List<PortfolioHoldingDto> portfolioHoldings = holdings != null ? holdings : new ArrayList<>();

                // Total value 계산
                double totalValue = portfolioHoldings.stream()
                                .mapToDouble(h -> h.getMarketValue().doubleValue())
                                .sum();

                return PortfolioDto.builder()
                                .id(id)
                                .userId(TEST_USER_ID)
                                .name(name)
                                .holdings(portfolioHoldings)
                                .totalValue(new BigDecimal(totalValue))
                                .build();
        }

        /**
         * PortfolioHoldingDto Mock 데이터 생성
         */
        private PortfolioHoldingDto createDummyHolding(String symbol, double quantity, double avgCost,
                        double marketValue) {
                return PortfolioHoldingDto.builder()
                                .symbol(symbol)
                                .assetType("STOCK")
                                .quantity(new BigDecimal(quantity))
                                .averageCost(new BigDecimal(avgCost))
                                .marketValue(new BigDecimal(marketValue))
                                .currentPrice(new BigDecimal(marketValue / quantity)) // 현재가 = 시장가치 / 수량
                                .gainLoss(BigDecimal.ZERO) // Default
                                .baseCurrency("USD")
                                .build();
        }

        // ==================== 고급 전략 테스트 (TARGET_ALLOCATION, RISK_PARITY, MOMENTUM)
        // ====================

        @Test
        @DisplayName("[성공] TARGET_ALLOCATION 전략 - 사용자 지정 비중 준수")
        void generateRecommendations_should_follow_target_allocation() {
                // Given
                RebalancingRequest request = new RebalancingRequest();
                request.setPortfolioId(TEST_PORTFOLIO_ID);
                request.setStrategy(RebalancingStrategy.TARGET_ALLOCATION);
                request.setTargetAllocations(Map.of("AAPL", 80.0, "TSLA", 20.0));
                request.setThresholdPercent(2.0);

                PortfolioDto mockPortfolio = createDummyPortfolio(
                                TEST_PORTFOLIO_ID,
                                "포트폴리오",
                                List.of(
                                                createDummyHolding("AAPL", 10.0, 150.0, 1500.0), // 60%
                                                createDummyHolding("TSLA", 5.0, 200.0, 1000.0) // 40%
                                ));

                when(portfolioService.getPortfolioById(eq(TEST_PORTFOLIO_ID), eq(TEST_USER_ID)))
                                .thenReturn(mockPortfolio);

                // When
                RebalancingResponse response = rebalancingService.generateRecommendations(TEST_USER_ID, request);

                // Then
                assertThat(response.getStrategy()).isEqualTo("TARGET_ALLOCATION");

                // AAPL: 60% -> 80% (BUY)
                // TSLA: 40% -> 20% (SELL)
                assertThat(response.getRecommendations())
                                .extracting("symbol")
                                .contains("AAPL", "TSLA");

                var appleRec = response.getRecommendations().stream().filter(r -> r.getSymbol().equals("AAPL"))
                                .findFirst().get();
                assertThat(appleRec.getAction()).isEqualTo("BUY");
                assertThat(appleRec.getTargetWeight()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("[성공] RISK_PARITY 전략 - 변동성 낮은 자산에 비중 확대")
        void generateRecommendations_should_assign_higher_weight_to_low_volatility() {
                // Given
                RebalancingRequest request = new RebalancingRequest();
                request.setPortfolioId(TEST_PORTFOLIO_ID);
                request.setStrategy(RebalancingStrategy.RISK_PARITY);
                request.setLookbackDays(30);

                PortfolioDto mockPortfolio = createDummyPortfolio(
                                TEST_PORTFOLIO_ID,
                                "포트폴리오",
                                List.of(
                                                createDummyHolding("LOW_VOL", 10.0, 100.0, 1000.0),
                                                createDummyHolding("HIGH_VOL", 10.0, 100.0, 1000.0)));

                when(portfolioService.getPortfolioById(eq(TEST_PORTFOLIO_ID), eq(TEST_USER_ID)))
                                .thenReturn(mockPortfolio);

                // Mock PriceHistory
                // Low Volatility Chart (작은 변동)
                when(priceHistoryService.getChartData(eq("LOW_VOL"), any(), any()))
                                .thenReturn(createDummyChartData(100.0, 101.0, 100.5, 99.5, 100.0));

                // High Volatility Chart (큰 변동)
                when(priceHistoryService.getChartData(eq("HIGH_VOL"), any(), any()))
                                .thenReturn(createDummyChartData(100.0, 120.0, 80.0, 130.0, 70.0));

                // When
                RebalancingResponse response = rebalancingService.generateRecommendations(TEST_USER_ID, request);

                // Then
                var lowVolRec = response.getRecommendations().stream().filter(r -> r.getSymbol().equals("LOW_VOL"))
                                .findFirst().get();
                var highVolRec = response.getRecommendations().stream().filter(r -> r.getSymbol().equals("HIGH_VOL"))
                                .findFirst().get();

                // 저변동성 자산 비중이 더 높아야 함
                assertThat(lowVolRec.getTargetWeight()).isGreaterThan(highVolRec.getTargetWeight());
        }

        @Test
        @DisplayName("[성공] MOMENTUM 전략 - 수익률 높은 자산에 비중 확대")
        void generateRecommendations_should_assign_higher_weight_to_high_momentum() {
                // Given
                RebalancingRequest request = new RebalancingRequest();
                request.setPortfolioId(TEST_PORTFOLIO_ID);
                request.setStrategy(RebalancingStrategy.MOMENTUM);
                request.setLookbackDays(30);

                PortfolioDto mockPortfolio = createDummyPortfolio(
                                TEST_PORTFOLIO_ID,
                                "포트폴리오",
                                List.of(
                                                createDummyHolding("WINNER", 10.0, 100.0, 1000.0),
                                                createDummyHolding("LOSER", 10.0, 100.0, 1000.0)));

                when(portfolioService.getPortfolioById(eq(TEST_PORTFOLIO_ID), eq(TEST_USER_ID)))
                                .thenReturn(mockPortfolio);

                // Mock PriceHistory
                // Winner: 100 -> 150
                when(priceHistoryService.getChartData(eq("WINNER"), any(), any()))
                                .thenReturn(createDummyChartData(100.0, 110.0, 120.0, 130.0, 150.0));

                // Loser: 100 -> 50
                when(priceHistoryService.getChartData(eq("LOSER"), any(), any()))
                                .thenReturn(createDummyChartData(100.0, 90.0, 80.0, 70.0, 50.0));

                // When
                RebalancingResponse response = rebalancingService.generateRecommendations(TEST_USER_ID, request);

                // Then
                var winnerRec = response.getRecommendations().stream().filter(r -> r.getSymbol().equals("WINNER"))
                                .findFirst().get();
                var loserRec = response.getRecommendations().stream().filter(r -> r.getSymbol().equals("LOSER"))
                                .findFirst().get();

                // 수익률 높은 자산 비중이 더 높아야 함
                assertThat(winnerRec.getTargetWeight()).isGreaterThan(loserRec.getTargetWeight());
        }

        private ChartDataDto createDummyChartData(Double... prices) {
                ChartDataDto dto = new ChartDataDto();
                List<ChartDataDto.DataPoint> data = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now().minusDays(prices.length);

                for (Double price : prices) {
                        ChartDataDto.DataPoint point = new ChartDataDto.DataPoint();
                        point.setTime(now.toString()); // time setting depends on DTO structure, assuming simple setter
                        point.setValue(new BigDecimal(price));
                        data.add(point);
                        now = now.plusDays(1);
                }
                dto.setData(data);
                return dto;
        }
}
