# Learning Log - 2025-12-04

## Redis 역직렬화 문제 해결

### 🆕 새로 배운 것

#### 1. GenericJackson2JsonRedisSerializer의 한계
- **기본 ObjectMapper 사용 시 Java 8 Time API 미지원**
  - `LocalDateTime`, `LocalDate` 등을 직렬화하려면 `JavaTimeModule` 필요
  - 기본 설정으로는 `InvalidDefinitionException` 발생

#### 2. Redis 직렬화에 필요한 ObjectMapper 설정
```java
@Bean
public ObjectMapper redisObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // ✅ Java 8 Time API 지원
    mapper.registerModule(new JavaTimeModule());

    // ✅ ISO-8601 포맷 사용 (타임스탬프 대신)
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // ✅ 타입 정보 포함 (역직렬화 시 정확한 타입 복원)
    mapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.EVERYTHING,
        JsonTypeInfo.As.PROPERTY
    );

    return mapper;
}
```

#### 3. DefaultTyping 옵션의 차이
- **NON_FINAL**: final이 아닌 클래스만 타입 정보 포함
  - 문제: Java record는 암묵적으로 final → 타입 정보 누락
  - 증상: `InvalidTypeIdException` 발생

- **EVERYTHING**: 모든 타입에 타입 정보 포함
  - 해결: record 타입도 정상 직렬화/역직렬화
  - 단점: JSON 크기 증가 (타입 정보 `@class` 필드 추가)

#### 4. DTO 설계와 직렬화
- **@Jacksonized만으로는 충분하지 않음**
  - @Jacksonized는 Builder 패턴을 Jackson에 알려주는 역할
  - 하지만 ObjectMapper에 JavaTimeModule이 없으면 LocalDateTime 처리 불가

- **Record vs Builder 패턴 DTO**
  - Record: 불변성 보장, 간결함 (테스트용 적합)
  - Builder 패턴 DTO: 복잡한 생성 로직, 선택적 필드 (프로덕션 적합)

---

## 🐛 해결한 문제

### Problem: Redis 캐시에서 PortfolioDto 역직렬화 실패

**증상**:
```
SerializationException: Could not write JSON:
Java 8 date/time type `java.time.LocalDateTime` not supported by default
```

**발생 위치**:
- `PortfolioService.getPortfolioById()` 메서드
- `@Cacheable(value = "portfolios")` 적용된 부분
- Redis 캐시에 DTO 저장 시도 시 발생

**영향**:
- Portfolio 캐시 기능 완전히 동작 불가
- 모든 조회 요청이 DB로 전달됨
- Phase 7 Redis 전환 목표 달성 불가

---

### Solution: 커스텀 ObjectMapper를 GenericJackson2JsonRedisSerializer에 주입

#### Before (CacheConfig.java:73)
```java
.serializeValuesWith(
    RedisSerializationContext.SerializationPair.fromSerializer(
        new GenericJackson2JsonRedisSerializer()  // ❌ 기본 ObjectMapper 사용
    )
);
```

#### After (CacheConfig.java:110-112)
```java
@Bean
public ObjectMapper redisObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.EVERYTHING,  // ✅ Record 타입도 지원
        JsonTypeInfo.As.PROPERTY
    );
    return mapper;
}

@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                  ObjectMapper redisObjectMapper) {  // ✅ 주입
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(redisObjectMapper)  // ✅ 커스텀 ObjectMapper
            )
        );
    // ...
}
```

---

### Learned

#### 1. 테스트가 실제 문제를 못 잡은 이유 (핵심!)

**RedisCacheIntegrationTest가 왜 실패하지 않았나?**

**원인**: 테스트용 DTO에 Java 8 Time API가 없었음
```java
// RedisCacheIntegrationTest.java:219-221
record StockPriceTestDto(String symbol, Double price) {}    // ✅ 단순 타입만
record CryptoPriceTestDto(String pair, Double price) {}      // ✅ 단순 타입만
record PortfolioTestDto(Long userId, String name) {}         // ✅ LocalDateTime 없음!
```

**실제 DTO는 LocalDateTime 포함**:
```java
// PortfolioDto.java:28-29
private LocalDateTime createdAt;     // ❌ JavaTimeModule 필요!
private LocalDateTime updatedAt;     // ❌ JavaTimeModule 필요!
```

**교훈**:
- **테스트 DTO는 실제 DTO와 동일한 구조여야 한다**
- 단순화된 테스트 데이터로는 프로덕션 문제를 발견 못함
- 복잡한 타입(LocalDateTime, List<T>, BigDecimal)을 포함한 통합 테스트 필수

#### 2. TDD의 올바른 적용
- **RED**: 실제 DTO를 사용한 테스트 작성
  ```java
  @Test
  void complex_dto_with_holdings_should_deserialize_correctly() {
      PortfolioDto result = portfolioService.getPortfolioById(...);
      // Redis에서 역직렬화 확인
  }
  ```

- **GREEN**: ObjectMapper 커스터마이징으로 통과

- **REFACTOR**: NON_FINAL → EVERYTHING 변경 (record 지원)

#### 3. 직렬화 설정의 중요성
- **Caffeine** (Local Cache): 직렬화 불필요 (객체 참조)
- **Redis** (Distributed Cache): 직렬화 필수 (네트워크 전송)
  - ObjectMapper 설정 누락 시 런타임 에러
  - 개발 환경에서 조기 발견 중요

#### 4. 측정과 검증
- **Before**: 테스트 실패 (SerializationException)
- **After**: 18개 테스트 모두 통과 ✅
- **검증 방법**:
  ```bash
  ./gradlew test --tests "*RedisCacheIntegrationTest"
  ./gradlew test --tests "*PortfolioCacheIntegrationTest"
  ```

---

## 📚 참고한 자료

### 공식 문서
- [Jackson Datatype: JSR310](https://github.com/FasterXML/jackson-modules-java8/tree/master/datetime)
- [Spring Data Redis - Serialization](https://docs.spring.io/spring-data/redis/docs/current/reference/html/#redis:serializer)
- [Jackson DefaultTyping](https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/ObjectMapper.DefaultTyping.html)

### Stack Overflow
- [Redis serialization with Java 8 LocalDateTime](https://stackoverflow.com/questions/31251929)

---

## 💡 적용 결과

### 테스트 결과
- **Before**: 18개 테스트 중 6개 실패
  - RedisCacheIntegrationTest: 5개 실패 (record 타입 문제)
  - PortfolioCacheIntegrationTest: 1개 실패 (객체 동일성 비교 문제)

- **After**: 18개 테스트 모두 통과 ✅
  - JavaTimeModule 추가 → LocalDateTime 직렬화 성공
  - DefaultTyping.EVERYTHING → record 타입 지원
  - 테스트 수정 → 필드 값 비교로 변경

### 코드 변경
```bash
modified:   src/main/java/com/pjsent/sentinel/config/CacheConfig.java (+33 lines)
modified:   src/test/java/com/pjsent/sentinel/portfolio/service/PortfolioCacheIntegrationTest.java (+25 lines)
```

### Git 커밋
```bash
git add src/main/java/com/pjsent/sentinel/config/CacheConfig.java
git add src/test/java/com/pjsent/sentinel/portfolio/service/PortfolioCacheIntegrationTest.java
git commit -m "fix(redis): add JavaTimeModule to ObjectMapper for LocalDateTime serialization

- Register JavaTimeModule for Java 8 Time API support
- Change DefaultTyping to EVERYTHING for record type support
- Add complex DTO deserialization test to catch LocalDateTime issues
- Fix test assertion to compare field values instead of object identity

Problem:
- GenericJackson2JsonRedisSerializer failed with LocalDateTime fields
- RedisCacheIntegrationTest used simple records without LocalDateTime
- Production DTO (PortfolioDto) has LocalDateTime fields

Solution:
- Custom ObjectMapper bean with JavaTimeModule
- activateDefaultTyping(EVERYTHING) for record support
- Integration test with actual Portfolio entity

Test Results:
- Before: 6 failed tests
- After: All 18 tests passed"
```

---

## 🔜 다음 작업

- [ ] k6 부하 테스트로 Redis 캐시 성능 측정
- [ ] Caffeine vs Redis 응답 시간 비교 (0ms vs 0.5-2ms)
- [ ] Phase 7 완료 후 EXPERIMENT_STATUS.md 업데이트
- [ ] Redis Cache Hit Rate 모니터링 (Actuator)

---

## 📊 성과 요약

### Before vs After

| 항목 | Before | After |
|------|--------|-------|
| 테스트 통과율 | 12/18 (67%) | 18/18 (100%) |
| LocalDateTime 직렬화 | ❌ 실패 | ✅ 성공 |
| Record 타입 지원 | ❌ 실패 | ✅ 성공 |
| Redis 캐시 사용 가능 | ❌ 불가 | ✅ 가능 |

### 핵심 교훈
1. **테스트 데이터는 프로덕션과 동일해야 한다**
2. **Redis = 직렬화 설정 필수 (ObjectMapper 커스터마이징)**
3. **DefaultTyping.EVERYTHING으로 record 타입 지원**
4. **TDD: 실제 시나리오를 재현하는 테스트 작성**

---

**Last Updated**: 2025-12-04 22:35
**Time Spent**: ~30분
**Files Changed**: 2
**Lines Added**: +58 (코드 +33, 테스트 +25)
**Tests**: 18/18 통과 ✅
