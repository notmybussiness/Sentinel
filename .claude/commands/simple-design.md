# Simple Design - Kent Beck의 단순한 설계

## 🎯 Kent Beck의 4 Rules

### 1. 테스트 통과 (Tests Pass)
```
모든 테스트가 통과해야 한다
→ 가장 중요한 규칙
```

### 2. 의도 명확 (Reveals Intention)
```
코드가 무엇을 하는지 명확해야 한다
→ 변수명, 메서드명, 클래스명
```

### 3. 중복 제거 (No Duplication)
```
중복된 코드가 없어야 한다
→ DRY (Don't Repeat Yourself)
```

### 4. 최소 요소 (Minimal Classes/Methods)
```
필요한 최소한의 클래스와 메서드만
→ YAGNI (You Aren't Gonna Need It)
```

---

## 🔍 체크 방법

### Rule 1: 테스트 통과
```bash
./gradlew test

# 모든 테스트 통과?
✅ Yes → 다음 단계
❌ No → 먼저 고치기
```

### Rule 2: 의도 명확
```java
// ❌ Bad
public List<User> get(int x) {
    return repo.findAll().stream()
        .filter(u -> u.getAge() > x)
        .collect(Collectors.toList());
}

// ✅ Good
public List<User> findUsersOlderThan(int age) {
    return userRepository.findByAgeGreaterThan(age);
}
```

**체크**:
- [ ] 메서드명이 동작을 설명하는가?
- [ ] 변수명이 의미를 담고 있는가?
- [ ] 주석 없이 이해 가능한가?

### Rule 3: 중복 제거
```java
// ❌ Bad - 중복 코드
public void sendEmailAlert(Alert alert) {
    String subject = "Price Alert: " + alert.getSymbol();
    String body = "Target price reached: " + alert.getTargetPrice();
    emailService.send(alert.getUserEmail(), subject, body);
}

public void sendSmsAlert(Alert alert) {
    String message = "Price Alert: " + alert.getSymbol() + 
                     " - Target: " + alert.getTargetPrice();
    smsService.send(alert.getUserPhone(), message);
}

// ✅ Good - 중복 제거
public void sendAlert(Alert alert, NotificationChannel channel) {
    AlertMessage message = AlertMessage.from(alert);
    notificationService.send(alert.getUser(), message, channel);
}
```

**체크**:
- [ ] 같은 로직이 2번 이상 나오는가?
- [ ] 공통 로직을 추출할 수 있는가?

### Rule 4: 최소 요소
```java
// ❌ Bad - 불필요한 추상화
interface PriceRepository {}
interface PriceService {}
interface PriceController {}
class PriceRepositoryImpl implements PriceRepository {}
class PriceServiceImpl implements PriceService {}
class PriceControllerImpl implements PriceController {}

// ✅ Good - 필요한 것만
class PriceRepository extends JpaRepository {}
class PriceService {}
class PriceController {}
```

**체크**:
- [ ] 사용하지 않는 클래스/메서드가 있는가?
- [ ] 미래를 위한 과도한 설계인가?
- [ ] 정말 지금 필요한가?

---

## 🚀 실전 적용

### Before (복잡한 설계)
```java
public class PortfolioCalculationStrategy {
    public abstract BigDecimal calculate(Portfolio portfolio);
}

public class TotalValueCalculationStrategy extends PortfolioCalculationStrategy {
    @Override
    public BigDecimal calculate(Portfolio portfolio) {
        return portfolio.getAssets().stream()
            .map(Asset::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

public class PortfolioService {
    private PortfolioCalculationStrategy strategy;
    
    public BigDecimal getTotalValue(Portfolio portfolio) {
        return strategy.calculate(portfolio);
    }
}
```
→ 과도한 추상화 (YAGNI 위반)

### After (단순한 설계)
```java
public class Portfolio {
    public BigDecimal calculateTotalValue() {
        return assets.stream()
            .map(Asset::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

public class PortfolioService {
    public BigDecimal getTotalValue(Portfolio portfolio) {
        return portfolio.calculateTotalValue();
    }
}
```
→ 간단하고 명확!

---

## 📝 체크리스트

```
/simple-design 실행 시:

✅ 1. 모든 테스트 통과?
✅ 2. 코드가 자기 설명적인가?
   - 변수명 명확
   - 메서드명 동작 설명
   - 주석 최소화
✅ 3. 중복 코드 없음?
   - 같은 로직 2번 이상 X
✅ 4. 불필요한 추상화 없음?
   - 지금 필요한 것만
   - YAGNI 준수
```

---

## 🎓 포트폴리오 강점

```
면접관: "설계 원칙은 무엇을 따르나요?"

당신: "Kent Beck의 Simple Design 4 Rules를 따릅니다:
      1. 테스트 통과
      2. 의도 명확
      3. 중복 제거
      4. 최소 요소
      
      실제로 Portfolio 계산 로직에서
      과도한 Strategy 패턴을 제거하고
      단순한 메서드로 리팩토링했습니다."

면접관: "왜 단순함을 추구하나요?"

당신: "복잡한 코드는 버그를 숨기고,
      유지보수를 어렵게 만듭니다.
      단순한 코드가 이해하기 쉽고,
      변경하기 쉽습니다."
```
