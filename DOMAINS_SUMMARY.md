# Sentinel Project - Domain Summary

## 🎨 **Frontend Domain (Next.js 14)**

### **Architecture & Organization**
```typescript
frontend/src/
├── app/                           # Next.js 14 App Router
│   ├── (auth)/login/             # Authentication pages
│   ├── (dashboard)/              # Protected dashboard pages
│   └── portfolios/[id]/rebalancing/ # NEW: Rebalancing interface
├── components/
│   ├── ui/                       # Radix UI + shadcn/ui primitives
│   ├── rebalancing/              # NEW: Rebalancing components
│   └── [domain-specific]/        # Feature-based components
└── store/                        # Zustand domain stores
```

### **Key Technologies**
- **Framework**: Next.js 14 App Router with TypeScript
- **Styling**: Tailwind CSS + Radix UI + shadcn/ui
- **State**: Zustand for domain-specific state management
- **Animation**: Framer Motion for smooth interactions
- **Testing**: Playwright E2E testing with cross-browser support

### **Major Components**
- **PortfolioCard**: Real-time portfolio overview with metrics
- **StockPrice**: Live price display with change indicators
- **RebalancingAnalysis**: NEW - Real-time deviation analysis
- **StrategySelector**: NEW - Algorithm selection interface
- **AllocationEditor**: NEW - Interactive allocation management

---

## 🔐 **Authentication Domain**

### **Security Architecture**
```java
JWT Implementation:
├── Access Token: 15 minutes (short-lived for security)
├── Refresh Token: 7 days (stored securely)
└── Kakao OAuth2: Social login integration
```

### **API Endpoints**
- `POST /api/v1/auth/kakao` - Initiate OAuth2 flow
- `POST /api/v1/auth/refresh` - Token refresh
- `POST /api/v1/auth/logout` - Session termination
- `GET /api/v1/auth/me` - User profile retrieval

### **Database Entities**
- **User**: `id`, `email`, `name`, `kakaoId`, `profileImageUrl`, `isActive`
- **UserSession**: `id`, `userId`, `accessTokenHash`, `refreshTokenHash`, `expiresAt`, `isActive`

### **Security Features**
- JWT token-based authentication with automatic refresh
- Kakao OAuth2 integration for social login
- User session management with secure token storage
- Request validation middleware for all protected endpoints

---

## 📊 **Market Data Domain**

### **Provider Strategy Pattern**
```java
MarketDataProviderFactory:
├── Primary: AlphaVantage (5 calls/min, high accuracy)
├── Fallback: Finnhub (60 calls/min, backup reliability)
└── Circuit Breaker: Automatic provider switching
```

### **API Endpoints**
- `GET /api/v1/market/price/{symbol}` - Single symbol price
- `POST /api/v1/market/prices` - Batch price retrieval
- **Rate Limiting**: Built-in API quota management
- **Caching**: 15-minute TTL to minimize external calls

### **Data Models**
```java
StockPriceDto {
    String symbol;           // Stock ticker symbol
    BigDecimal price;        // Current market price
    BigDecimal change;       // Price change amount
    BigDecimal changePercent; // Percentage change
    LocalDateTime timestamp; // Last update time
}
```

### **Features**
- Automatic provider failover for reliability
- Intelligent caching to respect API limits
- Real-time price updates with change tracking
- Batch processing for multiple symbols

---

## 💼 **Portfolio Domain**

### **Core Business Logic**
```java
Portfolio Calculations:
├── totalValue = Σ(quantity × currentPrice)     // Real-time valuation
├── totalCost = Σ(quantity × averageCost)       // Investment basis
├── gainLoss = totalValue - totalCost           // Profit/Loss
└── gainLossPercentage = (gainLoss ÷ totalCost) × 100
```

### **Database Entities**
- **Portfolio**: `id`, `userId`, `name`, `description`, financial metrics
- **PortfolioHolding**: `id`, `portfolioId`, `symbol`, `quantity`, `averageCost`, metrics

### **API Endpoints**
- `GET /api/v1/portfolios` - User portfolios list
- `POST /api/v1/portfolios` - Create new portfolio
- `PUT /api/v1/portfolios/{id}` - Update portfolio
- `DELETE /api/v1/portfolios/{id}` - Delete portfolio
- `POST /api/v1/portfolios/{id}/holdings` - Add holding
- `POST /api/v1/portfolios/{id}/recalculate` - Refresh calculations

### **NEW: Rebalancing Subsystem**
```java
Strategy Pattern Implementation:
├── RebalancingStrategy (interface)
├── ThresholdBasedRebalancingStrategy (5% deviation)
├── TimeBasedRebalancingStrategy (regular intervals)
└── HybridRebalancingStrategy (combined approach)
```

### **Rebalancing API**
- `POST /api/v1/portfolios/{id}/rebalancing/quick-analysis` - Deviation analysis
- `POST /api/v1/portfolios/{id}/rebalancing/recommendation` - Strategy recommendations
- `GET /api/v1/portfolios/{id}/rebalancing/strategies` - Available strategies

---

## ⚙️ **Common/Infrastructure Domain**

### **Configuration Components**
- **SecurityConfig**: JWT authentication, CORS, endpoint protection
- **RestTemplateConfig**: External API configuration with custom thread pools
- **GlobalExceptionHandler**: Centralized error handling with structured responses
- **JwtAuthenticationFilter**: Request interception for token validation

### **Cross-Cutting Concerns**
- **Logging**: Structured logging with request tracing
- **Caching**: Redis integration for production, simple cache for development
- **Validation**: Input validation with custom validators
- **Metrics**: Actuator endpoints for health monitoring

### **Development vs Production**
```yaml
Development (H2):
- In-memory database for fast iteration
- Security disabled for easier testing
- Debug logging enabled

Production (PostgreSQL):
- Persistent database with optimized queries
- Full JWT security enabled
- Performance monitoring active
```

---

## 🏗️ **Infrastructure Domain**

### **AWS Architecture (Cost-Optimized)**
```
Route53 → EC2 (Nginx) → {Frontend:3000, Backend:8080}
                     ↓
               {PostgreSQL RDS, Redis ElastiCache}
```

### **Terraform Modules**
- `vpc/` - Network foundation with public/private subnets
- `security/` - Security groups and access control
- `database/` - RDS PostgreSQL + ElastiCache Redis
- `compute/` - EC2 instances with auto-scaling capabilities

### **Cost Optimization**
- **95% cost reduction**: ALB removal, direct EC2 + Nginx
- **Monthly cost**: ~$16-20 (vs ~$22 ALB-based setup)
- **Performance**: Maintained with Nginx reverse proxy

### **CI/CD Pipeline**
- **GitHub Actions**: Automated testing and deployment
- **Branch Strategy**: feature → develop → main
- **Deployment**: Blue-green deployment with health checks
- **Monitoring**: CloudWatch integration with custom metrics

---

## 📋 **Current Domain Status**
✅ **Authentication**: Production-ready with Kakao OAuth2
✅ **Market Data**: Robust provider fallback system
✅ **Portfolio**: Complete with advanced rebalancing
✅ **Frontend**: Modern React interface with E2E testing
✅ **Infrastructure**: Cost-optimized AWS deployment ready