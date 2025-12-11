# Phase 3: Database Optimization

> **목표**: Portfolio CRUD API의 N+1 쿼리 문제 해결 및 DB 성능 최적화

---

## 📁 폴더 구조

```
phase3_db_optimization/
├── README.md                    # 이 파일
├── EXPERIMENT_DESIGN.md         # 실험 설계 (가설, 방법론, Success Criteria)
├── ANALYSIS.md                  # 결과 분석 및 사고의 흐름
├── setup/                       # 테스트 데이터 생성 스크립트
│   ├── generate_portfolios_sql.py   # SQL로 포트폴리오 생성 (빠름!)
│   ├── execute_sql.py               # SQL 파일 실행 헬퍼
│   └── check_data.py                # 생성된 데이터 확인
├── experiment5_portfolio/       # k6 테스트 스크립트
│   └── experiment05/
│       └── exp5_baseline.js     # Baseline 테스트
├── analysis/                    # 결과 분석 스크립트
│   └── analyze_baseline.py      # Baseline 결과 분석
└── data/                        # 생성된 데이터 및 결과
    ├── jmeter_tokens.csv        # JWT 토큰 (505개)
    ├── portfolios.sql           # 포트폴리오 생성 SQL (7MB)
    └── baseline_metrics/        # Baseline 실험 결과
        └── all_metrics.csv
```

---

## 🚀 Quick Start

### 1. 테스트 데이터 생성

```bash
# Backend 실행 확인
curl http://localhost:8080/actuator/health

# 토큰 생성 (최상위 scripts/)
cd ../../..
python -X utf8 generate_tokens.py

# 포트폴리오 SQL 생성
cd results/phase3_db_optimization/setup
python -X utf8 generate_portfolios_sql.py --users 500 > ../data/portfolios.sql

# DB에 INSERT (5-10초 소요)
python -X utf8 execute_sql.py ../data/portfolios.sql

# 데이터 확인
python -X utf8 check_data.py
```

**생성되는 데이터**:
- **Portfolios**: ~7,000개
- **Holdings**: ~48,000개
- **실행 시간**: 5-10초 (API 방식 15-30분 대비 180배 빠름)

---

### 2. Baseline 테스트 실행

```bash
# k6 스크립트 위치로 이동
cd ../experiment5_portfolio/experiment05

# 토큰 파일 복사 (Mac에서 실행 시)
cp ../../data/jmeter_tokens.csv ./

# k6 테스트 실행 (Mac에서)
k6 run exp5_baseline.js
```

**테스트 시나리오**:
- **Load**: 0 → 500 → 0 VUsers (8분)
- **Operations**:
  - 10%: Portfolio 생성
  - 20%: Holding 추가
  - 50%: Portfolio 조회 (N+1 확인!)
  - 20%: Portfolio 목록 (N+1 심각!)

---

### 3. 메트릭 추출

```bash
# 최상위 scripts/로 이동
cd ../../../..

# Prometheus에서 메트릭 추출 (15:05 ~ 15:13:30 예시)
python -X utf8 export_metrics.py \
  --start "2025-11-22T06:05:00Z" \
  --end "2025-11-22T06:13:30Z" \
  --single-file \
  --output results/phase3_db_optimization/data/baseline_metrics
```

---

### 4. 결과 분석

```bash
cd results/phase3_db_optimization/analysis

# 분석 실행
python -X utf8 analyze_baseline.py ../data/baseline_metrics/all_metrics.csv
```

**출력 예시**:
```
📊 P95 응답 시간 (95th Percentile)
   /api/v1/portfolios/{id}: Avg 3,954ms, Max 5,200ms  ⚠️
   /api/v1/portfolios:      Avg 2,287ms, Max 3,176ms  ⚠️

🔌 HikariCP Connection Pool
   Active Connections: Avg 18.3, Max 20  ⚠️ Pool 100%
   Pending Connections: Avg 153.7, Max 179  🚨
   Acquire Time (P95): Avg 2,219ms  🚨

🚨 에러율: 24.45%  ❌
```

---

## 📊 Baseline 결과 요약 (2025-11-22)

### 심각한 성능 문제 발견

| 지표 | 측정값 | 목표 | 상태 |
|------|--------|------|------|
| **P95 응답 시간** | 3,954ms | < 100ms | ❌ **40배 초과** |
| **에러율** | 24.45% | < 1% | ❌ **24배 초과** |
| **HikariCP Pool** | 100% 사용 | < 80% | ❌ **완전 포화** |
| **Connection Acquire** | 2,219ms | < 10ms | ❌ **220배 초과** |

### 원인 분석

**N+1 쿼리 문제**:
```sql
-- Portfolio 조회 시
SELECT * FROM portfolios WHERE id = 1;              -- 1번
SELECT * FROM portfolio_holdings WHERE portfolio_id = 1; -- +1번
SELECT * FROM portfolio_holdings WHERE portfolio_id = 2; -- +1번
...
-- 총 1 + N = 11번 쿼리!
```

**Connection Pool 고갈**:
1. N+1 쿼리로 요청당 10~20개 SQL 실행
2. 각 SQL마다 Connection 필요
3. Pool 크기(20)를 초과하는 동시 요청
4. Pending Queue에 153개 대기!
5. 2초 이상 대기 후 Timeout/에러

---

## 🔬 다음 실험

### Experiment 5a: Fetch Join

**변경**:
```java
// Before
@Query("SELECT p FROM Portfolio p WHERE p.id = :id")
Optional<Portfolio> findById(@Param("id") Long id);

// After
@Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.holdings WHERE p.id = :id")
Optional<Portfolio> findByIdWithHoldings(@Param("id") Long id);
```

**예상 효과**:
- SQL Queries: 10~20 → **1~2개** (95% 감소)
- P95 응답 시간: 3,954ms → **< 100ms** (97% 감소)
- 에러율: 24.45% → **< 1%** (95% 감소)
- Connection Acquire: 2,219ms → **< 10ms** (99% 감소)

---

## 📝 스크립트 상세 설명

### setup/generate_portfolios_sql.py

**기능**: 테스트 데이터를 SQL로 생성 (API 방식보다 180배 빠름)

**옵션**:
```bash
--users 500              # 총 유저 수
--min-portfolios 1       # 유저당 최소 포트폴리오
--max-portfolios 20      # 유저당 최대 포트폴리오
--min-holdings 3         # 포트폴리오당 최소 종목
--max-holdings 10        # 포트폴리오당 최대 종목
```

**특징**:
- ✅ 기존 데이터가 있어도 안전 (ID 자동 증가)
- ✅ Outlier 유저 포함 (1% - 극단적 케이스)
- ✅ 한국 주식 + 미국 주식 + 암호화폐 혼합
- ✅ UNIQUE 제약 위반 방지

---

### analysis/analyze_baseline.py

**기능**: Baseline 실험 결과 자동 분석

**분석 항목**:
- TPS (Requests per Second)
- 평균/P95 응답 시간
- CPU/메모리 사용률
- HikariCP Connection Pool 상태
- 에러율

**출력**: 성능 병목 지점 식별 및 개선 방향 제시

---

## 🔗 관련 문서

- **실험 설계**: [EXPERIMENT_DESIGN.md](EXPERIMENT_DESIGN.md)
- **결과 분석**: [ANALYSIS.md](ANALYSIS.md)
- **전체 현황**: [../../../../.claude/EXPERIMENT_STATUS.md](../../../../.claude/EXPERIMENT_STATUS.md)

---

**Last Updated**: 2025-11-22
**Status**: Baseline 테스트 완료, N+1 문제 확인
**Next Step**: Fetch Join 적용 (Experiment 5a)
