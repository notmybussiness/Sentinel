# WALKTHROUGH

## 0. UseCase와 설계 의도
- 문제: FE 개발이 BE 런타임과 수동 타입 정의에 묶여 병렬 개발이 어려웠다.
- 목표: FE는 OpenAPI 계약 + generated client + mock 서버만으로 개발 가능해야 한다.
- 핵심 의도:
  - 조회(read) API는 side-effect 0 보장
  - 갱신/발행(write)은 명시적 엔드포인트로 분리
  - 에러 응답 포맷을 단일 스키마로 고정해 FE 분기 복잡도 감소
- 리팩토링 이유:
  - 컨트롤러 try-catch 남용은 중복과 누락을 만든다.
  - 수동 타입은 계약 drift를 만든다.
  - 계약 우선(OpenAPI-first)으로 전환하면 팀 병렬성이 올라간다.

## 1. Clone/Checkout
```bash
git clone <repo-url>
cd Sentinel
git checkout feature/contract-auth-portfolio-market
```

## 2. Backend 검증
```bash
cd backend
./gradlew.bat test --tests "com.pjsent.sentinel.common.exception.GlobalExceptionHandlerTest" \
  --tests "com.pjsent.sentinel.user.controller.AuthControllerTest" \
  --tests "com.pjsent.sentinel.market.controller.MarketDataControllerTest" \
  --tests "com.pjsent.sentinel.portfolio.controller.PortfolioControllerTest" \
  --tests "com.pjsent.sentinel.market.service.MarketDataServiceTest" \
  --tests "com.pjsent.sentinel.contract.OpenApiContractTest"
```

## 3. OpenAPI 산출물 확인
`OpenApiContractTest` 실행 시 자동 생성:
- `docs/specs/api/openapi.json`
- `docs/specs/api/openapi.yaml`

## 4. Frontend generated client
```bash
cd ../frontend
npm ci
npm run api:generate
npm run lint
npm run build
```

## 5. Prism mock + k6
```bash
# repo root 기준
npx @stoplight/prism-cli mock docs/specs/api/openapi.yaml -h 0.0.0.0 -p 4010
k6 run -e BASE_URL=http://localhost:4010 k6/contract-mock-load.js
```

## 6. FE 병렬 개발 가이드
- FE 팀은 `docs/specs/api/openapi.yaml` + `frontend/lib/api/generated/*`만 보고 개발 시작 가능
- BE 런타임 없이 Prism mock으로 화면/상호작용 검증 가능
- 인증/재발급은 `frontend/lib/api/generated/mutator.ts` 기준으로 동작

## 7. 설계 결정 체크리스트
- 계약 우선: `/v3/api-docs`와 `docs/specs/api/*`를 기준으로 작업한다.
- Read/Write 분리: 조회 경로에서 이벤트 발행이 발생하면 회귀로 간주한다.
- 에러 스키마 통일: `ApiErrorResponse`를 벗어나는 응답은 허용하지 않는다.
- 타입 단일화: FE 수동 타입 대신 generated model을 기준으로 사용한다.