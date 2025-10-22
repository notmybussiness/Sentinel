# Sentinel Investment Dashboard - 프로젝트 현황 분석

> **분석 일시**: 2025-10-22
> **현재 Phase**: Phase 7 완료 (Rebalancing Algorithm)
> **다음 Phase**: Phase 8 (Testing & Deployment)

---

## 📊 전체 아키텍처 현황

### Backend Architecture
```
Spring Boot 3.5.5
├── 인증 (Authentication)
│   ├── ✅ Kakao OAuth 2.0
│   ├── ✅ JWT Token (Access + Refresh)
│   └── ✅ 개발자 모드 로그인
│
├── 시장 데이터 (Market Data)
│   ├── ✅ Provider Factory 패턴
│   ├── ✅ KoreaInvestmentProvider (Primary)
│   ├── ✅ AlphaVantageProvider (Fallback)
│   ├── ✅ FinnhubProvider (Fallback)
│   └── ✅ YahooFinanceProvider (Final Fallback)
│
├── 암호화폐 (Crypto)
│   ├── ✅ UpbitProvider (KRW)
│   ├── ✅ BinanceProvider (USD)
│   └── ✅ 실시간 스트리밍 (SSE, WebSocket, LongPolling)
│
├── 포트폴리오 (Portfolio)
│   ├── ✅ CRUD Operations
│   ├── ✅ Stock + Crypto Holdings
│   ├── ✅ 실시간 가격 업데이트
│   └── ✅ 자동 시드 데이터 (DataInitializer)
│
├── AI 분석 (AI Analysis)
│   ├── ✅ Gemini 2.5 Pro
│   ├── ✅ 5가지 분석 타입
│   └── ✅ 포트폴리오 인사이트
│
├── 백테스팅 (Backtesting)
│   ├── ✅ BacktestEngine
│   ├── ✅ PerformanceCalculator
│   ├── ✅ 7가지 성과 지표
│   └── ✅ HistoricalDataService (AlphaVantage)
│
└── 리밸런싱 (Rebalancing)
    ├── ✅ RebalancingService
    ├── ✅ Equal Weight 전략
    ├── ✅ BUY/SELL/HOLD 추천
    └── ✅ 거래 비용/세금 계산
```

### Frontend Architecture
```
Next.js 14 (App Router)
├── ✅ 인증 플로우 (Kakao + Dev Mode)
├── ✅ 포트폴리오 대시보드
├── ✅ 시장 데이터 뷰어
├── ✅ 암호화폐 트렌딩
├── ✅ AI 분석 모달
├── ✅ 백테스팅 Lab
└── ✅ 리밸런싱 추천
```

---

## 🔌 API 엔드포인트 현황

### 1️⃣ Authentication API
| Method | Endpoint | Status | FE 연동 | 비고 |
|--------|----------|--------|---------|------|
| GET | `/api/v1/auth/kakao/callback` | ✅ | ✅ | Kakao OAuth |
| POST | `/api/v1/auth/dev-login` | ✅ | ✅ | 개발자 모드 |
| POST | `/api/v1/auth/refresh` | ✅ | ✅ | 토큰 갱신 |
| POST | `/api/v1/auth/logout` | ✅ | ✅ | 로그아웃 |

### 2️⃣ Market Data API
| Method | Endpoint | Status | FE 연동 | 비고 |
|--------|----------|--------|---------|------|
| GET | `/api/v1/market/price/{symbol}` | ✅ | ✅ | 단일 종목 가격 |
| GET | `/api/v1/market/prices?symbols=` | ✅ | ⚠️ | Query 방식 (사용 안함) |
| POST | `/api/v1/market/prices` | ✅ | ✅ | 배치 가격 조회 |
| GET | `/api/v1/market/indices` | ✅ | ✅ | 주요 지수 (S&P, NASDAQ 등) |
| GET | `/api/v1/market/search?query=` | ✅ | ✅ | 종목 검색 |
| GET | `/api/v1/market/status` | ✅ | ❌ | 서비스 상태 |

**Provider 우선순위**:
1. KoreaInvestmentProvider (한국투자증권 API)
2. AlphaVantageProvider (Fallback)
3. FinnhubProvider (Fallback)
4. YahooFinanceProvider (최종 Fallback)

**⚠️ 현재 상태**:
- KIS API 키 설정 필요 (`application-secret.yml`)
- 설정 전까지 Yahoo Finance Fallback 사용 중

### 3️⃣ Crypto API
| Method | Endpoint | Status | FE 연동 | 비고 |
|--------|----------|--------|---------|------|
| GET | `/api/v1/crypto/price/{symbol}` | ✅ | ✅ | 단일 암호화폐 가격 |
| POST | `/api/v1/crypto/prices` | ✅ | ✅ | 배치 가격 조회 |
| GET | `/api/v1/crypto/trending` | ✅ | ✅ | 거래대금 상위 10개 |
| GET | `/api/v1/crypto/search?query=` | ✅ | ✅ | 암호화폐 검색 |
| GET | `/api/v1/crypto/historical/{symbol}` | ✅ | ❌ | 과거 데이터 |
| GET | `/api/v1/crypto/status` | ✅ | ❌ | 서비스 상태 |

**실시간 스트리밍**:
| Method | Endpoint | Status | FE 연동 | 비고 |
|--------|----------|--------|---------|------|
| GET | `/api/v1/crypto/stream/prices?method=SSE` | ✅ | ❌ | Server-Sent Events |
| GET | `/api/v1/crypto/stream/prices?method=WEBSOCKET` | ✅ | ❌ | WebSocket |
| GET | `/api/v1/crypto/stream/prices?method=LONGPOLLING` | ✅ | ❌ | Long Polling |

**Provider 현황**:
- ✅ UpbitProvider: KRW 거래 (Primary)
- ✅ BinanceProvider: USD 거래 (Secondary)

### 4️⃣ Portfolio API
| Method | Endpoint | Status | FE 연동 | 비고 |
|--------|----------|--------|---------|------|
| GET | `/api/v1/portfolios` | ✅ | ✅ | 포트폴리오 목록 |
| GET | `/api/v1/portfolios/{id}` | ✅ | ✅ | 포트폴리오 상세 |
| POST | `/api/v1/portfolios` | ✅ | ✅ | 포트폴리오 생성 |
| PUT | `/api/v1/portfolios/{id}` | ✅ | ✅ | 포트폴리오 수정 |
| DELETE | `/api/v1/portfolios/{id}` | ✅ | ✅ | 포트폴리오 삭제 |
| POST | `/api/v1/portfolios/{id}/holdings` | ✅ | ✅ | Holding 추가 |
| PUT | `/api/v1/portfolios/{id}/holdings/{hid}` | ✅ | ✅ | Holding 수정 |
| DELETE | `/api/v1/portfolios/{id}/holdings/{hid}` | ✅ | ✅ | Holding 삭제 |
| POST | `/api/v1/portfolios/{id}/recalculate` | ✅ | ✅ | 실시간 재계산 |

**자동 시드 데이터** (DataInitializer):
- 개발자 계정 자동 생성 (`dev@sentinel.com`)
- 샘플 포트폴리오 5개 자동 생성
- BTC, ETH, SOL, XRP, BNB 자동 추가

### 5️⃣ AI Analysis API
| Method | Endpoint | Status | FE 연동 | 비고 |
|--------|----------|--------|---------|------|
| POST | `/api/v1/ai/analyze` | ✅ | ✅ | Gemini 2.5 Pro 분석 |
| GET | `/api/v1/ai/status` | ✅ | ❌ | 서비스 상태 |

**분석 타입**:
- OVERVIEW: 포트폴리오 전체 개요
- RISK: 리스크 분석 및 진단
- DIVERSIFICATION: 분산 투자 평가
- OPTIMIZATION: 최적화 제안
- CUSTOM: 커스텀 질문

### 6️⃣ Backtesting API
| Method | Endpoint | Status | FE 연동 | 비고 |
|--------|----------|--------|---------|------|
| POST | `/api/v1/backtest/run` | ✅ | ✅ | 백테스팅 실행 |
| GET | `/api/v1/backtest/status` | ✅ | ❌ | 서비스 상태 |

**성과 지표** (7가지):
1. Total Return (누적 수익률)
2. Annualized Return (연평균 수익률)
3. Sharpe Ratio (샤프 비율)
4. Max Drawdown (최대 낙폭)
5. Volatility (변동성)
6. Win Rate (승률)
7. CAGR (연평균 성장률)

### 7️⃣ Rebalancing API
| Method | Endpoint | Status | FE 연동 | 비고 |
|--------|----------|--------|---------|------|
| POST | `/api/v1/rebalancing/recommend` | ✅ | ✅ | 리밸런싱 추천 |
| POST | `/api/v1/rebalancing/simulate` | ✅ | ❌ | 시뮬레이션 |
| GET | `/api/v1/rebalancing/strategies` | ✅ | ✅ | 전략 목록 |

**전략**:
- EQUAL_WEIGHT: 동일 비중 (현재 구현됨)
- MARKET_CAP_WEIGHTED: 시가총액 가중 (예정)
- RISK_PARITY: 리스크 패리티 (예정)

---

## 📱 Frontend 연동 현황

### ✅ 완전 연동 (Real API)
| 페이지/기능 | API | Mock 여부 | 비고 |
|------------|-----|-----------|------|
| 로그인 | `/auth/kakao/callback` | ❌ | OAuth + Dev Mode |
| 포트폴리오 목록 | `/portfolios` | ❌ | 실시간 데이터 |
| 포트폴리오 상세 | `/portfolios/{id}` | ❌ | Holdings 포함 |
| 종목 추가 | `/portfolios/{id}/holdings` | ❌ | Stock + Crypto |
| 종목 검색 | `/market/search`, `/crypto/search` | ❌ | 실시간 검색 |
| 시장 지수 | `/market/indices` | ❌ | S&P, NASDAQ 등 |
| 트렌딩 코인 | `/crypto/trending` | ❌ | 거래대금 상위 |
| AI 분석 | `/ai/analyze` | ❌ | Gemini 2.5 Pro |
| 백테스팅 | `/backtest/run` | ❌ | 7가지 지표 |
| 리밸런싱 | `/rebalancing/recommend` | ❌ | Equal Weight |

### ⚠️ Mock 데이터 사용 (프론트엔드)
| 컴포넌트 | 파일 | Mock 사용 이유 |
|----------|------|---------------|
| 추천 포트폴리오 | `RecommendedPortfolioCard.tsx` | Backend 구현 예정 |
| 시장 개요 (일부) | `page.tsx` | 일부 지표 Mock |

**Mock 데이터 위치**:
- `frontend/lib/mockData.ts` (442줄)
- `MockDataBadge` 컴포넌트로 시각적 구분

---

## 💡 발전 방향 제안

### 🎯 Phase 8: Testing & Deployment (Next)
**우선순위: 높음 | 기간: 2주**

#### 8.1 테스팅
- [ ] Unit Test (JUnit 5) - 목표: >80% 커버리지
  - Service Layer: PortfolioService, MarketDataService
  - Provider Layer: KoreaInvestmentProvider, UpbitProvider
- [ ] Integration Test (MockMvc)
  - Controller 통합 테스트
  - JWT 인증 플로우
- [ ] E2E Test (Playwright)
  - 포트폴리오 생성 → Holdings 추가 → AI 분석

#### 8.2 배포 준비
- [ ] AWS 인프라 설정 (EC2, RDS PostgreSQL)
- [ ] CI/CD 파이프라인 (GitHub Actions)
- [ ] 환경 변수 관리 (AWS Secrets Manager)

---

### 🚀 Phase 9-11: 고급 기능 (3개월)

## 1️⃣ 거래 기능 (Trading) ⭐⭐⭐
**우선순위: 최고 | 포트폴리오 완성도: 매우 높음**

### Backend 설계
```java
trading/
├── controller/
│   └── TradingController.java
├── service/
│   ├── TradingService.java           // 주문 관리
│   ├── OrderExecutionService.java    // 실제 거래 실행
│   └── TradingSimulationService.java // 페이퍼 트레이딩
├── entity/
│   ├── Order.java                    // 주문 엔티티
│   ├── Trade.java                    // 체결 내역
│   └── TradingAccount.java           // 거래 계좌
├── dto/
│   ├── OrderRequest.java
│   ├── OrderResponse.java
│   └── TradingHistoryDto.java
└── provider/
    ├── TradingProvider.java          // 인터페이스
    ├── KoreaInvestmentTradingProvider.java  // 한투 실거래
    ├── UpbitTradingProvider.java     // 업비트 실거래
    └── SimulatedTradingProvider.java // 페이퍼 트레이딩
```

### 핵심 API 엔드포인트
```yaml
POST /api/v1/trading/orders              # 주문 생성
  - Market Order (시장가)
  - Limit Order (지정가)
  - Stop Loss (손절)

GET /api/v1/trading/orders/{id}          # 주문 상세
PUT /api/v1/trading/orders/{id}/cancel   # 주문 취소

GET /api/v1/trading/history              # 거래 내역
  - Filter: 기간, 종목, 주문 타입

POST /api/v1/trading/simulate            # 페이퍼 트레이딩
  - 실제 가격으로 시뮬레이션
  - 가상 계좌 관리

GET /api/v1/trading/accounts             # 거래 계좌 목록
POST /api/v1/trading/accounts            # 계좌 연결
```

### 핵심 기능
1. **페이퍼 트레이딩** (Phase 9 우선)
   - 실제 가격으로 시뮬레이션
   - 거래 비용/세금 계산
   - Portfolio에 자동 반영

2. **실거래 통합** (Phase 10)
   - 한국투자증권 Open Trading API
   - Upbit OpenAPI (암호화폐)
   - OAuth 2.0 계좌 연결

3. **리스크 관리**
   - Stop Loss 자동 설정
   - Position Size 계산
   - 일일 거래 한도

### 기술적 고려사항
- **보안**: OAuth 2.0, API Key 암호화
- **비동기 처리**: CompletableFuture, @Async
- **트랜잭션**: Distributed Transaction (2PC)
- **실시간 체결**: WebSocket 통합

**포트폴리오 가치**: ⭐⭐⭐⭐⭐
- 실무에서 바로 사용 가능한 수준
- 취업/이직 시 큰 어필 포인트

---

## 2️⃣ 고급 백테스팅 엔진 ⭐⭐⭐
**우선순위: 높음 | 포트폴리오 완성도: 높음**

### 현재 상태
✅ 기본 백테스팅 완료 (7가지 지표)
- Total Return, CAGR, Sharpe Ratio
- Max Drawdown, Volatility, Win Rate

### 추가 기능
1. **Monte Carlo 시뮬레이션**
   - 1,000+ 시나리오 생성
   - 확률 분포 분석
   - 최악의 시나리오 예측

2. **멀티 전략 비교**
   - Equal Weight vs. Market Cap
   - Dynamic Rebalancing vs. Buy & Hold
   - 전략별 Sharpe Ratio 비교

3. **리밸런싱 최적화**
   - 최적 리밸런싱 주기 찾기
   - 거래 비용 고려한 임계값 설정

4. **퀀트 팩터 분석**
   - Momentum, Value, Quality
   - Factor Tilting 백테스트

### Backend 구조
```java
backtest/
├── engine/
│   ├── MonteCarloEngine.java
│   ├── StrategyComparisonEngine.java
│   └── FactorAnalysisEngine.java
├── strategy/
│   ├── TradingStrategy.java (인터페이스)
│   ├── MomentumStrategy.java
│   ├── ValueStrategy.java
│   └── MeanReversionStrategy.java
└── dto/
    ├── MonteCarloResult.java
    ├── StrategyComparisonDto.java
    └── FactorAnalysisDto.java
```

**포트폴리오 가치**: ⭐⭐⭐⭐
- 퀀트 개발자 포지션에 적합
- 금융공학 지식 어필

---

## 3️⃣ 알림 & 트리거 시스템 ⭐⭐
**우선순위: 중간 | 포트폴리오 완성도: 중간**

### 기능
1. **가격 알림**
   - "AAPL이 $180 이하로 떨어지면 알림"
   - "BTC가 10% 상승하면 알림"

2. **리밸런싱 알림**
   - "목표 비중과 5% 이상 차이나면 알림"
   - "최적 리밸런싱 시점 도달 시 알림"

3. **AI 인사이트 알림**
   - 포트폴리오 리스크 급등 시
   - 새로운 투자 기회 발견 시

### Backend 구조
```java
notification/
├── controller/NotificationController.java
├── service/
│   ├── NotificationService.java
│   ├── TriggerService.java           // 트리거 관리
│   └── AlertScheduler.java           // 주기적 체크
├── entity/
│   ├── Notification.java
│   └── Trigger.java                  // 트리거 설정
├── channel/
│   ├── EmailNotificationChannel.java
│   ├── SlackNotificationChannel.java
│   └── WebPushNotificationChannel.java
└── dto/
    ├── CreateTriggerRequest.java
    └── NotificationDto.java
```

**포트폴리오 가치**: ⭐⭐⭐
- 실무적 기능
- 이벤트 드리븐 아키텍처 경험

---

## 4️⃣ 소셜 기능 (공유 & 랭킹) ⭐
**우선순위: 낮음 | 포트폴리오 완성도: 중간**

### 기능
1. **포트폴리오 공유**
   - Public Link 생성
   - 익명화된 수익률 공유
   - 복사 가능한 전략

2. **리더보드**
   - 수익률 랭킹 (익명)
   - 전략별 상위 포트폴리오
   - 팔로우 기능

3. **커뮤니티 피드**
   - 투자 인사이트 공유
   - 댓글 & 리액션

### Backend 구조
```java
social/
├── controller/SocialController.java
├── service/
│   ├── SharingService.java
│   ├── LeaderboardService.java
│   └── FeedService.java
├── entity/
│   ├── SharedPortfolio.java
│   ├── Follow.java
│   └── Post.java
└── dto/
    ├── ShareLinkDto.java
    ├── LeaderboardDto.java
    └── PostDto.java
```

**포트폴리오 가치**: ⭐⭐
- SNS 경험 어필
- 확장성 설계 경험

---

## 5️⃣ 세금 최적화 (Tax-Loss Harvesting) ⭐⭐⭐
**우선순위: 중상 | 포트폴리오 완성도: 매우 높음**

### 기능
1. **Tax-Loss Harvesting**
   - 손실 종목 매도 → 세금 절감
   - 비슷한 종목으로 대체 (Wash Sale 방지)

2. **세금 시뮬레이션**
   - 예상 양도소득세 계산
   - 최적 매도 시점 추천

3. **배당 최적화**
   - 배당 일정 관리
   - 세금 효율적인 포트폴리오 구성

### Backend 구조
```java
tax/
├── controller/TaxController.java
├── service/
│   ├── TaxCalculationService.java
│   ├── TaxOptimizationService.java
│   └── TaxReportService.java
├── dto/
│   ├── TaxSimulationRequest.java
│   ├── TaxOptimizationDto.java
│   └── TaxReportDto.java
└── calculator/
    ├── CapitalGainsTaxCalculator.java
    └── DividendTaxCalculator.java
```

**포트폴리오 가치**: ⭐⭐⭐⭐⭐
- 핀테크 도메인 전문성
- 실무적 가치 매우 높음

---

## 📋 추천 로드맵 (3개월)

### Month 1: Phase 8-9
**Week 1-2**: Testing & Deployment
- Unit Test, Integration Test
- AWS 배포 준비

**Week 3-4**: 페이퍼 트레이딩
- TradingService 기본 구조
- SimulatedTradingProvider
- 주문 생성/취소 API

### Month 2: Phase 10
**Week 5-6**: 실거래 통합
- KoreaInvestmentTradingProvider
- UpbitTradingProvider
- OAuth 계좌 연결

**Week 7-8**: 리스크 관리
- Stop Loss 자동 설정
- Position Sizing
- 거래 한도 관리

### Month 3: Phase 11
**Week 9-10**: 고급 백테스팅
- Monte Carlo 시뮬레이션
- 멀티 전략 비교

**Week 11-12**: 세금 최적화 OR 알림 시스템
- 택 1 집중 개발
- 다른 하나는 Phase 12로 연기

---

## 🎯 최종 추천

### 최우선 개발 순서
1. **거래 기능 (Trading)** - 페이퍼 트레이딩부터
2. **고급 백테스팅** - Monte Carlo, 전략 비교
3. **세금 최적화** - Tax-Loss Harvesting

**이유**:
- 거래 기능은 포트폴리오 완성도를 크게 높임
- 백테스팅은 퀀트 개발자 포지션 타겟 시 필수
- 세금 최적화는 핀테크 도메인 전문성 증명

### 포트폴리오 임팩트 순위
1. 🥇 **거래 기능** - "실제로 사용 가능한 투자 플랫폼"
2. 🥈 **세금 최적화** - "실무적 가치가 높은 금융 솔루션"
3. 🥉 **고급 백테스팅** - "퀀트 전략 검증 시스템"

---

## 📌 현재 기술 부채

### Backend
- [ ] Circuit Breaker 패턴 미구현 (Provider 장애 시)
- [ ] Redis 캐싱 미구현 (개발 환경)
- [ ] Rate Limiting 미구현
- [ ] KIS API 키 설정 필요

### Frontend
- [ ] 추천 포트폴리오 Mock 데이터 → Real API 전환
- [ ] 실시간 스트리밍 UI 미구현 (SSE, WebSocket)
- [ ] E2E 테스트 인증 상태 전파 이슈

### DevOps
- [ ] 프로덕션 환경 구축
- [ ] 모니터링 시스템 (CloudWatch, Sentry)
- [ ] CI/CD 파이프라인

---

**문서 작성**: Claude Code SuperClaude
**다음 업데이트**: Phase 8 완료 시
