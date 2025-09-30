# 🧠 Backend Engineering Mindset - 함께 고민할 문제들

## 🎯 철학: "문제를 먼저 경험하고, 해결책을 함께 탐구"

실제 문제에 부딪힐 때마다 함께 고민하고 결정할 백엔드 엔지니어링 이슈들

---

## 🔄 **Concurrency & Thread Safety**

### **언제 마주칠 문제들**
```java
// 포트폴리오 가치 계산 중 race condition
@Service
public class PortfolioService {
    private double totalValue = 0.0;  // 이게 안전한가?

    public void updatePortfolioValue(Long portfolioId) {
        // 여러 사용자가 동시에 포트폴리오를 수정한다면?
        // DB에서 읽고 → 계산하고 → 저장하는 사이에 다른 요청이 들어온다면?
    }
}
```

### **함께 고민할 질문들**
- synchronized를 써야 할까? 성능은?
- DB 락을 걸까? 데드락 위험은?
- Optimistic Lock vs Pessimistic Lock?
- 아니면 애초에 stateless하게 설계?

---

## 📊 **Database Connection Management**

### **실제로 마주칠 상황**
```yaml
초기: 사용자 5명
- Connection pool 기본 설정으로 OK

성장: 사용자 50명
- "Connection timeout" 에러 발생
- 왜? Pool size가 작아서?
- 아니면 connection leak?
- 느린 쿼리 때문에 connection이 오래 점유?

더 성장: 사용자 500명
- Connection pool을 늘릴까?
- Read replica를 둘까?
- Connection을 아예 다르게 관리할까?
```

### **함께 고민할 점들**
- HikariCP 설정을 언제 튜닝해야 할까?
- Connection leak을 어떻게 찾을까?
- DB별로 connection pool을 나눠야 할까?

---

## 🔐 **Transaction Management**

### **복잡해지는 시나리오**
```java
@Transactional
public void rebalancePortfolio(Long portfolioId) {
    // 1. 현재 가격 조회 (외부 API)
    // 2. 새로운 allocation 계산
    // 3. 거래 기록 저장
    // 4. 포트폴리오 업데이트
    // 5. 알림 발송

    // 만약 3번에서 실패하면?
    // 외부 API 호출도 롤백되나?
    // 알림은 어떻게 처리하지?
}
```

### **함께 고민할 질문들**
- @Transactional의 범위를 어디까지?
- 외부 API 호출은 트랜잭션에 포함시킬까?
- Saga 패턴이 필요한 시점은?
- 보상 트랜잭션을 언제 구현할까?

---

## 🚀 **API Design & Error Handling**

### **진화하는 API 설계 고민**
```java
// 처음엔 간단하게
@GetMapping("/portfolios/{id}")
public Portfolio getPortfolio(@PathVariable Long id) {
    return portfolioService.findById(id);
}

// 그런데 문제들이 생긴다
// - 포트폴리오가 없으면?
// - 권한이 없는 사용자면?
// - 포트폴리오는 있는데 할당된 자산이 너무 많아서 느리면?
// - 클라이언트가 일부 정보만 필요하면?
```

### **함께 고민할 점들**
- 404 vs 403 vs 400 언제 뭘 써야 할까?
- Exception을 언제 catch하고 언제 propagate할까?
- API 버저닝을 언제부터 고려해야 할까?
- GraphQL이 필요한 시점은?

---

## 📈 **Performance & Scalability**

### **성능 문제가 나타나는 순간들**
```java
// N+1 문제
public List<PortfolioDto> getAllPortfolios() {
    List<Portfolio> portfolios = portfolioRepository.findAll();
    return portfolios.stream()
        .map(p -> {
            List<Asset> assets = assetRepository.findByPortfolioId(p.getId()); // N번 호출!
            return new PortfolioDto(p, assets);
        })
        .collect(Collectors.toList());
}
```

### **함께 고민할 질문들**
- Fetch Join vs @BatchSize vs 별도 쿼리?
- 페이징을 언제부터 도입할까?
- 인덱스는 어떤 걸 먼저 만들까?
- 쿼리 튜닝 vs 캐싱 vs 비정규화?

---

## 🔄 **Data Consistency & Eventual Consistency**

### **일관성 딜레마**
```java
// 포트폴리오 값 계산
// 1. 실시간 정확성 vs 성능
// 2. Strong consistency vs Eventual consistency

public double calculatePortfolioValue(Long portfolioId) {
    // 실시간 가격을 매번 API로 가져올까? (정확하지만 느림)
    // 캐시된 가격을 쓸까? (빠르지만 약간 부정확)
    // 비동기로 계산해서 결과만 저장할까?
}
```

### **함께 고민할 점들**
- 어느 정도의 지연은 허용할 수 있을까?
- 사용자에게 "계산 중..." 표시할까?
- 백그라운드 작업으로 처리할까?
- 이벤트 기반 아키텍처가 필요한 시점은?

---

## 🛡️ **Security & Authentication**

### **보안 고려사항들**
```java
// JWT vs Session
// - JWT: stateless하지만 revoke 어려움
// - Session: 서버 메모리 사용하지만 제어 쉬움

@GetMapping("/portfolios/{id}")
public Portfolio getPortfolio(
    @PathVariable Long id,
    Authentication auth  // 이 사용자가 이 포트폴리오를 볼 권한이 있나?
) {
    // RBAC? ABAC?
    // 데이터 레벨 보안은?
}
```

### **함께 고민할 질문들**
- 인증과 인가를 어떻게 분리할까?
- API 키 관리는?
- Rate limiting은 언제 필요할까?
- 민감한 데이터는 어떻게 처리할까?

---

## 📝 **Logging & Monitoring**

### **로깅 전략의 진화**
```java
// 처음엔 간단하게
logger.info("Portfolio created: " + portfolioId);

// 그런데 프로덕션에서는
// - 로그가 너무 많아서 중요한 걸 찾기 어려움
// - 성능에 영향을 줄까?
// - 민감한 정보가 로그에 남을까?
// - 분산 환경에서 추적이 어려움
```

### **함께 고민할 점들**
- 로그 레벨을 어떻게 나눌까?
- 구조화된 로깅이 필요한 시점은?
- APM 도구를 언제 도입할까?
- 메트릭과 로그의 차이는?

---

## 🔄 **Batch Processing & Scheduling**

### **배치 작업의 필요성**
```java
// 시장 데이터 업데이트
// - 실시간으로 할까? API 비용이 많이 나올텐데...
// - 배치로 할까? 지연은 어떻게 처리하지?

@Scheduled(fixedRate = 3600000) // 1시간마다
public void updateMarketData() {
    // 모든 자산의 가격을 업데이트
    // 만약 중간에 실패하면?
    // 다음 실행과 겹치면?
    // 실행 시간이 1시간을 넘으면?
}
```

### **함께 고민할 질문들**
- 배치 작업의 실패 처리는?
- 중복 실행 방지는?
- 배치와 실시간의 균형점은?
- 스케일 아웃된 환경에서는?

---

## 🎯 **Business Logic Organization**

### **코드 구조의 진화**
```java
// Domain-Driven Design?
// Layered Architecture?
// Hexagonal Architecture?

// Service가 너무 커졌을 때
@Service
public class PortfolioService {
    // 100줄의 비즈니스 로직
    // 여러 repository 의존
    // 외부 API 호출
    // 복잡한 계산

    // 이걸 어떻게 나눌까?
}
```

### **함께 고민할 점들**
- Service가 언제 너무 커진 걸까?
- Domain 객체에 로직을 둘까?
- CQRS가 필요한 시점은?
- 마이크로서비스 분리 기준은?

---

## 🔄 **Testing Strategy**

### **테스트의 현실적 고민**
```java
// 통합 테스트 vs 단위 테스트
@Test
public void 포트폴리오_생성_테스트() {
    // 실제 DB를 써야 할까?
    // Mock을 써야 할까?
    // 외부 API는 어떻게 테스트할까?
    // 테스트 데이터는 어떻게 관리할까?
}
```

### **함께 고민할 질문들**
- 테스트 커버리지 목표는?
- E2E 테스트 범위는?
- 테스트 환경 관리는?
- 테스트 속도 vs 신뢰성?

---

## 🚀 **Real-time Features**

### **실시간 요구사항**
```java
// 포트폴리오 값 실시간 업데이트
// - WebSocket? Server-Sent Events? Polling?
// - 모든 사용자에게 브로드캐스트?
// - 개별 포트폴리오만 업데이트?

@MessageMapping("/portfolio/{id}")
public void subscribeToPortfolio(@DestinationVariable Long id) {
    // 이 사용자에게만 업데이트를 보낼까?
    // 연결이 끊어지면?
    // 메시지 순서는?
}
```

### **함께 고민할 점들**
- 실시간의 정의는? (1초? 10초?)
- 네트워크 연결 관리는?
- 메시지 손실 처리는?
- 스케일 아웃에서의 실시간은?

---

## 📊 **Data Migration & Versioning**

### **스키마 변경의 현실**
```sql
-- 처음 설계
CREATE TABLE portfolios (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    total_value DECIMAL(15,2)
);

-- 몇 주 후... 요구사항 변경
-- 다중 통화 지원이 필요해졌다
-- 기존 데이터는?
-- 무중단 마이그레이션은?
-- 롤백 계획은?
```

### **함께 고민할 점들**
- 스키마 버저닝 전략은?
- 마이그레이션 중 서비스 중단은?
- 롤백 시나리오는?
- 데이터 정합성 검증은?

---

## 🎯 **개발하면서 함께 고민하는 방식**

### **각 Week별 만날 문제들**
```yaml
Week 1-2: 기본 CRUD
"어? 이 API가 생각보다 느리네? 왜 그럴까?"
→ DB 쿼리? Connection? 네트워크?

Week 3-4: 복잡한 로직
"포트폴리오 계산 중에 에러가 나면 어떻게 하지?"
→ 트랜잭션? 보상 처리? 사용자 통보?

Week 5-6: 성능 이슈
"사용자가 늘어나니까 응답이 느려졌어"
→ 캐싱? DB 최적화? 비동기 처리?

Week 7-8: 운영 준비
"프로덕션에서 문제가 생기면 어떻게 찾지?"
→ 로깅? 모니터링? 알림?
```

### **함께 탐구하는 과정**
1. **문제 인식**: "이상하네, 왜 그럴까?"
2. **원인 분석**: 여러 가능성 탐구
3. **해결책 비교**: 각각의 trade-off 분석
4. **실험**: 작은 규모로 테스트
5. **측정**: 실제 효과 확인
6. **결정**: 데이터 기반 선택
7. **회고**: 다음에는 어떻게 할까?

---

## 🤝 **함께 성장하는 마인드셋**

### **질문하는 습관**
- "이게 왜 느릴까?"
- "이 방법 말고 다른 방법은?"
- "스케일이 10배 커지면?"
- "이게 실패하면 어떻게 될까?"
- "사용자는 이걸 어떻게 받아들일까?"

### **실험하는 자세**
- 가정을 세우고 검증
- 작은 프로토타입으로 테스트
- 성능을 측정하고 비교
- 실패도 학습의 기회로

### **문서화하는 습관**
- 왜 이 결정을 했는지
- 어떤 다른 선택지가 있었는지
- 어떤 trade-off를 했는지
- 다음에는 어떻게 개선할지

---

이런 식으로 개발하면서 각 상황에 맞닥뜨릴 때마다 함께 고민하고 탐구해나가는 방식이면 어떨까요?

**실제 코드를 작성하면서 "아, 이 부분에서 이런 문제가 생기네?"** 하는 순간마다 함께 고민해보는 거죠!