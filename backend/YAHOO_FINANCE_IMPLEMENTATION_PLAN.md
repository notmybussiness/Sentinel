# Yahoo Finance Provider 구현 계획

## 📋 프로젝트 개요

**목표**: AlphaVantage API 제한 해결을 위한 Yahoo Finance API 통합
**현재 문제**: AlphaVantage 무료 플랜 25회/일 제한으로 실제 데이터 조회 불가
**해결방안**: Yahoo Finance 비공식 API를 Primary Provider로 사용

## 🏗️ 현재 아키텍처

### 기존 구조
```
MarketDataController
├── MarketDataService (Fallback 로직)
├── MarketDataProviderFactory (프로바이더 관리)
└── MarketDataProvider (인터페이스)
    └── AlphaVantageProvider (@Component)
```

### 기존 API 엔드포인트
- `GET /api/v1/market/price/{symbol}` - 단일 주식
- `GET /api/v1/market/bigtech-m7` - 빅테크 M7
- `GET /api/v1/market/prices?symbols=` - 복수 주식

## 🎯 구현 단계

### Phase 1: 기본 YahooFinanceProvider 구현
**목표**: 미국 주식 기본 조회 기능
**예상 시간**: 2-3시간

#### 1.1 YahooFinanceProvider 클래스 생성
```java
@Component
@Order(1) // 최고 우선순위
@Slf4j
public class YahooFinanceProvider implements MarketDataProvider
```

#### 1.2 핵심 메서드 구현
- `getMarketData(String symbol)`: 단일 주식 데이터 조회
- `isAvailable()`: 서비스 가용성 체크
- `getProviderName()`: "Yahoo Finance" 반환

#### 1.3 Yahoo Finance API 통합
- **엔드포인트**: `https://query1.finance.yahoo.com/v8/finance/chart/{symbol}`
- **응답 파싱**: JSON → StockPriceDto 변환
- **타임아웃**: 연결 5초, 읽기 10초

#### 1.4 검증 대상
- AAPL, MSFT, GOOGL, AMZN, TSLA, META, NVDA 테스트

---

### Phase 2: 우선순위 설정 및 통합
**목표**: 기존 시스템과 완전 통합
**예상 시간**: 1-2시간

#### 2.1 Spring Order 설정
```java
YahooFinanceProvider @Order(1)  // 최우선
AlphaVantageProvider @Order(2)   // 2순위
```

#### 2.2 Fallback 테스트
- Yahoo Finance 실패 → AlphaVantage 시도 시나리오
- 모든 프로바이더 실패 → 적절한 예외 처리

#### 2.3 RestTemplate 설정 추가
```java
@Bean
public RestTemplate yahooRestTemplate() {
    // User-Agent 헤더 필수
    // 타임아웃 설정
    // 인터셉터로 로깅
}
```

---

### Phase 3: 한국 주식 지원 추가
**목표**: 글로벌 주식 시장 지원
**예상 시간**: 2시간

#### 3.1 심볼 변환 로직
```java
private String convertToYahooSymbol(String symbol) {
    if (symbol.matches("\\d{6}")) return symbol + ".KS"; // 한국
    if (symbol.matches("\\d{4}")) return symbol + ".T";  // 일본
    return symbol; // 미국
}
```

#### 3.2 테스트 종목
- 한국: 005930 (삼성전자), 000660 (SK하이닉스)
- 일본: 7203 (토요타), 6758 (소니)
- 미국: 기존 빅테크 M7

#### 3.3 API 엔드포인트 확장
- 새로운 엔드포인트: `/api/v1/market/korea-stocks`
- 기존 엔드포인트에서 한국 주식 지원

---

### Phase 4: 오류 처리 및 최적화
**목표**: 프로덕션 레디 안정성
**예상 시간**: 2-3시간

#### 4.1 포괄적 예외 처리
```java
// 연결 타임아웃
// HTTP 오류 상태 (404, 500, 503)
// JSON 파싱 오류
// 데이터 검증 실패 (price <= 0)
// Yahoo Finance 서비스 장애
```

#### 4.2 자체 Rate Limiting
```java
@Component
public class YahooFinanceRateLimiter {
    private final RateLimiter rateLimiter = RateLimiter.create(10.0);
}
```

#### 4.3 Circuit Breaker 패턴
- 연속 5회 실패 → 30초 차단
- Half-Open → 1회 성공 시 복구
- 실패율 기반 자동 차단

#### 4.4 로깅 & 모니터링
```java
// 성공/실패 메트릭
// 응답 시간 측정
// 프로바이더별 성공률 추적
```

---

## 🧪 테스트 계획

### 단위 테스트
- YahooFinanceProvider 각 메서드 테스트
- 심볼 변환 로직 테스트
- 예외 상황 테스트

### 통합 테스트
- 전체 Fallback 체인 테스트
- 실제 API 호출 테스트 (MockWebServer)
- Controller → Service → Provider 플로우

### 성능 테스트
- 동시 요청 처리 (100 requests/sec)
- 메모리 사용량 모니터링
- 응답 시간 측정 (목표: <2초)

## 📊 예상 개선 효과

### Before (AlphaVantage만)
- **API 제한**: 25회/일
- **성공률**: ~0% (제한 초과)
- **비용**: $0/월
- **지연 시간**: 실시간

### After (Yahoo Finance + AlphaVantage)
- **API 제한**: 사실상 무제한
- **성공률**: ~95% (Yahoo Finance 가용성)
- **비용**: $0/월
- **지연 시간**: 15-20분 지연

## 🚀 배포 계획

### 개발 환경 테스트
1. 로컬에서 YahooFinanceProvider 테스트
2. 빅테크 M7 엔드포인트 정상 동작 확인
3. Frontend에서 실제 데이터 확인

### 프로덕션 준비
1. 환경변수 설정 (타임아웃, 레이트 제한)
2. 로깅 레벨 조정
3. 모니터링 대시보드 구성

## 🔧 필요한 의존성

```gradle
// Jackson for JSON parsing (이미 있음)
implementation 'com.fasterxml.jackson.core:jackson-core'

// Guava for RateLimiter (선택사항)
implementation 'com.google.guava:guava:32.1.2-jre'

// Apache HttpComponents (RestTemplate 향상)
implementation 'org.apache.httpcomponents:httpclient'
```

## 📝 체크리스트

### Phase 1 완료 조건
- [ ] YahooFinanceProvider 클래스 생성
- [ ] getMarketData() 메서드 구현
- [ ] JSON 파싱 로직 구현
- [ ] 미국 주식 7개 테스트 통과

### Phase 2 완료 조건
- [ ] @Order 어노테이션 설정
- [ ] Fallback 시나리오 테스트
- [ ] RestTemplate Bean 설정
- [ ] 기존 API 정상 동작 확인

### Phase 3 완료 조건
- [ ] 심볼 변환 로직 구현
- [ ] 한국 주식 테스트 통과
- [ ] 일본 주식 테스트 통과
- [ ] 새로운 API 엔드포인트 추가

### Phase 4 완료 조건
- [ ] 예외 처리 완료
- [ ] Rate Limiting 구현
- [ ] Circuit Breaker 패턴 적용
- [ ] 로깅 시스템 구축

---

**최종 목표**: AlphaVantage API 제한으로 인한 서비스 중단 문제를 완전히 해결하고,
안정적이고 확장 가능한 다중 프로바이더 시스템 구축