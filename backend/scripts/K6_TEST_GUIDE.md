# K6 Performance Test 작성 가이드

> **목적**: 성능 테스트 스크립트 작성 시 일관성 유지 및 체크리스트 제공
> **Last Updated**: 2025-12-04

---

## 📁 디렉토리 구조

```
scripts/
├── K6_TEST_GUIDE.md                    # 이 파일
├── common/
│   ├── tokens.csv                      # 사용자 인증 토큰 (generate_tokens.py로 생성)
│   └── k6/
│       └── utils.js                    # 공통 유틸 함수
├── phase{N}_{name}/                    # Phase별 실험 디렉토리
│   ├── README.md                       # 실험 개요 및 목표
│   ├── ANALYSIS.md                     # 실험 결과 분석
│   ├── tests/
│   │   └── exp{N}_{description}.js    # k6 테스트 스크립트
│   └── results/
│       ├── exp{N}_summary.json         # k6 결과 JSON
│       └── COMPARISON_REPORT.md        # Before/After 비교
└── templates/
    └── k6_template.js                  # 스크립트 템플릿
```

### 파일 명명 규칙

| 항목 | 규칙 | 예시 |
|------|------|------|
| Phase 디렉토리 | `phase{N}_{name}` | `phase7_redis_cache` |
| 테스트 스크립트 | `exp{N}_{description}.js` | `exp11_redis_baseline.js` |
| 결과 파일 | `exp{N}_summary.json` | `exp11_summary.json` |

---

## 📝 k6 스크립트 필수 구조

### 1. 헤더 주석 (필수)

```javascript
/**
 * Experiment {N} - {Title}
 *
 * Phase {N}: {Phase Name}
 * 목표: {테스트 목표 명시}
 *
 * Test Scenario:
 *   - {시나리오 상세}
 *
 * Expectation:
 *   - {예상 결과}
 *
 * ✅ PRE-TEST CHECKLIST:
 *   [ ] Backend 실행 (perf 프로파일): ./gradlew bootRun --args='--spring.profiles.active=perf'
 *   [ ] Docker 컨테이너 실행: docker-compose up -d (PostgreSQL, Redis 등)
 *   [ ] 토큰 파일 생성: python scripts/common/generate_tokens.py
 *   [ ] {추가 체크 항목}
 *
 * 📊 POST-TEST CHECKLIST:
 *   [ ] k6 결과 저장: --out json=results/exp{N}_summary.json
 *   [ ] 메트릭 확인: TPS, P95 Response Time, Error Rate
 *   [ ] {도메인별 메트릭}: Cache Hit Rate, DB Connection Pool, etc.
 *   [ ] ANALYSIS.md 업데이트
 */
```

### 2. Import (필수)

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Rate, Trend, Counter } from 'k6/metrics';
```

### 3. Configuration (필수)

```javascript
// Configuration
const BASE_URL = 'http://localhost:8080';
const TOKENS_FILE = '../../common/tokens.csv';

// Custom Metrics
const errorRate = new Rate('errors');
const {domainSpecific}Trend = new Trend('{metric_name}_duration');
```

### 4. Load Tokens (필수)

```javascript
const tokens = new SharedArray('tokens', function () {
    const data = open(TOKENS_FILE).split('\n').slice(1);
    return data.map(line => {
        const [userId, email, nickname, accessToken, refreshToken] = line.split(',');
        return { userId, accessToken: accessToken ? accessToken.trim() : '' };
    }).filter(t => t.accessToken);
});
```

### 5. Options (필수)

```javascript
export const options = {
    stages: [
        { duration: '30s', target: 100 },  // Ramp-up
        { duration: '2m', target: 100 },   // Sustain
        { duration: '30s', target: 0 },    // Ramp-down
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'],  // 목표치 설정
        'errors': ['rate<0.01'],
    },
};
```

### 6. Helper Functions (권장)

```javascript
function getHeaders(token) {
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

function getRandomElement(array) {
    return array[Math.floor(Math.random() * array.length)];
}
```

### 7. Main Function (필수)

```javascript
export default function () {
    const user = getRandomElement(tokens);

    // Test logic here

    sleep(0.1);  // Think time
}
```

---

## 🎯 Domain별 체크리스트

### Cache 테스트 (Redis/Caffeine)

**PRE-TEST**:
- [ ] Redis 컨테이너 실행 확인: `docker ps | grep redis`
- [ ] Redis 연결 확인: `redis-cli ping`
- [ ] 캐시 초기화: `redis-cli FLUSHALL` (선택적)
- [ ] `perf` 프로파일 확인: `spring.cache.type=redis`

**POST-TEST**:
- [ ] Cache Hit Rate 측정 (응답 시간 기반)
- [ ] Redis 메모리 사용량: `redis-cli INFO memory`
- [ ] Redis 키 개수: `redis-cli DBSIZE`
- [ ] TTL 확인: `redis-cli TTL {cache_key}`

**Custom Metrics**:
```javascript
const cacheHitRate = new Rate('cache_hits');
const cacheMissRate = new Rate('cache_misses');

// 응답 시간 기준 (예: 10ms 미만 = Cache Hit)
if (res.timings.duration < 10) {
    cacheHitRate.add(1);
} else {
    cacheMissRate.add(1);
}
```

### Database 테스트 (HikariCP)

**PRE-TEST**:
- [ ] PostgreSQL 실행 확인: `docker ps | grep postgres`
- [ ] DB 연결 확인: `psql -h localhost -U sentinel -d sentinel`
- [ ] Connection Pool 설정 확인: `application-perf.yml`

**POST-TEST**:
- [ ] Prometheus Metrics 확인: `http://localhost:8080/actuator/prometheus`
- [ ] HikariCP Active Connections
- [ ] HikariCP Pending Connections
- [ ] DB Slow Query 로그

### API 테스트 (External API)

**PRE-TEST**:
- [ ] API Rate Limit 확인 (AlphaVantage, Upbit 등)
- [ ] Mock Server 여부 확인
- [ ] Circuit Breaker 설정 확인

**POST-TEST**:
- [ ] API 호출 횟수 (Rate Limit 초과 여부)
- [ ] External API 응답 시간
- [ ] Circuit Breaker 상태 (OPEN/CLOSED)

---

## 🚀 실행 방법

### 1. Backend 실행 (perf 프로파일)

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=perf'
```

### 2. 토큰 생성 (최초 1회)

```bash
cd scripts
python common/generate_tokens.py
```

생성 결과: `scripts/common/tokens.csv`

### 3. k6 테스트 실행

```bash
cd scripts/phase{N}_{name}/tests
k6 run exp{N}_{description}.js --out json=../results/exp{N}_summary.json
```

### 4. 결과 확인

```bash
# JSON 결과 확인
cat ../results/exp{N}_summary.json | jq '.metrics'

# Prometheus Metrics 확인
curl http://localhost:8080/actuator/prometheus | grep hikari
```

---

## 📊 주요 메트릭 정의

| 메트릭 | 설명 | 목표치 |
|--------|------|--------|
| `http_req_duration` | HTTP 요청 응답 시간 (P95) | < 500ms |
| `http_reqs` | 총 요청 수 (TPS) | > 100 req/s |
| `errors` | 에러율 | < 1% |
| `cache_hits` | 캐시 히트율 | > 80% |
| `hikaricp_connections_active` | DB Active Connections | < Max Pool Size |
| `hikaricp_connections_pending` | DB Pending Connections | 0 |

---

## 🛠️ 트러블슈팅

### 1. 토큰 파일 없음

```
Error: open ../../common/tokens.csv: no such file or directory
```

**해결**: `python scripts/common/generate_tokens.py` 실행

### 2. Backend 연결 실패

```
Error: dial tcp 127.0.0.1:8080: connect: connection refused
```

**해결**: Backend가 perf 프로파일로 실행 중인지 확인

### 3. Redis 연결 실패

```
Error: Redis connection refused
```

**해결**: `docker-compose up -d` 실행 후 `redis-cli ping` 확인

### 4. 403 Unauthorized

```
Error: 403 Forbidden
```

**해결**: 토큰 만료. `generate_tokens.py` 재실행

---

## 📚 참고 문서

- [k6 Documentation](https://k6.io/docs/)
- [k6 Metrics](https://k6.io/docs/using-k6/metrics/)
- [Prometheus Metrics](http://localhost:8080/actuator/prometheus)
- [Phase별 실험 결과](./phase{N}_{name}/ANALYSIS.md)

---

## ✅ Quick Checklist (모든 테스트 공통)

```bash
# 1. Docker 실행
docker-compose up -d

# 2. Backend 실행 (perf 프로파일)
cd backend && ./gradlew bootRun --args='--spring.profiles.active=perf'

# 3. 토큰 생성 (최초 1회)
cd scripts && python common/generate_tokens.py

# 4. k6 테스트 실행
cd phase{N}_{name}/tests
k6 run exp{N}_{description}.js --out json=../results/exp{N}_summary.json

# 5. 결과 분석
# - TPS, P95, Error Rate 확인
# - Domain-specific metrics 확인
# - ANALYSIS.md 업데이트
```

---

**Last Updated**: 2025-12-04
**Author**: Claude Code
**Version**: 1.0
