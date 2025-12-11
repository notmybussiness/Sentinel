package com.pjsent.sentinel.crypto.streaming;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pjsent.sentinel.crypto.dto.CryptoPriceDto;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    /**
     * WebSocket 클라이언트 - Spring WebFlux에서 제공
     * ReactorNettyWebSocketClient = Netty 기반 비동기 구현체
     */
    private final WebSocketClient client = new ReactorNettyWebSocketClient();

    /** JSON 파싱용 Jackson ObjectMapper */
    private final ObjectMapper objectMapper = new ObjectMapper();

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
     */
    public Flux<CryptoPriceDto> connect(List<String> symbols) {
        URI uri = URI.create(UPBIT_WS_URL);
        String subscribeMessage = buildSubscriptionMessage(symbols);

        log.info("🔌 Upbit WebSocket 연결 시작. URI: {}, Symbols: {}", uri, symbols);

        // ----------------------------------------------------------------
        // Flux.create() 사용 이유:
        // - client.execute()는 Mono<Void> 반환 → 데이터를 밖으로 못 꺼냄
        // - Flux.create()로 sink를 만들면, 안에서 sink.next(data)로 밀어넣기 가능
        // ----------------------------------------------------------------
        return Flux.create(sink -> {

            client.execute(uri, session -> {
                log.info("✅ WebSocket 연결 성공. Session ID: {}", session.getId());

                // Step 1: 구독 메시지 전송
                // - session.textMessage(): String → WebSocketMessage 변환
                // - Flux.just(): 단일 값을 Flux로 감싸기 (send는 Flux<메시지>를 받음)
                Flux<CryptoPriceDto> inbound = session.receive()
                        // Step 3: 페이로드 추출
                        .map(WebSocketMessage::getPayloadAsText)
                        // Step 4: 디버그 로깅
                        .doOnNext(payload -> log.debug("📨 Raw: {}",
                                payload.substring(0, Math.min(100, payload.length()))))
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

                // Step 2: send와 receive를 동시에 실행
                // Mono.when()은 모든 Publisher가 완료될 때까지 대기하고 Mono<Void> 반환
                return Mono.when(
                        session.send(Flux.just(session.textMessage(subscribeMessage))),
                        inbound.then() // receive 스트림도 Mono<Void>로 변환
                );
            })
                    // subscribe() 호출해야 실제 연결이 시작됨!
                    .subscribe();

        }); // Flux.create 끝
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
