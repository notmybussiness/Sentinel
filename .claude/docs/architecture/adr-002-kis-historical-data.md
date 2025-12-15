# ADR-002: KIS 일봉 API를 백테스팅 Historical Data Source로 추가

## Status
✅ Implemented (2025-12-14)

## Context

### 현재 상황
- **Real-time 가격**: KIS API (Primary) ✅
  - Circuit Breaker: `kisApi` 설정됨
  - Rate Limiter: 50 req/s

- **Historical Data (백테스팅)**: AlphaVantage API ❌
  - Circuit Breaker: `alphaVantageApi` (방금 추가)
  - Rate Limit: 5 req/min, 100 req/day

### 문제점
1. **API 일관성 부재**: Real-time은 KIS, Historical은 AlphaVantage
2. **국내 주식 지원 불가**: AlphaVantage는 미국 주식 전용
3. **Rate Limit 제약**: AlphaVantage 5 req/min으로 대량 백테스팅 불가
4. **비용**: AlphaVantage Premium 필요 시 추가 비용

### 기술 부채 발견 경로
```
사용자: "I use KIS as main market API but why just added CB on alpha only?"
분석: HistoricalDataService가 AlphaVantage만 사용 중
원인: KIS Provider에 getHistoricalData() 미구현
```

## Decision Options

### Option 1: KIS 일봉 API 추가 (Recommended)
KIS `FHKST03010100` (국내주식 일봉 조회) API 구현

```java
// KoreaInvestmentProvider.java
@Override
public List<StockPriceDto> getHistoricalData(String symbol, int days) {
    String url = buildUrl(DAILY_PRICE_ENDPOINT, Map.of(
        "fid_cond_mrkt_div_code", "J",
        "fid_input_iscd", symbol,
        "fid_org_adj_prc", "0",  // 수정주가
        "fid_period_div_code", "D"  // 일봉
    ));
    // ...
}

@Override
public boolean supportsHistoricalData() {
    return true;
}
```

**장점**:
- ✅ API 일관성 (Real-time + Historical 모두 KIS)
- ✅ 국내 주식 지원
- ✅ Rate Limit 여유 (50 req/s)
- ✅ 추가 비용 없음 (기존 KIS 계약 사용)

**단점**:
- ⚠️ 개발 시간 필요 (2-3시간)
- ⚠️ KIS API 응답 포맷 파싱 추가 작업

### Option 2: 현상 유지 (AlphaVantage)
AlphaVantage 계속 사용

**장점**:
- 추가 개발 불필요
- 미국 주식 데이터 양호

**단점**:
- ❌ Rate Limit 심각 (5 req/min)
- ❌ 국내 주식 미지원
- ❌ API 일관성 부재

### Option 3: Hybrid 접근
- 국내 주식: KIS 일봉 API
- 미국 주식: AlphaVantage

**장점**:
- 양쪽 시장 모두 지원
- 최적의 데이터 소스 사용

**단점**:
- ⚠️ 복잡도 증가
- ⚠️ 두 API 모두 관리 필요

## Recommended Decision

**Option 3: Hybrid 접근** 선택

```
┌─────────────────────────────────────────────────────┐
│              HistoricalDataFacade                   │
├─────────────────────────────────────────────────────┤
│  getHistoricalPrices(symbol, assetType, ...)        │
└─────────────────┬───────────────────────────────────┘
                  │
         ┌───────┴────────┐
         │                │
         ▼                ▼
┌─────────────────┐ ┌─────────────────┐
│ KIS일봉API      │ │ AlphaVantage    │
│ (Korean Stock)  │ │ (US Stock)      │
└─────────────────┘ └─────────────────┘
         │                │
         └───────┬────────┘
                 ▼
┌─────────────────────────────────────────────────────┐
│              CryptoHistoricalDataService            │
│              (Upbit for Crypto)                     │
└─────────────────────────────────────────────────────┘
```

### 구현 로드맵
1. **Phase 1**: KIS 일봉 API 추가 (`KoreaInvestmentProvider`)
2. **Phase 2**: `HistoricalDataFacade` 확장 (국내/해외 분기)
3. **Phase 3**: 캐시 키 전략 업데이트

## Consequences

### Positive
- ✅ 국내 주식 백테스팅 지원
- ✅ Rate Limit 문제 해결 (KIS 50 req/s)
- ✅ API 비용 절감 (AlphaVantage 의존도 감소)
- ✅ Circuit Breaker 일관성 (`kisApi` 재사용)

### Negative
- ⚠️ 개발 시간 투자 필요 (4-6시간)
- ⚠️ 양쪽 API 파싱 로직 유지보수

### Neutral
- 기존 AlphaVantage 테스트 코드 유지 가능
- 점진적 마이그레이션 가능

## Implementation Plan

```
Step 1: KoreaInvestmentProvider 확장 (2h)
├── getHistoricalData() 구현
├── parseDailyPriceResponse() 추가
└── supportsHistoricalData() override

Step 2: HistoricalDataFacade 수정 (1h)
├── 심볼 기반 국내/해외 판별 로직
├── KIS or AlphaVantage 분기
└── 통합 테스트

Step 3: 테스트 및 검증 (1h)
├── 삼성전자 (005930) 백테스팅 테스트
├── AAPL 백테스팅 테스트 (AlphaVantage 유지 확인)
└── 혼합 포트폴리오 테스트
```

## Implementation Results (2025-12-14)

### 구현 완료 항목
| 항목 | 파일 | 상태 |
|------|------|------|
| KisHistoricalDataService | `backtest/service/KisHistoricalDataService.java` | ✅ 신규 |
| HistoricalDataFacade 수정 | `backtest/service/HistoricalDataFacade.java` | ✅ 수정 |
| BacktestController 수정 | `backtest/controller/BacktestController.java` | ✅ 수정 |
| CacheConfig 추가 | `config/CacheConfig.java` | ✅ 수정 |
| 단위 테스트 | `KisHistoricalDataServiceTest.java` | ✅ 신규 |

### 아키텍처
```
HistoricalDataFacade
├── 한국 주식 (6자리 숫자: 005930) → KisHistoricalDataService (KIS API)
├── 미국 주식 (AAPL, GOOGL)        → HistoricalDataService (AlphaVantage)
└── 암호화폐 (BTC, ETH)            → CryptoHistoricalDataService (Upbit)
```

### 실제 테스트 결과
```
삼성전자 (005930) 일봉 데이터 조회 성공!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📅 조회 기간: 2024-11-01 ~ 2024-12-13
📊 데이터 포인트: 31개
📈 시작가: 58,300원 (2024-11-01)
📉 종가:   56,100원 (2024-12-13)
📉 기간 수익률: -3.77%
```

### 캐시 설정
- Cache Name: `kisHistoricalData`
- TTL: 7일
- Circuit Breaker: `kisApi` (기존 KoreaInvestmentProvider와 공유)

## References
- [KIS Open API - 국내주식 일봉 조회](https://apiportal.koreainvestment.com)
- TR ID: `FHKST03010100` (국내주식 기간별 시세)
- Related Files: `KisHistoricalDataService.java`, `HistoricalDataFacade.java`

---
**Date**: 2025-12-14
**Author**: Claude Code
**Triggered by**: User question about KIS vs AlphaVantage Circuit Breaker
**Implemented**: 2025-12-14 (Baby Steps approach)
