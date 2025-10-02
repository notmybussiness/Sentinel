# 📋 Sentinel 프로젝트 계획 및 진행 상황

> **목적**: 세션 간 연속성을 위한 프로젝트 목표, 진행 상황, 다음 단계 추적
>
> **최종 업데이트**: 2025-10-02
> **현재 단계**: Phase 2 - 시장 데이터 통합 (40% 완료)

---

## 🎯 프로젝트 비전

**목표**: 백테스팅, 리밸런싱 추천, 실시간 시장 데이터를 제공하는 종합 포트폴리오 관리 플랫폼 구축

**타겟 사용자**: 데이터 기반 투자 전략을 원하는 개인 투자자

**핵심 가치**: 자동화와 지능형 추천을 통한 포트폴리오 관리 간소화

---

## 📊 현재 상태 요약

### 완료 현황
- **프론트엔드**: ✅ 100% 완료
- **백엔드**: 🟡 70% 완료 (인증 + 포트폴리오 + 시장 데이터 부분 완료)
- **통합**: 🟡 30% (시장 지수 API 연동 완료)
- **테스팅**: 🔴 5%
- **배포**: 🔴 0%

### 현재 작동하는 기능
1. ✅ 20개 이상의 컴포넌트로 구성된 완전한 프론트엔드 UI
2. ✅ Kakao OAuth 인증 플로우
3. ✅ 포트폴리오 CRUD 작업 (백엔드)
4. ✅ JWT 토큰 관리
5. ✅ 프론트엔드 테스트를 위한 개발 모드
6. ✅ Playwright E2E 테스트 (로그인 기능 4/4 통과)
7. ✅ **시장 지수 API 연동** (S&P 500, NASDAQ, DOW, KOSPI)
8. ✅ **홈페이지 실시간 시장 데이터 표시**

### 미완성 기능
1. 🟡 시장 데이터 API (부분 완료 - 지수만 완료, 종목 검색 미완)
2. ❌ 백테스팅 엔진
3. ❌ 리밸런싱 알고리즘
4. 🟡 프론트엔드-백엔드 연결 (인증 + 시장지수 완료, 포트폴리오 실시간 가격 미완)
5. 🟡 실시간 데이터 업데이트 (시장 지수만 1분 자동 갱신)

---

## 🗓️ 개발 로드맵

### Phase 1: 기초 구축 (✅ 완료)
**일정**: 1-2주차
**상태**: 100% 완료 (2025-09-30)

- [x] 백엔드 아키텍처 설정
- [x] 프론트엔드 Next.js 14 설정
- [x] 인증 시스템 (Kakao OAuth + JWT)
- [x] UI 컴포넌트 라이브러리 (20개 이상)
- [x] 디자인 시스템 (Glassmorphism 다크 테마)
- [x] 포트폴리오 CRUD 백엔드
- [x] 프론트엔드 개발용 Mock 데이터
- [x] Playwright E2E 테스트 설정 및 로그인 테스트

**완료일**: 2025-10-01

---

### Phase 2: 시장 데이터 통합 (🚧 진행 중)
**일정**: 3주차
**상태**: 40% 완료
**우선순위**: 높음
**완료일**: 2025-10-02 (부분 완료)

#### 백엔드 작업
- [x] `MarketDataService` 구현 완료
  - [x] AlphaVantage provider
  - [x] Finnhub provider
  - [x] getStockPrice() - 단일 종목 조회
  - [x] getStockPrices() - 여러 종목 조회
  - [x] **getMarketIndices()** - 주요 지수 조회 ⭐ 신규
  - [ ] Circuit breaker 구현
  - [ ] Redis 캐싱 레이어
- [x] 시장 데이터 엔드포인트 구현 (부분)
  - [x] `GET /api/v1/market/price/{symbol}` - 개별 종목 가격
  - [x] `GET /api/v1/market/prices` - 여러 종목 가격 (쿼리 파라미터)
  - [x] **`GET /api/v1/market/indices`** - 주요 지수 ⭐ 신규
  - [ ] `POST /api/v1/market/prices` - 배치 가격 조회 (Body)
  - [ ] `GET /api/v1/market/search` - 종목 검색
- [ ] Rate limiting 추가
- [x] 에러 처리 및 로깅

#### 프론트엔드 작업
- [x] **시장 데이터 API 클라이언트 생성** (`lib/api/market.ts`) ⭐ 신규
  - [x] getMarketIndices() - 지수 조회
  - [x] getStockPrice() - 단일 종목
  - [x] getStockPrices() - 여러 종목 (개별 호출)
- [x] Mock 데이터를 API 호출로 교체 (부분)
  - [x] **홈페이지 시장 지수** ⭐ 완료
  - [ ] 포트폴리오 Holdings 실시간 가격
  - [ ] Market 페이지 데이터
- [x] **로딩 상태 추가** ⭐ 완료
- [x] **에러 처리 UI 추가** ⭐ 완료
- [x] **React Query 자동 갱신** (1분마다) ⭐ 완료

#### 테스팅
- [ ] Provider 유닛 테스트
- [ ] 엔드포인트 통합 테스트
- [x] 실제 데이터로 프론트엔드 동작 확인 ⭐ 완료

---

---

## 🎉 2025년 10월 2일 완료 작업

### ✅ 시장 지수 API 구현 및 연동 (Phase 2 - 40% 완료)

**Backend 구현**:
1. **MarketIndexDto.java** - 시장 지수 DTO 생성
2. **MarketDataService.getMarketIndices()** - 지수 조회 로직
   - 4개 지수 심볼 정의 (^GSPC, ^IXIC, ^DJI, ^KS11)
   - 기존 getStockPrice() 재사용
   - StockPriceDto → MarketIndexDto 변환
3. **MarketDataController.getMarketIndices()** - GET /api/v1/market/indices
   - 에러 처리 및 로깅 추가

**Frontend 구현**:
1. **lib/api/market.ts** - Market API Client 생성
   - getMarketIndices() - 지수 조회
   - getStockPrice() - 단일 종목
   - getStockPrices() - 여러 종목
2. **홈페이지 Mock → API 교체** (app/page.tsx)
   - React Query useQuery 적용
   - 로딩 상태 UI
   - 에러 처리 UI
   - 1분마다 자동 갱신 (refetchInterval: 60000)

**테스트**:
- ✅ curl로 API 응답 확인 (4개 지수 정상)
- ✅ 브라우저 동작 확인 (http://localhost:3000)

**다음 단계**:
1. POST /api/v1/market/prices (배치 조회) 구현
2. 포트폴리오 Holdings 실시간 가격 연동
3. Market 페이지 Mock → API 교체

---

## 📅 2025년 10월 1일 작업 계획

### 🔴 Priority 1: MarketDataController 구현 (2-3일)

#### 1.1 Controller 클래스 생성
**파일**: `backend/src/main/java/com/pjsent/sentinel/market/controller/MarketDataController.java`

**구현 엔드포인트**:
```java
GET  /api/v1/market/price/{symbol}      // 개별 종목 가격
POST /api/v1/market/prices              // 배치 가격 조회
GET  /api/v1/market/indices             // 주요 지수 (S&P, NASDAQ, DOW, KOSPI)
GET  /api/v1/market/search              // 종목 검색
```

**참고 문서**:
- `.claude/backend/API_MARKET.md` - API 스펙 상세
- `PortfolioController.java` - 구현 참고 예시

#### 1.2 Circuit Breaker 설정
**파일**: `backend/src/main/resources/application.yml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      alphavantage:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
      finnhub:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
```

#### 1.3 Redis 캐싱 레이어
**MarketDataService에 캐싱 적용**:
```java
@Cacheable(value = "stock-prices", key = "#symbol", unless = "#result == null")
public StockPrice getPrice(String symbol) {
    // 기존 로직
}

@CacheEvict(value = "stock-prices", allEntries = true)
@Scheduled(fixedRate = 900000) // 15분마다 캐시 삭제
public void clearCache() {}
```

#### 1.4 테스트
- [ ] Swagger UI 수동 테스트 (http://localhost:8080/swagger-ui.html)
- [ ] Postman으로 각 엔드포인트 확인
- [ ] Unit 테스트 작성

---

### 🟡 Priority 2: 프론트엔드 Market API Client (1-2일)

#### 2.1 API Client 파일 생성
**파일**: `frontend/lib/api/market.ts`

```typescript
export interface MarketIndex {
  name: string;
  symbol: string;
  value: number;
  change: number;
  changePercent: number;
}

export interface StockPrice {
  symbol: string;
  price: number;
  change: number;
  changePercent: number;
  timestamp: string;
}

export interface SearchResult {
  symbol: string;
  name: string;
  exchange: string;
  type: string;
}

// API 함수들
export async function getMarketIndices(): Promise<MarketIndex[]>
export async function getStockPrice(symbol: string): Promise<StockPrice>
export async function getBatchPrices(symbols: string[]): Promise<StockPrice[]>
export async function searchSymbol(query: string): Promise<SearchResult[]>
```

#### 2.2 Mock 데이터 교체
**교체 대상 파일**:
1. `frontend/app/page.tsx` - 홈페이지 시장 지수 섹션
2. `frontend/app/portfolios/[id]/page.tsx` - 포트폴리오 상세 Holdings
3. `frontend/app/market/page.tsx` - 시장 데이터 페이지

**교체 패턴**:
```typescript
// 기존 (Mock)
const indices = mockMarketIndices;

// 변경 (API)
const { data: indices, isLoading, error } = useQuery({
  queryKey: ['market-indices'],
  queryFn: getMarketIndices,
  refetchInterval: 60000, // 1분마다 갱신
});
```

#### 2.3 로딩 상태 및 에러 UI
- [ ] Skeleton loader 컴포넌트 추가
- [ ] Error boundary 설정
- [ ] Retry 버튼 UI

---

### 🟢 Priority 3: Portfolio API 완전 연동 (1-2일)

#### 3.1 Portfolio API Client 실제 연동
**파일**: `frontend/lib/api/portfolio.ts`

**현재 상태**: Mock 데이터 사용 중
**목표**: 백엔드 API 완전 연동

```typescript
// 이미 백엔드에 구현된 엔드포인트:
GET    /api/v1/portfolios                    // 목록 조회
GET    /api/v1/portfolios/{id}               // 상세 조회
POST   /api/v1/portfolios                    // 생성
PUT    /api/v1/portfolios/{id}               // 수정
DELETE /api/v1/portfolios/{id}               // 삭제
POST   /api/v1/portfolios/{id}/holdings      // Holding 추가
PUT    /api/v1/portfolios/{id}/holdings/{id} // Holding 수정
DELETE /api/v1/portfolios/{id}/holdings/{id} // Holding 삭제
```

#### 3.2 Holdings 관리 UI 개선
- [ ] Holdings 추가 Modal UI
- [ ] 종목 검색 기능 (Market API 연동)
- [ ] 수량/가격 입력 폼
- [ ] 실시간 가격 업데이트 (Market API 연동 후)

---

## 📈 작업 순서 (이번 주 권장)

**Day 1-2**:
- MarketDataController.java 구현
- Circuit Breaker 설정
- Swagger/Postman 테스트

**Day 2-3**:
- Redis 캐싱 레이어 추가
- Rate limiting 구현
- Unit 테스트 작성

**Day 3-4**:
- 프론트엔드 `lib/api/market.ts` 생성
- API 함수 구현

**Day 4-5**:
- 홈페이지 Mock 데이터 → 실제 API 교체
- 로딩/에러 상태 UI 추가

**Day 5-6**:
- Portfolio 페이지 실시간 가격 연동
- Holdings 관리 UI 개선

**Day 6-7**:
- 통합 테스트
- 버그 수정
- PLAN.md 및 CURRENT_STATE.md 업데이트

---

### Phase 3: 포트폴리오 통합 (🔴 미시작)
**일정**: 4주차
**상태**: 0% 완료
**우선순위**: 높음

#### 작업 내용
- [ ] 프론트엔드 포트폴리오 페이지와 백엔드 연결
  - [ ] 포트폴리오 목록 API 통합
  - [ ] 포트폴리오 상세 API 통합
  - [ ] 포트폴리오 생성 기능
  - [ ] 포트폴리오 수정 기능
  - [ ] 포트폴리오 삭제 확인
- [ ] Holdings 관리 통합
  - [ ] 종목 검색으로 Holding 추가
  - [ ] Holding 수량 업데이트
  - [ ] Holding 삭제
  - [ ] 실시간 가치 업데이트
- [ ] 포트폴리오 자동 새로고침
  - [ ] 폴링 또는 WebSocket 구현
  - [ ] 페이지 새로고침 없이 UI 업데이트
- [ ] 에러 처리 및 검증

**완료 기준**:
- 사용자가 실제 포트폴리오 생성 및 관리 가능
- Holdings에 실시간 시장 가격 반영
- 모든 CRUD 작업 End-to-End 작동

---

### Phase 4: 백테스팅 엔진 (🔴 미시작)
**일정**: 5-6주차
**상태**: 0% 완료
**우선순위**: 중간

#### 백엔드 작업
- [ ] 백테스팅 데이터 모델 설계
  - [ ] `Backtest` 엔티티
  - [ ] `BacktestResult` 엔티티
  - [ ] 전략 정의
- [ ] 과거 데이터 fetching 구현
  - [ ] AlphaVantage 과거 데이터 API
  - [ ] 데이터 정규화 및 저장
- [ ] 백테스팅 엔진 생성
  - [ ] 포트폴리오 리밸런싱 시뮬레이션
  - [ ] 성과 계산
  - [ ] 리스크 지표 (Sharpe, Sortino, Max Drawdown)
- [ ] 백테스팅 엔드포인트 구현
  - [ ] `POST /api/v1/backtest/run`
  - [ ] `GET /api/v1/backtest/results/{id}`
  - [ ] `GET /api/v1/backtest/history`

#### 프론트엔드 작업
- [ ] Lab 페이지 구현 완성
  - [ ] 설정 폼 검증
  - [ ] 전략 선택 UI
  - [ ] 결과 시각화
- [ ] 결과 컴포넌트 생성
  - [ ] 성과 차트 (Recharts/Chart.js)
  - [ ] 지표 테이블
  - [ ] 벤치마크 비교
- [ ] 결과 내보내기 기능

**완료 기준**:
- 사용자가 포트폴리오에 대해 백테스트 실행 가능
- 결과에 과거 성과 표시
- 여러 전략 비교 가능

---

### Phase 5: 리밸런싱 알고리즘 (🔴 미시작)
**일정**: 7주차
**상태**: 0% 완료
**우선순위**: 중간

#### 작업 내용
- [ ] 리밸런싱 전략 구현
  - [ ] 임계값 기반 (5% 편차)
  - [ ] 시간 기반 (월간/분기별)
  - [ ] 하이브리드 접근
- [ ] 추천 엔진 생성
  - [ ] 목표 대비 현재 배분 계산
  - [ ] BUY/SELL/HOLD 추천 생성
  - [ ] 세금 효율성 최적화
- [ ] 프론트엔드 통합
  - [ ] RebalancingModal을 API에 연결
  - [ ] 실시간 추천 표시
  - [ ] 원클릭 리밸런싱 실행 (향후)

**완료 기준**:
- 리밸런싱 추천이 정확함
- UI에 필요한 작업 명확히 표시
- 여러 전략 사용 가능

---

### Phase 6: 테스팅 및 품질 (🔴 미시작)
**일정**: 8주차
**상태**: 0% 완료
**우선순위**: 중간

#### 작업 내용
- [ ] 백엔드 테스팅
  - [ ] 유닛 테스트 (>80% 커버리지)
  - [ ] 모든 엔드포인트 통합 테스트
  - [ ] 부하 테스트 (JMeter)
- [ ] 프론트엔드 테스팅
  - [ ] 컴포넌트 유닛 테스트 (Jest)
  - [ ] E2E 테스트 (Playwright)
  - [ ] 크로스 브라우저 테스팅
- [ ] 성능 최적화
  - [ ] API 응답 시간 (<200ms)
  - [ ] 프론트엔드 로드 시간 (<3초)
  - [ ] 번들 크기 최적화
- [ ] 보안 감사
  - [ ] JWT 보안 검토
  - [ ] SQL 인젝션 방지
  - [ ] XSS 보호
  - [ ] CORS 설정

---

### Phase 7: 배포 (🔴 미시작)
**일정**: 9주차
**상태**: 0% 완료
**우선순위**: 낮음

#### 작업 내용
- [ ] 프로덕션 환경 설정
  - [ ] AWS 계정 구성
  - [ ] RDS PostgreSQL 설정
  - [ ] ElastiCache Redis 설정
  - [ ] EC2 또는 ECS 구성
- [ ] CI/CD 구성
  - [ ] GitHub Actions 워크플로우
  - [ ] 자동화된 테스팅
  - [ ] 자동화된 배포
- [ ] 도메인 및 SSL
  - [ ] 도메인 이름 구매
  - [ ] DNS 구성
  - [ ] SSL 인증서 (Let's Encrypt)
- [ ] 모니터링 및 로깅
  - [ ] CloudWatch 설정
  - [ ] 에러 추적 (Sentry)
  - [ ] 성능 모니터링

**목표 날짜**: 9주차 종료

---

## 🎯 즉시 해야 할 일 (이번 주)

### Priority 1: 시장 데이터 백엔드
1. `MarketDataController.java` 구현
2. Provider circuit breaker 완성
3. Redis 캐싱 추가
4. Postman/Swagger로 테스트

### Priority 2: 시장 데이터 프론트엔드
1. `lib/api/market.ts` 생성
2. 홈페이지 Mock 지수 교체
3. 로딩 상태 추가
4. 실제 API 통합 테스트

### Priority 3: 포트폴리오 API 통합
1. 실제 엔드포인트로 `lib/api/portfolio.ts` 업데이트
2. 포트폴리오 목록 페이지 연결
3. 생성/수정/삭제 작업 테스트

---

## 📝 세션 체크리스트

**각 세션 시작 시**:
1. PLAN.md를 읽고 현재 단계 파악
2. 최신 구현 상황을 위해 CURRENT_STATE.md 읽기
3. 최근 변경사항 확인을 위해 progress/ 폴더 확인
4. 지난 세션의 미완료 작업 검토

**세션 중**:
1. 이 파일에서 완료된 작업 상태 업데이트
2. 중요한 변경사항이 있으면 progress 파일 생성
3. 새로운 기능으로 CURRENT_STATE.md 업데이트

**각 세션 종료 시**:
1. 완료된 작업에 [x] 표시
2. "Last Updated" 날짜 업데이트
3. 필요시 progress 파일 작성
4. 변경사항 커밋 및 푸시

---

## 🐛 알려진 이슈 및 기술 부채

### 높은 우선순위
- [ ] Kakao OAuth 리다이렉트 URI는 Kakao Console에서 설정 필요
- [x] 프론트엔드에 에러 boundary 없음 (Playwright 테스트 완료)
- [ ] 데이터 fetch 시 로딩 상태 누락

### 중간 우선순위
- [ ] Mock 데이터의 ID가 일관되지 않음
- [ ] 포트폴리오 재계산이 자동이 아님
- [ ] 오프라인 모드 없음
- [ ] Redis 연결 실패 (개발 환경 - H2로 대체 중)

### 낮은 우선순위
- [ ] 컴포넌트 문서가 더 상세할 수 있음
- [ ] 다크/라이트 모드 토글 없음 (항상 다크)

---

## 💡 향후 개선사항 (MVP 이후)

### 기능 아이디어
- [ ] 여러 포트폴리오 비교
- [ ] AI 기반 포트폴리오 제안
- [ ] 세금 손실 수확 추천
- [ ] 배당 추적 및 예측
- [ ] 뉴스 피드 통합
- [ ] 모바일 앱 (React Native)
- [ ] 소셜 기능 (포트폴리오 공유)
- [ ] 암호화폐 지원

### 기술 개선사항
- [ ] 실시간 업데이트를 위한 WebSocket
- [ ] GraphQL API 옵션
- [ ] 서버 사이드 렌더링 최적화
- [ ] Progressive Web App (PWA)
- [ ] 다국어 지원 (i18n)

---

## 📞 빠른 의사결정 로그

**최근 결정사항**:
1. **2025-10-01**: `.claude/` 폴더를 backend/ 및 frontend/ 하위 폴더로 재구성
2. **2025-10-01**: Playwright E2E 테스트 설정 및 로그인 기능 테스트 완료 (4/4 통과)
3. **2025-09-30**: 모든 컴포넌트에 glassmorphism 디자인 시스템 적용
4. **2025-09-30**: 캐러셀을 3개 항목 표시, 한 번에 1개씩 슬라이드하도록 선택
5. **2025-09-29**: 프론트엔드 개발 속도를 위해 Mock 데이터 사용 결정

**보류 중인 결정사항**:
- 어떤 차트 라이브러리? (Chart.js vs Recharts vs D3.js)
- 실시간 업데이트를 위한 WebSocket vs 폴링?
- AWS EC2 vs Vercel + Heroku에 배포?

---

## 🔄 버전 이력

### v1.0 - 프론트엔드 기초 (2025-10-01)
- 20개 이상의 컴포넌트로 완전한 프론트엔드 UI
- Kakao OAuth 인증
- 모든 페이지용 Mock 데이터
- Glassmorphism 디자인 시스템
- Playwright E2E 테스트 프레임워크

### v0.5 - 백엔드 기초 (2025-09-28)
- Spring Boot 3.5.5 설정
- PostgreSQL + Redis 통합
- User 및 Portfolio CRUD
- JWT 인증

---

## ⚠️ 주의사항

1. **Redis 연결**: 개발 환경에서는 H2 DB만으로 작동하지만, 프로덕션 배포 전 Redis 설정 필요
2. **API Rate Limiting**: AlphaVantage 무료 플랜은 분당 5회, 일일 100회 제한
3. **에러 처리**: 외부 API 실패 시 Fallback 전략 필요 (Finnhub → Mock 데이터)
4. **테스트 환경**: `.env.local`에 `NEXT_PUBLIC_DEV_MODE=true` 설정 유지
5. **Backend 실행**: Redis가 없어도 H2 데이터베이스로 개발 가능
6. **Playwright 테스트**: `cd frontend && npx playwright test`로 로그인 테스트 실행

---

**기억할 것**: 이 문서는 프로젝트 계획의 단일 진실 공급원입니다. 정기적으로 업데이트하고 연속성을 위해 각 세션 시작 시 참조하세요.

**다음 세션 목표**: 시장 데이터 백엔드 구현을 완료하고 프론트엔드 홈페이지에 연결하기.
