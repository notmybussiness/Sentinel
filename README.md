# Sentinel

> Portfolio Management + Backtesting + AI News Analysis Platform

실시간 시세 기반 포트폴리오 관리, 과거 데이터 백테스팅, AI 기반 뉴스 분석을 통합한 투자 플랫폼

---

## Table of Contents

- [System Architecture](#system-architecture)
- [System Workflow](#system-workflow)
- [Entity Relationship Diagram](#entity-relationship-diagram)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Problem & Solve](#problem--solve)

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   CLIENTS                                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                  │
│  │   Web Browser   │  │   Mobile App    │  │   API Client    │                  │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘                  │
└───────────┼─────────────────────┼─────────────────────┼──────────────────────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND (Next.js 16)                              │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌────────────┐│
│  │  Portfolio  │ │   Market    │ │  Backtest   │ │     AI      │ │  Settings  ││
│  │    Page     │ │    Page     │ │    Page     │ │   Analysis  │ │    Page    ││
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └────────────┘│
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                     React 19 + TypeScript + Tailwind                     │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────┬────────────────────────────────────────────┘
                                     │ REST API / SSE
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Spring Boot 3.5.5)                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                           API Gateway Layer                              │   │
│  │   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │   │
│  │   │   Auth   │ │Portfolio │ │  Market  │ │ Backtest │ │      AI      │  │   │
│  │   │Controller│ │Controller│ │Controller│ │Controller│ │  Controller  │  │   │
│  │   └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                           Service Layer                                  │   │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌────────────────────┐ │   │
│  │  │  Portfolio  │ │   Market    │ │  Backtest   │ │   AI Analysis      │ │   │
│  │  │   Service   │ │  Provider   │ │   Engine    │ │   (Gemini API)     │ │   │
│  │  │             │ │   Factory   │ │             │ │                    │ │   │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                          Event-Driven Layer                              │   │
│  │    ┌───────────────────┐        ┌───────────────────┐                   │   │
│  │    │  Kafka Producer   │◄──────►│  Kafka Consumer   │                   │   │
│  │    │  (Price Updates)  │        │  (Portfolio Sync) │                   │   │
│  │    └───────────────────┘        └───────────────────┘                   │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────┬────────────────────────────────────────────┘
                                     │
            ┌────────────────────────┼────────────────────────┐
            ▼                        ▼                        ▼
┌───────────────────┐   ┌───────────────────┐   ┌───────────────────────────────┐
│    PostgreSQL     │   │      Redis        │   │          Kafka                │
│  ┌─────────────┐  │   │  ┌─────────────┐  │   │  ┌─────────────────────────┐  │
│  │    Users    │  │   │  │   Session   │  │   │  │   market.price.update   │  │
│  │  Portfolios │  │   │  │    Cache    │  │   │  │   portfolio.sync        │  │
│  │   Holdings  │  │   │  │   Price     │  │   │  │   batch.dlq             │  │
│  │PriceHistory │  │   │  │   Cache     │  │   │  └─────────────────────────┘  │
│  └─────────────┘  │   │  └─────────────┘  │   │                               │
└───────────────────┘   └───────────────────┘   └───────────────────────────────┘
                                                               │
                         ┌─────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         PYTHON RAG (FastAPI)                                    │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                           RAG Pipeline                                   │   │
│  │   ┌────────────┐   ┌────────────┐   ┌────────────┐   ┌──────────────┐   │   │
│  │   │   Naver    │──►│   Text     │──►│ Embedding  │──►│   Milvus     │   │   │
│  │   │News Collect│   │  Splitter  │   │ ko-sroberta│   │  Vector DB   │   │   │
│  │   └────────────┘   └────────────┘   └────────────┘   └──────────────┘   │   │
│  │                                                             │            │   │
│  │   ┌─────────────────────────────────────────────────────────┘            │   │
│  │   ▼                                                                      │   │
│  │   ┌────────────┐   ┌────────────┐   ┌────────────┐                      │   │
│  │   │   Vector   │──►│ Cross-Enc  │──►│   Gemini   │──► Analysis Result   │   │
│  │   │   Search   │   │ Re-ranker  │   │    LLM     │                      │   │
│  │   │(Hybrid)    │   │(ko-reranker)│  │            │                      │   │
│  │   └────────────┘   └────────────┘   └────────────┘                      │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         EXTERNAL SERVICES                                        │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────┐ │
│  │Yahoo Finance│ │Korea Invest │ │   Upbit     │ │ Naver News  │ │  Gemini   │ │
│  │  (US Stock) │ │ (KR Stock)  │ │  (Crypto)   │ │    API      │ │    API    │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └───────────┘ │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## System Workflow

### 1. Portfolio Price Update Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Scheduler  │────►│   Market     │────►│    Kafka     │────►│  Portfolio   │
│   (Cron)     │     │   Provider   │     │   Producer   │     │   Consumer   │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
      │                    │                    │                     │
      │ Trigger            │ Fetch Price        │ Publish Event       │ Update Holdings
      ▼                    ▼                    ▼                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│   1. PortfolioPriceScheduler triggers price update                          │
│   2. MarketDataProviderFactory selects appropriate provider                 │
│      - Yahoo Finance (US stocks)                                            │
│      - Korea Investment (KR stocks)                                         │
│      - Upbit (Crypto)                                                       │
│   3. Kafka publishes PriceUpdateEvent                                       │
│   4. PortfolioPriceConsumer updates all holdings with new prices            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2. Backtest Workflow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BACKTEST EXECUTION FLOW                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   User Request                                                              │
│       │                                                                     │
│       ▼                                                                     │
│   ┌───────────────────┐                                                     │
│   │ BacktestController│ ◄── POST /api/v1/backtest                          │
│   └─────────┬─────────┘                                                     │
│             │                                                               │
│             ▼                                                               │
│   ┌───────────────────┐      ┌─────────────────────┐                       │
│   │HistoricalDataFacade│────►│HistoricalDataService │                       │
│   │  (Coordinator)    │      │  - KisHistorical    │                       │
│   │                   │      │  - CryptoHistorical │                       │
│   └─────────┬─────────┘      └─────────────────────┘                       │
│             │                                                               │
│             ▼                                                               │
│   ┌───────────────────┐                                                     │
│   │  BacktestEngine   │ ◄── Core simulation logic                          │
│   │                   │                                                     │
│   │  - Initialize     │                                                     │
│   │  - Simulate Days  │                                                     │
│   │  - Rebalancing    │                                                     │
│   │  - Track Equity   │                                                     │
│   └─────────┬─────────┘                                                     │
│             │                                                               │
│             ▼                                                               │
│   ┌───────────────────┐                                                     │
│   │PerformanceCalculator│ ◄── Calculate metrics                            │
│   │  - Total Return   │                                                     │
│   │  - CAGR           │                                                     │
│   │  - Max Drawdown   │                                                     │
│   │  - Sharpe Ratio   │                                                     │
│   └───────────────────┘                                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3. RAG News Analysis Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          RAG PIPELINE WORKFLOW                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────┐     ┌──────────┐     ┌───────────┐     ┌────────────────┐     │
│  │  Query  │────►│  Vector  │────►│  Re-rank  │────►│    Generate    │     │
│  │ (User)  │     │  Search  │     │(ko-rerank)│     │   (Gemini)     │     │
│  └─────────┘     └──────────┘     └───────────┘     └────────────────┘     │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STAGE 1: Embedding & Vector Search                                   │   │
│  │   - KoSRoBERTa encodes query to 768-dim vector                      │   │
│  │   - Milvus performs ANN search (HNSW index)                         │   │
│  │   - Returns top-K candidate documents                               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                          │                                                  │
│                          ▼                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STAGE 2: Cross-Encoder Re-ranking                                   │   │
│  │   - ko-reranker scores query-document pairs                         │   │
│  │   - Re-orders results by relevance                                  │   │
│  │   - Filters by min_relevance threshold                              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                          │                                                  │
│                          ▼                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ STAGE 3: LLM Generation (RAG)                                       │   │
│  │   - Context: Top-K relevant news articles                           │   │
│  │   - Prompt: User query + retrieved context                          │   │
│  │   - Output: AI-generated analysis with citations                    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DATABASE SCHEMA                                 │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────┐          ┌──────────────────────┐
│        users         │          │     user_sessions    │
├──────────────────────┤          ├──────────────────────┤
│ id (PK)              │──┐       │ id (PK)              │
│ kakao_id (UNIQUE)    │  │       │ user_id (FK)         │◄──┐
│ email (UNIQUE)       │  │       │ refresh_token        │   │
│ name                 │  │       │ device_info          │   │
│ profile_image_url    │  │       │ ip_address           │   │
│ is_active            │  │       │ created_at           │   │
│ created_at           │  │       │ expires_at           │   │
│ updated_at           │  │       └──────────────────────┘   │
└──────────────────────┘  │                                   │
            │             └───────────────────────────────────┘
            │ 1:N
            ▼
┌──────────────────────┐          ┌──────────────────────┐
│     portfolios       │          │  portfolio_holdings  │
├──────────────────────┤          ├──────────────────────┤
│ id (PK)              │──┐       │ id (PK)              │
│ user_id (FK)         │◄─┘       │ portfolio_id (FK)    │◄──┐
│ name                 │          │ symbol               │   │
│ description          │   1:N    │ quantity             │   │
│ total_value          │─────────►│ average_cost         │   │
│ total_cost           │          │ current_price        │   │
│ total_gain_loss      │          │ market_value         │   │
│ total_gain_loss_%    │          │ total_cost           │   │
│ version (Optimistic) │          │ gain_loss            │   │
│ created_at           │          │ gain_loss_percent    │   │
│ updated_at           │          │ asset_type           │   │
└──────────────────────┘          │ base_currency        │   │
                                  │ version (Optimistic) │   │
                                  │ created_at           │   │
                                  │ updated_at           │   │
                                  └──────────────────────┘   │
                                            │                 │
                                            │ UNIQUE          │
                                            │ (portfolio_id,  │
                                            │  symbol)        │
                                            └─────────────────┘

┌──────────────────────┐          ┌──────────────────────┐
│    price_history     │          │     crypto_price     │
├──────────────────────┤          ├──────────────────────┤
│ id (PK)              │          │ id (PK)              │
│ symbol               │          │ symbol               │
│ asset_type           │          │ base_currency        │
│ base_currency        │          │ price                │
│ timestamp            │          │ change_24h           │
│ open                 │          │ volume_24h           │
│ high                 │          │ market_cap           │
│ low                  │          │ created_at           │
│ close                │          │ updated_at           │
│ volume               │          └──────────────────────┘
│ created_at           │
└──────────────────────┘
        │
        │ INDEXES
        ├─► idx_symbol_timestamp
        ├─► idx_asset_type_timestamp
        └─► idx_timestamp

┌─────────────────────────────────────────────────────────────────────────────┐
│                         MILVUS VECTOR COLLECTION                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Collection: news_articles                                                  │
│  ┌────────────────────────────────────────────────────────────────────┐    │
│  │ Fields:                                                             │    │
│  │   - id: INT64 (Primary Key, Auto ID)                               │    │
│  │   - title: VARCHAR(512)                                            │    │
│  │   - content: VARCHAR(65535)                                        │    │
│  │   - source: VARCHAR(256)                                           │    │
│  │   - published_at: VARCHAR(64)                                      │    │
│  │   - url: VARCHAR(1024)                                             │    │
│  │   - embedding: FLOAT_VECTOR(768) ◄── ko-sroberta                   │    │
│  │                                                                     │    │
│  │ Index: HNSW (M=16, efConstruction=200)                             │    │
│  │ Metric: Cosine Similarity                                          │    │
│  └────────────────────────────────────────────────────────────────────┘    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Data Model Summary

| Entity | Description | Key Fields |
|--------|-------------|------------|
| **User** | OAuth2 (Kakao) authenticated user | email, kakaoId, name |
| **UserSession** | JWT refresh token management | refreshToken, expiresAt |
| **Portfolio** | User's investment portfolio | totalValue, totalCost, gainLoss |
| **PortfolioHolding** | Individual asset in portfolio | symbol, quantity, averageCost |
| **PriceHistory** | OHLCV time-series data | symbol, timestamp, open/high/low/close/volume |
| **CryptoPrice** | Real-time crypto prices | symbol, price, change24h |

---

## Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Main language |
| Spring Boot | 3.5.5 | Application framework |
| Spring Security | - | Authentication & Authorization |
| Spring Data JPA | - | ORM & Data access |
| Spring Kafka | - | Event-driven messaging |
| PostgreSQL | 15 | Relational database |
| Redis | 7+ | Cache & Session store |
| Kafka | 7.5.0 | Message broker |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| Next.js | 16 | React framework |
| React | 19 | UI library |
| TypeScript | 5+ | Type safety |
| Tailwind CSS | 4+ | Styling |

### AI/RAG
| Technology | Version | Purpose |
|------------|---------|---------|
| Python | 3.11+ | RAG pipeline |
| FastAPI | - | API server |
| Milvus | 2.3.3 | Vector database |
| KoSRoBERTa | - | Korean embeddings |
| ko-reranker | - | Korean cross-encoder |
| Gemini API | - | LLM generation |

---

## Project Structure

```
Sentinel/
├── backend/                          # Spring Boot Backend
│   ├── src/main/java/.../sentinel/
│   │   ├── ai/                       # AI Analysis (Gemini)
│   │   ├── backtest/                 # Backtesting Engine
│   │   ├── batch/                    # Spring Batch Jobs
│   │   ├── common/                   # Shared Config & Utils
│   │   ├── crypto/                   # Crypto Data & Streaming
│   │   ├── market/                   # Market Data Providers
│   │   ├── portfolio/                # Portfolio Management
│   │   ├── pricehistory/             # Price History OHLCV
│   │   └── user/                     # User & Auth
│   ├── docker-compose.yml            # Infra (Redis, Kafka, Postgres)
│   └── scripts/perf-tuning/          # k6 Performance Tests
│
├── frontend/                         # Next.js Frontend
│   ├── app/
│   │   ├── portfolio/                # Portfolio Pages
│   │   ├── market/                   # Market Data
│   │   ├── backtest/                 # Backtesting UI
│   │   ├── ai/                       # AI Analysis
│   │   └── settings/                 # User Settings
│   ├── components/                   # Reusable Components
│   └── lib/api/                      # API Client
│
├── python-rag/                       # Python RAG Pipeline
│   ├── data-pipeline/src/
│   │   ├── api/                      # FastAPI Server
│   │   ├── collectors/               # News Collectors
│   │   ├── embeddings/               # Embedding Models
│   │   ├── processors/               # Text Processing
│   │   ├── rag/                      # RAG Pipeline
│   │   └── storage/                  # Milvus Client
│   ├── experiments/                  # A/B Test Scripts
│   └── docker-compose.yml            # Milvus Stack
│
└── .claude/
    ├── experiments/                  # Experiment Records
    └── skills/                       # Claude Skills
```

---

## Getting Started

### Prerequisites
- Java 21+
- Node.js 20+
- Python 3.11+
- Docker & Docker Compose

### Quick Start

```bash
# 1. Start Infrastructure
cd backend
docker-compose up -d    # PostgreSQL, Redis, Kafka

# 2. Start RAG Services (Optional)
cd python-rag
docker-compose up -d    # Milvus, RAG API

# 3. Start Backend
cd backend
./gradlew.bat bootRun   # Windows
./gradlew bootRun       # Linux/Mac

# 4. Start Frontend
cd frontend
npm install
npm run dev

# Access
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080
# RAG API:  http://localhost:8000
```

---

## Problem & Solve

> 체계적인 문제 해결을 위한 과학적 실험 방법론 적용

### Experiment Workflow

우리는 성능 최적화와 기능 개선을 위해 다음과 같은 4단계 실험 방법론을 사용합니다:

```
┌─────────────────────────────────────────────────────────────────┐
│ Phase 1: PROBLEM IDENTIFICATION                                  │
│ - 현재 상태 측정 (Baseline)                                      │
│ - 문제 정의                                                      │
│ - 성공 기준 설정                                                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Phase 2: SOLUTION EXPLORATION                                    │
│ - 가능한 해결책 조사                                             │
│ - Trade-off 분석                                                 │
│ - 테스트할 접근법 선택                                           │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Phase 3: EXPERIMENT                                              │
│ - 가설: "X를 하면, Z 때문에 Y 결과가 나온다"                      │
│ - Control vs Treatment 설정                                      │
│ - 실험 실행 및 결과 수집                                         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ Phase 4: CONCLUSION                                              │
│ - 결과 분석                                                      │
│ - VALID / INVALID / INCONCLUSIVE 판정                            │
│ - 학습 내용 기록 및 다음 단계 정의                               │
└─────────────────────────────────────────────────────────────────┘
```

---

### Case Study: RAG 검색 정확도 개선 (EXP-03)

#### Problem Identification

| 항목 | 내용 |
|------|------|
| **현재 상태** | 검색 정확도 44% (Hybrid Search 기준) |
| **문제** | Vector Search만으로는 관련성 높은 기사를 상위에 노출시키지 못함 |
| **목표** | 검색 정확도 70%+ 달성 |

#### Solution Exploration

| Option | Pros | Cons | Effort |
|--------|------|------|--------|
| Query Expansion | 쿼리 풍부화 | 노이즈 증가 가능 | Med |
| Hybrid Search | BM25+Vector 결합 | 이미 적용됨 (44%) | Done |
| **Cross-Encoder Reranking** | **높은 정확도** | 추가 지연시간 | **Med** |
| Fine-tuning Embedder | 최적화된 임베딩 | 데이터/시간 필요 | High |

**선택: Cross-Encoder Reranking** - Two-stage retrieval로 정확도 향상 가능, 한국어 특화 모델 존재

#### Experiment

**가설:**
> Cross-encoder reranker를 vector search 후에 적용하면, cross-encoder가 query-document relevance를 bi-encoder similarity보다 더 정확하게 계산하기 때문에 precision이 10%+ 향상될 것이다.

**테스트 설정:**
- Control: Vector Search only (KoSRoBERTa embeddings)
- Treatment: Vector Search + Korean Cross-Encoder (ko-reranker)

**결과:**

| Query | Control | Treatment | Delta |
|-------|---------|-----------|-------|
| 삼성전자 | 100% | 100% | +0%p |
| SK하이닉스 | 80% | 100% | **+20%p** |
| 네이버 | 20% | 20% | +0%p |
| 카카오 | 100% | 100% | +0%p |
| LG에너지솔루션 | 20% | 40% | **+20%p** |
| **Average** | **64%** | **72%** | **+8%p** |

#### Conclusion

**Verdict: VALID**

**분석:**
- 가설 확인: Cross-encoder reranking이 검색 정확도를 8%p 향상시킴
- 특히 벡터 검색에서 약한 쿼리(SK하이닉스, LG에너지솔루션)에서 효과가 큼

**핵심 학습:**
1. **영어 vs 한국어 모델**: 처음 사용한 `ms-marco-MiniLM`은 오히려 정확도 하락 (64% → 60%)
2. **Cross-encoder 점수 범위**: 음수(-10 ~ -8)일 수 있음. `min_relevance` 필터에 주의
3. **전처리 vs 리랭킹**: 리랭킹이 정확도에 직접적 영향 (+8%p), 전처리는 간접적 (+0.9%)

---

### Bottleneck Analysis Patterns

| 증상 | 가능한 원인 | 조사 방법 |
|------|------------|----------|
| CPU 100% | 비효율적 알고리즘 | Hot method 프로파일링 |
| High GC pause | 메모리 누수 | Heap dump 분석 |
| DB connection wait | Pool 크기 부족 | Pool size 증가 |
| Slow queries | 인덱스 누락 | EXPLAIN ANALYZE |
| Thread exhaustion | Blocking I/O | Async 전환 |

### Performance Monitoring

```bash
# Docker stats
docker stats --no-stream

# PostgreSQL slow queries
psql -c "SELECT query, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10"

# HikariCP metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Redis latency
redis-cli --latency

# k6 load test
k6 run --vus 100 --duration 30s backend/scripts/perf-tuning/exp_baseline.js
```

---

### Experiment Naming Convention

```
EXP-<number>_<category>-<brief-description>

Examples:
- EXP-01_perf-db-indexing
- EXP-02_rag-preprocessing
- EXP-03_rag-korean-reranker
- EXP-04_perf-redis-caching
```

모든 실험 기록은 `.claude/experiments/` 디렉토리에 저장됩니다.

---

## License

MIT License

---

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feat/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request
