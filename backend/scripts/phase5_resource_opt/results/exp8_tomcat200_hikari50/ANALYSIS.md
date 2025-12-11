# exp8 분석 결과: Tomcat 200 Platform Threads + HikariCP 50

**실험 일시**: 2025-11-26 17:04:00 ~ 17:12:45 KST
**워크로드**: External API 위주 (Market 60%, Crypto 30%, Search 10%)
**부하**: 500 VUsers (k6)
**설정**:
- Virtual Threads: **disabled**
- Tomcat Threads: **200** (Platform Threads)
- HikariCP Pool: **50**

---

## 📊 핵심 결과 요약

### ✅ Success Rate: **80.0%**

```
200 Success:  9,176건 (80.0%)
201 Created:  2,230건 (19.4%)
400 Bad Req:     68건 (0.6%)
500 Error:        1건 (0.0%)
```

**평가**: 양호 (500 에러 거의 없음)

---

## 🚨 Critical Finding: HikariCP Pool 병목

### Connection Pool 상태

```
Active Connections:
  Average: 43.4 / 50 (86.7%)
  Max:     50 / 50 (100.0%)  ← Pool 완전 고갈
  Min:     0

Pending Requests:
  Average: 122.7개
  Max:     149개              ← Connection 대기 중인 요청

Pool Usage:
  Average: 86.7%
  Max:     100.0%             ← 병목 발생
```

### 🔍 문제 분석

**1. Pool Size 부족**:
- 평균 사용률 86.7% → 여유 없음
- 최대 100% → 완전 고갈
- 평균 122개의 요청이 Connection 대기

**2. Tomcat Thread vs HikariCP 불균형**:
- Tomcat Platform Threads: **200개**
- HikariCP Pool: **50개**
- 비율: 4:1 (Threads가 4배 많음)

**결과**:
- 200개의 Thread 중 최대 50개만 DB 접근 가능
- 나머지 150개는 Connection 대기
- **HikariCP가 병목지점**

---

## ⚡ Performance Metrics

### Throughput

```
Average TPS: 37.5 req/s
Max TPS:     201.2 req/s
```

**분석**:
- External API 워크로드 (I/O Bound)
- TPS가 낮은 이유:
  1. External API 응답 지연 (Market Mock, Upbit Real)
  2. HikariCP Pending으로 인한 대기 시간
  3. 500 VUs 부하에 비해 낮은 처리량

### Response Time

```
Average: 394.2 ms
Max:     654.1 ms
Min:     25.1 ms
```

**분석**:
- 평균 394ms는 External API 호출 시간
- Max 654ms는 HikariCP 대기 + External API
- Min 25ms는 Cache Hit 또는 빠른 응답

---

## 💻 Resource Usage

### CPU

```
Average: 15.4%
Max:     22.8%
```

**평가**: 매우 낮음 (I/O Bound 워크로드)
- CPU 병목 아님
- External API 대기로 인한 Idle 시간 많음

### JVM Threads

```
Average: 239개
Max:     254개
```

**구성**:
- Tomcat Threads: 200개
- Scheduler/GC/etc: ~54개

**평가**: Platform Threads 환경에서 안정적

### Heap Memory

```
Average: 79.2 MB
Max:     230.0 MB
```

**평가**: 메모리 압박 없음 (정상)

---

## 📈 Platform Threads vs Virtual Threads 비교

### Platform Threads (현재 - exp8)

✅ **장점**:
- Tomcat Thread Pool이 동시성 제한 → 시스템 보호
- CPU/메모리 사용 안정적
- Context Switching 오버헤드 제한적

❌ **단점**:
- HikariCP Pending 149개 발생 (병목)
- Pool 사용률 100% → Connection 대기
- TPS 37.5 req/s (낮음)

### Virtual Threads (Phase 5 실패)

❌ **문제**:
- 500 VUs가 동시에 HikariCP 요청
- Pending 428개 발생 (Platform의 2.9배)
- HTTP Success Rate 0% → 서비스 중단

---

## 🔬 Root Cause Analysis

### 왜 HikariCP가 병목인가?

**1. External API 워크로드의 특성**:
```
Market API 60%: Mock API (빠름, 하지만 DB 조회 필요)
Crypto API 30%: Upbit Real (느림, Cache/DB 조회)
Search API  10%: AlphaVantage (느림, Cache/DB 조회)
```

**2. DB Connection 사용 패턴**:
- 모든 요청이 DB 접근 (Portfolio, Holdings 조회)
- External API 호출 중에도 Connection 유지
- Long-lived Connection (응답 시간 평균 394ms)

**3. Pool Size vs Thread 불균형**:
```
요청 흐름:
500 VUs → Tomcat 200 Threads → HikariCP 50 Pool
                              ↑
                           병목 지점
```

---

## 💡 해결 방안

### 1️⃣ **HikariCP Pool Size 증가** (⭐ 최우선)

**현재**: 50개
**권장**: **100개** (Tomcat 200의 50%)

**근거**:
- Pending 122개 평균 → 최소 50+122 = 172개 필요
- 여유율 고려 → 200개의 50% = 100개
- 안전 마진 확보

**기대 효과**:
- Pending → 0
- Pool 사용률 < 80%
- TPS 증가 (대기 시간 감소)

### 2️⃣ **Connection Timeout 조정**

**현재**:
```yaml
hikari:
  connection-timeout: 30000  # 30초
```

**권장**: 유지 (적절함)
- 30초는 External API 타임아웃 고려 시 적절
- 너무 짧으면 요청 실패

### 3️⃣ **Async Processing 고려** (장기)

External API 호출을 비동기로 처리:
```java
@Async
CompletableFuture<Price> getPrice(String symbol)
```

**효과**:
- DB Connection 빨리 반환
- Pool 회전율 증가
- 동일 Pool Size로 더 많은 요청 처리

---

## 🎯 Next Steps

### **Step 1: Pool Size 실험**

**exp8a - HikariCP 75**:
```yaml
hikari:
  maximum-pool-size: 75
  minimum-idle: 15
```

**exp8b - HikariCP 100**:
```yaml
hikari:
  maximum-pool-size: 100
  minimum-idle: 20
```

**exp8c - HikariCP 150** (공격적):
```yaml
hikari:
  maximum-pool-size: 150
  minimum-idle: 30
```

**측정 지표**:
- HikariCP Pending (목표: 0)
- Pool Usage (목표: < 80%)
- TPS 증가율
- Response Time 감소

### **Step 2: Virtual Threads 재검토**

HikariCP 100으로 최적화 후:
- Virtual Threads 재활성화
- I/O Bound 워크로드에서 효과 검증
- Pool 병목 해결 후 Virtual Threads의 진짜 장점 확인

---

## 📊 실험 비교 (예상)

| Metric | exp8 (Pool 50) | exp8b (Pool 100) 예상 |
|--------|----------------|------------------------|
| HikariCP Pending | 149 | **0** |
| Pool Usage | 100% | **50~60%** |
| TPS | 37.5 | **60~80** (60% ↑) |
| Avg Response Time | 394ms | **250~300ms** (25% ↓) |
| Success Rate | 80% | **99%+** |

---

## 📝 Lessons Learned

### 1. **Pool Size는 Thread Pool과 균형 맞춰야 함**

```
Tomcat Threads : HikariCP Pool = 2:1 ~ 1:1
현재 200:50 (4:1) → 불균형
권장 200:100 (2:1)
```

### 2. **I/O Bound 워크로드의 특성**

- CPU는 낮음 (15%)
- Thread는 많이 필요 (200개 사용)
- **Connection Pool이 병목**

### 3. **External API 호출 패턴**

- Long-lived Connection (평균 394ms 유지)
- Pool 회전율 낮음
- → 더 큰 Pool Size 필요

### 4. **Platform Threads의 한계**

- Tomcat 200개 제한 → 동시성 상한
- Virtual Threads보다 안정적이지만 확장성 낮음
- Pool Size 최적화가 더 중요

---

## 🎯 결론

### ✅ 실험 성공

정확한 시간대 데이터로 **유의미한 결과** 확인:
- Success Rate 80% (양호)
- **HikariCP가 명확한 병목**
- Pool Size 증가 필요성 입증

### 🚀 다음 작업

1. **HikariCP Pool Size 100으로 증가**
2. **exp8 재실행 후 비교**
3. **Pending = 0 달성 확인**
4. **Virtual Threads 재검토**

---

**생성 일시**: 2025-11-26 17:20 KST
**다음 작업**: HikariCP Pool Size를 100으로 증가 후 exp8 재실행
