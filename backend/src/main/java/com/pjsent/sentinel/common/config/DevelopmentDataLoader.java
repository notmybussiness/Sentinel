package com.pjsent.sentinel.common.config;

import com.pjsent.sentinel.market.entity.MarketData;
import com.pjsent.sentinel.market.repository.MarketDataRepository;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.repository.PortfolioHoldingRepository;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import com.pjsent.sentinel.user.entity.User;
import com.pjsent.sentinel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 개발 환경용 테스트 데이터 로더
 * H2 데이터베이스에 초기 테스트 데이터를 생성합니다.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevelopmentDataLoader implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioHoldingRepository portfolioHoldingRepository;
    private final MarketDataRepository marketDataRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("개발 환경 테스트 데이터를 로딩합니다...");

        // 기존 데이터가 있으면 로딩하지 않음
        if (userRepository.count() > 0) {
            log.info("이미 데이터가 존재합니다. 테스트 데이터 로딩을 스킵합니다.");
            return;
        }

        // 1. 테스트 사용자 생성
        User testUser = User.builder()
                .kakaoId("test_kakao_id")
                .email("test@sentinel.com")
                .name("Test User")
                .profileImageUrl("https://example.com/avatar.jpg")
                .build();
        testUser = userRepository.save(testUser);
        log.info("테스트 사용자 생성: ID={}, Email={}", testUser.getId(), testUser.getEmail());

        // 2. 테스트 포트폴리오 생성
        Portfolio testPortfolio = Portfolio.builder()
                .userId(testUser.getId())
                .name("Test Portfolio")
                .description("Development test portfolio")
                .build();
        testPortfolio = portfolioRepository.save(testPortfolio);
        log.info("테스트 포트폴리오 생성: ID={}, Name={}", testPortfolio.getId(), testPortfolio.getName());

        // 3. 테스트 보유 종목 생성 (리밸런싱 데모용)
        createHolding(testPortfolio, "AAPL", 100, 150.00, 175.50);
        createHolding(testPortfolio, "MSFT", 50, 280.00, 320.75);
        createHolding(testPortfolio, "GOOGL", 30, 2800.00, 2650.25);
        createHolding(testPortfolio, "TSLA", 25, 220.00, 185.30);
        createHolding(testPortfolio, "NVDA", 15, 450.00, 520.80);

        // 4. 테스트 시장 데이터 생성
        createMarketData("AAPL", 175.50, 2.35);
        createMarketData("MSFT", 320.75, 1.84);
        createMarketData("GOOGL", 2650.25, -0.75);
        createMarketData("TSLA", 185.30, -3.25);
        createMarketData("NVDA", 520.80, 4.12);

        log.info("개발 환경 테스트 데이터 로딩 완료!");
        log.info("H2 Console: http://localhost:8080/h2-console");
        log.info("데이터베이스 URL: jdbc:h2:mem:testdb");
        log.info("사용자명: sa, 비밀번호: (비어있음)");
    }

    private void createHolding(Portfolio portfolio, String symbol, int quantity,
                               double averageCost, double currentPrice) {
        BigDecimal quantityBD = BigDecimal.valueOf(quantity);
        BigDecimal averageCostBD = BigDecimal.valueOf(averageCost);
        BigDecimal currentPriceBD = BigDecimal.valueOf(currentPrice);
        BigDecimal totalCost = quantityBD.multiply(averageCostBD);
        BigDecimal marketValue = quantityBD.multiply(currentPriceBD);
        BigDecimal gainLoss = marketValue.subtract(totalCost);
        BigDecimal gainLossPercent = gainLoss.divide(totalCost, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));

        PortfolioHolding holding = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol(symbol)
                .quantity(quantityBD)
                .averageCost(averageCostBD)
                .build();

        // 현재 가격 설정 (이후 계산된 값들 자동 업데이트)
        holding.updateCurrentPrice(currentPriceBD);

        portfolioHoldingRepository.save(holding);
        log.info("보유 종목 생성: {} x{} @ ${} (현재가: ${})", symbol, quantity, averageCost, currentPrice);
    }

    private void createMarketData(String symbol, double price, double changePercent) {
        MarketData marketData = MarketData.builder()
                .symbol(symbol)
                .price(BigDecimal.valueOf(price))
                .changePercent(BigDecimal.valueOf(changePercent))
                .high24h(BigDecimal.valueOf(price * 1.02))
                .low24h(BigDecimal.valueOf(price * 0.98))
                .marketCap(1000000000L)
                .volume(50000000L)
                .dataSource("TEST_PROVIDER")
                .build();

        marketDataRepository.save(marketData);
        log.info("시장 데이터 생성: {} @ ${} ({}%)", symbol, price, changePercent);
    }
}