# 📡 실시간 스트리밍 방식 비교: SSE vs Long Polling vs WebSocket

> Spring WebFlux 기반 실시간 데이터 스트리밍 구현 패턴 비교

---

## 🎯 개요

| 방식 | 연결 방향 | 프로토콜 | 레이턴시 | 복잡도 |
|------|-----------|----------|----------|--------|
| **SSE** | 단방향 (Server → Client) | HTTP | 낮음 | 낮음 |
| **Long Polling** | 단방향 (Server → Client) | HTTP | 중간 | 낮음 |
| **WebSocket** | 양방향 (Full Duplex) | WS/WSS | 최저 | 높음 |

---

## 1️⃣ Server-Sent Events (SSE)

### 개념
```
Client ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━► Server
          (HTTP 연결 1개 유지, 서버가 계속 Push)

        ◄━━━━━ event: price-update
               data: {"symbol":"BTC","price":50000000}
        
        ◄━━━━━ event: price-update
               data: {"symbol":"BTC","price":50100000}
        
        ◄━━━━━ event: price-update
               data: {"symbol":"BTC","price":50200000}
```

### Spring 서버 코드

```java
/**
 * SSE Controller - 연결당 1개의 Flux 스트림
 */
@RestController
@RequestMapping("/api/v1/sse")
public class SSEController {

    private final CryptoDataService cryptoDataService;

    /**
     * SSE 스트리밍 엔드포인트
     * 
     * produces = TEXT_EVENT_STREAM_VALUE 가 핵심!
     * 이게 있어야 브라우저가 SSE로 인식함
     */
    @GetMapping(value = "/stream", 
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)  // ⬅️ 핵심!
    public Flux<ServerSentEvent<CryptoPriceDto>> streamPrices(
            @RequestParam List<String> symbols) {
        
        return Flux.interval(Duration.ofSeconds(1))  // 1초마다 발행
                .flatMap(tick -> {
                    // 외부 API 호출 (Upbit REST API)
                    List<CryptoPriceDto> prices = cryptoDataService.getPrices(symbols);
                    return Flux.fromIterable(prices);
                })
                .map(price -> ServerSentEvent.<CryptoPriceDto>builder()
                        .event("price-update")    // 이벤트 타입
                        .data(price)              // 실제 데이터
                        .id(UUID.randomUUID().toString())  // 이벤트 ID (재연결 시 사용)
                        .build())
                .doOnSubscribe(sub -> log.info("🔵 SSE 연결 시작"))
                .doOnCancel(() -> log.info("🔴 SSE 연결 종료"));
    }
}
```

### 클라이언트 코드 (JavaScript)

```javascript
// 브라우저 내장 EventSource API 사용
const eventSource = new EventSource('/api/v1/sse/stream?symbols=BTC,ETH');

// 특정 이벤트 타입 수신
eventSource.addEventListener('price-update', (event) => {
    const price = JSON.parse(event.data);
    console.log('가격 업데이트:', price);
    // { symbol: 'BTC', price: 50000000, ... }
});

// 연결 상태 모니터링
eventSource.onopen = () => console.log('✅ SSE 연결됨');
eventSource.onerror = (e) => {
    console.log('❌ SSE 오류 - 자동 재연결 시도');
    // 브라우저가 자동으로 재연결 시도함!
};

// 연결 종료
// eventSource.close();
```

### 특징
- ✅ **자동 재연결**: 브라우저가 끊어지면 자동으로 재연결
- ✅ **간단한 구현**: EventSource API 한 줄로 연결
- ✅ **HTTP 호환**: 프록시, 방화벽 문제 적음
- ❌ **단방향만**: 클라이언트 → 서버 메시지 불가
- ❌ **텍스트만**: 바이너리 데이터 전송 불가

---

## 2️⃣ Long Polling

### 개념
```
Client ──────────────► Server (요청1)
       ◄────────────── 응답1 (데이터 있으면 즉시 반환)
       ⏳ (연결 끊김)

Client ──────────────► Server (요청2)  ← 즉시 다시 요청
       ◄────────────── 응답2
       ⏳ (연결 끊김)

Client ──────────────► Server (요청3)
       ...
```

### Spring 서버 코드

```java
/**
 * Long Polling Controller
 * 
 * SSE와 다른 점: TEXT_EVENT_STREAM이 아닌 일반 JSON 반환
 * 하지만 Flux를 사용하여 데이터가 준비될 때까지 대기
 */
@RestController
@RequestMapping("/api/v1/poll")
public class LongPollingController {

    private final CryptoDataService cryptoDataService;
    
    // 마지막 가격 캐시 (변경 감지용)
    private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();

    /**
     * Long Polling 엔드포인트
     * 
     * - 데이터가 변경될 때까지 대기 (최대 30초)
     * - 변경이 있거나 타임아웃되면 응답
     */
    @GetMapping("/prices")
    public Mono<List<CryptoPriceDto>> pollPrices(
            @RequestParam List<String> symbols,
            @RequestParam(defaultValue = "30") int timeoutSeconds) {
        
        return Flux.interval(Duration.ofMillis(500))  // 500ms마다 체크
                .take(Duration.ofSeconds(timeoutSeconds))  // 최대 대기 시간
                .flatMap(tick -> {
                    List<CryptoPriceDto> prices = cryptoDataService.getPrices(symbols);
                    
                    // 가격 변경 확인
                    boolean hasChange = prices.stream().anyMatch(p -> 
                        !p.getPrice().equals(lastPrices.get(p.getSymbol()))
                    );
                    
                    if (hasChange) {
                        // 캐시 업데이트
                        prices.forEach(p -> lastPrices.put(p.getSymbol(), p.getPrice()));
                        return Mono.just(prices);
                    }
                    return Mono.empty();  // 변경 없으면 스킵
                })
                .next()  // 첫 번째 결과만 반환
                .switchIfEmpty(Mono.defer(() -> {
                    // 타임아웃 시 현재 데이터 반환
                    return Mono.just(cryptoDataService.getPrices(symbols));
                }))
                .doOnSubscribe(sub -> log.info("🔵 Long Polling 시작"))
                .doOnSuccess(data -> log.info("🟢 Long Polling 응답"));
    }
}
```

### 클라이언트 코드 (JavaScript)

```javascript
/**
 * Long Polling 클라이언트
 * 
 * 핵심: 응답 받으면 즉시 다시 요청!
 */
async function startLongPolling(symbols) {
    const url = `/api/v1/poll/prices?symbols=${symbols.join(',')}&timeoutSeconds=30`;
    
    while (true) {  // 무한 루프
        try {
            console.log('🔵 폴링 요청 시작...');
            
            const response = await fetch(url);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            const prices = await response.json();
            console.log('가격 업데이트:', prices);
            
            // UI 업데이트
            updatePriceUI(prices);
            
        } catch (error) {
            console.error('❌ 폴링 오류:', error);
            // 오류 시 잠시 대기 후 재시도
            await new Promise(r => setTimeout(r, 3000));
        }
        
        // 즉시 다음 요청 (루프 계속)
    }
}

// 시작
startLongPolling(['BTC', 'ETH']);

// 중지하려면 AbortController 사용 필요
```

### 특징
- ✅ **단순함**: 일반 HTTP 요청/응답
- ✅ **호환성**: 모든 브라우저, 모든 서버 지원
- ❌ **비효율적**: 요청마다 HTTP 헤더 전송 (오버헤드)
- ❌ **수동 재연결**: 클라이언트가 직접 루프 관리

---

## 3️⃣ WebSocket

### 개념
```
Client ════════════════════════════════════════ Server
           (양방향 Full-Duplex 연결)

Client ──────► 구독 요청: {"type":"subscribe","symbols":["BTC"]}
       ◄────── 가격1: {"symbol":"BTC","price":50000000}
       ◄────── 가격2: {"symbol":"BTC","price":50100000}
Client ──────► 심볼 추가: {"type":"subscribe","symbols":["ETH"]}
       ◄────── 가격3: {"symbol":"ETH","price":3000000}
```

### Spring 서버 코드

```java
/**
 * WebSocket Handler - Reactive WebSocket
 */
@Component
public class CryptoWebSocketHandler implements WebSocketHandler {

    private final CryptoDataService cryptoDataService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("🔵 WebSocket 연결: {}", session.getId());
        
        // 1. 클라이언트로부터 메시지 수신 (구독 요청)
        Flux<WebSocketMessage> input = session.receive()
                .map(msg -> parseMessage(msg.getPayloadAsText()))
                .doOnNext(msg -> log.info("📩 수신: {}", msg));
        
        // 2. 서버에서 클라이언트로 데이터 Push
        Flux<WebSocketMessage> output = Flux.interval(Duration.ofMillis(500))
                .flatMap(tick -> {
                    // 현재 구독 중인 심볼 가격 조회
                    List<CryptoPriceDto> prices = cryptoDataService.getPrices(
                        List.of("BTC", "ETH")  // 실제로는 구독 목록에서 가져옴
                    );
                    return Flux.fromIterable(prices);
                })
                .map(price -> {
                    try {
                        String json = objectMapper.writeValueAsString(price);
                        return session.textMessage(json);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .doOnSubscribe(sub -> log.info("📡 스트리밍 시작"))
                .doFinally(signal -> log.info("🔴 WebSocket 종료: {}", signal));
        
        // 3. 입출력 동시 처리
        return session.send(output)
                .and(input.then());  // 입력도 구독 유지
    }
}

/**
 * WebSocket 설정
 */
@Configuration
public class WebSocketConfig {
    
    @Bean
    public HandlerMapping webSocketHandlerMapping(CryptoWebSocketHandler handler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/crypto", handler);
        
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);  // 높은 우선순위
        return mapping;
    }
    
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
```

### 클라이언트 코드 (JavaScript)

```javascript
/**
 * WebSocket 클라이언트
 */
class CryptoWebSocket {
    constructor(url) {
        this.url = url;
        this.ws = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
    }
    
    connect() {
        this.ws = new WebSocket(this.url);
        
        this.ws.onopen = () => {
            console.log('✅ WebSocket 연결됨');
            this.reconnectAttempts = 0;
            
            // 구독 요청
            this.subscribe(['BTC', 'ETH']);
        };
        
        this.ws.onmessage = (event) => {
            const price = JSON.parse(event.data);
            console.log('가격 업데이트:', price);
            updatePriceUI(price);
        };
        
        this.ws.onclose = (event) => {
            console.log('🔴 WebSocket 종료:', event.code);
            this.attemptReconnect();
        };
        
        this.ws.onerror = (error) => {
            console.error('❌ WebSocket 오류:', error);
        };
    }
    
    // 서버에 메시지 전송 (양방향!)
    subscribe(symbols) {
        this.ws.send(JSON.stringify({
            type: 'subscribe',
            symbols: symbols
        }));
    }
    
    unsubscribe(symbols) {
        this.ws.send(JSON.stringify({
            type: 'unsubscribe',
            symbols: symbols
        }));
    }
    
    attemptReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`🔄 재연결 시도 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
            setTimeout(() => this.connect(), 3000);
        }
    }
    
    close() {
        this.ws.close();
    }
}

// 사용
const ws = new CryptoWebSocket('ws://localhost:8080/ws/crypto');
ws.connect();
```

### 특징
- ✅ **양방향**: 클라이언트 ↔ 서버 실시간 통신
- ✅ **최저 레이턴시**: HTTP 오버헤드 없음
- ✅ **바이너리 지원**: 이미지, 파일 전송 가능
- ❌ **복잡한 구현**: 연결 관리, 재연결, 상태 관리
- ❌ **프록시 문제**: 일부 환경에서 차단될 수 있음

---

## 📊 코드 구조 비교

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Server-Side (Spring)                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐           │
│  │      SSE        │   │  Long Polling   │   │    WebSocket    │           │
│  ├─────────────────┤   ├─────────────────┤   ├─────────────────┤           │
│  │ @GetMapping     │   │ @GetMapping     │   │ WebSocketHandler│           │
│  │ produces=       │   │ (일반 JSON)      │   │ handle()        │           │
│  │ TEXT_EVENT_     │   │                 │   │                 │           │
│  │ STREAM_VALUE    │   │ Mono<List<>>    │   │ Flux + Session  │           │
│  │                 │   │                 │   │                 │           │
│  │ Flux<SSE<T>>    │   │ (단일 응답)      │   │ (양방향 스트림)  │           │
│  └─────────────────┘   └─────────────────┘   └─────────────────┘           │
│           │                    │                      │                     │
│           ▼                    ▼                      ▼                     │
│  ┌─────────────────────────────────────────────────────────────┐           │
│  │          공통: CryptoDataService (Upbit API 호출)            │           │
│  └─────────────────────────────────────────────────────────────┘           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           Client-Side (Browser)                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐           │
│  │      SSE        │   │  Long Polling   │   │    WebSocket    │           │
│  ├─────────────────┤   ├─────────────────┤   ├─────────────────┤           │
│  │ EventSource     │   │ while(true) {   │   │ new WebSocket() │           │
│  │ (내장 API)      │   │   await fetch() │   │                 │           │
│  │                 │   │ }               │   │ ws.send()       │           │
│  │ 자동 재연결 ✅   │   │                 │   │ (양방향 가능!)   │           │
│  │                 │   │ 수동 재연결 ❌   │   │                 │           │
│  │ 단방향 ❌       │   │ 단방향 ❌       │   │ 양방향 ✅        │           │
│  └─────────────────┘   └─────────────────┘   └─────────────────┘           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 현재 Sentinel 구현

### SSEStreamingService.java
```java
@Service
public class SSEStreamingService implements StreamingService {
    
    @Override
    public Flux<CryptoPriceDto> startStreaming(List<String> symbols, String baseCurrency) {
        return Flux.interval(Duration.ofSeconds(1))      // ← 1초마다
                .onBackpressureDrop()                    // ← 백프레셔 처리
                .flatMap(tick -> {
                    List<CryptoPriceDto> prices = cryptoDataService.getBatchCryptoPrices(symbols, baseCurrency);
                    return Flux.fromIterable(prices);
                })
                .filter(Objects::nonNull);
    }
}
```

### LongPollingStreamingService.java (예상)
```java
@Service
public class LongPollingStreamingService implements StreamingService {
    
    @Override
    public Flux<CryptoPriceDto> startStreaming(List<String> symbols, String baseCurrency) {
        // SSE와 거의 동일하게 구현되어 있음
        // 차이점: 클라이언트가 어떻게 처리하느냐
        return Flux.interval(Duration.ofSeconds(1))
                .flatMap(tick -> cryptoDataService.getBatchCryptoPrices(symbols, baseCurrency));
    }
}
```

### 📝 결론

| 항목 | SSE | Long Polling | WebSocket |
|------|-----|--------------|-----------|
| **연결 수** | 1개 유지 | 매번 새로 생성 | 1개 유지 |
| **방향** | 단방향 | 단방향 | 양방향 |
| **자동 재연결** | ✅ 브라우저 지원 | ❌ 수동 구현 | ❌ 수동 구현 |
| **Spring 구현** | `Flux<ServerSentEvent>` | `Mono<List<?>>` | `WebSocketHandler` |
| **클라이언트** | `EventSource` | `fetch()` 루프 | `WebSocket` |
| **권장 사용처** | 대시보드, 알림 | 레거시 시스템 | 트레이딩, 채팅 |

---

## 📁 관련 파일

- [SSEStreamingService.java](file:///c:/Users/zetto/Desktop/Sentinel/backend/src/main/java/com/pjsent/sentinel/crypto/streaming/SSEStreamingService.java)
- [WebSocketStreamingService.java](file:///c:/Users/zetto/Desktop/Sentinel/backend/src/main/java/com/pjsent/sentinel/crypto/streaming/WebSocketStreamingService.java)
- [LongPollingStreamingService.java](file:///c:/Users/zetto/Desktop/Sentinel/backend/src/main/java/com/pjsent/sentinel/crypto/streaming/LongPollingStreamingService.java)
- [CryptoStreamController.java](file:///c:/Users/zetto/Desktop/Sentinel/backend/src/main/java/com/pjsent/sentinel/crypto/controller/CryptoStreamController.java)
