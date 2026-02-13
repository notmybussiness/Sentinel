# 2026-02-13 T1 - Read Side Effect Removal

## 문제 상황
- 조회 API(`getStockPrice`)에서 이벤트 발행까지 수행되어 read/write 경계가 무너져 있었다.
- 이 구조는 캐시/재시도/조회 트래픽 증가 시 의도치 않은 이벤트 폭증을 만들 수 있다.

## UseCase
- FE가 포트폴리오 화면에서 가격을 자주 조회해도 시스템 상태가 바뀌지 않아야 한다.
- 운영자가 특정 심볼 가격을 강제로 갱신할 때만 이벤트 발행이 일어나야 한다.

## 고려한 접근
1. 기존 read 경로 유지 + 내부 플래그로 publish 제어
- 장점: 변경량이 적다.
- 단점: 호출자가 플래그를 알아야 해서 계약이 불명확해진다.
2. read/write API 경로 자체를 분리 (선택)
- 장점: URL/메서드 수준에서 의도가 명확하다.
- 단점: 초기 마이그레이션 비용이 있다.

## 최종 결정
- `MarketDataService.getStockPrice`는 pure read로 고정한다.
- `refreshStockPriceAndPublish`를 explicit write path로 분리한다.
- 컨트롤러에서 `POST /api/v1/market/price/{symbol}/refresh`를 write 진입점으로 둔다.

## TDD 기록
- RED: read path에서 publish가 발생하지 않아야 한다는 테스트 추가
- RED: write path에서 publish가 1회 발생해야 한다는 테스트 추가
- GREEN: 서비스 메서드 분리 + `Clock` 주입
- REFACTOR: search provider 분기(`supportsSearch`)를 정리해 예외 기반 흐름 축소

## 결과와 학습
- 조회 부작용 제거로 회귀 위험이 줄었다.
- FE는 OpenAPI 계약 기준으로 병렬 개발 가능한 기반을 확보했다.
- 설계 의도를 테스트 이름으로 남겨서, 이후 리팩토링 시 문서 역할도 하게 만들었다.