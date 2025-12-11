package com.pjsent.sentinel.crypto.streaming;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ========================================================================
 * UpbitWebSocketClient 단위 테스트
 * ========================================================================
 * 
 * TDD Cycle:
 * [RED] 테스트 먼저 작성 → 현재 상태
 * [GREEN] 테스트 통과하도록 구현 수정 (이미 구현됨)
 * [REFACTOR] 코드 개선
 * 
 * 테스트 범위:
 * - buildSubscriptionMessage() - Upbit 구독 메시지 생성
 * - parseMessage() - JSON → CryptoPriceDto 변환
 * - connect() - 실제 Upbit WebSocket 연결 (통합 테스트)
 */
class UpbitWebSocketClientTest {

    private UpbitWebSocketClient client;

    @BeforeEach
    void setUp() {
        client = new UpbitWebSocketClient();
    }

    // ========================================================================
    // buildSubscriptionMessage() 테스트
    // ========================================================================

    @Nested
    @DisplayName("buildSubscriptionMessage() 테스트")
    class BuildSubscriptionMessageTests {

        @Test
        @DisplayName("[성공] 단일 심볼로 구독 메시지 생성")
        void shouldBuildValidJsonForSingleSymbol() {
            // Given
            List<String> symbols = List.of("BTC");

            // When
            String result = client.buildSubscriptionMessage(symbols);

            // Then
            assertThat(result).contains("\"ticket\":");
            assertThat(result).contains("\"type\":\"ticker\"");
            assertThat(result).contains("\"codes\":[\"KRW-BTC\"]");
        }

        @Test
        @DisplayName("[성공] 복수 심볼로 구독 메시지 생성")
        void shouldBuildValidJsonForMultipleSymbols() {
            // Given
            List<String> symbols = List.of("BTC", "ETH", "XRP");

            // When
            String result = client.buildSubscriptionMessage(symbols);

            // Then
            assertThat(result).contains("\"KRW-BTC\"");
            assertThat(result).contains("\"KRW-ETH\"");
            assertThat(result).contains("\"KRW-XRP\"");
        }

        @Test
        @DisplayName("[성공] 소문자 심볼도 대문자로 변환")
        void shouldConvertLowercaseToUppercase() {
            // Given
            List<String> symbols = List.of("btc", "eth");

            // When
            String result = client.buildSubscriptionMessage(symbols);

            // Then
            assertThat(result).contains("\"KRW-BTC\"");
            assertThat(result).contains("\"KRW-ETH\"");
            assertThat(result).doesNotContain("KRW-btc");
        }
    }

    // ========================================================================
    // parseMessage() 테스트
    // ========================================================================

    @Nested
    @DisplayName("parseMessage() 테스트")
    class ParseMessageTests {

        @Test
        @DisplayName("[성공] 유효한 ticker JSON을 CryptoPriceDto로 변환")
        void shouldParseValidTickerJson() {
            // Given - Upbit 실제 응답 형식
            String json = """
                    {
                        "type": "ticker",
                        "code": "KRW-BTC",
                        "trade_price": 50000000.0,
                        "opening_price": 49000000.0,
                        "high_price": 51000000.0,
                        "low_price": 48000000.0,
                        "acc_trade_volume_24h": 1234.56,
                        "signed_change_price": 1000000.0,
                        "signed_change_rate": 0.02,
                        "acc_trade_price_24h": 61728000000.0
                    }
                    """;

            // When
            CryptoPriceDto result = client.parseMessage(json);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSymbol()).isEqualTo("BTC");
            assertThat(result.getBaseCurrency()).isEqualTo("KRW");
            assertThat(result.getMarketCode()).isEqualTo("KRW-BTC");
            assertThat(result.getPrice()).isEqualTo(50000000.0);
            assertThat(result.getOpenPrice()).isEqualTo(49000000.0);
            assertThat(result.getHighPrice()).isEqualTo(51000000.0);
            assertThat(result.getLowPrice()).isEqualTo(48000000.0);
            assertThat(result.getVolume()).isEqualTo(1234.56);
            assertThat(result.getChange()).isEqualTo(1000000.0);
            assertThat(result.getChangePercent()).isEqualTo(2.0); // 0.02 * 100
            assertThat(result.getTradeValue()).isEqualTo(61728000000.0);
            assertThat(result.getProvider()).isEqualTo("Upbit");
        }

        @Test
        @DisplayName("[실패] 에러 응답은 null 반환")
        void shouldReturnNullOnErrorResponse() {
            // Given
            String json = """
                    {
                        "error": {
                            "name": "UnsupportedMarket",
                            "message": "지원하지 않는 마켓입니다."
                        }
                    }
                    """;

            // When
            CryptoPriceDto result = client.parseMessage(json);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("[실패] 비-ticker 타입은 null 반환")
        void shouldReturnNullOnNonTickerType() {
            // Given - trade 타입 메시지
            String json = """
                    {
                        "type": "trade",
                        "code": "KRW-BTC",
                        "trade_price": 50000000.0
                    }
                    """;

            // When
            CryptoPriceDto result = client.parseMessage(json);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("[실패] 잘못된 JSON은 null 반환")
        void shouldReturnNullOnInvalidJson() {
            // Given
            String json = "{ invalid json }";

            // When
            CryptoPriceDto result = client.parseMessage(json);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("[실패] type 필드가 없으면 null 반환")
        void shouldReturnNullWhenTypeIsMissing() {
            // Given
            String json = """
                    {
                        "code": "KRW-BTC",
                        "trade_price": 50000000.0
                    }
                    """;

            // When
            CryptoPriceDto result = client.parseMessage(json);

            // Then
            assertThat(result).isNull();
        }
    }

    // ========================================================================
    // connect() 통합 테스트 (실제 Upbit 연결)
    // ========================================================================

    @Nested
    @DisplayName("connect() 통합 테스트")
    class ConnectIntegrationTests {

        @Test
        @DisplayName("[통합] 실제 Upbit WebSocket에서 BTC 가격 수신")
        void shouldReceiveRealPricesFromUpbit() {
            // Given
            List<String> symbols = List.of("BTC");

            // When
            Flux<CryptoPriceDto> priceStream = client.connect(symbols);

            // Then - 5초 내에 최소 1개의 가격 데이터 수신
            StepVerifier.create(priceStream.take(1))
                    .assertNext(dto -> {
                        assertThat(dto).isNotNull();
                        assertThat(dto.getSymbol()).isEqualTo("BTC");
                        assertThat(dto.getPrice()).isGreaterThan(0);
                        assertThat(dto.getProvider()).isEqualTo("Upbit");
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("[통합] 복수 심볼 구독 시 각각 데이터 수신")
        void shouldReceiveMultipleSymbolPrices() {
            // Given
            List<String> symbols = List.of("BTC", "ETH");

            // When
            Flux<CryptoPriceDto> priceStream = client.connect(symbols);

            // Then - 10초 내에 최소 3개의 가격 데이터 수신
            StepVerifier.create(priceStream.take(3))
                    .thenConsumeWhile(dto -> {
                        assertThat(dto.getSymbol()).isIn("BTC", "ETH");
                        assertThat(dto.getPrice()).isGreaterThan(0);
                        return true;
                    })
                    .expectComplete()
                    .verify(Duration.ofSeconds(15));
        }
    }
}
