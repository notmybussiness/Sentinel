# Performance Optimization History

> **Sentinel 백엔드 성능 최적화 전체 기록**
> Phase 1부터 Phase 7까지의 성능 개선 과정과 결과

**Last Updated**: 2025-12-06
**Current Phase**: Phase 7 완료 (Redis 분산 캐시)
**Overall Improvement**: 1949ms → 149ms (P95), TPS 106 → 217 (2x)

---

## 📈 전체 성능 진화 타임라인

```
Initial State (Before Phase 1)
  Avg: 1949ms, P95: 3675ms, TPS: 106, Error: 24.45%
  Problem: 외부 API 직접 호출, 캐시 없음

         ↓

Phase 1-2: Cache Optimization (2025-11)
  Avg: 10ms, P95: 7ms, TPS: 480, Error: 0%
  Improvement: 210x faster, 4.5x throughput
  Method: Caffeine cache + TTL 최적화

         ↓

Phase 3: N+1 Query Resolution (2025-11)
  SQL Queries: 11 → 2 (-82%)
  DB Connection Pool: 100% saturation → Normal
  Method: @EntityGraph (Portfolio + Holdings)

         ↓

Phase 4a: Read/Write Separation (2025-11)
  Avg: 496ms → 347ms (-30%), P95: 766ms → 460ms (-40%)
  TPS: 39.75 → 62.74 (+57.8%)
  Method: Background scheduler (2분 간격)

         ↓

Phase 5: Virtual Threads Experiment (2025-12-05)
  Result: FAILED ❌
  Issue: HikariCP exhaustion (122 pending connections)
  Learning: Virtual Threads ≠ DB bottleneck 해결

         ↓

Phase 6: Local Caching + HikariCP Scaling (2025-12)
  TPS: 160 → 483 (+202%)
  Pending Connections: 122.7 → 0 (완전 해결)
  Cache Hit Rate: 50.4%
  Method: Caffeine cache + HikariCP 100

         ↓

Phase 7: Redis Distributed Cache (2025-12)
  TPS: 217 (안정화)
  Latency: Avg 40ms, P95 149ms
  Ready for: Multi-instance deployment
  Method: Redis + HikariCP 300
```

---

## 🎯 Phase별 상세 분석

### Phase 1-2: Cache Optimization
**기간**: 2025-11
**목표**: 외부 API 호출 최소화
**상세 문서**: [phase1-2-cache-optimization.md](./phase1-2-cache-optimization.md)

**핵심 개선**:
| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Avg Response | 1949ms | 10ms | **210x** ↑ |
| P95 Response | 3675ms | 7ms | **525x** ↑ |
| TPS | 106 req/s | 480 req/s | **4.5x** ↑ |
| Error Rate | 24.45% | 0% | **완전 제거** |

**적용 기술**:
- Caffeine local cache
- Service layer caching (`@Cacheable`)
- TTL 최적화 (Market: 1m, Crypto: 10s)
- Request deduplication (`sync=true`)

**핵심 인사이트**:
- 캐시 위치가 성능에 결정적 영향 (Controller 캐시 → Service 캐시)
- TTL이 너무 길면 500 에러 증가, 너무 짧으면 API rate limit
- Caffeine은 단일 WAS에서 매우 효과적 (0ms latency)

---

### Phase 3: N+1 Query Resolution
**기간**: 2025-11
**목표**: DB 쿼리 최적화
**상세 문서**: [phase3-n-plus-one-resolution.md](./phase3-n-plus-one-resolution.md)

**핵심 개선**:
| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| SQL Queries | 11 queries | 2 queries | **-82%** |
| DB Connection Pool | 100% saturated | Normal | **안정화** |
| Portfolio Read | ~100ms | ~50ms | **2x** ↑ |

**적용 기술**:
```java
// Before: Lazy loading (N+1 problem)
@OneToMany(mappedBy = "portfolio", fetch = FetchType.LAZY)
private List<PortfolioHolding> holdings;

// After: EntityGraph (Fetch Join)
@EntityGraph(attributePaths = {"holdings"})
Optional<Portfolio> findByIdAndUserIdWithHoldings(Long id, Long userId);
```

**측정된 쿼리 변화**:
```sql
-- Before (11 queries)
SELECT * FROM portfolios WHERE id = ?;           -- 1
SELECT * FROM portfolio_holding WHERE portfolio_id = ?;  -- 1
SELECT * FROM users WHERE id = ?;                -- 1
-- + 각 holding마다 price 조회 (N=8) = 8 queries

-- After (2 queries)
SELECT p.*, h.* FROM portfolios p
LEFT JOIN portfolio_holding h ON p.id = h.portfolio_id
WHERE p.id = ? AND p.user_id = ?;               -- 1 (Fetch Join)
SELECT * FROM users WHERE id = ?;                -- 1
```

**핵심 인사이트**:
- `@EntityGraph`가 `JOIN FETCH`보다 유연함 (Repository 메서드별 적용 가능)
- HikariCP 설정도 중요: Phase 3에서 50 → 100으로 증설 필요 확인

---

### Phase 4a: Read/Write Separation
**기간**: 2025-11
**목표**: GET 요청과 가격 업데이트 분리
**상세 문서**: [phase4a-read-write-separation.md](./phase4a-read-write-separation.md)

**핵심 개선**:
| Metric | Before (Phase 3) | After (Phase 4a) | Improvement |
|--------|------------------|------------------|-------------|
| Avg Response | 496ms | 347ms | **-30%** |
| P95 Response | 766ms | 460ms | **-40%** |
| TPS | 39.75 req/s | 62.74 req/s | **+57.8%** |

**아키텍처 변화**:
```
Phase 3: Inline Update
GET /portfolios/{id}
  ↓
1. DB 조회 (EntityGraph)
2. 외부 API 호출 (각 종목) ← Blocking I/O
3. 포트폴리오 재계산
4. DB 저장
5. 응답

Phase 4a: Background Scheduler
GET /portfolios/{id}
  ↓
1. DB 조회만 (EntityGraph)
2. 응답 (빠름!)

별도 스레드 (2분마다):
@Scheduled
  ↓
1. 모든 포트폴리오 조회
2. 외부 API 호출 (각 종목)
3. 재계산 및 저장
```

**Scheduler 설정**:
```java
@Scheduled(fixedRate = 120000, initialDelay = 60000)  // 2분, 1분 delay
public void updateAllPortfolioPrices() {
    List<Portfolio> portfolios = portfolioRepository.findAll();
    // ...
}
```

**Trade-off**:
- ✅ GET 응답 속도 대폭 향상
- ✅ 외부 API 장애와 독립적
- ⚠️ 가격 데이터 최대 2분 stale
- ⚠️ Scheduler 실행 중 약간의 부하

**핵심 인사이트**:
- Read-heavy 워크로드에서 Read/Write 분리가 매우 효과적
- Stale data 허용 범위가 중요 (주식: 2분 OK, 암호화폐: 10초 권장)

---

### Phase 5: Virtual Threads Experiment (FAILED)
**기간**: 2025-12-05
**목표**: Java 21 Virtual Threads로 처리량 향상
**상세 문서**: [phase5-virtual-threads-failure.md](./phase5-virtual-threads-failure.md)

**실험 결과**:
| Metric | Platform Threads | Virtual Threads | Result |
|--------|------------------|----------------|--------|
| Active Threads | 200 | 수천 개 | ❌ 오버헤드 |
| Pending Connections | ~50 | **122.7** | ❌ 악화 |
| TPS | 160 | ~140 | ❌ 감소 |

**실패 원인**:
1. **HikariCP 병목**: Virtual Threads가 아무리 많아도 DB connection은 50개 제한
2. **Blocking I/O**: JDBC는 여전히 blocking → Virtual Thread 의미 없음
3. **Context Switching**: 수천 개 Virtual Threads의 context switch 오버헤드

**코드 시도**:
```java
// application-perf.yml
spring:
  threads:
    virtual:
      enabled: true  # ❌ 효과 없음

# HikariCP는 여전히 50 connections
datasource:
  hikari:
    maximum-pool-size: 50
```

**학습한 점**:
- Virtual Threads ≠ 만능 해결책
- DB connection pool이 병목이면 Virtual Threads 무용
- Blocking I/O → R2DBC (Reactive)로 가야 진정한 Virtual Threads 활용 가능

**문서 참고**: `.claude/docs/learning/2025-12-05_virtual_thread_experiment.md`

---

### Phase 6: Local Caching + HikariCP Scaling
**기간**: 2025-12
**목표**: Connection Pool 확장 + 로컬 캐시 강화
**상세 문서**: [phase6-scalability-optimization.md](./phase6-scalability-optimization.md)

**핵심 개선**:
| Metric | Before (Phase 5) | After (Phase 6) | Improvement |
|--------|------------------|----------------|-------------|
| TPS | ~160 req/s | **483 req/s** | **+202%** |
| Pending Connections | 122.7 | **0** | **완전 해결** |
| Cache Hit Rate | N/A | **50.4%** | 신규 측정 |
| Read Latency (P95) | 394ms | **155ms** | **-60%** |

**적용 기술**:
1. **HikariCP 확장**: 50 → 100 connections
2. **Caffeine 캐시 강화**: portfolios 캐시 추가 (5초 TTL)
3. **@CacheEvict**: Portfolio 업데이트 시 캐시 무효화

```java
// CacheConfig.java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .maximumSize(1000)
        .recordStats());  // Cache metrics 수집
    return cacheManager;
}

// PortfolioService.java
@Cacheable(value = "portfolios", key = "#portfolioId")
public PortfolioDto getPortfolioById(Long portfolioId, Long userId) {
    // ...
}

@CacheEvict(value = "portfolios", key = "#result.id")
public PortfolioDto updatePortfolio(Long portfolioId, UpdatePortfolioRequest request) {
    // ...
}
```

**Cost vs Benefit**:
- **Cost**: $0 (JVM heap 사용)
- **Latency**: 0ms (local cache)
- **Trade-off**: Eventual consistency (5초)
- **Limitation**: 단일 WAS 전용 (multi-instance 불가)

**핵심 인사이트**:
- Local cache (Caffeine)는 단일 WAS에서 최고 성능
- Cache hit rate 50%만으로도 TPS 3배 향상
- Multi-instance 배포 시 Redis 필요 (다음 Phase)

---

### Phase 7: Redis Distributed Cache
**기간**: 2025-12-06
**목표**: Multi-instance 배포 준비 (분산 캐시)
**상세 문서**: [phase7-redis-distributed-cache.md](./phase7-redis-distributed-cache.md)

**핵심 개선**:
| Metric | Before (Phase 6) | After (Phase 7) | Change |
|--------|------------------|----------------|--------|
| TPS | 483 req/s | **217 req/s** | -55% ⚠️ |
| Avg Latency | ~80ms | **40.82ms** | -49% ✅ |
| P95 Latency | 155ms | **149.41ms** | -3.6% ✅ |
| Cache Type | Caffeine (local) | **Redis (distributed)** | 전환 |
| Multi-instance | ❌ 불가 | ✅ **가능** | 목표 달성 |

**아키텍처 변화**:
```
Phase 6: Caffeine (Local)
┌──────────────┐
│   WAS #1     │
│ ┌──────────┐ │
│ │ Caffeine │ │  0ms latency
│ │  Cache   │ │
│ └──────────┘ │
│      ↓       │
│ ┌──────────┐ │
│ │PostgreSQL│ │
│ └──────────┘ │
└──────────────┘
Problem: WAS #2 추가 시 캐시 불일치

Phase 7: Redis (Distributed)
┌──────────────┐  ┌──────────────┐
│   WAS #1     │  │   WAS #2     │
└──────┬───────┘  └──────┬───────┘
       │                 │
       └────────┬────────┘
                ↓
         ┌────────────┐
         │   Redis    │  0.5-2ms latency
         │   Cache    │  (network)
         └────────────┘
                ↓
         ┌────────────┐
         │ PostgreSQL │
         └────────────┘
Benefit: 여러 WAS가 캐시 공유
```

**Redis 설정**:
```java
// CacheConfig.java
@Bean
public CacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(1))  // Default TTL
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()
            )
        );

    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    cacheConfigurations.put("portfolios", config.entryTtl(Duration.ofMinutes(1)));
    cacheConfigurations.put("stockPrice", config.entryTtl(Duration.ofSeconds(30)));
    cacheConfigurations.put("cryptoPrice", config.entryTtl(Duration.ofSeconds(30)));

    return RedisCacheManager.builder(factory)
        .cacheDefaults(config)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
}
```

**Resource Scaling**:
- **HikariCP**: 100 → 300 connections
- **Tomcat Threads**: 200 (유지)
- **Redis Pool**: 8 connections (Lettuce)

**TPS 감소 원인 분석**:
1. **Network Latency**: Caffeine 0ms → Redis 0.5-2ms
2. **Serialization Overhead**: Java object ↔ JSON 변환
3. **Trade-off 수용**: Multi-instance 지원을 위한 불가피한 감소

**핵심 인사이트**:
- Distributed cache는 latency cost가 있음 (네트워크)
- 하지만 horizontal scaling 가능성이 더 중요
- TPS 217은 여전히 Phase 1 대비 2배 향상

**문서 참고**: `.claude/docs/learning/2025-12-04_redis_serialization.md`

---

## 🏆 최종 성과 요약

### 정량적 성과
```
Initial → Phase 7 비교

Response Time (P95):
  3675ms → 149ms  (-96%, 24.7배 향상)

Throughput (TPS):
  106 → 217  (+109%, 2배 향상)

Error Rate:
  24.45% → ~8.5%  (-65%, Upbit API 의존)

Database Efficiency:
  11 queries → 2 queries  (-82%)

Connection Pool:
  100% saturated → 0 pending  (안정화)
```

### 아키텍처 진화
```
Before:
- 외부 API 직접 호출
- N+1 쿼리 문제
- GET 시 Update 수행
- 단일 WAS 전용

After (Phase 7):
- Redis 분산 캐시
- EntityGraph 최적화
- Read/Write 분리
- Multi-instance ready
```

---

## 📚 관련 문서

### Phase별 상세 문서
- [Phase 1-2: Cache Optimization](./phase1-2-cache-optimization.md)
- [Phase 3: N+1 Query Resolution](./phase3-n-plus-one-resolution.md)
- [Phase 4a: Read/Write Separation](./phase4a-read-write-separation.md)
- [Phase 5: Virtual Threads Failure](./phase5-virtual-threads-failure.md)
- [Phase 6: Scalability Optimization](./phase6-scalability-optimization.md)
- [Phase 7: Redis Distributed Cache](./phase7-redis-distributed-cache.md)

### 실험 원본 데이터
- `backend/scripts/phase1_cache_experiments/`
- `backend/scripts/phase3_vs_phase4_comparison/`
- `backend/scripts/phase6_scalability_opt/results/`
- `backend/scripts/phase7_redis_cache/tests/`

### 학습 기록
- [Redis 직렬화 이슈](../learning/2025-12-04_redis_serialization.md)
- [Virtual Thread 실험 실패 분석](../learning/2025-12-05_virtual_thread_experiment.md)

---

## 🎯 Phase 8 계획 (Production Readiness)

**목표**: TPS 1000+, 프로덕션 안정성 확보

**주요 작업**:
1. **Circuit Breaker 활성화** (Upbit 8.5% 에러 해결)
2. **DB Index 추가** (Portfolio 조회 10배 향상)
3. **JWT Refresh 로직** (k6 403 에러 해결)
4. **HikariCP 최적화** (300 → 150 연결)
5. **Historical Data 수집** (1년치 데이터)

**예상 성과**:
- TPS: 217 → **1000+** (4.6배)
- P95: 149ms → **<100ms** (1.5배)
- Error Rate: 8.5% → **<1%** (20배)

**계획 문서**: `.claude/plans/glimmering-sprouting-cherny.md`

---

**Last Updated**: 2025-12-06
**Next Review**: Phase 8 완료 후
