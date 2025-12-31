# Performance Tuning Guide

> 1000+ TPS 달성을 위한 성능 실험 가이드

---

## 현재 상태

| 메트릭 | 값 | 목표 |
|--------|-----|------|
| TPS | 217 req/s | 1000+ req/s |
| P95 | 149ms | < 100ms |
| 측정일 | 2025-12-25 | - |

---

## 1. 실험 환경 (3-Tier)

```
┌─────────────────┐     ┌─────────────────────────────────┐     ┌─────────────────┐
│   DB Server     │     │          WAS Server             │     │  Client Server  │
│  192.168.0.5    │     │        192.168.0.58             │     │    (macOS)      │
├─────────────────┤     ├─────────────────────────────────┤     ├─────────────────┤
│  PostgreSQL     │◄───►│  Spring Boot (:8080)            │◄────│    k6           │
│  (:5432)        │     │  Python RAG (:8000)             │     └─────────────────┘
│                 │     │  Grafana (:3001)                │
│                 │     │  InfluxDB (:8086)               │
│                 │     │  Prometheus (:9090)             │
└─────────────────┘     └─────────────────────────────────┘
```

---

## 2. 실험 목록

| 실험 | 항목 | 예상 효과 | 상태 |
|------|------|----------|------|
| EXP-01 | DB 인덱싱 | 10-50x 쿼리 개선 | 대기 |
| EXP-02 | JVM 힙 튜닝 | 10-15% TPS 향상 | 대기 |
| EXP-03 | 로깅 레벨 최적화 | TBD | 예정 |
| EXP-04 | Redis 연결풀 확대 | TBD | 예정 |
| EXP-05 | WebClient 비동기 전환 | TBD | 예정 |

**원칙**: 각 실험은 독립적으로 진행 (한 번에 하나의 변수만 변경)

---

## 3. 표준 실험 절차

### Phase 1: 환경 준비

```powershell
# [WAS] 모니터링 스택 시작
cd backend/scripts/perf-tuning/was
.\start_monitoring.ps1

# [WAS] Spring Boot 시작 (perf 프로파일)
cd backend
./gradlew bootRun --args='--spring.profiles.active=perf'

# [Client] 토큰 생성 (최초 1회)
python generate_tokens.py --url http://192.168.0.58:8080
```

### Phase 2: Baseline 측정

```bash
# [Client/Mac]
./run_k6.sh baseline

# Grafana에서 확인: http://192.168.0.58:3001
```

### Phase 3: 변경 적용

실험별 변경사항 적용 (예: DB 인덱스 추가, JVM 설정 변경)

### Phase 4: After 측정

```bash
# [Client/Mac]
./run_k6.sh after

# Before/After 비교
```

### Phase 5: 정리

1. 결과 기록
2. 다음 실험 결정
3. 필요시 롤백

---

## 4. EXP-01: DB 인덱싱

### 가설
> Portfolio, UserSession 테이블에 인덱스가 없어 Full Table Scan 발생.
> 인덱스 추가 시 쿼리 성능 10-50배 개선되어 TPS 상승 예상.

### 현재 상태 (인덱스 없음)

| 테이블 | 조회 컬럼 | 인덱스 |
|--------|----------|--------|
| portfolios | user_id | ❌ |
| portfolio_holdings | portfolio_id | △ (unique만) |
| user_sessions | user_id | ❌ |
| user_sessions | access_token_hash | ❌ |
| user_sessions | refresh_token_hash | ❌ |
| user_sessions | expires_at | ❌ |

### 변경 사항 (SQL)

```sql
-- Portfolio 인덱스
CREATE INDEX CONCURRENTLY idx_portfolios_user_id
ON portfolios(user_id);

CREATE INDEX CONCURRENTLY idx_portfolios_user_id_created_at
ON portfolios(user_id, created_at DESC);

-- PortfolioHolding 인덱스
CREATE INDEX CONCURRENTLY idx_portfolio_holdings_portfolio_id
ON portfolio_holdings(portfolio_id);

-- UserSession 인덱스
CREATE INDEX CONCURRENTLY idx_user_sessions_user_id
ON user_sessions(user_id);

CREATE INDEX CONCURRENTLY idx_user_sessions_access_token_hash
ON user_sessions(access_token_hash)
WHERE is_active = true;

CREATE INDEX CONCURRENTLY idx_user_sessions_refresh_token_hash
ON user_sessions(refresh_token_hash)
WHERE is_active = true;

CREATE INDEX CONCURRENTLY idx_user_sessions_expires_at
ON user_sessions(expires_at)
WHERE is_active = true;
```

### 예상 결과

| 메트릭 | Before | After (예상) |
|--------|--------|-------------|
| TPS | 217 | 300-400 |
| P95 | 149ms | 80-120ms |

### 롤백

```sql
DROP INDEX CONCURRENTLY idx_portfolios_user_id;
DROP INDEX CONCURRENTLY idx_portfolios_user_id_created_at;
DROP INDEX CONCURRENTLY idx_portfolio_holdings_portfolio_id;
DROP INDEX CONCURRENTLY idx_user_sessions_user_id;
DROP INDEX CONCURRENTLY idx_user_sessions_access_token_hash;
DROP INDEX CONCURRENTLY idx_user_sessions_refresh_token_hash;
DROP INDEX CONCURRENTLY idx_user_sessions_expires_at;
```

---

## 5. EXP-02: JVM 힙 튜닝

### 가설
> 초기 힙(512MB)과 최대 힙(2GB) 차이가 커서 런타임 힙 확장 시 GC 오버헤드 발생.
> Xms=Xmx 동일하게 설정 시 10-15% TPS 향상 예상.

### 변경 사항 (build.gradle)

```java
// Before
bootRun {
    jvmArgs = [
        '-Xms512m',
        '-Xmx2g',
        '-XX:+UseG1GC',
        '-XX:MaxGCPauseMillis=200'
    ]
}

// After
bootRun {
    jvmArgs = [
        '-Xms2g',
        '-Xmx2g',
        '-XX:+UseG1GC',
        '-XX:MaxGCPauseMillis=300',
        '-XX:G1HeapRegionSize=16m',
        '-XX:+ParallelRefProcEnabled'
    ]
}
```

---

## 6. 수집 메트릭

### k6 메트릭 (InfluxDB)

| 메트릭 | 설명 | 목표 |
|--------|------|------|
| `http_reqs` (rate) | TPS | > 1000 |
| `http_req_duration` p50 | 중앙값 지연시간 | < 50ms |
| `http_req_duration` p95 | 95% 지연시간 | < 100ms |
| `http_req_duration` p99 | 99% 지연시간 | < 200ms |
| `http_req_failed` | 실패율 | < 1% |

### Spring Boot 메트릭 (Prometheus)

| 메트릭 | 주의 신호 |
|--------|-----------|
| `hikaricp_connections_active` | max 근처 = 병목 |
| `hikaricp_connections_pending` | > 0 = 병목 |
| `jvm_memory_used_bytes{area="heap"}` | > 80% = 위험 |
| `jvm_gc_pause_seconds_sum` | 급증 = 문제 |

---

## 7. 결과 기록 템플릿

```markdown
## EXP-XX: [실험명]

### 실험 정보
- 일시: YYYY-MM-DD HH:MM
- 가설:

### 결과

| 메트릭 | Before | After | Delta | 판정 |
|--------|--------|-------|-------|------|
| TPS | | | | |
| P95 | | | | |
| P99 | | | | |
| Error Rate | | | | |

### 결론
- [ ] 성공 / 실패 / 추가 실험 필요
```

---

## 8. 스크립트 위치

```
backend/scripts/perf-tuning/
├── exp01_db_indexing.js          # EXP-01 스크립트
├── generate_tokens.py            # 토큰 생성
├── docker-compose.monitoring.yml
├── was/                          # WAS Server (Windows)
│   └── start_monitoring.ps1
├── client/                       # Client (macOS)
│   └── run_k6.sh
├── prometheus/
├── grafana/
└── results/                      # 결과 저장
```

---

## 9. 트러블슈팅

### k6가 WAS에 연결 안됨

```bash
ping 192.168.0.58
curl http://192.168.0.58:8080/actuator/health
```

### InfluxDB에 메트릭이 안들어감

```bash
curl http://192.168.0.58:8086/ping
```

### 토큰 만료

```bash
python3 generate_tokens.py --url http://192.168.0.58:8080 --output tokens.csv
```

---

**Source**: Consolidated from `docs/plans/` and `docs/perf-tuning/`
