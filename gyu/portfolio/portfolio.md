# Portfolio Notes

## Project
Sentinel - Contract-first BE/FE separation (Wave 1)

## What I changed
- OpenAPI contract를 우선 고정하고 FE generated client를 도입했다.
- auth/portfolio/market 컨트롤러를 문서화하고, 표준 에러 응답을 도입했다.
- market read/write 경로를 분리해 조회 부작용을 제거했다.
- Prism + k6 mock 부하테스트 경로를 마련했다.

## Engineering thinking
- 기능 구현보다 계약을 먼저 고정하면 팀 병렬성이 커진다.
- 글로벌 예외 스키마는 FE 에러 처리 비용을 크게 낮춘다.
- TDD로 read/write 의도를 테스트에 먼저 표현하면 리팩토링 안정성이 올라간다.
