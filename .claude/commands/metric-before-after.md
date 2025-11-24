# Metric Before/After - 성능 개선 정량화

## 🎯 목적
**포트폴리오 핵심: 정량적 성능 개선 증명**
- Before/After 측정값 자동 기록
- 개선율 계산
- README 자동 업데이트
- 면접 준비용 수치 확보

---

## 📊 측정 워크플로우

### Step 1: Before 측정 (기준선)
```bash
# 현재 커밋 기록
BEFORE_COMMIT=$(git rev-parse --short HEAD)
BEFORE_BRANCH=$(git branch --show-current)

# k6 부하 테스트 실행
k6 run --out json=results/before-${BEFORE_COMMIT}.json scripts/load-test.js

# 주요 지표 추출
BEFORE_P95=$(jq '.metrics.http_req_duration.values.p95' results/before-${BEFORE_COMMIT}.json)
BEFORE_RPS=$(jq '.metrics.http_reqs.values.rate' results/before-${BEFORE_COMMIT}.json)
BEFORE_ERROR_RATE=$(jq '.metrics.http_req_failed.values.rate' results/before-${BEFORE_COMMIT}.json)
```

**측정 항목**:
- ✅ P95 Latency (ms)
- ✅ Throughput (req/s)
- ✅ Error Rate (%)
- ✅ VUser (동시 사용자 수)

---

### Step 2: 최적화 작업
```
[여기서 N+1 해결, 캐싱 적용, 비동기 전환 등 작업]

예: /n+1-hunt → @EntityGraph 적용
예: /cache-strategy → Caffeine 추가
예: /async-convert → CompletableFuture 적용
```

---

### Step 3: After 측정 (개선 후)
```bash
# 최적화 후 커밋
git add .
git commit -m "perf: optimize [기능명]"
AFTER_COMMIT=$(git rev-parse --short HEAD)

# 동일 조건으로 재측정
k6 run --out json=results/after-${AFTER_COMMIT}.json scripts/load-test.js

# 주요 지표 추출
AFTER_P95=$(jq '.metrics.http_req_duration.values.p95' results/after-${AFTER_COMMIT}.json)
AFTER_RPS=$(jq '.metrics.http_reqs.values.rate' results/after-${AFTER_COMMIT}.json)
AFTER_ERROR_RATE=$(jq '.metrics.http_req_failed.values.rate' results/after-${AFTER_COMMIT}.json)
```

---

### Step 4: 개선율 계산
```bash
# 개선율 자동 계산
LATENCY_IMPROVEMENT=$(echo "scale=2; ($BEFORE_P95 - $AFTER_P95) / $BEFORE_P95 * 100" | bc)
RPS_IMPROVEMENT=$(echo "scale=2; ($AFTER_RPS - $BEFORE_RPS) / $BEFORE_RPS * 100" | bc)

echo "🎯 성능 개선 결과"
echo "==================="
echo "P95 Latency: ${BEFORE_P95}ms → ${AFTER_P95}ms (${LATENCY_IMPROVEMENT}% 개선)"
echo "Throughput: ${BEFORE_RPS} req/s → ${AFTER_RPS} req/s (${RPS_IMPROVEMENT}% 개선)"
echo "Error Rate: ${BEFORE_ERROR_RATE}% → ${AFTER_ERROR_RATE}%"
```

---

### Step 5: 문서 자동 생성

#### A. 분석 리포트 (.claude/docs/analysis/)
```markdown
# 성능 개선: [기능명]
Date: $(date +%Y-%m-%d)
Commit: ${BEFORE_COMMIT} → ${AFTER_COMMIT}

## Before
- P95 Latency: ${BEFORE_P95}ms
- Throughput: ${BEFORE_RPS} req/s
- Error Rate: ${BEFORE_ERROR_RATE}%

## After
- P95 Latency: ${AFTER_P95}ms
- Throughput: ${AFTER_RPS} req/s
- Error Rate: ${AFTER_ERROR_RATE}%

## 개선율
- ⚡ Latency: **${LATENCY_IMPROVEMENT}% 개선**
- 📈 Throughput: **${RPS_IMPROVEMENT}% 향상**

## 적용 방법
- [N+1 해결 / 캐싱 / 비동기 등]

## 커밋
${AFTER_COMMIT}: perf: optimize [기능명] (${LATENCY_IMPROVEMENT}% improvement)
```

#### B. README 자동 업데이트
```markdown
## 📊 성능 개선 사례

| 기능 | Before | After | 개선율 | 방법 |
|------|--------|-------|--------|------|
| [기능명] | ${BEFORE_P95}ms | ${AFTER_P95}ms | **${LATENCY_IMPROVEMENT}%** | @EntityGraph |

### 부하 테스트 결과
- **VUser**: 500 동시 사용자 처리 가능
- **P95 Latency**: ${AFTER_P95}ms (목표 500ms 달성)
- **Error Rate**: ${AFTER_ERROR_RATE}% (목표 0.1% 달성)
```

---

## 🎓 포트폴리오 활용

### 면접 준비 (구체적 수치)
```
면접관: "성능 최적화 경험이 있나요?"

당신: "네, Sentinel 프로젝트에서 N+1 쿼리를 해결해 
      Portfolio API의 응답 시간을 2500ms에서 150ms로 
      94% 개선했습니다."

면접관: "어떻게 측정했나요?"

당신: "k6로 500 VUser 부하 테스트를 진행했고,
      Before/After P95 latency를 측정했습니다.
      Git 커밋에 모든 측정값이 기록되어 있습니다."
```

### Git History로 증명
```bash
git log --grep="perf:" --oneline

# 출력
a1b2c3d perf: resolve Portfolio N+1 (94% improvement)
e4f5g6h perf: add Caffeine cache (95% cache hit, 98% latency reduction)
i7j8k9l perf: async API calls (10x throughput, 60% latency reduction)
```

### 커밋 메시지 템플릿
```
perf([scope]): [subject] ([improvement]% improvement)

- Before: [metric] [unit]
- After: [metric] [unit]
- Method: [solution]
- Test: k6 load test with [VUser] users

Closes #[issue-number]
```

**예시**:
```
perf(portfolio): resolve N+1 query (94% improvement)

- Before: 2500ms P95 latency
- After: 150ms P95 latency  
- Method: Applied @EntityGraph with attributePaths
- Test: k6 load test with 500 VUsers

Closes #42
```

---

## 📈 추적 가능한 지표

### 1. 응답 시간 (Latency)
```
- P50, P95, P99
- Before/After 비교
- 목표값 대비 달성률
```

### 2. 처리량 (Throughput)
```
- Requests per second (RPS)
- 동시 처리 가능 VUser
- Saturation point
```

### 3. 에러율
```
- HTTP 5xx 비율
- Timeout 비율
- 안정성 지표
```

### 4. 리소스 사용률
```
- DB Connection Pool
- JVM Heap Memory
- CPU 사용률
```

---

## 🛠️ 자동화 스크립트 (선택)

```python
# scripts/measure_improvement.py
import json
import subprocess
from datetime import datetime

def measure_before():
    """최적화 전 측정"""
    commit = subprocess.check_output(['git', 'rev-parse', '--short', 'HEAD']).decode().strip()
    
    # k6 실행
    subprocess.run(['k6', 'run', '--out', f'json=results/before-{commit}.json', 
                    'scripts/load-test.js'])
    
    # 결과 파싱
    with open(f'results/before-{commit}.json') as f:
        data = json.load(f)
    
    metrics = {
        'commit': commit,
        'timestamp': datetime.now().isoformat(),
        'p95_latency': data['metrics']['http_req_duration']['values']['p95'],
        'rps': data['metrics']['http_reqs']['values']['rate'],
        'error_rate': data['metrics']['http_req_failed']['values']['rate']
    }
    
    # Baseline 저장
    with open('baselines/before.json', 'w') as f:
        json.dump(metrics, f, indent=2)
    
    print(f"✅ Before 측정 완료: {metrics}")
    return metrics

def measure_after():
    """최적화 후 측정"""
    commit = subprocess.check_output(['git', 'rev-parse', '--short', 'HEAD']).decode().strip()
    
    subprocess.run(['k6', 'run', '--out', f'json=results/after-{commit}.json',
                    'scripts/load-test.js'])
    
    with open(f'results/after-{commit}.json') as f:
        data = json.load(f)
    
    metrics = {
        'commit': commit,
        'timestamp': datetime.now().isoformat(),
        'p95_latency': data['metrics']['http_req_duration']['values']['p95'],
        'rps': data['metrics']['http_reqs']['values']['rate'],
        'error_rate': data['metrics']['http_req_failed']['values']['rate']
    }
    
    # Before 로드
    with open('baselines/before.json') as f:
        before = json.load(f)
    
    # 개선율 계산
    latency_improvement = (before['p95_latency'] - metrics['p95_latency']) / before['p95_latency'] * 100
    rps_improvement = (metrics['rps'] - before['rps']) / before['rps'] * 100
    
    print(f"\n🎯 성능 개선 결과")
    print(f"P95 Latency: {before['p95_latency']:.0f}ms → {metrics['p95_latency']:.0f}ms ({latency_improvement:.1f}% 개선)")
    print(f"Throughput: {before['rps']:.1f} req/s → {metrics['rps']:.1f} req/s ({rps_improvement:.1f}% 향상)")
    
    return metrics, before, latency_improvement, rps_improvement

# 사용법
# python scripts/measure_improvement.py before
# [최적화 작업]
# python scripts/measure_improvement.py after
```

---

## 🚀 실행 예시

```bash
# Before 측정
/metric-before-after before

Claude: 
✅ Baseline 측정 완료
- P95 Latency: 2500ms
- Throughput: 20 req/s
- Baseline 저장: baselines/before.json

Ready to optimize!

# [N+1 해결 작업]

# After 측정
/metric-before-after after

Claude:
✅ 최적화 후 측정 완료

🎯 성능 개선 결과
==================
P95 Latency: 2500ms → 150ms (94% 개선) ✅
Throughput: 20 req/s → 200 req/s (900% 향상) ✅
Error Rate: 0.5% → 0.08% ✅

📊 문서 생성:
- .claude/docs/analysis/perf-improvement-20250115.md
- README.md 업데이트

Would you like to commit these changes?
```

---

## 📝 체크리스트

### 측정 전
- [ ] k6 스크립트 준비
- [ ] 테스트 환경 안정화
- [ ] Baseline 저장 공간 확보

### 측정 중
- [ ] 동일 조건 유지 (VUser, Duration)
- [ ] 외부 요인 제거 (다른 서비스 중지)
- [ ] 충분한 워밍업 시간

### 측정 후
- [ ] Before/After 비교표 작성
- [ ] 개선율 계산
- [ ] Git 커밋 (측정값 포함)
- [ ] README 업데이트
- [ ] 면접 준비 메모
