# Sentinel Performance Optimization - Status

> **Last Updated**: 2025-11-24
> **Current Phase**: Phase 3b (Cache 효과 검증)

---

## 🎯 Quick Status

```
✅ Phase 1: Cache Optimization (완료)
✅ Phase 2: External API (완료, 210배 개선)
✅ Phase 3a: EntityGraph N+1 (완료)
🔄 Phase 3b: Cache 효과 검증 (다음)
📋 Phase 3c: BatchSize N+1 완전 해결 (대기)
📋 Phase 4: Kafka Event-Driven (계획)
```

---

## 📊 Phase별 성과

### Phase 1: Cache TTL 최적화
- 500 에러: 827건 → **0건** (완전 제거)
- TTL: 1분 → 3분

### Phase 2: Cache Layer 이동
- Avg Response: 1,949ms → **10ms** (210배)
- P95: 3,675ms → **7ms** (525배)
- TPS: 106 → **480 req/s** (4.5배)
- 500 에러: 1,244건 → **0건**
- Cache: Provider → Service Layer

### Phase 3a: EntityGraph
- Portfolio 1:N Holdings Fetch Join
- N+1 쿼리 부분 해결

---

## 🔥 Next Steps

### 1. Phase 3b: Cache 효과 검증 (준비 완료 ✅)
**변경 사항**:
- ✅ CacheManager 분리: Market / Crypto
- ✅ TTL 재조정: Market 1분, Crypto 10초
- ✅ 모든 @Cacheable에 cacheManager 명시
- ✅ Provider 레벨 캐시 제거

**측정 항목**:
- Response Time (P95 < 50ms for Market, < 30ms for Crypto)
- Cache Hit Rate (Market >80%, Crypto >70%)
- API Calls/min (감소 확인)

**실험 환경**:
- WAS: Windows PC (192.168.0.58:8080) - 12400F + 16GB
- DB: Linux Server (192.168.0.5:5432) - 3400G + 12GB
- Monitoring: Grafana (192.168.0.5:3001) + Prometheus
- k6: MacBook

**문서**:
- 📁 `.claude/docs/plans/phase3b_experiment.md` (실험 계획)
- 📋 `.claude/docs/plans/phase3b_setup.md` (실행 가이드)

**다음 작업**: MacBook에서 k6 테스트 실행

### 2. Phase 3c: N+1 완전 해결 (3~5시간)
**방법**: @BatchSize(50) 적용

**예상**:
- SQL: 10~20개 → 2~3개
- Response: 100ms → 30ms

### 3. Phase 4: Kafka Event-Driven (2~3일)
**목표**: 트랜잭션 분리 → 빠른 응답

**예상**:
- Response: 810ms → 10ms (81배)

---

## 📚 Documentation

- **로드맵**: .claude/ROADMAP.md ← 전체 계획
- **메인 가이드**: .claude/CLAUDE.md
- **실험 결과**: backend/scripts/results/

---

**다음 작업**: Cache 효과 검증 (k6 테스트)
