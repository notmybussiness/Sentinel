# Code Review Self - 셀프 코드 리뷰

## 🎯 목적
**PR 전 자가 점검으로 리뷰 품질 향상**
- 코드 품질 의식 증명
- 리뷰어 시간 절약
- 버그 조기 발견

---

## ✅ 체크리스트

### 1. 코드 품질
```
- [ ] 메서드 길이 < 30줄
- [ ] 중복 코드 없음
- [ ] 매직 넘버 상수화
- [ ] 의미 있는 변수명
- [ ] 주석 필요한 곳만
```

### 2. 성능
```
- [ ] N+1 쿼리 없음
- [ ] 불필요한 DB 조회 없음
- [ ] 적절한 인덱스 사용
- [ ] 외부 API 호출 최소화
```

### 3. 테스트
```
- [ ] 단위 테스트 작성
- [ ] 커버리지 > 80%
- [ ] Edge case 테스트
- [ ] 모든 테스트 통과
```

### 4. 보안
```
- [ ] SQL Injection 방어
- [ ] XSS 방어
- [ ] 권한 체크
- [ ] 민감정보 로깅 없음
```

### 5. 문서화
```
- [ ] README 업데이트
- [ ] API 문서 갱신
- [ ] ADR 작성 (기술 결정 시)
- [ ] 마이그레이션 가이드 (Breaking Change)
```

---

## 🔍 자동 분석

### 실행 시 체크
```bash
# 1. 변경된 파일 개수
CHANGED_FILES=$(git diff --name-only HEAD | wc -l)
if [ $CHANGED_FILES -gt 10 ]; then
    echo "⚠️ 변경 파일 $CHANGED_FILES개 (권장: < 10개)"
fi

# 2. 코드 라인 수
LINES_CHANGED=$(git diff --shortstat HEAD | grep -oE '[0-9]+ insertions')
if [ "${LINES_CHANGED% *}" -gt 400 ]; then
    echo "⚠️ 변경 라인 ${LINES_CHANGED% *}줄 (권장: < 400줄)"
fi

# 3. TODO/FIXME 추가
NEW_TODOS=$(git diff HEAD | grep "^+.*TODO\|^+.*FIXME" | wc -l)
if [ $NEW_TODOS -gt 0 ]; then
    echo "⚠️ TODO/FIXME $NEW_TODOS개 추가됨"
fi

# 4. 테스트 커버리지
./gradlew test jacocoTestReport
COVERAGE=$(grep -oP 'Total.*?(\d+)%' build/reports/jacoco/test/html/index.html)
echo "📊 테스트 커버리지: $COVERAGE"
```

---

## 📊 리뷰 리포트 예시

```markdown
# Self Code Review - 2025-01-15

## 변경 요약
- Files: 8개
- Lines: +150 -30
- Commits: 3개

## 체크리스트 결과
✅ 코드 품질: 통과
✅ 성능: 통과 (N+1 없음)
✅ 테스트: 통과 (커버리지 87%)
⚠️ 문서: README 업데이트 필요

## 잠재적 이슈
1. PortfolioService.calculateTotalValue()
   - 복잡도: 8 (권장: < 7)
   - 제안: 메서드 분리

## 성능 영향
- Before: 2500ms
- After: 150ms
- 개선율: 94% ✅

## 다음 액션
- [ ] PortfolioService 리팩토링
- [ ] README 업데이트
```

---

## 🎓 포트폴리오 활용

```
면접관: "코드 품질을 어떻게 유지하나요?"

당신: "PR 전에 항상 셀프 코드 리뷰를 합니다.
      체크리스트로 품질, 성능, 테스트, 보안을 점검하고
      자동 분석 도구로 메트릭을 확인합니다.
      
      예를 들어:
      - 변경 파일 < 10개
      - 라인 수 < 400줄
      - 테스트 커버리지 > 80%
      
      기준을 벗어나면 리팩토링합니다."
```
