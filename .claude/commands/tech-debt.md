# Tech Debt - 기술 부채 추적

## 🎯 목적
**기술 부채 식별 및 관리**
- 복잡한 코드 탐지
- TODO/FIXME 추적
- 테스트 누락 식별

---

## 🔍 자동 탐지

### 1. 복잡도 높은 메서드
```bash
# Cyclomatic Complexity > 10
find src/main/java -name "*.java" | xargs grep -l "if\|else\|for\|while" | head -10
```

### 2. 테스트 없는 Service
```bash
# Service 있는데 Test 없음
SERVICES=$(find src/main/java -name "*Service.java")
for service in $SERVICES; do
    test_file="${service/main/test}"
    test_file="${test_file/.java/Test.java}"
    if [ ! -f "$test_file" ]; then
        echo "❌ Missing test: $service"
    fi
done
```

### 3. TODO/FIXME 목록
```bash
grep -rn "TODO\|FIXME" src/main/java --include="*.java"
```

---

## 📊 기술 부채 리포트

```markdown
# Tech Debt Report - 2025-01-15

## 🔴 High Priority
1. **PortfolioService.calculateTotalValue()**
   - Complexity: 12 (권장: < 7)
   - Lines: 45 (권장: < 30)
   - Action: 메서드 분리 필요

## 🟡 Medium Priority
2. **PriceService** - 테스트 없음
   - Coverage: 0%
   - Action: 단위 테스트 작성

## 🟢 Low Priority
3. **TODO**: Cache invalidation 전략
   - Location: CacheConfig.java:42
   - Action: 다음 스프린트
```

---

## 🛠️ 우선순위 결정

```
High: 복잡도 > 10 + 테스트 없음
Medium: 복잡도 > 7 or 테스트 없음
Low: TODO/FIXME
```

---

## 🎓 포트폴리오 활용

```
면접관: "기술 부채 어떻게 관리하나요?"

당신: "주기적으로 복잡도와 테스트 커버리지를 체크하고,
      우선순위를 정해서 리팩토링합니다.
      복잡도 > 10인 메서드는 반드시 분리합니다."
```
