# 2026-02-13 Weekly Dev Log

## Done
- BE/FE contract-first 전략으로 auth/portfolio/market OpenAPI 계약 고정
- `ApiErrorResponse` 도입 및 GlobalExceptionHandler 표준화
- Market read side-effect 제거 + explicit refresh write path 추가
- FE orval generated client 체인 도입 (`api:generate`)
- Prism mock 기준 k6 시나리오 추가

## Verification
- backend: 핵심 계약/컨트롤러/서비스 테스트 통과
- frontend: `npm run lint` (error 0, warnings only), `npm run build` 통과

## Next
- provider 설정의 `@Value`를 `@ConfigurationProperties`로 집약 (DI/IoC 강화)
- FE에서 generated model alias로 수동 타입 단계적 제거
- branch 분할 전략으로 후속 PR(Refactor/Perf/Docs) 병렬 진행
