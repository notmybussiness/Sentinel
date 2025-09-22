# Problem Solving Log

## Spring Security 설정 문제 (2025-01-05)

### 문제 상황
- Spring Security에서 `permitAll()` 설정을 했음에도 불구하고 403 Forbidden 오류 발생
- 메인페이지(`/`)와 Kakao OAuth 엔드포인트(`/api/v1/auth/kakao`) 접근 불가

### 원인 분석
```java
// 문제가 있던 설정
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
    .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers("/h2-console/**").permitAll()
    .requestMatchers("/**").permitAll()  // 이 설정이 있어도
    .anyRequest().authenticated()        // 이 설정이 나중에 와서 덮어씀
)
```

**핵심 문제**: Spring Security에서 `requestMatchers`와 `anyRequest()`의 순서가 중요함
- `anyRequest()`는 모든 요청을 의미
- `anyRequest().authenticated()`가 `permitAll()` 설정을 덮어씀

### 해결 방법
```java
// 수정된 설정
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
    .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers("/h2-console/**").permitAll()
    .anyRequest().permitAll()  // 모든 요청 허용 (개발용)
)
```

### 결과
- ✅ 메인페이지 접근 가능 (404 오류로 변경 - 컨트롤러 없음)
- ✅ Kakao OAuth 엔드포인트 정상 동작 (200 응답)
- ✅ Spring Security 설정이 올바르게 적용됨

### 교훈
1. **Spring Security 설정 순서 중요**: `requestMatchers` → `anyRequest()` 순서로 설정
2. **개발 환경에서는 `anyRequest().permitAll()` 사용**: 모든 요청 허용으로 테스트 용이
3. **프로덕션 환경에서는 세밀한 권한 설정 필요**: 보안을 위해 필요한 엔드포인트만 허용

### 관련 파일
- `src/main/java/com/pjsent/sentinel/common/config/SecurityConfig.java`
- 패키지 이동: `user.config` → `common.config`

---

## Config 리팩토링 (2025-01-05)

### 문제 상황
- Config 클래스들이 여러 위치에 분산되어 있음
- `config/`와 `user/config/`에 중복된 설정 존재

### 해결 방법
```
기존 구조:
├── config/
│   ├── AsyncConfig.java
│   └── RestTemplateConfig.java
└── user/
    └── config/
        ├── SecurityConfig.java
        └── JwtAuthenticationFilter.java

개선된 구조:
├── common/
│   └── config/                    # 공통 설정 통합
│       ├── SecurityConfig.java
│       ├── JwtAuthenticationFilter.java
│       ├── RestTemplateConfig.java
│       └── AsyncConfig.java
└── user/                         # 사용자 도메인
    ├── controller/
    ├── dto/
    ├── entity/
    ├── repository/
    └── service/
```

### 장점
- **유지보수성 향상**: 설정 변경 시 한 곳에서 관리
- **의존성 명확화**: 공통 설정과 도메인별 로직 분리
- **확장성**: 새로운 도메인 추가 시 공통 설정 재사용 가능
- **일관성**: 모든 도메인이 동일한 보안/HTTP 설정 사용

---

## ID 타입 통일 (2025-01-05)

### 문제 상황
- `PortfolioController`에서 `UUID userId` 사용
- `PortfolioService`와 `Portfolio` 엔티티에서 `Long userId` 사용
- 타입 불일치로 인한 컴파일 오류 발생

### 해결 방법
- 전체 시스템에서 ID 타입을 `Long`으로 통일
- `PortfolioController`의 모든 메서드에서 `@RequestParam Long userId` 사용

### 관련 파일
- `src/main/java/com/pjsent/sentinel/portfolio/controller/PortfolioController.java`
- `src/main/java/com/pjsent/sentinel/portfolio/service/PortfolioService.java`
- `src/main/java/com/pjsent/sentinel/user/entity/User.java`
- `src/main/java/com/pjsent/sentinel/portfolio/entity/Portfolio.java`

---

## JWT 라이브러리 API 호환성 (2025-01-05)

### 문제 상황
- JWT 라이브러리 버전 0.12.3에서 `Jwts.parserBuilder()` 메서드 사용 불가
- `JwtParserBuilder`와 `JwtParser` 타입 불일치

### 해결 방법
```java
// 문제가 있던 코드
JwtParser parser = Jwts.parserBuilder()
    .setSigningKey(getSigningKey())
    .build();

// 수정된 코드
JwtParser parser = Jwts.parser()
    .setSigningKey(getSigningKey());
```

### 교훈
- JWT 라이브러리 버전에 따른 API 차이 확인 필요
- `io.jsonwebtoken:jjwt-api:0.12.3`에서는 `parserBuilder()` 대신 `parser()` 사용

---

## 빈 이름 충돌 (2025-01-05)

### 문제 상황
- `RestTemplateConfig` 빈 이름이 두 곳에서 정의됨
- `com.pjsent.sentinel.config.RestTemplateConfig`
- `com.pjsent.sentinel.user.config.RestTemplateConfig`

### 해결 방법
- 중복된 `user.config.RestTemplateConfig` 삭제
- `common.config.RestTemplateConfig`만 유지

### 교훈
- Spring Boot에서 빈 이름은 유일해야 함
- Config 리팩토링 시 중복 빈 정의 주의 필요

---

## 모노레포 vs 멀티레포 아키텍처 선택 (2025-09-07)

### 프로젝트 현황
Project Sentinel이 성장하면서 프론트엔드(Next.js 14)와 백엔드(Spring Boot)를 분리해야 하는 시점에 도달했습니다. 이때 두 가지 아키텍처 옵션을 고려했습니다.

### 아키텍처 옵션 비교

#### 🏢 **멀티레포 (Multi-Repository) 방식**
**구조**:
```
sentinel-frontend/     (별도 저장소)
├── package.json
├── src/
└── ...

sentinel-backend/      (별도 저장소)  
├── build.gradle
├── src/main/java/
└── ...
```

**장점**:
- 독립적 개발 및 배포
- 각 팀별 권한 관리 용이
- 빌드 시스템 분리로 성능 최적화
- 기술 스택별 전문화

**단점**:
- 의존성 관리 복잡성
- API 인터페이스 변경 시 동기화 이슈
- 통합 테스트 어려움
- 코드 공유 및 재사용성 저하

#### 🏠 **모노레포 (Monorepo) 방식** ✅ **선택된 방식**
**구조**:
```
Sentinel/              (단일 저장소)
├── frontend/
│   ├── package.json
│   ├── src/
│   └── ...
├── backend/
│   ├── build.gradle
│   ├── src/main/java/
│   └── ...
├── package.json       (워크스페이스 관리)
├── docker-compose.yml
└── docs/
```

**장점**:
- 통합 코드베이스 관리
- API 변경 시 즉시 동기화 가능
- 공통 설정 및 스크립트 공유
- 통합 CI/CD 파이프라인
- 전체 프로젝트 검색 및 리팩토링 용이

**단점**:
- 저장소 크기 증가
- 빌드 시스템 복잡도 증가
- 권한 관리 세밀화 어려움

### 선택 근거: 왜 모노레포인가?

#### 1. **개발 효율성 우선**
- 현재 1-2명의 소규모 팀으로 시작
- API 변경이 빈번한 초기 개발 단계
- 프론트엔드-백엔드 간 긴밀한 협업 필요

#### 2. **타입 안전성 확보**
```typescript
// 백엔드 API 변경 시 프론트엔드도 즉시 감지 가능
interface StockPriceResponse {
  symbol: string;
  price: number;
  changePercent: number; // 백엔드에서 추가되면 즉시 반영
}
```

#### 3. **통합 개발 워크플로우**
```json
{
  "scripts": {
    "dev": "concurrently \"npm run dev:backend\" \"npm run dev:frontend\"",
    "build": "npm run build:backend && npm run build:frontend",
    "test": "npm run test:backend && npm run test:frontend"
  }
}
```

#### 4. **일관된 도구 체인**
- 통일된 ESLint, Prettier 설정
- 공통 Docker 설정
- 동일한 CI/CD 파이프라인

#### 5. **마이그레이션 용이성**
- 향후 필요시 멀티레포로 분리 가능
- Git subtree를 이용한 분리 전략 보유

### 구현 결과

#### ✅ **성공적인 모노레포 전환**
- **프론트엔드**: Next.js 14 + TypeScript + Tailwind CSS
- **백엔드**: Spring Boot 3.5.5 + Java 17
- **개발 서버**: 동시 실행 (frontend:3000, backend:8080)
- **Docker**: 통합 컨테이너 오케스트레이션

#### 📊 **성능 검증**
- **백엔드 API**: ✅ 정상 동작 (AAPL 주식 데이터 응답)
- **프론트엔드**: ✅ 현대적 대시보드 UI 완성
- **빌드 시간**: 백엔드 6초, 프론트엔드 2초
- **개발 생산성**: 단일 명령어로 전체 스택 실행

#### 🚀 **향후 확장성**
- **마이크로서비스 준비**: 도메인별 패키지 구조
- **팀 확장 대비**: 워크스페이스 기반 권한 분리 가능
- **배포 전략**: Docker Compose로 통합 배포 또는 개별 배포 선택

### 교훈

1. **스타트업 단계에서는 모노레포가 효율적**
   - 빠른 프로토타입 개발
   - API 변경에 대한 즉각적인 피드백

2. **도구 체인 통합의 중요성**
   - concurrently로 개발 서버 동시 실행
   - 공통 설정으로 일관성 유지

3. **미래 지향적 설계**
   - 모노레포 → 멀티레포 마이그레이션 경로 확보
   - 도메인 경계 명확화로 향후 분리 용이

4. **개발자 경험(DX) 최적화**
   - 단일 명령어로 전체 환경 실행
   - 통합 문서화 및 설정 관리

### 결론
Project Sentinel의 현재 상황(소규모 팀, 빈번한 API 변경, 빠른 개발 속도 요구)에서는 **모노레포 방식이 최적의 선택**이었습니다. 향후 팀이 확장되고 서비스가 성숙해지면 멀티레포로의 전환을 고려할 수 있지만, 현재로서는 개발 효율성과 코드 품질 모두를 만족하는 아키텍처를 구축했습니다.
