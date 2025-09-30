# 📊 Sentinel Financial Platform - Product Requirements Document (PRD)

## 🎯 Executive Summary

**Project**: Sentinel - Advanced Portfolio Management & Backtesting Platform
**Benchmark**: therich.io + Advanced Backtesting Laboratory
**Core Vision**: Create a professional-grade financial platform combining intuitive portfolio management with sophisticated backtesting and rebalancing strategies

**Key Differentiators**:
- 🧪 Advanced backtesting laboratory with custom rebalancing strategies
- 📈 Multi-asset portfolio management (stocks, crypto, commodities)
- 🎨 Clean dark-themed interface inspired by therich.io
- 👥 Celebrity/influencer portfolio sharing & insights
- ⚡ Real-time market data with intelligent caching

---

## 🏗️ Technical Architecture Overview

### **Frontend Stack**
- **Framework**: Next.js 14 (App Router)
- **Styling**: Tailwind CSS + shadcn/ui components
- **Charts**: Recharts (most widely used, beginner-friendly)
- **State**: Zustand + React Query (TanStack Query)
- **Theme**: Dark-first design system

### **Backend Stack**
- **API**: Spring Boot 3.5.5 (existing)
- **Database**: PostgreSQL + Redis (caching)
- **Data Sources**: Alpha Vantage, Finnhub, Gemini AI
- **Authentication**: OAuth 2.0 (Kakao) + JWT

### **Infrastructure**
- **Hosting**: AWS Free Tier (EC2 t2.micro, RDS t3.micro)
- **Frontend**: Vercel (free tier)
- **Environments**: dev (local) + prod (AWS)
- **Data Update**: Polling every hour, TTL 3600s caching

---

## 🎨 Design System & UI Components

### **Color Palette** (Dark Theme)
```scss
primary: {
  navy-900: '#0f172a',    // Main background
  navy-800: '#1e293b',    // Card backgrounds
  navy-700: '#334155',    // Borders
}
accent: {
  gold: '#f1b13c',        // Key actions (therich.io inspired)
  green: '#10b981',       // Gains
  red: '#ef4444',         // Losses
}
```

### **Reusable Component Library**
- 📊 **ChartContainer**: Wrapper for all chart components
- 💳 **PortfolioCard**: Portfolio display with performance metrics
- 📈 **StockTicker**: Real-time price display
- 🔍 **AssetSearchSelect**: Multi-asset search & selection
- ⚙️ **StrategyBuilder**: Drag-drop rebalancing strategy creator
- 📱 **DataTable**: Sortable, filterable table component

---

## 🏪 Feature Specifications

### **Phase 1: Core Platform (MVP)**

#### **1.1 Dashboard & Market Overview**
**Epic**: Landing Dashboard
```yaml
User Stories:
- As a user, I want to see market overview on landing page
- As a user, I want to track key indices (S&P 500, NASDAQ, Crypto)
- As a user, I want to see Fear & Greed Index
- As a user, I want to discover trending stocks/crypto

Components:
- MarketIndexGrid: Real-time index charts
- FearGreedMeter: CNN Fear & Greed Index widget
- TrendingAssets: Popular/trending assets
- MarketSentiment: News sentiment analysis
```

#### **1.2 Authentication & User Management**
**Epic**: User System
```yaml
Features:
- OAuth login (Kakao primarily)
- User profile management
- Portfolio ownership & privacy settings
- Basic notification preferences
```

#### **1.3 Portfolio Management**
**Epic**: Portfolio CRUD
```yaml
User Stories:
- As a user, I want to create multiple portfolios
- As a user, I want to add stocks/crypto to portfolios
- As a user, I want to track portfolio performance
- As a user, I want to set allocation percentages

Components:
- PortfolioBuilder: Asset selection & allocation
- PortfolioOverview: Performance dashboard
- AllocationPieChart: Visual allocation display
- TransactionHistory: Buy/sell tracking
```

### **Phase 2: Backtesting Laboratory**

#### **2.1 Basic Backtesting Engine**
**Epic**: Backtesting Core
```yaml
Priority Features:
1. Portfolio Saving & Loading
2. Historical Performance Simulation
3. Risk Analysis Metrics
4. Return Visualization

Advanced Features (Modular):
- Monte Carlo simulations
- Sharpe ratio calculations
- Maximum drawdown analysis
- Volatility metrics
```

#### **2.2 Rebalancing Strategies**
**Epic**: Strategy Engine
```yaml
Core Strategies:
- Calendar-based rebalancing (monthly/quarterly)
- Threshold-based rebalancing (deviation %)
- Momentum strategies
- Mean reversion strategies

Custom Strategy Builder:
- Drag-drop interface for strategy creation
- Conditional logic builder
- Backtest comparison tools
```

### **Phase 3: Social & Advanced Features**

#### **3.1 Celebrity Portfolio Sharing**
**Epic**: Social Investment
```yaml
Features:
- Public portfolio showcase
- Performance leaderboards
- Portfolio copying functionality
- Community insights & comments
```

#### **3.2 Market Insights & Analytics**
**Epic**: Advanced Analytics
```yaml
Features:
- Sector performance analysis
- Correlation analysis
- Risk attribution
- Market trend detection
```

---

## 🔌 API Priority & Structure

### **Priority 1: Dashboard APIs**
```typescript
// Market Overview
GET /api/v1/market/indices          // S&P 500, NASDAQ, crypto indices
GET /api/v1/market/fear-greed       // Fear & Greed Index
GET /api/v1/market/trending         // Trending assets
GET /api/v1/market/news            // Market news & sentiment

// Popular Charts Implementation Strategy:
- Aggregate portfolio holdings across users
- Track most-viewed assets
- Monitor social sentiment
- Combine with external trending APIs
```

### **Priority 2: Portfolio Lab APIs**
```typescript
// Portfolio Management
GET /api/v1/portfolios              // User portfolios
POST /api/v1/portfolios             // Create portfolio
PUT /api/v1/portfolios/{id}          // Update portfolio
DELETE /api/v1/portfolios/{id}       // Delete portfolio

// Backtesting Engine
POST /api/v1/backtest/run           // Execute backtesting
GET /api/v1/backtest/results/{id}    // Get backtest results
POST /api/v1/strategies      // Create rebalancing strategy
GET /api/v1/strategies              // List strategies

// My Portfolio (Rebalancing)
GET /api/v1/portfolios/:id/analysis     // Portfolio analysis
POST /api/v1/portfolios/:id/rebalance   // Execute rebalancing
GET /api/v1/portfolios/:id/suggestions  // Rebalancing suggestions
```

### **Priority 3: Market Insights APIs**
```typescript
GET /api/v1/insights/sectors        // Sector performance
GET /api/v1/insights/correlations   // Asset correlations
GET /api/v1/analytics/risk         // Risk metrics
```

### **Priority 4: Notifications & Settings**
```typescript
GET /api/v1/notifications          // User notifications
POST /api/v1/notifications/subscribe  // Subscribe to alerts
PUT /api/v1/settings/profile      // User preferences
```

---

## 📊 Chart Library Selection: **Recharts**

**Why Recharts?**
- ✅ Most popular React charting library (45k+ GitHub stars)
- ✅ Beginner-friendly with excellent documentation
- ✅ Highly customizable and extensible
- ✅ Built specifically for React (not wrapper)
- ✅ Great TypeScript support
- ✅ Perfect for financial charts (candlestick, line, area)

**Chart Components Needed**:
```typescript
// Market Overview Charts
- LineChart: Index performance over time
- AreaChart: Portfolio performance comparison
- PieChart: Portfolio allocation
- ComposedChart: Volume + price data
- Candlestick: Custom component for stock charts

// Backtesting Charts
- MultiLineChart: Strategy comparison
- ScatterPlot: Risk vs return analysis
- BarChart: Performance metrics
- Treemap: Sector allocation visualization
```

---

## 📊 Data Flow & Caching Strategy

### **Data Update Frequency**
```yaml
Real-time (Polling every 1 hour):
- Individual stock prices
- Crypto prices
- Portfolio valuations

Cached (TTL 3600s):
- Market indices
- News articles
- Sector data
- Historical data

Static/Daily Updates:
- Fear & Greed Index
- Economic indicators
- Company fundamentals
```

### **Polling → WebSocket Migration Path**
```typescript
// Phase 1: Polling Implementation
const usePolling = (url: string, interval: number) => {
  // Configurable polling with easy WebSocket migration
}

// Phase 2: WebSocket Layer (Drop-in replacement)
const useWebSocket = (url: string) => {
  // Same interface as polling hook
}

// Migration Strategy:
// 1. Abstract data fetching behind custom hooks
// 2. Implement polling first for simplicity
// 3. Add WebSocket layer that implements same interface
// 4. Switch via feature flag or environment variable
```

---

## 📋 Key Decision Points & Questions

### **🤔 Questions for Joint Decision-Making**

**1. Dashboard "Performance Charts" Clarification**
- You mentioned you're unsure about "performance charts" - I suggest these options:
  - **Option A**: Portfolio performance over time (your returns vs benchmarks)
  - **Option B**: Asset performance comparison charts
  - **Option C**: Market sector performance breakdown
  - **Recommended**: Option A (most valuable for users)

**2. Popular Charts Implementation Strategy**
```yaml
Options for determining "popular" assets:
A. User Portfolio Holdings: Track most-held assets across platform
B. Social Engagement: Most-viewed/shared portfolio assets
C. External APIs: Use trending from Alpha Vantage/social sentiment
D. Hybrid Approach: Combine internal + external data

Recommendation: Start with Option C (external APIs), add Option A later
```

**3. Celebrity Portfolio Integration Scope**
- **MVP Approach**: Manual curation of 10-15 famous investor portfolios
- **Advanced Approach**: API integration with public filing data (SEC 13F)
- **Social Approach**: Influencer partnership program
- **Question**: Which celebrities/investors should we target first?

**4. Settings & Notifications Priority**
```yaml
Essential Settings:
- Portfolio privacy (public/private)
- Email notifications (rebalancing alerts, performance updates)
- Display preferences (currency, time zone)

Advanced Settings (Phase 2):
- Risk tolerance settings
- Automatic rebalancing triggers
- Custom benchmark selection

Recommendation: Focus on privacy + basic notifications first
```

**5. Backtesting Strategy Complexity**
```yaml
Phase 1 (Essential):
- Portfolio saving/loading ✅
- Basic backtesting with historical returns ✅
- Simple rebalancing (calendar-based) ✅

Phase 2 (Advanced - Modular):
- Monte Carlo simulations
- Complex rebalancing algorithms
- Options strategy backtesting
- Sector rotation strategies

Question: Any specific rebalancing strategies you want prioritized?
```

---

## 💰 Cost & Performance Targets

### **AWS Free Tier Budget**
```yaml
Monthly Costs (Estimated):
- EC2 t2.micro: $0 (free tier)
- RDS t3.micro: $0 (free tier 12 months)
- Redis ElastiCache: ~$13/month (t3.micro)
- S3 Storage: ~$1-3/month
- Data Transfer: ~$1-2/month

Total: ~$15-18/month
```

### **Performance Targets**
```yaml
Load Times:
- Dashboard: <3 seconds
- Chart rendering: <1 second
- API responses: <500ms
- Portfolio calculations: <2 seconds

User Experience:
- Desktop-focused (no mobile optimization)
- Dark theme only
- No offline support
- Progressive Web App (optional)
```

---

**Created**: 2024-01-15
**Version**: 1.0
**Status**: Ready for Implementation