# 📊 Phase 8: SSE Backpressure & Connection Analysis

## 1. Experiment Overview
- **Test Period**: 16:05 ~ 16:16 (Requested Range)
- **Target**: SSE Endpoint with Backpressure (`onBackpressureDrop`)
- **Load**: Ramp-up to 1,000 VUs (50 VU/30s steps)

## 2. Key Findings

### 🔌 Concurrency (Active Connections)
- **Observed**: ~1,857 Active Connections at peak (16:16:45)
- **Expected**: 1,000 VUs
- **Ratio**: ~1.86x (Zombie Connections detected)
- **Analysis**:
    - Connections rise linearly with VUs but consistently overshoot.
    - **Cause**: k6 VUs disconnect after 30s (timeout), but Server keeps connection open longer.
    - **Status**: Improved from Phase 8 original (4x) to ~1.8x, but still leaking.

### 💾 Resource Usage
| Metric | Start (16:05) | Peak (16:16) | Delta |
|--------|---------------|--------------|-------|
| **JVM Heap** | ~100 MiB | ~1.18 GiB | +1.08 GiB |
| **Per Connection** | - | - | **~0.6 MB / conn** |
| **Active Threads** | 131 | 131 | **0 (Stable!)** |

- **Memory**: High consumption (~0.6MB per active SSE connection).
- **Threads**: **Excellent**. Virtual Threads (or NIO) kept Tomcat threads flat at 131. No thread exhaustion.

### ⚡ Reliability & Latency
- **Error Rate**: **Negligible** (Status 403/503 were 0 req/s mostly).
- **Latency (P95)**: 36ms → 630ms at peak. Acceptable for streaming handshake.

## 3. Conclusion & Recommendations
1.  **Zombie Connections**: The 1.8x discrepancy confirms server isn't detecting client disconnects fast enough.
    -   **Action**: Implement **Application-Level Heartbeat** or **Server-Side Timeout**.
2.  **Memory Footprint**: 1.1GB for 1000 users is high.
    -   **Action**: Investigate `ResponseBodyEmitter` retention or resize buffers.
3.  **Stability**: System is **STABLE** under 1000 VUs. No crashes, no OOM, no thread starvation.
