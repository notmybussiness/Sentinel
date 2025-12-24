---
description: Sentinel 백엔드 문제점/정상화 작업 참조 (BE 이슈, 문제점, 정상화)
---

# Sentinel Backend QA Issues Reference

이 워크플로우는 "BE 문제점", "백엔드 정상화", "BE 이슈" 등의 키워드로 호출됩니다.

## 참조할 QA 감사 보고서

**파일 위치:** `C:\Users\zetto\.gemini\antigravity\brain\ddfc54a1-e758-4440-b00d-6ec9b0c2d9f8\qa_audit_report.md`

1. 위 QA 감사 보고서 파일을 읽어와 현재 상태를 파악합니다.

## 🔴 Critical Issues (즉시 수정)

| # | 이슈 | 파일 | 상태 |
|---|------|------|------|
| 1 | `anyRequest().permitAll()` 보안 취약점 | SecurityConfig.java | ⬜ 미해결 |
| 2 | GeminiService Resilience4j 미적용 | GeminiService.java | ⬜ 미해결 |
| 3 | PriceHistory 테스트 0% | pricehistory/ 전체 | ⬜ 미해결 |
| 4 | KIS API CircuitBreaker 비활성화 | application.yml L186 | ⬜ 미해결 |

## 🟠 High Priority (2주 내)

| # | 이슈 | 파일 | 상태 |
|---|------|------|------|
| 5 | CryptoStreamController 테스트 미작성 (메모리 누수 위험) | CryptoStreamController.java | ⬜ 미해결 |
| 6 | BatchController 테스트 미작성 | BatchController.java | ⬜ 미해결 |
| 7 | GeminiService 테스트 미작성 | GeminiService.java | ⬜ 미해결 |
| 8 | PortfolioAnalysisService 테스트 미작성 | PortfolioAnalysisService.java | ⬜ 미해결 |
| 9 | SSEStreamingService 테스트 미작성 | SSEStreamingService.java | ⬜ 미해결 |
| 10 | DevController Profile 제한 필요 | DevController.java | ⬜ 미해결 |

## 🟡 Medium Priority (1개월 내)

| # | 이슈 | 파일 | 상태 |
|---|------|------|------|
| 11 | Custom Exception 추가 필요 | common/exception/ | ⬜ 미해결 |
| 12 | N+1 Query 검증 | PortfolioService, MarketDataService | ⬜ 미해결 |
| 13 | Input Validation 강화 | 금융 계산 관련 서비스들 | ⬜ 미해결 |
| 14 | Deprecated updatePortfolioPrices() 정리 | PortfolioService.java | ⬜ 미해결 |
| 15 | 빈 exception 디렉토리 정리 | sentinel/exception/ | ⬜ 미해결 |

## 작업 진행 시

1. 위 항목 중 작업할 이슈 선택
2. 해당 파일 확인
3. TDD 방식으로 테스트 먼저 작성 (RED)
4. 구현 (GREEN)
5. 리팩토링 (REFACTOR)
6. 완료 시 이 파일의 상태를 ✅로 업데이트

## 관련 문서

- `PROJECT_STRUCTURE.md`: 프로젝트 구조
- `API_MAP.md`: API 목록
- QA 감사 보고서: 상세 분석 내용
