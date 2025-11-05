# Sentinel 성능 테스트 데이터 생성 가이드

## 📋 사전 준비

### 1. Python 설치 확인
```bash
python --version
# Python 3.13.1 (이미 설치됨)
```

### 2. psycopg2 설치 (이미 완료)
```bash
pip install psycopg2-binary
```

### 3. PC2 PostgreSQL 실행 확인
```bash
# PC2에서 확인
docker-compose ps

# 실행 안 되어 있으면
docker-compose up -d
```

---

## 🚀 사용법

### 기본 실행

```bash
# backend 폴더로 이동
cd C:/Users/zetto/Desktop/Sentinel/backend

# 1000명 유저 + 엣지 케이스 생성
python scripts/generate_test_data.py --users 1000 --mode full
```

### 옵션 설명

#### `--users` : 생성할 유저 수 (기본값: 1000)
```bash
python scripts/generate_test_data.py --users 10      # 10명 (테스트용)
python scripts/generate_test_data.py --users 500     # 500명
python scripts/generate_test_data.py --users 1000    # 1000명 (기본값)
python scripts/generate_test_data.py --users 5000    # 5000명 (대용량)
```

#### `--mode` : 생성 모드 (기본값: full)

**1) normal**: 일반 사용자만 생성
- 각 유저당 1-5개 포트폴리오
- 각 포트폴리오당 2-10개 holdings
```bash
python scripts/generate_test_data.py --users 1000 --mode normal
```

**2) edge**: 엣지 케이스만 생성 (5명)
- 유저1: 포트폴리오 30개 (N+1 문제 테스트)
- 유저2: 포트폴리오 5개, 각 40개 holdings (대용량 테스트)
- 유저3-5: 최소 holdings (경계값 테스트)
```bash
python scripts/generate_test_data.py --mode edge
```

**3) full**: 일반 + 엣지 케이스 모두 생성 (추천 ✅)
```bash
python scripts/generate_test_data.py --users 1000 --mode full
```

#### `--clean` : 기존 테스트 데이터 삭제 후 재생성
```bash
python scripts/generate_test_data.py --users 1000 --mode full --clean
```

⚠️ **주의**: `dev@sentinel.com`은 삭제되지 않습니다.

---

## 📊 실행 예시

### 예시 1: 소량 테스트 (10명)
```bash
python scripts/generate_test_data.py --users 10 --mode full --clean

# 출력:
# ============================================================
#   Sentinel Performance Test Data Generator
# ============================================================
#   Target Users: 10
#   Mode: full
#   Clean: True
# ============================================================
# ✅ PostgreSQL 연결 성공
# 🧹 기존 테스트 데이터 삭제 중...
#   ✅ 삭제 완료: Users 0, Portfolios 0, Holdings 0
#
# 📊 일반 사용자 10명 생성 중...
#   진행률: 10/10 (100%)
#   ✅ 생성 완료: Users 10, Portfolios 32, Holdings 180
#
# 🔥 엣지 케이스 사용자 생성 중...
#   ✅ Case 1: 포트폴리오 30개, Holdings 225개
#   ✅ Case 2: 포트폴리오 5개, Holdings 200개
#   ✅ Case 3: 최소 holdings 사용자 3명
#
# ============================================================
#   📊 최종 통계
# ============================================================
#   총 Users:      15
#   총 Portfolios: 67
#   총 Holdings:   605
#   평균 Portfolios/User: 4.5
#   평균 Holdings/Portfolio: 9.0
# ============================================================
# ✅ 데이터 생성 완료!
```

### 예시 2: 중간 규모 (500명)
```bash
python scripts/generate_test_data.py --users 500 --mode full --clean
```

### 예시 3: 대용량 (5000명) - Phase 3 N+1 테스트용
```bash
python scripts/generate_test_data.py --users 5000 --mode full --clean

# 예상 결과:
# - 총 Users: 5,005명
# - 총 Portfolios: ~15,000개
# - 총 Holdings: ~135,000개
```

---

## 🧪 JMeter와 연동

### Phase 3: Portfolio N+1 테스트에서 활용

```bash
# 1. 대량 데이터 생성
python scripts/generate_test_data.py --users 5000 --mode full --clean

# 2. JMeter 테스트 실행
cd jmeter
./run-portfolio.sh 50 baseline

# 3. 결과 확인
cat results/portfolio-baseline-queries-*.txt

# 예상 결과 (N+1 문제):
#   query                                    | calls  | mean_ms | total_ms
# -------------------------------------------+--------+---------+----------
#  SELECT * FROM portfolios WHERE user_id=? |   50   |   5.2   |  260.0
#  SELECT * FROM holdings WHERE portfolio=?  |  5000  |   2.1   | 10500.0  # N+1!
```

---

## 🔧 트러블슈팅

### 1. PostgreSQL 연결 실패
```
psycopg2.OperationalError: connection to server at "192.168.0.5", port 5432 failed
```

**해결책**:
```bash
# PC2에서 PostgreSQL 실행 확인
docker-compose ps

# 방화벽 확인 (Windows PowerShell)
Test-NetConnection -ComputerName 192.168.0.5 -Port 5432

# PostgreSQL 재시작
docker-compose restart
```

### 2. UnicodeEncodeError (한글 출력 오류)
```
UnicodeEncodeError: 'cp949' codec can't encode character
```

**해결책**:
```bash
# 환경 변수 설정 후 실행
set PYTHONIOENCODING=utf-8
python scripts/generate_test_data.py --users 1000 --mode full
```

### 3. psycopg2 미설치
```
ModuleNotFoundError: No module named 'psycopg2'
```

**해결책**:
```bash
pip install psycopg2-binary
```

---

## 📈 데이터 검증

생성된 데이터를 PostgreSQL에서 직접 확인:

```bash
# PC2에서 psql 접속
docker exec -it sentinel-postgres psql -U sentinel -d sentinel

# 통계 조회
SELECT
    (SELECT COUNT(*) FROM users WHERE email LIKE 'testuser%') as users,
    (SELECT COUNT(*) FROM portfolios WHERE user_id IN
        (SELECT id FROM users WHERE email LIKE 'testuser%')) as portfolios,
    (SELECT COUNT(*) FROM portfolio_holdings WHERE portfolio_id IN
        (SELECT p.id FROM portfolios p JOIN users u ON p.user_id = u.id
         WHERE u.email LIKE 'testuser%')) as holdings;

# 엣지 케이스 확인
SELECT u.email, COUNT(p.id) as portfolio_count
FROM users u
LEFT JOIN portfolios p ON u.id = p.user_id
WHERE u.email LIKE 'testuser%'
GROUP BY u.id, u.email
ORDER BY portfolio_count DESC
LIMIT 10;

# 종료
\q
```

---

## 🎯 권장 워크플로우

### 1일차: 소량 테스트
```bash
# 10명으로 스크립트 동작 확인
python scripts/generate_test_data.py --users 10 --mode full --clean

# JMeter baseline 테스트
cd jmeter
./run-baseline.sh 10
```

### 2일차: 중간 규모 테스트
```bash
# 500명 생성
python scripts/generate_test_data.py --users 500 --mode full --clean

# JMeter portfolio 테스트
cd jmeter
./run-portfolio.sh 50 baseline
```

### 3일차: 대용량 N+1 테스트
```bash
# 5000명 생성
python scripts/generate_test_data.py --users 5000 --mode full --clean

# JMeter 300명 테스트
cd jmeter
./run-portfolio.sh 300 baseline

# 최적화 후 비교
./run-portfolio.sh 300 optimized
```

---

## 📝 스크립트 내부 동작

### 생성되는 데이터 구조

```
일반 사용자 (Normal Mode):
├─ testuser00001@test.com
│  ├─ 포트폴리오 1 (5개 holdings: AAPL, GOOGL, BTC, ETH, DOGE)
│  ├─ 포트폴리오 2 (8개 holdings: TSLA, NVDA, SOL, ...)
│  └─ 포트폴리오 3 (3개 holdings: ...)
├─ testuser00002@test.com
│  ├─ 포트폴리오 1 (7개 holdings)
│  └─ 포트폴리오 2 (4개 holdings)
...

엣지 케이스 (Edge Mode):
├─ testuser05001@test.com (Case 1)
│  ├─ 포트폴리오 1-30 (각 5-10개 holdings) → N+1 문제 테스트
├─ testuser05002@test.com (Case 2)
│  ├─ 포트폴리오 1-5 (각 40개 holdings) → 대용량 holdings 테스트
```

### 배치 처리 최적화

- **100명씩 배치 INSERT**: 네트워크 왕복 최소화
- **executemany() 사용**: PostgreSQL 벌크 INSERT
- **트랜잭션 단위 커밋**: 데이터 일관성 보장

---

**작성일**: 2025-11-04
**위치**: `backend/scripts/`
**Python**: 3.13.1
**PostgreSQL**: 15 (Docker)
