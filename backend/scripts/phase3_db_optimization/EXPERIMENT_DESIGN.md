# Experiment 5: Portfolio API DB Optimization

> **Phase 3 - Database Performance Optimization**

---

## 🎯 실험 목표

Portfolio CRUD API의 **N+1 쿼리 문제**를 해결하고, DB 성능을 극대화한다.

---

## 📋 실험 배경

### Phase 2에서 배운 것

Market API를 최적화하면서 배운 핵심:
1. **Bottleneck을 정확히 찾기** (External API → Cache Layer)
2. **근본 원인 해결** (Provider Cache → Service Cache)
3. **측정 가능한 개선** (210배 응답 시간 개선)

### Phase 3에서 해결할 문제

Portfolio API는 **DB 작업**이 주가 되므로:
1. **N+1 쿼리**: Portfolio → Holdings 연관 관계
2. **Lazy Loading**: 필요 시점에 추가 쿼리 발생
3. **Connection Pool**: DB 연결 대기 시간
4. **Index 부재**: 조회 성능 저하

---

## 🔬 실험 설계

### Independent Variables (독립 변수)

| Variable | Values | Description |
|----------|--------|-------------|
| **Query Strategy** | Default / Fetch Join / EntityGraph | 연관 관계 로딩 방식 |
| **Batch Size** | 0 / 10 / 50 / 100 | Lazy Loading 배치 크기 |
| **Connection Pool** | 10 / 20 / 50 / 100 | HikariCP 최대 연결 수 |
| **Index** | None / Optimized | DB 인덱스 유무 |

### Dependent Variables (종속 변수)

| Metric | Unit | Target |
|--------|------|--------|
| **SQL Queries/Request** | count | < 5 |
| **Avg Response Time** | ms | < 50ms |
| **P95 Response Time** | ms | < 100ms |
| **TPS** | req/s | > 200 |
| **Error Rate** | % | < 1% |
| **DB Connection Wait** | ms | < 10ms |

### Control Variables (통제 변수)

- VUsers: 200 (고정)
- Test Duration: 8분 (고정)
- Load Pattern: 0→200 Ramp-up (동일)
- Hardware: 동일 환경
- Test Data: 동일한 Portfolio/Holding 데이터

---

## 📊 실험 시나리오

### Mixed Workload (실제 사용 패턴 반영)

```javascript
// k6 Test Scenario
export default function() {
  const operation = Math.random();

  if (operation < 0.10) {
    // 10%: Portfolio 생성
    createPortfolio();
  } else if (operation < 0.30) {
    // 20%: Holding 추가
    addHolding();
  } else if (operation < 0.80) {
    // 50%: Portfolio 조회 (N+1 예상)
    getPortfolio();
  } else {
    // 20%: Portfolio 목록 조회
    listPortfolios();
  }
}
```

**비율 근거**:
- Read:Write = 7:3 (일반적인 웹 애플리케이션 패턴)
- Portfolio 조회가 가장 빈번 (N+1 문제 영향 크게 받음)

---

## 🧪 실험 계획

### Baseline: 현재 상태

**목표**: N+1 문제 확인 및 baseline 성능 측정

**설정**:
```yaml
Query Strategy: Default (Lazy Loading)
Batch Size: 0 (disabled)
Connection Pool: 10 (default)
Index: None
```

**예상 문제**:
```sql
-- Portfolio 조회
SELECT * FROM portfolio WHERE id = 1;           -- 1번 쿼리

-- Holdings 조회 (N+1!)
SELECT * FROM holding WHERE portfolio_id = 1;   -- +1번 쿼리

-- 10개 Portfolio 조회 시: 1 + 10 = 11번 쿼리!
```

**예상 성능**:
- SQL Queries: 10~20 per request
- Avg Response: 100~200ms
- TPS: 50~100 req/s

---

### Experiment 5a: Fetch Join

**목표**: N+1 문제 해결

**변경**:
```java
// Before (Baseline)
@Query("SELECT p FROM Portfolio p WHERE p.id = :id")
Optional<Portfolio> findById(@Param("id") Long id);

// After (5a)
@Query("SELECT p FROM Portfolio p " +
       "LEFT JOIN FETCH p.holdings " +
       "WHERE p.id = :id")
Optional<Portfolio> findByIdWithHoldings(@Param("id") Long id);
```

**예상 효과**:
- SQL Queries: 10~20 → **1** per request (95% 감소)
- Avg Response: 100ms → **20ms** (80% 개선)
- TPS: 50 → **150** req/s (3배 증가)

---

### Experiment 5b: Batch Size

**목표**: Lazy Loading 최적화 (Fetch Join 없이)

**변경**:
```yaml
# application-perf.yml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 50  # 0 → 50
```

**동작 방식**:
```sql
-- Before (N+1)
SELECT * FROM holding WHERE portfolio_id = 1;
SELECT * FROM holding WHERE portfolio_id = 2;
...
SELECT * FROM holding WHERE portfolio_id = 10;

-- After (Batch)
SELECT * FROM holding WHERE portfolio_id IN (1, 2, ..., 50);
```

**예상 효과**:
- SQL Queries: 10 → **log(10)** = ~2 (80% 감소)
- Avg Response: 100ms → **50ms** (50% 개선)
- Fetch Join보다는 덜 효과적이지만 코드 변경 최소

---

### Experiment 5c: Connection Pool Tuning

**목표**: DB 연결 대기 시간 감소

**변경**:
```yaml
# application-perf.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50        # 10 → 50
      minimum-idle: 20             # 추가
      connection-timeout: 10000    # 10초
      idle-timeout: 600000         # 10분
      max-lifetime: 1800000        # 30분
```

**예상 효과**:
- DB Connection Wait: 50ms → **< 5ms**
- Thread Blocking 감소
- TPS: 50 → **100** req/s (2배)

**주의**: N+1 문제가 해결되지 않으면 큰 효과 없음 (Phase 2 Connection Pool처럼)

---

### Experiment 5d: 종합 최적화

**목표**: 모든 최적화 기법 적용

**변경**:
```java
// 1. Fetch Join
@Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.holdings ...")

// 2. Batch Size
hibernate.default_batch_fetch_size: 50

// 3. Connection Pool
hikari.maximum-pool-size: 50

// 4. Index (선택적)
CREATE INDEX idx_holding_portfolio_id ON holding(portfolio_id);
```

**예상 효과**:
- SQL Queries: 20 → **1** (95% 감소)
- Avg Response: 100ms → **10ms** (90% 개선)
- P95: 200ms → **20ms** (90% 개선)
- TPS: 50 → **250** req/s (5배 증가)
- Error Rate: 0% 유지

---

## 📈 Success Criteria

### Must Have (필수)

| Metric | Baseline | Target | Success |
|--------|----------|--------|---------|
| SQL Queries | 10~20 | **< 5** | 75% 감소 |
| Avg Response | 100ms | **< 50ms** | 50% 개선 |
| Error Rate | < 5% | **< 1%** | 안정성 |

### Nice to Have (목표)

| Metric | Baseline | Stretch Goal |
|--------|----------|--------------|
| SQL Queries | 10 | **1** (N+1 완전 제거) |
| Avg Response | 100ms | **< 20ms** (5배 개선) |
| TPS | 50 | **> 200** (4배 증가) |

---

## 🔍 측정 방법

### 1. SQL 쿼리 수 측정

```yaml
# application-perf.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

**측정**:
- 로그에서 `select` 키워드 카운트
- JMeter Custom Counter

### 2. 응답 시간 측정

- JMeter Dashboard: Average Response Time
- P95, P99 Percentile
- Time Series Graph

### 3. DB Connection Pool 모니터링

```java
@GetMapping("/actuator/hikari")
public Map<String, Object> hikariMetrics() {
    HikariDataSource ds = (HikariDataSource) dataSource;
    HikariPoolMXBean pool = ds.getHikariPoolMXBean();

    return Map.of(
        "active", pool.getActiveConnections(),
        "idle", pool.getIdleConnections(),
        "total", pool.getTotalConnections(),
        "waiting", pool.getThreadsAwaitingConnection()
    );
}
```

### 4. TPS & Error Rate

- JMeter: Requests per Second
- JMeter: Response Code (200/500)

---

## 🗓️ 실험 일정

### Day 1: Baseline
1. 현재 코드 그대로 테스트
2. N+1 문제 확인 (SQL 로그)
3. Baseline 성능 측정

### Day 2: Fetch Join (5a)
1. Repository에 Fetch Join 추가
2. 테스트 실행
3. SQL 쿼리 수 비교

### Day 3: Batch Size (5b)
1. Fetch Join 제거 (Baseline 복원)
2. Batch Size 설정 추가
3. 테스트 실행
4. 5a와 비교

### Day 4: Connection Pool (5c)
1. Baseline 복원
2. Connection Pool 증가
3. 테스트 실행
4. 효과 분석 (N+1 있으면 효과 적음 예상)

### Day 5: 종합 최적화 (5d)
1. Fetch Join + Batch Size + Pool 모두 적용
2. 테스트 실행
3. 최종 성능 측정

### Day 6: 분석 & 리포트
1. 5개 실험 결과 비교
2. 통계적 유의성 검증 (t-test)
3. 최종 리포트 작성

---

## 📦 Test Data 준비

### 1. Test Users (200명)

```bash
python scripts/step1_generate_tokens.py --count 200
```

### 2. Sample Portfolios

```sql
-- 각 사용자당 2~5개 Portfolio
-- 각 Portfolio당 5~20개 Holdings
-- 총 ~2000 Portfolios, ~20000 Holdings
```

```bash
python scripts/step2_generate_portfolios.py
```

### 3. 데이터 분포

| Entity | Count | Avg per User |
|--------|-------|--------------|
| Users | 200 | 1 |
| Portfolios | 600 | 3 |
| Holdings | 6000 | 10 per portfolio |

**이유**: N+1 문제를 명확히 드러내기 위한 충분한 데이터

---

## 🎯 Expected Outcomes

### Hypothesis 1: Fetch Join이 가장 효과적
```
N+1 쿼리 완전 제거 → 단일 JOIN 쿼리
→ 95% 쿼리 감소, 80% 응답 시간 개선
```

### Hypothesis 2: Batch Size는 차선책
```
N→N 쿼리를 N→log(N)으로 감소
→ 80% 쿼리 감소, 50% 응답 시간 개선
(Fetch Join보다 덜 효과적, 하지만 코드 변경 최소)
```

### Hypothesis 3: Connection Pool 단독으로는 불충분
```
Phase 2에서 배운 것처럼, N+1이 있으면
Connection Pool만 늘려도 근본 해결 안 됨
→ 20~30% 개선에 그칠 것
```

### Hypothesis 4: 종합 최적화가 최고
```
Fetch Join + Batch Size + Pool
→ 90% 응답 시간 개선, 5배 TPS 증가
```

---

## 🚨 Risk & Mitigation

### Risk 1: 테스트 데이터 부족
- **Impact**: N+1 문제가 명확히 드러나지 않음
- **Mitigation**: 충분한 Holdings 생성 (portfolio당 10+)

### Risk 2: Fetch Join Cartesian Product
- **Impact**: JOIN 결과가 너무 커져서 오히려 느려짐
- **Mitigation**: `DISTINCT` 사용, 필요한 필드만 SELECT

### Risk 3: Connection Pool Exhaustion
- **Impact**: Pool이 부족해서 Connection Wait 발생
- **Mitigation**: Monitoring으로 적정 크기 찾기 (50~100)

### Risk 4: Transaction Deadlock
- **Impact**: Write 작업 동시 발생 시 Deadlock
- **Mitigation**: Read:Write = 7:3 비율 유지, Optimistic Lock

---

## 📚 Reference

### Phase 2에서 배운 교훈
1. **Bottleneck을 먼저 찾기**: N+1이 진짜 문제인지 확인
2. **Baseline 측정 필수**: 개선율을 정량적으로 증명
3. **한 번에 하나씩**: 변수를 하나씩 바꿔가며 인과 관계 파악
4. **통계적 검증**: t-test로 유의성 확인

### JPA N+1 Best Practices
- Fetch Join: 연관 관계 즉시 로딩
- @EntityGraph: 선언적 Fetch Join
- Batch Size: Lazy Loading 최적화
- DTO Projection: 필요한 데이터만 조회

---

## ✅ Checklist

### 실험 준비
- [ ] Test Users 200명 생성
- [ ] Sample Portfolios 생성 (600개)
- [ ] Sample Holdings 생성 (6000개)
- [ ] k6 스크립트 작성 (Mixed Workload)
- [ ] SQL 로깅 활성화

### 실험 실행
- [ ] Baseline 테스트
- [ ] Exp 5a: Fetch Join
- [ ] Exp 5b: Batch Size
- [ ] Exp 5c: Connection Pool
- [ ] Exp 5d: 종합 최적화

### 분석 & 문서화
- [ ] SQL 쿼리 수 비교
- [ ] 응답 시간 비교 (t-test)
- [ ] TPS 비교
- [ ] 최종 리포트 작성
- [ ] Velog 포스팅

---

**실험 시작 준비 완료!** 🚀

이제 Baseline 테스트를 실행하고, N+1 문제를 확인하는 단계로 넘어갑니다.
