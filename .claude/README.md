# Sentinel - 개발 시작 가이드

> **빠른 시작**: 환경 설정 → 실행 → 테스트
> 전체 프로젝트 구조는 [CLAUDE.md](./CLAUDE.md) 참조

**Last Updated**: 2025-12-06

---

## 🎯 시작하기 전에

### 필수 문서
1. **[CLAUDE.md](./CLAUDE.md)** - 프로젝트 전체 지도 (아키텍처, 코드 스타일, 워크플로우)
2. **[roadmap/CURRENT_STATUS.md](./roadmap/CURRENT_STATUS.md)** - 현재 Phase 7 완료, Phase 8 계획
3. **[specs/README.md](./specs/README.md)** - API 명세서 (35개 엔드포인트)

---

## 🚀 Quick Start

### 1. 환경 요구사항

| 항목 | 버전 |
|------|------|
| **Java** | 21+ |
| **Node.js** | 18+ |
| **Docker** | Latest |
| **PostgreSQL** | 14+ (Docker) |
| **Redis** | 7+ (Docker) |

### 2. PostgreSQL & Redis 시작

```bash
# 프로젝트 루트에서
docker-compose up -d

# 확인
docker ps
# CONTAINER STATUS가 Up 이면 정상
```

**서비스 포트**:
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

### 3. Backend 실행

```bash
# backend 디렉토리로 이동
cd backend

# 실행 (CMD 또는 PowerShell - Git Bash 불가!)
gradlew bootRun

# 또는 개발 프로필로 실행
gradlew bootRun --args='--spring.profiles.active=dev,secret'

# 또는 성능 테스트 프로필
gradlew bootRun --args='--spring.profiles.active=perf,secret'
```

**확인**:
- http://localhost:8080 접속
- http://localhost:8080/actuator/health 상태 확인

### 4. Frontend 실행 (Optional)

```bash
# frontend 디렉토리로 이동
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

**확인**:
- http://localhost:3000 접속

---

## 🧪 테스트

### Backend Unit/Integration Tests

```bash
cd backend

# 전체 테스트 실행
gradlew test

# 특정 클래스 테스트
gradlew test --tests PortfolioServiceTest

# 특정 메서드 테스트
gradlew test --tests PortfolioServiceTest.createPortfolio
```

### k6 Load Tests

```bash
cd backend/scripts/phase7_redis_cache/tests

# Phase 7 baseline 테스트 (Redis 캐시)
k6 run exp11_redis_baseline.js

# 결과 확인
cat ../results/exp11_summary.json | jq '.metrics.http_reqs.values.rate'
```

---

## 🔧 환경 설정 파일

### application-secret.yml (필수)

**위치**: `backend/src/main/resources/application-secret.yml`

```yaml
# JWT 설정
jwt:
  secret: your-jwt-secret-key-here

# Kakao OAuth2
kakao:
  oauth:
    client-id: your-kakao-client-id
    client-secret: your-kakao-client-secret

# Gemini AI
ai:
  gemini:
    api-key: your-gemini-api-key

# Stock Market APIs
stock:
  market:
    alphavantage:
      api-key: your-alphavantage-key
    finnhub:
      api-key: your-finnhub-key
    korea-investment:
      app-key: your-kis-app-key
      app-secret: your-kis-app-secret
```

**⚠️ 주의**: `application-secret.yml`은 `.gitignore`에 포함되어 있습니다.

---

## 📊 모니터링

### Actuator Endpoints

```bash
# Health Check
curl http://localhost:8080/actuator/health

# HikariCP Connection Pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending

# Cache Metrics
curl http://localhost:8080/actuator/metrics/cache.gets?tag=cache:portfolios

# All Metrics
curl http://localhost:8080/actuator/metrics
```

### Prometheus & Grafana

**Prometheus**: http://192.168.0.5:9090
**Grafana**: http://192.168.0.5:3001

**대시보드**:
- TPS (Requests per second)
- P95 Response Time
- Error Rate
- HikariCP Connection Pool
- Redis Cache Hit Rate

---

## 🐛 디버깅

### Backend 로그 레벨 조정

`application.yml` 또는 `application-dev.yml`:

```yaml
logging:
  level:
    root: INFO
    com.pjsent.sentinel: DEBUG
    org.hibernate.SQL: DEBUG                        # SQL 쿼리 출력
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE  # 파라미터 바인딩
    org.springframework.cache: DEBUG                # 캐시 동작
    org.springframework.data.redis: DEBUG           # Redis 동작
```

### 개발 모드 로그인

프론트엔드가 없을 때 빠른 테스트용:

```bash
# 개발 사용자로 자동 로그인 (테스트 전용)
POST http://localhost:8080/api/v1/auth/dev-login
```

**응답**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "eyJhbGciOiJIUzI1...",
  "expiresIn": 900,
  "user": {
    "id": 1,
    "email": "dev@sentinel.com",
    "name": "개발자"
  }
}
```

---

## 📁 주요 디렉토리 구조

### Backend

```
backend/
├── src/main/java/com/pjsent/sentinel/
│   ├── user/          # 인증 (OAuth2, JWT)
│   ├── portfolio/     # 포트폴리오 CRUD
│   ├── market/        # 주식 시장 데이터
│   ├── crypto/        # 암호화폐 (Upbit, Binance)
│   ├── backtest/      # 백테스팅 엔진
│   ├── rebalancing/   # 리밸런싱 알고리즘
│   └── config/        # 설정 (Cache, Security, Async)
│
├── src/main/resources/
│   ├── application.yml           # 기본 설정
│   ├── application-dev.yml       # 개발 프로필
│   ├── application-perf.yml      # 성능 테스트 프로필
│   └── application-secret.yml    # ⚠️ 비밀 정보 (.gitignore)
│
└── scripts/
    ├── phase7_redis_cache/tests/  # k6 테스트 스크립트
    └── results/                   # 성능 테스트 결과
```

### 문서 (.claude/)

```
.claude/
├── CLAUDE.md               # 👈 프로젝트 메타 가이드 (시작점)
├── README.md               # 👈 이 파일 (개발 시작 가이드)
│
├── specs/                  # API 명세서
│   ├── README.md
│   ├── API_AUTH.md
│   ├── API_PORTFOLIO.md
│   └── ...
│
├── roadmap/                # 프로젝트 계획
│   ├── ROADMAP.md
│   ├── CURRENT_STATUS.md
│   └── archive/
│
├── docs/                   # 기술 문서
│   ├── architecture/       # ADR, 설계 문서
│   ├── performance/        # 성능 최적화 기록
│   └── learning/           # 학습 기록
│
└── commands/               # 슬래시 커맨드
```

---

## 🔑 주요 명령어 치트시트

### Git

```bash
# 새 feature 브랜치
git checkout -b feat/circuit-breaker

# 커밋 (올바른 형식)
git commit -m "feat: add circuit breaker to UpbitProvider"
git commit -m "perf(phase8): optimize HikariCP connection pool"

# ❌ 절대 금지
git commit -m "feat: add feature

Generated with Claude Code
Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Gradle

```bash
# 빌드
gradlew build

# 테스트
gradlew test

# 클린 빌드
gradlew clean build

# 특정 프로필로 실행
gradlew bootRun --args='--spring.profiles.active=perf,secret'
```

### Docker

```bash
# 서비스 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 서비스 중지
docker-compose stop

# 서비스 중지 + 데이터 삭제
docker-compose down -v
```

### Redis CLI

```bash
# 연결 확인
redis-cli ping

# 통계 확인
redis-cli INFO stats | grep keyspace

# 캐시 키 조회
redis-cli KEYS "sentinel:*"

# 특정 캐시 값 확인
redis-cli GET "sentinel:portfolios::1"

# 캐시 전체 삭제 (주의!)
redis-cli FLUSHALL
```

---

## 🚨 주의사항

### 1. Backend 실행 환경
- ❌ **Git Bash에서 실행 불가**
- ✅ **CMD 또는 PowerShell 사용**

### 2. API Rate Limits
- **AlphaVantage**: 분당 5회, 일일 100회
- **Finnhub**: 분당 60회
- **Upbit**: 공식 제한 없음 (과도한 요청 시 차단 가능)

### 3. Spec-First Development
- 코드 작성 전 **반드시** `.claude/specs/API_*.md` 확인
- JSON 구조와 **정확히 일치**하는 코드 작성

### 4. 성능 테스트
- 항상 **k6로 Before/After 측정**
- Full path 테스트 (Client → WAS → DB)

---

## 🔗 추가 리소스

### 공식 문서
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Redis Documentation](https://redis.io/docs/)
- [k6 Documentation](https://k6.io/docs/)

### 프로젝트 문서
- **전체 아키텍처**: [CLAUDE.md](./CLAUDE.md)
- **API 스펙**: [specs/README.md](./specs/README.md)
- **현재 진행 상황**: [roadmap/CURRENT_STATUS.md](./roadmap/CURRENT_STATUS.md)
- **Phase 8 계획**: [plans/glimmering-sprouting-cherny.md](./plans/glimmering-sprouting-cherny.md)

---

## 📞 문제 해결

### 자주 발생하는 이슈

**1. `gradlew: command not found`**
```bash
# Windows: Git Bash 사용 중 (CMD/PowerShell 사용)
# 또는 gradlew.bat 사용
gradlew.bat bootRun
```

**2. `Cannot connect to PostgreSQL`**
```bash
# Docker 컨테이너 상태 확인
docker ps

# 재시작
docker-compose restart
```

**3. `Redis connection refused`**
```bash
# Redis 상태 확인
redis-cli ping

# Docker 로그 확인
docker-compose logs redis
```

**4. `JWT token expired` (k6 테스트)**
- JWT TTL: 15분 (dev), 24시간 (perf)
- k6 스크립트에 JWT refresh 로직 추가 필요 (Phase 8 계획)

---

**Last Updated**: 2025-12-06
**문의**: 프로젝트 관리자
