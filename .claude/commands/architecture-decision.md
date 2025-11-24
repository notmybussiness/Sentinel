# Architecture Decision Record - 기술 의사결정 기록

## 🎯 목적
**기술 선택의 이유를 문서화**
- Why? 왜 이 기술을 선택했는가
- Trade-offs: 장단점 분석
- Consequences: 영향 파악
- 포트폴리오: 사고 과정 증명

---

## 📝 ADR 템플릿

```markdown
# ADR-XXX: [결정 제목]

## Status
[Proposed | Accepted | Deprecated | Superseded]

## Context
[이 결정이 필요한 배경과 문제 상황]

## Decision
[선택한 방안]

## Consequences

### Positive
[긍정적 영향]

### Negative
[부정적 영향 / Trade-offs]

### Neutral
[중립적 영향]

## Alternatives Considered
[고려했지만 선택하지 않은 대안들]

## References
[관련 문서, 링크, 이슈]
```

---

## 🔥 실전 예시: Caffeine vs Redis

### ADR-001: 로컬 캐시로 Caffeine 선택

```markdown
# ADR-001: 가격 데이터 캐싱에 Caffeine 사용

## Status
✅ Accepted (2025-01-15)

## Context

### 문제 상황
- 외부 API (Binance, Upbit) 호출이 매우 빈번 (분당 5000회)
- API Rate Limit 초과로 15% 에러 발생
- 가격 데이터는 1분마다만 갱신되므로 실시간성 불필요

### 요구사항
- 캐시 Hit Rate 95% 이상
- TTL: 1분
- 서버 재시작 시 캐시 소실 허용
- 단일 WAS 서버 환경 (홈랩)

## Decision

**Caffeine 로컬 캐시를 선택**

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("prices");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .recordStats());
        return cacheManager;
    }
}
```

## Consequences

### Positive ✅
- **성능**: 메모리 접근 (1ms 미만) vs Redis 네트워크 (5~10ms)
- **단순성**: 별도 Redis 서버 불필요
- **비용**: 추가 인프라 비용 없음
- **개발 속도**: Spring Boot 통합 간단
- **실제 결과**: 
  - API 호출 95% 감소 (5000 → 250 req/min)
  - P95 Latency 98% 개선 (1200ms → 20ms)
  - Cache Hit Rate: 95.2%

### Negative ❌
- **확장성 제한**: 다중 서버 환경에서 캐시 불일치 가능
- **영속성 없음**: 서버 재시작 시 캐시 소실
- **메모리 제약**: WAS 메모리 사용량 증가 (약 100MB)

### Neutral ⚙️
- 현재 단일 서버 환경에서는 문제없음
- 향후 스케일 아웃 시 Redis로 전환 고려 필요

## Alternatives Considered

### 1. Redis (분산 캐시)
**장점**:
- 다중 서버 환경에서 일관성
- 서버 재시작 시에도 캐시 유지
- 더 큰 용량

**단점**:
- 추가 인프라 필요 (Redis 서버)
- 네트워크 지연 (5~10ms)
- 운영 복잡도 증가

**선택하지 않은 이유**:
- 현재 단일 서버 환경
- 서버 재시작 시 캐시 소실 허용 가능
- 성능 우선

### 2. Ehcache
**장점**:
- 오래된 검증된 라이브러리
- Disk persist 가능

**단점**:
- Caffeine보다 느린 성능
- Spring Boot 기본 통합 아님
- 복잡한 설정

**선택하지 않은 이유**:
- Caffeine이 성능 우수
- Spring Boot 기본 지원

### 3. 캐싱 없음 (현상 유지)
**단점**:
- API Rate Limit 초과
- 15% 에러율
- 느린 응답 시간

**선택하지 않은 이유**:
- 사용자 경험 저하
- API 비용 증가 가능성

## References
- [Caffeine GitHub](https://github.com/ben-manes/caffeine)
- [Spring Boot Caching](https://spring.io/guides/gs/caching/)
- [Performance Test Results](..analysis/cache-performance-20250115.md)
- Related Issue: #42
- Related Commit: a1b2c3d

## Migration Plan (if needed)
향후 다중 서버 환경으로 전환 시:
1. Redis 서버 구축
2. L1 (Caffeine) + L2 (Redis) 하이브리드 구성 고려
3. 캐시 Warming 전략 수립
```

---

## 🎯 다른 ADR 예시들

### ADR-002: 비동기 처리에 CompletableFuture 사용

```markdown
## Context
외부 API 4개를 순차 호출 시 5초 소요

## Decision
CompletableFuture로 병렬 처리

## Consequences
### Positive
- 5초 → 2초 (60% 개선)
- Throughput 10배 향상

### Negative
- 예외 처리 복잡도 증가
- Thread Pool 관리 필요

## Alternatives
- RxJava: 학습 곡선 높음
- Kotlin Coroutine: Java 프로젝트
```

### ADR-003: N+1 해결에 @EntityGraph 선택

```markdown
## Context
Portfolio 조회 시 101개 쿼리 발생

## Decision
@EntityGraph(attributePaths = {"assets"})

## Consequences
### Positive
- 2500ms → 150ms (94% 개선)
- 간단한 적용

### Negative
- 모든 Asset을 항상 로드 (불필요한 경우도)

## Alternatives
- @BatchSize: 부분적 개선만
- DTO Projection: 과도한 Boilerplate
```

---

## 📊 ADR 관리

### 디렉토리 구조
```bash
.claude/docs/decisions/
├── README.md                    # ADR 목록
├── adr-001-caffeine-cache.md
├── adr-002-completable-future.md
├── adr-003-entity-graph.md
└── adr-004-postgresql.md
```

### README.md (Index)
```markdown
# Architecture Decision Records

## Active
- [ADR-001](adr-001-caffeine-cache.md) - Caffeine 로컬 캐시 사용
- [ADR-002](adr-002-completable-future.md) - CompletableFuture 비동기 처리
- [ADR-003](adr-003-entity-graph.md) - @EntityGraph로 N+1 해결

## Deprecated
- ~~ADR-XXX~~: [이유]

## Superseded
- ADR-XXX → ADR-YYY: [마이그레이션 이유]
```

---

## 🎓 포트폴리오 활용

### 면접 대응
```
면접관: "기술 선택 시 어떤 기준으로 결정하나요?"

당신: "Trade-offs를 명확히 분석합니다.
      예를 들어 캐싱 도입 시:
      
      Caffeine vs Redis를 비교했고,
      - 현재: 단일 서버 (Caffeine 충분)
      - 성능: Caffeine 더 빠름 (1ms vs 10ms)
      - 향후: 스케일 아웃 시 Redis 전환
      
      이런 분석을 ADR로 문서화해서
      나중에 다시 왜 이 선택을 했는지 알 수 있게 했습니다."

면접관: "실제 효과는?"

당신: "API 호출 95% 감소, Latency 98% 개선으로
      측정값으로 증명됩니다."
```

### GitHub에서 보이는 모습
```
sentinel/
├── .claude/
│   └── docs/
│       └── decisions/
│           ├── README.md
│           ├── adr-001-caffeine-cache.md  ← 기술 선택 이유 명확
│           ├── adr-002-async-api.md
│           └── adr-003-n1-solution.md
```

**효과**:
- ✅ 사고 과정 투명하게 보임
- ✅ Trade-offs 분석 능력 증명
- ✅ 단순 따라하기가 아님을 보여줌

---

## 🛠️ 자동화 도구

### ADR 템플릿 생성 스크립트
```bash
#!/bin/bash
# scripts/create-adr.sh

ADR_DIR=".claude/docs/decisions"
NEXT_NUM=$(ls $ADR_DIR/adr-*.md 2>/dev/null | wc -l | awk '{print $1 + 1}')
ADR_NUM=$(printf "%03d" $NEXT_NUM)
TITLE=$1

if [ -z "$TITLE" ]; then
    echo "Usage: ./scripts/create-adr.sh 'Decision Title'"
    exit 1
fi

FILENAME="$ADR_DIR/adr-$ADR_NUM-$(echo $TITLE | tr '[:upper:]' '[:lower:]' | tr ' ' '-').md"

cat > "$FILENAME" << EOF
# ADR-$ADR_NUM: $TITLE

## Status
Proposed

## Context
[배경과 문제 상황을 설명하세요]

## Decision
[선택한 방안을 설명하세요]

## Consequences

### Positive
- [긍정적 영향]

### Negative
- [부정적 영향 / Trade-offs]

### Neutral
- [중립적 영향]

## Alternatives Considered

### Alternative 1: [대안 이름]
**장점**:
- 

**단점**:
- 

**선택하지 않은 이유**:
- 

## References
- [관련 이슈, 문서, 커밋]

---
Date: $(date +%Y-%m-%d)
Author: $(git config user.name)
EOF

echo "✅ Created: $FILENAME"
```

**사용법**:
```bash
./scripts/create-adr.sh "Use Caffeine for local caching"
```

---

## 📝 작성 가이드

### 좋은 ADR의 특징
1. **명확한 문제 정의**: Why가 분명
2. **대안 비교**: 최소 2개 이상
3. **측정 가능한 결과**: 정량적 수치
4. **솔직한 단점 기록**: Trade-offs 인정

### 나쁜 ADR 예시
```markdown
## Decision
Redis를 사용하기로 했다.

## Why
빠르니까.
```
→ ❌ 대안 비교 없음, 구체적 이유 없음

### 좋은 ADR 예시
```markdown
## Decision
Caffeine 로컬 캐시 사용

## Context
- 단일 서버 환경
- API Rate Limit 문제 (15% 에러)
- 1분 TTL로 충분

## Alternatives
1. Redis: 불필요한 복잡도
2. Ehcache: 성능 열세

## Results
- API 호출 95% 감소
- Latency 98% 개선
```
→ ✅ 배경, 대안, 결과 모두 명확
