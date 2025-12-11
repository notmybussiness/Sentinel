# Learning Log - 2025-12-05

## 🆕 새로 배운 것

### Virtual Thread가 항상 정답은 아니다

**Virtual Thread (Project Loom)**의 핵심 개념:
- Java 21부터 공식 지원
- Platform Thread와 달리 OS Thread를 1:1 매핑 안함
- 수백만 개 생성 가능 (경량)
- Blocking I/O 시 다른 Virtual Thread 실행

**Spring Boot 3.2+ 지원**:
```yaml
spring:
  threads:
    virtual:
      enabled: true  # 자동으로 VirtualThreadPerTaskExecutor 적용
```

### Virtual Thread가 도움이 되는 경우 vs 안되는 경우

#### ✅ 도움이 되는 경우
1. **외부 API 호출이 많음**
   ```java
   // 여러 API 동시 호출
   CompletableFuture.allOf(
       callPaymentAPI(),    // 500ms
       callShippingAPI(),   // 300ms
       callEmailAPI()       // 200ms
   ).join();

   // Platform Thread: 1초 동안 Thread Blocked
   // Virtual Thread: I/O 대기 중 다른 작업 처리 ✅
   ```

2. **Thread Pool Exhaustion**
   ```
   요청 1000개 동시 도착
   Platform Thread Pool: 200개 → 800개 대기 (병목)
   Virtual Thread: 1000개 생성 → 모두 처리 ✅
   ```

3. **Long-lived Connection (WebSocket, SSE)**
   ```java
   // 클라이언트 1000명 접속
   // Platform Thread: 1000개 필요 (메모리 부족)
   // Virtual Thread: 수천 개 가능 ✅
   ```

#### ❌ 도움이 안되는 경우 (현재 프로젝트)

1. **Connection Pool이 병목인 경우**
   ```
   Virtual Thread: 무제한 동시성
   HikariCP: 300개 제한
   Redis Pool: 100개 제한

   → Virtual Thread가 아무리 많아도 Connection 대기
   → 오히려 리소스 고갈 (327개 대기, 70% 에러)
   ```

2. **DB 작업이 대부분인 경우**
   ```java
   @Cacheable("portfolios")
   public PortfolioDto getPortfolio(Long id) {
       // 1. Redis 조회 (Blocking I/O)
       // 2. DB 조회 (Blocking I/O)
       // 3. DTO 변환 (CPU)

       // → DB/Redis 작업이 90% 이상
       // → Virtual Thread가 대기해도 Connection 안놓음
       // → Platform Thread와 차이 없음 ❌
   }
   ```

3. **단일 모노리스 아키텍처**
   ```
   현재: WAS → DB (단순 구조)
   Virtual Thread 효과: 거의 없음

   마이크로서비스: Service A → Service B → Service C
   Virtual Thread 효과: 큼 (서비스 간 HTTP 호출 많음)
   ```

---

## 🐛 해결한 문제

### Problem: Virtual Thread 적용 시 500 에러 폭증

**증상**:
- 500 에러율: 0% → 52.5% → 70.6%
- 평균 응답 시간: 40.82ms → 240.64ms → 199.14ms (5배 느려짐)
- TPS: 217.47 → 110.71 → 101.19 (53% 감소)
- HikariCP 대기: 8.26 → 125.05 → 103.18 (최대 327개)

**환경**:
```yaml
# Before (Platform Thread)
server:
  tomcat:
    threads:
      max: 200  # 최대 200개 동시 처리

# After (Virtual Thread)
spring:
  threads:
    virtual:
      enabled: true  # 무제한 동시 처리
```

### Root Cause 분석

#### 1차 실험 (Redis Pool 8개)
```
Virtual Thread: 수천 개 생성 가능
Redis Pool: 8개만 사용 가능
max-wait: 2000ms

동시 요청 → Redis 8개 초과 → 2초 대기 → TimeoutException → 500 에러 (52.5%)
```

#### 2차 실험 (Redis Pool 100개로 증설)
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8 → 100
          max-wait: 2000ms → 5000ms
```

**결과**: 500 에러 더 악화 (52.5% → 70.6%)

**원인**:
- HikariCP 300개도 부족 (최대 327개 대기)
- Virtual Thread가 동시 요청을 너무 많이 생성
- DB Connection Pool 부족 → 대기 → TimeoutException

### Solution: Platform Thread로 복구

**결론**: **Virtual Thread는 현재 아키텍처에 맞지 않음**

```yaml
# application-perf.yml 복구
spring:
  threads:
    virtual:
      enabled: false  # 비활성화 (또는 삭제)

server:
  tomcat:
    threads:
      max: 200
      min-spare: 50
```

### Learned

#### 1. Virtual Thread ≠ 만능 해결책

**Virtual Thread가 해결하는 것**:
- Thread Pool Exhaustion
- I/O 대기 시간 (Non-blocking I/O 스타일로 동작)

**Virtual Thread가 해결 못하는 것**:
- Connection Pool 제약
- Blocking I/O (JDBC, HikariCP)
- DB 쿼리 성능

#### 2. Connection Pool이 진짜 병목

```
Platform Thread 200개:
  → 동시 요청 최대 200개
  → HikariCP 300개로 충분
  → Redis Pool 8개도 충분
  → 안정적 ✅

Virtual Thread 무제한:
  → 동시 요청 수천 개
  → HikariCP 300개 부족
  → Redis Pool 100개도 부족
  → 리소스 고갈 🔴
```

#### 3. 아키텍처에 따라 기술 선택

| 아키텍처 | Virtual Thread 효과 |
|---------|-------------------|
| **단일 모노리스 + DB 중심** | ❌ 효과 없음 (현재) |
| **마이크로서비스 + HTTP 호출** | ✅ 효과 큼 |
| **이벤트 기반 (Kafka)** | ✅ 효과 큼 |
| **WebSocket/SSE 많음** | ✅ 효과 큼 |

#### 4. 측정의 중요성

**추측**: "Virtual Thread는 최신 기술이니까 더 빠를 것이다"
**측정**:
- 평균 응답 시간: 40.82ms → 199.14ms (5배 느려짐)
- 500 에러: 0% → 70.6%
- TPS: 217.47 → 101.19 (53% 감소)

**결론**: **추측하지 말고 측정하라**

---

## 📚 참고한 자료

### 공식 문서
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Boot 3.2 Release Notes - Virtual Threads](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.2-Release-Notes#virtual-threads)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)

### 블로그/아티클
- "Virtual Threads in Spring Boot 3.2" - Baeldung
- "When NOT to use Virtual Threads" - Nicolai Parlog

### 실험 데이터
- `metrics_export_pg300/`: Platform Thread (PG 300) - Baseline
- `metrics_export_vthread/`: Virtual Thread + Redis Pool 8
- `metrics_export_vthread2/`: Virtual Thread + Redis Pool 100

---

## 💡 적용 결과

### Platform Thread (최종 선택) ✅

```yaml
# application-perf.yml
server:
  port: 8080
  tomcat:
    threads:
      max: 200
      min-spare: 50
    max-connections: 10000

spring:
  datasource:
    hikari:
      maximum-pool-size: 300

  data:
    redis:
      lettuce:
        pool:
          max-active: 100  # Virtual Thread 실험 중 증설한 것 유지
```

**성능 (Platform Thread + PG 300)**:
- ✅ 평균 응답 시간: **40.82ms** (목표 < 50ms)
- ✅ P95 응답 시간: **149.41ms** (목표 < 150ms)
- ✅ TPS: **217.47 req/s**
- ✅ 500 에러: **0%**
- ✅ HikariCP 대기: **평균 8.26개** (안정적)

### Virtual Thread 실험 결과 (포기)

| 메트릭 | Platform | VThread (Pool 8) | VThread (Pool 100) |
|--------|----------|------------------|-------------------|
| 평균 응답 시간 | 40.82ms | 240.64ms | 199.14ms |
| TPS | 217.47 | 110.71 | 101.19 |
| 500 에러율 | 0% | 52.5% | 70.6% |
| HikariCP 대기 | 8.26 | 125.05 | 103.18 (최대 327) |
| CPU | 14% | 17% | 29% (최대 58%) |

### Git 커밋

```bash
# Virtual Thread 실험 시작
git commit -m "experiment: enable Virtual Threads for Phase 7b"

# Redis Pool 증설
git commit -m "fix: increase Redis pool size for Virtual Thread (8→100)"

# 실험 실패 확인 및 복구
git commit -m "revert: disable Virtual Threads (not suitable for current architecture)"
git commit -m "docs: add learning log for Virtual Thread experiment"
```

---

## 🔜 다음 단계

### 현재 아키텍처 개선 (Virtual Thread 없이)
- [x] PostgreSQL max_connections 증설 (100 → 300)
- [x] HikariCP 증설 (100 → 300)
- [x] Redis 연결 풀 증설 (8 → 100)
- [ ] Cache Aside 패턴 도입 (DB 완전 회피)
- [ ] Query 최적화 (인덱스 확인)

### 미래에 Virtual Thread 재검토할 시나리오
1. **마이크로서비스 전환** 시
   - Service Mesh (Service A → B → C)
   - 서비스 간 HTTP 호출 증가

2. **이벤트 기반 아키텍처** 도입 시
   - Kafka Producer/Consumer
   - 비동기 메시지 처리 증가

3. **외부 API 의존도 증가** 시
   - 실시간 주식/암호화폐 API 호출
   - 외부 결제/배송 API 호출

4. **WebSocket/SSE** 도입 시
   - 실시간 가격 스트리밍
   - Long-lived Connection 관리

**현재는 불필요**: DB 중심 모노리스 아키텍처

---

## 📊 성능 메트릭 비교표

### Phase별 성능 변화

| Phase | 기술 스택 | 평균 응답 시간 | TPS | HikariCP 대기 | 상태 |
|-------|----------|---------------|-----|--------------|------|
| Phase 6 | Caffeine Cache + CP 100 | - | 483 | 0 | ✅ 안정 |
| Phase 7 | Redis Cache + PG 100 | 181ms | 393 | 88.78 | 🔴 부족 |
| Phase 7 (Final) | Redis + PG 300 | **40.82ms** | **217.47** | **8.26** | ✅ 목표 달성 |
| Phase 7b | **Virtual Thread + Pool 8** | 240.64ms | 110.71 | 125.05 | 🔴 실패 |
| Phase 7c | **Virtual Thread + Pool 100** | 199.14ms | 101.19 | 103.18 | 🔴 실패 |

**최종 선택**: **Phase 7 (Platform Thread + PG 300)** ✅

---

## 💭 회고 및 인사이트

### 배운 점

1. **최신 기술 ≠ 항상 정답**
   - Virtual Thread는 Java 21의 킬러 피처
   - 하지만 현재 아키텍처엔 맞지 않음
   - 기술은 도구일 뿐, 상황에 맞게 선택

2. **측정 없는 최적화는 추측일 뿐**
   - "Virtual Thread는 빠를 것이다" → 추측
   - "40ms → 199ms" → 측정
   - 데이터가 진실을 말한다

3. **병목은 예상 밖에 있다**
   - 예상: Thread Pool Exhaustion
   - 실제: Connection Pool 부족
   - 전체 시스템을 봐야 함

4. **실험의 가치**
   - 2시간 투자로 Virtual Thread 부적합 확인
   - 나중에 "Virtual Thread 써볼까?" 질문에 데이터로 답변 가능
   - 실패한 실험도 가치 있음

### 포트폴리오 가치

이 실험은 다음을 증명:
- ✅ 최신 기술 학습 능력 (Virtual Thread)
- ✅ 측정 기반 의사결정
- ✅ 실패를 두려워하지 않는 자세
- ✅ 근본 원인 분석 능력 (Redis Pool → HikariCP)
- ✅ 문서화 습관 (Learning Log)

### 향후 적용

**다음 프로젝트에서**:
- 아키텍처 설계 시 Virtual Thread 고려사항 체크
- Connection Pool 크기 설계 (Thread 수와 균형)
- 실험 → 측정 → 문서화 프로세스 반복

---

**Total Commits Today**: 8
**Lines Changed**: +500 -200
**실험 시간**: 약 3시간
**가치**: 향후 몇 시간의 삽질 방지 ✅
