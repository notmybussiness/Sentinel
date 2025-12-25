# Baseline Report: [Endpoint Name]

> **Date**: YYYY-MM-DD HH:mm
> **Tester**: Claude Code /perf-tuning

---

## Test Configuration

| Item | Value |
|------|-------|
| **Endpoint** | `POST /api/v1/xxx` |
| **Method** | GET / POST |
| **Auth** | Bearer Token |
| **Duration** | 6m 30s |
| **Stages** | 50 → 100 → 200 → 300 → 400 → 500 → 0 VUs |

### Environment
| Component | Version/Config |
|-----------|----------------|
| Spring Boot | 3.x.x |
| JVM | OpenJDK 21 |
| Heap | -Xms512m -Xmx2g |
| Database | PostgreSQL 16 |
| Redis | 7.x |

---

## Results Summary

### Key Metrics

| Metric | Value | Target | Status |
|--------|-------|--------|--------|
| **Peak TPS** | XXX req/s | YYY req/s | 🔴/🟡/🟢 |
| **P50 Latency** | XXX ms | < YYY ms | 🔴/🟡/🟢 |
| **P95 Latency** | XXX ms | < YYY ms | 🔴/🟡/🟢 |
| **P99 Latency** | XXX ms | < YYY ms | 🔴/🟡/🟢 |
| **Error Rate** | X.XX% | < 1% | 🔴/🟡/🟢 |

### Throughput by Stage

| Stage | VUs | TPS | P95 | Error % | Notes |
|-------|-----|-----|-----|---------|-------|
| Warm-up | 50 | XXX | XXX ms | 0% | |
| Stage 2 | 100 | XXX | XXX ms | 0% | |
| Stage 3 | 200 | XXX | XXX ms | 0% | |
| Stage 4 | 300 | XXX | XXX ms | X% | ⚠️ Latency increase |
| Stage 5 | 400 | XXX | XXX ms | X% | |
| Stage 6 | 500 | XXX | XXX ms | X% | 🔴 Saturation |

---

## Saturation Analysis

### Saturation Point Detection

```
TPS
 ^
 |                    ●----●----● (plateau)
 |              ●----/
 |        ●----/
 |   ●---/
 |--●
 +-----------------------------------> VUs
    50   100   200   300   400   500
                      ^
                Saturation Point
```

**Findings**:
- **Saturation Point**: XXX VUs (TPS plateau 시작)
- **Breaking Point**: XXX VUs (Error rate spike)
- **Optimal Load**: XXX VUs (Best TPS/Latency ratio)

### Saturation Evidence
```
Stage 4 → Stage 5:
  TPS increase: (XXX - YYY) / YYY = Z% (< 5% threshold)
  P95 increase: XXX ms → YYY ms (50% increase)
  → Saturation detected at Stage 4 (300 VUs)
```

---

## Resource Utilization

### System Resources (at peak load)

| Resource | Value | Threshold | Status |
|----------|-------|-----------|--------|
| CPU | XX% | 80% | 🔴/🟡/🟢 |
| Memory | XXX MB / YYY MB | 80% | 🔴/🟡/🟢 |
| GC Pause (avg) | XX ms | 100ms | 🔴/🟡/🟢 |
| GC Pause (max) | XX ms | 500ms | 🔴/🟡/🟢 |

### Database

| Metric | Value | Notes |
|--------|-------|-------|
| Active Connections | XX / YY | |
| Pending Connections | XX | ⚠️ Pool exhaustion? |
| Avg Query Time | XX ms | |
| Slow Queries | X | |

### External Services

| Service | Avg Latency | Error Rate |
|---------|-------------|------------|
| Redis | XX ms | 0% |
| External API | XX ms | X% |

---

## Bottleneck Candidates

Based on the baseline analysis, potential bottlenecks identified:

### 🔴 High Priority
1. **[Bottleneck Name]**
   - Evidence: (데이터)
   - Impact: (영향도)
   - Hypothesis: (가설)

### 🟡 Medium Priority
1. **[Bottleneck Name]**
   - Evidence:
   - Impact:
   - Hypothesis:

### 🟢 Low Priority
1. **[Bottleneck Name]**
   - Evidence:
   - Impact:
   - Hypothesis:

---

## Recommended Experiments

Based on bottleneck analysis, prioritized experiments:

| Priority | Experiment | Expected Impact | Effort |
|----------|------------|-----------------|--------|
| 1 | [실험명] | +XX% TPS | Low |
| 2 | [실험명] | -XX% P95 | Medium |
| 3 | [실험명] | +XX% TPS | High |

---

## Raw Data

| Type | Path |
|------|------|
| k6 Script | `backend/scripts/perf-tuning/exp_xxx_baseline.js` |
| Results JSON | `backend/scripts/perf-tuning/results/baseline_YYYY-MM-DD.json` |
| System Metrics | (link if available) |

---

## Next Steps

1. [ ] Review bottleneck candidates with team
2. [ ] Prioritize experiments
3. [ ] Create hypothesis document for first experiment
4. [ ] Run experiment EXP-001
