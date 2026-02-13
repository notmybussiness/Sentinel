# TASK

## 목표
- BE/FE를 OpenAPI 계약 기준으로 분리한다.
- FE는 BE 런타임 없이 generated client + mock server만으로 개발 가능해야 한다.
- market/auth/portfolio를 1차 범위로 표준 에러 응답과 컨트롤러 계약을 고정한다.

## Wave 1 범위
- Backend
  - 표준 에러 응답 `ApiErrorResponse` 도입
  - `GlobalExceptionHandler` 통일
  - `AuthController`, `MarketDataController`, `PortfolioController` OpenAPI 주석 적용
  - `GET /api/v1/market/prices` deprecated
  - `POST /api/v1/market/price/{symbol}/refresh` 추가
  - `ServiceStatusResponse` DTO 분리
  - 계약 테스트 `OpenApiContractTest` 추가
  - OpenAPI 산출물 `docs/specs/api/openapi.json`, `docs/specs/api/openapi.yaml` 생성
- Frontend
  - `orval` 기반 generated client 체인 추가
  - `frontend/lib/api/generated/*` 생성
  - 기존 `client.ts`의 auth/portfolio/market 핵심 경로를 generated 호출로 전환
  - `npm run api:generate` 스크립트 추가
- Perf/Mock
  - Prism mock 기준 k6 시나리오 `k6/contract-mock-load.js` 추가

## 성공 기준
- Backend 타깃 테스트 통과
- OpenAPI 계약 테스트 통과
- Frontend build 통과
- Lint는 error 0 (warning 허용)
