# Phase 7: Redis Cache Integration

> **목표**: Local Cache (Simple/Caffeine) → Redis 분산 캐시 전환 후 성능 비교
> **기간**: 2025-12-04 ~
> **Status**: 🚧 In Progress

---

## 📝 Problem Statement

### 기존 문제 (Phase 6 - Local Cache)

**Phase 6 결과** (Local Caching):
- TPS: **483 req/s** (Phase 5 대비 +202%)
- Avg Response: **347ms** (Phase 4a 대비 -30%)
- Cache Hit Rate: **38-50%**
- DB Connection Pool: **정상** (Pending 0)

**제약사항**:
- ❌ **단일 WAS 전용**: Local Cache는 메모리 기반 (Caffeine/Simple)
- ❌ **Scale-out 불가**: 여러 WAS 인스턴스에서 캐시 공유 불가
- ❌ **데이터 불일치**: WAS1에서 캐시, WAS2에서 DB 조회 → 다른 결과

### 해결 방안: Redis 분산 캐시

**Redis 도입 효과**:
- ✅ **분산 캐시**: 여러 WAS가 동일한 캐시 공유
- ✅ **Scale-out 준비**: WAS 추가 시에도 캐시 일관성 유지
- ✅ **데이터 일관성**: 모든 WAS가 동일한 최신 데이터 조회

**Trade-off**:
- **Latency 증가**: Local (0ms) → Redis (0.5-2ms)
- **Network Overhead**: Redis 서버와의 통신 필요
- **운영 복잡도**: Redis 서버 관리 필요

---

## 🎯 Experiments

### Experiment 11: Redis Baseline

**파일**: `tests/exp11_redis_baseline.js`

**목표**:
- Redis 캐시 적용 후 기본 성능 측정
- Cache Hit Rate 확인 (목표: > 80%)
- Phase 6 (Local Cache) 대비 성능 비교

**Test Scenario**:
- 10%: Portfolio 생성 (Write - Cache Evict)
- 10%: Holding 추가 (Write - Cache Evict)
- 60%: Portfolio 조회 (Read - Cached!)
- 20%: Portfolio 목록 (Read)

**예상 결과**:
- TPS: > 400 req/s (Phase 6: 483 req/s, Redis Latency로 인한 소폭 감소)
- P95 Response: < 10ms (Cache Hit 시)
- Cache Hit Rate: > 80%
- Error Rate: < 1%

**실행 방법**:
```bash
# 1. Backend 실행 (perf 프로파일)
cd backend
./gradlew bootRun --args='--spring.profiles.active=perf'

# 2. k6 테스트
cd scripts/phase7_redis_cache/tests
k6 run exp11_redis_baseline.js --out json=../results/exp11_summary.json
```

**결과**: (TBD)

---

## 📊 Performance Comparison

### Before: Phase 6 (Local Cache)

| 메트릭 | 값 |
|--------|-----|
| TPS | 483 req/s |
| P95 Response | ~347ms (전체), < 1ms (Cache Hit) |
| Cache Hit Rate | 38-50% |
| DB Pool Pending | 0 |
| Cache Type | Simple (Local Memory) |

### After: Phase 7 (Redis Cache)

| 메트릭 | 값 |
|--------|-----|
| TPS | (TBD) |
| P95 Response | (TBD) |
| Cache Hit Rate | (TBD) |
| DB Pool Pending | (TBD) |
| Cache Type | Redis (Distributed) |

### 예상 결과

**성능**:
- TPS: 400-450 req/s (Redis Latency로 인한 10-20% 감소)
- P95 Response: 5-10ms (Cache Hit), Redis Latency 0.5-2ms 추가

**이점**:
- ✅ Multi-Instance 준비 완료
- ✅ 데이터 일관성 보장
- ✅ Scale-out 가능

**Trade-off**:
- ⚠️ Latency 약간 증가 (0ms → 1-2ms)
- ⚠️ Network Overhead

---

## 🔬 Redis Configuration

### Cache Settings

**파일**: `src/main/resources/application-perf.yml`

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
```

**파일**: `src/main/java/com/pjsent/sentinel/config/CacheConfig.java`

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    // Portfolio Cache: TTL 5초 (고빈도 조회)
    cacheConfigurations.put("portfolios", defaultConfig.entryTtl(Duration.ofSeconds(5)));

    // Stock Price: TTL 10초 (실시간 데이터)
    cacheConfigurations.put("stockPrice", defaultConfig.entryTtl(Duration.ofSeconds(10)));

    // Crypto Price: TTL 1분 (변동성 높음)
    cacheConfigurations.put("cryptoPrice", defaultConfig.entryTtl(Duration.ofMinutes(1)));

    // 기타: TTL 1-3분
    cacheConfigurations.put("trendingCoins", defaultConfig.entryTtl(Duration.ofMinutes(1)));
    cacheConfigurations.put("marketIndices", defaultConfig.entryTtl(Duration.ofMinutes(1)));
    cacheConfigurations.put("cryptoSearch", defaultConfig.entryTtl(Duration.ofMinutes(3)));
    cacheConfigurations.put("stockSearch", defaultConfig.entryTtl(Duration.ofMinutes(3)));

    return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
}
```

### Service Layer

**파일**: `src/main/java/com/pjsent/sentinel/portfolio/service/PortfolioService.java`

```java
@Cacheable(value = "portfolios", key = "#portfolioId")
public PortfolioDto getPortfolioById(Long portfolioId, Long userId) {
    // DB 조회 (Cache Miss 시에만 실행)
}

@CacheEvict(value = "portfolios", key = "#result.id")
public PortfolioDto createPortfolio(Long userId, PortfolioCreateRequest request) {
    // Portfolio 생성 후 캐시 무효화
}

@CacheEvict(value = "portfolios", key = "#portfolioId")
public PortfolioDto updatePortfolio(Long portfolioId, Long userId, PortfolioUpdateRequest request) {
    // Portfolio 수정 후 캐시 무효화
}
```

---

## 🧪 Testing Checklist

### Pre-Test

- [ ] Docker 컨테이너 실행: `docker-compose up -d`
- [ ] Redis 연결 확인: `redis-cli ping`
- [ ] PostgreSQL 연결 확인: `docker ps | grep postgres`
- [ ] Backend perf 프로파일 실행: `./gradlew bootRun --args='--spring.profiles.active=perf'`
- [ ] Redis Health Check: `curl http://localhost:8080/actuator/health | jq '.components.redis'`
- [ ] 토큰 파일 생성: `python scripts/common/generate_tokens.py`

### Post-Test

- [ ] k6 결과 확인: `cat results/exp11_summary.json | jq '.metrics'`
- [ ] TPS, P95, Error Rate, Cache Hit Rate 확인
- [ ] Redis 메모리 사용량: `redis-cli INFO memory`
- [ ] Redis 키 개수: `redis-cli DBSIZE`
- [ ] HikariCP 메트릭: `curl http://localhost:8080/actuator/prometheus | grep hikaricp`
- [ ] ANALYSIS.md 작성

---

## 📚 References

- [K6 Test Guide](../K6_TEST_GUIDE.md)
- [Phase 6 Results](../phase6_scalability_opt/results/COMPARISON_REPORT.md)
- [Redis Cache Config](../../backend/src/main/java/com/pjsent/sentinel/config/CacheConfig.java)
- [Spring Cache Documentation](https://docs.spring.io/spring-framework/reference/integration/cache.html)

---

## 🚀 Next Steps

### Experiment 12: Local + Redis 2-Tier Cache (TBD)

**목표**: L1 (Caffeine Local) + L2 (Redis Distributed) 하이브리드 캐시

**예상 효과**:
- **Latency**: Local Cache Hit 시 0ms (Redis 우회)
- **일관성**: Write 시 L1/L2 모두 무효화
- **성능**: TPS 500+ req/s (Phase 6 수준 유지 + Multi-Instance 지원)

**구현**:
- Caffeine (L1): TTL 짧게 (예: 5초)
- Redis (L2): TTL 길게 (예: 1분)
- Cache Miss: L1 → L2 → DB 순서로 조회

---

**Last Updated**: 2025-12-04
**Status**: Experiment 11 준비 완료
