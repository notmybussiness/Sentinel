# Sentinel - 다음 단계 계획

> **작성일**: 2025-10-23
> **현재 상태**: Phase 7 완료 + 데이터 영속성 구현 완료

---

## ✅ 완료된 작업 (2025-10-23)

### Backend
- PostgreSQL Docker 환경 구축
- `PriceHistory` 엔티티 및 리포지토리
- 자동 데이터 수집 스케줄러 (인덱스/ETF/암호화폐)
- REST API 엔드포인트 4개
- 영속성 100% 보장 (테스트 완료)

### Frontend
- 메인페이지 ETF/인덱스 섹션 추가
- Safe calculation 적용 (Division by zero 방지)
- 포트폴리오 차트 (기존 완료)

---

## 🎯 다음 단계 (우선순위 순)

### Phase 8: Testing & Deployment

#### 1. 백엔드 테스트
- [ ] Unit Test (JUnit 5) - 목표: >80% 커버리지
  - `PriceHistoryService` 테스트
  - `IndexDataCollectorService` 테스트
  - `CryptoDataCollectorService` 테스트
- [ ] Integration Test (MockMvc)
  - `/api/v1/price-history/*` 엔드포인트 테스트

#### 2. 프론트엔드 개선
- [ ] 포트폴리오 차트 Real API 연동
  - 현재: Mock 데이터 (30일 랜덤)
  - 목표: `GET /api/v1/price-history/chart` 사용
  - 전제조건: 1-2일 후 충분한 데이터 축적
- [ ] 추천 포트폴리오 Backend API 구현

#### 3. 성능 최적화 (Optional)
- [ ] Redis 캐싱 도입
- [ ] API Rate Limiting
- [ ] Circuit Breaker 패턴 구현

#### 4. 배포 준비
- [ ] AWS 인프라 설정 (EC2, RDS)
- [ ] CI/CD 파이프라인 (GitHub Actions)
- [ ] 환경 변수 관리 (AWS Secrets Manager)

---

## 🚀 Phase 9-11: 고급 기능 (장기 계획)

### 우선순위 1: 거래 기능 (Trading)
- 페이퍼 트레이딩 (시뮬레이션)
- 실거래 통합 (한국투자증권, Upbit)
- 리스크 관리 (Stop Loss, Position Sizing)

### 우선순위 2: 고급 백테스팅
- Monte Carlo 시뮬레이션
- 멀티 전략 비교
- 퀀트 팩터 분석

### 우선순위 3: 세금 최적화
- Tax-Loss Harvesting
- 세금 시뮬레이션
- 배당 최적화

---

## 📊 현재 기술 부채

### Backend
- [ ] Circuit Breaker 미구현
- [ ] Redis 미사용
- [ ] Rate Limiting 미구현

### Frontend
- [ ] 일부 Mock 데이터 사용 중
  - 추천 포트폴리오
  - 포트폴리오 차트 (데이터 쌓인 후 교체 예정)
- [ ] 실시간 스트리밍 UI 미구현 (SSE, WebSocket)

### DevOps
- [ ] 프로덕션 환경 없음
- [ ] 모니터링 시스템 없음
- [ ] CI/CD 파이프라인 없음

---

## 💡 즉시 시작 가능한 작업

### 1. 유닛 테스트 작성 (1-2일)
```java
// PriceHistoryServiceTest.java
@Test
void savePriceData_shouldSaveSuccessfully() {
    // given
    PriceHistory priceHistory = PriceHistory.builder()
        .symbol("BTC")
        .assetType(AssetType.CRYPTO)
        .close(BigDecimal.valueOf(50000))
        .build();

    // when
    PriceHistory saved = priceHistoryService.savePriceData(priceHistory);

    // then
    assertNotNull(saved.getId());
}
```

### 2. 포트폴리오 차트 Real API 연동 (1일 후)
```typescript
// 1-2일 후 데이터가 충분히 쌓이면
const { data: chartData } = useQuery({
  queryKey: ['portfolio-chart', portfolioId],
  queryFn: () => getChartData(
    portfolioSymbol,
    startTime,
    endTime
  ),
});
```

### 3. AWS 배포 준비 (1주일)
- EC2 인스턴스 설정
- RDS PostgreSQL 마이그레이션
- Docker Compose → ECS/ECR

---

## 📅 권장 일정

### Week 1-2: Testing
- Day 1-3: Unit Test 작성
- Day 4-5: Integration Test
- Day 6-7: E2E Test

### Week 3-4: 배포 준비
- Day 1-3: AWS 인프라 설정
- Day 4-5: CI/CD 파이프라인
- Day 6-7: 프로덕션 배포

### Month 2-3: 고급 기능
- Week 1-2: 페이퍼 트레이딩
- Week 3-4: 고급 백테스팅

---

## 🎯 목표

**단기 (1-2주)**:
- ✅ Testing 완료 (>80% 커버리지)
- ✅ AWS 배포 완료

**중기 (1-2개월)**:
- ✅ 거래 기능 (페이퍼 트레이딩)
- ✅ 고급 백테스팅

**장기 (3개월+)**:
- ✅ 실거래 통합
- ✅ 세금 최적화
- ✅ 프로덕션 운영

---

**다음 작업 시작 시 이 파일 참고**
