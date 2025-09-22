# Security Guidelines

## 🔐 보안 원칙

**절대 커밋하지 말 것**:
- API 키, 시크릿 키, 패스워드, 토큰
- 데이터베이스 연결 정보
- OAuth 클라이언트 시크릿
- SSL 인증서 및 개인키

**GitHub Secrets 사용 필수**:
```yaml
# Repository Settings → Secrets and variables → Actions
AWS_ACCESS_KEY_ID: "AKIA..."
AWS_SECRET_ACCESS_KEY: "..."
KAKAO_CLIENT_ID: "your-kakao-client-id"
KAKAO_CLIENT_SECRET: "your-kakao-client-secret"
ALPHA_VANTAGE_API_KEY: "your-alphavantage-key"
FINNHUB_API_KEY: "your-finnhub-key"
DB_PASSWORD: "SecurePassword123!"
```

## 📁 .gitignore 보호 패턴

### API 키 및 인증정보
```
**/api-keys.*
**/secret.*
**/*secret*
**/*key*
**/*password*
**/*token*
**/*credentials*
**/oauth.*
**/auth-config.*
```

### 인증서 및 키스토어
```
**/*.pem
**/*.p12
**/*.jks
**/*.keystore
```

### Terraform 민감 파일
```
*.tfvars
terraform.tfstate
terraform.tfstate.backup
.terraform/
```

### Claude Code 작업공간
```
.claude/
```

## 🛡️ 개발 시 보안 체크리스트

### 코드 작성 시
- [ ] 하드코딩된 키/패스워드 없음
- [ ] 환경변수 또는 GitHub Secrets 사용
- [ ] 로그에 민감정보 출력 없음
- [ ] 테스트 파일에 실제 키 사용 없음

### 커밋 전
- [ ] `git diff` 로 민감정보 확인
- [ ] `.gitignore` 패턴 동작 확인
- [ ] 설정파일에 예시값만 포함

### PR 생성 시
- [ ] 민감정보 노출 여부 재확인
- [ ] GitHub Secrets 업데이트 필요성 확인

## 🚨 사고 대응

### 민감정보 커밋된 경우
1. 즉시 키 무효화 (API 제공업체에서)
2. Git 히스토리 정리: `git filter-branch` 또는 BFG Repo-Cleaner
3. 새로운 키 생성 및 GitHub Secrets 업데이트
4. 팀원들에게 `git pull --force` 공지

### 정기 보안 점검
- 월 1회 GitHub Secrets 로테이션
- 분기 1회 `.gitignore` 패턴 점검
- API 키 사용량 및 접근 로그 모니터링

## 💡 개발팀 협업 규칙

### 새 팀원 온보딩
1. 개인 API 키 발급 및 GitHub Secrets 등록
2. 보안 가이드라인 교육
3. 개발환경 `.env.example` 파일 제공

### 코드 리뷰 필수사항
- 민감정보 하드코딩 확인
- 환경변수 사용 패턴 점검
- 로그 출력 내용 검토

**기억사항**: 모든 중요한 키값은 GitHub Secrets으로 관리하고, 절대 코드에 하드코딩하지 않습니다.