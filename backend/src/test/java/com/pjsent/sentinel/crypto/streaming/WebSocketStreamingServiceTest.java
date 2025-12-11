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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * WebSocketStreamingService 핵심 테스트
 * 
 * 1. 연결 성공 → 데이터 수신
 * 2. 연결 실패 → 재연결
 */
@ExtendWith(MockitoExtension.class)
class WebSocketStreamingServiceTest {

    @Mock
    private UpbitWebSocketClient upbitWebSocketClient;

    private WebSocketStreamingService service;

    @BeforeEach
    void setUp() {
        service = new WebSocketStreamingService(upbitWebSocketClient);
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    // ========================================================================
    // 1. 연결 성공
    // ========================================================================

    @Test
    @DisplayName("[연결] WebSocket 연결 성공 → 데이터 수신")
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
    // 2. 재연결 (Retry)
    // ========================================================================

    @Test
    @DisplayName("[재연결] 연결 실패 → 재시도 후 성공")
    void shouldRetry_whenConnectionFails() {
        // Given - 첫 번째 실패, 두 번째 성공
        var callCount = new AtomicInteger(0);
        var mockPrice = createMockPrice("BTC", 50000000.0);

        when(upbitWebSocketClient.connect(anyList()))
                .thenAnswer(invocation -> {
                    if (callCount.incrementAndGet() == 1) {
                        return Flux.error(new RuntimeException("Connection failed"));
                    }
                    return Flux.just(mockPrice);
                });

        // When
        var result = service.startStreaming(List.of("BTC"), "KRW");

        // Then - 재연결 후 데이터 수신 (backoff 2초 대기 필요)
        StepVerifier.create(result.take(1))
                .expectNextMatches(dto -> dto.getSymbol().equals("BTC"))
                .verifyComplete();

        // 최소 2번 호출 (실패 1회 + 성공 1회)
        assertThat(callCount.get()).isGreaterThanOrEqualTo(2);
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
