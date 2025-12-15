# Backend API Specifications

> **Last Updated**: 2025-12-14
> **Status**: ✅ 핵심 API 구현 완료

---

## API Specification Overview

| File | Domain | Endpoints | Status |
|------|--------|-----------|--------|
| **API_AUTH.md** | 인증 | 4개 | ✅ 완료 |
| **API_PORTFOLIO.md** | 포트폴리오 | 9개 | ✅ 완료 |
| **API_MARKET.md** | 주식 시세 | 4개 | ✅ 완료 |
| **API_CRYPTO.md** | 암호화폐 | 9개 | ✅ 완료 |
| **API_BACKTEST.md** | 백테스팅 | 4개 | ✅ 완료 |
| **API_REBALANCING.md** | 리밸런싱 | 3개 | 🚧 진행중 |

**총 33개 엔드포인트** (90% 구현 완료)

---

## Data Provider Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Sentinel Backend                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                 │
│  │  Portfolio  │  │   Market    │  │   Crypto    │                 │
│  │   Service   │  │   Service   │  │   Service   │                 │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘                 │
│         │                │                │                         │
│         └────────────────┼────────────────┘                         │
│                          │                                          │
│  ┌───────────────────────▼────────────────────────┐                │
│  │            HistoricalDataFacade                 │                │
│  │   (백테스팅용 과거 데이터 통합 라우팅)         │                │
│  └───────────────────────┬────────────────────────┘                │
│                          │                                          │
├──────────────────────────┼──────────────────────────────────────────┤
│         EXTERNAL API PROVIDERS                                      │
│                          │                                          │
│    ┌─────────────────────┼─────────────────────────┐               │
│    │                     │                         │               │
│    ▼                     ▼                         ▼               │
│ ┌─────────┐        ┌─────────────┐          ┌─────────┐           │
│ │  KIS    │        │ AlphaVantage│          │  Upbit  │           │
│ │(한국주식)│        │  (미국주식)  │          │(암호화폐)│           │
│ │50 req/s │        │  5 req/min  │          │600 req/m│           │
│ └─────────┘        └─────────────┘          └─────────┘           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Quick Reference

### 인증 (API_AUTH.md)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/kakao` | POST | 카카오 OAuth 로그인 |
| `/api/v1/auth/refresh` | POST | 토큰 갱신 |
| `/api/v1/auth/me` | GET | 현재 사용자 정보 |
| `/api/v1/auth/logout` | POST | 로그아웃 |

### 포트폴리오 (API_PORTFOLIO.md)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/portfolios` | GET | 사용자 포트폴리오 목록 |
| `/api/v1/portfolios` | POST | 포트폴리오 생성 |
| `/api/v1/portfolios/{id}` | GET | 포트폴리오 상세 |
| `/api/v1/portfolios/{id}` | PUT | 포트폴리오 수정 |
| `/api/v1/portfolios/{id}` | DELETE | 포트폴리오 삭제 |
| `/api/v1/portfolios/{id}/holdings` | POST | 보유종목 추가 |
| `/api/v1/portfolios/{id}/holdings/{hid}` | PUT | 보유종목 수정 |
| `/api/v1/portfolios/{id}/holdings/{hid}` | DELETE | 보유종목 삭제 |
| `/api/v1/portfolios/{id}/recalculate` | POST | 가치 재계산 |

### 주식 시세 (API_MARKET.md)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/market/price/{symbol}` | GET | 단일 주식 시세 |
| `/api/v1/market/prices` | POST | 복수 주식 시세 |
| `/api/v1/market/search` | GET | 종목 검색 |
| `/api/v1/market/indices` | GET | 시장 지수 |

### 암호화폐 (API_CRYPTO.md)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/crypto/price/{symbol}` | GET | 단일 코인 시세 |
| `/api/v1/crypto/prices` | POST | 복수 코인 시세 |
| `/api/v1/crypto/search` | GET | 코인 검색 |
| `/api/v1/crypto/trending` | GET | 트렌딩 코인 |
| `/api/v1/crypto/historical/{symbol}` | GET | 과거 시세 |
| `/api/v1/crypto/status` | GET | 서비스 상태 |
| `/api/v1/crypto/stream/prices` | GET | 실시간 스트리밍 (SSE) |
| `/api/v1/crypto/stream/methods` | GET | 스트리밍 방식 목록 |
| `/api/v1/crypto/stream/status` | GET | 스트리밍 상태 |

### 백테스팅 (API_BACKTEST.md)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/backtest/run` | POST | 백테스트 실행 |
| `/api/v1/backtest/historical/{symbol}` | GET | 과거 시세 조회 |
| `/api/v1/backtest/validate` | POST | 파라미터 검증 |
| `/api/v1/backtest/status` | GET | 서비스 상태 |

### 리밸런싱 (API_REBALANCING.md)
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/rebalancing/recommend` | POST | 리밸런싱 추천 |
| `/api/v1/rebalancing/simulate` | POST | 시뮬레이션 |
| `/api/v1/rebalancing/strategies` | GET | 전략 목록 |

---

## Cache Configuration Summary

| Cache Name | TTL | 용도 |
|------------|-----|------|
| `stockPrice` | 30초 | 주식 현재가 |
| `stockSearch` | 3분 | 종목 검색 |
| `cryptoPrice` | 30초 | 암호화폐 현재가 |
| `cryptoSearch` | 5분 | 코인 검색 |
| `trendingCoins` | 5분 | 트렌딩 코인 |
| `portfolios` | 5분 | 포트폴리오 상세 |
| `kisHistoricalData` | 7일 | KIS 한국주식 일봉 |
| `historicalData` | 7일 | AlphaVantage 미국주식 |
| `cryptoHistoricalData` | 7일 | Upbit 암호화폐 일봉 |

---

## Circuit Breaker Configuration

| Instance | Provider | Failure Threshold | Wait Duration |
|----------|----------|-------------------|---------------|
| `kisApi` | KIS 한국투자증권 | 50% | 15초 |
| `alphaVantageApi` | AlphaVantage | 50% | 60초 |
| `upbitApi` | Upbit | 50% | 15초 |

---

## Usage Guidelines

### Before Implementation
1. **Locate the spec**: 관련 `API_*.md` 파일 확인
2. **Read thoroughly**: 엔드포인트, DTO, 비즈니스 로직 이해
3. **Check examples**: Request/Response JSON 샘플 확인

### During Implementation
1. **Match exactly**: DTO 필드명, 타입, 유효성 규칙 준수
2. **Follow patterns**: Provider Pattern, EntityGraph 활용
3. **Apply optimizations**: Cache TTL, Connection Pool, N+1 방지

### After Implementation
1. **Verify alignment**: 코드와 스펙 일치 확인
2. **Update if changed**: 로직 변경 시 즉시 문서 업데이트
3. **Add examples**: 실제 Request/Response 샘플 추가

---

## Important Rules

1. **Never guess**: 스펙이 불명확하면 사용자에게 확인
2. **No divergence**: 코드와 문서는 항상 일치해야 함
3. **Evidence-based**: 모든 최적화는 k6 측정 결과 기반

---

**Last Updated**: 2025-12-14
**Maintainer**: Claude Code
