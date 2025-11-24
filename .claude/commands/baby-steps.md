# Baby Steps - 작은 단계로 개발

## 🎯 규칙
**15분 타이머**
- 15분 내 완료 못하면 → Revert
- 15분 내 완료하면 → Commit
- 한 번에 하나만 변경

---

## ⏱️ 워크플로우

### Step 1: 타이머 시작
```bash
# 15분 타이머
timer 15m

# 또는
sleep 900 && echo "⏰ Time's up!"
```

### Step 2: 작은 변경 하나만
```java
// ✅ Good - 한 가지만
"테스트 추가"
"메서드 하나 구현"
"변수명 리팩토링"

// ❌ Bad - 너무 많음
"테스트 + 구현 + 리팩토링 + 문서"
```

### Step 3: 결과 판단

#### 완료 시 (15분 내)
```bash
git add .
git commit -m "feat: add price validation"
# ✅ 다음 단계로
```

#### 미완료 시 (15분 초과)
```bash
git reset --hard HEAD
# 🔄 처음부터 다시 (더 작게 쪼개기)
```

---

## 📝 실전 예시

### Scenario: 가격 알림 기능 추가

#### ❌ Bad - 한번에 너무 많음
```
45분 소요:
- AlertEntity 생성
- AlertRepository 구현
- AlertService 구현
- AlertController 구현
- 테스트 작성
→ 타이머 초과! Revert!
```

#### ✅ Good - 작게 쪼개기
```
Step 1 (10분): AlertEntity 생성 + Commit
Step 2 (12분): AlertRepository 생성 + Commit
Step 3 (8분): AlertService 구현 + Commit
Step 4 (15분): AlertController 구현 + Commit
Step 5 (10분): 테스트 작성 + Commit

총 5개 Commit, 55분
→ 모두 성공! 🎉
```

---

## 🎯 장점

### 1. 집중력 향상
```
15분 = 하나의 작업에만 집중
→ 멀티태스킹 방지
```

### 2. 롤백 부담 감소
```
15분 작업만 날아감
→ 큰 손실 없음
```

### 3. Atomic Commit
```
하나의 변경 = 하나의 Commit
→ Git History 깔끔
```

### 4. 진행 가시화
```
매 15분마다 Commit
→ 진행 상황 명확
```

---

## 📊 Git History

### Baby Steps 적용 전
```
a1b2c3d feat: add entire alert system (2 hours)
[400 lines changed, hard to review]
```

### Baby Steps 적용 후
```
a1b2c3d feat: add AlertEntity (10min)
b2c3d4e feat: add AlertRepository (12min)
c3d4e5f feat: add AlertService (15min)
d4e5f6g feat: add AlertController (15min)
e5f6g7h test: add alert tests (10min)

[5 commits, each < 100 lines, easy to review]
```

---

## 🚀 팁

### 작업 쪼개는 법
```
큰 작업: "가격 알림 기능"
↓
1. Entity 정의
2. Repository 생성
3. Service 로직
4. Controller 엔드포인트
5. 테스트 작성
6. 문서화

각각 15분 내 완료 가능!
```

### 15분 내 못 끝낼 것 같으면
```
→ 더 작게 쪼개기!

"Service 구현" (20분 예상)
↓
"Service 메서드 1개만" (10분)
"Service 메서드 2개만" (15분)
```

---

## ⚠️ 주의사항

### 15분은 절대 규칙 아님
```
상황에 따라 조정:
- 간단한 작업: 10분
- 복잡한 작업: 20분
- 학습 중: 30분

중요한 건 "작게 쪼개기"
```

### Revert 두려워 말기
```
Revert = 실패 아님
Revert = 너무 크게 시작했다는 신호

→ 더 작게 쪼개서 재도전!
```
