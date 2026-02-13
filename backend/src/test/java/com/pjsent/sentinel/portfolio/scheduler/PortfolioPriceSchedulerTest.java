package com.pjsent.sentinel.portfolio.scheduler;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import com.pjsent.sentinel.crypto.service.CryptoDataService;
import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.MarketDataService;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding.AssetType;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * PortfolioPriceScheduler 단위 테스트
 * 
 * 🧢 Kent Beck: TDD 원칙 준수
 * 🦁 SRE: Fallback 스케줄러의 안정성 검증
 * 🦅 Murphy's Law: 외부 API 실패 시 복원력 검증
 */
@ExtendWith(MockitoExtension.class)
class PortfolioPriceSchedulerTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private CryptoDataService cryptoDataService;

    @InjectMocks
    private PortfolioPriceScheduler scheduler;

    private Portfolio portfolio;
    private PortfolioHolding stockHolding;
    private PortfolioHolding cryptoHolding;

    @BeforeEach
    void setUp() {
        portfolio = mock(Portfolio.class);
        stockHolding = mock(PortfolioHolding.class);
        cryptoHolding = mock(PortfolioHolding.class);

        lenient().when(stockHolding.getSymbol()).thenReturn("AAPL");
        lenient().when(stockHolding.getAssetType()).thenReturn(AssetType.STOCK);

        lenient().when(cryptoHolding.getSymbol()).thenReturn("BTC");
        lenient().when(cryptoHolding.getAssetType()).thenReturn(AssetType.CRYPTO);
        lenient().when(cryptoHolding.getBaseCurrency()).thenReturn("USD");
    }

    @Nested
    @DisplayName("정상 처리 케이스")
    class SuccessCases {

        @Test
        @DisplayName("모든 포트폴리오의 가격을 성공적으로 업데이트한다")
        void updateAllPortfolioPrices_Success() {
            // given
            List<PortfolioHolding> holdings = new ArrayList<>();
            holdings.add(stockHolding);
            given(portfolio.getHoldings()).willReturn(holdings);
            given(portfolioRepository.findAll()).willReturn(List.of(portfolio));

            StockPriceDto stockPrice = StockPriceDto.builder()
                    .symbol("AAPL")
                    .price(150.0)
                    .build();
            given(marketDataService.getStockPrice("AAPL")).willReturn(stockPrice);

            // when
            scheduler.updateAllPortfolioPrices();

            // then
            verify(stockHolding).updateCurrentPrice(BigDecimal.valueOf(150.0));
            verify(portfolio).recalculate();
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("STOCK과 CRYPTO 혼합 포트폴리오를 처리한다")
        void updateAllPortfolioPrices_MixedAssets() {
            // given
            List<PortfolioHolding> holdings = new ArrayList<>();
            holdings.add(stockHolding);
            holdings.add(cryptoHolding);
            given(portfolio.getHoldings()).willReturn(holdings);
            given(portfolioRepository.findAll()).willReturn(List.of(portfolio));

            StockPriceDto stockPrice = StockPriceDto.builder()
                    .symbol("AAPL")
                    .price(150.0)
                    .build();
            CryptoPriceDto cryptoPrice = CryptoPriceDto.builder()
                    .symbol("BTC")
                    .price(45000.0)
                    .build();

            given(marketDataService.getStockPrice("AAPL")).willReturn(stockPrice);
            given(cryptoDataService.getCryptoPrice("BTC", "USD")).willReturn(cryptoPrice);

            // when
            scheduler.updateAllPortfolioPrices();

            // then
            verify(stockHolding).updateCurrentPrice(BigDecimal.valueOf(150.0));
            verify(cryptoHolding).updateCurrentPrice(BigDecimal.valueOf(45000.0));
            verify(portfolio).recalculate();
            verify(portfolioRepository).save(portfolio);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("포트폴리오가 없으면 아무것도 하지 않는다")
        void updateAllPortfolioPrices_NoPortfolios() {
            // given
            given(portfolioRepository.findAll()).willReturn(Collections.emptyList());

            // when
            scheduler.updateAllPortfolioPrices();

            // then
            verify(portfolioRepository, never()).save(any());
            verify(marketDataService, never()).getStockPrice(anyString());
            verify(cryptoDataService, never()).getCryptoPrice(anyString(), anyString());
        }

        @Test
        @DisplayName("개별 Holding 업데이트 실패 시 나머지는 계속 진행한다")
        void updateAllPortfolioPrices_PartialFailure() {
            // given
            PortfolioHolding failingHolding = mock(PortfolioHolding.class);
            given(failingHolding.getSymbol()).willReturn("FAIL");
            given(failingHolding.getAssetType()).willReturn(AssetType.STOCK);

            List<PortfolioHolding> holdings = new ArrayList<>();
            holdings.add(failingHolding);
            holdings.add(stockHolding);
            given(portfolio.getHoldings()).willReturn(holdings);
            given(portfolioRepository.findAll()).willReturn(List.of(portfolio));

            // FAIL 심볼은 예외 발생
            given(marketDataService.getStockPrice("FAIL")).willThrow(new RuntimeException("API Error"));
            // AAPL은 정상
            StockPriceDto stockPrice = StockPriceDto.builder()
                    .symbol("AAPL")
                    .price(150.0)
                    .build();
            given(marketDataService.getStockPrice("AAPL")).willReturn(stockPrice);

            // when
            scheduler.updateAllPortfolioPrices();

            // then - 실패한 것은 건너뛰고 나머지 계속 진행
            verify(stockHolding).updateCurrentPrice(BigDecimal.valueOf(150.0));
            verify(portfolio).recalculate();
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("다중 포트폴리오를 순차적으로 업데이트한다")
        void updateAllPortfolioPrices_MultiplePortfolios() {
            // given
            Portfolio portfolio1 = mock(Portfolio.class);
            Portfolio portfolio2 = mock(Portfolio.class);
            PortfolioHolding holding1 = mock(PortfolioHolding.class);
            PortfolioHolding holding2 = mock(PortfolioHolding.class);

            given(holding1.getSymbol()).willReturn("AAPL");
            given(holding1.getAssetType()).willReturn(AssetType.STOCK);
            given(holding2.getSymbol()).willReturn("GOOG");
            given(holding2.getAssetType()).willReturn(AssetType.STOCK);

            List<PortfolioHolding> holdings1 = new ArrayList<>();
            holdings1.add(holding1);
            List<PortfolioHolding> holdings2 = new ArrayList<>();
            holdings2.add(holding2);

            given(portfolio1.getHoldings()).willReturn(holdings1);
            given(portfolio2.getHoldings()).willReturn(holdings2);
            given(portfolioRepository.findAll()).willReturn(List.of(portfolio1, portfolio2));

            StockPriceDto aaplPrice = StockPriceDto.builder().symbol("AAPL").price(150.0).build();
            StockPriceDto googPrice = StockPriceDto.builder().symbol("GOOG").price(2800.0).build();
            given(marketDataService.getStockPrice("AAPL")).willReturn(aaplPrice);
            given(marketDataService.getStockPrice("GOOG")).willReturn(googPrice);

            // when
            scheduler.updateAllPortfolioPrices();

            // then
            verify(holding1).updateCurrentPrice(BigDecimal.valueOf(150.0));
            verify(holding2).updateCurrentPrice(BigDecimal.valueOf(2800.0));
            verify(portfolio1).recalculate();
            verify(portfolio2).recalculate();
            verify(portfolioRepository, times(2)).save(any(Portfolio.class));
        }
    }
}
