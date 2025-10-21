package com.pjsent.sentinel.config;

import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import com.pjsent.sentinel.user.entity.User;
import com.pjsent.sentinel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 개발 환경 초기 데이터 생성
 * - 개발자 계정에 샘플 포트폴리오 및 암호화폐 holdings 자동 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== 초기 데이터 생성 시작 ===");

        // 개발자 계정 확인 (dev-login으로 생성된 계정)
        User devUser = userRepository.findByEmail("dev@sentinel.com").orElse(null);

        if (devUser == null) {
            log.info("개발자 계정이 없습니다. 첫 로그인 시 자동 생성됩니다.");
            return;
        }

        // 이미 포트폴리오가 있는지 확인
        if (portfolioRepository.findByUserId(devUser.getId()).stream().findAny().isPresent()) {
            log.info("개발자 계정에 이미 포트폴리오가 존재합니다. 초기화 생략.");
            return;
        }

        log.info("개발자 계정({})에 샘플 포트폴리오 생성 중...", devUser.getEmail());

        // 샘플 포트폴리오 생성
        Portfolio portfolio = createSamplePortfolio(devUser);
        portfolioRepository.save(portfolio);

        log.info("=== 초기 데이터 생성 완료 ===");
        log.info("포트폴리오: {}", portfolio.getName());
        log.info("Holdings: {} 종목", portfolio.getHoldings().size());
    }

    /**
     * 샘플 포트폴리오 생성 (한 달 전 대장 코인 중심)
     */
    private Portfolio createSamplePortfolio(User user) {
        Portfolio portfolio = Portfolio.builder()
                .userId(user.getId())
                .name("Crypto Leaders Portfolio")
                .description("주요 암호화폐 포트폴리오 (자동 생성 샘플)")
                .build();

        // 한 달 전 대략적인 가격 (2025-09-21 기준 추정치)
        // 실제 가격은 API 호출로 업데이트됨

        // 1. Bitcoin (BTC)
        PortfolioHolding btc = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("BTC")
                .quantity(new BigDecimal("0.5"))
                .averageCost(new BigDecimal("80000000")) // 8천만원 (한 달 전 추정가)
                .assetType(PortfolioHolding.AssetType.CRYPTO)
                .baseCurrency("KRW")
                .build();
        portfolio.addHolding(btc);

        // 2. Ethereum (ETH)
        PortfolioHolding eth = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("ETH")
                .quantity(new BigDecimal("5.0"))
                .averageCost(new BigDecimal("3500000")) // 350만원 (한 달 전 추정가)
                .assetType(PortfolioHolding.AssetType.CRYPTO)
                .baseCurrency("KRW")
                .build();
        portfolio.addHolding(eth);

        // 3. Solana (SOL)
        PortfolioHolding sol = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("SOL")
                .quantity(new BigDecimal("20.0"))
                .averageCost(new BigDecimal("180000")) // 18만원 (한 달 전 추정가)
                .assetType(PortfolioHolding.AssetType.CRYPTO)
                .baseCurrency("KRW")
                .build();
        portfolio.addHolding(sol);

        // 4. Ripple (XRP)
        PortfolioHolding xrp = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("XRP")
                .quantity(new BigDecimal("1000.0"))
                .averageCost(new BigDecimal("800")) // 800원 (한 달 전 추정가)
                .assetType(PortfolioHolding.AssetType.CRYPTO)
                .baseCurrency("KRW")
                .build();
        portfolio.addHolding(xrp);

        // 5. Binance Coin (BNB)
        PortfolioHolding bnb = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("BNB")
                .quantity(new BigDecimal("3.0"))
                .averageCost(new BigDecimal("700000")) // 70만원 (한 달 전 추정가)
                .assetType(PortfolioHolding.AssetType.CRYPTO)
                .baseCurrency("KRW")
                .build();
        portfolio.addHolding(bnb);

        BigDecimal totalCost = calculateTotalCost(portfolio);
        portfolio.updateTotalCost(totalCost);

        log.info("샘플 포트폴리오 생성 완료:");
        log.info("  - BTC: 0.5개 @ 80,000,000원");
        log.info("  - ETH: 5개 @ 3,500,000원");
        log.info("  - SOL: 20개 @ 180,000원");
        log.info("  - XRP: 1000개 @ 800원");
        log.info("  - BNB: 3개 @ 700,000원");
        log.info("  - 총 투자금: {}", totalCost);

        return portfolio;
    }

    /**
     * 총 매수 비용 계산
     */
    private BigDecimal calculateTotalCost(Portfolio portfolio) {
        return portfolio.getHoldings().stream()
                .map(holding -> holding.getAverageCost().multiply(holding.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
