# 2026-02-13 Weekly Dev Log

## 이번 주 핵심 목표
- BE/FE를 계약 중심으로 분리해서 팀 병렬 개발 속도를 높이는 것
- market read/write 경계를 명확히 해서 운영 리스크를 줄이는 것

## Done
- auth/portfolio/market OpenAPI 계약 고정
- `ApiErrorResponse` 도입 및 GlobalExceptionHandler 표준화
- Market read side-effect 제거 + explicit refresh write path 추가
- FE `orval` generated client 체인 도입 (`api:generate`)
- Prism mock 기준 k6 시나리오 추가

## 내가 의도적으로 선택한 설계
- 계약 먼저 고정:
  - FE 개발이 BE 런타임 일정과 충돌하지 않게 하려는 선택
- read/write 분리:
  - 조회 트래픽이 시스템 상태를 바꾸지 않도록 보장
- 단일 에러 스키마:
  - FE의 예외 분기 비용과 디버깅 시간을 줄이기 위한 선택

## Verification
- backend: 계약/컨트롤러/서비스 핵심 테스트 통과
- frontend: `npm run lint` (error 0, warnings only), `npm run build` 통과

## Next
- provider 설정의 `@Value`를 `@ConfigurationProperties`로 집약 (DI/IoC 강화)
- 공급자 우선순위/쿼터 정책을 factory에 명시적으로 반영
- Stream D(perf-mock) 분리 PR로 부하테스트 파이프라인 독립화