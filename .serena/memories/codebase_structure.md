# Sentinel - Codebase Structure

## Backend Domain Modules

| Domain | Path | Description |
|--------|------|-------------|
| **ai** | `sentinel/ai/` | Gemini AI integration, portfolio analysis |
| **backtest** | `sentinel/backtest/` | Backtesting engine, historical data |
| **batch** | `sentinel/batch/` | Spring Batch jobs (crypto history) |
| **common** | `sentinel/common/` | Shared configs, JWT filter, exceptions |
| **config** | `sentinel/config/` | App-level configs (cache, security) |
| **crypto** | `sentinel/crypto/` | Cryptocurrency data (Upbit provider) |
| **market** | `sentinel/market/` | Stock market data (KIS, AlphaVantage, Yahoo) |
| **portfolio** | `sentinel/portfolio/` | Portfolio CRUD, holdings management |
| **pricehistory** | `sentinel/pricehistory/` | Price history storage and retrieval |
| **rag** | `sentinel/rag/` | News RAG integration with Python service |
| **rebalancing** | `sentinel/rebalancing/` | Portfolio rebalancing strategies |
| **user** | `sentinel/user/` | User auth, Kakao OAuth, JWT |

## Key Files by Domain

### Portfolio
- `PortfolioController.java` - REST endpoints
- `PortfolioService.java` - Business logic with caching
- `Portfolio.java` - JPA entity
- `PortfolioRepository.java` - Data access with EntityGraph

### Authentication
- `AuthController.java` - Login/logout endpoints
- `AuthService.java` - Token management
- `JwtService.java` - JWT creation/validation
- `JwtAuthenticationFilter.java` - Request filtering

### Market Data
- `MarketDataController.java` - Stock price endpoints
- `MarketDataService.java` - Provider routing
- `YahooFinanceProvider.java` - Yahoo Finance integration
- `KoreaInvestmentProvider.java` - KIS API integration

### Cryptocurrency
- `CryptoDataController.java` - Crypto price endpoints
- `CryptoDataService.java` - Upbit integration
- `SSEStreamingService.java` - Real-time price streaming

## Frontend Pages

| Route | File | Description |
|-------|------|-------------|
| `/` | `app/page.tsx` | Home/Dashboard |
| `/portfolio` | `app/portfolio/page.tsx` | Portfolio list |
| `/portfolio/[id]` | `app/portfolio/[id]/page.tsx` | Portfolio detail |
| `/market` | `app/market/page.tsx` | Market data |
| `/backtest` | `app/backtest/page.tsx` | Backtesting UI |
| `/ai` | `app/ai/page.tsx` | AI analysis |
| `/price-history` | `app/price-history/page.tsx` | Historical charts |
| `/settings` | `app/settings/page.tsx` | User settings |

## Configuration Files

| File | Purpose |
|------|---------|
| `backend/build.gradle` | Gradle dependencies & JVM settings |
| `backend/docker-compose.yml` | Local dev infrastructure |
| `backend/src/main/resources/application.yml` | Spring config |
| `frontend/package.json` | NPM dependencies |
| `frontend/next.config.ts` | Next.js config |
| `.claude/CLAUDE.md` | Project status & context |
| `.claude/ARCHITECTURE.md` | Architecture docs (EDA, Streaming, Project Structure) |
| `.claude/PERF_TUNING_GUIDE.md` | Performance tuning experiments guide |

## Documentation Structure

| Location | Content |
|----------|---------|
| `.claude/` | Main documentation (CLAUDE.md, ARCHITECTURE.md, PERF_TUNING_GUIDE.md) |
| `.claude/skills/` | Claude Code skills |
| `backend/docs/` | API_MAP.md, GRAFANA_GUIDE.md, NEXT_PHASE_PLAN.md |
| `backend/scripts/perf-tuning/` | Performance test scripts and monitoring |

## Notes

- Architecture docs consolidated in `.claude/ARCHITECTURE.md`
- Performance tuning guide in `.claude/PERF_TUNING_GUIDE.md`
- Use Serena tools (`find_symbol`, `get_symbols_overview`) to explore API structure
- Controllers are located at `{domain}/controller/` directories