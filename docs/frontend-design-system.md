# Frontend Design System

## Key Components
- **PortfolioCard**: Hover animations, gain/loss colors
- **StockPrice**: Real-time updates, trend indicators  
- **PageTransition**: Smooth page transitions
- **Dashboard**: Grid layouts, responsive design

## State Management (Zustand)
```typescript
// Portfolio Store
portfolios[], selectedPortfolio, isLoading
fetchPortfolios(), createPortfolio(), updatePortfolio()

// Auth Store  
user, isAuthenticated
login(), logout(), refreshToken()
```

## Responsive Design
- **Mobile-first**: Tailwind breakpoints
- **Components**: Adaptive layouts sm/md/lg/xl
- **Grid System**: Dashboard, portfolio, market grids