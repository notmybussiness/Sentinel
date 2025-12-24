# 기술 부채 해결 & 포트폴리오 강화 실행 계획

## 개요
대량 데이터 + Index 최적화 + 페이징 + 분산락 + JWT 블랙리스트 등을 통한 기술적 깊이 강화

## 실행 순서

```
Phase 1: 더미 데이터 생성 (100만 건)
    ↓
Phase 2: Index 최적화 + EXPLAIN ANALYZE
    ↓
Phase 3: 페이징 딥다이브 (Offset vs Cursor vs Keyset)
    ↓
Phase 4: Redis 분산락 (Redisson)
    ↓
Phase 5: JWT 블랙리스트 (Redis)
    ↓
Phase 6: Optimistic Lock + 동시성 테스트
    ↓
Phase 7: 다중 WAS 구성
```

---

## Phase 1: 더미 데이터 생성

### 목적
- 100만 건 데이터로 **대용량 처리 경험** 증명
- Index, 페이징 성능 테스트 기반 마련

### 실행 방법
```bash
# PostgreSQL에서 직접 실행
docker exec -i sentinel-postgres psql -U postgres -d sentinel < scripts/data-generation/01_generate_price_history.sql
```

### 예상 결과
- 100개 심볼 x 10,000일 = 1,000,000건
- 테이블 크기: 약 200-300MB
- 인덱스 크기: 약 100-150MB

### 파일
- `01_generate_price_history.sql` - 더미 데이터 생성 SQL

---

## Phase 2: Index 최적화

### 목적
- **문제 확인**: 현재 쿼리 성능 측정 (EXPLAIN ANALYZE)
- **대안 비교**: B-Tree vs Hash vs Composite Index
- **Before/After**: 성능 개선 수치화

### 실행 방법
```bash
docker exec -i sentinel-postgres psql -U postgres -d sentinel < scripts/data-generation/02_index_optimization.sql
```

### 파일
- `02_index_optimization.sql` - 인덱스 분석 및 최적화 SQL

---

## Phase 3: 페이징 딥다이브

### 목적
- **문제 확인**: Offset 페이징의 성능 저하 (대용량에서)
- **대안 비교**: Offset vs Cursor vs Keyset
- **Before/After**: 100만번째 페이지 조회 시간 비교

### 실행 방법
```bash
docker exec -i sentinel-postgres psql -U postgres -d sentinel < scripts/data-generation/03_pagination_comparison.sql
```

### 파일
- `03_pagination_comparison.sql` - 페이징 방식 비교 SQL

---

## Phase 4: Redis 분산락

### 목적
- **문제 확인**: 다중 WAS에서 동시 실행 문제
- **대안 비교**: DB Lock vs Redis Lock vs Zookeeper
- **선택**: Redisson RLock (실무 표준)

### 실행 방법
```bash
# 1. build.gradle에 의존성 추가 (02_redisson_dependency.gradle 참조)
# 2. Java 코드 적용 (03_distributed_lock.java 참조)
# 3. 테스트 실행
./gradlew test --tests "*DistributedLock*"
```

### 파일
- `04_redisson_config.java` - Redisson 설정
- `04_distributed_lock_aop.java` - 분산락 AOP

---

## Phase 5: JWT 블랙리스트

### 목적
- **문제 확인**: 로그아웃 후에도 토큰 유효 (보안 취약점)
- **대안 비교**: Redis vs DB vs 짧은 만료시간
- **선택**: Redis 블랙리스트 (성능 + 자동 TTL)

### 실행 방법
```bash
# Java 코드 적용 후 테스트
./gradlew test --tests "*TokenBlacklist*"
```

### 파일
- `05_token_blacklist_service.java` - 블랙리스트 서비스
- `05_jwt_filter_update.java` - 필터 수정

---

## Phase 6: Optimistic Lock

### 목적
- **문제 확인**: 동시 수정 시 Lost Update
- **대안 비교**: Optimistic vs Pessimistic Lock
- **선택**: @Version + 재시도 전략

### 실행 방법
```bash
# Entity에 @Version 추가 후 테스트
./gradlew test --tests "*ConcurrencyTest*"
```

### 파일
- `06_optimistic_lock_entity.java` - Entity 수정
- `06_concurrency_test.java` - 동시성 테스트

---

## Phase 7: 다중 WAS 구성

### 목적
- **문제 확인**: 단일 서버의 한계
- **구현**: docker-compose로 WAS 2개 + Nginx 로드밸런서

### 실행 방법
```bash
docker-compose -f docker-compose.multi-was.yml up -d
```

### 파일
- `docker-compose.multi-was.yml` - 다중 WAS 구성
- `nginx.conf` - 로드밸런서 설정

---

## 면접 예상 질문

### Phase 1-2 (데이터/인덱스)
- "100만 건에서 어떻게 성능을 개선했나요?"
- "복합 인덱스 컬럼 순서는 왜 중요한가요?"
- "EXPLAIN ANALYZE 결과를 어떻게 해석하나요?"

### Phase 3 (페이징)
- "Offset 페이징의 문제점은?"
- "Cursor 페이징은 어떻게 구현하나요?"
- "무한 스크롤 vs 페이지네이션 차이?"

### Phase 4 (분산락)
- "분산락이 왜 필요한가요?"
- "Redlock 알고리즘을 설명해주세요"
- "TTL이 왜 필요한가요? 없으면 어떻게 되나요?"

### Phase 5 (JWT)
- "JWT 로그아웃을 어떻게 구현하나요?"
- "블랙리스트 방식의 단점은?"
- "토큰 탈취 시 대응 방안은?"

### Phase 6 (동시성)
- "Optimistic vs Pessimistic Lock 차이?"
- "Lost Update가 뭔가요?"
- "@Version 동작 원리는?"

### Phase 7 (스케일아웃)
- "세션은 어떻게 공유하나요?"
- "로드밸런싱 알고리즘은?"
- "헬스체크는 어떻게 하나요?"
