# Sentinel Backend

> **⚠️ 독립 Git 저장소**
>
> 이 폴더는 부모 저장소(Sentinel)와 별개의 Git 저장소입니다.

---

## 📋 프로젝트 정보

- **프레임워크**: Spring Boot 3.5.5
- **DB**: PostgreSQL 15
- **빌드 도구**: Gradle 8.14.3
- **Java**: 21
- **Claude Code**: `.claude/` 폴더에 설정 포함

---

## 🔧 로컬 개발 환경

### 1. PostgreSQL 시작 (부모 폴더에서)

```bash
cd ..
docker-compose up -d
```

### 2. Backend 실행

```bash
# 일반 모드
./gradlew bootRun

# 성능 테스트 모드
./gradlew bootRun --args="--spring.profiles.active=perf"
```

### 3. API 확인

```bash
# Health Check
curl http://localhost:8080/actuator/health

# Swagger UI
http://localhost:8080/swagger-ui.html
```

---

## 🎯 주요 기능

- ✅ Kakao OAuth 로그인
- ✅ 포트폴리오 CRUD
- ✅ 주식/암호화폐 가격 조회
- ✅ AI 포트폴리오 분석 (Gemini)
- ✅ 백테스팅 엔진
- ✅ 리밸런싱 추천
- ✅ 실시간 스트리밍 (SSE, WebSocket)

---

## 📂 프로젝트 구조

```
backend/
├── .claude/                        # Claude Code 설정
├── scripts/                        # 스크립트
│   ├── PERF_TEST_GUIDE.md         # 성능 테스트 가이드
│   ├── perf-test-users.sql        # 테스트 유저 생성
│   └── generate_jmeter_tokens.py  # JMeter 토큰 CSV
├── src/main/java/com/pjsent/sentinel/
│   ├── user/                      # 인증/인가
│   ├── portfolio/                 # 포트폴리오
│   ├── market/                    # 주식 데이터
│   ├── crypto/                    # 암호화폐 데이터
│   ├── ai/                        # AI 분석
│   ├── backtest/                  # 백테스팅
│   └── rebalancing/               # 리밸런싱
└── src/main/resources/
    ├── application.yml            # 기본 설정
    └── application-perf.yml       # 성능 테스트 설정
```

---

## 🧪 테스트

### 단위 테스트

```bash
./gradlew test
```

### 성능 테스트 (JMeter)

```bash
# 상세 가이드 참고
cat scripts/PERF_TEST_GUIDE.md
```

---

## 🌿 Git 브랜치 전략

```bash
# 현재 브랜치 확인
git branch

# 주요 브랜치
master       # 메인 브랜치
perf-test    # 성능 테스트 전용 (운영 금지)
```

---

## 📝 .claude 설정

이 폴더에는 Backend 전용 Claude Code 설정이 포함되어 있습니다:

```json
// .claude/settings.local.json
{
  "projectName": "Sentinel Backend",
  "language": "Java",
  "framework": "Spring Boot"
}
```

---

## 🔗 관련 문서

- **부모 프로젝트**: `../README.md`
- **전체 가이드**: `../.claude/CLAUDE.md`
- **성능 테스트**: `scripts/PERF_TEST_GUIDE.md`

---

**Git 저장소**: 독립 저장소 (부모와 별개)
**Claude Code**: `.claude/` 폴더 활용
