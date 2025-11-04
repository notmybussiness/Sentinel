# Sentinel Performance Test Experiments

> **Mac에서 실행** | **LF 변환 완료** | **중복 제거 완료**
>
> **Last Updated**: 2025-11-03

---

## 🎯 실험 개요

총 **4개 Phase** (중복 제거 완료):
1. **Phase 0**: Baseline (기본 성능 측정)
2. **Phase 1**: Stock API (외부 API 부하 테스트)
3. **Phase 2**: SSE (실시간 스트리밍 부하 테스트)
4. **Phase 3**: Portfolio (N+1 쿼리 문제 탐지)

---

## 📋 사전 준비

### 1. 환경 확인
```bash
# JMeter 설치 확인
jmeter --version

# Backend 실행 (PC1: 192.168.0.58)
cd backend
./gradlew bootRun

# PostgreSQL 실행 (PC2: 192.168.0.5)
docker-compose ps
```

### 2. Mac에서 테스트 준비
```bash
# jmeter 폴더로 이동
cd /path/to/Sentinel/backend/jmeter

# 스크립트 실행 권한 확인 (이미 설정됨)
ls -l *.sh
# -rwxr-xr-x 표시 확인
```

---

## 🧪 Experiment 1: Phase 0 - Baseline

### 목적
- **전체 API 기본 성능 파악**
- 병목 지점 사전 발견
- 이후 Phase 비교 기준선 확보

### 테스트 대상 API (7개)
1. POST `/api/v1/auth/dev-login` - 인증
2. GET `/api/v1/portfolios` - 포트폴리오 목록
3. GET `/api/v1/market/indices` - 주요 지수
4. GET `/api/v1/crypto/price/BTC` - BTC 가격
5. GET `/api/v1/crypto/price/ETH` - ETH 가격
6. GET `/api/v1/market/search?query=AAPL` - 종목 검색
7. GET `/api/v1/crypto/trending` - 트렌딩 암호화폐

### 실행 방법
```bash
# 1명 (Warm-up)
./run-baseline.sh 1

# 10명 (Light load)
./run-baseline.sh 10

# 50명 (Medium load)
./run-baseline.sh 50

# 100명 (Heavy load)
./run-baseline.sh 100
```

### 파라미터
- **users**: 동시 사용자 수 (기본값: 1)
- **loops**: 각 사용자당 반복 횟수 (고정: 100)

### 측정 지표
- **Response Time**: 평균, 중앙값, 95%ile
- **Throughput**: req/sec (TPS)
- **Error Rate**: %
- **Backend Memory**: MB

### 예상 결과
```
Users: 1
├─ Avg Response Time: 50ms
├─ Throughput: 20 req/sec
└─ Error Rate: 0%

Users: 10
├─ Avg Response Time: 120ms
├─ Throughput: 80 req/sec
└─ Error Rate: 0%

Users: 100
├─ Avg Response Time: 500ms
├─ Throughput: 150 req/sec
└─ Error Rate: 2%
```

### 결과 확인
```bash
# HTML 리포트
open results/baseline-10users-*-report/index.html

# JTL 파일 (CSV)
cat results/baseline-10users-*.jtl | head -20
```

---

## 🧪 Experiment 2: Phase 1 - Stock API

### 목적
- **외부 API 호출 성능 측정**
  - 한국투자증권 Open API (우선순위 1)
  - AlphaVantage (Fallback 1)
  - Finnhub (Fallback 2)
- Provider Fallback 전략 테스트
- Rate Limit 영향 파악
- 캐싱 적용 전 Baseline 확보

### Backend 구조
Backend는 **Provider Factory 패턴**을 사용:
```java
// MarketDataService.java
List<MarketDataProvider> availableProviders = providerFactory.getAvailableProviders();

// 우선순위 순으로 시도
for (MarketDataProvider provider : availableProviders) {
    try {
        return provider.getMarketData(symbol);
    } catch (Exception e) {
        // 다음 provider로 Fallback
    }
}
```

**Provider 우선순위**:
1. **KoreaInvestmentProvider** (한국투자증권 Open API)
2. **AlphaVantageProvider**
3. **FinnhubProvider**

### 테스트 대상 API
- GET `/api/v1/market/search?query={symbol}`
  - 테스트 종목: AAPL, GOOGL, MSFT, TSLA, NVDA
  - Random Controller로 랜덤 선택
- GET `/api/v1/market/price/{symbol}`
  - 단일 종목 가격 조회

### 실행 방법
```bash
# 10명, 600 req/min (기본)
./run-stock.sh 10

# 50명, 600 req/min
./run-stock.sh 50

# 50명, 1200 req/min (높은 처리량)
./run-stock.sh 50 1200
```

### 파라미터
- **users**: 동시 사용자 수 (기본값: 10)
- **throughput**: 분당 요청 수 (기본값: 600)
- **loops**: 각 사용자당 반복 횟수 (고정: 100)

### 측정 지표
- **Response Time**: 외부 API 응답 시간 포함
- **External API Calls**: AlphaVantage/Finnhub 호출 횟수
- **Rate Limit Errors**: 429 Too Many Requests

### 주의사항

⚠️ **한국투자증권 Open API**:
- OAuth 2.0 토큰 자동 갱신
- 국내 주식 실시간 시세
- API 문서: https://apiportal.koreainvestment.com

⚠️ **AlphaVantage 무료 플랜 제한** (Fallback):
- 분당 5회
- 일일 100회
- 초과 시 Finnhub로 자동 Fallback

⚠️ **Finnhub 무료 플랜** (Fallback 2):
- 분당 60회
- 일일 무제한

### 예상 결과
```
Users: 10, Throughput: 600
├─ Avg Response Time: 2000ms (외부 API 영향)
├─ Throughput: 25 req/sec
├─ Error Rate: 0%
└─ External API Calls: ~1000회

Users: 50, Throughput: 1200
├─ Avg Response Time: 5000ms (병목 발생)
├─ Throughput: 40 req/sec
├─ Error Rate: 5% (Rate Limit)
└─ External API Calls: ~2000회
```

### 결과 확인
```bash
# HTML 리포트
open results/stock-50users-*-report/index.html

# 에러 로그 확인
grep "429" results/stock-50users-*.jtl
```

### Provider Fallback 테스트
이 테스트에서 확인할 수 있는 것:
1. **한국투자증권 API 성공** → 가장 빠른 응답 (국내 서버)
2. **한국투자증권 실패** → AlphaVantage로 자동 Fallback
3. **AlphaVantage Rate Limit** → Finnhub로 자동 Fallback
4. **모든 Provider 실패** → 500 에러

**Backend 로그 확인**:
```bash
tail -f logs/application.log | grep "프로바이더"

# 예시 출력:
# [INFO] 프로바이더 KoreaInvestment로 시도 중. 심볼: AAPL
# [INFO] 주식 가격 데이터 조회 성공. 심볼: AAPL, 프로바이더: KoreaInvestment
```

### 최적화 아이디어
이 실험 후 다음 단계에서 최적화 가능:
1. **Redis Cache** (5분 TTL) → API 호출 80% 감소 (이미 구현됨)
2. **Batch Collection** (10분마다 수집) → API 호출 99% 감소
3. **Hybrid** (Cache + WebHook) → 최적의 균형

---

## 🧪 Experiment 3: Phase 2 - SSE Streaming

### 목적
- **Server-Sent Events 부하 테스트**
- Long-lived 연결 메모리 사용량 측정
- 동시 연결 수 한계 파악

### 테스트 대상 API
- GET `/api/v1/crypto/stream/prices?symbols=BTC,ETH,BNB,XRP&method=SSE`
  - 실시간 암호화폐 가격 스트리밍
  - 1초마다 업데이트

### 실행 방법
```bash
# 10 클라이언트, 5분
./run-sse.sh 10 300

# 50 클라이언트, 5분
./run-sse.sh 50 300

# 100 클라이언트, 10분
./run-sse.sh 100 600

# 스트레스 테스트 (500 클라이언트) ⚠️ 메모리 주의
./run-sse.sh 500 300
```

### 파라미터
- **clients**: 동시 SSE 클라이언트 수 (기본값: 10)
- **duration**: 테스트 지속 시간 초 단위 (기본값: 300)

### 측정 지표
- **Connection Success Rate**: 연결 성공률
- **Memory Usage**: 5초마다 자동 수집
- **Connection Drops**: 연결 끊김 횟수
- **Event Throughput**: 초당 이벤트 수

### 메모리 모니터링 (자동)
테스트 실행 중 자동으로 메모리 사용량을 기록합니다:
```bash
# 자동 생성되는 로그 파일
tail -f results/sse-memory-*.log

# 예시:
# 14:30:00 - Memory: 512MB
# 14:30:05 - Memory: 520MB
# 14:30:10 - Memory: 528MB
```

### 예상 결과
```
Clients: 10, Duration: 300s
├─ Avg Response Time: 50ms
├─ Connection Success: 100%
├─ Memory Usage: 500MB → 550MB (+50MB)
└─ Connection Drops: 0

Clients: 50, Duration: 300s
├─ Avg Response Time: 80ms
├─ Connection Success: 100%
├─ Memory Usage: 500MB → 700MB (+200MB)
└─ Connection Drops: 0

Clients: 100, Duration: 300s
├─ Avg Response Time: 150ms
├─ Connection Success: 98%
├─ Memory Usage: 500MB → 1200MB (+700MB)
└─ Connection Drops: 2

Clients: 500, Duration: 300s ⚠️ 스트레스
├─ Avg Response Time: 800ms
├─ Connection Success: 80%
├─ Memory Usage: 500MB → 3000MB (+2500MB)
└─ Connection Drops: 100
```

### 결과 확인
```bash
# HTML 리포트
open results/sse-50clients-*-report/index.html

# 메모리 사용량 추이
cat results/sse-memory-*.log

# 메모리 그래프 (gnuplot이 있다면)
gnuplot -e "plot 'results/sse-memory-*.log' using 2 with lines"
```

### 메모리 누수 체크
```bash
# 테스트 전 메모리
curl -s http://192.168.0.58:8080/actuator/metrics/jvm.memory.used | jq

# 테스트 실행
./run-sse.sh 100 300

# 테스트 후 10분 대기 (GC 발생)
sleep 600

# 테스트 후 메모리
curl -s http://192.168.0.58:8080/actuator/metrics/jvm.memory.used | jq

# 비교:
# 100MB 증가 = 정상 (연결 메타데이터)
# 500MB+ 증가 = 메모리 누수 의심
```

---

## 🧪 Experiment 4: Phase 3 - Portfolio N+1

### 목적
- **N+1 쿼리 문제 탐지**
- JOIN FETCH 최적화 효과 측정
- PostgreSQL 쿼리 실행 횟수 비교

### N+1 문제란?
```java
// N+1 Problem:
// 1. Portfolio 조회: 1 query
List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);

// 2. 각 Portfolio의 Holdings 조회: N queries
for (Portfolio p : portfolios) {
    p.getHoldings(); // Lazy loading → 개별 쿼리 발생
}

// 총 쿼리 수: 1 + N = 11 (Portfolio 10개인 경우)
```

### 테스트 대상 API
- GET `/api/v1/portfolios` - 포트폴리오 + Holdings 목록

### 실행 방법

#### Step 1: Baseline (N+1 문제 있음)
```bash
# 50명, baseline 모드
./run-portfolio.sh 50 baseline
```

#### Step 2: 코드 최적화
```java
// PortfolioRepository.java
@Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.holdings WHERE p.userId = :userId")
List<Portfolio> findByUserIdWithHoldings(@Param("userId") Long userId);
```

#### Step 3: Optimized (N+1 해결)
```bash
# 50명, optimized 모드
./run-portfolio.sh 50 optimized
```

#### Step 4: 결과 비교
```bash
# Query 통계 비교
diff results/portfolio-baseline-queries-*.txt results/portfolio-optimized-queries-*.txt
```

### 파라미터
- **users**: 동시 사용자 수 (기본값: 10)
- **mode**: baseline 또는 optimized (기본값: baseline)
- **loops**: 각 사용자당 반복 횟수 (고정: 100)

### 측정 지표
- **Response Time**: API 응답 시간
- **Query Count**: PostgreSQL 쿼리 실행 횟수 ⭐
- **Query Mean Time**: 쿼리 평균 실행 시간
- **Throughput**: req/sec

### PostgreSQL 통계 (자동 수집)
테스트 전후로 `pg_stat_statements`에서 쿼리 통계를 자동 수집합니다:
```bash
# 자동 생성되는 파일
cat results/portfolio-baseline-queries-*.txt

# 예시:
#   query                                    | calls | mean_ms | total_ms
# -------------------------------------------+-------+---------+----------
#  SELECT * FROM portfolios WHERE user_id=? |   50  |   5.2   |  260.0
#  SELECT * FROM holdings WHERE portfolio=?  |  500  |   2.1   | 1050.0  # N+1!
```

### 예상 결과

#### Baseline (N+1 문제)
```
Users: 50, Mode: baseline
├─ Avg Response Time: 200ms
├─ Throughput: 100 req/sec
├─ Query Count: 550 (50 Portfolio + 500 Holdings)
├─ Query Mean Time: 2.5ms
└─ Total Query Time: 1375ms
```

#### Optimized (JOIN FETCH)
```
Users: 50, Mode: optimized
├─ Avg Response Time: 80ms (60% 향상 ✅)
├─ Throughput: 200 req/sec (100% 향상 ✅)
├─ Query Count: 50 (단일 쿼리)
├─ Query Mean Time: 8.0ms
└─ Total Query Time: 400ms (71% 감소 ✅)
```

### 성능 개선 효과
| 지표 | Baseline | Optimized | 개선율 |
|------|----------|-----------|--------|
| Response Time | 200ms | 80ms | **60% 감소** ✅ |
| Throughput | 100 TPS | 200 TPS | **100% 증가** ✅ |
| Query Count | 550 | 50 | **91% 감소** ✅ |
| Total Query Time | 1375ms | 400ms | **71% 감소** ✅ |

### 결과 확인
```bash
# HTML 리포트 비교
open results/portfolio-baseline-50users-*-report/index.html
open results/portfolio-optimized-50users-*-report/index.html

# Query 통계 비교
echo "=== Baseline ==="
cat results/portfolio-baseline-queries-*.txt

echo "=== Optimized ==="
cat results/portfolio-optimized-queries-*.txt
```

### PostgreSQL에서 직접 확인
```bash
# PostgreSQL 접속
PGPASSWORD=sentinel_password psql -h 192.168.0.5 -U sentinel -d sentinel

# 쿼리 통계 확인
SELECT
    LEFT(query, 80) as query_preview,
    calls,
    ROUND(mean_exec_time::numeric, 2) as mean_ms,
    ROUND(total_exec_time::numeric, 2) as total_ms
FROM pg_stat_statements
WHERE query LIKE '%portfolio%' OR query LIKE '%holding%'
ORDER BY calls DESC
LIMIT 20;
```

---

## 📊 전체 실험 순서 (권장)

### 1일차: Baseline 측정
```bash
# Phase 0: 전체 API 기본 성능
./run-baseline.sh 1
./run-baseline.sh 10
./run-baseline.sh 50

# Phase 1: 외부 API 성능 (Cache 전)
./run-stock.sh 10
./run-stock.sh 50
```

### 2일차: 실시간 통신 테스트
```bash
# Phase 2: SSE 부하 테스트
./run-sse.sh 10 300
./run-sse.sh 50 300
./run-sse.sh 100 300
```

### 3일차: DB 최적화
```bash
# Phase 3: N+1 문제 측정
./run-portfolio.sh 50 baseline

# 코드 최적화 (JOIN FETCH 적용)

# Phase 3: 최적화 효과 측정
./run-portfolio.sh 50 optimized

# 결과 비교
diff results/portfolio-baseline-queries-*.txt \
     results/portfolio-optimized-queries-*.txt
```

---

## 🔧 트러블슈팅

### 1. Backend 연결 실패
```bash
# 확인
curl http://192.168.0.58:8080/actuator/health

# 해결
# - Backend 재시작: cd backend && ./gradlew bootRun
# - 방화벽 확인
# - 포트 사용 중 확인
```

### 2. PostgreSQL 연결 실패
```bash
# 확인
PGPASSWORD=sentinel_password psql -h 192.168.0.5 -U sentinel -d sentinel -c "SELECT 1"

# 해결
# - PostgreSQL 재시작: docker-compose up -d
# - pg_hba.conf 확인 (192.168.0.0/24 허용)
```

### 3. JMeter OutOfMemoryError
```bash
# 힙 메모리 증가
export HEAP="-Xms1g -Xmx4g"
./run-baseline.sh 100
```

### 4. pg_stat_statements 미설치
```bash
# PostgreSQL 접속
PGPASSWORD=sentinel_password psql -h 192.168.0.5 -U sentinel -d sentinel

# Extension 설치
CREATE EXTENSION pg_stat_statements;

# 통계 확인
SELECT COUNT(*) FROM pg_stat_statements;
```

---

## 📁 결과 파일 구조

```
results/
├── baseline-10users-20251103_123456.jtl
├── baseline-10users-20251103_123456-report/
│   └── index.html
├── stock-50users-20251103_130000.jtl
├── stock-50users-20251103_130000-report/
├── sse-50clients-20251103_140000.jtl
├── sse-50clients-20251103_140000-report/
├── sse-memory-20251103_140000.log             # 메모리 모니터링
├── portfolio-baseline-50users-20251103_150000.jtl
├── portfolio-baseline-queries-20251103_150000.txt  # PostgreSQL 통계
├── portfolio-optimized-50users-20251103_151000.jtl
└── portfolio-optimized-queries-20251103_151000.txt
```

---

## 📈 결과 해석 가이드

### Response Time
- **< 100ms**: 우수 ✅
- **100-500ms**: 양호 ⚠️
- **500-1000ms**: 느림 (최적화 필요)
- **> 1000ms**: 매우 느림 ❌

### Throughput (TPS)
- **> 1000 TPS**: 고성능
- **500-1000 TPS**: 중간 성능
- **100-500 TPS**: 일반 성능
- **< 100 TPS**: 병목 발생

### Error Rate
- **0%**: 완벽 ✅
- **< 1%**: 허용 가능
- **1-5%**: 주의 필요 ⚠️
- **> 5%**: 심각한 문제 ❌

### PostgreSQL Query Count (N+1)
- **1 쿼리**: 이상적 ✅
- **< 10 쿼리**: 양호
- **10-100 쿼리**: N+1 의심 ⚠️
- **> 100 쿼리**: N+1 확실 ❌

---

**작성일**: 2025-11-03
**위치**: `backend/jmeter/EXPERIMENTS.md`
**Mac 실행**: LF 변환 완료 ✅
**중복 제거**: 완료 ✅
