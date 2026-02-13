# IMPLEMENTATION

## 1) 계약/에러 표준화
- 추가: `backend/src/main/java/com/pjsent/sentinel/common/exception/ApiErrorResponse.java`
- 변경: `backend/src/main/java/com/pjsent/sentinel/common/exception/GlobalExceptionHandler.java`
  - 모든 주요 예외를 `ApiErrorResponse`로 통일
  - `status/error/message/path/details` 구조 고정

## 2) 컨트롤러 리팩토링
- 변경: `backend/src/main/java/com/pjsent/sentinel/user/controller/AuthController.java`
  - 과도한 try-catch 제거
  - bearer token 파싱 검증 추가
  - OpenAPI 주석 적용
- 변경: `backend/src/main/java/com/pjsent/sentinel/market/controller/MarketDataController.java`
  - 과도한 try-catch 제거
  - 배치 입력 검증을 예외 기반으로 통일
  - `GET /market/prices` deprecated
  - `POST /market/price/{symbol}/refresh` 추가
  - OpenAPI 주석 적용
- 변경: `backend/src/main/java/com/pjsent/sentinel/portfolio/controller/PortfolioController.java`
  - OpenAPI 주석 적용
- 추가: `backend/src/main/java/com/pjsent/sentinel/market/dto/ServiceStatusResponse.java`

## 3) 서비스 레이어(TDD)
- 변경: `backend/src/main/java/com/pjsent/sentinel/market/service/MarketDataService.java`
  - read path side-effect 제거 (`getStockPrice`에서 publish 제거)
  - write path 분리 (`refreshStockPriceAndPublish` 추가)
  - `Clock` 주입 가능 구조
  - search provider filter (`supportsSearch`) 반영

## 4) 테스트
- 변경: `backend/src/test/java/com/pjsent/sentinel/common/exception/GlobalExceptionHandlerTest.java`
- 변경: `backend/src/test/java/com/pjsent/sentinel/user/controller/AuthControllerTest.java`
- 변경: `backend/src/test/java/com/pjsent/sentinel/market/controller/MarketDataControllerTest.java`
- 변경: `backend/src/test/java/com/pjsent/sentinel/portfolio/controller/PortfolioControllerTest.java`
- 변경: `backend/src/test/java/com/pjsent/sentinel/market/service/MarketDataServiceTest.java`
- 추가: `backend/src/test/java/com/pjsent/sentinel/contract/OpenApiContractTest.java`
  - deprecated 계약 검증
  - `ApiErrorResponse` 스키마 노출 검증
  - OpenAPI json/yaml 스냅샷 export

## 5) FE generated client
- 변경: `frontend/package.json`
  - `api:generate` 스크립트 추가
  - `orval` dev dependency 추가
- 추가: `frontend/orval.config.ts`
- 추가: `frontend/lib/api/generated/mutator.ts`
- 생성: `frontend/lib/api/generated/sdk.ts`
- 생성: `frontend/lib/api/generated/model/*`
- 변경: `frontend/lib/api/client.ts`
  - auth/market/portfolio 핵심 경로를 generated SDK 호출로 전환

## 6) Mock perf
- 추가: `k6/contract-mock-load.js`
  - BASE_URL이 로컬 mock이 아니면 실패하도록 안전장치 포함

## 7) OpenAPI 산출물
- 생성: `docs/specs/api/openapi.json`
- 생성: `docs/specs/api/openapi.yaml`
