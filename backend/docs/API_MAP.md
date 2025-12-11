# 🗺️ API MAP

> **Last Updated**: 2025-11-25  
> **Total Endpoints**: 50+  
> **Base URL**: `/api/v1` (v1), `/api` (common)

---

## 📋 Domain Summary

| Domain | Endpoints | Key Features | SRE Concerns |
|--------|-----------|--------------|--------------|
| **AI** | 3 | Gemini analysis, status, test | ⏱️ Gemini API latency |
| **Backtest** | 2 | Run backtest, status | 🔍 Complex calculation |
| **Crypto** | 11 | Prices, search, trending, streaming | ⚡ High concurrency |
| **Market** | 5 | Stock prices, indices, search | ⚠️ N+1 Risk |
| **Portfolio** | 9 | CRUD portfolio/holdings | 🔄 Transactional |
| **PriceHistory** | 4 | Chart data, latest prices | 🔒 Indexing needed |
| **Rebalancing** | 3 | Recommendations, simulation | 📊 Calculation heavy |
| **User** | 6 | OAuth2, JWT, dev login | 🔐 Security critical |
| **Common** | 3 | Health, dev token, error | ✅ Utility |
| **PerfTest** | 3 | Bulk tokens (perf profile) | ⚠️ Profile-gated |

---

## 🤖 AI Domain (`/api/v1/ai`)

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| POST | `/analyze` | Gemini AI 포트폴리오 분석 | ✅ | ⏱️ External API (Gemini) |
| GET | `/status` | AI 서비스 상태 확인 | ❌ | - |
| POST | `/test` | AI 테스트 (개발용) | ❌ | - |

---

## 🔄 Backtest Domain (`/api/v1/backtest`)

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| POST | `/run` | 백테스팅 실행 | ✅ | 🔍 CPU intensive |
| GET | `/status` | 백테스팅 서비스 상태 | ❌ | - |

---

## 🪙 Crypto Domain (`/api/v1/crypto`)

### Data API

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| GET | `/price/{symbol}` | 단일 암호화폐 가격 조회 | ❌ | ⚡ Cacheable (1m) |
| POST | `/prices` | 배치 암호화폐 가격 조회 | ❌ | ⚠️ N+1 Risk |
| GET | `/search` | 암호화폐 검색 | ❌ | - |
| GET | `/trending` | 트렌딩 코인 (거래대금 상위) | ❌ | ⚡ Cached (1m) |
| GET | `/historical/{symbol}` | 과거 데이터 (캔들스틱) | ❌ | 🔍 DB query |
| GET | `/status` | 서비스 상태 확인 | ❌ | - |

### Streaming API (`/api/v1/crypto/stream`)

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| GET | `/prices` | 실시간 가격 스트리밍 (SSE) | ❌ | ⚡ Reactive (WebFlux) |
| GET | `/methods` | 스트리밍 방식 목록 | ❌ | - |
| GET | `/status` | 스트리밍 서비스 상태 | ❌ | - |

**Streaming Methods**: SSE (권장), WebSocket, LongPolling

---

## 📈 Market Domain (`/api/v1/market`)

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| GET | `/price/{symbol}` | 단일 주식 가격 조회 | ❌ | ⚡ Cacheable (1m) |
| GET | `/prices` | 복수 주식 가격 조회 (Query) | ❌ | ⚠️ N+1 Risk |
| POST | `/prices/batch` | 배치 주식 가격 조회 (Body) | ❌ | ⚠️ N+1 Risk |
| GET | `/indices` | 주요 시장 지수 조회 | ❌ | ⚡ Cached (1m) |
| GET | `/search` | 종목 심볼 검색 | ❌ | - |
| GET | `/status` | 서비스 상태 확인 | ❌ | - |

---

## 💼 Portfolio Domain (`/api/v1/portfolios`)

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| GET | `/` | 사용자 포트폴리오 목록 조회 | ✅ | ✅ EntityGraph (Phase 3) |
| GET | `/{portfolioId}` | 특정 포트폴리오 조회 | ✅ | ✅ Read-only, 백그라운드 업데이트 (Phase 4a) |
| POST | `/` | 포트폴리오 생성 | ✅ | 🔒 Transactional |
| PUT | `/{portfolioId}` | 포트폴리오 수정 | ✅ | 🔒 Transactional |
| DELETE | `/{portfolioId}` | 포트폴리오 삭제 | ✅ | 🔒 Transactional |
| POST | `/{portfolioId}/holdings` | 보유 종목 추가 | ✅ | 🔒 Transactional |
| PUT | `/{portfolioId}/holdings/{holdingId}` | 보유 종목 수정 | ✅ | 🔒 Transactional |
| DELETE | `/{portfolioId}/holdings/{holdingId}` | 보유 종목 삭제 | ✅ | 🔒 Transactional |
| POST | `/{portfolioId}/recalculate` | 포트폴리오 재계산 | ✅ | 🔄 Async로 전환 권장 (Virtual Thread) |

---

## 📉 PriceHistory Domain (`/api/v1/price-history`)

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| GET | `/chart` | 차트 데이터 조회 (시간 범위) | ❌ | 🔍 Indexing on `(symbol, timestamp)` |
| GET | `/latest/{symbol}` | 최신 가격 조회 (단일) | ❌ | - |
| GET | `/latest` | 최신 가격 조회 (복수) | ❌ | ⚠️ N+1 Risk |
| GET | `/symbols` | 자산 유형별 심볼 목록 | ❌ | - |

---

##⚖️ Rebalancing Domain (`/api/v1/rebalancing`)

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| POST | `/recommend` | 리밸런싱 추천 생성 | ✅ | 🔍 Algorithm-heavy |
| POST | `/simulate` | 리밸런싱 시뮬레이션 | ✅ | 🔍 Calculation-heavy |
| GET | `/strategies` | 지원 전략 목록 | ❌ | - |

---

## 👤 User Domain (`/api/v1/auth`)

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| GET | `/kakao` | Kakao 로그인 URL 조회 | ❌ | - |
| GET | `/kakao/callback` | Kakao OAuth2 콜백 | ❌ | 🔐 Security critical |
| POST | `/refresh` | 토큰 갱신 | ❌ | 🔐 JWT validation |
| POST | `/logout` | 로그아웃 | ✅ | 🔒 Redis cleanup |
| GET | `/me` | 현재 사용자 정보 | ✅ | - |
| POST | `/dev-login` | 개발 모드 로그인 | ❌ | ⚠️ Development only |

---

## 🔧 Common Domain (`/api`)

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| GET | `/health` | 헬스 체크 | ❌ | ✅ Monitoring |
| GET | `/dev/token` | 개발용 JWT 토큰 생성 | ❌ | ⚠️ Development only |
| GET | `/dev/verify` | JWT 토큰 검증 | ❌ | ⚠️ Development only |

---

## ⚠️ PerfTest Domain (`/api/v1/auth/perf`) - PROFILE GATED

| Method | Endpoint | Description | Auth | SRE Note |
|--------|----------|-------------|------|----------|
| GET | `/tokens` | 500명 토큰 일괄 발급 | ❌ | ⚠️ Only in `perf` profile |
| GET | `/token/{index}` | 단일 유저 토큰 발급 | ❌ | ⚠️ Only in `perf` profile |
| GET | `/stats` | 성능 테스트 유저 통계 | ❌ | ⚠️ Only in `perf` profile |

---

## 🚨 SRE Risk Matrix

### ⚠️ High Risk (Performance Bottleneck)

| Endpoint | Risk | Mitigation |
|----------|------|------------|
| `POST /api/v1/crypto/prices` | N+1 problem | ✅ Use Caffeine cache (1m TTL) |
| `POST /api/v1/market/prices/batch` | N+1 problem | ✅ Individual calls use cache |
| `POST /api/v1/portfolios/{id}/recalculate` | External API calls | 🔄 Async with Virtual Threads (계획) |
| `GET /api/v1/price-history/chart` | Slow DB query | 🔍 Add index on `(symbol, timestamp)` |
| ~~`GET /api/v1/portfolios/{id}`~~ | ~~N+1 problem~~ | ✅ **SOLVED** (Phase 3: EntityGraph) |

### 🔐 Security Critical

| Endpoint | Risk | Mitigation |
|----------|------|------------|
| `GET /api/v1/auth/kakao/callback` | OAuth2 token leak | ✅ HTTPS only, secure token storage |
| `POST /api/v1/auth/refresh` | JWT forgery | ✅ HMAC-SHA256 signature |
| `GET /api/v1/auth/me` | Unauthorized access | ✅ JWT validation in filter |

---

## 📊 Performance Optimization Checklist

- [ ] Add `@Cacheable` to `MarketDataService.getStockPrice()`
- [ ] Add composite index: `CREATE INDEX idx_price_history_symbol_time ON price_history(symbol, timestamp)`
- [ ] Implement `k6` stress test for `/api/v1/market/prices/batch`
- [ ] Add Resilience4j rate limiter for AlphaVantage API
- [ ] Convert `recalculatePortfolio` to async Virtual Thread
- [ ] Add circuit breaker for Gemini AI API

---

## 🔍 Testing Strategy

### Unit Tests (JUnit 5)
- All Service layer methods
- Custom validators

### Integration Tests (Testcontainers)
- Repository layer with real PostgreSQL
- External API mocking with WireMock

### Performance Tests (k6)
```bash
# Example: Batch price API
k6 run --vus 100 --duration 30s scripts/k6/batch-prices.js
```

---

## 📝 Notes

- All `/api/v1/*` endpoints return JSON
- Streaming endpoints (`/crypto/stream/*`) return `text/event-stream` (SSE)
- Auth required endpoints expect `Authorization: Bearer {token}` header
- Error responses follow RFC 7807 (Problem Details)
