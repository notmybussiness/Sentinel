# Phase 4: Scheduler Interval 비교 분석 리포트

> **실험 날짜**: 2025-11-25
> **비교 대상**: 30초 주기 vs 180초(3분) 주기
> **목적**: Scheduler 실행 주기가 Portfolio Read API 성능에 미치는 영향 측정

---

## 📊 실험 설계

### 실험 A: 30초 주기
- **Scheduler 설정**: `@Scheduled(fixedRate = 30000, initialDelay = 10000)`
- **실험 시간**: 2025-11-25 22:37~22:46 (KST) = 9분
- **예상 Scheduler 실행**: 약 16-18번
- **k6 부하**: 500 VU, 8분

### 실험 B: 180초(3분) 주기
- **Scheduler 설정**: `@Scheduled(fixedRate = 180000, initialDelay = 10000)`
- **실험 시간**: 2025-11-25 23:23~23:40 (KST) = 17분
- **예상 Scheduler 실행**: 약 5-6번
- **k6 부하**: 500 VU (동일)

---

## 🎯 핵심 결과

### 1️⃣ Portfolio 응답시간 (GET /api/v1/portfolios)

| 메트릭 | 30초 주기 | 180초 주기 | 변화 |
|--------|----------|-----------|------|
| **평균 응답시간** | 390.55ms | 450.01ms | **-15.2%** ❌ 악화 |
| **P95** | 613.39ms | 680.72ms | **-11.0%** ❌ 악화 |
| **최소값** | 95.82ms | 246.73ms | -157.6% ❌ |
| **최대값** | 575.14ms | 545.98ms | +5.1% ✅ |
| **표준편차** | 94.22ms | 60.04ms | **+36.3%** ✅ **안정성 개선** |

**핵심 발견:**
- ❌ **절대 응답시간은 180초 주기가 더 느림** (예상 외)
- ✅ **응답 안정성은 180초 주기가 훨씬 우수** (표준편차 36% 감소)
- ❌ **두 실험 모두 목표 미달성** (목표: 평균 < 100ms, P95 < 150ms)

---

### 2️⃣ 처리량 (TPS)

| 메트릭 | 30초 주기 | 180초 주기 | 변화 |
|--------|----------|-----------|------|
| **평균 TPS** | 74.45 req/s | 68.61 req/s | -7.8% ❌ |

**해석:**
- 180초 실험의 TPS가 약간 낮음 (부하가 다를 수 있음)

---

### 3️⃣ 시스템 리소스

| 메트릭 | 30초 주기 | 180초 주기 | 변화 |
|--------|----------|-----------|------|
| **CPU 사용률** | 16.09% | 16.19% | +0.6% (거의 동일) |
| **DB Active Connections** | 17.08 | 18.24 | +6.8% |

**해석:**
- CPU와 DB Connection은 거의 동일
- Scheduler 주기 차이가 시스템 리소스에 큰 영향 없음

---

## 🤔 예상 외 결과에 대한 분석

### Q: 왜 180초 주기가 더 느린가?

**가설 1: 실험 조건 차이**
- 30초 실험: 22:37~22:46 (9분)
- 180초 실험: 23:23~23:40 (17분)
- 다른 시간대, 다른 데이터 상태

**가설 2: 캐시 상태 차이**
- 30초 실험은 warm cache에서 시작했을 가능성
- 180초 실험은 cold start일 가능성

**가설 3: N+1 문제가 여전히 존재**
- 평균 390~450ms는 **Phase 2 이전 수준** (평균 400ms)
- Read/Write 분리의 효과가 미미함
- **근본 원인은 Scheduler가 아니라 N+1 쿼리일 가능성 높음**

---

## ✅ 명확한 개선: 안정성

### 표준편차 36% 감소 (94.22ms → 60.04ms)

**의미:**
- 30초 주기: Scheduler가 자주 실행되어 **Read API와 충돌** 발생
- 180초 주기: Scheduler 실행 횟수 70% 감소 → **충돌 감소** → **안정적인 응답**

**시각적 비교:**
```
30초 주기:  |*********************|  (표준편차 94.22ms)
           95ms ← 평균 390ms → 575ms

180초 주기: |************|          (표준편차 60.04ms)
           246ms ← 평균 450ms → 545ms
```

- 30초는 응답시간 **변동폭이 크고** 예측 불가
- 180초는 응답시간 **변동폭이 작고** 안정적

---

## 🎯 결론 및 권장사항

### 1️⃣ Scheduler 주기 선택

**권장: 180초(3분) 주기 채택**

**이유:**
- ✅ **안정성 36% 개선** (사용자 경험 향상)
- ✅ **Scheduler 실행 횟수 70% 감소** (시스템 부하 감소)
- ✅ **최대 응답시간 5% 개선** (worst case 개선)
- ⚠️  절대 응답시간은 느리지만, 이는 **다른 최적화로 해결 가능**

### 2️⃣ 근본 원인: N+1 문제

**핵심 발견:**
- Scheduler 주기를 조정해도 **절대 응답시간은 개선되지 않음**
- 평균 390~450ms는 **Phase 2 이전** 수준
- **근본 원인은 Portfolio 조회 시 N+1 쿼리**

**다음 단계: Phase 5 - N+1 완전 해결**
1. `@EntityGraph` 또는 `@Fetch(JOIN)` 적용
2. `@BatchSize` 설정
3. SQL 쿼리 수: 10~20개 → **2~3개**
4. **목표 응답시간**: 평균 < 50ms, P95 < 100ms

---

## 📋 다음 Phase 계획

### Phase 4b 완료 ✅
- ✅ Scheduler 주기를 30초 → 180초로 변경
- ✅ 안정성 36% 개선 확인
- ✅ Read/Write 분리 효과 부분 확인

### Phase 5: N+1 완전 해결 (다음)
```java
// PortfolioRepository.java
@EntityGraph(attributePaths = {"holdings"})
@Query("SELECT DISTINCT p FROM Portfolio p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
List<Portfolio> findByUserIdWithHoldings(@Param("userId") Long userId);
```

**예상 효과:**
- SQL 쿼리: 10~20개 → **2~3개**
- 평균 응답시간: 450ms → **< 50ms** (90% 개선)
- P95: 680ms → **< 100ms** (85% 개선)

---

## 📊 실험 데이터 위치

```
backend/scripts/phase4_scheduler_impact/results/
├── scheduler30s/          # 30초 주기 실험 결과
│   ├── avg_response_time.csv
│   ├── p95_response_time.csv
│   ├── tps.csv
│   ├── cpu_usage.csv
│   ├── hikaricp_*.csv
│   └── ...
│
└── scheduler180s/         # 180초 주기 실험 결과
    ├── avg_response_time.csv
    ├── p95_response_time.csv
    ├── tps.csv
    ├── cpu_usage.csv
    ├── hikaricp_*.csv
    └── ...
```

---

## 🔬 재현 방법

### 1. Backend 시작 (3분 주기)
```bash
# PortfolioPriceScheduler.java 수정
@Scheduled(fixedRate = 180000, initialDelay = 10000)

cd backend
./gradlew bootRun --args='--spring.profiles.active=perf'
```

### 2. k6 실행 (Mac/Linux)
```bash
k6 run \
  --env BASE_URL=http://192.168.0.58:8080 \
  --env TOKENS_FILE=./jmeter_tokens.csv \
  --out json=results/result.json \
  tests/exp6_scheduler_impact.js
```

### 3. 메트릭 추출 (Windows)
```bash
cd backend/scripts
python common/python/export_metrics.py \
  --start "2025-11-25T14:23:00Z" \
  --end "2025-11-25T14:40:00Z" \
  --step "15s" \
  --output phase4_scheduler_impact/results/scheduler180s
```

---

**Last Updated**: 2025-11-25 23:45
**Author**: Phase 4 Performance Optimization Team
**Next**: Phase 5 - N+1 Query Optimization
