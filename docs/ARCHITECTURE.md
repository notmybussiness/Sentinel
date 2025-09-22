# Sentinel Architecture

## System Status ✅ MVP Complete
- **Frontend**: Next.js 14 + TypeScript (Production Ready)
- **Backend**: Spring Boot 3.5.5 + Java 17 (Production Ready)  
- **Structure**: Monorepo with domain separation
- **Local Dev**: Working with Docker support

## Domain Architecture
```
├── Frontend (Next.js 14)
│   ├── (auth)/login/
│   └── (dashboard)/
├── Backend (Spring Boot)
│   ├── user/        # Auth, JWT, Kakao OAuth2
│   ├── portfolio/   # CRUD, holdings, calculations  
│   ├── market/      # External APIs, fallback strategy
│   └── common/      # Security, config, error handling
```

## Key Features
- **JWT Authentication**: Kakao OAuth2 + 15min/7day tokens
- **Market Data**: AlphaVantage → Finnhub fallback
- **Portfolio**: Real-time calculations, auto-updates
- **Frontend**: Stripe-style UI, Framer Motion, Zustand state

## Next Phase: AWS Deployment
- **Cost**: ~$16/month (ALB only paid service)
- **Timeline**: 4 weeks
- **Tools**: Terraform, GitHub Actions, Gemini reviews