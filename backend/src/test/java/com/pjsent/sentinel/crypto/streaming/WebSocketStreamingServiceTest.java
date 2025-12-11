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
    @DisplayName("[재연결] 연결 실패 시 재시도 메트릭 기록 확인")
    void shouldRecordReconnectionMetric_whenConnectionFails() {
        // Given - 에러 발생 후 빈 스트림으로 복구 (빠른 테스트용)
        when(upbitWebSocketClient.connect(anyList()))
                .thenReturn(Flux.error(new RuntimeException("Connection failed")));

        // When - startStreaming 호출 (에러 발생 → 캐시 폴백)
        var result = service.startStreaming(List.of("BTC"), "KRW");

        // Then - 최종적으로 캐시 폴백으로 빈 결과 반환 (재연결 5회 후)
        StepVerifier.create(result)
                .expectComplete()
                .verify(Duration.ofSeconds(60)); // retry backoff 고려

        // 재연결 메트릭이 기록되었는지 확인 (5회 재시도)
        verify(metrics, atLeast(1)).recordReconnection();
        verify(metrics, atLeast(1)).recordError();
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
