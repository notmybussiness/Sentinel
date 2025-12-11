# Phase 4: Scheduler Impact Test

> **Goal**: Measure performance impact of PortfolioPriceScheduler (30-second update cycle)  
> **Date**: 2025-11-25  
> **Status**: 🔄 In Progress

---

## 🎯 Objectives

Scheduler가 Portfolio Read API 성능에 미치는 영향 측정

### Key Questions
1. 30초마다 실행되는 Scheduler가 Read API를 느리게 만드는가?
2. DB Lock 또는 Transaction Conflict가 발생하는가?
3. Read/Write 분리의 효과가 충분한가?

---

## 🔬 Test Scenario

### Scheduler 설정
```java
@Scheduled(fixedRate = 30000, initialDelay = 10000)
// - 30초마다 실행 (이전: 5분)
// - 앱 시작 10초 후 첫 실행
```

### Scheduler 동작
1. 모든 Portfolio 조회
2. 각 Portfolio의 Holding 가격 업데이트 (External API 호출)
3. Portfolio 재계산 및 저장 (DB Write)

### 예상 충돌 시나리오
```
T=0s:  Read API 요청 (100 VU)
T=10s: Scheduler 시작 (모든 Portfolio Write)  ← 충돌 가능성!
T=40s: Scheduler 재실행
T=70s: Scheduler 재실행
```

---

## 🧪 Experiment Design

### Load Profile
```javascript
Ramp-up:   0 → 50 VU (30s)
Steady:    50 VU (2m)      // Scheduler 4번 실행 예상
Spike:     50 → 100 VU (30s)
High Load: 100 VU (1m)     // Scheduler 2번 실행
Ramp-down: 100 → 0 VU (30s)
```

### Metrics
- **Portfolio Read Time**: P95 < 150ms (목표)
- **Scheduler Conflict Rate**: < 10% (느린 응답 비율)
- **Cache Hit Rate**: > 80%
- **Error Rate**: < 1%

---

## 📊 Success Criteria

| Metric | Target | Notes |
|--------|--------|-------|
| P95 Response Time | < 150ms | Scheduler 실행 중에도 |
| Scheduler Conflict | < 10% | 500ms 이상 응답 |
| Error Rate | < 1% | Lock Timeout 없음 |
| Cache Hit Rate | > 80% | Read-only Transaction |

---

## 🚀 How to Run

### 1. Windows에서 준비
```bash
# WAS 시작 (perf profile)
cd C:\Users\zetto\Desktop\Sentinel\backend
./gradlew bootRun --args='--spring.profiles.active=perf'

# 토큰 파일 확인
# phase3_db_optimization/data/jmeter_tokens.csv 재사용
```

### 2. Mac에서 실행
```bash
# Naver Cloud에서 다운로드
cd ~/k6_tests/phase4_scheduler_impact

# k6 실행
k6 run \
  --env BASE_URL=http://192.168.0.58:8080 \
  --env TOKENS_FILE=./jmeter_tokens.csv \
  --out json=results/exp4/result.json \
  tests/exp4_scheduler_impact.js
```

### 3. Windows에서 분석
```bash
# 결과 다운로드 (Naver Cloud)

# Prometheus 메트릭 추출
cd C:\Users\zetto\Desktop\Sentinel\backend\scripts
python common\python\export_metrics.py \
  --prometheus-url http://192.168.0.5:9090 \
  --start "2025-XX-XXTXX:XX:XXZ" \
  --end "2025-XX-XXTXX:XX:XXZ" \
  --output phase4_scheduler_impact\results\exp4
```

---

## 🔍 Analysis Points

### 1. Response Time Patterns
- 30초마다 응답시간 spike 발생하는가?
- P95가 목표(150ms)를 초과하는가?

### 2. Scheduler Logs
```bash
# WAS 로그에서 Scheduler 실행 시간 확인
grep "Portfolio Price Update" logs/application.log

# 예상 로그:
# [Scheduled] Portfolio Price Update Started
# [Scheduled] Portfolio Price Update Completed. Portfolios: 7000, Holdings: 48000
```

### 3. DB Metrics
- Active Connections 증가?
- Lock Wait Time?
- Transaction Conflicts?

---

## 💡 Expected Results

### Best Case ✅
- Scheduler가 Read API에 영향 없음
- P95 < 100ms 유지
- @Transactional(readOnly=true) 효과

### Worst Case ❌
- 30초마다 응답시간 spike
- P95 > 300ms
- Lock Timeout 에러

### Most Likely 🤔
- 약간의 성능 저하 (P95: 100~150ms)
- Scheduler 실행 중 높은 DB CPU
- Cache Hit로 대부분 영향 최소화

---

## 🛠️ Mitigation Strategies

### If Scheduler is Problematic:
1. **Increase Interval**: 30초 → 1분 또는 2분
2. **Batch Processing**: 한 번에 전체가 아닌 100개씩 나눠서
3. **Async with Virtual Threads**: Scheduler를 비동기로
4. **Separate DB**: Read Replica 사용

---

**Next**: 테스트 실행 후 REPORT.md 작성
