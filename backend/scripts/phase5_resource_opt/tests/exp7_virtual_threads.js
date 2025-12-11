import { Rate, Trend, Counter } from 'k6/metrics';
import { check, sleep } from 'k6';
import http from 'k6/http';
import { SharedArray } from 'k6/data';

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
const schedulerConflict = new Rate('scheduler_conflict_rate');

// Load tokens from CSV
const tokens = new SharedArray('tokens', function () {
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
        { duration: '6m', target: 500 },  // Stay at 500 VUsers
        { duration: '1m', target: 0 },    // Ramp-down to 0
    ],
    thresholds: {
        'http_req_duration': ['p(95)<200'],  // Target improved to 200ms (was 500ms)
        'errors': ['rate<0.01'],              // Error rate should be below 1%
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
        description: 'Virtual Thread test portfolio'
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

    if (success && res.body) {
        const portfolios = res.json();
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
export default function () {
    const user = getRandomToken();
    const operation = Math.random();

    if (operation < 0.10) {
        // 10%: Portfolio 생성
        const portfolio = createPortfolio(user.accessToken);

        if (portfolio && Math.random() > 0.5) {
            addHolding(user.accessToken, portfolio.id);
        }
    } else if (operation < 0.30) {
        // 20%: Holding 추가
        const portfolios = getPortfolioList(user.accessToken);

        if (portfolios && portfolios.length > 0) {
            const randomPortfolio = getRandomElement(portfolios);
            addHolding(user.accessToken, randomPortfolio.id);
        }
    } else if (operation < 0.80) {
        // 50%: Portfolio 조회
        const portfolios = getPortfolioList(user.accessToken);

        if (portfolios && portfolios.length > 0) {
            const randomPortfolio = getRandomElement(portfolios);
            getPortfolio(user.accessToken, randomPortfolio.id);
        }
    } else {
        // 20%: Portfolio 목록 조회
        getPortfolioList(user.accessToken);
    }

    sleep(0.1); // 100ms think time
}

// Setup function
export function setup() {
    console.log('🚀 Experiment 7: Virtual Threads & Resource Opt Starting...');
    console.log(`📊 Backend: ${BASE_URL}`);
    console.log(`👥 Users: ${tokens.length}`);
    console.log(`⏱️  Duration: 8 minutes`);
    console.log(`📈 Load: 0 → 500 → 0 VUsers`);
    console.log(`⚡ Virtual Threads: ENABLED`);
    console.log(`🗄️  HikariCP Pool: 50`);
    console.log('');
}

// Teardown function
export function teardown() {
    console.log('');
    console.log('✅ Experiment 7 Completed!');
    console.log('📊 Check if throughput improved and thread count is low');
}
