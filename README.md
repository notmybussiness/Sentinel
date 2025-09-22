# Project Sentinel 🚀

데이터 기반 투자 대시보드 - 감정이 아닌 데이터에 기반한 합리적인 투자 결정을 도와주는 플랫폼

## 📋 프로젝트 개요

Project Sentinel은 사용자가 감정적이 아닌 합리적인 투자 결정을 내릴 수 있도록 도와주는 데이터 기반 투자 대시보드입니다. 실시간 시장 데이터, 포트폴리오 관리, 시장 지표 분석 기능을 제공합니다.

## 🏗️ 모노레포 구조

```
Sentinel/
├── frontend/              # Next.js 14 프론트엔드
│   ├── src/
│   │   ├── app/          # App Router 구조
│   │   ├── components/   # 재사용 가능한 컴포넌트
│   │   ├── lib/          # 유틸리티 함수
│   │   ├── hooks/        # 커스텀 React 훅
│   │   ├── store/        # Zustand 상태 관리
│   │   ├── types/        # TypeScript 타입 정의
│   │   └── utils/        # 헬퍼 함수
│   └── public/           # 정적 파일
└── backend/               # Spring Boot 백엔드
    ├── src/
    │   ├── main/java/    # Java 소스 코드
    │   └── main/resources/  # 애플리케이션 설정
    └── build.gradle      # 빌드 설정
```

## 🛠 기술 스택

### Frontend
- **Next.js 14** (App Router)
- **TypeScript**
- **Tailwind CSS** + **shadcn/ui**
- **Framer Motion** (애니메이션)
- **Zustand** (상태 관리)
- **Radix UI** (접근 가능한 UI 컴포넌트)

### Backend
- **Spring Boot 3.5.5** (Java 17)
- **PostgreSQL** (메인 데이터베이스)
- **Redis** (캐싱)
- **Spring Data JPA** (ORM)
- **Spring Security + JWT** (인증/인가)

### External APIs
- **Alpha Vantage API** (주요 데이터 소스)
- **Finnhub API** (보조 데이터 소스)

## 🚀 빠른 시작

### 사전 요구사항
- Node.js 18+ 
- Java 17+
- PostgreSQL (프로덕션용)
- Redis (프로덕션용)

### 설치 및 실행

1. **프로젝트 클론**
```bash
git clone <repository-url>
cd Sentinel
```

2. **의존성 설치**
```bash
npm run setup
```

3. **개발 서버 실행 (동시 실행)**
```bash
npm run dev
```

이 명령은 다음을 동시에 실행합니다:
- 백엔드: http://localhost:8080
- 프론트엔드: http://localhost:3000

### 개별 실행

**백엔드만 실행:**
```bash
npm run dev:backend
```

**프론트엔드만 실행:**
```bash
npm run dev:frontend
```

## 📊 주요 기능

### Phase 1 (MVP) - 현재 단계
- [x] 기본 Spring Boot 애플리케이션 설정
- [x] 환경별 설정 분리
- [x] 데이터베이스 연결 설정
- [x] 모노레포 구조 구성
- [x] Next.js 14 프론트엔드 셋업
- [ ] 사용자 인증 (Kakao OAuth2)
- [ ] 기본 API 엔드포인트 연동
- [ ] 시장 데이터 수집 서비스

### Phase 2 (향후)
- [ ] 실시간 시장 데이터 스트리밍
- [ ] 투자 포트폴리오 관리
- [ ] 시장 지표 대시보드
- [ ] 알림 시스템

### Phase 3 (향후)
- [ ] 고급 분석 도구
- [ ] 백테스팅 기능
- [ ] AI 기반 투자 추천

## 🧪 테스트

```bash
# 전체 테스트
npm run test

# 백엔드 테스트
npm run test:backend

# 프론트엔드 테스트
npm run test:frontend
```

## 📦 빌드 및 배포

### 로컬 빌드
```bash
npm run build
```

### Docker 배포
```bash
# Docker 컨테이너 빌드
npm run docker:build

# 서비스 시작
npm run docker:up

# 로그 확인
npm run docker:logs

# 서비스 중지
npm run docker:down
```

## 🔧 개발 환경

### Backend 개발 환경
- H2 인메모리 데이터베이스
- 상세한 로깅
- H2 콘솔: http://localhost:8080/h2-console

### Frontend 개발 환경  
- Hot Reload 활성화
- TypeScript 타입 체크
- ESLint + Prettier

## 📝 API 문서

API 문서는 애플리케이션 실행 후 다음 URL에서 확인할 수 있습니다:
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 📞 연락처

프로젝트 관련 문의사항이 있으시면 이슈를 생성해 주세요.

---

**Project Sentinel Team**  
**버전**: 1.0.0  
**최종 업데이트**: 2025-09-07