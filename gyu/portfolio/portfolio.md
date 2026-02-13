# Portfolio Notes

## Project
Sentinel - Contract-first BE/FE separation (Wave 1)

## 문제 정의
- FE 개발이 BE 서버 상태, 수동 타입, 불명확한 에러 응답에 의존해 병렬 개발 속도가 낮았다.
- Market 조회 API가 write 성격(side-effect)을 가져 운영 안정성을 해칠 수 있었다.

## 내가 세운 원칙
- 계약(OpenAPI)을 코드보다 먼저 고정한다.
- read/write는 URL과 서비스 메서드에서 명시적으로 분리한다.
- 예외 응답은 단일 스키마로 통일한다.
- 타입은 generated source를 단일 진실 원천(single source of truth)으로 사용한다.

## 실행 내용
- OpenAPI 계약 고정 + 계약 테스트(`OpenApiContractTest`) 도입
- `ApiErrorResponse` + `GlobalExceptionHandler` 표준화
- market read side-effect 제거, explicit refresh write path 추가
- FE에 `orval` generated client 도입 및 핵심 호출 전환
- Prism + k6 mock 기반 계약 부하테스트 경로 추가

## 기술적 의사결정과 이유
- 컨트롤러 try-catch 남용 제거:
  - 중복된 에러 매핑을 줄이고, 글로벌 핸들러 중심으로 일관성 확보
- deprecated 정책 유지:
  - 기존 클라이언트 호환을 깨지 않으면서 단계적 전환 가능
- provider 정책 분리(후속 PR):
  - 공급자 무료 제공량/한도 전략을 코드 정책으로 관리하기 위해 factory 중심 라우팅 적용

## 성과
- FE가 BE 런타임 없이 mock + generated client로 개발 가능한 상태를 만들었다.
- read/write 경계가 테스트로 보호되어 리팩토링 안전성이 높아졌다.
- 문서(TASK/IMPLEMENTATION/WALKTHROUGH)와 코드 변경을 1:1로 추적 가능하게 정리했다.

## 신입 개발자로서 보여주고 싶은 점
- 기능 구현보다 "변경 후 운영 안정성"을 우선하는 사고
- 계약/테스트/문서를 함께 설계해 팀 병렬성을 높이는 습관
- 리팩토링 이유를 코드와 테스트 이름으로 남기는 커뮤니케이션 방식