# Sentinel System Architecture

## System Status ✅ MVP Complete
- **Frontend**: Next.js 14 + TypeScript (Production Ready)
- **Backend**: Spring Boot 3.5.5 + Java 17 (Production Ready)  
- **Structure**: Monorepo with domain separation
- **Local Dev**: Working with Docker support

## System Overview

### Technology Stack
```
Frontend: Next.js 14 + TypeScript + Tailwind CSS
Backend:  Spring Boot 3.5.5 + Java 17 + PostgreSQL
Auth:     JWT + Kakao OAuth2
Market:   AlphaVantage → Finnhub fallback
Infra:    Docker + AWS + Terraform
```

### Domain Architecture
```
├── Frontend (Next.js 14)
│   ├── (auth)/login/           # Authentication pages
│   └── (dashboard)/           # Main dashboard
├── Backend (Spring Boot)
│   ├── user/                  # Auth, JWT, Kakao OAuth2
│   ├── portfolio/            # CRUD, holdings, calculations  
│   ├── market/               # External APIs, fallback strategy
│   └── common/               # Security, config, error handling
```

## Backend Package Architecture

### Package Structure
```
com.pjsent.sentinel/
├── common/                   # 공통 설정 및 보안
│   ├── config/              # Security, JWT, RestTemplate, Async
│   ├── exception/           # Global exception handling
│   └── response/            # Standardized response format
├── user/                    # 사용자 도메인
│   ├── controller/          # REST endpoints
│   ├── dto/                 # Request/Response objects
│   ├── entity/              # User entity
│   ├── repository/          # Data access layer
│   └── service/             # Business logic
├── market/                  # 시장 데이터 도메인
│   ├── provider/            # Market data providers
│   ├── service/             # Market data service
│   └── dto/                 # Market data objects
└── portfolio/               # 포트폴리오 도메인
    ├── controller/          # Portfolio CRUD
    ├── dto/                 # Portfolio objects
    ├── entity/              # Portfolio, Holdings entities
    ├── repository/          # Portfolio data access
    └── service/             # Portfolio business logic
```

### Key Design Patterns
- **Provider Pattern**: Market data fallback strategy (AlphaVantage → Finnhub)
- **Repository Pattern**: JPA data access with Spring Data
- **Global Exception Handler**: Standardized error responses
- **JWT Security**: Stateless authentication with filter chain
- **Domain-Driven Design**: Clear separation of business domains

## Key Features

### Authentication System
- **JWT Authentication**: Access (15min) + Refresh (7day) tokens
- **Kakao OAuth2**: Social login integration
- **Security Filter**: JWT validation middleware
- **Session Management**: Stateless with token refresh

### Market Data Integration
- **Primary**: AlphaVantage API for stock prices
- **Fallback**: Finnhub API for reliability
- **Provider Pattern**: Seamless fallback mechanism
- **Caching**: Redis for price data optimization

### Portfolio Management
- **Real-time Calculations**: Automatic P&L updates
- **Holdings Tracking**: Buy/sell transactions
- **Performance Analytics**: Returns, percentages, charts
- **Auto-updates**: Market price integration

### Frontend Architecture
- **Stripe-style UI**: Modern dashboard design
- **Framer Motion**: Smooth animations
- **Zustand State**: Client-side state management
- **TypeScript**: Type safety throughout
- **Responsive Design**: Mobile-first approach

## Infrastructure & Deployment

### Current Setup
- **Development**: Docker Compose with hot reload
- **Database**: PostgreSQL with JPA/Hibernate
- **Cache**: Redis for session and market data
- **Proxy**: Nginx reverse proxy configuration

### AWS Deployment Strategy
- **Cost Target**: ~$16/month (optimized from $22 ALB to $0.75 EC2+Nginx)
- **Timeline**: 4 weeks deployment roadmap
- **Infrastructure**: Terraform for IaC
- **CI/CD**: GitHub Actions pipeline
- **Monitoring**: CloudWatch + application metrics

### Architecture Decisions

#### Cost Optimization
- **ALB Replacement**: EC2 + Nginx reverse proxy (95% cost reduction)
- **Instance**: t2.micro for MVP (free tier eligible)
- **Database**: RDS PostgreSQL db.t3.micro
- **Storage**: EBS GP2 minimal allocation

#### Security
- **Defense in Depth**: Multiple security layers
- **JWT Validation**: Stateless authentication
- **HTTPS**: SSL termination at Nginx
- **Database**: Private subnet with security groups

#### Scalability
- **Horizontal**: Auto Scaling Groups ready
- **Vertical**: Instance type upgrades planned
- **Database**: Read replicas for scaling reads
- **Cache**: Redis cluster for session management

## Quality Attributes

### Performance
- **Response Time**: <200ms API calls
- **Load Time**: <3s initial page load
- **Throughput**: 100+ concurrent users supported
- **Caching**: Multi-layer caching strategy

### Reliability
- **Uptime**: 99.9% target availability
- **Fallbacks**: Market data provider failover
- **Error Handling**: Graceful degradation
- **Monitoring**: Health checks and alerts

### Security
- **Authentication**: OAuth2 + JWT tokens
- **Authorization**: Role-based access control
- **Data Protection**: Encryption at rest and transit
- **Audit**: Comprehensive logging

### Maintainability
- **Domain Separation**: Clear module boundaries
- **Testing**: Unit + Integration test coverage
- **Documentation**: API docs + architecture guides
- **Code Quality**: Linting + automated reviews

## Next Phase Development

### Immediate Priorities
1. AWS infrastructure deployment
2. CI/CD pipeline activation
3. Production monitoring setup
4. Performance optimization

### Future Enhancements
- Real-time WebSocket price updates
- Advanced portfolio analytics
- Mobile application development
- Multi-user collaboration features