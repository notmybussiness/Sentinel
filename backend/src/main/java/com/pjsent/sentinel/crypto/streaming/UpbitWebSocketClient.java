package com.pjsent.sentinel.crypto.streaming;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * ========================================================================
 * Upbit WebSocket Client
 * ========================================================================
 * 
 * [역할]
 * - Upbit 거래소의 WebSocket API에 연결하여 실시간 시세를 수신
 * - 수신한 JSON 데이터를 CryptoPriceDto로 변환하여 Flux로 반환
 * 
 * [구조]
 * Upbit WS Server ──메시지──▶ 이 클라이언트 ──Flux<DTO>──▶ Service
 * 
 * [Upbit WebSocket 프로토콜]
 * 1. 연결: wss://api.upbit.com/websocket/v1
 * 2. 구독 메시지 전송: [{"ticket":"UUID"},{"type":"ticker","codes":["KRW-BTC"]}]
 * 3. 실시간 데이터 수신: {"type":"ticker","code":"KRW-BTC","trade_price":50000000,...}
 */
@Slf4j
@Component
public class UpbitWebSocketClient {

    // ========================================================================
    // 1. 상수 & 필드
    // ========================================================================

    /** Upbit WebSocket 엔드포인트 (공식 문서 참고) */
    private static final String UPBIT_WS_URL = "wss://api.upbit.com/websocket/v1";

    /** Keep-Alive 간격 (초) - Upbit은 120초 동안 메시지 없으면 연결 종료 */
    private static final int PING_INTERVAL_SECONDS = 30;

    /** 연결 타임아웃 (초) */
    private static final int CONNECTION_TIMEOUT_SECONDS = 10;

    /** 읽기 타임아웃 (초) - 마지막 메시지 수신 후 이 시간이 지나면 재연결 필요 */
    private static final int READ_TIMEOUT_SECONDS = 120;

    /**
     * WebSocket 클라이언트 - Netty 기반 비동기 구현체
     * 타임아웃 핸들러가 설정된 커스텀 HttpClient 사용
     */
    private final WebSocketClient client;

    /** JSON 파싱용 Jackson ObjectMapper */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 마지막 메시지 수신 시간 (좀비 연결 감지용) */
    private final AtomicLong lastMessageTimestamp = new AtomicLong(System.currentTimeMillis());

    // ========================================================================
    // 생성자: 타임아웃이 설정된 WebSocket 클라이언트 생성
    // ========================================================================

    public UpbitWebSocketClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT_SECONDS))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(CONNECTION_TIMEOUT_SECONDS));
                });

        this.client = new ReactorNettyWebSocketClient(httpClient);
        log.info("✅ UpbitWebSocketClient 초기화 완료. Ping 간격: {}s, 읽기 타임아웃: {}s",
                PING_INTERVAL_SECONDS, READ_TIMEOUT_SECONDS);
    }

    // ========================================================================
    // 2. 메인 메서드: connect()
    // ========================================================================

    /**
     * Upbit에 WebSocket 연결하고 실시간 시세를 Flux로 반환
     *
     * @param symbols 구독할 심볼 목록 (예: ["BTC", "ETH"])
     * @return 실시간 CryptoPriceDto 스트림
     *
     *         [흐름]
     *         1. Flux.create()로 수동 Flux 생성 (sink 패턴)
     *         2. client.execute()로 WebSocket 연결
     *         3. 구독 메시지 전송 (session.send)
     *         4. 응답 수신 (session.receive)
     *         5. JSON 파싱 → sink.next()로 데이터 밀어넣기
     *         6. Keep-Alive: 주기적 ping 전송으로 연결 유지
     */
    public Flux<CryptoPriceDto> connect(List<String> symbols) {
        URI uri = URI.create(UPBIT_WS_URL);
        String subscribeMessage = buildSubscriptionMessage(symbols);

        log.info("🔌 Upbit WebSocket 연결 시작. URI: {}, Symbols: {}", uri, symbols);

        // 연결 시작 시 타임스탬프 초기화
        lastMessageTimestamp.set(System.currentTimeMillis());

        // ----------------------------------------------------------------
        // Flux.create() 사용 이유:
        // - client.execute()는 Mono<Void> 반환 → 데이터를 밖으로 못 꺼냄
        // - Flux.create()로 sink를 만들면, 안에서 sink.next(data)로 밀어넣기 가능
        // ----------------------------------------------------------------
        return Flux.create(sink -> {

            client.execute(uri, session -> {
                log.info("✅ WebSocket 연결 성공. Session ID: {}", session.getId());

                // ============================================================
                // Keep-Alive: 주기적 Ping 전송 스트림
                // ============================================================
                Flux<WebSocketMessage> pingStream = createPingStream(session, symbols);

                // ============================================================
                // 데이터 수신 스트림
                // ============================================================
                Flux<CryptoPriceDto> inbound = session.receive()
                        // Step 3: 페이로드 추출
                        .map(WebSocketMessage::getPayloadAsText)
                        // Step 4: 마지막 메시지 시간 업데이트 + 디버그 로깅
                        .doOnNext(payload -> {
                            lastMessageTimestamp.set(System.currentTimeMillis());
                            log.debug("📨 Raw: {}", payload.substring(0, Math.min(100, payload.length())));
                        })
                        // Step 5: JSON → CryptoPriceDto 변환
                        .map(this::parseMessage)
                        // Step 6: null 필터링 (파싱 실패한 경우)
                        .filter(dto -> dto != null)
                        // Step 7: 🔥 핵심! sink에 밀어넣기 → 외부 Flux로 전달
                        .doOnNext(sink::next)
                        // Step 8: 에러 처리
                        .doOnError(e -> {
                            log.error("❌ WebSocket 수신 오류: {}", e.getMessage());
                            sink.error(e);
                        })
                        // Step 9: 연결 종료 시
                        .doOnComplete(() -> {
                            log.warn("🔌 WebSocket 연결 종료됨");
                            sink.complete();
                        });

                // ============================================================
                // send와 receive를 동시에 실행
                // - 구독 메시지 전송
                // - Ping 메시지 주기적 전송 (Keep-Alive)
                // - 데이터 수신
                // ============================================================
                Flux<WebSocketMessage> outbound = Flux.merge(
                        Flux.just(session.textMessage(subscribeMessage)),
                        pingStream
                );

                return Mono.when(
                        session.send(outbound),
                        inbound.then()
                );
            })
                    .subscribe();

        }); // Flux.create 끝
    }

    // ========================================================================
    // Keep-Alive: Ping 스트림 생성
    // ========================================================================

    /**
     * 주기적으로 Ping 메시지를 생성하는 Flux
     * Upbit은 "PING" 문자열을 보내면 "PONG"으로 응답하지 않지만,
     * 구독 갱신 메시지를 보내면 연결이 유지됨
     *
     * @param session WebSocket 세션
     * @param symbols 구독 중인 심볼 목록
     * @return Ping 메시지 Flux
     */
    private Flux<WebSocketMessage> createPingStream(WebSocketSession session, List<String> symbols) {
        return Flux.interval(Duration.ofSeconds(PING_INTERVAL_SECONDS))
                .map(tick -> {
                    // 좀비 연결 체크: 마지막 메시지로부터 READ_TIMEOUT_SECONDS 초과 시 경고
                    long silentDuration = System.currentTimeMillis() - lastMessageTimestamp.get();
                    if (silentDuration > READ_TIMEOUT_SECONDS * 1000L) {
                        log.warn("⚠️ 좀비 연결 감지: {}초 동안 메시지 없음", silentDuration / 1000);
                    }

                    // 구독 갱신 메시지로 연결 유지 (Upbit은 PING 미지원)
                    String pingMessage = buildSubscriptionMessage(symbols);
                    log.debug("💓 Keep-Alive ping 전송 (tick={})", tick);
                    return session.textMessage(pingMessage);
                })
                .doOnSubscribe(sub -> log.info("💓 Keep-Alive 활성화: {}초 간격", PING_INTERVAL_SECONDS));
    }

    /**
     * 마지막 메시지 수신 이후 경과 시간 (초)
     * 모니터링용 메트릭 제공
     */
    public long getSecondsSinceLastMessage() {
        return (System.currentTimeMillis() - lastMessageTimestamp.get()) / 1000;
    }

    // ========================================================================
    // 3. 헬퍼 메서드: 구독 메시지 생성
    // ========================================================================

    /**
     * Upbit WebSocket 구독 메시지 생성
     * 
     * @param symbols ["BTC", "ETH"]
     * @return [{"ticket":"uuid"},{"type":"ticker","codes":["KRW-BTC","KRW-ETH"]}]
     * 
     *         [Upbit 공식 포맷]
     *         - ticket: 고유 식별자 (UUID 권장)
     *         - type: "ticker" (시세), "trade" (체결), "orderbook" (호가)
     *         - codes: 마켓 코드 (KRW-BTC 형식)
     */
    String buildSubscriptionMessage(List<String> symbols) {
        // BTC → "KRW-BTC" 형태로 변환
        String codesArray = symbols.stream()
                .map(s -> "\"KRW-" + s.toUpperCase() + "\"")
                .collect(Collectors.joining(","));

        String ticket = UUID.randomUUID().toString();

        return String.format(
                "[{\"ticket\":\"%s\"},{\"type\":\"ticker\",\"codes\":[%s]}]",
                ticket, codesArray);
    }

    // ========================================================================
    // 4. 헬퍼 메서드: JSON 파싱
    // ========================================================================

    /**
     * Upbit JSON 응답 → CryptoPriceDto 변환
     * 
     * @param json Upbit에서 받은 JSON 문자열
     * @return CryptoPriceDto 또는 null (파싱 실패 시)
     * 
     *         [Upbit 응답 예시]
     *         {
     *         "type": "ticker",
     *         "code": "KRW-BTC",
     *         "trade_price": 50000000,
     *         "signed_change_price": 100000,
     *         "signed_change_rate": 0.002,
     *         "trade_volume": 0.5,
     *         "acc_trade_volume_24h": 1000,
     *         ...
     *         }
     */
    CryptoPriceDto parseMessage(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);

            // 에러 응답 체크
            if (node.has("error")) {
                log.error("Upbit 에러 응답: {}", node.get("error"));
                return null;
            }

            // ticker 타입만 처리
            if (!node.has("type") || !"ticker".equals(node.get("type").asText())) {
                return null;
            }

            // 마켓 코드에서 심볼 추출: "KRW-BTC" → "BTC"
            String code = node.get("code").asText();
            String symbol = code.replace("KRW-", "");

            return CryptoPriceDto.builder()
                    .symbol(symbol)
                    .baseCurrency("KRW")
                    .marketCode(code)
                    .price(node.path("trade_price").asDouble())
                    .openPrice(node.path("opening_price").asDouble())
                    .highPrice(node.path("high_price").asDouble())
                    .lowPrice(node.path("low_price").asDouble())
                    .volume(node.path("acc_trade_volume_24h").asDouble())
                    .change(node.path("signed_change_price").asDouble())
                    .changePercent(node.path("signed_change_rate").asDouble() * 100)
                    .tradeValue(node.path("acc_trade_price_24h").asDouble())
                    .provider("Upbit")
                    .build();

        } catch (Exception e) {
            log.warn("JSON 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
