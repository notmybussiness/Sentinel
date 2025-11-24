# Learning Log - 학습 기록 자동화

## 🎯 목적
**꾸준한 학습과 성장 증명**
- 일일 학습 내용 자동 기록
- 문제 해결 과정 문서화
- 포트폴리오: 학습 능력 증명

---

## 📝 일일 학습 로그

### 자동 생성 템플릿
```markdown
# Learning Log - YYYY-MM-DD

## 🆕 새로 배운 것
- 

## 🐛 해결한 문제
### Problem
[문제 상황]

### Solution
[해결 방법]

### Learned
[배운 점]

## 📚 참고한 자료
- 

## 💡 적용 결과
- 

## 🔜 내일 할 일
- 

---
Total Commits Today: X
Lines Changed: +XXX -YYY
```

---

## 🚀 실행 예시

### Claude Code에서
```
You: /learning-log

Claude: Let me create today's learning log.

Analyzing today's work...
- Commits: 5
- Files changed: 8
- Main focus: N+1 query optimization

Generated: .claude/docs/learning/2025-01-15.md

Summary:
✅ New: @EntityGraph annotation
✅ Solved: Portfolio API N+1 (94% improvement)
✅ Reference: Spring Data JPA docs
✅ Result: P95 latency 2500ms → 150ms

Would you like to edit before saving?
```

---

## 📊 학습 로그 실제 예시

### 2025-01-15.md
```markdown
# Learning Log - 2025-01-15

## 🆕 새로 배운 것

### @EntityGraph로 N+1 해결
JPA에서 N+1 문제를 해결하는 방법 학습:
- `@EntityGraph(attributePaths = {"assets"})`
- LAZY Loading을 유지하면서 필요할 때만 JOIN FETCH
- `@BatchSize`보다 명시적이고 효과적

### k6 부하 테스트
- VUser 설정으로 동시 사용자 시뮬레이션
- `--out json=results.json`으로 결과 저장
- jq로 P95 latency 추출: `jq '.metrics.http_req_duration.values.p95'`

## 🐛 해결한 문제

### Problem: Portfolio API 느림
- 증상: 응답 시간 2500ms
- 원인: Asset 조회 시 N+1 쿼리 (101개 쿼리 발생)
- 영향: VUser 100명만 처리 가능

### Solution: @EntityGraph 적용
```java
@EntityGraph(attributePaths = {"assets", "assets.cryptocurrency"})
Portfolio findByUserId(Long userId);
```

### Learned
1. **Hibernate 로깅 활성화가 중요**
   - `spring.jpa.show-sql=true`로 쿼리 확인
   - N+1 육안 식별 가능
   
2. **측정의 중요성**
   - Before/After 수치로 개선 증명
   - 추측보다 측정
   
3. **Atomic Commit**
   - 하나의 문제 = 하나의 커밋
   - 롤백 용이성 확보

## 📚 참고한 자료

### 공식 문서
- [Spring Data JPA - EntityGraph](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.entity-graph)
- [k6 Documentation](https://k6.io/docs/)

### 블로그/아티클
- Vlad Mihalcea: N+1 Query Problem
- Baeldung: Spring Data JPA @EntityGraph

### Stack Overflow
- [How to solve N+1 problem](https://stackoverflow.com/questions/XXXXX)

## 💡 적용 결과

### 성능 개선
- **Before**: 2500ms (101 queries)
- **After**: 150ms (1 query)
- **개선율**: 94% ✅

### 부하 테스트
- **VUser**: 100 → 500 처리 가능
- **P95 Latency**: 2500ms → 150ms
- **Error Rate**: 0.5% → 0.08%

### Git 커밋
```
a1b2c3d perf: resolve Portfolio N+1 with @EntityGraph (94% improvement)
```

## 🔜 내일 할 일
- [ ] Price API 비동기 처리 적용
- [ ] Caffeine 캐시 도입 검토
- [ ] Connection Pool 설정 최적화

---
Total Commits Today: 5
Lines Changed: +150 -30
Test Coverage: 85% → 87%
```

---

## 🎓 포트폴리오 활용

### 학습 능력 증명
```
면접관: "새로운 기술을 어떻게 학습하나요?"

당신: "매일 학습 로그를 작성합니다.
      예를 들어 N+1 문제를 처음 접했을 때:
      
      1. 문제 상황 기록
      2. 해결 방법 탐색 (공식 문서, 블로그)
      3. 실제 적용
      4. 측정값으로 효과 검증
      5. 배운 점 정리
      
      이런 과정을 문서화해서
      3개월 후에도 다시 찾아볼 수 있습니다."

면접관: "실제 보여줄 수 있나요?"

당신: "네, GitHub의 .claude/docs/learning/ 폴더에
      100개 이상의 일일 학습 로그가 있습니다."
```

### 성장 과정 시각화
```bash
# 월별 학습 주제 통계
ls .claude/docs/learning/2025-01-*.md | xargs grep "## 🆕 새로 배운 것" -A 3

# 출력
2025-01-15: @EntityGraph, k6 load testing
2025-01-16: Caffeine cache, Redis comparison
2025-01-17: CompletableFuture, Async processing
...

# 해결한 문제 개수
grep -r "## 🐛 해결한 문제" .claude/docs/learning/*.md | wc -l

# 출력: 45개 문제 해결
```

---

## 📈 학습 트렌드 분석

### 자주 배운 주제
```python
# scripts/analyze_learning.py
import glob
import re
from collections import Counter

def analyze_learning_topics():
    topics = []
    
    for file in glob.glob('.claude/docs/learning/*.md'):
        with open(file) as f:
            content = f.read()
            # 주제 추출 (###로 시작하는 제목)
            found = re.findall(r'### (.+)', content)
            topics.extend(found)
    
    counter = Counter(topics)
    
    print("📊 Most Learned Topics:")
    for topic, count in counter.most_common(10):
        print(f"  {count}회: {topic}")

# 실행
analyze_learning_topics()

# 출력
📊 Most Learned Topics:
  5회: N+1 Query Optimization
  4회: Caching Strategy
  3회: Async Processing
  3회: Load Testing
  2회: Database Indexing
```

---

## 🛠️ 자동화 스크립트

### 자동 로그 생성
```bash
#!/bin/bash
# scripts/create-learning-log.sh

DATE=$(date +%Y-%m-%d)
LOG_DIR=".claude/docs/learning"
LOG_FILE="$LOG_DIR/$DATE.md"

# 이미 존재하면 열기
if [ -f "$LOG_FILE" ]; then
    code "$LOG_FILE"
    exit 0
fi

# 오늘의 Git 활동 분석
COMMITS=$(git log --since="$DATE 00:00:00" --until="$DATE 23:59:59" --oneline | wc -l)
CHANGES=$(git diff --shortstat "@{1 day ago}")

# 템플릿 생성
cat > "$LOG_FILE" << EOF
# Learning Log - $DATE

## 🆕 새로 배운 것

### 

## 🐛 해결한 문제

### Problem


### Solution


### Learned


## 📚 참고한 자료

- 

## 💡 적용 결과

- 

## 🔜 내일 할 일

- [ ] 

---
Total Commits Today: $COMMITS
Lines Changed: $CHANGES
EOF

echo "✅ Created: $LOG_FILE"
code "$LOG_FILE"
```

**사용법**:
```bash
# 매일 아침 실행
./scripts/create-learning-log.sh

# 또는 Git hook으로 자동화
# .git/hooks/post-commit
```

---

## 📊 월간/주간 리뷰

### 주간 리뷰 생성
```markdown
# Weekly Review - 2025 Week 3 (Jan 15-21)

## 🎯 이번 주 목표
- [x] Portfolio API 최적화
- [x] k6 부하 테스트 환경 구축
- [ ] Redis 캐시 도입 (다음 주로 연기)

## 📈 성과
- N+1 해결: 94% 성능 개선
- 부하 테스트: 500 VUser 처리 가능
- 테스트 커버리지: 85% → 87%

## 📚 학습한 기술
1. @EntityGraph (JPA)
2. k6 load testing
3. Caffeine cache

## 🐛 해결한 문제
1. Portfolio API N+1 (94% 개선)
2. Connection Pool 고갈 (20으로 증설)

## 💡 인사이트
- 추측하지 말고 측정하라
- Atomic Commit의 중요성
- Before/After 수치의 힘

## 🔜 다음 주 계획
- [ ] Price API 비동기 처리
- [ ] Caffeine 캐시 도입
- [ ] Prometheus 대시보드 구축
```

---

## 🎯 습관화 팁

### 매일 5분 투자
```
오전 (작업 시작):
- 어제 로그 리뷰 (2분)
- 오늘 할 일 정리 (3분)

오후 (작업 완료):
- 배운 것 기록 (5분)
- 해결한 문제 문서화 (10분)
- 내일 계획 (2분)

총 22분 투자 → 3개월 후 막대한 자산
```

### 작은 것도 기록
```
"오늘 배운 게 없는데..."

→ 작은 것도 기록!
  - IntelliJ 단축키
  - Git alias
  - SQL 쿼리 최적화
  - 에러 메시지 해석법
```

---

## 📝 체크리스트

### 좋은 학습 로그
- [ ] 구체적 (추상적 X)
- [ ] 측정 가능 (수치 포함)
- [ ] 재현 가능 (코드 포함)
- [ ] 참고 자료 링크
- [ ] 적용 결과 기록

### 나쁜 학습 로그
```markdown
# 2025-01-15
오늘 N+1 배웠다. 좋았다.
```
→ ❌ 구체성 없음, 재현 불가

### 좋은 학습 로그
```markdown
# 2025-01-15
N+1 쿼리를 @EntityGraph로 해결:
- Before: 2500ms (101 queries)
- After: 150ms (1 query)
- Method: attributePaths = {"assets"}
- Reference: https://docs.spring.io/...
```
→ ✅ 구체적, 측정 가능, 재현 가능
