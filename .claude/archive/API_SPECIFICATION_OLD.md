# 🔌 Sentinel API Specification

## 📋 Overview

**Base URL**: `https://api.sentinel.com`
**Authentication**: Bearer JWT Token
**Content-Type**: `application/json`
**Rate Limiting**: 1000 requests/hour per user

---

## 🔐 Authentication

### **OAuth Login**
```http
POST /api/v1/auth/oauth/kakao
Content-Type: application/json

{
  "code": "authorization_code",
  "redirect_uri": "https://sentinel.com/auth/callback"
}

Response: 200 OK
{
  "access_token": "jwt_token_here",
  "refresh_token": "refresh_token_here",
  "expires_in": 3600,
  "user": {
    "id": 1,
    "name": "김투자",
    "email": "invest@kakao.com",
    "created_at": "2024-01-15T10:30:00Z"
  }
}
```

### **Token Refresh**
```http
POST /api/v1/auth/refresh
Authorization: Bearer {refresh_token}

Response: 200 OK
{
  "access_token": "new_jwt_token",
  "expires_in": 3600
}
```

---

## 📊 Priority 1: Dashboard APIs

### **Market Overview**
```http
GET /api/v1/dashboard/overview
Authorization: Bearer {token}
Cache-Control: max-age=300

Response: 200 OK
{
  "market_indices": [
    {
      "symbol": "^GSPC",
      "name": "S&P 500",
      "price": 4150.5,
      "change": 12.3,
      "change_percent": 0.29,
      "last_updated": "2024-01-15T16:00:00Z"
    },
    {
      "symbol": "^IXIC",
      "name": "NASDAQ",
      "price": 12800.1,
      "change": -45.2,
      "change_percent": -0.35,
      "last_updated": "2024-01-15T16:00:00Z"
    }
  ],
  "fear_greed_index": {
    "value": 52,
    "classification": "Neutral",
    "last_updated": "2024-01-15"
  },
  "trending_assets": [
    {
      "symbol": "TSLA",
      "name": "Tesla Inc",
      "price": 205.3,
      "change_percent": 2.1,
      "volume": 45000000,
      "market_cap": 652000000000
    }
  ],
  "user_portfolio_summary": {
    "total_value": 125000.50,
    "total_return": 0.089,
    "today_change": 245.80,
    "portfolio_count": 3
  }
}
```

### **Market Indices Detail**
```http
GET /api/v1/market/indices
Query Parameters:
- symbols: ^GSPC,^IXIC,^DJI (optional, default: major indices)
- timeframe: 1d,1w,1m,3m,1y (optional, default: 1d)

Response: 200 OK
{
  "indices": [
    {
      "symbol": "^GSPC",
      "name": "S&P 500",
      "current_price": 4150.5,
      "change": 12.3,
      "change_percent": 0.29,
      "volume": 3200000000,
      "market_cap": 45000000000000,
      "historical_data": [
        {
          "date": "2024-01-15",
          "open": 4140.2,
          "high": 4155.8,
          "low": 4135.1,
          "close": 4150.5,
          "volume": 3200000000
        }
      ]
    }
  ],
  "last_updated": "2024-01-15T16:00:00Z"
}
```

### **Fear & Greed Index**
```http
GET /api/v1/market/fear-greed
Cache-Control: max-age=86400

Response: 200 OK
{
  "current": {
    "value": 52,
    "classification": "Neutral",
    "last_updated": "2024-01-15"
  },
  "historical": [
    {
      "date": "2024-01-14",
      "value": 48,
      "classification": "Fear"
    },
    {
      "date": "2024-01-13",
      "value": 55,
      "classification": "Neutral"
    }
  ]
}
```

### **Trending Assets**
```http
GET /api/v1/market/trending
Query Parameters:
- category: stocks,crypto,all (default: all)
- limit: number (default: 10, max: 50)
- timeframe: 1d,1w,1m (default: 1d)

Response: 200 OK
{
  "trending": [
    {
      "symbol": "TSLA",
      "name": "Tesla Inc",
      "price": 205.3,
      "change": 4.2,
      "change_percent": 2.1,
      "volume": 45000000,
      "market_cap": 652000000000,
      "trending_score": 95.5,
      "social_mentions": 12547
    }
  ],
  "methodology": "Combined social sentiment, volume spike, price movement",
  "last_updated": "2024-01-15T16:00:00Z"
}
```

---

## 💼 Priority 2: Portfolio Lab APIs

### **Portfolio Management**

#### **List User Portfolios**
```http
GET /api/v1/portfolios
Authorization: Bearer {token}

Response: 200 OK
{
  "portfolios": [
    {
      "id": 1,
      "name": "Growth Portfolio",
      "description": "High growth tech stocks",
      "total_value": 50000.0,
      "total_return": 0.12,
      "total_return_amount": 5357.14,
      "day_change": 125.50,
      "day_change_percent": 0.0025,
      "asset_count": 8,
      "created_at": "2024-01-01T00:00:00Z",
      "updated_at": "2024-01-15T10:30:00Z",
      "is_public": false
    }
  ],
  "total_count": 2,
  "total_portfolio_value": 125000.50
}
```

#### **Create Portfolio**
```http
POST /api/v1/portfolios
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Tech Growth Portfolio",
  "description": "Focus on high-growth technology companies",
  "is_public": false,
  "initial_cash": 10000.0
}

Response: 201 Created
{
  "id": 3,
  "name": "Tech Growth Portfolio",
  "description": "Focus on high-growth technology companies",
  "total_value": 10000.0,
  "cash_balance": 10000.0,
  "is_public": false,
  "created_at": "2024-01-15T10:45:00Z"
}
```

#### **Get Portfolio Details**
```http
GET /api/v1/portfolios/{id}
Authorization: Bearer {token}

Response: 200 OK
{
  "id": 1,
  "name": "Growth Portfolio",
  "description": "High growth tech stocks",
  "total_value": 50000.0,
  "cash_balance": 2500.0,
  "invested_amount": 47500.0,
  "total_return": 0.12,
  "total_return_amount": 5357.14,
  "allocations": [
    {
      "symbol": "AAPL",
      "name": "Apple Inc",
      "shares": 50.0,
      "current_price": 185.2,
      "current_value": 9260.0,
      "allocation_percent": 18.52,
      "total_return": 0.089,
      "total_return_amount": 756.0,
      "day_change": 15.30,
      "day_change_percent": 0.0083
    }
  ],
  "performance_history": [
    {
      "date": "2024-01-15",
      "total_value": 50000.0,
      "return_percent": 0.12
    }
  ],
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-15T10:30:00Z"
}
```

#### **Update Portfolio**
```http
PUT /api/v1/portfolios/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Updated Portfolio Name",
  "description": "Updated description",
  "allocations": [
    {
      "symbol": "AAPL",
      "action": "buy",
      "shares": 10.0
    },
    {
      "symbol": "MSFT",
      "action": "sell",
      "shares": 5.0
    }
  ]
}

Response: 200 OK
{
  "id": 1,
  "transactions": [
    {
      "symbol": "AAPL",
      "action": "buy",
      "shares": 10.0,
      "price": 185.2,
      "total_cost": 1852.0,
      "timestamp": "2024-01-15T10:45:00Z"
    }
  ],
  "new_total_value": 51852.0,
  "cash_balance": 648.0
}
```

### **Backtesting Engine**

#### **Run Backtest**
```http
POST /api/v1/backtest/run
Authorization: Bearer {token}
Content-Type: application/json

{
  "portfolio_id": 1,
  "start_date": "2020-01-01",
  "end_date": "2024-01-01",
  "initial_investment": 10000.0,
  "strategy": {
    "type": "rebalance",
    "frequency": "monthly",
    "threshold": 0.05
  },
  "benchmark": "^GSPC"
}

Response: 202 Accepted
{
  "backtest_id": "bt_abc123def456",
  "status": "running",
  "estimated_completion": "2024-01-15T10:50:00Z"
}
```

#### **Get Backtest Results**
```http
GET /api/v1/backtest/results/{backtest_id}
Authorization: Bearer {token}

Response: 200 OK
{
  "backtest_id": "bt_abc123def456",
  "status": "completed",
  "portfolio_id": 1,
  "parameters": {
    "start_date": "2020-01-01",
    "end_date": "2024-01-01",
    "initial_investment": 10000.0,
    "strategy": {
      "type": "rebalance",
      "frequency": "monthly"
    }
  },
  "results": {
    "portfolio_performance": {
      "final_value": 14567.89,
      "total_return": 0.4568,
      "annualized_return": 0.0987,
      "volatility": 0.156,
      "sharpe_ratio": 0.63,
      "max_drawdown": -0.23,
      "max_drawdown_date": "2020-03-23"
    },
    "benchmark_performance": {
      "final_value": 13245.67,
      "total_return": 0.3246,
      "annualized_return": 0.0732
    },
    "outperformance": {
      "absolute": 0.1322,
      "annualized": 0.0255
    },
    "time_series": [
      {
        "date": "2020-01-01",
        "portfolio_value": 10000.0,
        "benchmark_value": 10000.0,
        "portfolio_return": 0.0,
        "benchmark_return": 0.0
      }
    ],
    "monthly_returns": [
      {
        "month": "2020-01",
        "portfolio_return": 0.045,
        "benchmark_return": 0.023
      }
    ]
  },
  "completed_at": "2024-01-15T10:48:32Z"
}
```

### **Rebalancing Strategies**

#### **Create Strategy**
```http
POST /api/v1/strategies
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Conservative Growth",
  "description": "Monthly rebalancing with 5% threshold",
  "type": "threshold_rebalance",
  "parameters": {
    "frequency": "monthly",
    "threshold": 0.05,
    "target_allocations": {
      "AAPL": 0.25,
      "MSFT": 0.25,
      "GOOGL": 0.20,
      "AMZN": 0.20,
      "CASH": 0.10
    }
  },
  "is_public": false
}

Response: 201 Created
{
  "id": 1,
  "name": "Conservative Growth",
  "type": "threshold_rebalance",
  "created_at": "2024-01-15T11:00:00Z"
}
```

#### **Apply Strategy to Portfolio**
```http
POST /api/v1/portfolios/{portfolio_id}/rebalance
Authorization: Bearer {token}
Content-Type: application/json

{
  "strategy_id": 1,
  "dry_run": true
}

Response: 200 OK
{
  "rebalance_id": "rb_xyz789abc123",
  "current_allocations": {
    "AAPL": 0.32,
    "MSFT": 0.28,
    "GOOGL": 0.18,
    "AMZN": 0.15,
    "CASH": 0.07
  },
  "target_allocations": {
    "AAPL": 0.25,
    "MSFT": 0.25,
    "GOOGL": 0.20,
    "AMZN": 0.20,
    "CASH": 0.10
  },
  "required_transactions": [
    {
      "symbol": "AAPL",
      "action": "sell",
      "shares": 15.2,
      "estimated_value": 2816.04
    },
    {
      "symbol": "MSFT",
      "action": "sell",
      "shares": 8.1,
      "estimated_value": 2430.30
    },
    {
      "symbol": "GOOGL",
      "action": "buy",
      "shares": 6.7,
      "estimated_value": 1005.50
    }
  ],
  "estimated_costs": {
    "transaction_fees": 9.99,
    "total_impact": 0.0012
  },
  "dry_run": true
}
```

---

## 📈 Priority 3: Market Insights APIs

### **Sector Analysis**
```http
GET /api/v1/insights/sectors
Authorization: Bearer {token}
Query Parameters:
- timeframe: 1d,1w,1m,3m,1y (default: 1m)

Response: 200 OK
{
  "sectors": [
    {
      "name": "Technology",
      "return_1d": 0.012,
      "return_1w": 0.045,
      "return_1m": 0.089,
      "return_3m": 0.156,
      "return_1y": 0.234,
      "market_cap": 12500000000000,
      "top_performers": [
        {
          "symbol": "AAPL",
          "return_1m": 0.125
        }
      ]
    }
  ],
  "last_updated": "2024-01-15T16:00:00Z"
}
```

### **Asset Correlations**
```http
GET /api/v1/insights/correlations
Authorization: Bearer {token}
Query Parameters:
- symbols: AAPL,MSFT,GOOGL (required)
- period: 1m,3m,6m,1y (default: 3m)

Response: 200 OK
{
  "correlation_matrix": {
    "AAPL": {
      "MSFT": 0.72,
      "GOOGL": 0.68
    },
    "MSFT": {
      "AAPL": 0.72,
      "GOOGL": 0.81
    },
    "GOOGL": {
      "AAPL": 0.68,
      "MSFT": 0.81
    }
  },
  "analysis": {
    "highest_correlation": {
      "pair": ["MSFT", "GOOGL"],
      "value": 0.81
    },
    "lowest_correlation": {
      "pair": ["AAPL", "GOOGL"],
      "value": 0.68
    }
  },
  "period": "3m",
  "calculated_at": "2024-01-15T16:00:00Z"
}
```

---

## 🔔 Priority 4: Notifications & Settings APIs

### **Notifications**
```http
GET /api/v1/notifications
Authorization: Bearer {token}
Query Parameters:
- limit: number (default: 20, max: 100)
- unread_only: boolean (default: false)
- type: portfolio_alert,market_update,rebalance_suggestion

Response: 200 OK
{
  "notifications": [
    {
      "id": 1,
      "type": "portfolio_alert",
      "title": "Portfolio Rebalancing Needed",
      "message": "Your Growth Portfolio has drifted 5.2% from target allocation",
      "data": {
        "portfolio_id": 1,
        "drift_percentage": 0.052
      },
      "read": false,
      "created_at": "2024-01-15T10:30:00Z"
    }
  ],
  "unread_count": 3,
  "total_count": 25
}
```

### **User Settings**
```http
GET /api/v1/settings/profile
Authorization: Bearer {token}

Response: 200 OK
{
  "user_preferences": {
    "display_currency": "USD",
    "timezone": "America/New_York",
    "risk_tolerance": "moderate",
    "email_notifications": {
      "portfolio_alerts": true,
      "market_updates": false,
      "weekly_summary": true
    },
    "auto_rebalance": {
      "enabled": true,
      "threshold": 0.05,
      "frequency": "monthly"
    }
  }
}

PUT /api/v1/settings/profile
Authorization: Bearer {token}
Content-Type: application/json

{
  "display_currency": "KRW",
  "email_notifications": {
    "portfolio_alerts": false,
    "market_updates": true
  }
}

Response: 200 OK
{
  "message": "Settings updated successfully",
  "updated_at": "2024-01-15T11:15:00Z"
}
```

---

## 👥 Social Features APIs

### **Public Portfolios**
```http
GET /api/v1/portfolios/public
Query Parameters:
- category: celebrity,influencer,top_performer,all (default: all)
- sort: performance,followers,created_date (default: performance)
- limit: number (default: 20, max: 50)
- timeframe: 1m,3m,6m,1y (default: 1y)

Response: 200 OK
{
  "portfolios": [
    {
      "id": "warren_buffett_style",
      "name": "Warren Buffett Style Portfolio",
      "owner": {
        "type": "celebrity",
        "name": "Warren Buffett Style",
        "verified": true
      },
      "description": "Value investing with long-term focus",
      "performance": {
        "return_1y": 0.089,
        "return_3y": 0.234,
        "volatility": 0.123,
        "sharpe_ratio": 0.72
      },
      "allocations": [
        {
          "symbol": "AAPL",
          "percentage": 45.2,
          "name": "Apple Inc"
        }
      ],
      "followers": 15420,
      "last_updated": "2024-01-10T00:00:00Z",
      "can_copy": true
    }
  ]
}
```

### **Copy Portfolio**
```http
POST /api/v1/portfolios/{public_portfolio_id}/copy
Authorization: Bearer {token}
Content-Type: application/json

{
  "new_name": "My Buffett Style Portfolio",
  "copy_allocations": true,
  "initial_investment": 10000.0
}

Response: 201 Created
{
  "new_portfolio_id": 5,
  "name": "My Buffett Style Portfolio",
  "copied_allocations": 8,
  "initial_value": 10000.0,
  "created_at": "2024-01-15T11:30:00Z"
}
```

---

## 🔍 Search & Discovery APIs

### **Asset Search**
```http
GET /api/v1/search/assets
Query Parameters:
- q: search query (required)
- type: stock,crypto,etf,all (default: all)
- limit: number (default: 10, max: 50)

Response: 200 OK
{
  "results": [
    {
      "symbol": "AAPL",
      "name": "Apple Inc",
      "type": "stock",
      "exchange": "NASDAQ",
      "currency": "USD",
      "current_price": 185.2,
      "market_cap": 2890000000000,
      "description": "Technology company"
    }
  ],
  "total_count": 1,
  "query": "apple"
}
```

---

## 📊 Data Models

### **Portfolio Model**
```typescript
interface Portfolio {
  id: number;
  name: string;
  description?: string;
  user_id: number;
  total_value: number;
  cash_balance: number;
  invested_amount: number;
  total_return: number;
  total_return_amount: number;
  day_change: number;
  day_change_percent: number;
  allocations: Allocation[];
  is_public: boolean;
  created_at: string;
  updated_at: string;
}

interface Allocation {
  symbol: string;
  name: string;
  shares: number;
  current_price: number;
  current_value: number;
  allocation_percent: number;
  total_return: number;
  total_return_amount: number;
  day_change: number;
  day_change_percent: number;
}
```

### **Backtest Model**
```typescript
interface BacktestResult {
  backtest_id: string;
  status: 'running' | 'completed' | 'failed';
  portfolio_id: number;
  parameters: BacktestParameters;
  results?: BacktestResults;
  error?: string;
  created_at: string;
  completed_at?: string;
}

interface BacktestResults {
  portfolio_performance: PerformanceMetrics;
  benchmark_performance: PerformanceMetrics;
  outperformance: OutperformanceMetrics;
  time_series: TimeSeriesPoint[];
  monthly_returns: MonthlyReturn[];
}
```

---

## ⚠️ Error Handling

### **Standard Error Response**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "error": {
    "code": "INVALID_PORTFOLIO_ALLOCATION",
    "message": "Portfolio allocation percentages must sum to 100%",
    "details": {
      "current_sum": 95.5,
      "expected_sum": 100.0
    },
    "timestamp": "2024-01-15T11:45:00Z",
    "request_id": "req_abc123def456"
  }
}
```

### **Common Error Codes**
```yaml
Authentication Errors:
- INVALID_TOKEN: JWT token is invalid or expired
- INSUFFICIENT_PERMISSIONS: User lacks required permissions
- RATE_LIMIT_EXCEEDED: Too many requests from user

Portfolio Errors:
- PORTFOLIO_NOT_FOUND: Portfolio does not exist
- INVALID_ALLOCATION: Portfolio allocation is invalid
- INSUFFICIENT_FUNDS: Not enough cash for transaction

Market Data Errors:
- SYMBOL_NOT_FOUND: Asset symbol not recognized
- DATA_UNAVAILABLE: Market data temporarily unavailable
- EXTERNAL_API_ERROR: External data provider error

Backtesting Errors:
- INVALID_DATE_RANGE: Start date must be before end date
- INSUFFICIENT_HISTORICAL_DATA: Not enough data for backtest
- BACKTEST_IN_PROGRESS: Cannot start new backtest while one is running
```

---

## 📈 Performance & Caching

### **Cache Headers**
```yaml
Static Data (24 hours):
- Fear & Greed Index
- Historical market data
- Company fundamentals

Market Data (15 minutes):
- Stock prices
- Market indices
- Crypto prices

User Data (5 minutes):
- Portfolio values
- User portfolios
- Notifications

Real-time (No cache):
- Transaction execution
- Backtest results
- User authentication
```

### **Rate Limiting**
```yaml
Rate Limits per User:
- API calls: 1000/hour
- Backtest runs: 10/hour
- Portfolio updates: 100/hour
- Search queries: 200/hour

Rate Limits per IP:
- Anonymous calls: 100/hour
- Authentication attempts: 10/hour
```

---

**Created**: 2024-01-15
**Version**: 1.0
**Status**: Ready for Implementation