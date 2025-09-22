# Project Structure

## Backend Package Structure
```
com.pjsent.sentinel/
├── common/      # Security, config, error handling
├── user/        # Authentication, JWT, OAuth2
├── market/      # External APIs, fallback strategy
└── portfolio/   # CRUD, holdings, calculations
```

## Key Patterns
- **Provider Pattern**: Market data fallback strategy
- **Repository Pattern**: JPA data access
- **Global Exception Handler**: Standardized error responses
- **JWT Security**: Stateless authentication
