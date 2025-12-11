import { check, sleep } from 'k6';
import http from 'k6/http';
import { Counter, Trend } from 'k6/metrics';

// ==============================================================================
// 🧪 SSE Backpressure Test - VU 한계 탐색
// 
// 목표: 50 VU 단위로 30초씩 증가하여 1000 VU까지 테스트
// 총 시간: 약 10분 30초 (50→100→150→...→1000 + Cooldown)
// ==============================================================================

const sseConnectTime = new Trend('sse_connect_time', true);
const sseErrors = new Counter('sse_error_count');
const sseMsgReceived = new Counter('sse_msg_received');

export const options = {
    scenarios: {
        sse_step_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                // 50 VU 단위, 30초씩 증가
                { duration: '30s', target: 50 },
                { duration: '30s', target: 100 },
                { duration: '30s', target: 150 },
                { duration: '30s', target: 200 },
                { duration: '30s', target: 250 },
                { duration: '30s', target: 300 },
                { duration: '30s', target: 350 },
                { duration: '30s', target: 400 },
                { duration: '30s', target: 450 },
                { duration: '30s', target: 500 },
                { duration: '30s', target: 550 },
                { duration: '30s', target: 600 },
                { duration: '30s', target: 650 },
                { duration: '30s', target: 700 },
                { duration: '30s', target: 750 },
                { duration: '30s', target: 800 },
                { duration: '30s', target: 850 },
                { duration: '30s', target: 900 },
                { duration: '30s', target: 950 },
                { duration: '30s', target: 1000 },
                // Cooldown
                { duration: '30s', target: 0 },
            ],
            gracefulStop: '10s',
        },
    },
    thresholds: {
        sse_error_count: ['count<50'],
        http_req_failed: ['rate<0.05'],
    },
};

// 서버 주소 설정
const BASE_URL = __ENV.BASE_URL || 'http://192.168.0.58:8080';
const ENDPOINT = '/api/v1/crypto/stream/prices';
const PARAMS = '?symbols=BTC,ETH&method=WebSocket';

export default function () {
    const url = `${BASE_URL}${ENDPOINT}${PARAMS}`;

    const params = {
        tags: { test: 'sse_backpressure' },
        timeout: '35s', // 30초 유지 후 종료
    };

    const start = Date.now();

    try {
        const res = http.get(url, params);
        const duration = Date.now() - start;
        sseConnectTime.add(duration);

        // 응답 체크
        const success = check(res, {
            'status is 200': (r) => r.status === 200,
            'has SSE content': (r) => r.body && r.body.includes('event:'),
        });

        if (!success) {
            sseErrors.add(1);
            console.error(`[VU ${__VU}] Status: ${res.status}`);
        } else {
            // SSE 메시지 수 카운트 (대략적)
            const eventCount = (res.body.match(/event:/g) || []).length;
            sseMsgReceived.add(eventCount);
        }
    } catch (e) {
        // Timeout은 정상 (30초 연결 유지)
        if (!e.message.includes('timeout')) {
            sseErrors.add(1);
            console.error(`[VU ${__VU}] Error: ${e.message}`);
        }
    }
}

export function handleSummary(data) {
    const now = new Date().toISOString().slice(0, 19).replace(/:/g, '-');
    return {
        [`results/sse_backpressure_${now}.json`]: JSON.stringify(data, null, 2),
        'stdout': textSummary(data),
    };
}

function textSummary(data) {
    const metrics = data.metrics;
    return `
=== SSE Backpressure Test Summary ===
VUs Peak:        ${data.options?.scenarios?.sse_step_test?.stages?.slice(-2)[0]?.target || 'N/A'}
Total Requests:  ${metrics.http_reqs?.values?.count || 0}
Errors:          ${metrics.sse_error_count?.values?.count || 0}
Avg Connect:     ${Math.round(metrics.sse_connect_time?.values?.avg || 0)}ms
P95 Connect:     ${Math.round(metrics.sse_connect_time?.values?.['p(95)'] || 0)}ms
Messages Recv:   ${metrics.sse_msg_received?.values?.count || 0}
=====================================
`;
}
