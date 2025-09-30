# 📋 Sentinel Project Plan & Progress

> **Purpose**: Track project goals, progress, and next steps for continuous development across sessions
>
> **Last Updated**: 2025-10-01
> **Current Phase**: Frontend Complete → Backend Integration

---

## 🎯 Project Vision

**Goal**: Create a comprehensive portfolio management platform with backtesting, rebalancing recommendations, and real-time market data.

**Target Users**: Individual investors who want data-driven investment strategies

**Core Value**: Simplify portfolio management through automation and intelligent recommendations

---

## 📊 Current Status Overview

### Completion Summary
- **Frontend**: ✅ 100% Complete
- **Backend**: 🟡 60% Complete (Auth + Portfolio CRUD done)
- **Integration**: 🔴 10% (Dev mode only)
- **Testing**: 🔴 5%
- **Deployment**: 🔴 0%

### What's Working Now
1. ✅ Complete frontend UI with 20+ components
2. ✅ Kakao OAuth authentication flow
3. ✅ Portfolio CRUD operations (backend)
4. ✅ JWT token management
5. ✅ Dev mode for frontend testing

### What's Missing
1. ❌ Market data API integration
2. ❌ Backtesting engine
3. ❌ Rebalancing algorithm
4. ❌ Frontend-Backend connection (except auth)
5. ❌ Real-time data updates

---

## 🗓️ Development Roadmap

### Phase 1: Foundation (✅ COMPLETE)
**Timeline**: Week 1-2
**Status**: 100% Complete (2025-09-30)

- [x] Backend architecture setup
- [x] Frontend Next.js 14 setup
- [x] Authentication system (Kakao OAuth + JWT)
- [x] UI component library (20+ components)
- [x] Design system (Glassmorphism dark theme)
- [x] Portfolio CRUD backend
- [x] Mock data for frontend development

**Completion Date**: 2025-10-01

---

### Phase 2: Market Data Integration (🚧 IN PROGRESS)
**Timeline**: Week 3
**Status**: 20% Complete
**Priority**: HIGH

#### Backend Tasks
- [ ] Complete `MarketDataService` implementation
  - [x] AlphaVantage provider
  - [x] Finnhub provider
  - [ ] Circuit breaker implementation
  - [ ] Redis caching layer
- [ ] Implement market data endpoints
  - [ ] `GET /api/v1/market/price/{symbol}`
  - [ ] `POST /api/v1/market/prices` (batch)
  - [ ] `GET /api/v1/market/indices`
  - [ ] `GET /api/v1/market/search`
- [ ] Add rate limiting
- [ ] Error handling and logging

#### Frontend Tasks
- [ ] Create market data API client (`lib/api/market.ts`)
- [ ] Replace mock data with API calls
  - [ ] Homepage market indices
  - [ ] Portfolio holdings real-time prices
  - [ ] Market page data
- [ ] Add loading states
- [ ] Add error handling UI

#### Testing
- [ ] Unit tests for providers
- [ ] Integration tests for endpoints
- [ ] Frontend E2E tests with real data

**Next Step**: Implement MarketDataController and test with Postman

---

### Phase 3: Portfolio Integration (🔴 NOT STARTED)
**Timeline**: Week 4
**Status**: 0% Complete
**Priority**: HIGH

#### Tasks
- [ ] Connect frontend portfolio pages to backend
  - [ ] List portfolios API integration
  - [ ] Portfolio detail API integration
  - [ ] Create portfolio functionality
  - [ ] Update portfolio functionality
  - [ ] Delete portfolio confirmation
- [ ] Holdings management integration
  - [ ] Add holding with symbol search
  - [ ] Update holding quantities
  - [ ] Delete holdings
  - [ ] Real-time value updates
- [ ] Auto-refresh portfolio values
  - [ ] Implement polling or WebSocket
  - [ ] Update UI without page refresh
- [ ] Error handling and validation

**Acceptance Criteria**:
- User can create and manage real portfolios
- Holdings reflect real-time market prices
- All CRUD operations work end-to-end

---

### Phase 4: Backtesting Engine (🔴 NOT STARTED)
**Timeline**: Week 5-6
**Status**: 0% Complete
**Priority**: MEDIUM

#### Backend Tasks
- [ ] Design backtesting data model
  - [ ] `Backtest` entity
  - [ ] `BacktestResult` entity
  - [ ] Strategy definitions
- [ ] Implement historical data fetching
  - [ ] AlphaVantage historical API
  - [ ] Data normalization and storage
- [ ] Create backtesting engine
  - [ ] Portfolio rebalancing simulation
  - [ ] Performance calculation
  - [ ] Risk metrics (Sharpe, Sortino, Max Drawdown)
- [ ] Implement backtesting endpoints
  - [ ] `POST /api/v1/backtest/run`
  - [ ] `GET /api/v1/backtest/results/{id}`
  - [ ] `GET /api/v1/backtest/history`

#### Frontend Tasks
- [ ] Complete lab page implementation
  - [ ] Configuration form validation
  - [ ] Strategy selection UI
  - [ ] Results visualization
- [ ] Create results components
  - [ ] Performance chart (Recharts/Chart.js)
  - [ ] Metrics table
  - [ ] Comparison with benchmarks
- [ ] Export results functionality

**Acceptance Criteria**:
- User can run backtests on portfolios
- Results show historical performance
- Multiple strategies can be compared

---

### Phase 5: Rebalancing Algorithm (🔴 NOT STARTED)
**Timeline**: Week 7
**Status**: 0% Complete
**Priority**: MEDIUM

#### Tasks
- [ ] Implement rebalancing strategies
  - [ ] Threshold-based (5% deviation)
  - [ ] Time-based (monthly/quarterly)
  - [ ] Hybrid approach
- [ ] Create recommendation engine
  - [ ] Calculate target vs current allocation
  - [ ] Generate BUY/SELL/HOLD recommendations
  - [ ] Optimize for tax efficiency
- [ ] Integrate with frontend
  - [ ] Connect RebalancingModal to API
  - [ ] Show real-time recommendations
  - [ ] One-click rebalancing execution (future)

**Acceptance Criteria**:
- Rebalancing recommendations are accurate
- UI clearly shows required actions
- Multiple strategies available

---

### Phase 6: Testing & Quality (🔴 NOT STARTED)
**Timeline**: Week 8
**Status**: 0% Complete
**Priority**: MEDIUM

#### Tasks
- [ ] Backend testing
  - [ ] Unit tests (>80% coverage)
  - [ ] Integration tests for all endpoints
  - [ ] Load testing (JMeter)
- [ ] Frontend testing
  - [ ] Component unit tests (Jest)
  - [ ] E2E tests (Playwright)
  - [ ] Cross-browser testing
- [ ] Performance optimization
  - [ ] API response time (<200ms)
  - [ ] Frontend load time (<3s)
  - [ ] Bundle size optimization
- [ ] Security audit
  - [ ] JWT security review
  - [ ] SQL injection prevention
  - [ ] XSS protection
  - [ ] CORS configuration

---

### Phase 7: Deployment (🔴 NOT STARTED)
**Timeline**: Week 9
**Status**: 0% Complete
**Priority**: LOW

#### Tasks
- [ ] Setup production environment
  - [ ] AWS account configuration
  - [ ] RDS PostgreSQL setup
  - [ ] ElastiCache Redis setup
  - [ ] EC2 or ECS configuration
- [ ] Configure CI/CD
  - [ ] GitHub Actions workflow
  - [ ] Automated testing
  - [ ] Automated deployment
- [ ] Domain and SSL
  - [ ] Purchase domain name
  - [ ] Configure DNS
  - [ ] SSL certificate (Let's Encrypt)
- [ ] Monitoring and logging
  - [ ] CloudWatch setup
  - [ ] Error tracking (Sentry)
  - [ ] Performance monitoring

**Target Date**: End of Week 9

---

## 🎯 Immediate Next Steps (This Week)

### Priority 1: Market Data Backend
1. Implement `MarketDataController.java`
2. Complete provider circuit breaker
3. Add Redis caching
4. Test with Postman/Swagger

### Priority 2: Market Data Frontend
1. Create `lib/api/market.ts`
2. Replace mock indices on homepage
3. Add loading states
4. Test real API integration

### Priority 3: Portfolio API Integration
1. Update `lib/api/portfolio.ts` with real endpoints
2. Connect portfolio list page
3. Test create/update/delete operations

---

## 📝 Session Checklist

**At Start of Each Session**:
1. Read PLAN.md to understand current phase
2. Read CURRENT_STATE.md for latest implementation
3. Check progress/ folder for recent changes
4. Review last session's incomplete tasks

**During Session**:
1. Update task status in this file as completed
2. Create progress file for significant changes
3. Update CURRENT_STATE.md with new features

**At End of Session**:
1. Mark completed tasks with [x]
2. Update "Last Updated" date
3. Write progress file if needed
4. Commit and push changes

---

## 🐛 Known Issues & Tech Debt

### High Priority
- [ ] Kakao OAuth redirect URI must be configured in Kakao Console
- [ ] No error boundary for frontend
- [ ] Missing loading states on data fetch

### Medium Priority
- [ ] Mock data should have consistent IDs
- [ ] Portfolio recalculation not automatic
- [ ] No offline mode

### Low Priority
- [ ] Component documentation could be more detailed
- [ ] No dark/light mode toggle (always dark)

---

## 💡 Future Enhancements (Post-MVP)

### Feature Ideas
- [ ] Multiple portfolio comparison
- [ ] AI-powered portfolio suggestions
- [ ] Tax loss harvesting recommendations
- [ ] Dividend tracking and projections
- [ ] News feed integration
- [ ] Mobile app (React Native)
- [ ] Social features (share portfolios)
- [ ] Cryptocurrency support

### Technical Improvements
- [ ] WebSocket for real-time updates
- [ ] GraphQL API option
- [ ] Server-side rendering optimization
- [ ] Progressive Web App (PWA)
- [ ] Multi-language support (i18n)

---

## 📞 Quick Decision Log

**Recent Decisions**:
1. **2025-10-01**: Organized `.claude/` folder into backend/ and frontend/ subfolders
2. **2025-09-30**: Applied glassmorphism design system to all components
3. **2025-09-30**: Chose carousel to show 3 items, slide 1 at a time
4. **2025-09-29**: Decided to use mock data for frontend development speed

**Pending Decisions**:
- Which charting library? (Chart.js vs Recharts vs D3.js)
- WebSocket vs polling for real-time updates?
- Deploy on AWS EC2 vs Vercel + Heroku?

---

## 🔄 Version History

### v1.0 - Frontend Foundation (2025-10-01)
- Complete frontend UI with 20+ components
- Kakao OAuth authentication
- Mock data for all pages
- Glassmorphism design system

### v0.5 - Backend Foundation (2025-09-28)
- Spring Boot 3.5.5 setup
- PostgreSQL + Redis integration
- User and Portfolio CRUD
- JWT authentication

---

**Remember**: This document is the source of truth for project planning. Update it regularly and reference it at the start of each session for continuity.

**Next Session Goal**: Complete market data backend implementation and connect to frontend homepage.