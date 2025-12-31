# Sentinel - Project Overview

## Purpose
Sentinel is a **portfolio management + backtesting + AI-powered news analysis platform**. It enables users to:
- Manage investment portfolios (stocks, crypto)
- Run historical backtests on trading strategies
- Get AI-powered analysis using RAG (Retrieval-Augmented Generation)

## Tech Stack

### Backend (Spring Boot)
- **Framework**: Spring Boot 3.5.5, Java 21
- **Database**: PostgreSQL (production), H2 (dev)
- **Cache**: Redis (distributed cache)
- **Security**: Spring Security, JWT authentication (jjwt)
- **Messaging**: Apache Kafka (Event-Driven Architecture)
- **Resilience**: Resilience4j (Circuit Breaker, Retry)
- **Batch**: Spring Batch (historical data collection)
- **API Docs**: SpringDoc OpenAPI (Swagger)
- **Utils**: Lombok, MapStruct
- **Monitoring**: Actuator, Micrometer, Prometheus

### Frontend (Next.js)
- **Framework**: Next.js 16, React 19
- **Language**: TypeScript 5
- **Styling**: TailwindCSS 4, Framer Motion
- **Charts**: Recharts
- **E2E Testing**: Playwright

### Python RAG Service
- **Framework**: FastAPI, Uvicorn
- **ML**: Sentence Transformers, PyTorch
- **Vector DB**: Milvus (for embeddings)
- **AI**: Gemini API integration

## Project Structure
```
Sentinel/
├── backend/            # Spring Boot application
│   └── src/main/java/com/pjsent/sentinel/
│       ├── ai/         # Gemini AI integration
│       ├── backtest/   # Backtesting engine
│       ├── batch/      # Spring Batch jobs
│       ├── common/     # Shared configs, exceptions
│       ├── crypto/     # Cryptocurrency data (Upbit)
│       ├── market/     # Stock market data (KIS, Yahoo)
│       ├── portfolio/  # Portfolio management
│       ├── pricehistory/ # Historical prices
│       ├── rag/        # News RAG integration
│       ├── rebalancing/ # Portfolio rebalancing
│       └── user/       # Authentication
├── frontend/           # Next.js application
│   └── app/            # App router pages
├── python-rag/         # Python RAG service
│   ├── embedding-service/
│   ├── data-pipeline/
│   └── infrastructure/
├── .claude/            # Claude Code config
│   ├── specs/          # API specifications
│   └── skills/         # Claude skills
└── docs/               # Documentation
    └── plans/          # Feature plans
```

## Current Status (Phase 8)
- **Performance Target**: 1000+ TPS (current: 217 TPS)
- **Focus**: DB indexing, JVM tuning experiments

## External API Providers
| Provider | Purpose | Rate Limit |
|----------|---------|------------|
| KIS (한국투자증권) | Korean stocks | 50 req/s |
| AlphaVantage | US stocks | 5 req/min |
| Upbit | Cryptocurrency | 600 req/min |
| Gemini | AI analysis | - |
