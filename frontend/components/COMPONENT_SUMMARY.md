# 📦 Sentinel 컴포넌트 라이브러리 완성! ✅

## 🎉 완료된 작업

### 1. ✅ Next.js 14 프로젝트 초기화
- **프레임워크**: Next.js 14 (App Router)
- **언어**: TypeScript
- **스타일링**: Tailwind CSS + Linear Design System
- **상태 관리**: Zustand
- **데이터 페칭**: TanStack Query
- **차트**: Recharts

### 2. ✅ Linear Design System 테마 적용
```typescript
// 브랜드 색상
brand-primary: #7070ff  // 주요 버튼, 링크
brand-text: #fff        // 브랜드 텍스트

// 강조 색상
accent-green: #4cb782   // 수익, 매수 표시
accent-red: #eb5757     // 손실, 매도 표시
accent-indigo: #5e6ad2  // Mock 데이터 표시

// 타이포그래피
폰트: Inter Variable, SF Pro Display
크기: 11px ~ 18px (5단계)
굵기: 300 ~ 680 (5단계)
```

### 3. ✅ 재사용 가능한 UI 컴포넌트 (13개)

#### 기본 컴포넌트
1. **Button** - 4가지 변형 (primary, secondary, ghost, danger), 3가지 크기
2. **Card** - 유연한 패딩, 그림자, 호버 효과
3. **Input** - 라벨, 에러, 도움말, 아이콘 지원
4. **Badge** - 6가지 변형 (default, success, warning, danger, info, mock)
5. **Modal** - ESC 닫기, 외부 클릭 닫기, 4가지 크기
6. **Spinner** - 3가지 크기, 전체 화면 버전
7. **Skeleton** - text, circular, rectangular, 카드 레이아웃

#### 특화 컴포넌트
8. **MockDataBadge** - Mock 데이터 표시 (출처 표시 가능)
9. **EmptyState** - 데이터 없을 때 UI
10. **PortfolioCard** - 포트폴리오 카드 (수익률 자동 색상)
11. **HoldingItem** - 보유 종목 아이템 (수정/삭제 기능)
12. **RebalancingModal** - 리밸런싱 권장사항 팝업
13. **IndexCard** - 시장 지수 카드 (변동 표시)

#### 마켓 데이터 컴포넌트
14. **AssetFilter** - 7가지 자산 분류 필터 (이모지 포함)
15. **StockSearchBar** - 종목 검색 바

### 4. ✅ 유틸리티 함수 (11개)

```typescript
// Tailwind 클래스 병합
cn(...classes)

// 포맷팅
formatCurrency(1234567) // => "1,234,567원"
formatNumber(1234.56, 2) // => "1,234.56"
formatPercent(12.34) // => "+12.34%"
formatDate(new Date()) // => "2025년 9월 30일"
formatRelativeTime(date) // => "2시간 전"

// 스타일링
getChangeColorClass(12.5) // => "text-accent-green"

// 에러 처리
getErrorMessage(error) // => "에러 메시지"
```

### 5. ✅ TypeScript 타입 정의
- User, AuthResponse
- Portfolio, PortfolioHolding
- StockPrice, MarketIndex
- RebalancingRecommendation, RebalancingStrategy
- AssetClass, ApiResponse, ApiError

---

## 🌐 실행 방법

### 개발 서버 시작
```bash
cd frontend
npm run dev
```

### 컴포넌트 미리보기
http://localhost:3000/components-preview

모든 컴포넌트를 한 페이지에서 확인 가능! 🎨

---

## 📸 컴포넌트 미리보기

### Buttons (4 variants × 3 sizes)
```
Primary  Secondary  Ghost  Danger  Loading  Disabled
Small    Medium     Large
```

### Cards (3 shadow levels)
```
기본 카드      호버 카드      그림자 강조
```

### Inputs (with labels, errors, helpers, icons)
```
이메일 입력
비밀번호 입력
에러 예시 (빨간색 테두리)
도움말 예시
```

### Badges (6 variants)
```
Default  Success  Warning  Danger  Info  Mock Data
```

### Portfolio Card
```
┌─────────────────────────────────┐
│ 장기 투자 포트폴리오        [활성]│
│ S&P 500 중심의 장기 투자 전략    │
│                                 │
│ 총 자산                         │
│ ₩50,000,000                     │
│                                 │
│ 손익 금액      수익률           │
│ ₩5,000,000    +11.11%          │
│                                 │
│ 보유 종목: 0개                  │
└─────────────────────────────────┘
```

### Holding Item
```
AAPL 100주
┌────────────────────────────────────────┐
│ 평균 매수가  현재가  평가금액  손익    │
│ ₩150,000   ₩180,000  ₩18M  ₩3M(+20%) │
│                           [수정] [삭제]│
└────────────────────────────────────────┘
```

### Rebalancing Modal
```
🔻 매도 (SELL)
AAPL  현재 40% → 목표 30%  -25주  -₩4,500,000

🔺 매수 (BUY)
GOOGL  현재 20% → 목표 30%  +10주  +₩3,000,000
```

### Index Card
```
S&P 500                [SPX]
4,567.89
▲ 45.23 (+1.00%)
```

### Asset Filter
```
[전체] 주식 부동산 코인 금 채권 현금
🌐   📈  🏠   ₿  🏆 📜  💵
```

---

## 🎯 다음 단계

### 이제 디자인 수정 요청을 받습니다!

수정하고 싶은 부분을 알려주세요:

#### 1. 색상 조정
- 브랜드 색상 변경 (#7070ff → 다른 색)
- 강조 색상 변경 (수익/손실 색상)
- 배경/텍스트 색상 조정

#### 2. 크기/간격
- 버튼 크기 조정
- 카드 패딩 변경
- 테두리 반경 수정

#### 3. 타이포그래피
- 폰트 크기 조정
- 폰트 굵기 변경
- 제목/본문 스타일

#### 4. 컴포넌트 스타일
- 특정 컴포넌트 디자인 변경
- 호버 효과 조정
- 애니메이션 속도

#### 5. 추가 변형
- 새로운 버튼 스타일
- 카드 레이아웃 추가
- 커스텀 컴포넌트

---

## 📝 현재 상태

✅ **완료**:
1. Next.js 14 프로젝트 초기화
2. Linear Design System 적용
3. 15개 재사용 컴포넌트
4. 11개 유틸리티 함수
5. TypeScript 타입 정의
6. 컴포넌트 미리보기 페이지

⏳ **대기 중**:
1. 디자인 수정 (사용자 피드백)
2. 레이아웃 컴포넌트 (Header, Footer)
3. 인증 시스템 (Kakao OAuth)
4. API 클라이언트 레이어
5. 실제 페이지 구현

---

## 💬 디자인 피드백 예시

**예시 1 - 색상 변경**:
> "브랜드 색상을 파란색(#4ea7fc)으로 변경하고 싶어요"

**예시 2 - 버튼 크기**:
> "버튼이 너무 작은 것 같아요. 기본 크기를 조금 키워주세요"

**예시 3 - 카드 스타일**:
> "포트폴리오 카드에 그라데이션 배경을 추가하고 싶어요"

**예시 4 - 전체 톤**:
> "전체적으로 더 모던하고 미니멀한 느낌으로 바꿔주세요"

**예시 5 - 특정 컴포넌트**:
> "리밸런싱 모달을 더 눈에 띄게 만들고 싶어요"

---

## 🚀 준비 완료!

컴포넌트 라이브러리가 준비되었습니다.
디자인 수정 사항을 알려주시면 바로 적용하고 본격적인 페이지 개발로 넘어가겠습니다! 🎨