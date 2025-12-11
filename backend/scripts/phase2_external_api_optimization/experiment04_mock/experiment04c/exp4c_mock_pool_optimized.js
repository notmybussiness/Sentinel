/**
 * Experiment 4c: Mock API with Optimized Connection Pool
 *
 * 목적: Connection Pool 최적화 후 성능 측정
 * 변경:
 *   - maxConnTotal: 200 → 500
 *   - maxConnPerRoute: 50 → 200 (Tomcat max-threads와 1:1)
 *   - connectionRequestTimeout: 3s → 1s (Fail Fast)
 * 설정: Mock API, 캐시 ON, sync=true 유지
 * 부하: 0 → 500 VUser (Step-up)
 *
 * 실행:
 *   k6 run --out influxdb=http://192.168.0.5:8086/k6 exp4c_mock_pool_optimized.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// Custom Metrics
const errorRate = new Rate('errors');
const error500Count = new Counter('error_500');
const responseTimeTrend = new Trend('custom_response_time');

// 요청 타입별 메트릭
const marketRequests = new Counter('market_requests');
const cryptoRequests = new Counter('crypto_requests');
const searchRequests = new Counter('search_requests');

// JWT 토큰 로드 (500명 독립 유저)
const tokens = new SharedArray('tokens', function() {
  const csvData = open('./jmeter_tokens.csv');
  const lines = csvData.split('\n').slice(1);

  return lines
    .filter(line => line.trim())
    .map(line => {
      const parts = line.split(',');
      return {
        userId: parts[0],
        email: parts[1],
        nickname: parts[2],
        accessToken: parts[3],
        refreshToken: parts[4],
      };
    });
});

// Test Configuration
export const options = {
  stages: [
    { duration: '1m', target: 100 },  // 0 → 100
    { duration: '1m', target: 200 },  // 100 → 200
    { duration: '1m', target: 300 },  // 200 → 300
    { duration: '1m', target: 400 },  // 300 → 400
    { duration: '1m', target: 500 },  // 400 → 500
    { duration: '2m', target: 500 },  // Steady: 500 VUsers
    { duration: '1m', target: 0 },    // Ramp down
  ],

  thresholds: {
    'errors': ['rate<0.05'],              // 에러율 < 5%
    'error_500': ['count<100'],           // 500 에러 < 100건
    'http_req_duration': ['p(95)<3000'],  // P95 < 3초
  },

  tags: {
    experiment: 'experiment04c',
    phase: 'mock_pool_optimized',
    api: 'mock',
  },
};

const BASE_URL = 'http://192.168.0.58:8080';

// 테스트 종목 (Mock API)
const STOCK_SYMBOLS = ['005930', '000660', '035720', '035420', '051910'];

// 테스트 암호화폐 (Upbit 실제 API)
const CRYPTO_SYMBOLS = ['BTC', 'ETH', 'XRP', 'SOL', 'ADA'];

export function setup() {
  console.log('='.repeat(60));
  console.log('Experiment 4c: Mock API with Optimized Connection Pool');
  console.log('='.repeat(60));
  console.log('Target: ' + BASE_URL);
  console.log('VUsers: 0 → 500 (Step-up)');
  console.log('Duration: 8 minutes');
  console.log('Tokens loaded: ' + tokens.length);
  console.log('');
  console.log('Changes from Baseline:');
  console.log('  - maxConnTotal: 200 → 500');
  console.log('  - maxConnPerRoute: 50 → 200 (Tomcat 1:1)');
  console.log('  - connectionRequestTimeout: 3s → 1s (Fail Fast)');
  console.log('='.repeat(60));

  // Health Check
  const healthResponse = http.get(BASE_URL + '/actuator/health', { timeout: '5s' });
  if (healthResponse.status !== 200) {
    throw new Error('WAS not healthy: ' + healthResponse.status);
  }
  console.log('WAS Health Check: OK');
  console.log('');
}

export default function() {
  const token = tokens[__VU % tokens.length];

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token.accessToken,
  };

  // 요청 타입 분배: Market 60%, Crypto 30%, Search 10%
  const rand = Math.random();

  if (rand < 0.6) {
    testMarketPrice(headers);
  } else if (rand < 0.9) {
    testCryptoPrice(headers);
  } else {
    testSearch(headers);
  }

  // Think time: 0.5 ~ 1.5초
  sleep(Math.random() + 0.5);
}

function testMarketPrice(headers) {
  const symbol = STOCK_SYMBOLS[Math.floor(Math.random() * STOCK_SYMBOLS.length)];

  const response = http.get(
    BASE_URL + '/api/v1/market/price/' + symbol,
    { headers, tags: { name: 'market_price' } }
  );

  marketRequests.add(1);
  responseTimeTrend.add(response.timings.duration);

  const success = check(response, {
    'market: status 200': (r) => r.status === 200,
    'market: response < 3s': (r) => r.timings.duration < 3000,
    'market: has body': (r) => r.body && r.body.length > 0,
  });

  if (!success) {
    errorRate.add(1);
    if (response.status >= 500) {
      error500Count.add(1);
    }
  } else {
    errorRate.add(0);
  }
}

function testCryptoPrice(headers) {
  const symbol = CRYPTO_SYMBOLS[Math.floor(Math.random() * CRYPTO_SYMBOLS.length)];

  const response = http.get(
    BASE_URL + '/api/v1/crypto/price/' + symbol + '?baseCurrency=KRW',
    { headers, tags: { name: 'crypto_price' } }
  );

  cryptoRequests.add(1);
  responseTimeTrend.add(response.timings.duration);

  const success = check(response, {
    'crypto: status 200': (r) => r.status === 200,
    'crypto: response < 3s': (r) => r.timings.duration < 3000,
    'crypto: has body': (r) => r.body && r.body.length > 0,
  });

  if (!success) {
    errorRate.add(1);
    if (response.status >= 500) {
      error500Count.add(1);
    }
  } else {
    errorRate.add(0);
  }
}

function testSearch(headers) {
  const queries = ['삼성', 'NAVER', '카카오', 'SK'];
  const query = queries[Math.floor(Math.random() * queries.length)];

  const response = http.get(
    BASE_URL + '/api/v1/market/search?query=' + encodeURIComponent(query),
    { headers, tags: { name: 'search' } }
  );

  searchRequests.add(1);
  responseTimeTrend.add(response.timings.duration);

  const success = check(response, {
    'search: status 200': (r) => r.status === 200,
    'search: response < 3s': (r) => r.timings.duration < 3000,
  });

  if (!success) {
    errorRate.add(1);
    if (response.status >= 500) {
      error500Count.add(1);
    }
  } else {
    errorRate.add(0);
  }
}

export function teardown(data) {
  console.log('');
  console.log('Experiment 4c Pool Optimized Test Completed');
  console.log('Check Grafana for real-time analysis');
}

export function handleSummary(data) {
  const errorPercent = (data.metrics.errors.values.rate * 100).toFixed(2);
  const error500 = data.metrics.error_500 ? data.metrics.error_500.values.count : 0;
  const avgDuration = data.metrics.http_req_duration.values.avg.toFixed(2);
  const p95Duration = data.metrics.http_req_duration.values['p(95)'].toFixed(2);
  const totalRequests = data.metrics.http_reqs.values.count;
  const rps = data.metrics.http_reqs.values.rate.toFixed(2);
  const successRate = (100 - parseFloat(errorPercent)).toFixed(2);

  const marketReqs = data.metrics.market_requests ? data.metrics.market_requests.values.count : 0;
  const cryptoReqs = data.metrics.crypto_requests ? data.metrics.crypto_requests.values.count : 0;
  const searchReqs = data.metrics.search_requests ? data.metrics.search_requests.values.count : 0;

  console.log('');
  console.log('='.repeat(60));
  console.log('Experiment 4c: Optimized Connection Pool - Summary');
  console.log('='.repeat(60));
  console.log('Total Requests:      ' + totalRequests);
  console.log('  - Market:          ' + marketReqs);
  console.log('  - Crypto:          ' + cryptoReqs);
  console.log('  - Search:          ' + searchReqs);
  console.log('');
  console.log('Success Rate:        ' + successRate + '%');
  console.log('Error Rate:          ' + errorPercent + '%');
  console.log('500 Errors:          ' + error500);
  console.log('Average TPS:         ' + rps + ' req/s');
  console.log('');
  console.log('Response Time:');
  console.log('  Average:           ' + avgDuration + ' ms');
  console.log('  P95:               ' + p95Duration + ' ms');
  console.log('='.repeat(60));

  return {
    'stdout': textSummary(data),
    './exp4c_mock_pool_optimized_summary.json': JSON.stringify(data, null, 2),
  };
}

function textSummary(data) {
  const successRate = ((1 - data.metrics.errors.values.rate) * 100).toFixed(2);
  const avgTime = data.metrics.http_req_duration.values.avg.toFixed(2);
  const p95Time = data.metrics.http_req_duration.values['p(95)'].toFixed(2);
  const tps = data.metrics.http_reqs.values.rate.toFixed(2);

  return '\n' +
    'Experiment 4c: Optimized Connection Pool\n' +
    '=========================================\n' +
    'Total Requests: ' + data.metrics.http_reqs.values.count + '\n' +
    'Success Rate: ' + successRate + '%\n' +
    'Avg Response Time: ' + avgTime + 'ms\n' +
    'P95: ' + p95Time + 'ms\n' +
    'TPS: ' + tps + ' req/s\n';
}
