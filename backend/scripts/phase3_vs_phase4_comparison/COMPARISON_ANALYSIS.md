# Phase 3 vs Phase 4 비교 분석

> **목적**: GET 요청 시 Update를 같이 하던 방식(Phase 3) vs Scheduler로 분리한 방식(Phase 4)의 성능 차이 분석  
> **분석 날짜**: 2025-11-26  
> **분석자**: Sentinel Squad

---

## 🔍 핵심 차이점

### Phase 3: Inline Update (GET 시 실시간 업데이트)

**아키텍처**:
```java
// PortfolioService.java (Phase 3)
public PortfolioDto getPortfolioById(Long portfolioId, Long userId) {
    Portfolio portfolio = portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));
    
    // ❌ GET 요청마다 외부 API 호출 (Blocking I/O)
    updatePortfolioPrices(portfolio);  // Deprecated
    
    return convertToDto(portfolio);
}

private void updatePortfolioPrices(Portfolio portfolio) {
    for (PortfolioHolding holding : portfolio.getHoldings()) {
        // ❌ 각 종목마다 외부 API 호출 (MarketDataService, CryptoDataService)
        // ❌ Resilience4j Timeout: 1초
        // ❌ N개의 종목 → N번의 외부 API 호출 (직렬)
    }
    portfolio.recalculate();
    portfolioRepository.save(portfolio);
}
```

**특징**:
- ✅ **장점**: 항상 최신 가격 조회
- ❌ **단점**:
  - GET 요청 응답시간 = DB 조회 + (N × 외부 API 호출)
  - 외부 API 장애 시 READ API도 느려짐
  - 500 VU 부하 시 초당 1,500~2,000회 외부 API 호출
  - Circuit Breaker 발동 가능성

---

### Phase 4: Background Scheduler (Read/Write 분리)

**아키텍처**:
```java
// PortfolioService.java (Phase 4)
@Transactional(readOnly = true)
public PortfolioDto getPortfolioById(Long portfolioId, Long userId) {
    Portfolio portfolio = portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));
    
    // ✅ 순수 DB 조회만 수행 (외부 API 호출 없음)
    return convertToDto(portfolio);
}
```

```java
// PortfolioPriceScheduler.java (Phase 4)
@Scheduled(fixedRate = 180000, initialDelay = 600000)  // 3분마다
@Transactional
public void updateAllPortfolioPrices() {
    List<Portfolio> portfolios = portfolioRepository.findAll();
    
    for (Portfolio portfolio : portfolios) {
        for (PortfolioHolding holding : portfolio.getHoldings()) {
            updateHoldingPrice(holding);  // 외부 API 호출
        }
        portfolio.recalculate();
        portfolioRepository.save(portfolio);
    }
}
```

**특징**:
- ✅ **장점**:
  - GET 요청 응답시간 = DB 조회만 (수십 ms)
  - 외부 API 상태와 독립적
  - READ 처리량 급증
- ⚠️  **단점**:
  - 가격 데이터가 최대 3분까지 stale
  - Scheduler 실행 중 DB Lock 경합 가능성

---

## 📊 현재까지 확인된 데이터

### Phase 3 Experiment 5 (EntityGraph Optimized)

**실험 조건**:
- **k6 스크립트**: `exp5_baseline.js`
- **부하**: 500 VU, 8분
- **코드 상태**: EntityGraph 적용 (N+1 해결), **BUT** GET 시 Update 호출
- **결과**: **데이터 없음** ❌

> **문제**: Phase 3 README에 Baseline 테스트 완료했다고 하지만, 실제 결과 파일이 없음  
> **Hypothesis**: `experiment5_portfolio` 폴더에 데이터가 있을 수 있으나, 구조가 다름

---

### Phase 4 Scheduler 30s vs 180s

**실험 A: 30초 주기**
| 메트릭 | 값 |
|--------|-----|
| **평균 응답시간** | 390.55ms |
| **P95** | 613.39ms |
| **TPS** | 74.45 req/s |
| **CPU 사용률** | 16.09% |
| **표준편차** | 94.22ms |

**실험 B: 180초(3분) 주기**
| 메트릭 | 값 |
|--------|-----|
| **평균 응답시간** | 450.01ms |
| **P95** | 680.72ms |
| **TPS** | 68.61 req/s |
| **CPU 사용률** | 16.19% |
| **표준편차** | 60.04ms ✅ **36% 개선** |

**핵심 발견** (from `ANALYSIS_REPORT.md`):
> ❌ **두 실험 모두 목표 미달성** (목표: 평균 < 100ms, P95 < 150ms)  
> 🤔 **평균 390~450ms는 Phase 2 이전 수준**  
> 💡 **근본 원인은 N+1 쿼리일 가능성 높음**

**Scouter XLog 확인** (from `EXPERIMENT_SUMMARY.md`):
- ✅ **SQL Count: 5번** (N+1 아님)
- ✅ **SQL Time: 50~100ms** (병목 아님)
- ❌ **응답시간: 390~450ms** → **SQL 제외 290~400ms가 미스터리**
- 🔥 **Active Threads: 233~254개** (Tomcat max-threads 한계 근접!)

---

## 🎯 비교를 위한 실험 설계

### 목표

**Phase 3 EntityGraph (with Inline Update)** vs **Phase 4 Scheduler 180s**를 공정하게 비교

---

### Baseline Measurement Plan

#### Experiment A: Phase 3 Reproduction (Inline Update)

**코드 변경**:
```java
// PortfolioService.java
@Transactional
public PortfolioDto getPortfolioById(Long portfolioId, Long userId) {
    Portfolio portfolio = portfolioRepository.findByIdAndUserIdWithHoldings(portfolioId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("포트폴리오", portfolioId));
    
    // ⚠️ EXPERIMENT ONLY: Phase 3 방식 재현
    updatePortfolioPrices(portfolio);
    
    return convertToDto(portfolio);
}
```

**Scheduler 설정**:
```java
// PortfolioPriceScheduler.java
// @Scheduled 주석 처리 또는 삭제
```

**테스트 스크립트**:
- `phase3_db_optimization/tests/exp5_baseline.js`
- LAN 연결 (jitter 낮춤)
- 500 VU, 8분

**예상 결과**:
- ❌ **평균 응답시간**: 500~1,000ms (외부 API 호출 포함)
- ❌ **P95**: 1,000~2,000ms
- ❌ **TPS**: 30~50 req/s
- ⚠️  **에러율**: 외부 API Timeout으로 5~10%

---

#### Experiment B: Phase 4 Scheduler 180s (Current)

**코드 변경**: 없음 (현재 상태 유지)

**Scheduler 설정**:
```java
@Scheduled(fixedRate = 180000, initialDelay = 600000)
```

**테스트 스크립트**:
- `phase4_scheduler_impact/tests/exp6_scheduler_impact.js`
- LAN 연결 (동일 조건)
- 500 VU, 8분

**예상 결과** (이미 측정됨):
- ✅ **평균 응답시간**: 450ms
- ✅ **P95**: 680ms
- ✅ **TPS**: 68 req/s
- ⚠️  여전히 목표(< 100ms) 미달

---

## 🔬 측정할 메트릭

### Application 메트릭
- ✅ `avg_response_time.csv`
- ✅ `p95_response_time.csv`
- ✅ `tps.csv`
- ✅ `status_code_distribution.csv`

### JVM 메트릭
- ✅ `active_threads.csv` (🔥 중요!)
- ✅ `cpu_usage.csv`
- ✅ `jvm_memory.csv`

### Database 메트릭
- ✅ `hikaricp_active.csv`
- ✅ `hikaricp_pending.csv`
- ✅ `hikaricp_usage_percent.csv`

### 외부 API 메트릭 (Phase 3만 해당)
- ⭐ **Circuit Breaker 상태** (Resilience4j)
- ⭐ **외부 API 호출 횟수**
- ⭐ **외부 API Timeout 횟수**

---

## 💡 가설 및 예상 결론

### 가설 1: Inline Update는 외부 API 병목으로 느릴 것

**예상**:
- Phase 3 평균 응답시간: **500~1,000ms** (2~10배 느림)
- Phase 4 평균 응답시간: **450ms** (현재)

**검증 방법**:
- Scouter XLog에서 `MarketDataService.getStockPrice()` 호출 시간 측정
- Prometheus에서 `resilience4j_circuitbreaker_calls_total` 메트릭 확인

---

### 가설 2: Scheduler 분리는 안정성과 처리량을 개선할 것

**예상**:
- Phase 3 TPS: **30~50 req/s**
- Phase 4 TPS: **68 req/s** (2배 개선)

**예상**:
- Phase 3 표준편차: **150~200ms** (변동폭 큼)
- Phase 4 표준편차: **60ms** (안정적)

---

### 가설 3: 두 방식 모두 스레드 고갈 문제를 가지고 있음

**근거**:
- Phase 4에서도 **Active Threads: 233~254개** (max-threads에 근접)
- 응답시간의 90%가 **"미스터리 지연"** (SQL 50ms vs 응답 450ms)

**가능한 원인**:
1. **DTO 변환 오버헤드** (Stream, BigDecimal 연산)
2. **Transaction 범위** (readOnly이지만 영향?)
3. **Tomcat Thread Pool 부족** (500 VU → 250개 필요)

**다음 단계** (Phase 5):
- Java 21 Virtual Threads 활성화
- HikariCP max-connections 증가 (20 → 50)

---

## 📋 실험 실행 계획

### Step 1: Phase 3 Reproduction 준비

```bash
# 1. PortfolioService.java 수정
# - getPortfolioById()에서 updatePortfolioPrices() 호출 추가

# 2. PortfolioPriceScheduler.java 비활성화
# - @Scheduled 주석 처리

# 3. Backend 재시작
cd backend
./gradlew bootRun --args='--spring.profiles.active=perf'
```

---

### Step 2: Phase 3 테스트 실행

```bash
# Mac에서 k6 실행
cd backend/scripts/phase3_db_optimization/tests

k6 run \
  --env BASE_URL=http://192.168.0.58:8080 \
  --env TOKENS_FILE=./jmeter_tokens.csv \
  --out json=results/phase3_inline_update.json \
  exp5_baseline.js
```

---

### Step 3: 메트릭 추출 (Phase 3)

```bash
# Windows에서 Prometheus 메트릭 추출
cd backend/scripts
python common/python/export_metrics.py \
  --start "2025-11-26T03:00:00Z" \
  --end "2025-11-26T03:10:00Z" \
  --step "15s" \
  --output phase3_vs_phase4_comparison/results/phase3_inline_update
```

---

### Step 4: Phase 4 테스트 재실행 (동일 조건)

```bash
# 1. PortfolioService.java 원복
# - getPortfolioById()에서 updatePortfolioPrices() 제거

# 2. PortfolioPriceScheduler.java 활성화
# - @Scheduled 주석 해제

# 3. Backend 재시작
./gradlew bootRun --args='--spring.profiles.active=perf'

# 4. k6 실행
cd backend/scripts/phase4_scheduler_impact/tests

k6 run \
  --env BASE_URL=http://192.168.0.58:8080 \
  --env TOKENS_FILE=./jmeter_tokens.csv \
  --out json=results/phase4_scheduler_180s.json \
  exp6_scheduler_impact.js
```

---

### Step 5: 비교 분석

```python
# Python 분석 스크립트 작성
# compare_phase3_vs_phase4.py
import pandas as pd
import matplotlib.pyplot as plt

# Phase 3 결과 로드
phase3_avg = pd.read_csv('phase3_inline_update/avg_response_time.csv')

# Phase 4 결과 로드
phase4_avg = pd.read_csv('phase4_scheduler_180s/avg_response_time.csv')

# 비교 차트 생성
# 1. 평균 응답시간 시계열
# 2. P95 응답시간 비교
# 3. TPS 비교
# 4. Active Threads 비교
```

---

## 🎯 Success Criteria

Scheduler 분리가 **효과적**이라고 판단하는 기준:

1. **평균 응답시간**: Phase 4가 Phase 3 대비 **2배 이상 빠름**
2. **TPS**: Phase 4가 Phase 3 대비 **1.5배 이상 높음**
3. **안정성**: Phase 4 표준편차가 Phase 3 대비 **30% 이상 낮음**
4. **에러율**: Phase 4가 Phase 3 대비 **50% 이상 낮음**

---

## 📝 예상 결론

### 시나리오 A: Scheduler 분리가 명확히 우수함 ✅

**결과**:
- Phase 3: 평균 1,000ms, TPS 40 req/s, 에러율 10%
- Phase 4: 평균 450ms, TPS 68 req/s, 에러율 0%

**결론**:
> **Read/Write 분리 전략은 성공적!**  
> Inline Update는 외부 API 의존성으로 인해 응답시간과 안정성 모두 악화시킴.  
> Phase 4 Scheduler 방식을 유지하고, Phase 5에서 Virtual Threads로 추가 최적화 진행.

---

### 시나리오 B: 두 방식 모두 비슷함 ⚠️

**결과**:
- Phase 3: 평균 500ms, TPS 60 req/s
- Phase 4: 평균 450ms, TPS 68 req/s

**결론**:
> **Scheduler 분리 효과는 있으나 제한적.**  
> 근본 원인은 **스레드 고갈 또는 DTO 변환 오버헤드**.  
> Phase 5에서 Virtual Threads + Connection Pool 증가로 병목 해결 필요.

---

## 🚀 Next Steps

1. ✅ 이 문서 검토 및 승인
2. ⏳ Phase 3 코드 재현 (Inline Update)
3. ⏳ Phase 3 테스트 실행
4. ⏳ Phase 4 테스트 재실행 (공평한 비교)
5. ⏳ 결과 비교 분석
6. ⏳ 최종 리포트 작성

---

**Last Updated**: 2025-11-26  
**Author**: Sentinel Squad  
**Status**: 실험 설계 완료, 승인 대기
