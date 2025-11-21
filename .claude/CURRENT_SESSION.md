# Current Session State

> **Session Date**: 2025-11-21
> **Current Phase**: Phase 3 DB Optimization - Baseline 준비
> **Status**: Backend 재시작 대기 중

---

## 📍 지금 어디까지 했나

### ✅ 완료된 작업

1. **문서 구조 대대적 재정리**
   - Phase 1, 2, 3로 실험 결과 폴더 재구성
   - README.md (Problem-Solution)
   - ANALYSIS.md (사고의 흐름)
   - 중앙 관리: `.claude/EXPERIMENT_STATUS.md`

2. **Phase 3 준비 작업**
   - SQL 로깅 활성화 (`application-perf.yml`)
   - Lambda Mock API 3개 테스트 (모두 정상)
   - `generate_tokens.py` 설정 (빠른 버전)
   - `step2_generate_portfolios.py` 준비

3. **Git Commit**
   - Commit: c7ada81
   - Message: "refactor: reorganize performance experiment documentation"
   - 94 files changed

---

## 🎯 지금 해야 할 일

### 1단계: Backend 재시작 (Windows PC1)

```bash
cd C:\Users\zetto\Desktop\Sentinel\backend
.\gradlew bootRun --args="--spring.profiles.active=perf"
```

**반드시 확인**:
- [ ] Port 8080 리스닝
- [ ] 콘솔에 SQL 로그 출력 (`Hibernate: select ...`)
- [ ] `/api/v1/auth/perf/stats` 호출해서 500명 유저 확인

### 2단계: Test Data 생성 (Mac)

```bash
cd /path/to/sentinel/backend/scripts

# 토큰 생성 (1-2분)
python3 generate_tokens.py

# 포트폴리오 생성 (15-30분)
python3 step2_generate_portfolios.py --tokens jmeter_tokens.csv
```

**결과물**:
- `jmeter_tokens.csv` (500개 JWT 토큰)
- DB에 ~2,500 portfolios, ~15,000 holdings

### 3단계: Baseline 테스트 (Mac)

```bash
cp jmeter_tokens.csv scripts/k6/
cd scripts/k6
k6 run exp5_baseline.js
```

**측정 항목**:
- SQL 쿼리 수 (N+1 패턴 확인!)
- 평균/P95 응답 시간
- TPS

---

## 🔑 핵심 포인트

### generate_tokens.py 동작 방식

```
Python Script
    ↓
GET /api/v1/auth/perf/tokens (Backend API)
    ↓
Backend: DB에서 perftest 유저 500명 조회 (이미 존재해야 함)
    ↓
각 유저별로 JWT 토큰 생성
    ↓
500개 토큰 반환 (1번의 API 호출로!)
    ↓
jmeter_tokens.csv 저장
```

**중요**: Backend에 500명 perftest 유저가 미리 DB에 있어야 함

### step2_generate_portfolios.py 동작 방식

```
Python Script
    ↓
POST /api/v1/portfolios (각 포트폴리오)
    ↓
Backend Controller → Service → JPA Repository
    ↓
Hibernate (ORM): INSERT INTO portfolio (...)
    ↓
POST /api/v1/portfolios/{id}/holdings (각 종목)
    ↓
Hibernate (ORM): INSERT INTO holding (...)
```

**중요**: Python은 REST API만 호출, DB 직접 접근 안 함

---

## 📊 Phase 3 목표

### Baseline (현재 상태)
- SQL Queries: 10-20 per request (예상)
- Avg Response: 100-200ms (예상)
- N+1 문제 확인!

### Experiment 5a (Fetch Join)
- SQL Queries: 1 per request
- Avg Response: < 50ms

### Experiment 5d (종합 최적화)
- SQL Queries: < 5 per request
- Avg Response: < 50ms
- TPS: > 200 req/s

---

## 📚 참고 문서

**전체 현황**:
- `.claude/EXPERIMENT_STATUS.md` ← 가장 먼저 읽기

**Phase 3 상세**:
- `scripts/PHASE3_SETUP_GUIDE.md` ← 실행 가이드
- `scripts/results/phase3_db_optimization/EXPERIMENT_DESIGN.md` ← 실험 설계
- `scripts/results/phase3_db_optimization/ANALYSIS.md` ← 결과 분석 (작성 예정)

**Phase 1, 2 결과**:
- `scripts/results/phase1_cache_experiments/` ← Cache TTL 최적화
- `scripts/results/phase2_external_api_optimization/` ← 210배 개선

---

## ⚠️ 주의사항

1. **Backend는 Windows에서 실행** (CMD/PowerShell, Git Bash 안됨)
2. **k6 테스트는 Mac에서 실행**
3. **CSV 토큰 파일은 Mac에 있어야 함**
4. **불명확한 점은 사용자에게 질문** (추측 금지)

---

**Last Updated**: 2025-11-21 16:40
**Next Action**: Backend 재시작 (perf 프로필)
**Git Commit**: c7ada81
