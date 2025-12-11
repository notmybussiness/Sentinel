# 📊 Sentinel SSE Monitoring Guide (Grafana + Prometheus)

This guide provides instructions on how to set up a Grafana dashboard to monitor the SSE (Server-Sent Events) streaming performance of the Sentinel application.

## 1. Prerequisites
- **Sentinel Backend** running with Actuator & Prometheus enabled.
- **Prometheus** configured to scrape Sentinel (default port: 8080).
- **Grafana** connected to Prometheus datasource.

## 2. Key Metrics to Watch

### 🚀 Latency (Response Time)
Measure how long it takes for the server to initiate the SSE stream.
- **Query:** `http_server_requests_seconds_max{uri="/api/v1/crypto/stream/prices"}`
- **Visualization:** Time Series or Gauge
- **Thresholds:**
    - 🟢 < 100ms
    - 🟡 > 500ms
    - 🔴 > 1s

### 👥 Concurrency (Active Connections)
Track the number of active SSE connections (WAS Load).
- **Query:** `http_server_requests_active_value{uri="/api/v1/crypto/stream/prices"}`
- **Visualization:** Stat or Time Series
- **Note:** This directly correlates with the number of `k6` VUs.

### 🖥️ System Resources
Monitor the impact of streaming on the backend server.
- **CPU Usage:** `system_cpu_usage`
- **Memory Usage:** `jvm_memory_used_bytes{area="heap"}`
- **GC Pauses:** `jvm_gc_pause_seconds_max`

### 🧵 Application Server (Tomcat)
Monitor usage of the worker thread pool. If threads are exhausted, new connections (including SSE attributes) will hang.
- **Busy Threads:** `tomcat_threads_busy`
- **Total Threads:** `tomcat_threads_current`
- **Utilization %:** `tomcat_threads_busy / tomcat_threads_config_max_threads`

### 🔌 Database Connection Pool (HikariCP)
Key bottleneck for DB-intensive operations (SSE updates typically query DB).
- **Active Connections:** `hikaricp_connections_active`
- **Pending Connections:** `hikaricp_connections_pending` (Should be 0. If High -> Increase Pool Size)
- **Pool Usage:** `hikaricp_connections_usage`

## 3. Creating the Dashboard

1.  Open Grafana and click **"New Dashboard"**.
2.  **Add Visualization** for each metric above.
3.  **Title:** "Sentinel Streaming Monitor"
4.  **Auto-Refresh:** Set to `5s` for real-time monitoring.

## 4. Alerting Rules (Recommended)

- **High Error Rate Alert**:
    - Query: `rate(http_server_requests_seconds_count{status=~"5.."}[1m]) > 0`
    - Trigger if any 500 errors occur during streaming.

- **High Latency Alert**:
    - Query: `http_server_requests_seconds_max{uri="/api/v1/crypto/stream/prices"} > 2`
    - Trigger if connection takes > 2 seconds.
