---
name: perf-tuning
description: Performance testing and tuning workflow. Measures baseline, finds saturation point, identifies bottlenecks, and runs A/B experiments. Use when optimizing performance, load testing, finding bottlenecks, or comparing before/after metrics. Keywords: performance, load test, k6, benchmark, saturation, bottleneck, tuning, optimization, TPS, latency.
---

# Performance Tuning Skill

## Purpose
Systematic performance optimization through:
1. **Baseline Measurement** - Find saturation point with k6 load tests
2. **Bottleneck Analysis** - Identify performance limiting factors
3. **Experiment Execution** - A/B comparison with controlled variables

## Workflow Overview

```
┌─────────────────────────────────────────────────────────┐
│ Phase 1: BASELINE                                       │
│ - Generate/run k6 load test script                      │
│ - Find saturation point (TPS plateau)                   │
│ - Collect metrics: TPS, P50/P95/P99, Error Rate         │
│ - Save baseline report                                  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Phase 2: BOTTLENECK ANALYSIS                            │
│ - System resources (CPU, Memory, GC)                    │
│ - Database (slow queries, connection pool)              │
│ - Network (latency, connection limits)                  │
│ - Application (thread pool, caching)                    │
│ - Generate hypothesis for improvement                   │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│ Phase 3: EXPERIMENT                                     │
│ - Define experiment (hypothesis, variable, expected)    │
│ - Apply tuning change                                   │
│ - Run experiment test                                   │
│ - Compare: Control (baseline) vs Treatment (tuned)      │
│ - Generate comparison report                            │
└─────────────────────────────────────────────────────────┘
```

---

## Phase 1: Baseline Measurement

### Step 1.1: Identify Target Endpoint
Ask user or analyze codebase:
- Which endpoint(s) to test?
- Expected load pattern (steady, spike, ramp)?
- Target metrics (TPS goal, latency SLA)?

### Step 1.2: Generate k6 Script
Create load test script using this template:

```javascript
// File: backend/scripts/perf-tuning/exp_<name>_baseline.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const latency = new Trend('latency');

// Test configuration - Saturation Point Discovery
export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Warm-up
    { duration: '1m', target: 100 },   // Ramp to 100 VUs
    { duration: '1m', target: 200 },   // Ramp to 200 VUs
    { duration: '1m', target: 300 },   // Ramp to 300 VUs
    { duration: '1m', target: 400 },   // Ramp to 400 VUs
    { duration: '1m', target: 500 },   // Ramp to 500 VUs (find ceiling)
    { duration: '30s', target: 0 },    // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% under 500ms
    errors: ['rate<0.01'],              // Error rate < 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';

export default function () {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${TOKEN}`,
  };

  // === MODIFY THIS SECTION FOR YOUR ENDPOINT ===
  const res = http.get(`${BASE_URL}/api/v1/your-endpoint`, { headers });
  // OR for POST:
  // const payload = JSON.stringify({ key: 'value' });
  // const res = http.post(`${BASE_URL}/api/v1/your-endpoint`, payload, { headers });
  // ==============================================

  // Record metrics
  latency.add(res.timings.duration);
  errorRate.add(res.status !== 200);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.1); // Think time
}

export function handleSummary(data) {
  const timestamp = new Date().toISOString().split('T')[0];
  return {
    [`backend/scripts/perf-tuning/results/baseline_${timestamp}.json`]: JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, opts) {
  const metrics = data.metrics;
  return `
================================================================================
                         BASELINE TEST RESULTS
================================================================================
Endpoint:      ${BASE_URL}
Duration:      ${(data.state.testRunDurationMs / 1000).toFixed(0)}s
VUs (max):     ${data.metrics.vus_max?.values?.max || 'N/A'}

THROUGHPUT
  Requests:    ${metrics.http_reqs?.values?.count || 0}
  TPS:         ${(metrics.http_reqs?.values?.rate || 0).toFixed(2)} req/s

LATENCY
  P50:         ${(metrics.http_req_duration?.values?.['p(50)'] || 0).toFixed(2)}ms
  P95:         ${(metrics.http_req_duration?.values?.['p(95)'] || 0).toFixed(2)}ms
  P99:         ${(metrics.http_req_duration?.values?.['p(99)'] || 0).toFixed(2)}ms
  Max:         ${(metrics.http_req_duration?.values?.max || 0).toFixed(2)}ms

ERRORS
  Rate:        ${((metrics.http_req_failed?.values?.rate || 0) * 100).toFixed(2)}%
  Count:       ${metrics.http_req_failed?.values?.passes || 0}

================================================================================
`;
}
```

### Step 1.3: Run Baseline Test
```bash
# Create results directory
mkdir -p backend/scripts/perf-tuning/results

# Run k6 test
k6 run backend/scripts/perf-tuning/exp_<name>_baseline.js \
  -e BASE_URL=http://localhost:8080 \
  -e TOKEN="<jwt_token>"
```

### Step 1.4: Find Saturation Point
Analyze results to find:
- **Saturation Point**: VU count where TPS stops increasing
- **Breaking Point**: VU count where errors spike or latency explodes
- **Optimal Load**: VU count with best TPS/latency ratio

**Saturation Detection Logic**:
```
If (TPS[n] - TPS[n-1]) / TPS[n-1] < 5%  AND  P95[n] > P95[n-1] * 1.5
  → Saturation point reached at stage n-1
```

### Step 1.5: Save Baseline Report
Create baseline report at: `docs/perf-tuning/BASELINE_<name>.md`

```markdown
# Baseline Report: <Endpoint Name>

## Test Configuration
- **Date**: YYYY-MM-DD HH:mm
- **Endpoint**: POST /api/v1/xxx
- **Duration**: 6m 30s
- **Max VUs**: 500

## Results Summary

| Metric | Value |
|--------|-------|
| Peak TPS | XXX req/s |
| Saturation Point | XXX VUs |
| P50 Latency | XXX ms |
| P95 Latency | XXX ms |
| P99 Latency | XXX ms |
| Error Rate | X.XX% |

## Saturation Analysis
- TPS plateaued at XXX VUs
- Latency degradation started at XXX VUs
- Error spike observed at XXX VUs

## Raw Data
- JSON: `backend/scripts/perf-tuning/results/baseline_YYYY-MM-DD.json`
```

---

## Phase 2: Bottleneck Analysis

### Step 2.1: System Resource Monitoring
During load test, collect:

**CPU & Memory**:
```bash
# Linux
vmstat 1 | tee cpu_stats.log
top -b -d 1 | grep java | tee mem_stats.log

# Docker
docker stats --no-stream sentinel-backend
```

**JVM (if applicable)**:
```bash
# GC logs (add to JVM args)
-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10M

# Thread dump
jstack <pid> > thread_dump.txt

# Heap histogram
jmap -histo <pid> | head -30
```

### Step 2.2: Database Analysis
```sql
-- PostgreSQL slow queries
SELECT query, calls, mean_time, total_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;

-- Active connections
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

-- Lock waits
SELECT * FROM pg_locks WHERE NOT granted;
```

**Connection Pool (HikariCP)**:
```bash
# Check via actuator
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending
```

### Step 2.3: Network & External Services
```bash
# Redis latency
redis-cli --latency

# External API response times (check application logs)
grep "external_api" app.log | awk '{print $NF}' | sort -n
```

### Step 2.4: Bottleneck Identification
Common bottleneck patterns:

| Symptom | Likely Cause | Investigation |
|---------|--------------|---------------|
| CPU 100% | Inefficient algorithm, no caching | Profile hot methods |
| High GC pause | Memory leak, large objects | Heap dump analysis |
| DB connection wait | Pool too small | Increase pool size |
| Slow queries | Missing index, N+1 problem | EXPLAIN ANALYZE |
| Thread pool exhaustion | Blocking I/O | Use async/reactive |
| External API slow | No timeout, no retry | Add circuit breaker |

### Step 2.5: Generate Hypothesis
Document in: `docs/perf-tuning/HYPOTHESIS_<exp_id>.md`

```markdown
# Hypothesis: <Experiment ID>

## Observation
- Current TPS: XXX
- Bottleneck: <identified bottleneck>
- Evidence: <supporting data>

## Hypothesis
If we <change>, then TPS will increase by <X%> because <reason>.

## Proposed Change
- Type: [Config | Code | Infrastructure]
- File(s): <file paths>
- Change: <specific change description>

## Expected Outcome
- TPS: +XX%
- P95: -XX ms
- Error Rate: unchanged

## Rollback Plan
<how to revert if experiment fails>
```

---

## Phase 3: Experiment Execution

### Step 3.1: Create Experiment Script
Copy baseline script and modify:
```bash
cp backend/scripts/perf-tuning/exp_<name>_baseline.js \
   backend/scripts/perf-tuning/exp_<name>_<exp_id>.js
```

### Step 3.2: Apply Tuning Change
Document the exact change:
```markdown
## Change Applied
- File: `backend/src/main/resources/application.yml`
- Before: `hikari.maximum-pool-size: 10`
- After: `hikari.maximum-pool-size: 30`
```

### Step 3.3: Run Experiment Test
```bash
# Same conditions as baseline
k6 run backend/scripts/perf-tuning/exp_<name>_<exp_id>.js \
  -e BASE_URL=http://localhost:8080 \
  -e TOKEN="<jwt_token>"
```

### Step 3.4: Compare Results
Create comparison report: `docs/perf-tuning/EXPERIMENT_<exp_id>.md`

```markdown
# Experiment Report: <Exp ID>

## Hypothesis
<copy from hypothesis doc>

## Test Conditions
- Same as baseline (6m30s, ramp to 500 VUs)
- Change applied: <description>

## Results Comparison

| Metric | Baseline | Experiment | Delta | % Change |
|--------|----------|------------|-------|----------|
| Peak TPS | XXX | YYY | +ZZZ | +XX% |
| P50 | XXX ms | YYY ms | -ZZ ms | -XX% |
| P95 | XXX ms | YYY ms | -ZZ ms | -XX% |
| P99 | XXX ms | YYY ms | -ZZ ms | -XX% |
| Error Rate | X.X% | Y.Y% | -Z.Z% | - |

## Conclusion
- [ ] Hypothesis confirmed
- [ ] Hypothesis rejected
- [ ] Inconclusive (need more data)

## Recommendation
- [ ] Deploy to production
- [ ] Run longer test
- [ ] Try different approach
- [ ] Revert change

## Learnings
<what we learned from this experiment>
```

---

## File Structure

```
backend/scripts/perf-tuning/
├── exp_<name>_baseline.js       # Baseline test script
├── exp_<name>_<exp_id>.js       # Experiment test scripts
├── results/
│   ├── baseline_YYYY-MM-DD.json
│   └── exp_<id>_YYYY-MM-DD.json
└── templates/
    └── k6_template.js           # Reusable template

docs/perf-tuning/
├── BASELINE_<name>.md           # Baseline reports
├── HYPOTHESIS_<exp_id>.md       # Experiment hypotheses
└── EXPERIMENT_<exp_id>.md       # Experiment results
```

---

## Quick Commands

```bash
# Run baseline test
k6 run backend/scripts/perf-tuning/exp_baseline.js

# Run with custom VUs
k6 run --vus 100 --duration 30s backend/scripts/perf-tuning/exp_baseline.js

# Run with environment variables
k6 run -e BASE_URL=http://localhost:8080 -e TOKEN="xxx" script.js

# Output to JSON
k6 run --out json=results.json script.js

# Real-time metrics to InfluxDB (if configured)
k6 run --out influxdb=http://localhost:8086/k6 script.js
```

---

## User Interaction Points

### Before Baseline
Ask user:
1. "Which endpoint do you want to test?"
2. "What's the target TPS or SLA?"
3. "Do you have a JWT token for authenticated endpoints?"

### After Baseline
Report:
1. Saturation point and key metrics
2. Identified bottleneck candidates
3. Proposed optimization hypotheses

### Before Experiment
Confirm:
1. "I'll apply <change>. Proceed?"
2. "This may require restart. OK?"

### After Experiment
Report:
1. A/B comparison table
2. Recommendation (deploy/revert/iterate)

---

## Integration with Project

### Existing k6 Scripts
Reference existing scripts in:
- `backend/scripts/phase*/tests/`
- `backend/scripts/common/k6/utils.js`

### Metrics Collection
If Prometheus/Grafana available:
- Add `--out experimental-prometheus-rw` flag
- Dashboard: Grafana k6 dashboard ID 2587

### CI/CD Integration
```yaml
# GitHub Actions example
- name: Run Performance Test
  run: |
    k6 run backend/scripts/perf-tuning/exp_baseline.js
  env:
    BASE_URL: ${{ secrets.TEST_URL }}
    TOKEN: ${{ secrets.TEST_TOKEN }}
```
