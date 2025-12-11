# Sentinel 프로젝트 초기 분석

> **분석일**: 2024-11-24
> **현재 Phase**: Phase 3b (Cache 효과 검증)

---

## 🏗️ 프로젝트 구조

### 패키지 구조
```
com.pjsent.sentinel/
├── ai/                    # AI 분석 (Gemini)
├── backtest/              # 백테스팅 엔진
├── common/                # 공통 설정/예외
├── config/                # Spring Configuration
├── crypto/                # 암호화폐 데이터
├── market/                # 주식 시장 데이터
├── portfolio/             # 포트폴리오 관리
├── pricehistory/          # 가격 이력
├── rebalancing/           # 리밸런싱 알고리즘
└── user/                  # 인증/사용자 관리
```

### Entity 분석
**총 7개 Entity**:
1. **User** - 사용자
2. **UserSession** - 세션 관리
3. **Portfolio** - 포트폴리오
4. **PortfolioHolding** - 포트폴리오 보유 종목
5. **MarketData** - 시장 데이터
6. **PriceHistory** - 가격 이력
7. **(기타)** - 추가 Entity

### Entity 관계
```
User (1) ←→ (N) Portfolio
Portfolio (1) ←→ (N) PortfolioHolding
User (1) ←→ (N) UserSession
```

---

## 🔍 식별된 N+1 의심 지점

### 1. Portfolio → Holdings 관계
```java
// Portfolio.java
@OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL,
           orphanRemoval = true, fetch = FetchType.LAZY)
private List<PortfolioHolding> holdings = new ArrayList<>();
```

**문제**:
- Portfolio 목록 조회 시 각 Portfolio마다 Holdings를 개별 쿼리로 조회
- 1 + N 쿼리 발생 (N = Portfolio 개수)

**해결 방안** (Phase 3a에서 적용):
- `@EntityGraph` 적용 ✅
- Fetch Join 사용 ✅

---

## 🌐 외부 API 사용 현황

### HTTP Client
- **RestTemplate** 사용 (10개 파일)
- Connection Pool 설정 필요

### API Provider
1. **AlphaVantage** - 주식 데이터
2. **Finnhub** - 주식 데이터
3. **KoreaInvestment** - 한국 주식 (KIS API)
4. **YahooFinance** - 과거 데이터
5. **Upbit** - 암호화폐 (KRW)
6. **Binance** - 암호화폐 (USD)
7. **Gemini** - AI 분석

---

## 🎯 성능 최적화 현황

### Phase 1: Cache TTL 최적화 ✅
- 500 에러: 827건 → **0건** (완전 제거)
- Cache TTL: 1분 → 3분

### Phase 2: Cache Layer 이동 ✅
- 평균 응답: 1,949ms → **10ms** (210배 개선)
- Cache: Provider → Service Layer

### Phase 3a: EntityGraph N+1 해결 ✅
- Portfolio 1:N Holdings Fetch Join
- N+1 쿼리 부분 해결

### Phase 3b: Cache 효과 검증 🔄 (다음 작업)
- CacheManager 분리: Market / Crypto
- TTL 재조정: Market 1분, Crypto 10초

---

## 📊 기술 스택

### Backend
- Spring Boot 3.5.5
- Spring Data JPA
- PostgreSQL
- Caffeine Cache
- Resilience4j (Circuit Breaker)
- JWT Authentication

### API
- RestTemplate (HTTP Client)
- Kakao OAuth
- Multiple Market Data Providers

### Testing
- k6 (부하 테스트)
- JUnit + Mockito

---

## 🎯 다음 단계

1. **Phase 3b**: Cache 효과 검증 (1~2시간)
2. **Phase 3c**: BatchSize로 N+1 완전 해결 (3~5시간)
3. **Phase 4**: Kafka Event-Driven (2~3일)

---

**분석자**: Claude Code
**참조**: `.claude/EXPERIMENT_STATUS.md`, `.claude/ROADMAP.md`
