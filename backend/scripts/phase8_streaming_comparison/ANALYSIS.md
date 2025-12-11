# Phase 8: 스트리밍 방식 성능 비교

> **목표**: SSE vs Long Polling vs WebSocket 성능 비교 실험

---

## 📋 실험 계획

### Experiment 12: SSE Baseline
- **대상**: `/api/v1/crypto/stream/prices?method=SSE`
- **부하**: 100, 500, 1000 동시 연결
- **측정 항목**:
  - 연결당 메모리 사용량
  - 메시지 지연 시간 (Latency)
  - 연결 유지율 (Connection Stability)
  - 서버 CPU 사용률

### Experiment 13: Long Polling Baseline
- **대상**: `/api/v1/crypto/stream/prices?method=LongPolling`
- **부하**: 100, 500, 1000 동시 연결
- **측정 항목**:
  - 요청 수 (Requests/sec)
  - HTTP 오버헤드
  - 평균 응답 시간

### Experiment 14: WebSocket Baseline
- **대상**: Upbit 실제 WebSocket 연결 (구현 필요)
- **부하**: 100, 500, 1000 동시 연결
- **측정 항목**:
  - 메시지 레이턴시
  - 연결 안정성
  - 메모리 사용량

---

## 🔧 사전 작업

- [ ] SSE 부하 테스트 스크립트 작성 (`exp12_sse_baseline.js`)
- [ ] Long Polling 부하 테스트 스크립트 작성 (`exp13_longpolling.js`)
- [ ] Upbit WebSocket 실제 연결 구현 (TODO 해결)
- [ ] WebSocket 부하 테스트 스크립트 작성 (`exp14_websocket.js`)

---

## 📊 예상 결과

| 방식 | 동시 연결 100 | 동시 연결 500 | 동시 연결 1000 |
|------|---------------|---------------|----------------|
| SSE | 안정적 예상 | ? | ? |
| Long Polling | 요청 폭주 예상 | 서버 부하 증가 | DB 커넥션 문제? |
| WebSocket | 최고 성능 예상 | 메모리 주의 | 연결 관리 이슈? |

---

## 📁 폴더 구조

```
scripts/phase8_streaming_comparison/
├── ANALYSIS.md           ← 이 파일
├── tests/
│   ├── exp12_sse_baseline.js
│   ├── exp13_longpolling.js
│   └── exp14_websocket.js
└── results/
    ├── exp12_sse_results.json
    ├── exp13_longpolling_results.json
    └── exp14_websocket_results.json
```

---

## 🔜 다음 단계

1. SSE k6 테스트 스크립트 작성
2. 서버 실행 후 SSE 기본 성능 측정
3. Long Polling 스크립트 작성 및 비교
4. WebSocket 실제 구현 후 최종 비교

---

**Last Updated**: 2024-12-08
**Status**: 🚧 Planning
