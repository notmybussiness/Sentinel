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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 개발 환경 초기 데이터 생성
 * - 개발자 계정에 샘플 포트폴리오 및 암호화폐 holdings 자동 생성
 *
 * ⚠️ 비활성화됨: 성능 테스트를 위해 자동 데이터 생성 중지
 * 필요 시 @Component 주석 해제
 */
@Slf4j
//@Component  // ⚠️ 주석처리: 자동 데이터 생성 비활성화
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== 초기 데이터 생성 시작 ===");

        // 개발자 계정 확인 또는 생성
        User devUser = userRepository.findByEmail("dev@sentinel.com")
                .orElseGet(() -> {
                    log.info("개발자 계정이 없습니다. 자동 생성합니다.");
                    User newUser = User.builder()
                            .kakaoId("dev-auto-created")
                            .email("dev@sentinel.com")
                            .name("개발자")
                            .profileImageUrl(null)
                            .build();
                    return userRepository.save(newUser);
                });

        // 이미 포트폴리오가 있는지 확인
        if (portfolioRepository.countByUserId(devUser.getId()) > 0) {
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
     * 샘플 포트폴리오 생성 (2주 전 구매 시뮬레이션)
     */
    private Portfolio createSamplePortfolio(User user) {
        Portfolio portfolio = Portfolio.builder()
                .userId(user.getId())
                .name("Crypto Leaders Portfolio")
                .description("주요 암호화폐 포트폴리오 (2주 전 구매 시뮬레이션)")
                .build();

        // 2주 전 날짜 설정
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);

        // 2주 전 대략적인 가격
        // 실제 가격은 API 호출로 업데이트됨

        // 1. Bitcoin (BTC)
        PortfolioHolding btc = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("BTC")
                .quantity(new BigDecimal("0.5"))
                .averageCost(new BigDecimal("75000000")) // 7천5백만원 (2주 전 추정가)
                .assetType(PortfolioHolding.AssetType.CRYPTO)
                .baseCurrency("KRW")
                .build();
        setCreatedAt(btc, twoWeeksAgo);
        portfolio.addHolding(btc);

        // 2. Ethereum (ETH)
        PortfolioHolding eth = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("ETH")
                .quantity(new BigDecimal("5.0"))
                .averageCost(new BigDecimal("3200000")) // 320만원 (2주 전 추정가)
                .assetType(PortfolioHolding.AssetType.CRYPTO)
                .baseCurrency("KRW")
                .build();
        setCreatedAt(eth, twoWeeksAgo);
        portfolio.addHolding(eth);

        // 3. Solana (SOL)
        PortfolioHolding sol = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("SOL")
                .quantity(new BigDecimal("20.0"))
                .averageCost(new BigDecimal("160000")) // 16만원 (2주 전 추정가)
                .assetType(PortfolioHolding.AssetType.CRYPTO)
                .baseCurrency("KRW")
                .build();
        setCreatedAt(sol, twoWeeksAgo);
        portfolio.addHolding(sol);

        // 4. Ripple (XRP)
        PortfolioHolding xrp = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("XRP")
                .quantity(new BigDecimal("1000.0"))
                .averageCost(new BigDecimal("700")) // 700원 (2주 전 추정가)
                .assetType(PortfolioHolding.AssetType.CRYPTO)
                .baseCurrency("KRW")
                .build();
        setCreatedAt(xrp, twoWeeksAgo);
        portfolio.addHolding(xrp);

        // 5. Binance Coin (BNB)
        PortfolioHolding bnb = PortfolioHolding.builder()
                .portfolio(portfolio)
                .symbol("BNB")
                .quantity(new BigDecimal("3.0"))
                .averageCost(new BigDecimal("650000")) // 65만원 (2주 전 추정가)
                .assetType(PortfolioHolding.AssetType.CRYPTO)
                .baseCurrency("KRW")
                .build();
        setCreatedAt(bnb, twoWeeksAgo);
        portfolio.addHolding(bnb);

        // 초기 가격 설정 (평단가로 초기화 - 추후 API로 업데이트됨)
        for (PortfolioHolding holding : portfolio.getHoldings()) {
            holding.updateCurrentPrice(holding.getAverageCost());
        }

        // 포트폴리오 총계 계산
        portfolio.recalculate();

        // 포트폴리오 생성일도 2주 전으로 설정
        setCreatedAt(portfolio, twoWeeksAgo);

        log.info("샘플 포트폴리오 생성 완료 (2주 전 구매 시뮬레이션):");
        log.info("  - 구매일: {}", twoWeeksAgo);
        log.info("  - BTC: 0.5개 @ 75,000,000원");
        log.info("  - ETH: 5개 @ 3,200,000원");
        log.info("  - SOL: 20개 @ 160,000원");
        log.info("  - XRP: 1000개 @ 700원");
        log.info("  - BNB: 3개 @ 650,000원");
        log.info("  - 총 투자금: {}", portfolio.getTotalCost());

        return portfolio;
    }

    /**
     * Reflection을 사용하여 createdAt 설정 (테스트 데이터 생성용)
     */
    private void setCreatedAt(Object entity, LocalDateTime dateTime) {
        try {
            Field createdAtField = entity.getClass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(entity, dateTime);
        } catch (Exception e) {
            log.warn("createdAt 설정 실패: {}", e.getMessage());
        }
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
