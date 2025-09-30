# 📦 Sentinel 프론트엔드 컴포넌트 라이브러리

## 개요
Linear Design System 기반의 재사용 가능한 React 컴포넌트 라이브러리

---

## 🎨 디자인 시스템 (Tailwind Config)

### 색상 팔레트
```typescript
// 브랜드 색상
brand-primary: #7070ff  // 주요 액션
brand-text: #fff        // 브랜드 텍스트

// 배경 색상
background-primary: #fff          // 메인 배경
background-secondary: #f9f8f9     // 보조 배경
background-tertiary: #f4f2f4      // 3차 배경

// 텍스트 색상
text-primary: #282a30    // 주요 텍스트
text-secondary: #3c4149  // 보조 텍스트
text-tertiary: #6f6e77   // 3차 텍스트

// 강조 색상
accent-green: #4cb782   // 수익/매수
accent-red: #eb5757     // 손실/매도
accent-blue: #4ea7fc    // 정보
accent-indigo: #5e6ad2  // Mock 데이터
```

### 타이포그래피
- **폰트**: Inter Variable, SF Pro Display
- **크기**: micro(11px) → mini(12px) → small(13px) → regular(15px) → large(18px)
- **굵기**: light(300), normal(400), medium(510), semibold(590), bold(680)

### 간격 & 반경
- **Border Radius**: 4px, 6px, 8px, 12px, 16px, 24px
- **Shadow**: tiny, low, medium, high
- **Transition**: quick(100ms), regular(250ms)

---

## 📁 컴포넌트 구조

### 1. UI 컴포넌트 (`/components/ui`)
기본 UI 빌딩 블록

#### Button
```tsx
<Button variant="primary" size="md" loading={false}>
  클릭하세요
</Button>
```
- **Variants**: primary, secondary, ghost, danger
- **Sizes**: sm, md, lg
- **Features**: 로딩 상태, 전체 너비, 비활성화

#### Card
```tsx
<Card padding="md" shadow="low" hover>
  <CardHeader>
    <CardTitle>제목</CardTitle>
  </CardHeader>
  <CardContent>내용</CardContent>
</Card>
```
- **Padding**: none, sm, md, lg
- **Shadow**: none, tiny, low, medium, high
- **Features**: 호버 효과, 클릭 이벤트

#### Input
```tsx
<Input
  label="이메일"
  type="email"
  error="유효한 이메일을 입력하세요"
  leftIcon={<SearchIcon />}
  rightIcon={<ClearIcon />}
/>
```
- **Features**: 라벨, 에러 메시지, 도움말, 아이콘

#### Badge
```tsx
<Badge variant="success">활성</Badge>
<Badge variant="mock">Mock 데이터</Badge>
```
- **Variants**: default, success, warning, danger, info, mock

#### Modal
```tsx
<Modal isOpen={true} onClose={handleClose} title="제목" size="md">
  <p>내용</p>
  <ModalFooter>
    <Button variant="secondary">취소</Button>
    <Button variant="primary">확인</Button>
  </ModalFooter>
</Modal>
```
- **Sizes**: sm, md, lg, xl
- **Features**: ESC로 닫기, 외부 클릭 닫기

#### Spinner & Skeleton
```tsx
<Spinner size="md" />
<FullPageSpinner />

<Skeleton variant="text" />
<Skeleton variant="circular" className="w-12 h-12" />
<SkeletonCard />
```
- **로딩 상태**: 스피너 또는 스켈레톤 UI

---

### 2. 공통 컴포넌트 (`/components/common`)

#### MockDataBadge
```tsx
<MockDataBadge
  show={true}
  source="frontend_reference/mock_portfolios.json"
/>
```
- **목적**: Mock 데이터를 사용하는 컴포넌트에 표시
- **스타일**: 보라색 테두리, 아이콘 포함

#### EmptyState
```tsx
<EmptyState
  title="포트폴리오가 없습니다"
  description="첫 번째 포트폴리오를 만들어보세요"
  actionLabel="포트폴리오 생성"
  onAction={() => setShowCreateModal(true)}
/>
```
- **용도**: 데이터가 없을 때 표시

---

### 3. 포트폴리오 컴포넌트 (`/components/portfolio`)

#### PortfolioCard
```tsx
<PortfolioCard
  portfolio={portfolio}
  onClick={() => navigate(`/portfolios/${portfolio.id}`)}
  showBadge={true}
/>
```
- **표시 정보**: 이름, 총 자산, 손익, 수익률, 보유 종목 수
- **색상**: 수익률에 따라 자동 색상 변경

#### HoldingItem
```tsx
<HoldingItem
  holding={holding}
  onEdit={() => handleEdit(holding.id)}
  onDelete={() => handleDelete(holding.id)}
/>
```
- **표시 정보**: 심볼, 수량, 평균 매수가, 현재가, 평가 금액, 손익
- **액션**: 수정, 삭제 버튼

#### RebalancingModal
```tsx
<RebalancingModal
  isOpen={showRebalancing}
  onClose={() => setShowRebalancing(false)}
  recommendations={recommendations}
  onExecute={handleExecuteRebalancing}
  loading={loading}
/>
```
- **기능**: 매수/매도 권장사항 표시
- **그룹화**: SELL, BUY, HOLD로 구분
- **정보**: 현재 비중, 목표 비중, 조정 수량, 예상 금액

---

### 4. 마켓 데이터 컴포넌트 (`/components/market`)

#### IndexCard
```tsx
<IndexCard
  index={{
    name: "S&P 500",
    symbol: "SPX",
    value: 4500.25,
    change: 25.30,
    changePercent: 0.56,
    timestamp: "2025-09-30T15:00:00Z"
  }}
/>
```
- **표시 정보**: 지수명, 현재가, 변동, 변동률
- **색상**: 상승/하락에 따라 자동 색상

#### AssetFilter
```tsx
<AssetFilter
  selected={selectedAsset}
  onChange={setSelectedAsset}
/>
```
- **필터 옵션**: 전체, 주식, 부동산, 코인, 금, 채권, 현금
- **아이콘**: 각 자산 분류별 이모지

#### StockSearchBar
```tsx
<StockSearchBar
  onSearch={(query) => handleSearch(query)}
  placeholder="종목명 또는 심볼 검색"
/>
```
- **기능**: 실시간 검색, 검색어 초기화
- **아이콘**: 검색 아이콘, 클리어 버튼

---

## 🛠️ 유틸리티 함수 (`/lib/utils.ts`)

### 포맷팅 함수
```typescript
// Tailwind 클래스 병합
cn('text-red', 'hover:text-blue', className)

// 통화 포맷 (한국 원화)
formatCurrency(1234567) // => "1,234,567원"

// 숫자 포맷
formatNumber(1234567.89, 2) // => "1,234,567.89"

// 퍼센트 포맷 (+/- 기호)
formatPercent(12.34) // => "+12.34%"
formatPercent(-5.67) // => "-5.67%"

// 날짜 포맷
formatDate(new Date()) // => "2025년 9월 30일"

// 상대 시간
formatRelativeTime(date) // => "2시간 전"

// 수익률 색상 클래스
getChangeColorClass(12.5) // => "text-accent-green"
getChangeColorClass(-5.2) // => "text-accent-red"

// 에러 메시지 추출
getErrorMessage(error) // => "에러 메시지"
```

---

## 📊 TypeScript 타입 정의 (`/types/index.ts`)

### 주요 타입
```typescript
// 사용자
interface User {
  id: number
  kakaoId: string
  nickname: string
  email: string
  profileImage?: string
  createdAt: string
}

// 포트폴리오
interface Portfolio {
  id: number
  name: string
  totalValue: number
  totalGainLoss: number
  totalGainLossPercent: number
  holdings: PortfolioHolding[]
}

// 보유 종목
interface PortfolioHolding {
  id: number
  symbol: string
  quantity: number
  averageCost: number
  currentPrice: number
  gainLossPercent: number
}

// 리밸런싱 권장
interface RebalancingRecommendation {
  symbol: string
  action: 'BUY' | 'SELL' | 'HOLD'
  quantity: number
  estimatedAmount: number
}

// 시장 지수
interface MarketIndex {
  name: string
  symbol: string
  value: number
  changePercent: number
}

// 자산 분류
type AssetClass = 'STOCK' | 'REAL_ESTATE' | 'CRYPTO' | 'GOLD' | 'BOND' | 'CASH'
```

---

## ✅ 다음 단계: 필요한 컴포넌트

### 레이아웃 컴포넌트 (아직 미생성)
- `Header.tsx` - 네비게이션 헤더
- `Footer.tsx` - 푸터
- `Container.tsx` - 페이지 컨테이너

### 추가 컴포넌트
- `Carousel.tsx` - 추천 포트폴리오 캐러셀
- `PortfolioChart.tsx` - 수익률 차트 (Recharts 사용)
- `PriceChart.tsx` - 가격 차트
- `Table.tsx` - 데이터 테이블

### 페이지별 컴포넌트
- 홈페이지 섹션 컴포넌트들
- 백테스팅/실험실 UI
- 대시보드 위젯

---

## 🎯 사용 예시

### 포트폴리오 목록 페이지
```tsx
import { PortfolioCard } from '@/components/portfolio/PortfolioCard'
import { EmptyState } from '@/components/common/EmptyState'
import { MockDataBadge } from '@/components/common/MockDataBadge'

export default function PortfoliosPage() {
  const { portfolios, loading } = usePortfolios()

  if (loading) return <SkeletonCard />

  if (!portfolios.length) {
    return (
      <EmptyState
        title="포트폴리오가 없습니다"
        actionLabel="생성하기"
        onAction={() => setShowCreate(true)}
      />
    )
  }

  return (
    <div>
      <MockDataBadge show={true} source="mock_portfolios.json" />
      <div className="grid grid-cols-3 gap-4">
        {portfolios.map(p => (
          <PortfolioCard key={p.id} portfolio={p} />
        ))}
      </div>
    </div>
  )
}
```

### 리밸런싱 실행
```tsx
import { RebalancingModal } from '@/components/portfolio/RebalancingModal'
import { Button } from '@/components/ui/Button'

export default function PortfolioDetail() {
  const [showRebalancing, setShowRebalancing] = useState(false)
  const { recommendations, execute } = useRebalancing(portfolioId)

  return (
    <>
      <Button onClick={() => setShowRebalancing(true)}>
        리밸런싱 분석
      </Button>

      <RebalancingModal
        isOpen={showRebalancing}
        onClose={() => setShowRebalancing(false)}
        recommendations={recommendations}
        onExecute={execute}
      />
    </>
  )
}
```

---

## 🚀 다음 작업: 디자인 수정 요청

현재 컴포넌트들이 준비되었습니다. 디자인적으로 수정하고 싶은 부분을 알려주세요:

1. **색상 조정**: 브랜드 색상, 강조 색상 변경
2. **간격/크기**: 버튼 크기, 카드 패딩, 테두리 반경
3. **타이포그래피**: 폰트 크기, 굵기 조정
4. **컴포넌트 스타일**: 특정 컴포넌트의 디자인 변경
5. **추가 변형**: 새로운 버튼 스타일, 카드 레이아웃 등

수정 사항을 알려주시면 바로 반영하고 본격적인 페이지 개발로 넘어가겠습니다! 🎨