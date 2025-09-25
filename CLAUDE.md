# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

```bash
npm run dev                      # Start both frontend and backend
npm run dev:backend              # Backend only (port 8080)
npm run dev:frontend             # Frontend only (port 3000)
npm run test:backend             # ./gradlew.bat test
npm run build:backend            # ./gradlew.bat build
```

## Key API Endpoints

```bash
GET /api/v1/market/price/{symbol}        # Individual stock price
GET /api/v1/market/prices?symbols=A,B,C  # Multiple stock prices
GET /api/v1/market/status                # Service status
POST /api/v1/auth/kakao/callback         # Kakao OAuth
```

## Architecture

- **Backend**: Spring Boot 3.5.5, multi-provider stock data system with automatic fallback (Yahoo Finance → Finnhub → AlphaVantage)
- **Frontend**: Next.js 14 + TypeScript + Tailwind CSS
- **Stock Data**: MarketDataProviderFactory manages multiple API providers, individual API calls preferred over bulk
- **Auth**: JWT + Kakao OAuth2

## Development Notes

- Use individual stock API endpoints (`/price/{symbol}`) instead of bulk calls to avoid rate limiting
- MarketDataProviderFactory handles provider failover automatically
- Backend runs on port 8080, frontend on 3000
- Environment variables required for external API keys