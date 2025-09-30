# Sentinel 프론트엔드

Next.js 14 + TypeScript + Tailwind CSS 기반의 투자 포트폴리오 관리 플랫폼

## 🚀 빠른 시작

### 개발 서버 실행
```bash
npm run dev
```

서버 실행 후 다음 주소로 접속:
- **메인 앱**: http://localhost:3000
- **컴포넌트 미리보기**: http://localhost:3000/components-preview

### 빌드
```bash
npm run build
npm start
```

## 📦 기술 스택

- **프레임워크**: Next.js 14 (App Router)
- **언어**: TypeScript
- **스타일링**: Tailwind CSS (Linear Design System)
- **상태 관리**: Zustand
- **데이터 페칭**: TanStack Query (React Query)
- **HTTP 클라이언트**: Axios
- **차트**: Recharts
- **아이콘**: Lucide React

## 🎨 디자인 시스템

Linear Design System을 기반으로 한 일관된 디자인 언어

### 색상 팔레트
- **Brand Primary**: `#7070ff` - 주요 액션 버튼, 링크
- **Accent Green**: `#4cb782` - 수익, 매수
- **Accent Red**: `#eb5757` - 손실, 매도
- **Accent Indigo**: `#5e6ad2` - Mock 데이터 표시

### 타이포그래피
- **폰트**: Inter Variable, SF Pro Display
- **크기**: micro(11px), mini(12px), small(13px), regular(15px), large(18px)

## 📁 프로젝트 구조

```
frontend/
├── app/                          # Next.js App Router 페이지
│   ├── components-preview/       # 컴포넌트 미리보기 페이지
│   ├── auth/                     # 인증 페이지
│   ├── portfolios/               # 포트폴리오 페이지
│   ├── market/                   # 시장 데이터 페이지
│   ├── lab/                      # 백테스팅/실험실
│   └── layout.tsx                # 루트 레이아웃
├── components/                   # 재사용 가능한 컴포넌트
│   ├── ui/                       # 기본 UI 컴포넌트
│   ├── layout/                   # 레이아웃 컴포넌트
│   ├── portfolio/                # 포트폴리오 관련
│   ├── market/                   # 마켓 데이터 관련
│   ├── carousel/                 # 캐러셀
│   └── common/                   # 공통 컴포넌트
├── lib/                          # 유틸리티 함수
├── types/                        # TypeScript 타입 정의
├── hooks/                        # 커스텀 훅
├── stores/                       # Zustand 스토어
└── public/                       # 정적 파일
```

## 🧩 컴포넌트 라이브러리

### UI 컴포넌트
- `Button` - 다양한 변형의 버튼
- `Card` - 콘텐츠 카드
- `Input` - 입력 필드
- `Badge` - 뱃지/태그
- `Modal` - 모달 다이얼로그
- `Spinner` - 로딩 스피너
- `Skeleton` - 스켈레톤 로딩

### 포트폴리오 컴포넌트
- `PortfolioCard` - 포트폴리오 카드
- `HoldingItem` - 보유 종목 아이템
- `RebalancingModal` - 리밸런싱 팝업

### 마켓 데이터 컴포넌트
- `IndexCard` - 시장 지수 카드
- `AssetFilter` - 자산 분류 필터
- `StockSearchBar` - 종목 검색

### 공통 컴포넌트
- `MockDataBadge` - Mock 데이터 표시
- `EmptyState` - 빈 상태 UI

자세한 내용은 `COMPONENT_LIBRARY.md` 참고

## 🔧 환경 변수

`.env.local` 파일 생성 (`.env.example` 참고):

```bash
# API 설정
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

# Kakao OAuth
NEXT_PUBLIC_KAKAO_CLIENT_ID=your-kakao-client-id
NEXT_PUBLIC_KAKAO_REDIRECT_URI=http://localhost:3000/auth/callback

# 개발 모드
NEXT_PUBLIC_DEV_MODE=true
```

## 🎯 다음 개발 단계

1. ✅ 컴포넌트 라이브러리 구축
2. ⏳ 레이아웃 컴포넌트 (Header, Footer)
3. ⏳ 인증 시스템 (Kakao OAuth)
4. ⏳ API 클라이언트 레이어
5. ⏳ 포트폴리오 관리 페이지
6. ⏳ 백테스팅/실험실 UI
7. ⏳ 마켓 데이터 페이지
