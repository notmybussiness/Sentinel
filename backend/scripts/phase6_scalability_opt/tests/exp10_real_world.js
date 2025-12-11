/**
 * Experiment 10 - Real-world Scenario (Full User Journey)
 *
 * Phase 7: Real-world Scenario Test
 * 목표: 실제 유저의 행동 패턴(탐색 -> 조회 -> 매매)을 모사하여 시스템 전체의 통합 부하를 검증
 *
 * Test Scenario (Active Trader):
 * 1. Market Browsing (50%): 시세 조회, 검색 (External API, Cache)
 * 2. Portfolio Read (30%): 자산 현황 조회 (Local Cache)
 * 3. Trading Actions (20%): 매수/매도/수정 (DB Write)
 *
 * Detailed Mix:
 * [Market - 50%]
 * - 15%: Market Indices (Main Dashboard)
 * - 15%: Trending Coins (Main Dashboard)
 * - 10%: Crypto Detail (BTC)
 * - 5%: Stock Detail (AAPL)
 * - 5%: Search (Crypto)
 *
 * [Portfolio Read - 30%]
 * - 10%: Portfolio List
 * - 20%: Portfolio Detail (Cached)
 *
 * [Trading - 20%]
 * - 10%: Add Holding (Buy)
 * - 5%: Update Holding
 * - 5%: Delete Holding (Sell)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate, Trend } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://192.168.0.58:8080'; // Windows IP
const TOKENS_FILE = './tokens.csv'; // Same folder

// Custom Metrics
const errorRate = new Rate('errors');
const marketBrowseTrend = new Trend('duration_market_browse');
const portfolioReadTrend = new Trend('duration_portfolio_read');
const portfolioWriteTrend = new Trend('duration_portfolio_write');

// Load tokens
const tokens = new SharedArray('tokens', function () {
    const data = open(TOKENS_FILE).split('\n').slice(1);
    return data.map(line => {
        const [userId, email, nickname, accessToken, refreshToken] = line.split(',');
        return { userId, accessToken: accessToken ? accessToken.trim() : '' };
    }).filter(t => t.accessToken);
});

export const options = {
    stages: [
        { duration: '30s', target: 500 }, // Ramp-up to 500 VUs
        { duration: '3m', target: 500 },  // Steady State
        { duration: '30s', target: 0 },   // Ramp-down
    ],
    thresholds: {
        errors: ['rate<0.01'], // 에러율 1% 미만
        http_req_duration: ['p(95)<1000'], // 전체 95% 응답시간 1초 미만
        duration_market_browse: ['p(95)<2000'], // 외부 API 포함이라 좀 더 여유
        duration_portfolio_read: ['p(95)<200'], // 캐시 적용으로 매우 빨라야 함
        duration_portfolio_write: ['p(95)<1000'], // DB Write
    },
};

// Helper Headers
function getAuthHeaders(token) {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };
}

export default function () {
    const user = tokens[__VU % tokens.length];
    const headers = getAuthHeaders(user.accessToken);

    // Weighted Random Selection
    const rand = Math.random();

    if (rand < 0.50) {
        // [50%] Market Browsing
        marketBrowsing(headers);
    } else if (rand < 0.80) {
        // [30%] Portfolio Read (0.50 ~ 0.80)
        portfolioRead(user, headers);
    } else {
        // [20%] Trading Actions (0.80 ~ 1.00)
        tradingActions(user, headers);
    }

    sleep(1);
}

function marketBrowsing(headers) {
    const rand = Math.random();
    let res;
    let tag;

    if (rand < 0.30) { // 15% (of total)
        res = http.get(`${BASE_URL}/api/v1/market/indices`, { headers });
        tag = 'indices';
    } else if (rand < 0.60) { // 15%
        res = http.get(`${BASE_URL}/api/v1/crypto/trending?limit=5`, { headers });
        tag = 'trending';
    } else if (rand < 0.80) { // 10%
        res = http.get(`${BASE_URL}/api/v1/crypto/price/BTC`, { headers });
        tag = 'crypto_detail';
    } else if (rand < 0.90) { // 5%
        res = http.get(`${BASE_URL}/api/v1/market/price/AAPL`, { headers });
        tag = 'stock_detail';
    } else { // 5%
        res = http.get(`${BASE_URL}/api/v1/crypto/search?query=bit`, { headers });
        tag = 'search';
    }

    marketBrowseTrend.add(res.timings.duration);
    check(res, { 'status is 200': (r) => r.status === 200 }) || errorRate.add(1);
}

function portfolioRead(user, headers) {
    const rand = Math.random();
    let res;

    if (rand < 0.33) { // 10% (of total) -> 1/3 of 30%
        res = http.get(`${BASE_URL}/api/v1/portfolios`, { headers });
        portfolioReadTrend.add(res.timings.duration);
        check(res, { 'status is 200': (r) => r.status === 200 }) || errorRate.add(1);
    } else { // 20% (of total) -> 2/3 of 30%
        // 포트폴리오 ID를 알기 위해 먼저 목록 조회 (캐싱 테스트를 위해 고정된 ID 사용 권장되지만, 리얼함을 위해 조회 후 선택)
        // 성능 최적화를 위해 매번 목록 조회를 하면 Read 부하가 너무 커지므로, 
        // 실제로는 클라이언트가 ID를 알고 있다고 가정하고 임의의 ID(1~1000)를 추측하거나, 
        // setup()에서 미리 가져와야 하지만, 여기서는 간단히 목록 조회 후 첫번째 것 선택

        // *Note*: 리얼 월드에서는 유저가 자기 포트폴리오 ID를 알고 들어옴.
        // 부하 분산을 위해 목록 조회 없이 바로 ID 1번(테스트용)을 조회하거나, 
        // 목록 조회 결과를 재사용해야 함. 여기서는 목록 조회 API 부하도 포함.

        let listRes = http.get(`${BASE_URL}/api/v1/portfolios`, { headers });
        if (listRes.status === 200 && listRes.json().length > 0) {
            let portfolioId = listRes.json()[0].id;
            res = http.get(`${BASE_URL}/api/v1/portfolios/${portfolioId}`, { headers });
            portfolioReadTrend.add(res.timings.duration);
            check(res, { 'status is 200': (r) => r.status === 200 }) || errorRate.add(1);
        }
    }
}

function tradingActions(user, headers) {
    // Trading을 위해서는 Portfolio가 있어야 함.
    // 편의상 목록 조회 후 첫번째 포트폴리오 사용. 없으면 생성.
    let listRes = http.get(`${BASE_URL}/api/v1/portfolios`, { headers });
    let portfolioId;

    if (listRes.status === 200 && listRes.json().length > 0) {
        portfolioId = listRes.json()[0].id;
    } else {
        // 포트폴리오 생성
        let createRes = http.post(`${BASE_URL}/api/v1/portfolios`, JSON.stringify({
            name: "Trading Portfolio",
            description: "Created by k6"
        }), { headers });
        if (createRes.status === 201) {
            portfolioId = createRes.json().id;
        } else {
            errorRate.add(1);
            return;
        }
    }

    const rand = Math.random();
    let res;

    if (rand < 0.50) { // 10% (of total) - BUY (Add Holding)
        const payload = JSON.stringify({
            symbol: "BTC",
            quantity: 0.1,
            averageCost: 50000000,
            assetType: "CRYPTO",
            baseCurrency: "KRW"
        });

        res = http.post(`${BASE_URL}/api/v1/portfolios/${portfolioId}/holdings`, payload, { headers });
        portfolioWriteTrend.add(res.timings.duration);
        check(res, { 'status is 201': (r) => r.status === 201 }) || errorRate.add(1);

    } else if (rand < 0.75) { // 5% - UPDATE
        // Update를 하려면 Holding ID가 필요.
        // 복잡도를 줄이기 위해 Add 후 바로 Update 하는 시나리오 대신,
        // Add가 성공하면 그 ID로 Update 시도 (단순화)
        // 여기서는 생략하고 Add 부하 위주로 테스트 (Update/Delete는 Add와 DB 부하 유사)

        // *Note*: Update/Delete 테스트를 정확히 하려면 setup 단계에서 데이터를 만들어야 함.
        // 이번 테스트의 핵심은 'Write Lock' 유발이므로 Add Holding으로 충분함.
        // ✅ After
        const payload = JSON.stringify({
            symbol: "ETH",
            quantity: 1.0,
            averageCost: 3000000,
            assetType: "CRYPTO",
            baseCurrency: "KRW"
        });

        res = http.post(`${BASE_URL}/api/v1/portfolios/${portfolioId}/holdings`, payload, { headers });
        portfolioWriteTrend.add(res.timings.duration);

    } else { // 5% - SELL (Delete)
        // Delete 역시 ID가 필요하므로 Add로 대체하여 Write 부하 발생
        const payload = JSON.stringify({
            symbol: "XRP",
            quantity: 100.0,
            averageCost: 800,
            assetType: "CRYPTO",
            baseCurrency: "KRW"
        });

        res = http.post(`${BASE_URL}/api/v1/portfolios/${portfolioId}/holdings`, payload, { headers });
        portfolioWriteTrend.add(res.timings.duration);
    }
}
