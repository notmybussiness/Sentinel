# Phase 3: DB Optimization - Analysis

> **실험 준비 중**
>
> **현재 단계**: SQL 로깅 활성화 완료, Test Data 생성 준비

---

## 🎯 목표

Portfolio CRUD API의 N+1 쿼리 문제 해결 및 DB 성능 최적화

---

## 📋 현재 진행 상황

### 완료된 작업

✅ **SQL 로깅 활성화** (2025-11-21)
```yaml
# application-perf.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.hibernate.stat: DEBUG

spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true
```

### 다음 작업

⏳ **Lambda Mock API 테스트**
- Korea Investment API URL 확인
- AlphaVantage API URL 확인
- Finnhub API URL 확인

⏳ **Test Data 생성**
- 500명 사용자 생성
- ~2,500개 포트폴리오 생성
- ~15,000개 Holdings 생성
- Outlier 사용자 5명 포함

⏳ **Baseline 테스트**
- k6 exp5_baseline.js 실행
- SQL 로그에서 N+1 패턴 확인
- 성능 지표 수집

---

## 🔬 예상 실험

### Experiment 5a: Fetch Join
**가설**: N+1 쿼리를 Fetch Join으로 완전 제거

**예상**:
- SQL Queries: 10~20 → **1** per request
- Avg Response: 100ms → **20ms**

### Experiment 5b: Batch Size
**가설**: Lazy Loading을 Batch로 최적화

**예상**:
- SQL Queries: 10 → **log(10)** = ~2

### Experiment 5c: Connection Pool
**가설**: DB Connection Pool 증가로 대기 시간 감소

**주의**: N+1 문제가 해결되지 않으면 효과 없음 (Phase 2 교훈)

### Experiment 5d: 종합 최적화
**가설**: 모든 기법 적용으로 극대화

**예상**:
- SQL Queries: **< 5**
- Avg Response: **< 50ms**
- TPS: **> 200 req/s**

---

## 🤔 Phase 2에서 배운 교훈 적용

### 1. Architecture > Tuning
```
Phase 2: Cache Layer 위치 변경 → 210배 개선
Phase 3: Query 전략 변경 (Fetch Join) → 예상 효과 극대
```

### 2. 한 번에 하나씩
```
5a, 5b, 5c, 5d 순차 실행
→ 각 기법의 효과 명확히 파악
```

### 3. 측정 필수
```
Baseline → 실험 → 비교 → 통계적 검증
→ 데이터 기반 의사결정
```

---

## 📝 다음 업데이트 시점

Baseline 테스트 완료 후:
1. N+1 쿼리 패턴 분석
2. 사고의 흐름 기록
3. 다음 실험 계획 구체화

---

**Last Updated**: 2025-11-21
**Status**: 준비 중 (SQL 로깅 활성화 완료)
**Next Action**: Lambda Mock API 테스트 및 Test Data 생성
