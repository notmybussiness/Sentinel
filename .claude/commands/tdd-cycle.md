# TDD Cycle - Test-Driven Development

## 🎯 목적
**Kent Beck의 TDD: Red → Green → Refactor**
- 테스트 먼저 작성 (Red)
- 최소 구현 (Green)
- 리팩토링 (Refactor)
- Atomic commit으로 이력 증명

---

## 🔴 Step 1: Red - 실패하는 테스트 작성

### 테스트 먼저 작성
```java
// PortfolioServiceTest.java
@Test
@DisplayName("사용자 ID로 포트폴리오 조회 시 자산 목록도 함께 반환")
void findByUserId_ShouldReturnPortfolioWithAssets() {
    // Given
    Long userId = 1L;
    
    // When
    Portfolio portfolio = portfolioService.findByUserId(userId);
    
    // Then
    assertThat(portfolio).isNotNull();
    assertThat(portfolio.getAssets()).isNotEmpty();
    assertThat(portfolio.getAssets()).hasSize(3);
    // 이 시점에서 테스트는 실패! (구현 안 됨)
}
```

### 테스트 실행 → 실패 확인
```bash
./gradlew test --tests PortfolioServiceTest

# 출력
❌ findByUserId_ShouldReturnPortfolioWithAssets FAILED
   java.lang.NullPointerException: portfolio is null
```

### Red 커밋
```bash
git add src/test/java/com/sentinel/service/PortfolioServiceTest.java
git commit -m "test: add failing test for portfolio with assets (RED)"
```

---

## 🟢 Step 2: Green - 최소 구현

### 테스트를 통과시키는 최소한의 코드
```java
// PortfolioService.java
public Portfolio findByUserId(Long userId) {
    Portfolio portfolio = portfolioRepository.findByUserId(userId);
    
    // 간단하지만 N+1 발생하는 코드
    // 일단 테스트만 통과시키기!
    return portfolio;
}

// PortfolioRepository.java
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Portfolio findByUserId(Long userId);
}
```

### 테스트 재실행 → 통과 확인
```bash
./gradlew test --tests PortfolioServiceTest

# 출력
✅ findByUserId_ShouldReturnPortfolioWithAssets PASSED
```

### Green 커밋
```bash
git add src/main/java/com/sentinel/service/PortfolioService.java
git add src/main/java/com/sentinel/repository/PortfolioRepository.java
git commit -m "feat: implement portfolio finder (GREEN)"
```

---

## 🔵 Step 3: Refactor - 리팩토링

### 성능 개선 (N+1 해결)
```java
// PortfolioRepository.java
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    
    @EntityGraph(attributePaths = {"assets", "assets.cryptocurrency"})
    Portfolio findByUserId(Long userId);
}
```

### 테스트 재실행 → 여전히 통과
```bash
./gradlew test --tests PortfolioServiceTest

# 출력
✅ findByUserId_ShouldReturnPortfolioWithAssets PASSED
```

### Refactor 커밋
```bash
git add src/main/java/com/sentinel/repository/PortfolioRepository.java
git commit -m "refactor: resolve N+1 with @EntityGraph (REFACTOR)"
```

---

## 📊 TDD 사이클 Git History

```bash
git log --oneline -3

# 출력 (역순)
c3d4e5f refactor: resolve N+1 with @EntityGraph (REFACTOR)
b2c3d4e feat: implement portfolio finder (GREEN)
a1b2c3d test: add failing test for portfolio with assets (RED)
```

**포트폴리오 강점**:
- ✅ TDD로 개발한 명확한 증거
- ✅ Red → Green → Refactor 흐름 보임
- ✅ 테스트 우선 개발 습관

---

## 🎓 TDD의 이점

### 1. 설계 개선
```
테스트를 먼저 작성하면:
- 인터페이스가 명확해짐
- 의존성이 줄어듦
- 테스트 가능한 코드가 됨
```

### 2. 버그 조기 발견
```
구현 전 테스트 작성:
- 예외 케이스 미리 고려
- Edge case 누락 방지
```

### 3. 리팩토링 안전성
```
테스트가 있으면:
- 리팩토링 후에도 동작 보장
- 회귀 버그 방지
```

### 4. 문서화
```
테스트 자체가 문서:
- 사용법 예시
- 기대 동작 명시
```

---

## 🧪 테스트 작성 팁

### Given-When-Then 패턴
```java
@Test
void testName() {
    // Given: 테스트 준비
    User user = new User("test@example.com");
    
    // When: 테스트 실행
    Portfolio portfolio = portfolioService.create(user);
    
    // Then: 검증
    assertThat(portfolio).isNotNull();
    assertThat(portfolio.getUser()).isEqualTo(user);
}
```

### 테스트 이름 짓기
```java
// ❌ Bad
@Test
void test1() { }

// ✅ Good
@Test
@DisplayName("유효하지 않은 사용자 ID로 조회 시 예외 발생")
void findByUserId_WithInvalidId_ShouldThrowException() { }
```

### AAA 패턴 (Arrange-Act-Assert)
```java
@Test
void calculateTotalValue_ShouldSumAllAssetValues() {
    // Arrange: 준비
    Portfolio portfolio = createPortfolioWithAssets();
    
    // Act: 실행
    BigDecimal totalValue = portfolio.calculateTotalValue();
    
    // Assert: 검증
    assertThat(totalValue).isEqualByComparingTo(new BigDecimal("1000.00"));
}
```

---

## 🚀 TDD 실전 워크플로우

### Scenario: 가격 알림 기능 추가

#### 1. Red - 테스트 작성
```java
@Test
@DisplayName("목표 가격 도달 시 알림 전송")
void sendAlert_WhenTargetPriceReached() {
    // Given
    Alert alert = Alert.builder()
        .userId(1L)
        .symbol("BTC")
        .targetPrice(new BigDecimal("50000"))
        .build();
    
    Price currentPrice = new Price("BTC", new BigDecimal("50100"));
    
    // When
    boolean sent = alertService.checkAndSend(alert, currentPrice);
    
    // Then
    assertThat(sent).isTrue();
    verify(notificationService).send(any(AlertNotification.class));
}
```

**커밋**: `test: add alert notification test (RED)`

#### 2. Green - 최소 구현
```java
@Service
public class AlertService {
    
    @Autowired
    private NotificationService notificationService;
    
    public boolean checkAndSend(Alert alert, Price currentPrice) {
        if (currentPrice.getValue().compareTo(alert.getTargetPrice()) >= 0) {
            AlertNotification notification = new AlertNotification(alert, currentPrice);
            notificationService.send(notification);
            return true;
        }
        return false;
    }
}
```

**커밋**: `feat: implement alert notification (GREEN)`

#### 3. Refactor - 개선
```java
@Service
public class AlertService {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AlertRepository alertRepository;
    
    public boolean checkAndSend(Alert alert, Price currentPrice) {
        if (!isTargetPriceReached(alert, currentPrice)) {
            return false;
        }
        
        sendNotification(alert, currentPrice);
        markAsTriggered(alert);
        return true;
    }
    
    private boolean isTargetPriceReached(Alert alert, Price currentPrice) {
        return currentPrice.getValue().compareTo(alert.getTargetPrice()) >= 0;
    }
    
    private void sendNotification(Alert alert, Price currentPrice) {
        AlertNotification notification = AlertNotification.of(alert, currentPrice);
        notificationService.send(notification);
    }
    
    private void markAsTriggered(Alert alert) {
        alert.markAsTriggered();
        alertRepository.save(alert);
    }
}
```

**커밋**: `refactor: extract methods in AlertService (REFACTOR)`

---

## 📝 TDD 체크리스트

### 테스트 작성 시
- [ ] 실패하는 테스트부터 작성
- [ ] Given-When-Then 구조 사용
- [ ] 명확한 테스트 이름
- [ ] 하나의 테스트는 하나의 시나리오만

### 구현 시
- [ ] 테스트를 통과시키는 최소 코드
- [ ] 과도한 설계 지양
- [ ] Red → Green 확인

### 리팩토링 시
- [ ] 테스트 통과 유지
- [ ] 중복 제거
- [ ] 가독성 개선
- [ ] 성능 최적화

---

## 🎯 포트폴리오 활용

### 면접 대응
```
면접관: "TDD 경험이 있나요?"

당신: "네, Sentinel 프로젝트에서 모든 기능을 TDD로 개발했습니다.
      Git history를 보시면 Red-Green-Refactor 사이클로
      커밋된 것을 확인하실 수 있습니다."

면접관: "예시를 보여주실 수 있나요?"

당신: "가격 알림 기능을 개발할 때,
      1. 먼저 실패하는 테스트 작성 (RED commit)
      2. 최소 구현으로 통과 (GREEN commit)
      3. 메서드 추출로 리팩토링 (REFACTOR commit)
      
      이렇게 3개 커밋으로 관리했습니다."
```

### Git History 증거
```bash
# TDD 커밋만 필터링
git log --grep="RED\|GREEN\|REFACTOR" --oneline

# 출력
a1b2c3d test: add alert notification test (RED)
b2c3d4e feat: implement alert notification (GREEN)
c3d4e5f refactor: extract methods in AlertService (REFACTOR)
d4e5f6g test: add price validation test (RED)
e5f6g7h feat: implement price validator (GREEN)
```

---

## 🛠️ 자동화 (선택)

### TDD 커밋 템플릿
```bash
# .git/commit-template-tdd
# TDD Phase: [RED/GREEN/REFACTOR]
#
# test: [RED] add failing test for [feature]
# feat: [GREEN] implement [feature]
# refactor: [REFACTOR] improve [aspect]

git config commit.template .git/commit-template-tdd
```

### 테스트 커버리지 측정
```gradle
// build.gradle
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.9"
}

test {
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    reports {
        html.required = true
        csv.required = false
    }
}
```

```bash
# 커버리지 확인
./gradlew test jacocoTestReport

# 결과
build/reports/jacoco/test/html/index.html
```
