package com.pjsent.sentinel.integration;

import com.pjsent.sentinel.common.event.PriceUpdateEvent;
import com.pjsent.sentinel.market.producer.MarketPriceProducer;
import com.pjsent.sentinel.portfolio.entity.Portfolio;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding.AssetType;
import com.pjsent.sentinel.portfolio.repository.PortfolioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * EDA 통합 테스트 (EmbeddedKafka)
 * 
 * Producer → Kafka → Consumer 전체 플로우 검증
 * 
 * 🧢 Kent Beck: E2E 시나리오 검증
 * 🦁 SRE: 메시지 전달 및 처리 검증
 * 🦅 Murphy's Law: 비동기 처리 안정성 검증
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = { "market-price-updates" }, brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=test-group",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
        "spring.main.allow-bean-definition-overriding=true",
        // Disable unnecessary services for test
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "spring.data.redis.repositories.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-jwt-secret-for-eda-integration-test-must-be-long-enough",
        "kakao.oauth.client-id=test-client",
        "kakao.oauth.client-secret=test-secret",
        "kakao.oauth.redirect-uri=http://localhost:8080/test",
        "stock.market.alphavantage.api-key=test-key",
        "stock.market.finnhub.api-key=test-key",
        "cors.allowed-origins=http://localhost:3000",
        "spring.cache.type=none"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EDAIntegrationTest {

    @Autowired
    private MarketPriceProducer marketPriceProducer;

    @MockitoBean
    private PortfolioRepository portfolioRepository;

    @MockitoBean
    private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

    @Test
    @DisplayName("Producer가 이벤트를 발행하면 Consumer가 수신하여 포트폴리오를 업데이트한다")
    void endToEndPriceUpdate() throws Exception {
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
        marketPriceProducer.publishPriceUpdate(event);

        // then - 비동기 처리 대기 (최대 10초)
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(holding).updateCurrentPrice(newPrice);
            verify(portfolio).recalculate();
            verify(portfolioRepository).save(portfolio);
        });
    }

    @Test
    @DisplayName("여러 이벤트를 연속 발행하면 순차적으로 처리된다")
    void multipleEventsProcessedSequentially() throws Exception {
        // given
        PriceUpdateEvent event1 = new PriceUpdateEvent("AAPL", BigDecimal.valueOf(150.0), AssetType.STOCK,
                LocalDateTime.now());
        PriceUpdateEvent event2 = new PriceUpdateEvent("GOOG", BigDecimal.valueOf(2800.0), AssetType.STOCK,
                LocalDateTime.now());

        Portfolio portfolio1 = mock(Portfolio.class);
        Portfolio portfolio2 = mock(Portfolio.class);
        PortfolioHolding holding1 = mock(PortfolioHolding.class);
        PortfolioHolding holding2 = mock(PortfolioHolding.class);

        given(holding1.getSymbol()).willReturn("AAPL");
        given(holding1.getAssetType()).willReturn(AssetType.STOCK);
        given(holding2.getSymbol()).willReturn("GOOG");
        given(holding2.getAssetType()).willReturn(AssetType.STOCK);
        given(portfolio1.getHoldings()).willReturn(List.of(holding1));
        given(portfolio2.getHoldings()).willReturn(List.of(holding2));
        given(portfolioRepository.findByHoldings_Symbol("AAPL")).willReturn(List.of(portfolio1));
        given(portfolioRepository.findByHoldings_Symbol("GOOG")).willReturn(List.of(portfolio2));

        // when
        marketPriceProducer.publishPriceUpdate(event1);
        marketPriceProducer.publishPriceUpdate(event2);

        // then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(holding1).updateCurrentPrice(BigDecimal.valueOf(150.0));
            verify(holding2).updateCurrentPrice(BigDecimal.valueOf(2800.0));
            verify(portfolioRepository, times(2)).save(any(Portfolio.class));
        });
    }

    @Test
    @DisplayName("매칭되는 포트폴리오가 없어도 에러 없이 처리된다")
    void noMatchingPortfolio_NoError() throws Exception {
        // given
        PriceUpdateEvent event = new PriceUpdateEvent("UNKNOWN", BigDecimal.valueOf(100.0), AssetType.STOCK,
                LocalDateTime.now());
        given(portfolioRepository.findByHoldings_Symbol("UNKNOWN")).willReturn(List.of());

        // when
        marketPriceProducer.publishPriceUpdate(event);

        // then - 에러 없이 통과, save 호출 없음
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(portfolioRepository).findByHoldings_Symbol("UNKNOWN");
            verify(portfolioRepository, never()).save(any());
        });
    }
}
