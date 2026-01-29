package com.pjsent.sentinel.portfolio.mapper;

import com.pjsent.sentinel.portfolio.dto.PortfolioDto;
import com.pjsent.sentinel.portfolio.dto.PortfolioHoldingDto;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PortfolioMapper 단위 테스트
 * 
 * TDD Phase 3:
 * - Portfolio/PortfolioHolding → DTO 변환 테스트
 * - Null-safe 처리 검증
 */
class PortfolioMapperTest {

    private PortfolioMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PortfolioMapper();
    }

    @Nested
    @DisplayName("Portfolio → PortfolioDto 변환 테스트")
    class PortfolioToDtoTests {

        @Test
        @DisplayName("Portfolio를 PortfolioDto로 정상 변환해야 함")
        void should_convert_portfolio_to_dto() {
            // Given
            Portfolio portfolio = createTestPortfolio();

            // When
            PortfolioDto dto = mapper.toDto(portfolio);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getUserId()).isEqualTo(100L);
            assertThat(dto.getName()).isEqualTo("Test Portfolio");
            assertThat(dto.getDescription()).isEqualTo("Test Description");
            assertThat(dto.getHoldings()).hasSize(1);
        }

        @Test
        @DisplayName("null Portfolio는 null을 반환해야 함")
        void should_return_null_for_null_portfolio() {
            // When
            PortfolioDto dto = mapper.toDto(null);

            // Then
            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("빈 holdings 리스트도 정상 처리해야 함")
        void should_handle_empty_holdings_list() {
            // Given
            Portfolio portfolio = Portfolio.builder()
                    .userId(100L)
                    .name("Empty Portfolio")
                    .description("No holdings")
                    .build();
            ReflectionTestUtils.setField(portfolio, "id", 2L);
            ReflectionTestUtils.setField(portfolio, "holdings", new ArrayList<>());

            // When
            PortfolioDto dto = mapper.toDto(portfolio);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getHoldings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("PortfolioHolding → PortfolioHoldingDto 변환 테스트")
    class HoldingToDtoTests {

        @Test
        @DisplayName("PortfolioHolding을 PortfolioHoldingDto로 정상 변환해야 함")
        void should_convert_holding_to_dto() {
            // Given
            Portfolio portfolio = createTestPortfolio();
            PortfolioHolding holding = portfolio.getHoldings().get(0);

            // When
            PortfolioHoldingDto dto = mapper.toHoldingDto(holding);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getSymbol()).isEqualTo("AAPL");
            assertThat(dto.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10));
            assertThat(dto.getAssetType()).isEqualTo("STOCK");
        }

        @Test
        @DisplayName("null PortfolioHolding은 null을 반환해야 함")
        void should_return_null_for_null_holding() {
            // When
            PortfolioHoldingDto dto = mapper.toHoldingDto(null);

            // Then
            assertThat(dto).isNull();
        }
    }

    @Nested
    @DisplayName("Holdings 리스트 변환 테스트")
    class HoldingListTests {

        @Test
        @DisplayName("빈 리스트는 빈 리스트를 반환해야 함")
        void should_return_empty_list_for_empty_input() {
            // When
            List<PortfolioHoldingDto> dtos = mapper.toHoldingDtoList(Collections.emptyList());

            // Then
            assertThat(dtos).isEmpty();
        }

        @Test
        @DisplayName("null 리스트는 빈 리스트를 반환해야 함")
        void should_return_empty_list_for_null_input() {
            // When
            List<PortfolioHoldingDto> dtos = mapper.toHoldingDtoList(null);

            // Then
            assertThat(dtos).isEmpty();
        }
    }

    // Helper: 테스트용 Portfolio 생성
    private Portfolio createTestPortfolio() {
        Portfolio portfolio = Portfolio.builder()
                .userId(100L)
                .name("Test Portfolio")
                .description("Test Description")
                .build();
        ReflectionTestUtils.setField(portfolio, "id", 1L);
        ReflectionTestUtils.setField(portfolio, "totalValue", BigDecimal.valueOf(1500));
        ReflectionTestUtils.setField(portfolio, "totalCost", BigDecimal.valueOf(1000));

        PortfolioHolding holding = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("AAPL")
                .quantity(BigDecimal.valueOf(10))
                .averageCost(BigDecimal.valueOf(100))
                .assetType(AssetType.STOCK)
                .baseCurrency("USD")
                .build();
        ReflectionTestUtils.setField(holding, "id", 10L);
        ReflectionTestUtils.setField(holding, "currentPrice", BigDecimal.valueOf(150));

        List<PortfolioHolding> holdings = new ArrayList<>();
        holdings.add(holding);
        ReflectionTestUtils.setField(portfolio, "holdings", holdings);

        return portfolio;
    }
}
