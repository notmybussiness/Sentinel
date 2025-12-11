# Architecture Decision Records (ADR)

> **Last Updated**: 2024-11-24

---

## 📚 ADR 목록

### 성능 최적화
1. [ADR-001: Cache Layer를 Service로 이동](./ADR-001-cache-layer-service.md) - 2024-11-20
2. [ADR-002: EntityGraph로 N+1 해결](./ADR-002-entity-graph-n1.md) - 2024-11-22
3. [ADR-003: Cache TTL 전략](./ADR-003-cache-ttl-strategy.md) - 2024-11-19

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
