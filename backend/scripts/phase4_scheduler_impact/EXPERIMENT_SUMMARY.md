# Phase 4: 실험 설계 및 결과 요약

## 🧪 실험 조건

### 공통 조건
- **Backend**: Spring Boot (포트 8080)
- **부하 도구**: k6
- **부하 프로파일**: 500 VU (Virtual Users)
- **데이터**: Portfolio ~7,000개, Holdings ~48,000개 (추정)
- **측정 도구**: Prometheus + Grafana, Scouter

### 실험 A: Scheduler 30초 주기
```
시간: 2025-11-25 22:37~22:46 (KST) = 9분
Scheduler: @Scheduled(fixedRate = 30000, initialDelay = 10000)
예상 실행: 약 16-18번 (30초마다)
```

### 실험 B: Scheduler 180초(3분) 주기
```
시간: 2025-11-25 23:23~23:40 (KST) = 17분
Scheduler: @Scheduled(fixedRate = 180000, initialDelay = 10000)
예상 실행: 약 5-6번 (3분마다)
```

---

## 📊 수집된 메트릭

### 1. Application 메트릭
- ✅ `avg_response_time.csv` - 평균 응답시간
- ✅ `p95_response_time.csv` - P95 응답시간
- ✅ `tps.csv` - 처리량 (Transactions Per Second)
- ✅ `status_code_distribution.csv` - HTTP 상태 코드 분포

### 2. JVM 메트릭
- ✅ `active_threads.csv` - JVM 활성 스레드
- ✅ `jvm_memory.csv` - JVM 메모리 사용량
- ✅ `cpu_usage.csv` - CPU 사용률

### 3. Database 메트릭
- ✅ `hikaricp_active.csv` - DB 활성 커넥션
- ✅ `hikaricp_pending.csv` - DB 대기 커넥션
- ✅ `hikaricp_usage_percent.csv` - DB 커넥션 사용률
- ✅ `hikaricp_acquire_time_p95.csv` - DB 커넥션 획득 시간 (P95)

---

## 🔍 지금까지 발견한 것

### ✅ 확인된 사실
1. **SQL Count: 5번** (Scouter XLog)
   - N+1 문제 아님
   - EntityGraph/Fetch Join이 작동 중

2. **SQL Time: 50~100ms**
   - SQL 자체는 빠름
   - 병목 아님

3. **응답시간: 390~450ms**
   - SQL 시간을 제외하면 **290~400ms가 미스터리**

4. **Active Threads: 233~254개** 🔥
   - Tomcat max-threads 한계에 근접
   - **스레드 부족 가능성!**

---

## 🎯 다음 확인 단계

### Step 1: Tomcat 설정 확인
```yaml
# application.yml
server:
  tomcat:
    threads:
      max: ???    # 현재 설정이 얼마인지?
      min-spare: ???
```

### Step 2: HikariCP 상태 분석
- Connection Pool이 충분한지?
- Connection 대기 시간은?

### Step 3: 병목 구간 특정
- Scouter Method Profile 확인
- 어느 메서드가 스레드를 오래 점유하는지?

---

## 💡 현재 가설

### 가설 1: 스레드 풀 고갈 🔥 가능성 높음
```
500 VU 요청 → Tomcat 스레드 250개 필요
각 요청이 500ms 소요 → 스레드가 오래 점유됨
→ 새 요청이 스레드 대기
→ 응답시간 증가
```

### 가설 2: DTO 변환 오버헤드
```
Portfolio → DTO 변환 시
- Holdings 순회
- BigDecimal 연산
- Stream 처리
→ CPU 소비 많음
```

### 가설 3: DB Connection 대기
```
500 VU → 많은 동시 DB 요청
HikariCP Pool 부족 → 대기 시간 발생
```

---

## 📋 검증 순서

1. Tomcat max-threads 설정 확인
2. HikariCP 메트릭 분석
3. Scouter Method Profile 분석
4. 필요시 설정 튜닝
