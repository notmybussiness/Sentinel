# Backend API Specifications

> **The Absolute Truth for Sentinel Backend Development**
>
> Before implementing any endpoint, **READ** the relevant spec.
> After implementation, **VERIFY** alignment.

---

## 📋 API Specification Files

| File | Domain | Key Features |
|------|--------|--------------|
| **API_AUTH.md** | Authentication & Users | OAuth2 (Kakao), JWT, Dev Login |
| **API_PORTFOLIO.md** | Portfolio Management | CRUD Portfolio, Holdings |
| **API_MARKET.md** | Stock Market Data | AlphaVantage, Finnhub |
| **API_CRYPTO.md** | Cryptocurrency | Upbit, Binance, Streaming (SSE/WebSocket) |
| **API_BACKTEST.md** | Backtesting | Historical performance simulation |
| **API_REBALANCING.md** | Rebalancing | Equal Weight, Target Allocation |

---

## 🎯 Usage Guidelines

### Before Implementation

1. **Locate the spec**: Find the relevant `API_*.md` file for your domain
2. **Read thoroughly**: Understand endpoints, DTOs, business logic
3. **Check examples**: Review request/response JSON samples

### During Implementation

1. **Match exactly**: DTO field names, types, validation rules
2. **Follow patterns**: Provider Pattern (Crypto), EntityGraph (Portfolio)
3. **Apply optimizations**: Cache TTL, Connection Pool, N+1 prevention

### After Implementation

1. **Verify alignment**: Compare your code with spec
2. **Update if changed**: If logic evolved, update the spec immediately
3. **Add examples**: Include real request/response samples

---

## 📚 Cross-Reference

### Architecture Context
- **Project Structure**: `../docs/PROJECT_STRUCTURE.md`
- **API Map**: `../docs/API_MAP.md`

### Performance Context
- **Experiment Status**: `../EXPERIMENT_STATUS.md`
- **Optimization Results**: `../../backend/scripts/results/`

---

## ⚠️ Important Rules

1. **Never guess**: If spec is unclear, ask the user
2. **No divergence**: Code and docs must always match
3. **Evidence-based**: All optimizations backed by k6 metrics

---

**Last Updated**: 2025-12-03
**Status**: API specs to be migrated from `docs/API_MAP.md`
