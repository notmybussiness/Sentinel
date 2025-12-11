# 🏗️ INFRASTRUCTURE & TEST ENVIRONMENT

> **Context**: Physical 3-Tier Architecture for Realistic Performance Testing.
> **Last Updated**: 2025-11-25

## 1. Network Topology (Local Network)

| Node | Role | IP Address | Port(s) | Hardware Spec |
|------|------|------------|---------|---------------|
| **Client** | Load Generator (k6) | `External (Mac)` | - | **MacBook Air M1** (8GB RAM) |
| **WAS** | App Server (Spring Boot) | `192.168.0.58` | 8080 | **i5-12400F** / **16GB RAM** |
| **DB/Infra** | Database & Monitoring | `192.168.0.5` | 5432, 9090, 3000 | **Ryzen 5 3400G** / **12GB RAM** |

---

## 2. Component Details & Constraints

### 🖥️ WAS Node (`192.168.0.58`)
- **CPU**: Intel Core i5-12400F (6C/12T) - *High Performance*
- **RAM**: 16GB (Sufficient for Heap + OS)
- **Role**: Spring Boot 3.5.5 Application Server
- **Constraint**: Expect to be the strongest node. CPU will likely stay low unless logic is extremely heavy.

### 🗄️ DB/Infra Node (`192.168.0.5`)
- **CPU**: AMD Ryzen 5 3400G (4C/8T) - *Potential Bottleneck*
- **RAM**: 12GB (Monitor Swap usage)
- **Stack**:
  - PostgreSQL 15 (Port 5432)
  - Prometheus (Port 9090)
  - Grafana (Port 3000)
- **Risk**: This node handles both DB queries AND monitoring ingestion. Heavy queries might disrupt metric collection.

### 🚀 Client Node (MacBook Air M1)
- **CPU**: Apple M1 (8 Core)
- **RAM**: 8GB - *Critical Resource*
- **Role**: k6 Load Generator
- **Warning**: High VU counts (>2000) may cause OOM on this device due to 8GB RAM limit.

---

## 3. Testing Strategy (SRE Persona)

1.  **Bottleneck Hunting**:
    - Primary suspect is **DB CPU (3400G)**.
    - If DB CPU < 50% but latency is high -> Check **Network** or **DB Lock**.
2.  **Client Monitoring**:
    - Watch Mac's Memory Pressure. If yellow/red, reduce k6 VUs.
3.  **Metrics**:
    - Prometheus scrapes WAS every 5s (default). Ensure DB Server isn't overloaded by Prometheus itself.