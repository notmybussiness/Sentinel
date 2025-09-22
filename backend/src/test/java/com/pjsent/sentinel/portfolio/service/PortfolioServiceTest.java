package com.pjsent.sentinel.portfolio.service;

import com.pjsent.sentinel.market.dto.StockPriceDto;
import com.pjsent.sentinel.market.service.MarketDataService;
import com.pjsent.sentinel.portfolio.dto.*;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.repository.PortfolioHoldingRepository;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PortfolioService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioHoldingRepository holdingRepository;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private PortfolioService portfolioService;

    private Long userId;
    private Long portfolioId;
    private Portfolio portfolio;
    private PortfolioHolding holding;

    @BeforeEach
    void setUp() {
        userId = 1L;
        portfolioId = 1L;
        
        portfolio = Portfolio.builder()
                .userId(userId)
                .name("테스트 포트폴리오")
                .description("테스트용 포트폴리오")
                .build();
        
        // Reflection을 사용하여 id 설정
        try {
            java.lang.reflect.Field idField = Portfolio.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(portfolio, portfolioId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set portfolio id", e);
        }

        holding = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .averageCost(new BigDecimal("150.00"))
                .build();
    }

    @Test
    @DisplayName("사용자의 모든 포트폴리오 조회 성공")
    void should_ReturnPortfolioList_When_GetPortfoliosByUserId() {
        // Given
        when(portfolioRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(portfolio));

        // When
        List<PortfolioDto> result = portfolioService.getPortfoliosByUserId(userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("테스트 포트폴리오");
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("특정 포트폴리오 조회 성공")
    void should_ReturnPortfolio_When_GetPortfolioById() {
        // Given
        when(portfolioRepository.findByIdAndUserId(portfolioId, userId))
                .thenReturn(Optional.of(portfolio));

        // When
        PortfolioDto result = portfolioService.getPortfolioById(portfolioId, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(portfolioId);
        assertThat(result.getName()).isEqualTo("테스트 포트폴리오");
    }

    @Test
    @DisplayName("존재하지 않는 포트폴리오 조회 시 예외 발생")
    void should_ThrowException_When_PortfolioNotFound() {
        // Given
        when(portfolioRepository.findByIdAndUserId(portfolioId, userId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> portfolioService.getPortfolioById(portfolioId, userId))

        .isInstanceOf(com.pjsent.sentinel.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("포트폴리오");
    }

    @Test
    @DisplayName("포트폴리오 생성 성공")
    void should_CreatePortfolio_When_ValidRequest() {
        // Given
        CreatePortfolioRequest request = new CreatePortfolioRequest();
        request.setName("새 포트폴리오");
        request.setDescription("새로운 포트폴리오");

        when(portfolioRepository.existsByUserIdAndName(userId, request.getName()))
                .thenReturn(false);
        when(portfolioRepository.save(any(Portfolio.class)))
                .thenReturn(portfolio);

        // When
        PortfolioDto result = portfolioService.createPortfolio(userId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 포트폴리오");
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("중복된 포트폴리오 이름으로 생성 시 예외 발생")
    void should_ThrowException_When_DuplicatePortfolioName() {
        // Given
        CreatePortfolioRequest request = new CreatePortfolioRequest();
        request.setName("중복 포트폴리오");

        when(portfolioRepository.existsByUserIdAndName(userId, request.getName()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> portfolioService.createPortfolio(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 포트폴리오 이름입니다");
    }

    @Test
    @DisplayName("포트폴리오 수정 성공")
    void should_UpdatePortfolio_When_ValidRequest() {
        // Given
        UpdatePortfolioRequest request = new UpdatePortfolioRequest();
        request.setName("수정된 포트폴리오");
        request.setDescription("수정된 설명");

        when(portfolioRepository.findByIdAndUserId(portfolioId, userId))
                .thenReturn(Optional.of(portfolio));
        when(portfolioRepository.existsByUserIdAndName(userId, request.getName()))
                .thenReturn(false);
        when(portfolioRepository.save(any(Portfolio.class)))
                .thenReturn(portfolio);

        // When
        PortfolioDto result = portfolioService.updatePortfolio(portfolioId, userId, request);

        // Then
        assertThat(result).isNotNull();
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("포트폴리오 삭제 성공")
    void should_DeletePortfolio_When_ValidPortfolioId() {
        // Given
        when(portfolioRepository.findByIdAndUserId(portfolioId, userId))
                .thenReturn(Optional.of(portfolio));

        // When
        portfolioService.deletePortfolio(portfolioId, userId);

        // Then
        verify(portfolioRepository).delete(portfolio);
    }

    @Test
    @DisplayName("보유 종목 추가 성공")
    void should_AddHolding_When_ValidRequest() {
        // Given
        AddHoldingRequest request = new AddHoldingRequest();
        request.setSymbol("AAPL");
        request.setQuantity(new BigDecimal("10"));
        request.setAverageCost(new BigDecimal("150.00"));

        StockPriceDto stockPrice = StockPriceDto.builder()
                .symbol("AAPL")
                .price(160.0)
                .build();

        when(portfolioRepository.findByIdAndUserId(portfolioId, userId))
                .thenReturn(Optional.of(portfolio));
        when(holdingRepository.existsByPortfolioIdAndSymbol(portfolioId, request.getSymbol()))
                .thenReturn(false);
        when(marketDataService.getStockPrice(request.getSymbol()))
                .thenReturn(stockPrice);
        when(holdingRepository.save(any(PortfolioHolding.class)))
                .thenReturn(holding);

        // When
        PortfolioHoldingDto result = portfolioService.addHolding(portfolioId, userId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSymbol()).isEqualTo("AAPL");
        verify(holdingRepository).save(any(PortfolioHolding.class));
    }

    @Test
    @DisplayName("중복된 보유 종목 추가 시 예외 발생")
    void should_ThrowException_When_DuplicateHolding() {
        // Given
        AddHoldingRequest request = new AddHoldingRequest();
        request.setSymbol("AAPL");
        request.setQuantity(new BigDecimal("10"));
        request.setAverageCost(new BigDecimal("150.00"));

        when(portfolioRepository.findByIdAndUserId(portfolioId, userId))
                .thenReturn(Optional.of(portfolio));
        when(holdingRepository.existsByPortfolioIdAndSymbol(portfolioId, request.getSymbol()))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> portfolioService.addHolding(portfolioId, userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 보유 종목입니다");
    }

    @Test
    @DisplayName("보유 종목 수정 성공")
    void should_UpdateHolding_When_ValidRequest() {
        // Given
        UpdateHoldingRequest request = new UpdateHoldingRequest();
        request.setQuantity(new BigDecimal("20"));
        request.setAverageCost(new BigDecimal("155.00"));

        when(portfolioRepository.findByIdAndUserId(portfolioId, userId))
                .thenReturn(Optional.of(portfolio));
        when(holdingRepository.findById(holding.getId()))
                .thenReturn(Optional.of(holding));
        when(holdingRepository.save(any(PortfolioHolding.class)))
                .thenReturn(holding);

        // When
        PortfolioHoldingDto result = portfolioService.updateHolding(portfolioId, holding.getId(), userId, request);

        // Then
        assertThat(result).isNotNull();
        verify(holdingRepository).save(any(PortfolioHolding.class));
    }

    @Test
    @DisplayName("보유 종목 삭제 성공")
    void should_DeleteHolding_When_ValidHoldingId() {
        // Given
        when(portfolioRepository.findByIdAndUserId(portfolioId, userId))
                .thenReturn(Optional.of(portfolio));
        when(holdingRepository.findById(holding.getId()))
                .thenReturn(Optional.of(holding));

        // When
        portfolioService.deleteHolding(portfolioId, holding.getId(), userId);

        // Then
        verify(holdingRepository).delete(holding);
    }

    @Test
    @DisplayName("포트폴리오 재계산 성공")
    void should_RecalculatePortfolio_When_ValidPortfolioId() {
        // Given
        portfolio.getHoldings().add(holding);
        
        StockPriceDto stockPrice = StockPriceDto.builder()
                .symbol("AAPL")
                .price(160.0)
                .build();

        when(portfolioRepository.findByIdAndUserId(portfolioId, userId))
                .thenReturn(Optional.of(portfolio));
        when(marketDataService.getStockPrice(holding.getSymbol()))
                .thenReturn(stockPrice);
        when(portfolioRepository.save(any(Portfolio.class)))
                .thenReturn(portfolio);

        // When
        PortfolioDto result = portfolioService.recalculatePortfolio(portfolioId, userId);

        // Then
        assertThat(result).isNotNull();
        verify(portfolioRepository).save(any(Portfolio.class));
    }
}
