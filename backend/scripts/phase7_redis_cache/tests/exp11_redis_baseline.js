/**
 * Experiment 11 - Redis Cache Baseline
 *
 * Phase 7: Redis Cache Integration
 * 목표: Redis 분산 캐시 적용 후 성능 측정 및 Cache Hit Rate 확인
 *
 * Test Scenario (Read-Heavy Workload):
 *   - 10%: Portfolio 생성 (Write - Cache Evict)
 *   - 10%: Holding 추가 (Write - Cache Evict)
 *   - 60%: Portfolio 조회 (Read - Cached!)
 *   - 20%: Portfolio 목록 (Read)
 *
 * Expectation:
 *   - Portfolio 조회 응답시간: < 5ms (Cache Hit, Redis Latency 포함)
 *   - Cache Hit Rate: > 80%
 *   - TPS: > 400 req/s
 *   - Error Rate: < 1%
 *   - DB Connection Pool: 여유로움 (캐시로 인한 DB 부하 감소)
 *
 * ✅ PRE-TEST CHECKLIST:
 *   [ ] 1. Docker 컨테이너 실행 확인
 *       docker ps | grep postgres
 *       docker ps | grep redis
 *
 *   [ ] 2. Redis 연결 확인
 *       redis-cli ping
 *       (응답: PONG)
 *
 *   [ ] 3. Redis 캐시 초기화 (선택적 - 깨끗한 상태에서 시작)
 *       redis-cli FLUSHALL
 *
 *   [ ] 4. Backend 실행 (perf 프로파일 필수!)
 *       cd backend
 *       ./gradlew bootRun --args='--spring.profiles.active=perf'
 *       (확인: 로그에서 "Redis 캐시 매니저 초기화" 메시지)
 *
 *   [ ] 5. perf 프로파일 확인
 *       curl http://localhost:8080/actuator/health | jq '.components.redis'
 *       (응답: "status": "UP")
 *
 *   [ ] 6. 토큰 파일 생성 (최초 1회)
 *       cd scripts
 *       python common/generate_tokens.py
 *       ls -la common/tokens.csv
 *
 * 📊 POST-TEST CHECKLIST:
 *   [ ] 1. k6 결과 저장 확인
 *       ls -la ../results/exp11_summary.json
 *
 *   [ ] 2. 핵심 메트릭 확인
 *       cat ../results/exp11_summary.json | jq '.metrics | {
 *         TPS: .http_reqs.rate,
 *         P95: .http_req_duration["p(95)"],
 *         ErrorRate: .errors.rate,
 *         CacheHitRate: .cache_hits.rate
 *       }'
 *
 *   [ ] 3. Redis 메트릭 확인
 *       redis-cli INFO memory | grep used_memory_human
 *       redis-cli DBSIZE
 *       redis-cli KEYS "sentinel:portfolios::*" | wc -l
 *
 *   [ ] 4. Redis TTL 확인 (portfolios 캐시는 5초)
 *       redis-cli KEYS "sentinel:portfolios::*" | head -1 | xargs redis-cli TTL
 *       (응답: 0~5 사이 값)
 *
 *   [ ] 5. Prometheus Metrics 확인 (HikariCP)
 *       curl http://localhost:8080/actuator/prometheus | grep hikaricp_connections_active
 *       curl http://localhost:8080/actuator/prometheus | grep hikaricp_connections_pending
 *
 *   [ ] 6. ANALYSIS.md 업데이트
 *       - Before (Phase 6 - Local Cache): TPS, P95, Cache Hit Rate
 *       - After (Phase 7 - Redis): TPS, P95, Cache Hit Rate
 *       - Redis Latency Impact (예상: 0.5-2ms)
 *       - Multi-Instance 준비 상태
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate, Trend, Counter } from 'k6/metrics';

// ========================================
// Configuration
// ========================================
const BASE_URL = 'http://192.168.0.58:8080';
const TOKENS_FILE = './tokens.csv';

// ========================================
// Custom Metrics
// ========================================
const errorRate = new Rate('errors');
const portfolioGetTrend = new Trend('portfolio_get_duration');
const portfolioListTrend = new Trend('portfolio_list_duration');
const portfolioCreateTrend = new Trend('portfolio_create_duration');

// Cache Metrics (응답 시간 기반 추정)
// Redis Latency 고려: 5ms 이하면 Cache Hit으로 간주
const cacheHitRate = new Rate('cache_hits');
const cacheMissRate = new Rate('cache_misses');

// ========================================
// Load Tokens from CSV
// ========================================
const tokens = new SharedArray('tokens', function () {
    const data = open(TOKENS_FILE).split('\n').slice(1);
    return data.map(line => {
        const [userId, email, nickname, accessToken, refreshToken] = line.split(',');
        return {
            userId,
            accessToken: accessToken ? accessToken.trim() : '',
            refreshToken: refreshToken ? refreshToken.trim() : ''
        };
    }).filter(t => t.accessToken);
});

// Token Refresh 메트릭
const tokenRefreshCounter = new Counter('token_refreshes');

// ========================================
// Test Configuration
// ========================================
export const options = {
    stages: [
        { duration: '30s', target: 500 },  // Fast Ramp-up
        { duration: '3m', target: 500 },   // Sustain (3분)
        { duration: '30s', target: 0 },    // Ramp-down
    ],
    thresholds: {
        'http_req_duration': ['p(95)<100'],        // 목표: 100ms 이하
        'portfolio_get_duration': ['p(95)<10'],    // Portfolio 조회: 10ms 이하
        'errors': ['rate<0.01'],                   // 에러율 1% 미만
        'cache_hits': ['rate>0.8'],                // 캐시 히트율 80% 이상
    },
};

// ========================================
// Helper Functions
// ========================================
function getHeaders(token) {
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

function getRandomElement(array) {
    return array[Math.floor(Math.random() * array.length)];
}

/**
 * 토큰 갱신 (TOKEN_EXPIRED 응답 시 호출)
 * @param {string} refreshToken - 리프레시 토큰
 * @returns {object|null} - 새 토큰 정보 또는 null
 */
function refreshAccessToken(refreshToken) {
    const payload = JSON.stringify({ refreshToken: refreshToken });
    const res = http.post(`${BASE_URL}/api/v1/auth/refresh`, payload, {
        headers: { 'Content-Type': 'application/json' }
    });

    if (res.status === 200) {
        const data = res.json();
        return {
            accessToken: data.accessToken,
            refreshToken: data.refreshToken
        };
    }
    return null;
}

/**
 * 인증된 요청 실행 (토큰 만료 시 자동 갱신)
 * @param {string} method - HTTP 메서드 (GET, POST, etc.)
 * @param {string} url - 요청 URL
 * @param {object} user - 사용자 토큰 정보 { accessToken, refreshToken }
 * @param {string|null} body - 요청 바디 (POST/PUT)
 * @returns {object} - { response, tokenRefreshed }
 */
function authenticatedRequest(method, url, user, body = null) {
    let res;
    if (method === 'GET') {
        res = http.get(url, { headers: getHeaders(user.accessToken) });
    } else if (method === 'POST') {
        res = http.post(url, body, { headers: getHeaders(user.accessToken) });
    }

    // 토큰 만료 시 갱신 시도
    if (res.status === 401) {
        try {
            const errorBody = res.json();
            if (errorBody.code === 'TOKEN_EXPIRED' && user.refreshToken) {
                const newTokens = refreshAccessToken(user.refreshToken);
                if (newTokens) {
                    // 토큰 갱신 성공 - 재시도
                    user.accessToken = newTokens.accessToken;
                    user.refreshToken = newTokens.refreshToken;

                    if (method === 'GET') {
                        res = http.get(url, { headers: getHeaders(user.accessToken) });
                    } else if (method === 'POST') {
                        res = http.post(url, body, { headers: getHeaders(user.accessToken) });
                    }
                    return { response: res, tokenRefreshed: true };
                }
            }
        } catch (e) {
            // JSON 파싱 실패 시 무시
        }
    }

    return { response: res, tokenRefreshed: false };
}

// ========================================
// API Operations
// ========================================

/**
 * Portfolio 목록 조회
 * - Read-Only
 * - Cache: 현재 구현에 따라 다름 (개별 Portfolio는 캐싱, List는 비캐싱 가능)
 */
function getPortfolioList(user) {
    const { response: res, tokenRefreshed } = authenticatedRequest(
        'GET',
        `${BASE_URL}/api/v1/portfolios`,
        user
    );

    if (tokenRefreshed) {
        tokenRefreshCounter.add(1);
    }

    const success = check(res, { 'list retrieved': (r) => r.status === 200 });
    errorRate.add(!success);
    portfolioListTrend.add(res.timings.duration);

    return success ? res.json() : [];
}

/**
 * Portfolio 조회 (By ID)
 * - Read-Only
 * - Cache: portfolios (TTL: 5초)
 * - 🎯 Main Target: Cache Hit Rate 측정
 */
function getPortfolio(user, portfolioId) {
    const { response: res, tokenRefreshed } = authenticatedRequest(
        'GET',
        `${BASE_URL}/api/v1/portfolios/${portfolioId}`,
        user
    );

    if (tokenRefreshed) {
        tokenRefreshCounter.add(1);
    }

    const success = check(res, { 'portfolio retrieved': (r) => r.status === 200 });
    errorRate.add(!success);
    portfolioGetTrend.add(res.timings.duration);

    // ✅ Cache Hit 판단: Redis Latency 고려 (5ms 이하 = Cache Hit)
    // Local Cache (Caffeine): < 1ms
    // Redis Cache: 0.5-2ms (hit), > 10ms (miss + DB query)
    if (success) {
        const isCacheHit = res.timings.duration < 5;
        cacheHitRate.add(isCacheHit);
        cacheMissRate.add(!isCacheHit);
    }
}

/**
 * Portfolio 생성
 * - Write Operation
 * - Cache: Evict (해당 사용자의 portfolios 캐시 무효화)
 */
function createPortfolio(user) {
    const payload = JSON.stringify({
        name: `RedisTest ${Date.now()}`,
        description: 'Testing Redis Cache Eviction'
    });

    const { response: res, tokenRefreshed } = authenticatedRequest(
        'POST',
        `${BASE_URL}/api/v1/portfolios`,
        user,
        payload
    );

    if (tokenRefreshed) {
        tokenRefreshCounter.add(1);
    }

    const success = check(res, { 'portfolio created': (r) => r.status === 201 });
    errorRate.add(!success);
    portfolioCreateTrend.add(res.timings.duration);
}

// ========================================
// Main Test Function
// ========================================
export default function () {
    // 사용자 객체 복사 (토큰 갱신 시 원본 수정 방지)
    const originalUser = getRandomElement(tokens);
    const user = {
        userId: originalUser.userId,
        accessToken: originalUser.accessToken,
        refreshToken: originalUser.refreshToken
    };

    const operation = Math.random();

    // 10%: Write (Create Portfolio)
    if (operation < 0.10) {
        createPortfolio(user);
    }
    // 10%: Write (Add Holding - 생략, Create로 대체하여 단순화)
    else if (operation < 0.20) {
        createPortfolio(user);
    }
    // 80%: Read Operations
    else {
        // Portfolio 목록 조회
        const portfolios = getPortfolioList(user);

        if (portfolios.length > 0) {
            const pid = getRandomElement(portfolios).id;

            // ✅ 반복 조회로 Cache Hit 효과 확인
            // 첫 조회: Cache Miss (또는 이전 요청에서 캐싱됨)
            getPortfolio(user, pid);

            // 두 번째 조회: Cache Hit 예상 (TTL 5초 이내)
            getPortfolio(user, pid);
        }
    }

    sleep(0.1);  // Think time
}

// ========================================
// Test Lifecycle (Optional)
// ========================================

/**
 * Setup: 테스트 시작 전 1회 실행
 * - Redis 연결 확인
 * - Backend Health Check
 */
export function setup() {
    console.log('=== Setup Phase ===');

    // Health Check
    const healthRes = http.get(`${BASE_URL}/actuator/health`);
    if (healthRes.status !== 200) {
        throw new Error(`Backend is not ready: ${healthRes.status}`);
    }

    const health = JSON.parse(healthRes.body);
    console.log(`Backend Status: ${health.status}`);
    console.log(`Redis Status: ${health.components.redis.status}`);

    if (health.components.redis.status !== 'UP') {
        throw new Error('Redis is not ready!');
    }

    console.log('✅ Setup Complete: Backend and Redis are ready');
}

/**
 * Teardown: 테스트 종료 후 1회 실행
 * - 최종 메트릭 출력 (선택적)
 */
export function teardown(data) {
    console.log('=== Teardown Phase ===');
    console.log('Test completed. Check results in ../results/exp11_summary.json');
}
