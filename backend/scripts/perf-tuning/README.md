# EXP-01: DB Indexing Performance Test

## Architecture

```
┌─────────────────┐     ┌─────────────────────────────┐     ┌─────────────┐
│   DB Server     │     │        WAS Server           │     │   Client    │
│  192.168.0.5    │     │      192.168.0.58           │     │   (macOS)   │
│                 │     │                             │     │             │
│   PostgreSQL    │◄───►│  Spring Boot + Python RAG   │◄────│     k6      │
│    (:5432)      │     │       (:8080)               │     │             │
│                 │     │                             │     │             │
│                 │     │  + Grafana    (:3001)       │     │             │
│                 │     │  + InfluxDB   (:8086) ◄─────┼─────┼── metrics  │
│                 │     │  + Prometheus (:9090)       │     │             │
└─────────────────┘     └─────────────────────────────┘     └─────────────┘
```

---

## Quick Start

### 1. WAS Server (192.168.0.58): Start Monitoring

```powershell
cd backend/scripts/perf-tuning/was
.\start_monitoring.ps1
```

### 2. Client (macOS): Run k6

```bash
cd backend/scripts/perf-tuning/client
chmod +x run_k6.sh

# Baseline (인덱스 적용 전)
./run_k6.sh baseline

# After (인덱스 적용 후)
./run_k6.sh after
```

### 3. Compare Results

Grafana: http://192.168.0.58:3001/d/exp01-db-indexing

---

## Files

```
perf-tuning/
├── exp01_db_indexing.js          # k6 test script
├── generate_tokens.py            # Token generator
├── docker-compose.monitoring.yml # Monitoring stack
│
├── was/                          # WAS (Windows)
│   └── start_monitoring.ps1
│
├── client/                       # Client (macOS)
│   └── run_k6.sh
│
├── prometheus/
│   └── prometheus.yml
├── grafana/
│   └── dashboards/
│       └── exp01-db-indexing.json
└── results/
```

---

## Grafana Metrics

### k6 Metrics (InfluxDB)

| Panel | Metric | Target |
|-------|--------|--------|
| TPS | `http_reqs` rate | > 1000 req/s |
| P95 Latency | `http_req_duration` p95 | < 100ms |
| Error Rate | `error_rate` | < 1% |
| Portfolio List | `portfolio_list_ms` p95 | < 200ms |
| Portfolio Get | `portfolio_get_ms` p95 | < 200ms |
| Auth Check | `auth_check_ms` p95 | < 50ms |

### Spring Boot Metrics (Prometheus)

| Panel | Metric | Watch For |
|-------|--------|-----------|
| DB Connections | `hikaricp_connections_active` | Spikes during load |
| Connection Wait | `hikaricp_connections_pending` | Should be 0 |
| Acquire Time | `hikaricp_connections_acquire_seconds` | > 100ms = bottleneck |
| Heap Memory | `jvm_memory_used_bytes` | < 80% of max |
| GC Pause | `jvm_gc_pause_seconds` | < 200ms |

---

## Manual k6 Commands (macOS)

```bash
# Direct run (no monitoring)
k6 run -e WAS_URL=http://192.168.0.58:8080 exp01_db_indexing.js

# With InfluxDB output
k6 run \
  --out influxdb=http://192.168.0.58:8086/k6 \
  -e WAS_URL=http://192.168.0.58:8080 \
  --tag phase=baseline \
  exp01_db_indexing.js
```

---

## Index SQL (Apply after baseline)

```bash
# DB Server (192.168.0.5)
psql -U postgres -d sentinel
```

```sql
-- Portfolio
CREATE INDEX CONCURRENTLY idx_portfolios_user_id ON portfolios(user_id);

-- UserSession
CREATE INDEX CONCURRENTLY idx_user_sessions_access_token_hash
ON user_sessions(access_token_hash) WHERE is_active = true;

CREATE INDEX CONCURRENTLY idx_user_sessions_user_id ON user_sessions(user_id);
```

Full SQL: `docs/plans/PLAN_PERF_1000TPS.md`
