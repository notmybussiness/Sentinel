# 🔐 Authentication API Specification

## Base Information
- **Domain**: `/api/v1/auth`
- **Authentication**: JWT Bearer Token
- **Rate Limit**: 100 requests/hour

---

## Endpoints

### 1. Kakao OAuth Login
**Endpoint**: `POST /api/v1/auth/kakao`

**Request**:
```json
{
  "code": "authorization_code_from_kakao"
}
```

**Response**: `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "refresh_token_here",
  "user": {
    "id": 1,
    "email": "user@kakao.com",
    "name": "김투자",
    "nickname": "investor123",
    "profileImageUrl": "https://...",
    "provider": "KAKAO",
    "createdAt": "2025-01-15T10:30:00Z"
  }
}
```

**Errors**:
- `400 Bad Request`: Invalid authorization code
- `401 Unauthorized`: Kakao authentication failed
- `500 Internal Server Error`: Server error

---

### 2. Token Refresh
**Endpoint**: `POST /api/v1/auth/refresh`

**Headers**:
```
Authorization: Bearer {refresh_token}
```

**Response**: `200 OK`
```json
{
  "accessToken": "new_jwt_token_here",
  "expiresIn": 3600
}
```

**Errors**:
- `401 Unauthorized`: Invalid or expired refresh token
- `403 Forbidden`: Token revoked

---

### 3. Logout
**Endpoint**: `POST /api/v1/auth/logout`

**Headers**:
```
Authorization: Bearer {access_token}
```

**Response**: `200 OK`
```json
{
  "message": "Successfully logged out"
}
```

---

### 4. Get Current User
**Endpoint**: `GET /api/v1/auth/me`

**Headers**:
```
Authorization: Bearer {access_token}
```

**Response**: `200 OK`
```json
{
  "id": 1,
  "email": "user@kakao.com",
  "name": "김투자",
  "nickname": "investor123",
  "profileImageUrl": "https://...",
  "provider": "KAKAO",
  "createdAt": "2025-01-15T10:30:00Z"
}
```

**Errors**:
- `401 Unauthorized`: Invalid or expired token

---

## Implementation Status

### ✅ Implemented
- Kakao OAuth integration
- JWT token generation (15min access, 7days refresh)
- Token refresh mechanism
- User session management

### 🚧 Pending
- Email/password authentication
- Social login (Google, Naver)
- Two-factor authentication (2FA)
- Password reset flow

---

## Backend Implementation

**Controller**: `backend/src/main/java/com/pjsent/sentinel/user/controller/AuthController.java`

**Service**: `backend/src/main/java/com/pjsent/sentinel/user/service/AuthService.java`

**Entity**: `backend/src/main/java/com/pjsent/sentinel/user/entity/User.java`

**Security**: `backend/src/main/java/com/pjsent/sentinel/common/security/JwtAuthenticationFilter.java`

---

**Last Updated**: 2025-10-01