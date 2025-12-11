# Phase 5 Database Failure Analysis Summary

**분석 시각**: 2025-11-26T14:30:38.381174
**장애 시각**: 2025-11-26T13:56:00+09:00 ~ 2025-11-26T14:00:00+09:00

---

## 🚨 Critical Issues: 6건

| Metric | Status | Details |
|--------|--------|--------|
| hikaricp_active | ❌ CRITICAL | ⚠️ 50개 중 45개 이상이면 Pool 고갈 위험 (Max: 50.00) |
| hikaricp_idle | ❌ CRITICAL | ⚠️ 5개 이하면 Pool 부족 (Min: 0.00) |
| hikaricp_pending | ❌ CRITICAL | ⚠️ 10개 이상이면 심각한 병목 (Max: 428.00) |
| hikaricp_usage_percent | ❌ CRITICAL | ⚠️ 90% 이상이면 위험 (Max: 100.00) |
| http_success_rate | ❌ CRITICAL | ⚠️ 급락하면 서비스 불가 (Min: 0.00) |
| db_query_rate | ❌ CRITICAL | ⚠️ 급락하면 Connection Pool 고갈 (Min: 0.00) |

---

## 📊 All Metrics Summary

| Metric | Avg | Max | Min | Status |
|--------|-----|-----|-----|--------|
| hikaricp_active | 40.51 | 50.00 | 0.00 | ❌ |
| hikaricp_idle | 1.84 | 10.00 | 0.00 | ❌ |
| hikaricp_pending | 291.78 | 428.00 | 0.00 | ❌ |
| hikaricp_timeout | 0.00 | 0.00 | 0.00 | ✅ |
| hikaricp_usage_percent | 81.02 | 100.00 | 0.00 | ❌ |
| http_success_rate | 23.62 | 180.76 | 0.00 | ❌ |
| jvm_threads_live | 83.96 | 91.00 | 57.00 | ✅ |
| jvm_threads_peak | 85.02 | 92.00 | 60.00 | ✅ |
| jvm_threads_waiting | 34.14 | 41.00 | 9.00 | ✅ |
| jvm_memory_used_heap | 134606070.48 | 352196384.00 | 4194304.00 | ✅ |
| jvm_memory_usage_percent | -4776898142.18 | 8.28 | -16357785600.00 | ✅ |
| jvm_gc_pause_rate | 0.58 | 2.09 | 0.00 | ✅ |
| system_cpu_usage | 26.94 | 34.06 | 2.89 | ✅ |
| process_cpu_usage | 21.24 | 28.80 | 0.26 | ✅ |
| db_query_rate | 43.43 | 180.69 | 0.00 | ❌ |

---

## 🔍 Root Cause Analysis

### ✅ Connection Pool Exhaustion (Confirmed)
- HikariCP 관련 메트릭에서 2개 이상 이상 징후 발견
- **원인**: Virtual Thread (500 VUs) vs. HikariCP Pool (50개) 불균형

