# 📈 Fintech Backend 성능 최적화 프로젝트

> **Spring Boot 기반 포트폴리오 관리 시스템 성능 개선 사례**  
> Java 21, Spring Boot 3.5.5, PostgreSQL, Redis

---

## 🎯 프로젝트 개요

### 문제 상황

실시간 포트폴리오 조회 API가 **평균 500ms**의 응답시간을 보이며, 높은 부하 상황에서 **24% 에러율** 발생.

- **부하 조건**: 500 VU (Virtual Users), 8분간 지속
- **기존 성능**: P95 응답시간 **3,954ms**, TPS **40 req/s**
- **에러율**: **24.45%** (타임아웃 및 DB 커넥션 고갈)

### 목표

- ✅ P95 응답시간 **100ms 이하** (목표 97% 개선)
- ✅ TPS **200 req/s 이상** (목표 5배 증가)
- ✅ 에러율 **1% 이하** (목표 95% 감소)

---

## 🔧 Phase 3: DB Query 최적화 (N+1 문제 해결)

### 문제 분석

JPA Lazy Loading으로 인한 **N+1 쿼리 문제** 발견:

```sql
-- 기존: Portfolio 1개 조회 시 11번의 쿼리 발생
SELECT * FROM portfolios WHERE id = 1;              -- 1번
SELECT * FROM portfolio_holdings WHERE portfolio_id = 1; -- +1
SELECT * FROM portfolio_holdings WHERE portfolio_id = 2; -- +1
... (총 10개 Holdings)
```

**문제점**:
- 요청 1개당 **평균 11회 SQL 실행**
- HikariCP Pool (20 Connections) **완전 포화**
- DB Connection 대기 시간 **2,219ms**

### 해결 방법

**EntityGraph를 활용한 Eager Loading**:

```java
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    
    @EntityGraph(attributePaths = {"holdings"})
    @Query("SELECT DISTINCT p FROM Portfolio p " +
           "WHERE p.userId = :userId " +
           "ORDER BY p.createdAt DESC")
    List<Portfolio> findByUserIdOrderByCreatedAtDescWithHoldings(
        @Param("userId") Long userId
    );
}
```

### 성과

| 지표 | Before | After | 개선율 |
|------|--------|-------|--------|
| **SQL 쿼리 수** | 11회 | **2회** | **-82%** |
| **평균 응답시간** | N/A | 496ms | N/A |
| **에러율** | 24.45% | **0%** | **-100%** |
| **DB Connection 사용률** | 100% | 정상 | 포화 해소 |

---

## ⚡ Phase 4: Read/Write 분리 아키텍처

### 문제 분석

Phase 3 이후에도 **평균 496ms**로 목표 미달:

```java
// 기존: GET 요청마다 외부 API 호출
public PortfolioDto getPortfolioById(Long portfolioId) {
    Portfolio portfolio = repository.findById(portfolioId);
    
    // ❌ Blocking I/O: 외부 API 호출 (MarketData, CryptoPrice)
    updatePortfolioPrices(portfolio);  // 평균 300~400ms 소요
    
    return convertToDto(portfolio);
}
```

**병목점**:
- 외부 API 호출: **300~400ms** (Timeout 1초)
- MarketData API, CryptoPrice API **직렬 호출**
- 외부 API 장애 시 **READ API도 영향**

### 해결 방법

**Background Scheduler 도입 (Read/Write 분리)**:

```java
// 1. Read API: DB 조회만 수행
@Transactional(readOnly = true)
public PortfolioDto getPortfolioById(Long portfolioId) {
    Portfolio portfolio = repository.findByIdWithHoldings(portfolioId);
    
    // ✅ 순수 DB 조회만 (외부 API 호출 없음)
    return convertToDto(portfolio);
}

// 2. Background Scheduler: 주기적 가격 업데이트
@Scheduled(fixedRate = 30000)  // 30초마다
@Transactional
public void updateAllPortfolioPrices() {
    List<Portfolio> portfolios = repository.findAll();
    
    for (Portfolio portfolio : portfolios) {
        // 외부 API 호출 (백그라운드)
        updateHoldingPrices(portfolio);
    }
}
```

**아키텍처 개선**:
- ✅ **Read API**: DB 조회만 → 응답 속도 극대화
- ✅ **Scheduler**: 백그라운드에서 가격 업데이트
- ✅ **외부 API 장애 격리**: READ API 안정성 보장

### 성과

| 지표 | Phase 3 (Inline) | Phase 4 (Scheduler) | 개선율 |
|------|------------------|---------------------|--------|
| **평균 응답시간** | 496ms | **347ms** | **-30.0%** ✅ |
| **P95 응답시간** | 766ms | **460ms** | **-39.9%** ✅ |
| **P99 응답시간** | 824ms | **603ms** | **-26.9%** ✅ |
| **TPS** | 39.75 req/s | **62.74 req/s** | **+57.8%** ✅ |
| **응답 안정성 (표준편차)** | 208.71ms | **160.27ms** | **-23.2%** ✅ |

---

## 📊 최종 성과 요약

### Phase 3 → Phase 4 통합 개선율

```
📈 처리량 (TPS)
   Before: ~40 req/s
   After:  62.74 req/s
   개선율: +57.8% ✅

⚡ 응답시간 (P95)
   Before: 3,954ms (Baseline)
   After:  460ms (Phase 4)
   개선율: -88.4% ✅

🎯 안정성 (에러율)
   Before: 24.45%
   After:  0%
   개선율: -100% ✅
```

### 핵심 성과

1. **N+1 쿼리 완전 해결**
   - SQL 쿼리 수: 11회 → **2회** (-82%)
   - DB Connection Pool 포화 해소

2. **Read/Write 분리로 외부 API 의존성 격리**
   - 외부 API 장애 영향 차단
   - READ API 응답속도 향상 (496ms → 347ms, -30%)

3. **처리량 50% 이상 증가**
   - TPS: 39.75 → 62.74 req/s (+57.8%)
   - 동일 부하 환경에서 1.6배 더 많은 요청 처리

4. **안정성 확보**
   - 에러율 24.45% → 0% (완전 해소)
   - 응답 변동성 23% 감소 (표준편차 개선)

---

## 🛠️ 사용 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.5
- **Language**: Java 21
- **ORM**: Spring Data JPA + QueryDSL
- **Database**: PostgreSQL 15, Redis (Cache)

### Performance
- **Load Testing**: k6
- **Monitoring**: Prometheus + Grafana
- **APM**: Scouter

### Architecture
- **Pattern**: Read/Write Separation, CQRS 원칙 적용
- **Optimization**: EntityGraph, Background Scheduler

---

## 💡 배운 점 & 인사이트

### 1. **측정 없이 최적화 없다**

모든 개선은 **Before/After 정량 측정**으로 검증:
- Prometheus 메트릭 수집 (15초 간격)
- k6 부하 테스트 (500 VU, 8분)
- 통계적 비교 분석 (평균, P95, P99, 표준편차)

### 2. **Architecture > Tuning**

단순 파라미터 튜닝(Connection Pool 증가)보다  
**아키텍처 개선**(N+1 해결, Read/Write 분리)이 효과적:

- Phase 3 (N+1 해결): 에러율 24% → 0%
- Phase 4 (분리): 응답시간 496ms → 347ms

### 3. **병목점 단계적 해결**

```
1단계: N+1 쿼리 (가장 심각)
  ↓ EntityGraph 적용
2단계: 외부 API Blocking I/O
  ↓ Scheduler 분리
3단계: 스레드 고갈 (다음 Phase 예정)
  ↓ Virtual Threads
```

단계별로 **가장 큰 병목점**을 먼저 해결하는 전략 채택.

---

## 🚀 향후 계획 (Phase 5)

현재 347ms로 목표(< 100ms)에는 미달.  
**스레드 고갈** 문제 해결 예정:

### 발견된 병목점
- Active Threads: **233~251개** (Tomcat max에 근접)
- 스레드 대기로 인한 응답 지연

### 해결 방안
1. ✅ **Java 21 Virtual Threads** 활성화
2. ✅ **HikariCP Pool** 증대 (20 → 50)
3. ✅ **DTO 변환** 로직 최적화

**예상 효과**: 347ms → **< 100ms** (목표 달성)

---

## 📌 프로젝트 링크

- **GitHub Repository**: [Sentinel Backend](https://github.com/your-repo)
- **기술 블로그**: [Velog - Performance Optimization Series](https://velog.io/@your-blog)

---

**작성일**: 2025-11-26  
**프로젝트 기간**: 2025-11-21 ~ 2025-11-26 (6일)  
**역할**: Backend Developer (Performance Optimization Lead)
