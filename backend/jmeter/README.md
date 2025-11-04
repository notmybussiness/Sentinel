# Sentinel JMeter Performance Tests

> **목적**: 성능 실험을 위한 JMeter 테스트 시나리오
> **실행 방식**: CLI 전용 (메모리 효율, 고성능)
> **Mac 실행**: LF 변환 완료 ✅
> **최종 업데이트**: 2025-11-03

---

## 📦 파일 구조

```
jmeter/
├── README.md                    # 이 문서 (빠른 참조)
├── EXPERIMENTS.md               # ⭐ 실험 상세 설명 (필독!)
│
├── common-functions.sh          # 공통 함수 모듈
│
├── test1-baseline.jmx           # Phase 0: 전체 API 베이스라인
├── test2-stock.jmx              # Phase 1: 주식 API (외부 API 호출)
├── test3-sse.jmx                # Phase 2: Server-Sent Events
├── test4-websocket.jmx          # Phase 2: WebSocket (플러그인 필요)
├── test5-longpoll.jmx           # Phase 2: Long Polling
├── test6-portfolio.jmx          # Phase 3: 포트폴리오 N+1 쿼리
│
├── run-baseline.sh              # Phase 0 실행 스크립트
├── run-stock.sh                 # Phase 1 실행 스크립트
├── run-sse.sh                   # Phase 2 실행 스크립트
├── run-portfolio.sh             # Phase 3 실행 스크립트
│
└── results/                     # 테스트 결과 (자동 생성)
    ├── *.jtl                    # CSV 결과 파일
    ├── *-report/                # HTML 리포트
    ├── *-memory.log             # 메모리 사용량 로그
    └── *-queries.txt            # PostgreSQL 쿼리 통계
```

---

## 🎯 4가지 Phase (중복 제거 완료)

1. **Phase 0**: Baseline - 전체 API 기본 성능
2. **Phase 1**: Stock API - 외부 API 부하 테스트
3. **Phase 2**: SSE - 실시간 스트리밍
4. **Phase 3**: Portfolio - N+1 쿼리 문제 탐지

**각 실험 상세 설명**: `EXPERIMENTS.md` 참고 ⭐

---

## 🚀 빠른 시작 (Mac)

### 1. JMeter 설치

```bash
# Mac에서 JMeter 설치
brew install jmeter

# 설치 확인
jmeter --version
```

### 2. 환경 준비

**Backend 서버 시작** (PC1: 192.168.0.58):
```bash
cd backend
./gradlew bootRun
```

**PostgreSQL 시작** (PC2: 192.168.0.5):
```bash
docker-compose up -d
```

### 3. 테스트 실행 (Mac)

```bash
# jmeter 폴더로 이동
cd /path/to/Sentinel/backend/jmeter

# Phase 0: Baseline (10명)
./run-baseline.sh 10

# Phase 1: Stock API (50명)
./run-stock.sh 50

# Phase 2: SSE (50 클라이언트, 5분)
./run-sse.sh 50 300

# Phase 3: Portfolio N+1 (50명, baseline 모드)
./run-portfolio.sh 50 baseline
```

**각 Phase 상세 설명**: `EXPERIMENTS.md` 참고 ⭐

---

## 📊 테스트 시나리오 요약

### Phase 0: Baseline Test (`run-baseline.sh`)

**실행**:
```bash
./run-baseline.sh [users]

# 예제
./run-baseline.sh 1      # 1명
./run-baseline.sh 10     # 10명
./run-baseline.sh 100    # 100명
```

**테스트 대상 API**:
- POST /api/v1/auth/dev-login
- GET /api/v1/portfolios
- GET /api/v1/market/indices
- GET /api/v1/crypto/price/BTC
- GET /api/v1/crypto/price/ETH
- GET /api/v1/market/search?query=AAPL
- GET /api/v1/crypto/trending

---

### Phase 1: Stock API Test (`run-stock.sh`)

**실행**:
```bash
./run-stock.sh [users] [throughput]

# 예제
./run-stock.sh 10           # 10명, 600 req/min
./run-stock.sh 50 1200      # 50명, 1200 req/min
```

**특징**:
- AlphaVantage, Finnhub API 호출
- Random Controller로 다양한 종목 검색 (AAPL, GOOGL, MSFT, TSLA, NVDA)
- Throughput 제어 가능

---

### Phase 2: SSE Streaming Test (`run-sse.sh`)

**실행**:
```bash
./run-sse.sh [clients] [duration]

# 예제
./run-sse.sh 10 300     # 10명, 5분
./run-sse.sh 50 300     # 50명, 5분
./run-sse.sh 100 600    # 100명, 10분
```

**특징**:
- Long-lived 연결
- 실시간 암호화폐 가격 스트리밍 (BTC, ETH, BNB, XRP)
- 자동 메모리 모니터링 (`results/sse-memory-*.log`)

**권장 시나리오**:
- Light: 10 clients, 300s
- Medium: 50 clients, 300s
- Heavy: 100 clients, 300s
- Stress: 500 clients, 300s ⚠️

---

### Phase 3: Portfolio N+1 Test (`run-portfolio.sh`)

**실행**:
```bash
./run-portfolio.sh [users] [mode]

# Baseline (N+1 문제)
./run-portfolio.sh 50 baseline

# Optimized (JOIN FETCH)
./run-portfolio.sh 50 optimized
```

**워크플로우**:
1. Baseline 측정: `./run-portfolio.sh 50 baseline`
2. 코드에 JOIN FETCH 적용
3. Optimized 측정: `./run-portfolio.sh 50 optimized`
4. 결과 비교 (`results/portfolio-*-queries-*.txt`)

**자동 수집 데이터**:
- PostgreSQL 쿼리 통계
- 응답 시간
- 쿼리 실행 횟수

---

## 🔧 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `HOST` | 192.168.0.58 | Backend 서버 IP |
| `PG_HOST` | 192.168.0.5 | PostgreSQL IP |
| `PG_PORT` | 5432 | PostgreSQL 포트 |
| `PG_USER` | sentinel | PostgreSQL 사용자 |
| `PG_PASSWORD` | sentinel_password | PostgreSQL 비밀번호 |
| `PG_DB` | sentinel | 데이터베이스 이름 |

**사용 예제**:
```bash
HOST=192.168.1.100 ./run-baseline.sh 10
PG_PASSWORD=mypassword ./run-portfolio.sh 50
```

---

## 📈 결과 확인

### HTML 리포트 열기
```bash
open results/baseline-10users-*-report/index.html
```

### 메모리 로그 확인
```bash
cat results/sse-memory-*.log
```

### PostgreSQL 쿼리 통계
```bash
cat results/portfolio-baseline-queries-*.txt
```

---

## 🆘 트러블슈팅

### Backend 연결 실패
```bash
curl http://192.168.0.58:8080/actuator/health
```

### PostgreSQL 연결 실패
```bash
PGPASSWORD=sentinel_password psql -h 192.168.0.5 -U sentinel -d sentinel -c "SELECT 1"
```

### JMeter OutOfMemoryError
```bash
export HEAP="-Xms1g -Xmx4g"
./run-baseline.sh 100
```

---

## ✅ 체크리스트

- [ ] JMeter 설치 (`jmeter --version`)
- [ ] Backend 실행 중 (192.168.0.58:8080)
- [ ] PostgreSQL 실행 중 (192.168.0.5:5432)
- [ ] 방화벽 포트 열림 (8080, 5432)
- [ ] `results/` 디렉토리 생성

---

**작성일**: 2025-11-03
**버전**: v2.0
**위치**: `backend/jmeter/`
