/**
 * k6 Baseline Test Template
 *
 * Saturation point discovery through progressive load increase.
 *
 * Usage:
 *   k6 run -e BASE_URL=http://localhost:8080 -e TOKEN="xxx" this_script.js
 *
 * Stages:
 *   1. Warm-up (30s → 50 VUs)
 *   2. Progressive ramp (100 → 500 VUs, 1min each)
 *   3. Ramp-down (30s → 0)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// ============================================================================
// CUSTOM METRICS
// ============================================================================
const errorRate = new Rate('error_rate');
const latencyTrend = new Trend('latency_ms');
const successCount = new Counter('success_count');
const failCount = new Counter('fail_count');

// ============================================================================
// TEST CONFIGURATION
// ============================================================================
export const options = {
  stages: [
    // Phase 1: Warm-up
    { duration: '30s', target: 50 },

    // Phase 2: Progressive load increase (find saturation)
    { duration: '1m', target: 100 },
    { duration: '1m', target: 200 },
    { duration: '1m', target: 300 },
    { duration: '1m', target: 400 },
    { duration: '1m', target: 500 },

    // Phase 3: Ramp-down
    { duration: '30s', target: 0 },
  ],

  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],  // Latency SLA
    error_rate: ['rate<0.01'],                       // Error rate < 1%
    http_req_failed: ['rate<0.01'],                  // HTTP failures < 1%
  },

  // Graceful stop
  gracefulRampDown: '10s',
};

// ============================================================================
// ENVIRONMENT VARIABLES
// ============================================================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';
const ENDPOINT = __ENV.ENDPOINT || '/api/v1/health';  // Override with -e ENDPOINT=/api/xxx

// ============================================================================
// REQUEST HEADERS
// ============================================================================
function getHeaders() {
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  if (TOKEN) {
    headers['Authorization'] = `Bearer ${TOKEN}`;
  }

  return headers;
}

// ============================================================================
// MAIN TEST FUNCTION
// ============================================================================
export default function () {
  const url = `${BASE_URL}${ENDPOINT}`;
  const headers = getHeaders();

  // ========================================
  // MODIFY THIS SECTION FOR YOUR TEST
  // ========================================

  // GET request example
  const res = http.get(url, { headers, tags: { name: 'target_endpoint' } });

  // POST request example (uncomment if needed)
  // const payload = JSON.stringify({
  //   key: 'value',
  //   timestamp: Date.now(),
  // });
  // const res = http.post(url, payload, { headers, tags: { name: 'target_endpoint' } });

  // ========================================
  // END MODIFY SECTION
  // ========================================

  // Record metrics
  latencyTrend.add(res.timings.duration);

  const isSuccess = res.status >= 200 && res.status < 300;
  errorRate.add(!isSuccess);

  if (isSuccess) {
    successCount.add(1);
  } else {
    failCount.add(1);
    if (__ENV.DEBUG) {
      console.log(`❌ Error: ${res.status} - ${res.body?.substring(0, 100)}`);
    }
  }

  // Assertions
  check(res, {
    'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    'response time < 500ms': (r) => r.timings.duration < 500,
    'response time < 1000ms': (r) => r.timings.duration < 1000,
  });

  // Think time (simulates user delay between requests)
  sleep(Math.random() * 0.2 + 0.1);  // 100-300ms
}

// ============================================================================
// LIFECYCLE HOOKS
// ============================================================================
export function setup() {
  console.log('========================================');
  console.log('  BASELINE TEST STARTING');
  console.log('========================================');
  console.log(`Target: ${BASE_URL}${ENDPOINT}`);
  console.log(`Auth: ${TOKEN ? 'Bearer token provided' : 'No auth'}`);
  console.log('----------------------------------------');

  // Verify endpoint is reachable
  const res = http.get(`${BASE_URL}/actuator/health`, { timeout: '5s' });
  if (res.status !== 200) {
    console.warn(`⚠️ Health check failed: ${res.status}`);
  }

  return { startTime: Date.now() };
}

export function teardown(data) {
  const duration = ((Date.now() - data.startTime) / 1000).toFixed(0);
  console.log('----------------------------------------');
  console.log(`  TEST COMPLETED (${duration}s)`);
  console.log('========================================');
}

// ============================================================================
// SUMMARY REPORT
// ============================================================================
export function handleSummary(data) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-').split('T')[0];
  const metrics = data.metrics;

  // Calculate key metrics
  const tps = metrics.http_reqs?.values?.rate || 0;
  const p50 = metrics.http_req_duration?.values?.['p(50)'] || 0;
  const p95 = metrics.http_req_duration?.values?.['p(95)'] || 0;
  const p99 = metrics.http_req_duration?.values?.['p(99)'] || 0;
  const maxLatency = metrics.http_req_duration?.values?.max || 0;
  const errorRate = (metrics.http_req_failed?.values?.rate || 0) * 100;
  const totalRequests = metrics.http_reqs?.values?.count || 0;
  const maxVUs = metrics.vus_max?.values?.max || 0;

  // Text summary
  const summary = `
================================================================================
                         BASELINE TEST RESULTS
================================================================================

  CONFIGURATION
  -------------
  Endpoint:        ${BASE_URL}${ENDPOINT}
  Duration:        ${(data.state.testRunDurationMs / 1000).toFixed(0)}s
  Max VUs:         ${maxVUs}
  Total Requests:  ${totalRequests.toLocaleString()}

  THROUGHPUT
  ----------
  TPS (avg):       ${tps.toFixed(2)} req/s

  LATENCY (ms)
  ------------
  P50:             ${p50.toFixed(2)}
  P95:             ${p95.toFixed(2)}
  P99:             ${p99.toFixed(2)}
  Max:             ${maxLatency.toFixed(2)}

  RELIABILITY
  -----------
  Error Rate:      ${errorRate.toFixed(3)}%
  Success Count:   ${metrics.success_count?.values?.count || 0}
  Fail Count:      ${metrics.fail_count?.values?.count || 0}

  THRESHOLDS
  ----------
  P95 < 500ms:     ${p95 < 500 ? '✅ PASS' : '❌ FAIL'}
  P99 < 1000ms:    ${p99 < 1000 ? '✅ PASS' : '❌ FAIL'}
  Error < 1%:      ${errorRate < 1 ? '✅ PASS' : '❌ FAIL'}

================================================================================
                         SATURATION ANALYSIS
================================================================================

  🎯 Peak TPS:     ${tps.toFixed(2)} req/s at ${maxVUs} VUs

  📊 To find saturation point, compare TPS across stages:
     - If TPS stops growing while VUs increase → saturation reached
     - If P95 spikes while TPS plateaus → system under stress
     - If errors spike → breaking point reached

================================================================================
`;

  // JSON data for later analysis
  const jsonData = {
    timestamp: new Date().toISOString(),
    config: {
      endpoint: `${BASE_URL}${ENDPOINT}`,
      durationMs: data.state.testRunDurationMs,
      maxVUs: maxVUs,
    },
    metrics: {
      tps: tps,
      totalRequests: totalRequests,
      latency: {
        p50: p50,
        p95: p95,
        p99: p99,
        max: maxLatency,
      },
      errorRate: errorRate,
      successCount: metrics.success_count?.values?.count || 0,
      failCount: metrics.fail_count?.values?.count || 0,
    },
    thresholds: {
      p95Under500ms: p95 < 500,
      p99Under1000ms: p99 < 1000,
      errorUnder1pct: errorRate < 1,
    },
    raw: data,
  };

  return {
    'stdout': summary,
    [`backend/scripts/perf-tuning/results/baseline_${timestamp}.json`]: JSON.stringify(jsonData, null, 2),
  };
}
