package com.pjsent.sentinel.crypto.streaming;

import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * WebSocketStreamingService ?듭떖 ?뚯뒪??
 * 
 * 1. ?곌껐 ?깃났 ???곗씠???섏떊
 * 2. ?곌껐 ?ㅽ뙣 ???ъ뿰寃?
 */
@ExtendWith(MockitoExtension.class)
class WebSocketStreamingServiceTest {

        @Mock
        private UpbitWebSocketClient upbitWebSocketClient;

        @Mock
        private WebSocketMetrics metrics;

        private WebSocketStreamingService service;

        @BeforeEach
        void setUp() {
                // Mock getSecondsSinceLastMessage for metrics update
                lenient().when(upbitWebSocketClient.getSecondsSinceLastMessage()).thenReturn(0L);

                service = new WebSocketStreamingService(upbitWebSocketClient, metrics);
                ReflectionTestUtils.setField(service, "enabled", true);
        }

        // ========================================================================
        // 1. ?곌껐 ?깃났
        // ========================================================================

        @Test
        @DisplayName("[?곌껐] WebSocket ?곌껐 ?깃났 ???곗씠???섏떊")
        void shouldReceiveData_whenConnected() {
                // Given
                var mockPrice = createMockPrice("BTC", 50000000.0);
                when(upbitWebSocketClient.connect(anyList()))
                                .thenReturn(Flux.just(mockPrice));

                // When
                var result = service.startStreaming(List.of("BTC"), "KRW");

                // Then
                StepVerifier.create(result.take(1))
                                .expectNextMatches(dto -> dto.getSymbol().equals("BTC") && dto.getPrice() == 50000000.0)
                                .verifyComplete();

                verify(upbitWebSocketClient, times(1)).connect(anyList());
        }

        // ========================================================================
        // 2. ?ъ뿰寃?(Retry)
        // ========================================================================

        @Test
        @DisplayName("[?ъ뿰寃? ?곌껐 ?ㅽ뙣 ???ъ떆??硫뷀듃由?湲곕줉 ?뺤씤")
        void shouldRecordReconnectionMetric_whenConnectionFails() {
                // Given - ?먮윭 諛쒖깮 ??鍮??ㅽ듃由쇱쑝濡?蹂듦뎄 (鍮좊Ⅸ ?뚯뒪?몄슜)
                when(upbitWebSocketClient.connect(anyList()))
                                .thenReturn(Flux.error(new RuntimeException("Connection failed")));

                // When - startStreaming ?몄텧 (?먮윭 諛쒖깮 ??罹먯떆 ?대갚)
                // Then - retry backoff chain completes under virtual time without real-time waiting
                StepVerifier.withVirtualTime(() -> service.startStreaming(List.of("BTC"), "KRW"))
                                .expectSubscription()
                                .thenAwait(Duration.ofMinutes(10))
                                .expectComplete()
                                .verify();

                // ?ъ뿰寃?硫뷀듃由?씠 湲곕줉?섏뿀?붿? ?뺤씤 (5???ъ떆??
                verify(metrics, atLeast(1)).recordReconnection();
                verify(metrics, atLeast(1)).recordError();
        }
        // ========================================================================
        // 3. 鍮꾪솢?깊솕 ?곹깭 (Disabled)
        // ========================================================================

        @Test
        @DisplayName("[Disabled] 鍮꾪솢?깊솕 ??罹먯떆???곗씠??諛섑솚")
        void shouldReturnCachedData_whenDisabled() {
                // Given
                ReflectionTestUtils.setField(service, "enabled", false);

                // Cache dummy data
                CryptoPriceDto cachedPrice = createMockPrice("BTC", 51000000.0);

                // Let's use reflection to put data in priceCache
                @SuppressWarnings("unchecked")
                java.util.Map<String, CryptoPriceDto> cache = (java.util.Map<String, CryptoPriceDto>) ReflectionTestUtils
                                .getField(service, "priceCache");
                cache.put("KRW-BTC", cachedPrice);

                // When
                var result = service.startStreaming(List.of("BTC"), "KRW");

                // Then
                StepVerifier.create(result)
                                .expectNextMatches(dto -> dto.getPrice() == 51000000.0)
                                .verifyComplete();

                verify(upbitWebSocketClient, never()).connect(anyList());
        }

        // ========================================================================
        // 4. Fallback (Cache) - ?듯빀 ?뚯뒪???깃꺽
        // ========================================================================

        @Test
        @DisplayName("[Fallback] ?꾩쟾 ?ㅽ뙣 ??罹먯떆 媛?諛섑솚 ?뺤씤")
        void shouldReturnCachedData_whenConnectionCompletelyFails() {
                // Given
                // 1. Pre-populate cache
                CryptoPriceDto cachedPrice = createMockPrice("BTC", 49000000.0);
                @SuppressWarnings("unchecked")
                java.util.Map<String, CryptoPriceDto> cache = (java.util.Map<String, CryptoPriceDto>) ReflectionTestUtils
                                .getField(service, "priceCache");
                cache.put("KRW-BTC", cachedPrice);

                // 2. Mock connection failure
                when(upbitWebSocketClient.connect(anyList()))
                                .thenReturn(Flux.error(new RuntimeException("Total failure")));

                // When
                var result = service.startStreaming(List.of("BTC"), "KRW");

                // Then: Reuse metrics verification or similar
                // Because of Retry.backoff, it handles error internally and then onErrorResume
                // returns cache
                // We need to verify that we get the cached value eventually

                // Adjust retry specs for testing to speed it up?
                // Method uses constant Retry.backoff(5, 2s). That's 10+ seconds.
                // We can't easily change the Retry spec inside the service without refactoring
                // or using virtual time.
                // StepVerifier.withVirtualTime is key here.

                StepVerifier.withVirtualTime(() -> service.startStreaming(List.of("BTC"), "KRW"))
                                .expectSubscription()
                                .thenAwait(Duration.ofMinutes(5)) // Wait enough for retries
                                .expectNextMatches(dto -> dto.getPrice() == 49000000.0)
                                .verifyComplete();
        }

        // ========================================================================
        // Helper
        // ========================================================================

        private CryptoPriceDto createMockPrice(String symbol, double price) {
                return CryptoPriceDto.builder()
                                .symbol(symbol)
                                .baseCurrency("KRW")
                                .marketCode("KRW-" + symbol)
                                .price(price)
                                .provider("Upbit")
                                .build();
        }
}

