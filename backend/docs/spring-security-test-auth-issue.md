# Spring Security 테스트에서 JWT 인증 처리 문제 해결

## 문제점

Controller 테스트 작성 중 Authentication 처리 방식이 일치하지 않아 500 에러 발생

### 실제 환경 (JwtAuthenticationFilter)
```java
Long userId = jwtService.getUserIdFromToken(jwt);
UsernamePasswordAuthenticationToken authToken =
    new UsernamePasswordAuthenticationToken(
        userId,  // Principal로 Long 타입 설정
        null,
        userDetails.getAuthorities()
    );
```

### Controller의 불일치
```java
// PortfolioController (정상)
Long userId = (Long) authentication.getPrincipal();

// RebalancingController (문제)
Long userId = Long.parseLong(authentication.getName());  // 타입 불일치
```

### 테스트 실패 원인
- `authentication.getName()`은 `principal.toString()`을 반환
- Long 타입이 String으로 변환되어 ClassCastException 발생

## 해결방안 탐색

### 1. Spring Security 기본 제공 방식
```java
@WithMockUser(username = "user")
```
**문제**: principal이 String이므로 Long userId와 타입 불일치

### 2. 수동으로 Authentication 설정
```java
.with(authentication(new UsernamePasswordAuthenticationToken(userId, null)))
```
**문제**: 모든 테스트마다 반복 작성 필요

### 3. Custom Annotation 생성 (채택)
실제 JWT 인증 방식과 동일하게 동작하는 커스텀 어노테이션 구현

## 해결 과정

### Step 1: Controller 통일
```java
// RebalancingController 수정
- Long userId = Long.parseLong(authentication.getName());
+ Long userId = (Long) authentication.getPrincipal();
```

### Step 2: Custom Annotation 생성
```java
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockJwtUserSecurityContextFactory.class)
public @interface WithMockJwtUser {
    long userId() default 1L;
    String email() default "test@example.com";
    String[] authorities() default {"ROLE_USER"};
}
```

### Step 3: SecurityContext Factory 구현
```java
public class WithMockJwtUserSecurityContextFactory
    implements WithSecurityContextFactory<WithMockJwtUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockJwtUser annotation) {
        // JwtAuthenticationFilter와 동일한 방식
        Authentication auth = new UsernamePasswordAuthenticationToken(
            annotation.userId(),  // Principal: Long
            null,
            userDetails.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
```

### Step 4: 테스트 코드 적용
```java
// 기존
mockMvc.perform(get("/api/v1/portfolios")
    .with(authentication(new UsernamePasswordAuthenticationToken(userId, null))))

// 개선
@Test
@WithMockJwtUser(userId = 1L)
void testMethod() {
    mockMvc.perform(get("/api/v1/portfolios"))
}
```

## 결과

### 테스트 통계
- 총 68개 테스트 작성
- 100% 통과 (0개 실패)
- 커버리지: 6개 Controller

### 개선 효과
1. **일관성**: 모든 Controller에서 `getPrincipal()` 사용
2. **재사용성**: `@WithMockJwtUser` 어노테이션으로 코드 간소화
3. **타입 안정성**: 컴파일 타임에 타입 검증
4. **실제 환경 반영**: JWT 인증 방식과 동일한 테스트 환경

### 주요 교훈
- 테스트 환경은 실제 운영 환경과 최대한 동일해야 함
- Custom Annotation을 활용하면 테스트 코드 품질 향상
- Principal 타입을 명확히 정의하여 런타임 에러 방지

## 참고 자료
- Spring Security Testing: https://docs.spring.io/spring-security/reference/servlet/test/
- @WithSecurityContext: https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/test/context/support/WithSecurityContext.html
