/**
 * Experiment 9 - Local Caching (Caffeine) Verification
 *
 * Phase 6: Scalability Optimization
 * 목표: Local Cache 적용 후 Read API 성능 및 DB 부하 감소 확인
 *
 * Test Scenario (Mixed Workload):
 *   - 10%: Portfolio 생성 (Write - Cache Evict)
 *   - 20%: Holding 추가 (Write - Cache Evict)
 *   - 50%: Portfolio 조회 (Read - Cached!)
 *   - 20%: Portfolio 목록 (Read - Uncached or Cached depending on impl)
 *
 * Expectation:
 *   - getPortfolio 응답시간: < 1ms (Cache Hit)
 *   - DB Connection Pool: 여유로움
 *   - TPS: 대폭 상승
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate, Trend, Counter } from 'k6/metrics';

// Configuration
const BASE_URL = 'http://localhost:8080';
const TOKENS_FILE = '../../common/tokens.csv'; // Adjust path if needed

// Custom Metrics
const errorRate = new Rate('errors');
const portfolioGetTrend = new Trend('portfolio_get_duration');
const cacheHitRate = new Rate('cache_hits'); // 추정치 (응답시간 기반)

// Load tokens from CSV
const tokens = new SharedArray('tokens', function () {
    const data = open(TOKENS_FILE).split('\n').slice(1);
    return data.map(line => {
        const [userId, email, nickname, accessToken, refreshToken] = line.split(',');
        return { userId, accessToken: accessToken ? accessToken.trim() : '' };
    }).filter(t => t.accessToken);
});

export const options = {
    stages: [
        { duration: '30s', target: 500 }, // Fast Ramp-up
        { duration: '3m', target: 500 },  // 3분 유지 (충분함)
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        'http_req_duration': ['p(95)<100'], // 목표: 100ms 이하 (기존 500ms)
        'errors': ['rate<0.01'],
    },
};

function getHeaders(token) {
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

function getRandomElement(array) {
    return array[Math.floor(Math.random() * array.length)];
}

// API Operations
function getPortfolioList(token) {
    const res = http.get(`${BASE_URL}/api/v1/portfolios`, { headers: getHeaders(token) });
    check(res, { 'list retrieved': (r) => r.status === 200 });
    return res.status === 200 ? res.json() : [];
}

function getPortfolio(token, portfolioId) {
    const res = http.get(`${BASE_URL}/api/v1/portfolios/${portfolioId}`, { headers: getHeaders(token) });

    const success = check(res, { 'portfolio retrieved': (r) => r.status === 200 });
    errorRate.add(!success);
    portfolioGetTrend.add(res.timings.duration);

    // Cache Hit 추정: 10ms 미만이면 캐시 히트로 간주
    if (success) {
        cacheHitRate.add(res.timings.duration < 10);
    }
}

function createPortfolio(token) {
    const payload = JSON.stringify({
        name: `CacheTest ${Date.now()}`,
        description: 'Testing Cache Eviction'
    });
    http.post(`${BASE_URL}/api/v1/portfolios`, payload, { headers: getHeaders(token) });
}

export default function () {
    const user = getRandomElement(tokens);
    const operation = Math.random();

    if (operation < 0.10) {
        // 10%: Write (Create)
        createPortfolio(user.accessToken);
    } else if (operation < 0.30) {
        // 20%: Write (Add Holding - 생략, Create로 대체하여 단순화)
        createPortfolio(user.accessToken);
    } else {
        // 70%: Read (Get Portfolio) - Main Target
        // 먼저 목록을 가져와서 ID 획득
        const portfolios = getPortfolioList(user.accessToken);
        if (portfolios.length > 0) {
            const pid = getRandomElement(portfolios).id;
            // 반복 조회로 캐시 효과 확인
            getPortfolio(user.accessToken, pid);
            getPortfolio(user.accessToken, pid); // Cache Hit 예상
        }
    }

    sleep(0.1);
}
