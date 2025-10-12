# 🚀 Sentinel Crypto Integration Plan - Upbit Primary

> **Last Updated**: 2025-01-12
> **Status**: 🔄 Week 1 완료 (60%)
> **Primary API**: Upbit
> **Fallback Strategy**: Upbit → Binance

---

## 📌 Overview

**목표**: Upbit API를 사용하여 암호화폐 시장 데이터를 Sentinel 플랫폼에 통합

**전략**:
- **Primary**: Upbit (한국 최대 거래소, 실시간 WebSocket, 무료)
- **Fallback**: Binance (글로벌 거래소, 높은 신뢰성)
- **Search (Optional)**: CoinGecko (포괄적인 코인 검색)

**예상 기간**: 2-3주

---

## 🎯 Phase 1: Backend - Upbit Provider (Week 1)

### ✅ Day 1-2: Provider Interface & DTOs

**작업 내용**:
1. `crypto` 패키지 생성
2. `CryptoDataProvider` 인터페이스 작성
3. DTOs 작성 (CryptoPriceDto, CryptoSearchResultDto, TrendingCoinsDto)
4. `CryptoDataProviderFactory` 작성

**파일 생성**:
```
backend/src/main/java/com/pjsent/sentinel/crypto/
├── service/
│   ├── provider/
│   │   └── CryptoDataProvider.java        # 인터페이스
│   └── factory/
│       └── CryptoDataProviderFactory.java  # 팩토리
└── dto/
    ├── CryptoPriceDto.java
    ├── CryptoSearchResultDto.java
    └── TrendingCoinsDto.java
```

**완료 조건**:
- [x] CryptoDataProvider 인터페이스 작성 완료 ✅
- [x] 3개 DTO 작성 완료 ✅
- [x] Factory 패턴 구현 완료 ✅
- [x] 컴파일 에러 없음 ✅

---

### ✅ Day 3-4: Upbit REST API Provider

**작업 내용**:
1. `UpbitProvider` 구현 (CryptoDataProvider 인터페이스 구현)
2. Upbit REST API 통합
   - `/v1/ticker` (현재가 조회)
   - `/v1/market/all` (마켓 코드 조회)
   - `/v1/candles/days` (일봉 조회 - Historical data)
3. 에러 핸들링 및 로깅
4. Unit 테스트 작성

**Upbit API 엔드포인트**:
```
Base URL: https://api.upbit.com/v1

# 현재가 조회
GET /ticker?markets=KRW-BTC,KRW-ETH

# 마켓 코드 조회 (전체 코인 리스트)
GET /market/all

# 일봉 조회 (Historical)
GET /candles/days?market=KRW-BTC&count=30
```

**파일 생성**:
```
backend/src/main/java/com/pjsent/sentinel/crypto/
└── service/
    └── provider/
        └── UpbitProvider.java
```

**완료 조건**:
- [x] UpbitProvider 구현 완료 ✅
- [x] 6개 API 메서드 연동 완료 ✅
- [x] 에러 핸들링 구현 ✅
- [x] 실제 API 호출 테스트 성공 ✅

---

### ✅ Day 5: Service Layer & Controller

**작업 내용**:
1. `CryptoDataService` 구현
   - Provider Factory 통합
   - Fallback 전략 (Upbit → Binance)
   - 에러 핸들링
2. `CryptoDataController` 구현
   - REST 엔드포인트 정의
   - Request/Response DTO 매핑
   - API 문서화 (Swagger)

**엔드포인트 설계**:
```
# 단일 암호화폐 가격 조회
GET /api/v1/crypto/price/{symbol}?baseCurrency=KRW

# 배치 가격 조회
POST /api/v1/crypto/prices
Body: { "symbols": ["BTC", "ETH"], "baseCurrency": "KRW" }

# 암호화폐 검색
GET /api/v1/crypto/search?query=bitcoin

# 트렌딩 코인 (거래량 Top 10)
GET /api/v1/crypto/trending?baseCurrency=KRW&limit=10

# 서비스 상태
GET /api/v1/crypto/status
```

**파일 생성**:
```
backend/src/main/java/com/pjsent/sentinel/crypto/
├── service/
│   └── CryptoDataService.java
└── controller/
    └── CryptoDataController.java
```

**완료 조건**:
- [x] CryptoDataService 구현 완료 ✅
- [x] CryptoDataController 구현 완료 ✅
- [x] 모든 엔드포인트 curl 테스트 성공 ✅
- [x] Swagger 문서 생성 확인 ✅

---

### ✅ Day 6-7: Configuration & Testing

**작업 내용**:
1. `application.yml` 설정 추가
2. 환경 변수 설정 가이드 작성
3. Integration 테스트 작성
4. API 문서 작성 (`.claude/backend/API_CRYPTO.md`)

**Configuration**:
```yaml
# application.yml
crypto:
  market:
    upbit:
      enabled: ${UPBIT_ENABLED:true}
      base-url: https://api.upbit.com/v1
      # Upbit은 public API에 API 키 불필요
      timeout: 10000

    binance:
      enabled: ${BINANCE_ENABLED:true}
      base-url: https://api.binance.com/api/v3
      timeout: 10000
```

**완료 조건**:
- [x] application.yml 설정 완료 ✅
- [x] Backend 빌드 및 실행 성공 ✅
- [ ] API_CRYPTO.md 문서 작성 (Optional)
- [x] 전체 테스트 통과 ✅

---

## 🎯 Phase 2: Backend - Upbit WebSocket (Week 2)

### ✅ Day 1-3: WebSocket Service

**작업 내용**:
1. `CryptoWebSocketService` 구현
2. Upbit WebSocket 연결
   - `wss://api.upbit.com/websocket/v1`
   - Ticker 스트림 구독
3. 실시간 가격 캐시 (ConcurrentHashMap)
4. Auto-reconnect 로직

**Upbit WebSocket 메시지 포맷**:
```json
// Subscribe Request
[
  {"ticket": "test"},
  {"type": "ticker", "codes": ["KRW-BTC", "KRW-ETH"]},
  {"format": "DEFAULT"}
]

// Response
{
  "type": "ticker",
  "code": "KRW-BTC",
  "trade_price": 45000000,
  "change": "RISE",
  "change_rate": 0.025,
  ...
}
```

**파일 생성**:
```
backend/src/main/java/com/pjsent/sentinel/crypto/
└── service/
    └── CryptoWebSocketService.java
```

**완료 조건**:
- [ ] WebSocket 연결 성공
- [ ] 실시간 가격 업데이트 확인
- [ ] Auto-reconnect 동작 확인
- [ ] 가격 캐시 동작 확인

---

### ✅ Day 4-5: SSE Streaming

**작업 내용**:
1. `CryptoStreamController` 구현 (SSE)
2. Frontend용 실시간 스트리밍 엔드포인트
3. 성능 테스트 (동시 연결)

**SSE 엔드포인트**:
```
GET /api/v1/crypto/stream/prices?symbols=BTC,ETH&baseCurrency=KRW
→ Server-Sent Events (EventSource)
```

**파일 생성**:
```
backend/src/main/java/com/pjsent/sentinel/crypto/
└── controller/
    └── CryptoStreamController.java
```

**완료 조건**:
- [ ] SSE 엔드포인트 구현 완료
- [ ] 브라우저 EventSource 테스트 성공
- [ ] 동시 10개 연결 성능 테스트 통과

---

### ✅ Day 6-7: Binance Fallback Provider

**작업 내용**:
1. `BinanceProvider` 구현 (CryptoDataProvider 인터페이스)
2. Binance REST API 통합
3. Fallback 전략 테스트
4. Provider Factory 업데이트

**Binance API 엔드포인트**:
```
Base URL: https://api.binance.com/api/v3

# 현재가 조회
GET /ticker/24hr?symbol=BTCUSDT

# 마켓 정보
GET /exchangeInfo

# Historical
GET /klines?symbol=BTCUSDT&interval=1d
```

**파일 생성**:
```
backend/src/main/java/com/pjsent/sentinel/crypto/
└── service/
    └── provider/
        └── BinanceProvider.java
```

**완료 조건**:
- [ ] BinanceProvider 구현 완료
- [ ] Fallback 시나리오 테스트 통과
- [ ] Factory에 Binance 추가 완료

---

## 🎯 Phase 3: Frontend Integration (Week 3)

### ✅ Day 1-2: API Client & Types

**작업 내용**:
1. `lib/api/crypto.ts` 생성
2. TypeScript types 정의
3. REST API 함수 구현
4. SSE 연결 함수 구현

**파일 생성**:
```
frontend/
├── lib/
│   └── api/
│       └── crypto.ts          # Crypto API client
└── types/
    └── index.ts               # CryptoPrice, TrendingCoin 타입 추가
```

**API 함수**:
```typescript
// REST API
getCryptoPrice(symbol: string, baseCurrency: string)
getBatchCryptoPrices(symbols: string[], baseCurrency: string)
searchCrypto(query: string)
getTrendingCoins(baseCurrency: string, limit: number)

// SSE Stream
subscribeToCryptoPrices(symbols: string[], onUpdate: callback)
```

**완료 조건**:
- [x] crypto.ts 작성 완료 ✅
- [x] TypeScript 타입 정의 완료 ✅
- [x] 모든 API 함수 테스트 성공 ✅
- [ ] SSE 연결 테스트 (Week 2)

---

### ✅ Day 3-5: UI Components

**작업 내용**:
1. `TrendingCryptoCard` 컴포넌트
2. HomePage 업데이트 (실시간 가격)
3. `CryptoMarketPage` 생성 (검색 + 목록)
4. `AddCryptoHoldingModal` 생성

**파일 생성**:
```
frontend/
├── components/
│   ├── crypto/
│   │   ├── TrendingCryptoCard.tsx
│   │   └── CryptoPriceDisplay.tsx
│   └── portfolio/
│       └── AddCryptoHoldingModal.tsx
└── app/
    └── market/
        └── crypto/
            └── page.tsx       # CryptoMarketPage
```

**완료 조건**:
- [x] TrendingCryptoCard 구현 완료 ✅
- [x] HomePage에 crypto 섹션 추가 ✅
- [x] QueryClientProvider 설정 ✅
- [ ] CryptoMarketPage 구현 (Optional)
- [ ] AddCryptoHoldingModal 구현 (Week 3)

---

### ✅ Day 6-7: Portfolio Integration & Real-time Updates

**작업 내용**:
1. Portfolio Holdings에 crypto 지원 추가
2. 실시간 가격 업데이트 (SSE)
3. Currency selector (KRW, USDT, BTC)
4. E2E 테스트 (Playwright)

**Database Migration**:
```sql
-- portfolio_holdings 테이블에 컬럼 추가
ALTER TABLE portfolio_holdings
ADD COLUMN asset_type VARCHAR(20) DEFAULT 'STOCK' NOT NULL;

ALTER TABLE portfolio_holdings
ADD COLUMN base_currency VARCHAR(10) DEFAULT 'USD';
```

**완료 조건**:
- [ ] Database migration 완료
- [ ] Portfolio에 crypto holdings 추가 가능
- [ ] 실시간 가격 업데이트 동작 확인
- [ ] E2E 테스트 5개 이상 작성

---

## 📋 Upbit API 정보

### API Key 요구사항

**Public API (API Key 불필요)**:
- ✅ 현재가 조회 (`/v1/ticker`)
- ✅ 마켓 코드 조회 (`/v1/market/all`)
- ✅ 호가 정보 (`/v1/orderbook`)
- ✅ 체결 내역 (`/v1/trades/ticks`)
- ✅ 캔들 데이터 (`/v1/candles/*`)
- ✅ WebSocket 구독

**Private API (API Key 필요 - 현재 미사용)**:
- ⚠️ 계좌 조회
- ⚠️ 주문하기
- ⚠️ 주문 취소

**결론**: Sentinel은 Public API만 사용하므로 **API Key 불필요** ✅

### Rate Limits

```
Upbit Public API:
- REST API: 분당 600회 (IP 기준)
- WebSocket: 연결 제한 없음
- Cost: FREE

Binance (Fallback):
- REST API: 분당 1200 weight
- WebSocket: 연결 300개/5분
- Cost: FREE
```

---

## 🔑 환경 변수 설정

### Backend (`application.yml`)

```yaml
crypto:
  market:
    upbit:
      enabled: true
      base-url: https://api.upbit.com/v1
      websocket-url: wss://api.upbit.com/websocket/v1
      timeout: 10000

    binance:
      enabled: true
      base-url: https://api.binance.com/api/v3
      timeout: 10000
```

### Frontend (`.env.local`)

```bash
# 기존 환경 변수는 유지
# Crypto 관련 추가 환경 변수 없음 (Backend에서 처리)
```

---

## ✅ 완료 체크리스트

### Phase 1: Backend REST API (Week 1) ✅ 완료
- [x] Day 1-2: Provider Interface & DTOs ✅
- [x] Day 3-4: Upbit REST API Provider ✅
- [x] Day 5: Service Layer & Controller ✅
- [x] Day 6-7: Configuration & Testing ✅
- [x] Bonus: Frontend 통합 완료 ✅

### Phase 2: Backend WebSocket (Week 2)
- [ ] Day 1-3: WebSocket Service
- [ ] Day 4-5: SSE Streaming
- [ ] Day 6-7: Binance Fallback Provider

### Phase 3: Frontend Integration (Week 3)
- [ ] Day 1-2: API Client & Types
- [ ] Day 3-5: UI Components
- [ ] Day 6-7: Portfolio Integration & Real-time Updates

---

## 📝 다음 단계

### 🎯 Week 1 완료 요약 (2025-01-12)

**구현 완료 내용:**
1. ✅ Backend: Provider, Service, Controller (10개 파일)
2. ✅ Frontend: API Client, TrendingCryptoCard, 홈페이지 통합
3. ✅ 6개 REST API 엔드포인트 작동 확인
4. ✅ Upbit 실시간 데이터 (BTC, ETH, XRP 등) 조회 성공

**테스트 결과:**
- ✅ Backend: http://localhost:8080 정상 작동
- ✅ Frontend: http://localhost:3000 정상 작동
- ✅ 홈페이지에 트렌딩 암호화폐 Top 5 표시

### 🎯 다음 작업: Week 2

**시작 명령**:
```
"Phase 4 Week 2 시작: Upbit WebSocket Service 구현해줘"
```

---

## 📚 참고 문서

### Upbit API 문서
- **공식 문서**: https://docs.upbit.com/
- **REST API**: https://docs.upbit.com/reference/REST-API
- **WebSocket**: https://docs.upbit.com/reference/WebSocket

### 프로젝트 내부 문서
- **Backend API 스펙**: `.claude/backend/API_CRYPTO.md` (작성 예정)
- **Frontend Components**: `.claude/frontend/COMPONENTS.md`
- **메인 가이드**: `.claude/CLAUDE.md`

---

**Last Updated**: 2025-01-12
**Next Action**: Phase 2 (Week 2) WebSocket 구현
**Status**: 🔄 Week 1 완료 (60%)
