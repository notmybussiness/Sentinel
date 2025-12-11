# ADR-001: Redis 분산 캐시를 Kafka EDA보다 우선 구현

## Status
✅ Accepted (2025-12-04)

## Context

### 현재 상황
- **Phase 6 완료**: Caffeine 로컬 캐시로 성능 최적화 달성
  - TPS: 483 req/s
  - Avg Response Time: 155ms
  - Cache Hit Rate: 38-50%
  - HikariCP: 100 connections (Pending 0)

### 기술 부채 및 향후 과제
1. **Kafka EDA 도입 계획**:
   - 목표: 347ms → 10ms (97% 개선)
   - Portfolio 조회 시 외부 API 차단 (비동기 이벤트 기반 아키텍처)

2. **분산 캐시 필요성**:
   - Caffeine은 단일 WAS 전용 (JVM 로컬 메모리)
   - 다중 인스턴스 환경에서 캐시 불일치 문제
   - 수평 확장(Scale-out) 불가

### 의사결정 시점
Kafka EDA 구현 계획을 완료하고 작업을 시작하려던 시점에서 **우선순위 변경 결정**:
> "지금 구현 계획 괜찮은데 redis를 써보는 게 우선일거같아서 일단 브랜치 다시 main에서 새로 redis적용하는거 부터해보자"

## Decision

**Redis 분산 캐시를 Kafka EDA보다 먼저 구현**

### 구현 방식
- **Option 1 선택**: Caffeine 완전 대체 (Redis 단독 사용)
- **Alternative Options**:
  - Option 2: 2-Tier Cache (Caffeine L1 + Redis L2)
  - Option 3: Hybrid (조회 빈도 기반 조건부 캐싱)

### 기술 스택
```yaml
Redis:
  - Version: 7.2-alpine
  - Max Memory: 512MB
  - Eviction Policy: allkeys-lru
  - Persistence: AOF (Append-Only File)
  - Connection Pool: 8 active/idle (Lettuce)
```

### Cache TTL Strategy (Phase 6 결과 반영)
```java
cacheConfigurations.put("stockPrice", 10s);      // 주식 가격
cacheConfigurations.put("cryptoPrice", 1m);      // 암호화폐 가격
cacheConfigurations.put("portfolios", 5s);       // 포트폴리오 (고빈도)
cacheConfigurations.put("trendingCoins", 1m);    // 트렌딩 코인
cacheConfigurations.put("marketIndices", 1m);    // 시장 지수
cacheConfigurations.put("cryptoSearch", 3m);     // 검색 결과
cacheConfigurations.put("stockSearch", 3m);      // 검색 결과
```

## Consequences

### Positive ✅

#### 1. 분산 시스템 실전 경험
- **포트폴리오 가치**: Redis 분산 캐시 구축 경험
- **학습 목표**: 네트워크 기반 캐싱, 직렬화, 캐시 일관성 문제 해결
- **실무 스킬**: 대부분의 프로덕션 환경이 분산 캐시 사용

#### 2. 수평 확장 가능성 확보
- **Before**: Caffeine → 단일 WAS만 가능
- **After**: Redis → 여러 WAS에서 캐시 공유
- **효과**: 트래픽 증가 시 서버 추가만으로 대응 가능

#### 3. 캐시 영속성
- **Before**: WAS 재시작 시 캐시 소실
- **After**: Redis AOF 활성화로 재시작 후에도 캐시 유지
- **효과**: Cold Start 시간 단축

#### 4. 단순한 구현
- **Option 1 선택**: Redis 단독 사용
- **이점**: 2-Tier 대비 복잡도 낮음, 디버깅 용이
- **Trade-off 명확**: 성능 vs 확장성

### Negative ❌

#### 1. 네트워크 지연 오버헤드
- **Caffeine**: 0ms (JVM 메모리 직접 접근)
- **Redis**: 0.5-2ms (네트워크 왕복 시간)
- **예상 영향**:
  - TPS: 483 → ~430-460 req/s (-5~-10%)
  - Avg Response Time: 155ms → 157-162ms (+1-2ms per cache operation)

#### 2. 인프라 복잡도 증가
- **추가 구성 요소**: Redis 서버 (Docker)
- **운영 부담**: 모니터링, 장애 대응, 메모리 관리
- **로컬 개발**: Docker 필수 (추가 리소스 소비)

#### 3. 직렬화/역직렬화 오버헤드
- **Caffeine**: Java 객체 그대로 저장
- **Redis**: JSON 직렬화 필요 (GenericJackson2JsonRedisSerializer)
- **CPU 사용량**: 약간 증가 예상

### Neutral ⚙️

#### 1. Kafka EDA 계획 보존
- **상태**: Kafka 구현 계획을 `.claude/plans/ticklish-popping-mist.md`에 저장
- **향후**: Redis 테스트 완료 후 Kafka 작업 재개 가능
- **브랜치 분리**: `feature/redis-cache` (Redis), `feat/phase7-kafka-eda` (Kafka)

#### 2. 성능 Trade-off 수용
- **허용 기준**: ~5-10% 성능 저하 수용
- **이유**: 분산 시스템 경험 및 확장성이 더 중요
- **검증 계획**: k6로 실제 측정 후 최종 판단

## Why Redis First? (왜 Redis가 우선인가?)

### 1. 학습 순서 측면
```mermaid
Caffeine (로컬) → Redis (분산) → Kafka (이벤트)
  단순          중간 복잡도      고급
```
- **점진적 난이도**: 캐싱 → 분산 캐싱 → 이벤트 기반 아키텍처
- **기초 다지기**: Redis 경험이 Kafka 이해에 도움 (메시지 직렬화, 분산 시스템 개념)

### 2. 리스크 관리
- **Redis 실패 시**: Caffeine으로 롤백 가능 (단순 설정 변경)
- **Kafka 실패 시**: 전체 아키텍처 재설계 필요 (영향 범위 큼)
- **검증 용이성**: k6 테스트로 성능 측정 즉시 가능

### 3. 포트폴리오 우선순위
- **면접 빈출 주제**: "분산 캐시 경험이 있나요?" → Redis
- **Kafka는 고급 주제**: 주니어/미드레벨에서는 Redis 경험이 더 중요
- **실무 활용도**: Redis는 거의 모든 회사에서 사용, Kafka는 대규모 시스템

### 4. 즉시 적용 가능성
- **Redis**: 기존 코드 최소 변경 (CacheConfig만 수정)
- **Kafka**: 전체 아키텍처 변경 (Controller, Service, Scheduler 재설계)
- **검증 시간**: Redis 1일 vs Kafka 1주일

## Alternatives Considered

### Alternative 1: Kafka EDA 먼저 구현
**장점**:
- 347ms → 10ms의 극적인 성능 개선 (97%)
- 이벤트 기반 아키텍처 경험 획득
- 비동기 처리 패턴 학습

**단점**:
- 구현 복잡도 높음 (Controller, Service, Event 전체 재설계)
- 실패 시 롤백 어려움
- Redis 경험 없이 Kafka로 건너뛰는 것은 학습 순서 비효율

**선택하지 않은 이유**:
- **학습 곡선**: 분산 캐시 경험 없이 이벤트 스트리밍은 너무 급격한 도약
- **리스크**: Kafka 실패 시 대안이 없음 (Redis는 Caffeine으로 롤백 가능)
- **실무 우선순위**: Redis 경험이 취업 시장에서 더 보편적

### Alternative 2: Redis와 Kafka 동시 작업
**장점**:
- 빠른 진행 속도

**단점**:
- 두 가지 큰 변경을 동시에 검증하기 어려움
- 성능 문제 발생 시 원인 파악 불가 (Redis? Kafka? 둘 다?)
- 브랜치 충돌 가능성

**선택하지 않은 이유**:
- **검증 불가능**: 성능 개선/저하의 원인을 특정할 수 없음
- **베스트 프랙티스**: 한 번에 하나의 변수만 변경

### Alternative 3: 현상 유지 (Caffeine 계속 사용)
**장점**:
- 성능 최고 (0ms latency)
- 추가 인프라 불필요

**단점**:
- 분산 시스템 경험 부족
- 수평 확장 불가
- 포트폴리오 다양성 부족

**선택하지 않은 이유**:
- **학습 목표**: 분산 시스템 경험이 필수
- **실무 대비**: 프로덕션 환경은 대부분 분산 캐시 사용

## Implementation Plan

### Phase 7a: Redis 구현 (현재 진행 중)
- [x] docker-compose.yml 작성 (Redis 7.2-alpine)
- [x] application-perf.yml 설정 (Redis 연결 정보)
- [x] CacheConfig.java 재작성 (Caffeine → Redis)
- [x] build.gradle 수정 (Caffeine 의존성 제거)
- [x] 코드 컴파일 및 커밋
- [ ] Docker Desktop 시작 및 Redis 컨테이너 실행
- [ ] Redis 연결 테스트
- [ ] Backend 실행 (perf profile)
- [ ] k6 성능 테스트 및 Phase 6 비교
- [ ] 성능 저하가 예상 범위(-5~-10%) 내인지 검증
- [ ] ADR 업데이트 (실제 측정값 반영)

### Phase 7b: Kafka EDA (Redis 검증 후)
- `.claude/plans/ticklish-popping-mist.md` 계획 참조
- Event Model 설계
- Producer/Consumer 구현
- 트랜잭션 분리
- 성능 테스트 (347ms → 10ms 검증)

## Expected Results

### 성능 예측 (Redis)
```
Baseline (Phase 6 - Caffeine):
- TPS: 483 req/s
- Avg: 155ms
- P95: ~300ms
- Cache Hit Rate: 38-50%

Expected (Phase 7a - Redis):
- TPS: 430-460 req/s (-5~-10%)
- Avg: 157-162ms (+1-2ms per cache op)
- P95: ~305-310ms
- Cache Hit Rate: 유지 (38-50%)
```

### 성능 예측 (Kafka - 향후)
```
Current (Phase 6):
- Portfolio 조회: 347ms (API 호출 포함)

Expected (Phase 7b - Kafka):
- Portfolio 조회: ~10ms (DB만 조회)
- 가격 업데이트: 백그라운드 이벤트
- 예상 개선: 97%
```

## Success Criteria

### Redis 구현 성공 기준
1. **기능 동작**: Redis 캐시 정상 작동 (Hit/Miss 로그 확인)
2. **성능 허용 범위**: TPS 저하가 10% 이하
3. **안정성**: 에러율 0% 유지
4. **인프라**: Docker 컨테이너 안정 운영

### 실패 시 대응
- **성능 저하 > 10%**: Option 2 (2-Tier Cache) 재검토
- **Redis 불안정**: Caffeine 롤백
- **학습 목표 달성**: 실패하더라도 분산 캐시 경험 획득

## References
- Kafka EDA 계획: `.claude/plans/ticklish-popping-mist.md`
- Phase 6 결과: `scripts/SCALABILITY_REPORT.md`
- Redis vs Caffeine 비교: `CacheConfig.java` (주석 참조)
- 브랜치: `feature/redis-cache`
- 관련 커밋: `1040ab4` (feat(phase7): Replace Caffeine with Redis distributed cache)

## Migration Back Plan
만약 Redis 성능이 기대에 못 미칠 경우:

### Rollback to Caffeine
```bash
git checkout feature/phase4a-read-write-separation
# 또는
git revert 1040ab4
```

### Option 2: 2-Tier Cache 전환
```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory redisFactory) {
    // L1: Caffeine (빠른 로컬 캐시)
    CaffeineCacheManager caffeineManager = new CaffeineCacheManager();

    // L2: Redis (분산 캐시)
    RedisCacheManager redisManager = RedisCacheManager.builder(redisFactory).build();

    // 2-Tier 구성
    return new CompositeCacheManager(caffeineManager, redisManager);
}
```

---

**Date**: 2025-12-04
**Author**: Backend Development Team
**Branch**: `feature/redis-cache`
**Status**: 🚧 Implementation In Progress (Docker Desktop startup pending)
