# Project Sentinel - Comprehensive Context Map

*Generated: 2025-09-09 | Version: 1.0.0 | Status: MVP Complete*

## 🎯 Project Overview

**Sentinel** - Data-driven investment dashboard enabling rational investment decisions based on data rather than emotion.

**Architecture**: Monorepo with Next.js 14 frontend + Spring Boot 3.5.5 backend
**Status**: Production-ready MVP with domain separation and comprehensive API structure

## 📊 Project Metrics

```yaml
codebase_size:
  backend_java_files: 53
  frontend_ts_files: 7
  total_config_files: 15+
  domains: 4 (user, portfolio, market, common)

git_status:
  branch: feature/resttemplate-async  
  working_tree: clean
  recent_commit: 2189ef0 (docs consolidation)
```

## 🏗️ Architecture & Structure

### Monorepo Layout
```
Sentinel/
├── frontend/           # Next.js 14 + TypeScript
├── backend/            # Spring Boot 3.5.5 + Java 17  
├── docs/              # Domain API references
├── infrastructure/    # Terraform configs
└── .github/          # CI/CD workflows
```

### Domain-Driven Design
```yaml
domains:
  user:      # Authentication, JWT, Kakao OAuth2, sessions
  portfolio: # CRUD, holdings, real-time calculations  
  market:    # External APIs, provider pattern, fallbacks
  common:    # Security, config, global exception handling
```

## 🛠️ Technology Stack

### Frontend Stack
```yaml
framework: Next.js 14 (App Router)
language: TypeScript
styling: Tailwind CSS + shadcn/ui
state: Zustand
animation: Framer Motion
ui_library: Radix UI
icons: Lucide React
```

### Backend Stack  
```yaml
framework: Spring Boot 3.5.5
language: Java 17
database: PostgreSQL (prod) / H2 (dev)
cache: Redis  
security: Spring Security + JWT
orm: Spring Data JPA
testing: JUnit 5 + TestContainers
```

### External Integrations
```yaml
market_data:
  primary: Alpha Vantage API (5 calls/min)
  fallback: Finnhub API (60 calls/min)
  
authentication:
  provider: Kakao OAuth2
  tokens: JWT (15min access / 7day refresh)
```

## 🔧 Dependencies Analysis

### Root Package Configuration
```json
{
  "name": "project-sentinel",
  "version": "1.0.0", 
  "workspaces": ["frontend"],
  "engines": {
    "node": ">=18.0.0",
    "npm": ">=8.0.0"
  }
}
```

### Frontend Dependencies
```yaml
core:
  - next: "^14.0.0"
  - react: "^18.0.0" 
  - typescript: "^5.0.0"

ui_components:
  - "@radix-ui/*": 20+ components
  - class-variance-authority: "^0.7.0"
  - framer-motion: "^10.16.5"
  - lucide-react: "^0.294.0"

state_management:
  - zustand: "^4.4.7"
  
data_visualization:
  - recharts: "^2.8.0"
```

### Backend Dependencies  
```yaml
spring_boot: "3.5.5"
java_version: "17"

core_dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-security
  - spring-boot-starter-oauth2-client
  - spring-boot-starter-webflux
  - spring-boot-starter-data-redis

security:
  - jjwt-api: "0.12.3" (JWT handling)
  
resilience:
  - resilience4j-spring-boot3: "2.2.0"
  - spring-retry
  
mapping:
  - mapstruct: "1.5.5.Final"

databases:
  - h2 (development)
  - postgresql (production)

testing:
  - testcontainers
  - spring-security-test
```

## ⚙️ Configuration & Environment

### Application Configuration
```yaml
server_port: 8080
spring_profiles: dev (default), prod

async_config:
  core_pool_size: 10
  max_pool_size: 50
  queue_capacity: 200

rest_template:
  connect_timeout: 10000ms
  read_timeout: 15000ms

jwt:
  access_token_expiration: 15min
  refresh_token_expiration: 7days
```

### Development Environment
```yaml
database: H2 (in-memory)
h2_console: /h2-console
cache: Simple (no Redis)
security: Disabled for development
logging: DEBUG level for all components
```

### Production Environment (Docker)
```yaml
database: PostgreSQL
cache: Redis  
reverse_proxy: Nginx (optional)
health_checks: Enabled
monitoring: Actuator endpoints
```

### Environment Variables
```env
# Authentication
KAKAO_CLIENT_ID=*
KAKAO_CLIENT_SECRET=*

# Market Data APIs  
ALPHA_VANTAGE_API_KEY=*
FINNHUB_API_KEY=*

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/sentinel
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=*

# Security
JWT_SECRET_KEY=*

# Application URLs
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

## 🚀 Build & Development

### Available Scripts
```bash
# Development
npm run dev              # Start both frontend + backend
npm run dev:backend      # Backend only (port 8080) 
npm run dev:frontend     # Frontend only (port 3000)

# Building
npm run build           # Build both
npm run build:backend   # Gradle build
npm run build:frontend  # Next.js build

# Testing
npm run test           # All tests
npm run test:backend   # Java tests
npm run test:frontend  # Frontend tests

# Docker
npm run docker:build   # Build containers
npm run docker:up      # Start services
npm run docker:down    # Stop services
```

### Development Workflow
1. **Setup**: `npm run setup` (installs all dependencies)
2. **Development**: `npm run dev` (concurrent execution)
3. **Testing**: `npm run test` (full test suite)
4. **Production**: `npm run docker:up` (containerized deployment)

## 📋 Key APIs & Endpoints

### Authentication APIs
```yaml
POST /api/v1/auth/kakao          # Kakao login start
GET  /api/v1/auth/kakao/callback # OAuth2 callback  
POST /api/v1/auth/refresh        # Token refresh
POST /api/v1/auth/logout         # Logout
GET  /api/v1/auth/me            # Current user info
```

### Market Data APIs
```yaml
GET /api/v1/market/price/{symbol}        # Single stock price
GET /api/v1/market/prices?symbols=A,B    # Multiple stocks  
GET /api/v1/market/status                # Provider health
```

### Portfolio APIs
```yaml
GET    /api/v1/portfolios?userId={id}                    # List portfolios
POST   /api/v1/portfolios?userId={id}                    # Create portfolio
GET    /api/v1/portfolios/{id}?userId={id}               # Portfolio details
PUT    /api/v1/portfolios/{id}?userId={id}               # Update portfolio
DELETE /api/v1/portfolios/{id}?userId={id}               # Delete portfolio
POST   /api/v1/portfolios/{id}/holdings?userId={id}      # Add holding
PUT    /api/v1/portfolios/{id}/holdings/{hId}?userId={id} # Update holding
DELETE /api/v1/portfolios/{id}/holdings/{hId}?userId={id} # Remove holding
POST   /api/v1/portfolios/{id}/recalculate?userId={id}   # Price refresh
```

## 🔒 Security & Architecture Patterns

### Security Model
```yaml
authentication: JWT with refresh tokens
authorization: User-specific data access (userId validation)
oauth2: Kakao integration for social login
endpoints: All /api/v1/* require JWT (except auth)
```

### Market Data Pattern
```yaml
pattern: Provider with fallback strategy
primary: AlphaVantage (5 calls/min, higher accuracy)
fallback: Finnhub (60 calls/min, backup)
factory: MarketDataProviderFactory manages selection
service: Automatic fallback on provider failure
cache: 15-minute TTL for API responses
```

### Business Logic Calculations
```yaml
portfolio_total_value: Σ(quantity × currentPrice)
portfolio_total_cost: Σ(quantity × averageCost)
gain_loss: totalValue - totalCost
gain_loss_percentage: (gainLoss ÷ totalCost) × 100
```

## 📁 Project File Organization

### Backend Architecture
```
backend/src/main/java/com/pjsent/sentinel/
├── common/           # Shared configs, exception handling
├── market/           # Market data domain  
├── portfolio/        # Portfolio management domain
└── user/            # Authentication domain
```

### Frontend Architecture  
```
frontend/src/
├── app/             # Next.js App Router pages
├── components/      # Reusable UI components
├── lib/            # Utility functions
├── hooks/          # Custom React hooks
├── store/          # Zustand state management
└── types/          # TypeScript definitions
```

## 🎯 Development Status & Next Steps

### Current Status: ✅ MVP Complete
- [x] Monorepo structure with domain separation
- [x] Spring Boot backend with comprehensive API structure  
- [x] Next.js frontend with modern stack
- [x] Docker containerization setup
- [x] CI/CD pipeline configuration
- [x] Comprehensive documentation structure

### Phase 2 (Next)
- [ ] Implement Kakao OAuth2 authentication
- [ ] Connect frontend to backend APIs
- [ ] Real-time market data streaming with SSE
- [ ] Redis caching implementation
- [ ] Production deployment automation

### Phase 3 (Future)
- [ ] Advanced analytics and backtesting
- [ ] AI-based investment recommendations  
- [ ] Mobile app development
- [ ] Multi-language support

---

*This context map provides comprehensive project understanding for development, onboarding, and decision-making. Last updated: 2025-09-09*