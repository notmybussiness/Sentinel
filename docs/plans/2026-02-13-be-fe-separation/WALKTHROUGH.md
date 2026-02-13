# WALKTHROUGH

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
