# 📋 Sentinel - Implementation Workflow & Decisions

## 🎯 핵심 결정사항

### **1️⃣ Dashboard Performance Charts**
**결정**: Portfolio Performance Over Time (개인 포트폴리오 수익률 추이)
**확장성**: 자산별 성과 비교 추후 추가

```typescript
// API 확장 구조
GET /api/v1/portfolios/{id}/performance
Query Parameters:
- view: 'portfolio' | 'assets' | 'comparison'
- timeframe: '1d' | '1w' | '1m' | '3m' | '6m' | '1y' | 'all'
- benchmark: '^GSPC' | '^IXIC' | 'custom'

Response:
{
  portfolio_performance: {
    timeSeries: [{ date, value, return_percent }],
    totalReturn: 0.125,
    benchmark_comparison: { outperformance: 0.032 }
  },
  asset_breakdown?: [  // when view=assets
    { symbol: "AAPL", performance: {...} }
  ]
}
```

### **2️⃣ Celebrity Portfolio Integration**
**결정**: MVP Manual Curation
**타겟 인물**: 널리 알려진 투자자 10-15명

```yaml
Celebrity Portfolio List:
Famous Investors:
  - Warren Buffett (Value Investing)
  - Ray Dalio (All Weather Strategy)
  - Peter Lynch (Growth at Reasonable Price)
  - Benjamin Graham (Deep Value)
  - Cathie Wood (ARK Innovation)
  - Joel Greenblatt (Magic Formula)
  - David Swensen (Endowment Model)
  - John Bogle (Index Fund Pioneer)

Korean Context:
  - 이채원 (피델리티)
  - 박현주 (미래에셋)
  - 하이투자증권 추천 포트폴리오
```

### **3️⃣ Popular Charts Implementation**
**로드맵**: C → D → A 순서

```yaml
Phase 1 (Week 3-4): External APIs
  - Alpha Vantage trending
  - Finnhub social sentiment
  - Implementation: /api/v1/market/trending

Phase 2 (Week 7-8): Hybrid Data
  - External + social engagement
  - Most viewed portfolios
  - Implementation: Internal analytics + External

Phase 3 (Post-Launch): User Holdings
  - Platform user portfolio aggregation
  - Privacy-respecting analytics
  - Implementation: Anonymized holdings data
```

### **4️⃣ Custom Rebalancing Strategy API**
**핵심 요구**: 언제든 커스텀 알고리즘 삽입 가능한 확장 구조

```typescript
// 확장 가능한 Strategy Framework
interface RebalancingStrategy {
  id: string;
  name: string;
  type: 'calendar' | 'threshold' | 'momentum' | 'mean_reversion' | 'custom';

  // 확장 가능한 알고리즘 정의
  algorithm: {
    engine: 'javascript' | 'formula' | 'external_api';
    code?: string;  // JavaScript algorithm code
    formula?: string;  // Mathematical formula
    api_endpoint?: string;  // External algorithm service
    parameters: Record<string, any>;
  };

  // 실행 조건
  triggers: {
    time_based?: { frequency: 'daily' | 'weekly' | 'monthly' };
    threshold_based?: { deviation_percent: number };
    custom_condition?: { logic: string };
  };

  // 제약 조건
  constraints: {
    max_allocation_per_asset?: number;
    min_cash_percentage?: number;
    blacklist_assets?: string[];
    sector_limits?: Record<string, number>;
  };
}

// API Endpoints
POST /api/v1/strategies/custom
PUT  /api/v1/strategies/{id}/algorithm
POST /api/v1/strategies/{id}/validate    // Algorithm validation
POST /api/v1/strategies/{id}/backtest    // Custom strategy backtesting
GET  /api/v1/strategies/templates        // Pre-built algorithm templates
```

### **5️⃣ Settings & Notifications Priority**

```typescript
// MVP Settings (Week 2)
interface MVPSettings {
  portfolio_privacy: 'public' | 'private' | 'followers_only';
  email_notifications: {
    rebalancing_alerts: boolean;
    portfolio_performance: boolean;
    market_updates: boolean;
  };
  display_preferences: {
    currency: 'USD' | 'KRW';
    timezone: string;
    date_format: 'US' | 'KR';
  };
}

// Phase 2 Advanced Settings (Week 8)
interface AdvancedSettings extends MVPSettings {
  auto_rebalancing: {
    enabled: boolean;
    strategy_id: string;
    max_deviation_percent: number;
    min_rebalance_interval_days: number;
  };

  custom_benchmarks: {
    primary: '^GSPC' | '^KS11' | 'custom';
    custom_benchmark?: {
      name: string;
      symbol: string;
      weights: Record<string, number>;
    };
  };
}
```

---

## 🚀 9-Week Implementation Roadmap

### **🗓️ Overview**
- **Total Duration**: 9 weeks
- **Team Size**: 1 Full-stack Developer
- **Total Effort**: ~375 hours
- **Technology Stack**: Next.js 14, Spring Boot 3.5.5, PostgreSQL, Redis
- **Deployment**: AWS Free Tier + Vercel

---

## **Phase 1: Foundation & Setup** (Week 1-2)

### **Week 1: Infrastructure & Design System**
```yaml
🏗️ Infrastructure Setup (20 hours):
- [ ] AWS account setup + free tier configuration
- [ ] PostgreSQL RDS setup (t3.micro)
- [ ] Redis cache configuration (ElastiCache t3.micro)
- [ ] CI/CD pipeline (GitHub Actions)
- [ ] Environment configuration (dev/prod)

🎨 Design System Foundation (20 hours):
- [ ] Tailwind CSS + shadcn/ui setup
- [ ] Dark theme configuration (therich.io inspired)
- [ ] Color palette implementation
- [ ] Typography scale & spacing system
- [ ] Reusable component base structure

🔧 Frontend Tooling:
- [ ] Next.js 14 project setup
- [ ] TypeScript configuration
- [ ] ESLint + Prettier setup
- [ ] Zustand store configuration
- [ ] React Query setup

Estimated Time: 40 hours
Dependencies: AWS account, domain name (optional)
Risks: AWS free tier limitations
```

### **Week 2: Authentication & API Foundation**
```yaml
🔐 Authentication System (25 hours):
- [ ] Kakao OAuth integration
- [ ] JWT token management
- [ ] Protected route implementation
- [ ] User session management
- [ ] User profile management

📡 API Infrastructure (20 hours):
- [ ] Spring Boot API endpoints (basic CRUD)
- [ ] Database schema design
- [ ] Data source integrations (Alpha Vantage, Finnhub)
- [ ] Caching layer implementation
- [ ] Error handling & logging

🧪 Testing Setup:
- [ ] Unit test configuration
- [ ] API integration tests
- [ ] Frontend testing utilities

Estimated Time: 45 hours
Parallel Streams: Backend (25h) + Frontend (20h)
Critical Path: OAuth setup, database schema
```

---

## **Phase 2: Core MVP Development** (Week 3-4)

### **Week 3: Dashboard & Market Data**
```yaml
📊 Market Overview Dashboard (30 hours):
- [ ] Market indices integration (S&P 500, NASDAQ, Crypto)
- [ ] Recharts implementation & configuration
- [ ] Real-time price polling system
- [ ] Fear & Greed Index integration
- [ ] Responsive grid layout

🔍 Asset Discovery (20 hours):
- [ ] Asset search functionality
- [ ] Trending assets API
- [ ] Market sentiment integration
- [ ] News feed implementation

Portfolio Performance Charts (Primary):
  - [ ] Portfolio value over time line charts
  - [ ] Benchmark comparison (S&P 500, 코스피)
  - [ ] Return metrics display (총 수익률, 연환산 수익률)
  - [ ] Expandable asset-level performance (Phase 2)

Popular Charts (External APIs):
  - [ ] Alpha Vantage trending integration
  - [ ] Finnhub social sentiment
  - [ ] Trending display with volume/price data

Celebrity Portfolios Data:
  - [ ] Static JSON data for 10-15 famous investors
  - [ ] Portfolio performance calculation
  - [ ] Copy portfolio functionality

Components Built:
- MarketIndexGrid
- FearGreedMeter
- TrendingAssets
- AssetSearchSelect

Estimated Time: 50 hours
Focus: Frontend-heavy with API integration
Dependencies: External API keys (Alpha Vantage, Finnhub)
```

### **Week 4: Portfolio Management Core**
```yaml
💼 Portfolio CRUD Operations (30 hours):
- [ ] Portfolio creation & management
- [ ] Asset allocation interface
- [ ] Portfolio performance tracking
- [ ] Transaction history

📈 Portfolio Visualization (25 hours):
- [ ] Portfolio allocation pie charts
- [ ] Performance line charts
- [ ] Asset breakdown tables
- [ ] Profit/loss calculations

🔄 Data Synchronization:
- [ ] Hourly price updates implementation
- [ ] Cache invalidation strategy
- [ ] Polling system with error handling

Components Built:
- PortfolioBuilder
- PortfolioCard
- AllocationPieChart
- PerformanceChart

Estimated Time: 55 hours
Parallel Streams: Portfolio Logic (30h) + UI Components (25h)
Critical Path: Portfolio calculation engine
```

---

## **Phase 3: Backtesting Laboratory** (Week 5-6)

### **Week 5: Backtesting Engine Core**
```yaml
🧪 Backtesting Infrastructure (40 hours):
- [ ] Historical data collection & storage
- [ ] Portfolio simulation engine
- [ ] Performance metrics calculation
- [ ] Risk analysis algorithms

📊 Basic Analytics (20 hours):
- [ ] Return calculations
- [ ] Volatility measurements
- [ ] Sharpe ratio implementation
- [ ] Maximum drawdown analysis

💾 Portfolio Save/Load System:
- [ ] Portfolio snapshot functionality
- [ ] Version history tracking
- [ ] Export/import capabilities

Backend Focus: Complex calculation engine
Estimated Time: 60 hours
Critical Path: Historical data pipeline
Dependencies: Historical price data accuracy
```

### **Week 6: Rebalancing Strategies**
```yaml
⚙️ Strategy Engine (30 hours):
- [ ] Rebalancing algorithm framework
- [ ] Calendar-based rebalancing
- [ ] Threshold-based rebalancing
- [ ] Custom strategy builder (basic)

Custom Strategy Framework:
  - [ ] Strategy template system
  - [ ] JavaScript algorithm executor
  - [ ] Formula-based calculation engine
  - [ ] Algorithm validation system
  - [ ] Custom strategy backtest integration

Pre-built Templates:
  - [ ] Calendar rebalancing
  - [ ] Threshold rebalancing
  - [ ] Momentum strategy template
  - [ ] Mean reversion template

📈 Strategy Comparison (20 hours):
- [ ] Multi-strategy backtesting
- [ ] Performance comparison charts
- [ ] Strategy optimization tools

🎛️ User Interface:
- [ ] Strategy configuration UI
- [ ] Backtest results visualization
- [ ] Strategy comparison dashboard

Components Built:
- StrategyBuilder
- BacktestResults
- StrategyComparison

Estimated Time: 50 hours
Parallel Streams: Algorithm Development (30h) + UI (20h)
Critical Path: Strategy calculation accuracy
```

---

## **Phase 4: Social Features & Polish** (Week 7-8)

### **Week 7: Celebrity Portfolios & Social Features**
```yaml
👥 Social Investment Platform (25 hours):
- [ ] Public portfolio showcase
- [ ] Celebrity portfolio integration
- [ ] Portfolio copying functionality
- [ ] Performance leaderboards

🔍 Discovery Features (20 hours):
- [ ] Portfolio search & filtering
- [ ] Popular portfolios ranking
- [ ] Social proof indicators
- [ ] Portfolio analytics for public viewing

🛡️ Privacy & Permissions:
- [ ] Portfolio privacy settings
- [ ] Share permission management
- [ ] Anonymous portfolio viewing

Estimated Time: 45 hours
Focus: Social features with privacy considerations
Dependencies: Celebrity portfolio data sources
```

### **Week 8: Advanced Analytics & Insights**
```yaml
📊 Market Insights (20 hours):
- [ ] Sector performance analysis
- [ ] Asset correlation calculations
- [ ] Market trend detection
- [ ] Risk attribution analysis

🧠 Smart Recommendations (20 hours):
- [ ] Portfolio optimization suggestions
- [ ] Rebalancing recommendations
- [ ] Risk adjustment alerts
- [ ] Performance improvement insights

Advanced Settings:
  - [ ] Auto-rebalancing configuration
  - [ ] Custom benchmark selection
  - [ ] Risk tolerance integration

Popular Charts Hybrid:
  - [ ] Internal analytics integration
  - [ ] Social engagement tracking
  - [ ] Most viewed portfolios

✨ UX Polish:
- [ ] Loading states & animations
- [ ] Error boundary implementation
- [ ] Accessibility improvements
- [ ] Performance optimization

Estimated Time: 40 hours
Focus: Analytics engine + UX refinement
Critical Path: Analytics accuracy
```

---

## **Phase 5: Deployment & Optimization** (Week 9)

### **Week 9: Production Deployment**
```yaml
🚀 Production Readiness (15 hours):
- [ ] AWS EC2 deployment configuration
- [ ] Database migration scripts
- [ ] Environment variable management
- [ ] SSL certificate setup

⚡ Performance Optimization (10 hours):
- [ ] Bundle size optimization
- [ ] Chart rendering performance
- [ ] API response time optimization
- [ ] Caching strategy refinement

🧪 Testing & QA (10 hours):
- [ ] End-to-end testing
- [ ] Load testing
- [ ] Security testing
- [ ] User acceptance testing

📊 Monitoring & Analytics:
- [ ] Application monitoring setup
- [ ] Error tracking (Sentry)
- [ ] Performance metrics
- [ ] User analytics

Estimated Time: 35 hours
Critical Path: Production deployment & monitoring
Dependencies: Domain name, SSL certificates
```

---

## 🎯 **Milestone & Quality Gates**

### **Week 2 Milestone: Foundation Complete**
```yaml
✅ Success Criteria:
- [ ] AWS infrastructure provisioned
- [ ] Authentication working end-to-end
- [ ] Database schema deployed
- [ ] Basic API endpoints functional
- [ ] Design system components ready

🚨 Risk Mitigation:
- OAuth setup issues → Use email/password fallback
- AWS costs exceeded → Monitor billing alerts
- API rate limits → Implement aggressive caching
```

### **Week 4 Milestone: MVP Core Ready**
```yaml
✅ Success Criteria:
- [ ] User can create and manage portfolios
- [ ] Market data displays correctly
- [ ] Portfolio performance calculations accurate
- [ ] Responsive design works on desktop
- [ ] Caching system functional

📊 Performance Targets:
- Dashboard load time: <3 seconds
- Chart render time: <1 second
- API response time: <500ms
```

### **Week 6 Milestone: Backtesting Functional**
```yaml
✅ Success Criteria:
- [ ] Historical backtesting produces accurate results
- [ ] Multiple rebalancing strategies implemented
- [ ] Portfolio comparisons working
- [ ] Results visualization complete
- [ ] Save/load functionality operational

🔍 Quality Validation:
- Backtest accuracy verified against known results
- Performance metrics mathematically correct
- Edge cases handled (market crashes, etc.)
```

### **Week 8 Milestone: Feature Complete**
```yaml
✅ Success Criteria:
- [ ] Social features functional
- [ ] Celebrity portfolios integrated
- [ ] Advanced analytics working
- [ ] Mobile-responsive (desktop-focused)
- [ ] Error handling robust

👥 User Testing:
- Internal testing with sample portfolios
- Performance testing under load
- Security testing completed
```

### **Week 9 Milestone: Production Ready**
```yaml
✅ Success Criteria:
- [ ] Application deployed to AWS
- [ ] SSL certificates configured
- [ ] Monitoring and alerting active
- [ ] Database backups configured
- [ ] Performance targets met

🚀 Go-Live Checklist:
- [ ] Domain name configured
- [ ] CDN setup (CloudFront)
- [ ] Error tracking active
- [ ] User analytics configured
- [ ] Backup and recovery tested
```

---

## 📊 Celebrity Portfolio 데이터 구조

```json
{
  "celebrity_portfolios": [
    {
      "id": "warren_buffett_style",
      "name": "Warren Buffett Style",
      "description": "가치투자의 대가, 장기 보유 중심",
      "category": "value_investing",
      "allocations": [
        { "symbol": "AAPL", "name": "Apple Inc", "percentage": 47.6, "shares_equivalent": 895000000 },
        { "symbol": "BAC", "name": "Bank of America", "percentage": 13.2, "shares_equivalent": 1032000000 },
        { "symbol": "CVX", "name": "Chevron", "percentage": 8.9, "shares_equivalent": 159000000 }
      ],
      "performance_metrics": {
        "return_1y": 0.089,
        "return_3y": 0.234,
        "volatility": 0.123,
        "sharpe_ratio": 0.72,
        "max_drawdown": -0.18
      },
      "investment_philosophy": "가치투자, 장기보유, 우량기업 중심",
      "last_updated": "2024-01-01",
      "followers": 15420,
      "verified": true,
      "copy_available": true
    }
  ]
}
```

---

**Created**: 2024-01-15
**Version**: 1.0
**Status**: Ready for Execution
**Next Action**: Begin Week 1 Infrastructure Setup