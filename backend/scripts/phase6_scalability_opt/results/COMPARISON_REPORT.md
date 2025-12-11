# Phase 6 실험 결과 비교 분석

## 📊 실험 개요

### Experiment 1: 17:49~17:53 (Before - DTO 수정 전)
- k6 스크립트: DTO 불일치 (averagePrice, purchaseDate 등)
- Cache 설정: expireAfterWrite만 사용 (1분 TTL)
- Prometheus: histogram 설정 미흡 (P95 데이터 없음)

### Experiment 2: 18:57~19:02 (After - DTO 수정 후)
- k6 스크립트: DTO 일치 (averageCost, assetType, baseCurrency)
- Cache 설정: 동일 (아직 expireAfterAccess 미적용)
- Prometheus: histogram 설정 수정 (P95 데이터 수집 성공)

---

## 🎯 핵심 지표 비교

### 1. Status Code 분포

| Metric | Exp1 (17:49~17:53) | Exp2 (18:57~19:02) | 변화 |
|--------|-------------------|-------------------|------|
| **200 OK (TPS)** | ~140 TPS | ~145 TPS | +3.6% ✅ |
| **201 Created (TPS)** | 없음 | ~3-16 TPS | 신규 발생 ✅ |
| **400 Error (TPS)** | ~25 TPS (15%) | ~20-25 TPS (14%) | 유사 ⚠️ |
| **500 Error (TPS)** | ~13 TPS (9%) | ~12-14 TPS (8.5%) | 약간 개선 |

**분석**:
- ✅ **201 Created 성공**: k6 스크립트 DTO 수정으로 Holding 추가 성공
- ⚠️ **400 에러 지속**: k6 스크립트에 일부 잘못된 요청 여전히 존재
- ⚠️ **500 에러 지속**: Upbit API 호출 실패 문제 미해결

---

### 2. System Resources

| Metric | Exp1 | Exp2 | 변화 |
|--------|------|------|------|
| **Active Threads** | 250개 | 250개 | 동일 ❌ |
| **CPU Usage** | 8% | 6-7% | 유사 |
| **JVM Memory** | 정상 | 정상 | 동일 |

**분석**:
- ❌ **Active Threads 250개**: Tomcat max-threads(200) 초과
- ❌ **낮은 CPU (6-8%)**: 대부분의 스레드가 I/O 대기 (블로킹)
- 💥 **sync=true 문제 지속**: 스레드 블로킹으로 인한 리소스 비효율

---

### 3. Response Time (NEW!)

| Endpoint | P95 Response Time | 비고 |
|----------|------------------|------|
| **/api/v1/crypto/price/{symbol}** | 3.2~6.7ms | 매우 빠름 ✅ |
| **/actuator/prometheus** | 32~55ms | Grafana 스크래핑 |

**분석**:
- ✅ **P95 데이터 수집 성공**: Prometheus histogram 설정 수정 효과
- ✅ **매우 빠른 응답 속도**: 3~6ms (캐시 히트 시)
- ⚠️ 하지만 500 에러 발생 시 응답 시간 불명 (캐시 미스 + API 실패)

---

## 🔍 주요 발견 사항

### ✅ 개선된 점
1. **DTO 수정 효과**: 201 Created 성공, Holding 추가 작동
2. **P95 메트릭 수집**: Prometheus 설정 수정으로 세밀한 모니터링 가능
3. **응답 시간 우수**: 캐시 히트 시 P95 3~6ms

### ⚠️ 여전한 문제
1. **400 에러 14%**: k6 스크립트에 여전히 잘못된 요청 존재
   - 원인: 일부 엔드포인트의 DTO 불일치 가능성
   - 해결: 전체 k6 스크립트 DTO 검증 필요

2. **500 에러 8.5%**: Upbit API 호출 실패
   - 원인: Connection Pool 고갈 또는 Rate Limit
   - 해결: Binance Provider 추가, Circuit Breaker 필요

3. **Active Threads 250개**: 시스템 리소스 비효율
   - 원인: `sync=true`로 인한 스레드 블로킹
   - 해결: `sync=false` 또는 `refreshAfterWrite` 적용 필요

---

## 🎯 다음 단계 권장 사항

### 우선순위 1: k6 스크립트 완전 수정
- [ ] 모든 엔드포인트의 DTO 검증
- [ ] 400 에러 발생 엔드포인트 식별 및 수정
- [ ] 예상 효과: 400 에러 14% → 0%

### 우선순위 2: Cache 전략 개선
- [ ] `expireAfterAccess` 적용 (Adaptive TTL)
- [ ] `sync=false`로 변경 (스레드 블로킹 제거)
- [ ] 예상 효과: Active Threads 250 → 100, API 호출 60% 감소

### 우선순위 3: Upbit API 안정성 개선
- [ ] Binance Provider 추가 (Fallback)
- [ ] Circuit Breaker 적용 (Resilience4j)
- [ ] 예상 효과: 500 에러 8.5% → 1% 이하

---

## 📈 예상 최종 성과

현재 상태에서 위 3가지 개선 적용 시:

| Metric | 현재 | 개선 후 (예상) | 개선율 |
|--------|------|---------------|--------|
| **200 OK** | 145 TPS | 170 TPS | +17% |
| **400 Error** | 20-25 TPS (14%) | 0 TPS (0%) | -100% |
| **500 Error** | 12-14 TPS (8.5%) | 1-2 TPS (1%) | -88% |
| **Active Threads** | 250개 | 100개 | -60% |
| **API 호출 빈도** | 1분마다 | 3-10분마다 | -67% |
| **전체 에러율** | 22.5% | 1% | -96% |

---

**작성 시각**: 2025-11-30 19:07
**작성자**: Claude Code Performance Analysis
