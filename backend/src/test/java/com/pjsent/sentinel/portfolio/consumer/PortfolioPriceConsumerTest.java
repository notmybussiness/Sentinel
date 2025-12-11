package com.pjsent.sentinel.portfolio.consumer;

import com.pjsent.sentinel.common.event.PriceUpdateEvent;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding.AssetType;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * PortfolioPriceConsumer 단위 테스트
 * 
 * 🧢 Kent Beck: 다양한 시나리오 커버
 * 🦅 Murphy's Law: 예외 상황 및 Edge Case 검증
 */
@ExtendWith(MockitoExtension.class)
class PortfolioPriceConsumerTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @InjectMocks
    private PortfolioPriceConsumer portfolioPriceConsumer;

    @Nested
    @DisplayName("정상 처리 케이스")
    class SuccessCases {

        @Test
        @DisplayName("가격 업데이트 이벤트를 수신하면 관련 포트폴리오를 업데이트한다")
        void handlePriceUpdate_Success() {
            // given
            String symbol = "AAPL";
            BigDecimal newPrice = BigDecimal.valueOf(155.0);
            PriceUpdateEvent event = new PriceUpdateEvent(symbol, newPrice, AssetType.STOCK, LocalDateTime.now());

            Portfolio portfolio = mock(Portfolio.class);
            PortfolioHolding holding = mock(PortfolioHolding.class);

            given(holding.getSymbol()).willReturn(symbol);
            given(holding.getAssetType()).willReturn(AssetType.STOCK);
            given(portfolio.getHoldings()).willReturn(List.of(holding));
            given(portfolioRepository.findByHoldings_Symbol(symbol)).willReturn(List.of(portfolio));

            // when
            portfolioPriceConsumer.handlePriceUpdate(event);

            // then
            verify(holding).updateCurrentPrice(newPrice);
            verify(portfolio).recalculate();
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("다중 포트폴리오가 같은 심볼을 보유한 경우 모두 업데이트한다")
        void handlePriceUpdate_MultiplePortfolios() {
            // given
            String symbol = "AAPL";
            BigDecimal newPrice = BigDecimal.valueOf(160.0);
            PriceUpdateEvent event = new PriceUpdateEvent(symbol, newPrice, AssetType.STOCK, LocalDateTime.now());

            Portfolio portfolio1 = mock(Portfolio.class);
            Portfolio portfolio2 = mock(Portfolio.class);
            PortfolioHolding holding1 = mock(PortfolioHolding.class);
            PortfolioHolding holding2 = mock(PortfolioHolding.class);

            given(holding1.getSymbol()).willReturn(symbol);
            given(holding1.getAssetType()).willReturn(AssetType.STOCK);
            given(holding2.getSymbol()).willReturn(symbol);
            given(holding2.getAssetType()).willReturn(AssetType.STOCK);
            given(portfolio1.getHoldings()).willReturn(List.of(holding1));
            given(portfolio2.getHoldings()).willReturn(List.of(holding2));
            given(portfolioRepository.findByHoldings_Symbol(symbol)).willReturn(List.of(portfolio1, portfolio2));

            // when
            portfolioPriceConsumer.handlePriceUpdate(event);

            // then
            verify(holding1).updateCurrentPrice(newPrice);
            verify(holding2).updateCurrentPrice(newPrice);
            verify(portfolio1).recalculate();
            verify(portfolio2).recalculate();
            verify(portfolioRepository, times(2)).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("CRYPTO 타입 이벤트를 올바르게 처리한다")
        void handlePriceUpdate_CryptoType() {
            // given
            String symbol = "BTC";
            BigDecimal newPrice = new BigDecimal("45000.50");
            PriceUpdateEvent event = new PriceUpdateEvent(symbol, newPrice, AssetType.CRYPTO, LocalDateTime.now());

            Portfolio portfolio = mock(Portfolio.class);
            PortfolioHolding holding = mock(PortfolioHolding.class);

            given(holding.getSymbol()).willReturn(symbol);
            given(holding.getAssetType()).willReturn(AssetType.CRYPTO);
            given(portfolio.getHoldings()).willReturn(List.of(holding));
            given(portfolioRepository.findByHoldings_Symbol(symbol)).willReturn(List.of(portfolio));

            // when
            portfolioPriceConsumer.handlePriceUpdate(event);

            // then
            verify(holding).updateCurrentPrice(newPrice);
            verify(portfolio).recalculate();
            verify(portfolioRepository).save(portfolio);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("해당 심볼을 보유한 포트폴리오가 없으면 아무것도 하지 않는다")
        void handlePriceUpdate_NoPortfolio() {
            // given
            String symbol = "UNKNOWN";
            PriceUpdateEvent event = new PriceUpdateEvent(symbol, BigDecimal.valueOf(100.0), AssetType.STOCK,
                    LocalDateTime.now());

            given(portfolioRepository.findByHoldings_Symbol(symbol)).willReturn(Collections.emptyList());

            // when
            portfolioPriceConsumer.handlePriceUpdate(event);

            // then
            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("심볼은 같지만 AssetType이 다르면 업데이트하지 않는다")
        void handlePriceUpdate_AssetTypeMismatch() {
            // given - 같은 심볼이지만 STOCK으로 요청, 실제는 CRYPTO
            String symbol = "ETH";
            BigDecimal newPrice = BigDecimal.valueOf(3000.0);
            PriceUpdateEvent event = new PriceUpdateEvent(symbol, newPrice, AssetType.STOCK, LocalDateTime.now());

            Portfolio portfolio = mock(Portfolio.class);
            PortfolioHolding holding = mock(PortfolioHolding.class);

            given(holding.getSymbol()).willReturn(symbol);
            given(holding.getAssetType()).willReturn(AssetType.CRYPTO); // 타입 불일치
            given(portfolio.getHoldings()).willReturn(List.of(holding));
            given(portfolioRepository.findByHoldings_Symbol(symbol)).willReturn(List.of(portfolio));

            // when
            portfolioPriceConsumer.handlePriceUpdate(event);

            // then - 가격 업데이트 안됨, 저장도 안됨
            verify(holding, never()).updateCurrentPrice(any());
            verify(portfolio, never()).recalculate();
            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("포트폴리오 내 여러 Holding 중 일부만 매칭되는 경우")
        void handlePriceUpdate_PartialMatch() {
            // given
            String symbol = "AAPL";
            BigDecimal newPrice = BigDecimal.valueOf(155.0);
            PriceUpdateEvent event = new PriceUpdateEvent(symbol, newPrice, AssetType.STOCK, LocalDateTime.now());

            Portfolio portfolio = mock(Portfolio.class);
            PortfolioHolding holdingAAPL = mock(PortfolioHolding.class);
            PortfolioHolding holdingGOOG = mock(PortfolioHolding.class);

            given(holdingAAPL.getSymbol()).willReturn("AAPL");
            given(holdingAAPL.getAssetType()).willReturn(AssetType.STOCK);
            given(holdingGOOG.getSymbol()).willReturn("GOOG");
            // holdingGOOG.getAssetType()은 호출되지 않음 (심볼이 다르므로)
            given(portfolio.getHoldings()).willReturn(List.of(holdingAAPL, holdingGOOG));
            given(portfolioRepository.findByHoldings_Symbol(symbol)).willReturn(List.of(portfolio));

            // when
            portfolioPriceConsumer.handlePriceUpdate(event);

            // then - AAPL만 업데이트, GOOG는 그대로
            verify(holdingAAPL).updateCurrentPrice(newPrice);
            verify(holdingGOOG, never()).updateCurrentPrice(any());
            verify(portfolio).recalculate();
            verify(portfolioRepository).save(portfolio);
        }
    }
}
