# Authentication Domain API Reference

## Core APIs

### POST /api/v1/auth/kakao
**Purpose**: Kakao OAuth2 로그인 시작  
**When**: 사용자가 로그인 버튼 클릭 시  
**Response**: Kakao 로그인 URL 리다이렉트

### GET /api/v1/auth/kakao/callback
**Purpose**: Kakao OAuth2 콜백 처리  
**When**: Kakao 로그인 완료 후 자동 호출  
**Response**: JWT 토큰 발급

### POST /api/v1/auth/refresh
**Purpose**: Access 토큰 갱신  
**When**: 토큰 만료 시 (15분마다)  
**Request**: `{"refreshToken": "..."}`  
**Response**: 새로운 Access 토큰

### POST /api/v1/auth/logout
**Purpose**: 로그아웃  
**When**: 사용자가 로그아웃 버튼 클릭 시  
**Action**: 세션 무효화

### GET /api/v1/auth/me
**Purpose**: 현재 사용자 정보 조회  
**When**: 페이지 로드 시 인증 상태 확인  
**Response**: UserDto (id, email, name, profileImageUrl)

## Key Components
- **JWT**: Access(15분) + Refresh(7일) 토큰
- **Entities**: User, UserSession
- **Security**: Spring Security + JWT 필터