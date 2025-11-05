# JMeter 성능 테스트 가이드

> **⚠️ 주의**: 이 설정은 `perf-test` 브랜치에서만 사용하세요!
>
> 운영 환경(main, dev)에서는 절대 사용 금지!

---

## 📋 목차

1. [환경 설정](#1-환경-설정)
2. [데이터베이스 준비](#2-데이터베이스-준비)
3. [Backend 실행](#3-backend-실행)
4. [토큰 CSV 생성](#4-토큰-csv-생성)
5. [JMeter 설정](#5-jmeter-설정)
6. [테스트 실행](#6-테스트-실행)
7. [정리 작업](#7-정리-작업)

---

## 1. 환경 설정

### Git Branch 확인

```bash
# 현재 브랜치 확인
git branch

# perf-test 브랜치로 이동
git checkout perf-test
```

### 필수 도구 확인

```bash
# Java 21
java -version

# PostgreSQL 실행 중
docker ps | grep sentinel-postgres

# Python 3.x (토큰 CSV 생성용)
python --version
pip install requests
```

---

## 2. 데이터베이스 준비

### 2.1. PostgreSQL 접속

```bash
# Docker 컨테이너 접속
docker exec -it sentinel-postgres psql -U sentinel -d sentinel

# 또는 로컬 psql
psql -U sentinel -d sentinel -h localhost -p 5432
```

### 2.2. 테스트 유저 500명 생성

```bash
# SQL 스크립트 실행
psql -U sentinel -d sentinel -h localhost -p 5432 -f scripts/perf-test-users.sql
```

**예상 출력:**
```sql
NOTICE:  ✅ 성능 테스트 유저 생성 완료: 500 명
 total_users | first_user_id | last_user_id
-------------+---------------+--------------
         500 |           101 |          600
```

### 2.3. 유저 생성 확인

```sql
-- 총 유저 수 확인
SELECT COUNT(*) FROM users WHERE email LIKE 'perftest%@sentinel.com';

-- 샘플 유저 확인
SELECT id, email, nickname, oauth_provider
FROM users
WHERE email LIKE 'perftest%@sentinel.com'
ORDER BY id
LIMIT 10;
```

---

## 3. Backend 실행

### 3.1. perf 프로파일로 실행

**CMD/PowerShell** (Git Bash 불가):

```bash
cd backend
gradlew bootRun --args="--spring.profiles.active=perf"
```

### 3.2. 실행 확인

```bash
# Health Check
curl http://localhost:8080/actuator/health

# 성능 테스트 유저 통계
curl http://localhost:8080/api/v1/auth/perf/stats
```

**예상 응답:**
```json
{
  "totalUsers": 500,
  "expectedUsers": 500,
  "status": "READY",
  "message": "성능 테스트 준비 완료"
}
```

---

## 4. 토큰 CSV 생성

### 4.1. Python 스크립트 실행

```bash
cd backend/scripts
python generate_jmeter_tokens.py
```

**예상 출력:**
```
============================================================
JMeter 성능 테스트용 토큰 CSV 생성
============================================================

🔍 서버 상태 확인 중...
✅ 서버 연결 성공
   총 유저: 500/500
   상태: READY
   메시지: 성능 테스트 준비 완료

🔄 토큰 발급 API 호출 중...
✅ 토큰 발급 완료: 500개

🔄 CSV 파일 생성 중: jmeter_tokens.csv
✅ CSV 파일 생성 완료: jmeter_tokens.csv
   총 500개 토큰 저장

✅ 완료!
```

### 4.2. CSV 파일 확인

```bash
# 파일 생성 확인
ls -lh jmeter_tokens.csv

# 내용 확인 (처음 5줄)
head -n 5 jmeter_tokens.csv
```

**CSV 형식:**
```csv
userId,email,nickname,accessToken,refreshToken
101,perftest001@sentinel.com,PerfUser_001,eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...,eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
102,perftest002@sentinel.com,PerfUser_002,eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...,eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 5. JMeter 설정

### 5.1. CSV Data Set Config 추가

1. **Thread Group** 우클릭 → **Add** → **Config Element** → **CSV Data Set Config**

2. 설정:
   ```
   Filename: C:\Users\zetto\Desktop\Sentinel\backend\scripts\jmeter_tokens.csv
   File Encoding: UTF-8
   Variable Names: userId,email,nickname,accessToken,refreshToken
   Delimiter: ,
   Recycle on EOF: True
   Stop thread on EOF: False
   Sharing mode: All threads
   ```

### 5.2. HTTP Header Manager 추가

1. **Thread Group** 우클릭 → **Add** → **Config Element** → **HTTP Header Manager**

2. 헤더 추가:
   ```
   Name: Authorization
   Value: Bearer ${accessToken}
   ```

### 5.3. HTTP Request 설정 예시

```
Protocol: http
Server Name: localhost
Port: 8080
Method: GET
Path: /api/v1/portfolios
```

---

## 6. 테스트 실행

### 6.1. 단일 유저 테스트 (Dry Run)

```
Thread Group 설정:
- Number of Threads: 1
- Ramp-Up Period: 1
- Loop Count: 1
```

**실행 → 결과 확인**

### 6.2. 부하 테스트

```
Thread Group 설정:
- Number of Threads: 100  (동시 사용자 100명)
- Ramp-Up Period: 10      (10초 동안 순차 시작)
- Loop Count: 10          (각 유저당 10회 요청)

총 요청 수: 100 * 10 = 1,000회
```

### 6.3. 모니터링

```bash
# Backend 로그 확인
tail -f logs/application.log

# PostgreSQL 커넥션 확인
docker exec -it sentinel-postgres psql -U sentinel -d sentinel -c \
  "SELECT count(*) FROM pg_stat_activity WHERE datname='sentinel';"
```

---

## 7. 정리 작업

### 7.1. 테스트 유저 삭제 (선택사항)

```sql
-- 성능 테스트 유저 삭제
DELETE FROM users WHERE email LIKE 'perftest%@sentinel.com';

-- 확인
SELECT COUNT(*) FROM users WHERE email LIKE 'perftest%@sentinel.com';
```

### 7.2. Git 정리

```bash
# 변경사항 커밋
git add .
git commit -m "perf: JMeter 성능 테스트 인프라 구축"

# dev 브랜치로 복귀
git checkout dev
```

---

## 📊 성능 지표 확인

### JMeter Listeners 추천

1. **View Results Tree** - 개별 요청 확인
2. **Summary Report** - 전체 통계
3. **Aggregate Report** - 상세 통계
4. **Response Time Graph** - 응답 시간 그래프

### 주요 지표

- **Throughput**: 초당 처리량 (requests/sec)
- **Average Response Time**: 평균 응답 시간 (ms)
- **90% Line**: 90% 응답 시간 (P90)
- **Error %**: 오류율 (%)

---

## 🔧 트러블슈팅

### 문제 1: "서버 연결 실패"

```bash
# Backend 실행 확인
curl http://localhost:8080/actuator/health

# perf 프로파일 확인
# application-perf.yml이 로드되었는지 로그 확인
```

### 문제 2: "토큰 발급 실패"

```sql
-- 유저 수 확인
SELECT COUNT(*) FROM users WHERE email LIKE 'perftest%@sentinel.com';

-- 500명 미만이면 SQL 스크립트 재실행
```

### 문제 3: "JWT 인증 실패"

```bash
# 토큰 만료 확인 (24시간)
# application-perf.yml: jwt.access-token-expiration: 86400000

# 토큰 재생성
python generate_jmeter_tokens.py
```

### 문제 4: "자동 데이터 수집 실행됨"

```bash
# @Profile("!perf") 확인
# CryptoDataCollectorService.java
# IndexDataCollectorService.java

# perf 프로파일로 실행되었는지 확인
# 로그에 "⚠️ [PERF TEST] 간소화된 Security 설정 활성화" 메시지 확인
```

---

## 📌 주요 변경 사항

### 비활성화된 기능 (perf 프로파일)

- ✅ 자동 암호화폐 데이터 수집 (5분마다)
- ✅ 자동 인덱스 데이터 수집 (10분마다)
- ✅ AI 분석 (Gemini)
- ✅ WebSocket 스트리밍
- ✅ Redis 캐싱

### 간소화된 기능

- ✅ JWT 토큰만 검증 (DB 조회 생략)
- ✅ UserDetailsService 미사용
- ✅ 최소 로깅

### 최적화된 설정

- ✅ Tomcat 쓰레드 풀: 200
- ✅ 커넥션: 10,000
- ✅ 타임아웃 단축
- ✅ 토큰 만료 시간 연장 (24시간)

---

## 📞 문의

문제가 발생하면:
1. Backend 로그 확인
2. JMeter 로그 확인
3. PostgreSQL 로그 확인

---

**작성일**: 2025-01-05
**버전**: 1.0.0
**브랜치**: perf-test
