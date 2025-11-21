# Sentinel Performance Optimization - Experiment Status

> **전체 실험 진행 상황 요약**
>
> **Last Updated**: 2025-11-21
> **Current Phase**: Phase 3 (DB Optimization)
> **Status**: Phase 2 완료 (210배 성능 개선), Phase 3 준비 중

---

## 🎯 현재 위치

### ✅ 완료된 Phase

- **Phase 1**: Cache Optimization (완료)
- **Phase 2**: External API Optimization (완료)

### 🔄 진행 중

- **Phase 3**: Database Optimization (준비 중)
  - **다음 작업**: SQL 로깅 활성화 완료 ✅
  - **다음 단계**: Lambda Mock API 테스트 → Test Data 생성 → Baseline 실행

---

## 📊 Phase별 성과 요약

### Phase 1: Cache Optimization
**목표**: External API Rate Limit 회피
**기간**: 2025-11-14 ~ 2025-11-20

| Experiment | 주요 변경 | 결과 |
|------------|----------|------|
| **01** | Caffeine Cache 도입 (TTL 1분) | 500 에러 827 → 1,140건 (역효과) |
| **02** | Circuit Breaker 적용 | 500 에러 0건, 응답시간 4배 증가 |
| **03** | Cache TTL 3분 최적화 | 500 에러 0건 완전 제거 ✅ |

**핵심 발견**:
- ❌ 캐싱이 항상 좋은 것은 아니다 (Experiment 01 역효과)
- ⚠️ Circuit Breaker는 세밀한 튜닝이 필요 (Experiment 02)
- ✅ TTL을 Rate Limit에 맞춰 조정하면 효과적 (Experiment 03)

**다음 Phase로 넘어간 이유**:
- Cache Layer **위치**를 최적화하면 더 큰 성능 개선 가능성 발견

---

### Phase 2: External API Optimization
**목표**: Cache Layer 위치 최적화 + Connection Pool 튜닝
**기간**: 2025-11-20 ~ 2025-11-21

| Experiment | 주요 변경 | 결과 |
|------------|----------|------|
| **04 (Baseline)** | Provider Cache + Simple HTTP | Avg 1,949ms, 500 에러 1,244건 |
| **04b** | Connection Pool 200/50 | 에러 증가 (3,381건) |
| **04c** | Connection Pool 500/200 | Thread Pool 포화 |
| **04d** | Service Cache (TTL 10초) | **Avg 10ms (210배 개선!)** ✅ |

**성과**:
- ✅ 평균 응답시간: 1,949ms → **10ms** (210배 개선)
- ✅ P95: 3,675ms → **7ms** (525배 개선)
- ✅ TPS: 106 → **480 req/s** (4.5배 증가)
- ✅ 500 에러: 1,244건 → **0건** (완벽 제거)

**핵심 발견**:
- 🎯 Cache Layer **위치**가 모든 것을 결정 (Provider → Service로 이동)
- 🎯 짧은 TTL(10초)이 긴 TTL(3분)보다 Lock Contention 감소
- 🎯 Connection Pool은 부차적 (Cache Hit Rate가 높으면 무의미)

**다음 Phase로 넘어간 이유**:
- External API 병목 완전 해결 → 이제 **DB 최적화** 차례

---

### Phase 3: Database Optimization
**목표**: Portfolio CRUD API의 N+1 쿼리 해결 + DB 성능 최적화
**기간**: 2025-11-21 ~ (진행 중)

| Task | Status | Notes |
|------|--------|-------|
| SQL 로깅 활성화 | ✅ 완료 | `application-perf.yml` 수정 완료 |
| Lambda Mock API 확인 | ⏳ 다음 | 3개 API 배포 상태 테스트 |
| Test Users 생성 (500명) | ⏳ 대기 | `step1_generate_tokens.py` |
| Portfolios 생성 (~2,500개) | ⏳ 대기 | `step2_generate_portfolios.py` |
| Baseline 테스트 실행 | ⏳ 대기 | k6 `exp5_baseline.js` |
| N+1 쿼리 패턴 확인 | ⏳ 대기 | SQL 로그 분석 |

**예상 실험**:
- **Exp 5a**: Fetch Join (N+1 완전 제거)
- **Exp 5b**: Batch Size (N→log(N))
- **Exp 5c**: Connection Pool Tuning
- **Exp 5d**: 종합 최적화

**목표 성능**:
- SQL Queries: 10~20 → **< 5** per request
- Avg Response: 100ms → **< 50ms**
- P95: 200ms → **< 100ms**
- TPS: 50 → **> 200 req/s**

---

## 🚀 다음 작업 (Phase 3)

### ✅ 완료된 준비 작업 (2025-11-21)

1. **SQL 로깅 활성화** ✅
   - `application-perf.yml` 수정 완료
   - `org.hibernate.SQL: DEBUG`
   - `org.hibernate.type.descriptor.sql.BasicBinder: TRACE`
   - `generate_statistics: true`

2. **Lambda Mock API 테스트** ✅
   - Korea Investment API: 정상 (50-100ms)
   - AlphaVantage API: 정상 (280ms)
   - Finnhub API: 정상 (550ms)
   - 모든 API 200 OK, 데이터 정상 반환

3. **Test Scripts 준비** ✅
   - `generate_tokens.py` → `scripts/` 복사 완료
   - `step2_generate_portfolios.py` → `scripts/` 복사 완료
   - 빠른 토큰 생성 방식 (1번 API 호출로 500개)

4. **문서 정리** ✅
   - Phase 1, 2, 3 문서 구조 재정리
   - EXPERIMENT_STATUS.md 생성
   - CLAUDE.md 업데이트
   - Git commit 완료 (c7ada81)

### 📍 현재 위치: Backend 재시작 대기

**다음 즉시 작업**:

1. **Backend 재시작 (Windows PC1)**
   ```bash
   # CMD or PowerShell에서 실행
   cd C:\Users\zetto\Desktop\Sentinel\backend
   .\gradlew bootRun --args="--spring.profiles.active=perf"
   ```

   **확인사항**:
   - Port 8080 리스닝 확인
   - 콘솔에 SQL 로그 출력 확인 (Hibernate: select ...)
   - Lambda Mock API URL 3개 설정 확인

2. **Test Data 생성 (Mac)**
   ```bash
   cd /path/to/sentinel/backend/scripts

   # Step 1: 토큰 생성 (1-2분 소요)
   python3 generate_tokens.py
   # 결과: jmeter_tokens.csv (500개 토큰)

   # Step 2: 포트폴리오 생성 (15-30분 소요)
   python3 step2_generate_portfolios.py --tokens jmeter_tokens.csv
   # 결과: ~2,500 portfolios, ~15,000 holdings
   ```

   **참고**:
   - `generate_tokens.py`는 Backend의 `/api/v1/auth/perf/tokens` API 호출
   - Backend에 500명 perftest 유저가 미리 존재해야 함
   - `step2`는 REST API로 Portfolio/Holding 생성 (JPA ORM으로 DB INSERT)

3. **Baseline 테스트 실행 (Mac)**
   ```bash
   cp jmeter_tokens.csv scripts/k6/
   cd scripts/k6
   k6 run exp5_baseline.js
   ```

   **측정 목표**:
   - SQL 쿼리 수 (N+1 패턴 확인)
   - 평균 응답 시간
   - P95 응답 시간
   - TPS (처리량)

### 이후 계획

5. SQL 로그에서 N+1 패턴 확인
6. Experiment 5a (Fetch Join) 구현 및 테스트
7. Experiment 5b, 5c, 5d 순차 진행
8. 최종 성능 비교 분석

---

## 📚 문서 구조

### 전체 현황
- **이 파일**: `.claude/EXPERIMENT_STATUS.md` ← Claude Code 시작 시 먼저 읽기

### Phase별 상세 문서
```
scripts/results/
├── phase1_cache_experiments/
│   ├── README.md              # Problem → Solution 요약
│   ├── EXPERIMENT_DESIGN.md   # 실험 설계 (없음, Phase 2부터 추가)
│   └── ANALYSIS.md            # 결과 분석 + 사고의 흐름
├── phase2_external_api_optimization/
│   ├── README.md              # Problem → Solution 요약
│   ├── EXPERIMENT_DESIGN.md   # 실험 설계 (없음)
│   └── ANALYSIS.md            # 결과 분석 + 사고의 흐름
└── phase3_db_optimization/
    ├── README.md              # Problem → Solution 요약
    ├── EXPERIMENT_DESIGN.md   # 실험 설계
    └── ANALYSIS.md            # (생성 예정) 결과 분석
```

### 실행 가이드
- `scripts/PHASE3_SETUP_GUIDE.md` - Phase 3 실행 가이드 (단계별)

---

## 🎓 주요 교훈

### Phase 1에서 배운 것
1. **캐싱은 만능이 아니다** - 실시간 데이터에는 역효과 가능
2. **Circuit Breaker 튜닝 중요** - 기본값은 일반적인 상황용
3. **TTL은 Rate Limit에 맞춰야** - 3분 TTL로 완벽 해결

### Phase 2에서 배운 것
1. **Cache Layer 위치가 핵심** - Service > Provider
2. **짧은 TTL이 더 좋을 수 있다** - Lock Contention 최소화
3. **Connection Pool은 부차적** - Cache 없으면 중요, 있으면 무의미
4. **측정 없이 최적화하지 마라** - 통계적 검증 필수

### Phase 3에서 배울 것 (예상)
1. N+1 쿼리 해결 방법 (Fetch Join vs Batch Size)
2. DB Connection Pool 튜닝의 효과
3. 종합 최적화 시 시너지 효과

---

## ⚠️ 주의사항

### Claude Code 사용 시
1. **항상 이 파일을 먼저 읽기** - 현재 위치와 다음 작업 파악
2. **불명확한 점은 사용자에게 질문** - 추측하지 말 것
3. **Phase별 상세 문서 참조** - README, DESIGN, ANALYSIS 확인

### 실험 진행 시
1. **한 번에 하나씩** - 변수를 하나씩 바꿔가며 테스트
2. **측정 필수** - Baseline → 변경 → 비교 (통계적 검증)
3. **문서화** - 사고의 흐름과 다음 단계 계획 기록

---

## 📞 Contact

문서 구조나 실험 방향에 대해 질문이 있으면:
1. 먼저 해당 Phase의 `ANALYSIS.md` 확인
2. 여전히 불명확하면 **사용자에게 질문**
3. 추측하지 말고 명확히 하기

---

**Last Updated**: 2025-11-21
**Current Task**: Lambda Mock API 테스트 및 Test Data 생성 준비
**Next Milestone**: Phase 3 Baseline 테스트 완료
