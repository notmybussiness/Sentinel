# Architecture Decision Records (ADR)

> **Last Updated**: 2025-12-14

---

## 📚 ADR 목록

### 성능 최적화 (Phase 1-7 완료)
| ADR | 제목 | 상태 | 결과 |
|-----|------|------|------|
| ADR-001 | Cache Layer를 Service로 이동 | ✅ 완료 | 1949ms → 10ms (210x 개선) |
| ADR-002 | EntityGraph로 N+1 해결 | ✅ 완료 | 11 queries → 2 queries |
| ADR-003 | Cache TTL 전략 | ✅ 완료 | Hit Rate 95%+ |

### 인프라 & 캐싱
| ADR | 제목 | 상태 | 날짜 |
|-----|------|------|------|
| [ADR-004](./adr-001-redis-before-kafka.md) | Redis 분산 캐시를 Kafka EDA보다 우선 구현 | ✅ Accepted | 2025-12-04 |

### 데이터 소스 & API
| ADR | 제목 | 상태 | 날짜 |
|-----|------|------|------|
| [ADR-005](./adr-002-kis-historical-data.md) | Historical Data Source 선택 (KIS vs 외부 API) | ✅ Implemented | 2025-12-14 |

### 아키텍처 설계
| 문서 | 설명 |
|------|------|
| [Kafka EDA Design](./kafka-eda-design.md) | Event-Driven Architecture 설계 (Phase 8+) |
| [Branch Strategy](./branch-strategy.md) | Git 브랜치 전략 |

---

## 📝 ADR 작성 가이드

새로운 기술 결정 시 `/architecture-decision` 명령어 사용:

```bash
/architecture-decision
```

### ADR 템플릿
```markdown
# ADR-XXX: [제목]

**Status**: Accepted / Proposed / Deprecated
**Date**: YYYY-MM-DD
**Deciders**: [이름]

## Context
어떤 문제가 있었는가?

## Decision
어떤 결정을 내렸는가?

## Consequences
결과는 어떠했는가?
- ✅ 장점
- ⚠️ 단점
- 📊 측정 결과

## Alternatives Considered
고려했던 다른 방안들
```

---

## 🎯 ADR의 가치

### 포트폴리오 강점
- ✅ 기술 선택에 대한 **명확한 근거**
- ✅ Trade-offs 이해도 증명
- ✅ 문제 해결 능력 시각화

### 면접 대비
- "왜 이 기술을 선택했나요?" → ADR로 답변
- "다른 방법은 없었나요?" → Alternatives Considered
- "결과는 어땠나요?" → 정량적 데이터 제시

---

**다음 작업**: `/architecture-decision` 명령어로 ADR 작성 시작
