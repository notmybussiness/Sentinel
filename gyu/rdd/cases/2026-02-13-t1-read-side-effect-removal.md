# 2026-02-13 T1 - Read Side Effect Removal

## UseCase
- 포트폴리오/마켓 조회 시 읽기 API가 이벤트 발행(write)을 유발하면, 예측 불가한 부작용이 누적된다.
- 조회는 조회만 수행하고, 갱신/발행은 명시적 write path로 분리해야 한다.

## 변경 의도
- `MarketDataService.getStockPrice`를 pure read로 유지
- `refreshStockPriceAndPublish`를 write path로 분리
- 컨트롤러에서 read/write 계약을 URL 레벨로 명확히 구분

## TDD 기록
- RED: read path publish 금지 / write path publish 보장 테스트 추가
- GREEN: 서비스 메서드 분리 + Clock 주입
- REFACTOR: search provider supportsSearch 필터 정리

## 결과
- read side effect 제거
- 계약/에러 응답 스키마 표준화
- FE generated client 기반 병렬 개발 가능 기반 확보
