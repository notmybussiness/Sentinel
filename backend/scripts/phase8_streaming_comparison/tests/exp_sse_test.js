import { check, sleep } from 'k6';
import http from 'k6/http';
import { Counter, Trend } from 'k6/metrics';

// ==============================================================================
// 🧪 Sentinel SSE Performance Test Script (Standard Version)
// 
// Target Envirionment:
// - App Server: localhost:8080 or 192.168.0.5
// 
// Note: Uses standard k6/http to ensure compatibility.
// We test "Concurrency" by holding open connections.
// ==============================================================================

const sseConnectTime = new Trend('sse_connect_time', true);
const sseErrors = new Counter('sse_error_count');

export const options = {
    scenarios: {
        sse_streaming_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 100 },   // Warm up
                { duration: '2m', target: 500 },   // Ramp
                { duration: '2m', target: 1000 },  // Stress
                { duration: '1m', target: 1000 },  // Sustain
                { duration: '30s', target: 0 },    // Cooldown
            ],
            gracefulStop: '10s',
        },
    },
    thresholds: {
        sse_error_count: ['count<10'],
    },
};

const BASE_URL = 'http://192.168.0.58:8080';
// const BASE_URL = 'http://192.168.0.58:8080'; // Uncomment for remote
const ENDPOINT = '/api/v1/crypto/stream/prices';
const PARAMS = '?symbols=BTC,ETH,XRP&method=SSE';

export default function () {
    const url = `${BASE_URL}${ENDPOINT}${PARAMS}`;

    const params = {
        tags: { my_tag: 'sse_test' },
        timeout: '60s', // Hold connection for 60s (Simulate active user)
    };

    const start = Date.now();

    try {
        // SSE is just a long-lived HTTP GET
        // We set a timeout to drop the connection naturally after 60s
        const res = http.get(url, params);

        const duration = Date.now() - start;
        sseConnectTime.add(duration);

        if (res.status !== 200) {
            sseErrors.add(1);
            console.error(`Status ${res.status}`);
        }
    } catch (e) {
        // Timeout is expected here as we just want to hold connection
        if (e.error().includes('timeout')) {
            // This is good! Connection held for 60s.
        } else {
            sseErrors.add(1);
            console.error(`Error: ${e.error()}`);
        }
    }
}
