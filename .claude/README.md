# 📚 .claude/ Folder Guide

> **.claude/ 폴더는 프로젝트 문서와 진행 기록을 관리하는 공간입니다**
>
> **첫 시작은 항상 [CLAUDE.md](./CLAUDE.md)에서!**

---

## 📁 Folder Structure

```
.claude/
├── CLAUDE.md              # ⭐ Entry Point - 모든 문서의 Navigation Hub
├── PLAN.md                # ⭐ 프로젝트 계획 + 진행 체크리스트
├── CURRENT_STATE.md       # 📊 현재 구현 상태 스냅샷
├── README.md              # 📖 이 파일 - 폴더 구조 설명
│
├── backend/               # 🔧 Backend Domain API 스펙
│   ├── API_AUTH.md        # 인증 API
│   ├── API_PORTFOLIO.md   # 포트폴리오 API
│   └── API_MARKET.md      # 시장 데이터 API
│
├── frontend/              # 🎨 Frontend 문서
│   ├── COMPONENTS.md      # 컴포넌트 라이브러리 (20+)
│   ├── THEME_DATA.md      # 디자인 시스템 (색상, 타이포그래피)
│   ├── MODULES.md         # 모듈 구조 (lib, contexts, types)
│   └── PAGES.md           # 페이지 라우팅 및 구조
│
├── progress/              # 📝 일일 작업 기록
│   └── YYYY-MM-DD-{feature-name}.md
│
└── archive/               # 📦 과거 기획 문서 보관
    ├── SENTINEL_PRD.md
    ├── 3WEEK_RAPID_DEPLOYMENT_PLAN.md
    ├── BACKEND_ENGINEERING_MINDSET.md
    ├── IMPLEMENTATION_DECISIONS.md
    └── API_SPECIFICATION_OLD.md
```

---

## 📖 문서 읽는 순서

### 🆕 처음 프로젝트 시작할 때
1. **[CLAUDE.md](./CLAUDE.md)** - 프로젝트 개요, 전체 구조
2. **[PLAN.md](./PLAN.md)** - 프로젝트 목표, 로드맵, Phase 확인
3. **[CURRENT_STATE.md](./CURRENT_STATE.md)** - 현재 구현 상태
4. 필요 시 `backend/` 또는 `frontend/` 문서 참조

### 🔄 매 세션 시작할 때
1. **[PLAN.md](./PLAN.md)** → "Immediate Next Steps" 확인
2. **[CURRENT_STATE.md](./CURRENT_STATE.md)** → 최신 구현 상태
3. **`progress/`** → 최근 작업 내용 확인
4. 작업 시작!

### 🔍 특정 작업할 때
**Backend 개발**:
- `backend/API_[domain].md` 참조

**Frontend 개발**:
- `frontend/COMPONENTS.md` → 사용 가능한 컴포넌트
- `frontend/THEME_DATA.md` → 디자인 가이드
- `frontend/PAGES.md` → 페이지 구조

---

## 🎯 각 문서의 목적

### Core Documents

#### **CLAUDE.md** (⭐ 가장 중요)
- **목적**: 전체 프로젝트 Navigation Hub
- **언제**: 프로젝트 처음 시작, 문서 찾을 때
- **내용**: Quick start, 문서 맵, 개발 워크플로우

#### **PLAN.md** (⭐ 매 세션 필수)
- **목적**: 프로젝트 계획 + 진행 체크리스트
- **언제**: 매 세션 시작/종료 시
- **내용**: 7단계 로드맵, 체크리스트, Next Steps
- **업데이트**: 매 세션 (체크박스 상태)

#### **CURRENT_STATE.md**
- **목적**: 현재 구현 상태 스냅샷
- **언제**: 현재 무엇이 완료/미완인지 확인
- **내용**: 완료 기능, 디자인 시스템, 환경 설정
- **업데이트**: 새 기능 추가 시

---

### Backend Documents (`backend/`)

#### **API_AUTH.md**
- 인증 관련 API 엔드포인트
- Kakao OAuth, JWT 토큰 관리
- Request/Response 예시

#### **API_PORTFOLIO.md**
- 포트폴리오 CRUD API
- Holdings 관리
- 가격 재계산 로직

#### **API_MARKET.md**
- 시장 데이터 API (🚧 작업 중)
- 가격 조회, 검색, 지수
- Provider 시스템 (AlphaVantage, Finnhub)

---

### Frontend Documents (`frontend/`)

#### **COMPONENTS.md**
- 20+ UI 컴포넌트 라이브러리
- 각 컴포넌트 Props, 사용법
- 개발 체크리스트

#### **THEME_DATA.md**
- 디자인 시스템 전체
- 색상, 타이포그래피, 간격
- Glassmorphism 가이드

#### **MODULES.md**
- 프론트엔드 모듈 구조
- lib, contexts, types 설명
- Import patterns

#### **PAGES.md**
- 페이지 라우팅 맵
- 각 페이지 상세 설명
- Protected route pattern

---

### Progress Files (`progress/`)

**파일명 규칙**: `YYYY-MM-DD-{feature-name}.md`

**목적**: 일일 작업 기록

**내용 구조**:
```markdown
# {기능명} - YYYY-MM-DD

## Summary
간단한 요약

## Changes Made
구체적인 변경 사항

## Files Modified
변경된 파일 목록

## Result
최종 결과
```

**업데이트**: 중요 작업 완료 시

---

### Archive (`archive/`)

**목적**: 과거 기획 문서 보관

**파일**:
- `SENTINEL_PRD.md`: 초기 PRD
- `3WEEK_RAPID_DEPLOYMENT_PLAN.md`: 배포 계획
- `BACKEND_ENGINEERING_MINDSET.md`: 백엔드 원칙
- `IMPLEMENTATION_DECISIONS.md`: 기술 결정
- `API_SPECIFICATION_OLD.md`: 구 API 스펙 (참고용)

**용도**: 참고용, 거의 읽지 않음

---

## 🔄 정리 주기

### 매 세션
- **PLAN.md**: 체크박스 상태 업데이트
- **CURRENT_STATE.md**: 새 기능 추가

### 주요 작업 완료 시
- **progress/**: 새 파일 생성
- **backend/API_*.md**: API 변경 시 업데이트
- **frontend/COMPONENTS.md**: 새 컴포넌트 추가 시

### 거의 안 함
- **CLAUDE.md**: 구조 대변경 시에만
- **README.md**: 폴더 구조 변경 시
- **THEME_DATA.md**: 디자인 시스템 변경 시

---

## 💡 문서 작성 가이드

### Progress 파일 작성
```markdown
# Feature Name - 2025-MM-DD

## Summary
What was accomplished in 2-3 sentences

## Changes Made
- Specific change 1
- Specific change 2
- Specific change 3

## Files Modified
- path/to/file1.tsx
- path/to/file2.ts

## Result
Final outcome and next steps
```

### 문서 업데이트 원칙
1. **정확성**: 코드와 문서는 항상 동기화
2. **간결성**: 핵심만 작성, 불필요한 설명 제거
3. **구조화**: 일관된 포맷 유지
4. **날짜**: 주요 업데이트 시 날짜 기록

---

## 🚨 주의사항

### 절대 커밋하지 말 것
- `.env.local` (Frontend 환경 변수)
- `application-secret.yml` (Backend 시크릿)
- 개인 API 키 정보

### Git에 포함할 것
- 모든 `.claude/` 문서들
- `progress/` 작업 기록
- `archive/` 참고 문서

---

## 📞 Quick Reference

### 문서 찾기
- **다음 할 일?** → PLAN.md
- **현재 상태?** → CURRENT_STATE.md
- **API 스펙?** → backend/API_*.md
- **컴포넌트?** → frontend/COMPONENTS.md
- **디자인?** → frontend/THEME_DATA.md

### 세션 워크플로우
```
시작: PLAN.md → CURRENT_STATE.md → progress/latest
작업: 코딩 + 테스트 + 문서 참조
종료: PLAN.md 업데이트 → CURRENT_STATE.md 업데이트 → Git commit
```

---

**Last Updated**: 2025-10-01
**Version**: 2.0 - Reorganized Structure
**Total Documents**: 15 (4 core + 3 backend + 4 frontend + archive)