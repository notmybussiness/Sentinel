package com.pjsent.sentinel.crypto.streaming;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ========================================================================
 * WebSocket Streaming Metrics
 * ========================================================================
 *
 * Prometheus/Grafana에서 모니터링 가능한 WebSocket 관련 메트릭 수집
 *
 * [제공 메트릭]
 * - websocket_connections_total: 총 연결 시도 횟수
 * - websocket_reconnections_total: 재연결 횟수
 * - websocket_messages_received_total: 수신 메시지 수
 * - websocket_messages_dropped_total: 백프레셔로 드롭된 메시지 수
 * - websocket_errors_total: 에러 발생 횟수
 * - websocket_last_message_seconds: 마지막 메시지 수신 후 경과 시간
 * - websocket_connection_duration_seconds: 연결 유지 시간
 *
 * [Grafana 쿼리 예시]
 * - 재연결율: rate(websocket_reconnections_total[5m])
 * - 메시지 드롭율: rate(websocket_messages_dropped_total[5m]) / rate(websocket_messages_received_total[5m])
 * - 평균 연결 시간: websocket_connection_duration_seconds_sum / websocket_connection_duration_seconds_count
 */
@Component
@Slf4j
@Getter
public class WebSocketMetrics {

    private static final String METRIC_PREFIX = "websocket";

    // ========================================================================
    // Counters (누적값)
    // ========================================================================

    /** 총 연결 시도 횟수 */
    private final Counter connectionsTotal;

    /** 재연결 횟수 */
    private final Counter reconnectionsTotal;

    /** 수신 메시지 총 개수 */
    private final Counter messagesReceivedTotal;

    /** 백프레셔로 드롭된 메시지 수 */
    private final Counter messagesDroppedTotal;

    /** 에러 발생 횟수 */
    private final Counter errorsTotal;

    // ========================================================================
    // Gauges (현재값)
    // ========================================================================

    /** 마지막 메시지 수신 후 경과 시간 (초) */
    private final AtomicLong lastMessageAgeSeconds = new AtomicLong(0);

    /** 현재 활성 연결 수 */
    private final AtomicLong activeConnections = new AtomicLong(0);

    // ========================================================================
    // Timers (시간 측정)
    // ========================================================================

    /** 연결 유지 시간 측정 */
    private final Timer connectionDuration;

    // ========================================================================
    // 생성자
    // ========================================================================

    public WebSocketMetrics(MeterRegistry registry) {
        // Counters
        this.connectionsTotal = Counter.builder(METRIC_PREFIX + "_connections_total")
                .description("Total WebSocket connection attempts")
                .tag("provider", "upbit")
                .register(registry);

        this.reconnectionsTotal = Counter.builder(METRIC_PREFIX + "_reconnections_total")
                .description("Total WebSocket reconnection attempts")
                .tag("provider", "upbit")
                .register(registry);

        this.messagesReceivedTotal = Counter.builder(METRIC_PREFIX + "_messages_received_total")
                .description("Total messages received via WebSocket")
                .tag("provider", "upbit")
                .register(registry);

        this.messagesDroppedTotal = Counter.builder(METRIC_PREFIX + "_messages_dropped_total")
                .description("Messages dropped due to backpressure")
                .tag("provider", "upbit")
                .register(registry);

        this.errorsTotal = Counter.builder(METRIC_PREFIX + "_errors_total")
                .description("Total WebSocket errors")
                .tag("provider", "upbit")
                .register(registry);

        // Gauges
        Gauge.builder(METRIC_PREFIX + "_last_message_age_seconds", lastMessageAgeSeconds, AtomicLong::get)
                .description("Seconds since last message received")
                .tag("provider", "upbit")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + "_active_connections", activeConnections, AtomicLong::get)
                .description("Current active WebSocket connections")
                .tag("provider", "upbit")
                .register(registry);

        // Timers
        this.connectionDuration = Timer.builder(METRIC_PREFIX + "_connection_duration_seconds")
                .description("WebSocket connection duration")
                .tag("provider", "upbit")
                .register(registry);

        log.info("WebSocketMetrics initialized with {} prefix", METRIC_PREFIX);
    }

    // ========================================================================
    // 메트릭 업데이트 메서드
    // ========================================================================

    /** 새 연결 시작 시 호출 */
    public void recordConnectionStart() {
        connectionsTotal.increment();
        activeConnections.incrementAndGet();
    }

    /** 연결 종료 시 호출 */
    public void recordConnectionEnd(long durationMs) {
        activeConnections.decrementAndGet();
        connectionDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /** 재연결 시도 시 호출 */
    public void recordReconnection() {
        reconnectionsTotal.increment();
    }

    /** 메시지 수신 시 호출 */
    public void recordMessageReceived() {
        messagesReceivedTotal.increment();
        lastMessageAgeSeconds.set(0);
    }

    /** 메시지 드롭 시 호출 */
    public void recordMessageDropped() {
        messagesDroppedTotal.increment();
    }

    /** 에러 발생 시 호출 */
    public void recordError() {
        errorsTotal.increment();
    }

    /** 마지막 메시지 수신 후 경과 시간 업데이트 */
    public void updateLastMessageAge(long seconds) {
        lastMessageAgeSeconds.set(seconds);
    }

    // ========================================================================
    // 통계 조회 메서드 (디버깅/로깅용)
    // ========================================================================

    public String getStats() {
        return String.format(
                "WebSocket Stats: connections=%d, reconnections=%d, received=%d, dropped=%d, errors=%d, active=%d",
                (long) connectionsTotal.count(),
                (long) reconnectionsTotal.count(),
                (long) messagesReceivedTotal.count(),
                (long) messagesDroppedTotal.count(),
                (long) errorsTotal.count(),
                activeConnections.get()
        );
    }
}
