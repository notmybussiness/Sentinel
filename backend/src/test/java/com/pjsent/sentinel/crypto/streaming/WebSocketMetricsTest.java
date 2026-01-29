package com.pjsent.sentinel.crypto.streaming;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocketMetrics 단위 테스트
 * 
 * TDD Cycle: RED phase
 * Micrometer SimpleMeterRegistry를 사용하여 메트릭 수집 검증
 */
@DisplayName("WebSocketMetrics 테스트")
class WebSocketMetricsTest {

    private SimpleMeterRegistry registry;
    private WebSocketMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new WebSocketMetrics(registry);
    }

    @Test
    @DisplayName("recordConnectionStart: 연결 시도 카운터와 활성 연결 게이지 증가")
    void recordConnectionStart_ShouldIncrementCounters() {
        // When
        metrics.recordConnectionStart();

        // Then
        assertThat(registry.find("websocket_connections_total").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("websocket_active_connections").gauge().value()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordConnectionEnd: 활성 연결 감소 및 지속 시간 타이머 기록")
    void recordConnectionEnd_ShouldDecrementActiveConnections() {
        // Given
        metrics.recordConnectionStart();

        // When
        metrics.recordConnectionEnd(1000); // 1초

        // Then
        assertThat(registry.find("websocket_active_connections").gauge().value()).isEqualTo(0.0);
        assertThat(registry.find("websocket_connection_duration_seconds").timer().count()).isEqualTo(1);
        assertThat(registry.find("websocket_connection_duration_seconds").timer()
                .totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("recordReconnection: 재연결 카운터 증가")
    void recordReconnection_ShouldIncrementReconnectionCounter() {
        // When
        metrics.recordReconnection();

        // Then
        assertThat(registry.find("websocket_reconnections_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordMessageReceived: 메시지 수신 카운터 증가 및 마지막 수신 시간 초기화")
    void recordMessageReceived_ShouldResetLastMessageAge() {
        // Given
        metrics.updateLastMessageAge(100);

        // When
        metrics.recordMessageReceived();

        // Then
        assertThat(registry.find("websocket_messages_received_total").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("websocket_last_message_age_seconds").gauge().value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("recordMessageDropped: 드롭된 메시지 카운터 증가")
    void recordMessageDropped_ShouldIncrementDroppedCounter() {
        // When
        metrics.recordMessageDropped();

        // Then
        assertThat(registry.find("websocket_messages_dropped_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("recordError: 에러 카운터 증가")
    void recordError_ShouldIncrementErrorCounter() {
        // When
        metrics.recordError();

        // Then
        assertThat(registry.find("websocket_errors_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("getStats: 포맷팅된 통계 문자열 반환")
    void getStats_ShouldReturnFormattedString() {
        // Given
        metrics.recordConnectionStart();
        metrics.recordMessageReceived();
        metrics.recordError();

        // When
        String stats = metrics.getStats();

        // Then
        assertThat(stats).contains("connections=1");
        assertThat(stats).contains("received=1");
        assertThat(stats).contains("errors=1");
    }
}
