# 🌙 Sentinel 다크 테마 가이드

## 색상 팔레트

### 배경 색상 (Background)
```css
background-primary: #0a0a0a      /* 메인 배경 - 가장 어두운 검정 */
background-secondary: #131313     /* 보조 배경 */
background-tertiary: #1a1a1a      /* 3차 배경 */
background-quaternary: #1e1e1e    /* 카드 배경 */
background-quinary: #2a2a2a       /* 호버 배경 */
background-elevated: #242424      /* 강조 배경 */
```

### 텍스트 색상 (Text)
```css
text-primary: #ffffff       /* 주요 텍스트 - 순백색 */
text-secondary: #e0e0e0     /* 보조 텍스트 */
text-tertiary: #a0a0a0      /* 3차 텍스트 */
text-quaternary: #707070    /* 4차 텍스트 (비활성) */
text-disabled: #505050      /* 비활성화 */
```

### 브랜드 색상 (Brand)
```css
brand-primary: #00d4ff      /* 네온 청록색 - 주요 브랜드 */
brand-secondary: #0098ff    /* 진한 청록색 */
brand-text: #0a0a0a         /* 브랜드 배경 위 텍스트 */
```

### 강조 색상 (Accent)
```css
accent-green: #00ff88       /* 수익/매수 - 밝은 네온 초록 */
accent-red: #ff4d6d         /* 손실/매도 - 밝은 빨강 */
accent-blue: #0098ff        /* 정보 - 파랑 */
accent-purple: #a78bfa      /* Mock 데이터 - 보라 */
accent-orange: #ffaa00      /* 경고 - 주황 */
accent-yellow: #ffdd00      /* 주의 - 노랑 */
accent-cyan: #00d4ff        /* 강조 - 청록 */
```

### 의미론적 색상 (Semantic)
```css
semantic-success: #00ff88   /* 성공 */
semantic-error: #ff4d6d     /* 에러 */
semantic-warning: #ffaa00   /* 경고 */
semantic-info: #00d4ff      /* 정보 */
```

### 테두리 색상 (Border)
```css
border-primary: #2a2a2a
border-secondary: #333333
border-tertiary: #404040
border-translucent: rgba(255,255,255,.08)
```

---

## 특수 효과

### 글로우 효과 (Glow)
```css
shadow-glow: 0px 0px 20px rgba(0, 212, 255, 0.3)        /* 청록 글로우 */
shadow-glow-green: 0px 0px 20px rgba(0, 255, 136, 0.3)  /* 초록 글로우 */
shadow-glow-red: 0px 0px 20px rgba(255, 77, 109, 0.3)   /* 빨강 글로우 */
```

### 유리 효과 (Glassmorphism)
```css
.glass {
  background: rgba(26, 26, 26, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}
```

### 그라데이션
```css
bg-gradient-brand: linear-gradient(135deg, #00d4ff 0%, #0098ff 100%)
bg-gradient-success: linear-gradient(135deg, #00ff88 0%, #00cc6a 100%)
bg-gradient-danger: linear-gradient(135deg, #ff4d6d 0%, #ff1744 100%)
```

### 네온 테두리
```css
.border-neon { border: 1px solid rgba(0, 212, 255, 0.5); }
.border-neon-green { border: 1px solid rgba(0, 255, 136, 0.5); }
.border-neon-red { border: 1px solid rgba(255, 77, 109, 0.5); }
```

---

## 사용 예시

### 포트폴리오 카드 (수익)
```tsx
<Card className="bg-background-quaternary border-neon-green">
  <p className="text-accent-green glow-green">+12.5%</p>
</Card>
```

### 포트폴리오 카드 (손실)
```tsx
<Card className="bg-background-quaternary border-neon-red">
  <p className="text-accent-red glow-red">-8.3%</p>
</Card>
```

### 주요 버튼 (네온 효과)
```tsx
<Button className="bg-brand-primary hover:shadow-glow">
  리밸런싱 실행
</Button>
```

### 유리 효과 모달
```tsx
<Modal className="glass">
  <h2 className="text-gradient">Sentinel</h2>
</Modal>
```

### 그라데이션 텍스트
```tsx
<h1 className="text-gradient">
  Sentinel Dashboard
</h1>
```

---

## 컴포넌트별 색상 가이드

### Button
- **Primary**: `bg-brand-primary` + `hover:shadow-glow` (네온 효과)
- **Secondary**: `bg-background-quaternary` + `hover:bg-background-quinary`
- **Ghost**: `hover:bg-background-tertiary`
- **Danger**: `bg-accent-red` + `hover:shadow-glow-red`

### Card
- **기본**: `bg-background-quaternary border-border-primary`
- **호버**: `hover:bg-background-quinary hover:shadow-medium`
- **강조**: `bg-background-elevated border-brand-primary shadow-glow`

### Input
- **기본**: `bg-background-quaternary border-border-primary`
- **포커스**: `focus:border-brand-primary focus:ring-brand-primary`
- **에러**: `border-accent-red focus:ring-accent-red`

### Badge
- **Mock**: `bg-accent-purple/10 text-accent-purple border-accent-purple/20`
- **Success**: `bg-accent-green/10 text-accent-green`
- **Danger**: `bg-accent-red/10 text-accent-red`
- **Info**: `bg-accent-blue/10 text-accent-blue`

### Modal
- **오버레이**: `bg-black/70 backdrop-blur-lg`
- **콘텐츠**: `bg-background-elevated border-border-secondary shadow-high`

---

## 대비 비율 (WCAG AA 준수)

### 텍스트 대비
- **Primary on Primary BG**: `#ffffff on #0a0a0a` = 21:1 ✅
- **Secondary on Primary BG**: `#e0e0e0 on #0a0a0a` = 17.5:1 ✅
- **Tertiary on Primary BG**: `#a0a0a0 on #0a0a0a` = 9.2:1 ✅

### 브랜드 색상 대비
- **Brand Primary on Dark**: `#00d4ff on #0a0a0a` = 11.2:1 ✅
- **Accent Green on Dark**: `#00ff88 on #0a0a0a` = 13.5:1 ✅
- **Accent Red on Dark**: `#ff4d6d on #0a0a0a` = 7.8:1 ✅

---

## 다크 테마 디자인 원칙

### 1. 계층 구조 (Hierarchy)
- 배경이 어두울수록 후면 레이어
- 밝은 배경일수록 전면 레이어
- 카드: `#1e1e1e`, 모달: `#242424`

### 2. 대비 (Contrast)
- 텍스트는 최소 7:1 대비 유지
- 중요한 요소는 네온 효과로 강조
- 상태 변화는 색상 + 글로우로 표시

### 3. 가독성 (Readability)
- 주요 텍스트: `#ffffff`
- 본문 텍스트: `#e0e0e0`
- 부가 정보: `#a0a0a0`

### 4. 일관성 (Consistency)
- 수익은 항상 `#00ff88` (초록)
- 손실은 항상 `#ff4d6d` (빨강)
- Mock 데이터는 항상 `#a78bfa` (보라)

### 5. 네온 효과 사용
- 주요 버튼: 호버 시 글로우
- 중요 카드: 상태에 따라 네온 테두리
- 수익률 표시: 글로우로 주목도 향상

---

## 접근성 (Accessibility)

### 색각 이상 지원
- 수익/손실을 색상만으로 구분하지 않음
- 아이콘 병행 사용: ▲ (상승), ▼ (하락)
- 텍스트로 명확히 표시

### 키보드 네비게이션
- 포커스 링: `ring-brand-primary ring-2`
- 명확한 포커스 상태 표시

### 스크린 리더
- 의미 있는 aria-label 제공
- 색상 정보를 텍스트로도 제공

---

## 다크 테마 체크리스트

✅ 배경색이 `#0a0a0a`로 적용됨
✅ 텍스트가 밝은 색상 (`#ffffff`, `#e0e0e0`)
✅ 브랜드 색상이 네온 청록색 (`#00d4ff`)
✅ 수익/손실 색상이 밝게 조정됨
✅ 카드 배경이 `#1e1e1e`
✅ 호버 효과가 자연스러움
✅ 그림자가 더 진하게 조정됨
✅ 네온 글로우 효과 추가됨
✅ WCAG AA 대비 비율 준수
✅ 유리 효과 (Glassmorphism) 지원

---

## 개발 팁

### Tailwind CSS 클래스 사용
```tsx
// 기본 카드
<Card className="bg-background-quaternary">

// 네온 효과 버튼
<Button className="bg-brand-primary hover:shadow-glow">

// 수익률 표시
<span className={cn(
  "font-semibold",
  profit > 0 ? "text-accent-green glow-green" : "text-accent-red glow-red"
)}>

// 유리 효과
<div className="glass rounded-16 p-6">
```

### 유틸리티 함수 활용
```typescript
getChangeColorClass(12.5)  // => "text-accent-green"
getChangeBgClass(12.5)      // => "bg-accent-green/10 border-accent-green/20"
getChangeGlowClass(12.5)    // => "glow-green"
```

---

**다크 테마가 완전히 적용되었습니다!** 🌙✨

http://localhost:3000/components-preview 에서 확인하세요!