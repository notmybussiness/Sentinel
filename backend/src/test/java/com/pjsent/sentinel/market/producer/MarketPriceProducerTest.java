package com.pjsent.sentinel.market.producer;

import com.pjsent.sentinel.common.event.PriceUpdateEvent;
import com.pjsent.sentinel.portfolio.entity.PortfolioHolding.AssetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * MarketPriceProducer 단위 테스트
 * 
 * 🧢 Kent Beck: TDD 원칙에 따라 다양한 Edge Case 커버
 * 🦅 Murphy's Law: 자산 타입별, 가격 범위별 테스트
 */
@ExtendWith(MockitoExtension.class)
class MarketPriceProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private MarketPriceProducer marketPriceProducer;

    private static final String TOPIC = "market-price-updates";

    @Nested
    @DisplayName("STOCK 타입 이벤트 발행")
    class StockEventPublish {

        @Test
        @DisplayName("일반 주식 가격 업데이트 이벤트를 Kafka로 발행한다")
        void publishStockPriceUpdate() {
            // given
            PriceUpdateEvent event = new PriceUpdateEvent(
                    "AAPL",
                    BigDecimal.valueOf(150.0),
                    AssetType.STOCK,
                    LocalDateTime.now());

            // when
            marketPriceProducer.publishPriceUpdate(event);

            // then
            verify(kafkaTemplate).send(eq(TOPIC), eq("AAPL"), eq(event));
        }

        @Test
        @DisplayName("대용량 가격 (높은 정밀도) 이벤트를 발행한다")
        void publishHighPrecisionPriceUpdate() {
            // given - 버크셔 해서웨이급 고가 주식
            PriceUpdateEvent event = new PriceUpdateEvent(
                    "BRK.A",
                    new BigDecimal("543210.1234"),
                    AssetType.STOCK,
                    LocalDateTime.now());

            // when
            marketPriceProducer.publishPriceUpdate(event);

            // then
            verify(kafkaTemplate).send(eq(TOPIC), eq("BRK.A"), eq(event));
        }

        @Test
        @DisplayName("저가 주식 (페니스탁) 이벤트를 발행한다")
        void publishPennyStockPriceUpdate() {
            // given - 1달러 미만 페니스탁
            PriceUpdateEvent event = new PriceUpdateEvent(
                    "PENNY",
                    new BigDecimal("0.0012"),
                    AssetType.STOCK,
                    LocalDateTime.now());

            // when
            marketPriceProducer.publishPriceUpdate(event);

            // then
            verify(kafkaTemplate).send(eq(TOPIC), eq("PENNY"), eq(event));
        }
    }

    @Nested
    @DisplayName("CRYPTO 타입 이벤트 발행")
    class CryptoEventPublish {

        @Test
        @DisplayName("비트코인 가격 업데이트 이벤트를 발행한다")
        void publishBitcoinPriceUpdate() {
            // given
            PriceUpdateEvent event = new PriceUpdateEvent(
                    "BTC",
                    new BigDecimal("45000.50"),
                    AssetType.CRYPTO,
                    LocalDateTime.now());

            // when
            marketPriceProducer.publishPriceUpdate(event);

            // then
            verify(kafkaTemplate).send(eq(TOPIC), eq("BTC"), eq(event));
        }

        @Test
        @DisplayName("저가 altcoin 가격 업데이트 이벤트를 발행한다")
        void publishAltcoinPriceUpdate() {
            // given - 극소 단위 altcoin
            PriceUpdateEvent event = new PriceUpdateEvent(
                    "SHIB",
                    new BigDecimal("0.00001234"),
                    AssetType.CRYPTO,
                    LocalDateTime.now());

            // when
            marketPriceProducer.publishPriceUpdate(event);

            // then
            verify(kafkaTemplate).send(eq(TOPIC), eq("SHIB"), eq(event));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("가격이 0인 이벤트를 발행한다")
        void publishZeroPriceUpdate() {
            // given - 거래 정지 등의 이유로 가격이 0
            PriceUpdateEvent event = new PriceUpdateEvent(
                    "DELISTED",
                    BigDecimal.ZERO,
                    AssetType.STOCK,
                    LocalDateTime.now());

            // when
            marketPriceProducer.publishPriceUpdate(event);

            // then
            verify(kafkaTemplate).send(eq(TOPIC), eq("DELISTED"), eq(event));
        }
    }
}
