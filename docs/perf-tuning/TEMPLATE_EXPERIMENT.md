# Experiment Report: EXP-XXX

> **Status**: 🔬 In Progress | ✅ Completed | ❌ Failed

## Overview

| Item | Value |
|------|-------|
| **Experiment ID** | EXP-XXX |
| **Date** | YYYY-MM-DD |
| **Target** | `/api/v1/xxx` |
| **Baseline** | `baseline_YYYY-MM-DD.json` |

---

## Hypothesis

### Observation
현재 상태에서 관찰된 문제점:
- TPS: XXX req/s (목표: YYY req/s)
- P95: XXX ms (목표: YYY ms)
- 병목 지점: (예: DB Connection Pool, GC, External API)

### Evidence
병목 지점을 뒷받침하는 데이터:
```
# 예시: HikariCP 대기 시간
hikaricp.connections.pending: 15
hikaricp.connections.active: 10 (max: 10)
```

### Hypothesis Statement
> If we **[변경 사항]**, then **[예상 결과]** because **[이유]**.

예시:
> If we increase HikariCP pool size from 10 to 30, then TPS will increase by 50% because the connection wait time will be eliminated.

---

## Experiment Design

### Independent Variable (변경 사항)
| Before | After |
|--------|-------|
| `hikari.maximum-pool-size: 10` | `hikari.maximum-pool-size: 30` |

### Controlled Variables (고정 사항)
- Test duration: 6m 30s
- VU stages: 50 → 100 → 200 → 300 → 400 → 500 → 0
- Server: Same hardware, same JVM settings
- Database: Same data, same indexes

### File Changes
```diff
# backend/src/main/resources/application.yml
spring:
  datasource:
    hikari:
-     maximum-pool-size: 10
+     maximum-pool-size: 30
```

---

## Results

### Metrics Comparison

| Metric | Baseline | Experiment | Delta | % Change |
|--------|----------|------------|-------|----------|
| **TPS** | XXX | YYY | +ZZZ | +XX% |
| **P50** | XXX ms | YYY ms | -ZZ ms | -XX% |
| **P95** | XXX ms | YYY ms | -ZZ ms | -XX% |
| **P99** | XXX ms | YYY ms | -ZZ ms | -XX% |
| **Max** | XXX ms | YYY ms | -ZZ ms | -XX% |
| **Error Rate** | X.X% | Y.Y% | -Z.Z% | - |

### Saturation Point Comparison

| Stage | VUs | Baseline TPS | Experiment TPS | Improvement |
|-------|-----|--------------|----------------|-------------|
| 2 | 100 | XXX | YYY | +ZZ% |
| 3 | 200 | XXX | YYY | +ZZ% |
| 4 | 300 | XXX | YYY | +ZZ% |
| 5 | 400 | XXX | YYY | +ZZ% |
| 6 | 500 | XXX | YYY | +ZZ% |

### Resource Utilization

| Resource | Baseline | Experiment | Notes |
|----------|----------|------------|-------|
| CPU | XX% | YY% | |
| Memory | XXX MB | YYY MB | |
| GC Pause | XX ms | YY ms | |
| DB Connections | XX/XX | YY/YY | active/max |

---

## Analysis

### TPS Trend
```
VUs:     50   100   200   300   400   500
Baseline: |----●----●----●----●----●----●
Experiment:|----●----●----●----●----●----●
                                    ^
                            Saturation point
```

### Conclusion

- [ ] ✅ **Hypothesis Confirmed** - 예상대로 성능 향상
- [ ] ❌ **Hypothesis Rejected** - 예상과 다른 결과
- [ ] ⚠️ **Inconclusive** - 추가 데이터 필요

**Summary**:
> (실험 결과 요약 작성)

---

## Recommendation

- [ ] 🚀 **Deploy to Production** - 성능 향상 확인됨
- [ ] 🔄 **Run Longer Test** - Stability 확인 필요
- [ ] 🔧 **Try Different Approach** - 다른 최적화 시도
- [ ] ⏪ **Revert Change** - 효과 없음 또는 부작용

**Next Steps**:
1.
2.
3.

---

## Learnings

### What Worked
-

### What Didn't Work
-

### Surprises
-

### Future Experiments
-

---

## Artifacts

| Type | Path |
|------|------|
| Baseline JSON | `backend/scripts/perf-tuning/results/baseline_YYYY-MM-DD.json` |
| Experiment JSON | `backend/scripts/perf-tuning/results/exp_XXX_YYYY-MM-DD.json` |
| k6 Script | `backend/scripts/perf-tuning/exp_XXX.js` |
| Config Diff | (link to commit) |

---

## Rollback Plan

If issues arise in production:

```bash
# 1. Revert configuration
git revert <commit-hash>

# 2. Restart application
./gradlew bootRun

# 3. Verify
curl http://localhost:8080/actuator/health
```
