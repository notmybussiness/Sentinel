import http from 'k6/http';
import { check, sleep, fail } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:4010';
const REQUIRE_LOCAL_MOCK = (__ENV.REQUIRE_LOCAL_MOCK || 'true').toLowerCase() === 'true';

if (REQUIRE_LOCAL_MOCK && !BASE_URL.includes('localhost') && !BASE_URL.includes('127.0.0.1')) {
  fail(`BASE_URL must point to local Prism mock server. Received: ${BASE_URL}`);
}

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
    },
    load: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '1m', target: 5 },
        { duration: '2m', target: 10 },
        { duration: '1m', target: 0 },
      ],
      startTime: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

function hitMarketEndpoints() {
  const marketStatus = http.get(`${BASE_URL}/api/v1/market/status`);
  check(marketStatus, {
    'market/status returns 200': (r) => r.status === 200,
  });

  const marketQuery = http.get(`${BASE_URL}/api/v1/market/prices?symbols=AAPL,MSFT`);
  check(marketQuery, {
    'market/prices query returns 200': (r) => r.status === 200,
  });

  const marketBatch = http.post(
    `${BASE_URL}/api/v1/market/prices`,
    JSON.stringify(['AAPL', 'MSFT', 'GOOGL']),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(marketBatch, {
    'market/prices batch returns 200': (r) => r.status === 200,
  });

  const marketRefresh = http.post(`${BASE_URL}/api/v1/market/price/AAPL/refresh`);
  check(marketRefresh, {
    'market refresh returns 200': (r) => r.status === 200,
  });
}

function hitAuthEndpoints() {
  const kakaoLoginUrl = http.get(`${BASE_URL}/api/v1/auth/kakao`);
  check(kakaoLoginUrl, {
    'auth/kakao returns 200': (r) => r.status === 200,
  });

  const devLogin = http.post(`${BASE_URL}/api/v1/auth/dev-login`);
  check(devLogin, {
    'auth/dev-login returns 200': (r) => r.status === 200,
  });
}

function hitPortfolioEndpoints() {
  const portfolios = http.get(`${BASE_URL}/api/v1/portfolios`);
  check(portfolios, {
    'portfolios returns 200 or 401': (r) => r.status === 200 || r.status === 401,
  });
}

export default function () {
  hitMarketEndpoints();
  hitAuthEndpoints();
  hitPortfolioEndpoints();
  sleep(1);
}
