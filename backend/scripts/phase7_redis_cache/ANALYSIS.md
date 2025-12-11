# Phase 7: Redis Cache - Performance Analysis

> **분석 기간**: 2025-12-04 ~
> **분석자**: Claude Code
> **Status**: 🚧 In Progress

---

## 📊 Experiment 11: Redis Baseline Performance

### 실험 목적

Local Cache (Phase 6) → Redis 분산 캐시 전환 후:
1. 성능 변화 측정 (TPS, P95 Response Time)
2. Cache Hit Rate 확인
3. Redis Latency Impact 분석
4. Multi-Instance 준비 상태 검증

---

## 🔬 실험 설정

### Environment

| 항목 | 값 |
|------|-----|
| Backend Profile | `perf` |
| Cache Type | Redis |
| Redis Version | 7.2-alpine |
| Redis Max Memory | 512MB |
| Redis Eviction Policy | allkeys-lru |
| HikariCP Pool Size | 100 |
| Virtual Threads | Enabled |

### Cache Configuration

| Cache Name | TTL | 용도 |
|-----------|-----|------|
| `portfolios` | 5s | Portfolio 조회 (고빈도) |
| `stockPrice` | 10s | 주식 가격 (실시간) |
| `cryptoPrice` | 1m | 암호화폐 가격 |
| `trendingCoins` | 1m | 트렌딩 코인 |
| `marketIndices` | 1m | 시장 지수 |
| `cryptoSearch` | 3m | 암호화폐 검색 |
| `stockSearch` | 3m | 주식 검색 |

### Load Test Configuration

```javascript
// k6 Options
stages: [
    { duration: '30s', target: 500 },  // Ramp-up
    { duration: '3m', target: 500 },   // Sustain
    { duration: '30s', target: 0 },    // Ramp-down
]

thresholds: {
    'http_req_duration': ['p(95)<100'],
    'portfolio_get_duration': ['p(95)<10'],
    'errors': ['rate<0.01'],
    'cache_hits': ['rate>0.8'],
}
```

**Test Scenario**:
- 10%: Portfolio 생성 (Write)
- 10%: Holding 추가 (Write)
- 60%: Portfolio 조회 (Read - Main Target)
- 20%: Portfolio 목록 (Read)

---

## 📈 Results

### Before: Phase 6 (Local Cache)

**성능 메트릭**:
| 메트릭 | 값 |
|--------|-----|
| TPS | 483 req/s |
| Avg Response Time | 347ms |
| P95 Response Time | (TBD) |
| Cache Hit Rate | 38-50% |
| Error Rate | 0% |
| DB Connection Pool (Active) | 정상 |
| DB Connection Pool (Pending) | 0 |

**Cache 특성**:
- Type: Simple Cache (Spring Default)
- Location: Local Memory (WAS 내부)
- Latency: 0ms
- Shared: ❌ (단일 WAS 전용)

### After: Phase 7 (Redis Cache)

**성능 메트릭**: (TBD - 실험 후 작성)

| 메트릭 | 값 |
|--------|-----|
| TPS | (TBD) |
| Avg Response Time | (TBD) |
| P95 Response Time | (TBD) |
| Portfolio GET P95 | (TBD) |
| Cache Hit Rate | (TBD) |
| Error Rate | (TBD) |
| DB Connection Pool (Active) | (TBD) |
| DB Connection Pool (Pending) | (TBD) |

**Cache 특성**:
- Type: Redis Cache
- Location: Redis Server (Docker)
- Latency: 0.5-2ms (hit), > 10ms (miss)
- Shared: ✅ (Multi-Instance 지원)

**Redis Metrics**: (TBD)

```bash
# Redis Memory Usage
redis-cli INFO memory | grep used_memory_human

# Redis Keys Count
redis-cli DBSIZE

# Portfolio Cache Keys
redis-cli KEYS "sentinel:portfolios::*" | wc -l

# TTL Check (portfolios: 5초)
redis-cli TTL "sentinel:portfolios::1"
```

---

## 🔍 Analysis

### 1. Performance Impact (TBD)

**TPS 변화**:
- Before (Local): 483 req/s
- After (Redis): (TBD) req/s
- **Change**: (TBD)%

**예상**:
- Redis Latency (0.5-2ms) 추가로 인한 10-20% 감소 예상
- 목표: > 400 req/s

**Response Time 변화**:
- Before (Local Cache Hit): < 1ms
- After (Redis Cache Hit): 5-10ms
- **Redis Latency Impact**: +1-2ms

### 2. Cache Hit Rate (TBD)

**측정 방법**:
- Response Time 기준: < 5ms = Cache Hit
- Local Cache: < 1ms
- Redis Cache: 0.5-2ms (hit), > 10ms (miss + DB query)

**결과**: (TBD)

### 3. Redis Performance (TBD)

**Redis Memory**:
- Initial: (TBD) MB
- Peak: (TBD) MB
- Keys Count: (TBD)

**Cache Key Distribution**:
```bash
# portfolios 캐시 키 수
redis-cli KEYS "sentinel:portfolios::*" | wc -l

# 예상: 500명 사용자 × 평균 3개 Portfolio = ~1500 keys
```

**TTL Verification**:
```bash
# portfolios 캐시 (TTL: 5초)
redis-cli TTL "sentinel:portfolios::1"
# 응답: 0~5 사이 값
```

### 4. DB Connection Pool (TBD)

**Before (Local Cache)**:
- Active: 정상
- Pending: 0

**After (Redis Cache)**:
- Active: (TBD)
- Pending: (TBD)

**예상**: 캐시로 인한 DB 부하 감소 유지

---

## 💡 Insights

### 예상 결과

**✅ 성공 기준**:
1. TPS: > 400 req/s (Phase 6 대비 -20% 이내)
2. Cache Hit Rate: > 80%
3. Error Rate: < 1%
4. Redis Latency: 1-2ms (acceptable)
5. Multi-Instance 준비: ✅

**⚠️ Trade-offs**:
- Latency 증가: Local (0ms) → Redis (1-2ms)
- Network Overhead: Redis 서버 통신 필요
- 운영 복잡도: Redis 서버 관리 필요

**✅ 이점**:
- **Scale-out 가능**: 여러 WAS 인스턴스에서 캐시 공유
- **데이터 일관성**: 모든 WAS가 동일한 캐시 참조
- **Cache Eviction**: Write 시 모든 WAS에 즉시 반영

### 배운 점 (TBD - 실험 후 작성)

1. **Redis Latency Impact**:
   - (TBD)

2. **Cache Hit Rate**:
   - (TBD)

3. **Multi-Instance 준비**:
   - (TBD)

---

## 🚀 Next Steps

### Experiment 12: 2-Tier Cache (Caffeine + Redis)

**목표**: L1 (Local) + L2 (Redis) 하이브리드 캐시로 Latency 최소화

**구현**:
```java
@Configuration
public class TwoTierCacheConfig {
    @Bean
    public CacheManager cacheManager(
        RedisConnectionFactory redisFactory
    ) {
        // L1: Caffeine (Local, Fast)
        CaffeineCacheManager caffeineCache = new CaffeineCacheManager();
        caffeineCache.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .maximumSize(1000));

        // L2: Redis (Distributed, Shared)
        RedisCacheManager redisCache = RedisCacheManager.builder(redisFactory)
            .cacheDefaults(defaultConfig)
            .build();

        // Hybrid: L1 → L2 → DB
        return new CompositeCacheManager(caffeineCache, redisCache);
    }
}
```

**예상 효과**:
- Cache Hit (L1): 0ms (Caffeine)
- Cache Hit (L2): 1-2ms (Redis)
- Cache Miss: > 10ms (DB)
- TPS: 500+ req/s (Phase 6 수준 + Multi-Instance 지원)

### Experiment 13: Multi-Instance Load Balancing (TBD)

**목표**: 2개 이상의 WAS 인스턴스에서 Redis 캐시 공유 검증

**설정**:
- WAS1: Port 8080
- WAS2: Port 8081
- Redis: Shared (localhost:6379)
- Load Balancer: Nginx (Round-Robin)

**검증 항목**:
- [ ] WAS1에서 Write → WAS2에서 Read (최신 데이터 확인)
- [ ] Cache Eviction 동기화
- [ ] Redis Connection Pool 분산

---

## 📚 References

- [Phase 6 Results](../phase6_scalability_opt/results/COMPARISON_REPORT.md)
- [Redis Cache Config](../../src/main/java/com/pjsent/sentinel/config/CacheConfig.java)
- [k6 Test Script](./tests/exp11_redis_baseline.js)
- [Spring Cache Documentation](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Redis Documentation](https://redis.io/docs/)

---

**Last Updated**: 2025-12-04
**Status**: Waiting for Experiment 11 Results
