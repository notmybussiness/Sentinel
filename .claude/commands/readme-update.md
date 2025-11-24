# README Update - README 자동 업데이트

## 🎯 목적
**프로젝트 최신 상태 반영**
- 최근 커밋 기반 업데이트
- 성능 지표 자동 갱신
- 기술 스택 동기화

---

## 📝 업데이트 항목

### 1. 성능 개선 사례 테이블
```markdown
## 📊 성능 개선 사례

| 기능 | Before | After | 개선율 | 방법 |
|------|--------|-------|--------|------|
| Portfolio API | 2500ms | 150ms | **94%** | @EntityGraph |
| Price API | 5000ms | 500ms | **90%** | Caffeine Cache |
| Async Processing | 5초 | 2초 | **60%** | CompletableFuture |
```

### 2. 부하 테스트 결과
```markdown
## 🎯 부하 테스트 결과
- **VUser**: 500 동시 사용자 처리
- **P95 Latency**: 450ms (목표 500ms 달성 ✅)
- **Throughput**: 200 req/s
- **Error Rate**: 0.08% (목표 0.1% 달성 ✅)
```

### 3. 기술 스택
```markdown
## 🛠️ 기술 스택
- **Backend**: Spring Boot 3.2.1, Java 17
- **Database**: PostgreSQL 15
- **Cache**: Caffeine (Local)
- **Test**: JUnit 5, k6
- **Monitoring**: Prometheus, Grafana
```

---

## 🚀 자동 생성 스크립트

```bash
#!/bin/bash
# scripts/update-readme.sh

# 최근 perf: 커밋 가져오기
PERF_COMMITS=$(git log --grep="perf:" --oneline -5)

# 성능 테이블 생성
echo "| 기능 | Before | After | 개선율 | 방법 |"
echo "|------|--------|-------|--------|------|"

while IFS= read -r commit; do
    # 커밋 메시지에서 정보 추출
    # 예: "perf: Portfolio API (94% improvement)"
    echo "| ... | ... | ... | **XX%** | ... |"
done <<< "$PERF_COMMITS"

# build.gradle에서 버전 추출
SPRING_VERSION=$(grep "springBoot" build.gradle)
JAVA_VERSION=$(grep "sourceCompatibility" build.gradle)

# 최근 k6 결과 로드
LATEST_K6=$(ls -t results/*.json | head -1)
P95=$(jq '.metrics.http_req_duration.values.p95' $LATEST_K6)
```

---

## 📊 업데이트 예시

### Before
```markdown
# Sentinel

포트폴리오 관리 플랫폼
```

### After
```markdown
# Sentinel - AI-Driven Portfolio Management

## 📊 성능 개선 사례
| 기능 | Before | After | 개선율 | 방법 |
|------|--------|-------|--------|------|
| Portfolio API | 2500ms | 150ms | **94%** | @EntityGraph |

## 🎯 부하 테스트 결과
- VUser: 500
- P95: 450ms ✅
```

---

## 🎓 포트폴리오 강점

```
면접관: "프로젝트 성과가 궁금한데요?"

당신: "README에 정량적 지표를 정리했습니다.
      예를 들어 Portfolio API를
      2500ms에서 150ms로 94% 개선했고,
      부하 테스트로 500 VUser 처리를 검증했습니다."

면접관: "어떻게 유지하나요?"

당신: "Git 커밋과 k6 결과를 기반으로
      README를 자동 업데이트합니다.
      항상 최신 상태를 유지합니다."
```
