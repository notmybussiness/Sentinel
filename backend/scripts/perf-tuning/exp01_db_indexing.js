/**
 * EXP-01: DB Indexing Performance Test
 *
 * Target Tables:
 *   - portfolios (user_id)
 *   - user_sessions (user_id, access_token_hash, expires_at)
 *
 * Test Scenario:
 *   - 60% Portfolio List (portfolios.user_id)
 *   - 20% Portfolio Get (portfolios.id + holdings)
 *   - 20% Auth-heavy requests (user_sessions 조회)
 *
 * Usage:
 *   k6 run -e BASE_URL=http://localhost:8080 exp01_db_indexing.js
 *
 * Expected Results:
 *   - Before Index: TPS ~217, P95 ~149ms
 *   - After Index: TPS 300-400, P95 80-120ms
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate, Trend, Counter } from 'k6/metrics';

// ============================================================================
// CONFIGURATION
// ============================================================================
// Environment:
//   DB Server:     PostgreSQL (separate machine)
//   WAS Server:    Spring Boot + RAG + Monitoring (Grafana, InfluxDB, Prometheus)
//   Client Server: k6 (this script)
//
// Usage:
//   k6 run -e WAS_URL=http://192.168.x.x:8080 -e INFLUX_URL=http://192.168.x.x:8086 exp01_db_indexing.js
//
const BASE_URL = __ENV.WAS_URL || __ENV.BASE_URL || 'http://localhost:8080';
const INFLUX_URL = __ENV.INFLUX_URL || 'http://localhost:8086';
const TOKENS_FILE = __ENV.TOKENS_FILE || './tokens.csv';
const TEST_DURATION = __ENV.DURATION || '3m';

// ============================================================================
// CUSTOM METRICS
// ============================================================================
const errorRate = new Rate('error_rate');
const portfolioListTrend = new Trend('portfolio_list_ms');
const portfolioGetTrend = new Trend('portfolio_get_ms');
const authCheckTrend = new Trend('auth_check_ms');
const successCount = new Counter('success_count');
const failCount = new Counter('fail_count');

// ============================================================================
// LOAD TEST USERS
// ============================================================================
let tokens;
try {
  tokens = new SharedArray('tokens', function () {
    const data = open(TOKENS_FILE).split('\n').slice(1);
    return data.map(line => {
      const parts = line.split(',');
      return {
        userId: parts[0],
        accessToken: parts[3] ? parts[3].trim() : '',
        refreshToken: parts[4] ? parts[4].trim() : ''
      };
    }).filter(t => t.accessToken);
  });
} catch (e) {
  console.warn('tokens.csv not found. Using env TOKEN instead.');
  tokens = null;
}

// ============================================================================
// TEST CONFIGURATION
// ============================================================================
export const options = {
  scenarios: {
    db_indexing_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        // Warm-up
        { duration: '30s', target: 100 },
        // Ramp to load
        { duration: '30s', target: 300 },
        // Sustain
        { duration: TEST_DURATION, target: 300 },
        // Peak
        { duration: '1m', target: 500 },
        // Ramp down
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<500'],
    'portfolio_list_ms': ['p(95)<200', 'avg<100'],
    'portfolio_get_ms': ['p(95)<200', 'avg<100'],
    'auth_check_ms': ['p(95)<50', 'avg<20'],
    'error_rate': ['rate<0.01'],
  },
};

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================
function getToken() {
  if (tokens && tokens.length > 0) {
    const user = tokens[Math.floor(Math.random() * tokens.length)];
    return user.accessToken;
  }
  return __ENV.TOKEN || '';
}

function getHeaders(token) {
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };
}

// ============================================================================
// API OPERATIONS
// ============================================================================

/**
 * Portfolio List - Tests: portfolios.user_id index
 */
function testPortfolioList(token) {
  const url = `${BASE_URL}/api/v1/portfolios`;
  const res = http.get(url, {
    headers: getHeaders(token),
    tags: { name: 'portfolio_list' }
  });

  portfolioListTrend.add(res.timings.duration);

  const success = res.status === 200;
  errorRate.add(!success);
  success ? successCount.add(1) : failCount.add(1);

  check(res, {
    'portfolio list: status 200': (r) => r.status === 200,
    'portfolio list: < 200ms': (r) => r.timings.duration < 200,
  });

  return success ? res.json() : [];
}

/**
 * Portfolio Get - Tests: portfolios.id + portfolio_holdings.portfolio_id
 */
function testPortfolioGet(token, portfolioId) {
  const url = `${BASE_URL}/api/v1/portfolios/${portfolioId}`;
  const res = http.get(url, {
    headers: getHeaders(token),
    tags: { name: 'portfolio_get' }
  });

  portfolioGetTrend.add(res.timings.duration);

  const success = res.status === 200;
  errorRate.add(!success);
  success ? successCount.add(1) : failCount.add(1);

  check(res, {
    'portfolio get: status 200': (r) => r.status === 200,
    'portfolio get: < 200ms': (r) => r.timings.duration < 200,
  });
}

/**
 * Auth Check - Tests: user_sessions.access_token_hash index
 * Every authenticated request checks the session table
 */
function testAuthCheck(token) {
  // /api/v1/auth/me forces session lookup
  const url = `${BASE_URL}/api/v1/auth/me`;
  const res = http.get(url, {
    headers: getHeaders(token),
    tags: { name: 'auth_check' }
  });

  authCheckTrend.add(res.timings.duration);

  const success = res.status === 200;
  errorRate.add(!success);
  success ? successCount.add(1) : failCount.add(1);

  check(res, {
    'auth check: status 200': (r) => r.status === 200,
    'auth check: < 50ms': (r) => r.timings.duration < 50,
  });
}

// ============================================================================
// MAIN TEST FUNCTION
// ============================================================================
export default function () {
  const token = getToken();
  if (!token) {
    console.error('No token available');
    return;
  }

  const operation = Math.random();

  // 60%: Portfolio List (user_id index)
  if (operation < 0.60) {
    const portfolios = testPortfolioList(token);

    // Follow up with a get if we have portfolios
    if (portfolios.length > 0) {
      const pid = portfolios[Math.floor(Math.random() * portfolios.length)].id;
      testPortfolioGet(token, pid);
    }
  }
  // 20%: Portfolio Get directly
  else if (operation < 0.80) {
    const portfolios = testPortfolioList(token);
    if (portfolios.length > 0) {
      const pid = portfolios[Math.floor(Math.random() * portfolios.length)].id;
      testPortfolioGet(token, pid);
      testPortfolioGet(token, pid); // Repeat to test cache vs DB
    }
  }
  // 20%: Auth-heavy (session table stress)
  else {
    testAuthCheck(token);
    testAuthCheck(token);
    testAuthCheck(token);
  }

  sleep(0.05 + Math.random() * 0.1); // 50-150ms think time
}

// ============================================================================
// LIFECYCLE
// ============================================================================
export function setup() {
  console.log('╔════════════════════════════════════════════════════════════════╗');
  console.log('║  EXP-01: DB INDEXING PERFORMANCE TEST                          ║');
  console.log('╠════════════════════════════════════════════════════════════════╣');
  console.log(`║  Target:     ${BASE_URL.padEnd(47)}║`);
  console.log(`║  Tokens:     ${tokens ? tokens.length + ' users' : 'ENV TOKEN'} `.padEnd(66) + '║');
  console.log('╚════════════════════════════════════════════════════════════════╝');

  // Health check
  const res = http.get(`${BASE_URL}/actuator/health`, { timeout: '10s' });
  if (res.status !== 200) {
    throw new Error(`Health check failed: ${res.status}`);
  }

  console.log('Health check passed');
  return { startTime: Date.now() };
}

export function teardown(data) {
  const duration = ((Date.now() - data.startTime) / 1000).toFixed(0);
  console.log('');
  console.log(`Test completed in ${duration}s`);
}

// ============================================================================
// SUMMARY REPORT
// ============================================================================
export function handleSummary(data) {
  const m = data.metrics;
  const timestamp = new Date().toISOString().split('T')[0];

  const tps = m.http_reqs?.values?.rate || 0;
  const p50 = m.http_req_duration?.values?.['p(50)'] || 0;
  const p95 = m.http_req_duration?.values?.['p(95)'] || 0;
  const p99 = m.http_req_duration?.values?.['p(99)'] || 0;
  const errRate = (m.http_req_failed?.values?.rate || 0) * 100;

  const portfolioListP95 = m.portfolio_list_ms?.values?.['p(95)'] || 0;
  const portfolioGetP95 = m.portfolio_get_ms?.values?.['p(95)'] || 0;
  const authCheckP95 = m.auth_check_ms?.values?.['p(95)'] || 0;

  const summary = `
╔════════════════════════════════════════════════════════════════╗
║             EXP-01: DB INDEXING TEST RESULTS                   ║
╠════════════════════════════════════════════════════════════════╣
║                                                                ║
║  THROUGHPUT                                                    ║
║  ──────────                                                    ║
║  TPS:              ${tps.toFixed(2).padStart(10)} req/s                        ║
║  Total Requests:   ${(m.http_reqs?.values?.count || 0).toString().padStart(10)}                              ║
║                                                                ║
║  LATENCY (overall)                                             ║
║  ──────────                                                    ║
║  P50:              ${p50.toFixed(2).padStart(10)} ms                           ║
║  P95:              ${p95.toFixed(2).padStart(10)} ms                           ║
║  P99:              ${p99.toFixed(2).padStart(10)} ms                           ║
║                                                                ║
║  BY ENDPOINT (P95)                                             ║
║  ──────────                                                    ║
║  Portfolio List:   ${portfolioListP95.toFixed(2).padStart(10)} ms  (idx: user_id)           ║
║  Portfolio Get:    ${portfolioGetP95.toFixed(2).padStart(10)} ms  (idx: id)                ║
║  Auth Check:       ${authCheckP95.toFixed(2).padStart(10)} ms  (idx: access_token_hash)  ║
║                                                                ║
║  RELIABILITY                                                   ║
║  ──────────                                                    ║
║  Error Rate:       ${errRate.toFixed(3).padStart(10)} %                            ║
║  Success:          ${(m.success_count?.values?.count || 0).toString().padStart(10)}                              ║
║  Failures:         ${(m.fail_count?.values?.count || 0).toString().padStart(10)}                              ║
║                                                                ║
╚════════════════════════════════════════════════════════════════╝

Copy this for PLAN_PERF_1000TPS.md:
| TPS | ${tps.toFixed(2)} |
| P95 | ${p95.toFixed(2)}ms |
| P99 | ${p99.toFixed(2)}ms |
| Error Rate | ${errRate.toFixed(3)}% |
`;

  const jsonData = {
    experiment: 'EXP-01',
    name: 'DB Indexing',
    timestamp: new Date().toISOString(),
    metrics: {
      tps,
      totalRequests: m.http_reqs?.values?.count || 0,
      latency: { p50, p95, p99 },
      byEndpoint: {
        portfolioList: { p95: portfolioListP95 },
        portfolioGet: { p95: portfolioGetP95 },
        authCheck: { p95: authCheckP95 },
      },
      errorRate: errRate,
    },
  };

  return {
    'stdout': summary,
    [`backend/scripts/perf-tuning/results/exp01_${timestamp}.json`]: JSON.stringify(jsonData, null, 2),
  };
}
