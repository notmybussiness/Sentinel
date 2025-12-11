                  /**
 * Experiment 5 - Baseline Test
 *
 * Portfolio CRUD API의 N+1 쿼리 문제를 확인하는 Baseline 테스트
 *
 * Test Scenario (Mixed Workload):
 *   - 10%: Portfolio 생성 (Write)
 *   - 20%: Holding 추가 (Write)
 *   - 50%: Portfolio 조회 (Read - N+1 예상!)
 *   - 20%: Portfolio 목록 (Read - N+1 심각!)
 *
 * Load Pattern:
 *   - 0 → 200 VUsers (1분 ramp-up)
 *   - 200 VUsers (6분 유지)
 *   - 200 → 0 VUsers (1분 ramp-down)
 *   - 총 8분
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate, Trend, Counter } from 'k6/metrics';

// Configuration
const BASE_URL = 'http://192.168.0.58:8080';
const TOKENS_FILE = __ENV.TOKENS_FILE || './tokens.csv';

// Custom Metrics
const errorRate = new Rate('errors');
const portfolioGetTrend = new Trend('portfolio_get_duration');
const portfolioListTrend = new Trend('portfolio_list_duration');
const portfolioCreateTrend = new Trend('portfolio_create_duration');
const holdingAddTrend = new Trend('holding_add_duration');
const sqlQueriesCounter = new Counter('sql_queries_total');

// Load tokens from CSV
const tokens = new SharedArray('tokens', function() {
    const data = open(TOKENS_FILE).split('\n').slice(1); // Skip header
    return data.map(line => {
        const [userId, email, nickname, accessToken, refreshToken] = line.split(',');
        return {
            userId: userId,
            email: email,
            nickname: nickname,
            accessToken: accessToken ? accessToken.trim() : '',
            refreshToken: refreshToken ? refreshToken.trim() : ''
        };
    }).filter(t => t.accessToken); // Filter out empty lines
});

// Test options
export const options = {
    stages: [
        { duration: '1m', target: 500 },  // Ramp-up to 500 VUsers
        { duration: '6m', target: 500 },  // Stay at 500 VUsers (Phase 2와 동일)
        { duration: '1m', target: 0 },    // Ramp-down to 0
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'],  // 95% of requests should be below 500ms
        'errors': ['rate<0.05'],              // Error rate should be below 5%
    },
};

// Korean stock symbols (종목코드 6자리)
const KOREAN_STOCKS = [
    '005930', '000660', '035420', '035720', '051910', '006400', '068270',
    '105560', '055550', '003550', '207940', '373220'
];

// US stock symbols
const US_STOCKS = [
    'AAPL', 'MSFT', 'GOOGL', 'AMZN', 'META', 'TSLA', 'NVDA', 'AMD',
    'JPM', 'BAC', 'V', 'MA', 'JNJ', 'PFE', 'DIS', 'NFLX'
];

// All stocks (mixed)
const ALL_STOCKS = [...KOREAN_STOCKS, ...US_STOCKS];

const CRYPTO_SYMBOLS = [
    'BTC', 'ETH', 'BNB', 'XRP', 'ADA', 'SOL', 'DOGE', 'DOT'
];

// Helper functions
function getRandomToken() {
    return tokens[Math.floor(Math.random() * tokens.length)];
}

function getHeaders(token) {
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

function getRandomElement(array) {
    return array[Math.floor(Math.random() * array.length)];
}

function generateRandomHolding(isCrypto = false) {
    if (isCrypto) {
        return {
            symbol: getRandomElement(CRYPTO_SYMBOLS),
            quantity: (Math.random() * 10).toFixed(6),
            averageCost: (Math.random() * 50000 + 100).toFixed(2),
            assetType: 'CRYPTO',
            baseCurrency: Math.random() > 0.5 ? 'USD' : 'KRW'
        };
    } else {
        const symbol = getRandomElement(ALL_STOCKS);
        const isKorean = KOREAN_STOCKS.includes(symbol);

        return {
            symbol: symbol,
            quantity: (Math.random() * 100 + 1).toFixed(2),
            averageCost: isKorean
                ? (Math.random() * 500000 + 10000).toFixed(0)  // 한국: 10,000-500,000원
                : (Math.random() * 500 + 10).toFixed(2),        // 미국: $10-500
            assetType: 'STOCK',
            baseCurrency: isKorean ? 'KRW' : 'USD'
        };
    }
}

// API Operations
function createPortfolio(token) {
    const payload = JSON.stringify({
        name: `Test Portfolio ${Date.now()}`,
        description: 'Baseline test portfolio'
    });

    const res = http.post(
        `${BASE_URL}/api/v1/portfolios`,
        payload,
        { headers: getHeaders(token) }
    );

    const success = check(res, {
        'portfolio created': (r) => r.status === 201,
    });

    errorRate.add(!success);
    portfolioCreateTrend.add(res.timings.duration);

    return success ? res.json() : null;
}

function getPortfolioList(token) {
    const res = http.get(
        `${BASE_URL}/api/v1/portfolios`,
        { headers: getHeaders(token) }
    );

    const success = check(res, {
        'portfolio list retrieved': (r) => r.status === 200,
    });

    errorRate.add(!success);
    portfolioListTrend.add(res.timings.duration);

    // N+1 문제 확인을 위한 SQL 쿼리 수 추정
    if (success && res.body) {
        const portfolios = res.json();
        // 각 포트폴리오당 평균 1개의 추가 쿼리 발생 예상 (N+1)
        sqlQueriesCounter.add(portfolios.length + 1);
    }

    return success ? res.json() : null;
}

function getPortfolio(token, portfolioId) {
    const res = http.get(
        `${BASE_URL}/api/v1/portfolios/${portfolioId}`,
        { headers: getHeaders(token) }
    );

    const success = check(res, {
        'portfolio retrieved': (r) => r.status === 200,
    });

    errorRate.add(!success);
    portfolioGetTrend.add(res.timings.duration);

    // N+1 문제 확인: Portfolio 1개 + Holdings 쿼리 1개 = 2개
    if (success) {
        sqlQueriesCounter.add(2);
    }

    return success ? res.json() : null;
}

function addHolding(token, portfolioId, isCrypto = false) {
    const payload = JSON.stringify(generateRandomHolding(isCrypto));

    const res = http.post(
        `${BASE_URL}/api/v1/portfolios/${portfolioId}/holdings`,
        payload,
        { headers: getHeaders(token) }
    );

    const success = check(res, {
        'holding added': (r) => r.status === 201,
    });

    errorRate.add(!success);
    holdingAddTrend.add(res.timings.duration);

    return success;
}

// Main test scenario
export default function() {
    const user = getRandomToken();
    const operation = Math.random();

    if (operation < 0.10) {
        // 10%: Portfolio 생성
        const portfolio = createPortfolio(user.accessToken);

        if (portfolio && Math.random() > 0.5) {
            // 생성한 포트폴리오에 바로 종목 추가
            addHolding(user.accessToken, portfolio.id);
        }
    } else if (operation < 0.30) {
        // 20%: Holding 추가
        // 먼저 포트폴리오 목록을 가져와서 하나를 선택
        const portfolios = getPortfolioList(user.accessToken);

        if (portfolios && portfolios.length > 0) {
            const randomPortfolio = getRandomElement(portfolios);
            addHolding(user.accessToken, randomPortfolio.id);
        }
    } else if (operation < 0.80) {
        // 50%: Portfolio 조회 (N+1 예상!)
        const portfolios = getPortfolioList(user.accessToken);

        if (portfolios && portfolios.length > 0) {
            const randomPortfolio = getRandomElement(portfolios);
            getPortfolio(user.accessToken, randomPortfolio.id);
        }
    } else {
        // 20%: Portfolio 목록 조회 (N+1 심각!)
        getPortfolioList(user.accessToken);
    }

    sleep(0.1); // 100ms think time
}

// Setup function (optional)
export function setup() {
    console.log('🚀 Experiment 5 Baseline Test Starting...');
    console.log(`📊 Backend: ${BASE_URL}`);
    console.log(`👥 Users: ${tokens.length}`);
    console.log(`⏱️  Duration: 8 minutes`);
    console.log(`📈 Load: 0 → 500 → 0 VUsers (Phase 2와 동일)`);
    console.log(`📦 Stock Mix: Korean ${KOREAN_STOCKS.length} + US ${US_STOCKS.length}`);
    console.log('');
}

// Teardown function (optional)
export function teardown() {
    console.log('');
    console.log('✅ Experiment 5 Baseline Test Completed!');
    console.log('📊 Check the results and SQL logs for N+1 queries');
}
