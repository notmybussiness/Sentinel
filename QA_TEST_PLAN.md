# QA Test Plan - Sentinel UX Improvements
**Date**: 2025-10-19
**Version**: 1.0
**Testing Environment**: Local Development (Frontend: 3000, Backend: 8080)

---

## Executive Summary

This test plan covers critical UX improvements:
- Unified holding modal (stock + crypto)
- PriceDisplay null handling
- Dev login authentication enhancement

**Test Coverage Areas**: 4 major areas, 23 test cases total
**Priority Distribution**: 8 Critical, 9 High, 4 Medium, 2 Low

---

## 1. User Flow Testing - Unified Holding Modal

### TC-001: Open Unified Holding Modal [CRITICAL]
**Priority**: Critical
**Prerequisites**: User logged in, portfolio detail page open
**Steps**:
1. Navigate to portfolio detail page
2. Click "+ 종목 추가" button
3. Verify modal opens

**Expected Results**:
- Modal opens with title "종목 추가"
- Asset type selector visible (📈 Stock / ₿ Crypto)
- Default selection is Stock
- Form shows stock-specific fields (symbol, quantity, averageCost)

**Actual Results**: [To be filled during testing]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-002: Stock Addition Flow [CRITICAL]
**Priority**: Critical
**Prerequisites**: Unified modal open, Stock type selected
**Steps**:
1. Verify Stock tab is selected (📈 Stock)
2. Enter symbol: "AAPL"
3. Enter quantity: "10"
4. Enter average cost: "150.50"
5. Click "추가" button

**Expected Results**:
- Form validation passes
- API call to `/api/v1/portfolios/{id}/holdings` with:
  ```json
  {
    "symbol": "AAPL",
    "quantity": 10,
    "averageCost": 150.50,
    "assetType": "STOCK"
  }
  ```
- Modal closes on success
- Portfolio refetches and shows new holding
- AAPL appears in holdings list with correct values

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-003: Crypto Addition Flow - KRW [CRITICAL]
**Priority**: Critical
**Prerequisites**: Unified modal open
**Steps**:
1. Click "₿ Crypto" tab
2. Verify baseCurrency selector appears
3. Select "KRW" (default)
4. Enter symbol: "BTC"
5. Enter quantity: "0.5"
6. Enter average cost: "80000000"
7. Click "추가" button

**Expected Results**:
- Form shows baseCurrency dropdown (KRW/USD)
- Form validation passes
- API call with:
  ```json
  {
    "symbol": "BTC",
    "quantity": 0.5,
    "averageCost": 80000000,
    "assetType": "CRYPTO",
    "baseCurrency": "KRW"
  }
  ```
- Modal closes
- BTC holding appears with KRW prices

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-004: Crypto Addition Flow - USD [HIGH]
**Priority**: High
**Prerequisites**: Unified modal open, Crypto tab selected
**Steps**:
1. Select Crypto tab
2. Change baseCurrency to "USD"
3. Enter symbol: "ETH"
4. Enter quantity: "2.5"
5. Enter average cost: "2500"
6. Submit

**Expected Results**:
- USD selected in dropdown
- API call includes `"baseCurrency": "USD"`
- ETH holding shows USD prices
- Price format shows $ symbol

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-005: Asset Type Switching [HIGH]
**Priority**: High
**Prerequisites**: Modal open
**Steps**:
1. Start with Stock tab, enter "AAPL"
2. Switch to Crypto tab
3. Verify form fields reset
4. Enter "BTC"
5. Switch back to Stock tab
6. Verify form fields reset again

**Expected Results**:
- Form fields clear when switching tabs
- No data persists between switches
- baseCurrency selector only visible on Crypto tab
- Symbol validation appropriate to selected type

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-006: Form Validation - Stock [HIGH]
**Priority**: High
**Prerequisites**: Modal open, Stock tab
**Test Cases**:
| Input | Expected Behavior |
|-------|-------------------|
| Empty symbol | Error: "Symbol is required" |
| Symbol < 1 char | Error: "Symbol must be at least 1 character" |
| Quantity = 0 | Error: "Quantity must be > 0" |
| Quantity = -5 | Error: "Quantity must be > 0" |
| Average cost = 0 | Error: "Average cost must be > 0" |
| Average cost = -100 | Error: "Average cost must be > 0" |

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-007: Form Validation - Crypto [HIGH]
**Priority**: High
**Prerequisites**: Modal open, Crypto tab
**Test Cases**:
| Input | Expected Behavior |
|-------|-------------------|
| Empty symbol | Error: "Symbol is required" |
| Empty baseCurrency | Error or default to KRW |
| Quantity = 0 | Error: "Quantity must be > 0" |
| Quantity = 0.00001 | Accept (crypto allows small fractions) |
| Average cost = 0 | Error: "Average cost must be > 0" |

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-008: Modal Close Behavior [MEDIUM]
**Priority**: Medium
**Prerequisites**: Modal open with data entered
**Steps**:
1. Enter data in form (don't submit)
2. Click outside modal (backdrop)
3. Reopen modal

**Expected Results**:
- Modal closes when clicking backdrop
- Form data is cleared
- No unsaved data warning (acceptable for this use case)
- Reopening shows fresh form

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

## 2. Error Handling Testing - PriceDisplay

### TC-009: Null Price Handling [CRITICAL]
**Priority**: Critical
**Prerequisites**: Portfolio with holdings
**Steps**:
1. Add a crypto that may fail price fetch (e.g., "SOL", "ADA")
2. Observe PriceDisplay component
3. Check browser console for errors

**Expected Results**:
- Component displays "-" for current price
- No console errors
- No component crash
- Page remains functional

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-010: NaN Price Handling [HIGH]
**Priority**: High
**Prerequisites**: Developer tools open
**Steps**:
1. Mock API to return NaN or invalid number
2. Observe PriceDisplay rendering

**Expected Results**:
- Gracefully displays "-"
- No "NaN" text visible to user
- No runtime errors

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-011: Undefined Price Handling [HIGH]
**Priority**: High
**Prerequisites**: Network conditions can be simulated
**Steps**:
1. Disconnect network during price fetch
2. Or mock API to return undefined

**Expected Results**:
- PriceDisplay shows "-"
- Optional: Loading state appears first
- No crash

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-012: Price Fetch Network Failure [MEDIUM]
**Priority**: Medium
**Prerequisites**: Browser DevTools Network tab
**Steps**:
1. Throttle network to "Slow 3G"
2. Add new holding
3. Observe price loading behavior

**Expected Results**:
- Loading indicator appears
- Timeout after reasonable duration
- Falls back to "-" if fetch fails
- No infinite loading state

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

## 3. Authentication Testing - Dev Login

### TC-013: Dev Login Flow [CRITICAL]
**Priority**: Critical
**Prerequisites**: Logged out state
**Steps**:
1. Navigate to `/login`
2. Click "개발자 로그인 (테스트)" button
3. Observe redirect behavior

**Expected Results**:
- API call to `/api/v1/auth/dev-login` (POST)
- Response contains:
  ```json
  {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "user": {
      "id": 1,
      "email": "dev@sentinel.com",
      "name": "Developer"
    }
  }
  ```
- Tokens stored in localStorage
- **Immediate redirect** to `/` (no page refresh)
- Home page shows authenticated state

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-014: JWT Token Validation [CRITICAL]
**Priority**: Critical
**Prerequisites**: Dev login completed
**Steps**:
1. Open browser DevTools → Application → Local Storage
2. Verify tokens exist
3. Decode JWT (use jwt.io)
4. Make authenticated API call (e.g., GET /api/v1/portfolios)

**Expected Results**:
- `accessToken` exists in localStorage
- `refreshToken` exists in localStorage
- JWT payload contains:
  - `sub`: "1" (user ID)
  - `email`: "dev@sentinel.com"
  - `exp`: future timestamp
- API calls succeed with Authorization header

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-015: Portfolio Creation After Login [CRITICAL]
**Priority**: Critical
**Prerequisites**: Dev login completed
**Steps**:
1. Navigate to home page
2. Click "포트폴리오 생성" button
3. Fill form and submit

**Expected Results**:
- Modal opens
- Form accepts input
- API call succeeds with JWT token
- New portfolio appears in list
- No authentication errors

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-016: Holding Addition After Login [HIGH]
**Priority**: High
**Prerequisites**: Dev login + portfolio exists
**Steps**:
1. Open portfolio detail
2. Click "+ 종목 추가"
3. Add stock or crypto

**Expected Results**:
- Modal opens
- Addition succeeds
- API returns 200 OK
- Holding appears in list

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-017: Session Persistence [MEDIUM]
**Priority**: Medium
**Prerequisites**: Dev login completed
**Steps**:
1. Log in via dev login
2. Refresh page (F5)
3. Navigate between pages

**Expected Results**:
- User remains authenticated after refresh
- Tokens persist in localStorage
- No re-login required
- Protected routes accessible

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-018: Logout Flow [LOW]
**Priority**: Low
**Prerequisites**: Authenticated session
**Steps**:
1. Click logout button (if exists)
2. Or manually clear localStorage
3. Try accessing protected route

**Expected Results**:
- Tokens removed from storage
- Redirect to login page
- Cannot access protected routes

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

## 4. Regression Testing - Existing Features

### TC-019: AI Analysis Modal [HIGH]
**Priority**: High
**Prerequisites**: Portfolio with holdings
**Steps**:
1. Open portfolio detail
2. Click "🤖 AI 분석" button
3. Select analysis type
4. Submit

**Expected Results**:
- Modal opens correctly
- All 5 analysis types available
- API call succeeds
- Results display properly
- No console errors

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-020: Rebalancing Modal [HIGH]
**Priority**: High
**Prerequisites**: Portfolio with ≥2 holdings
**Steps**:
1. Open portfolio detail
2. Click "리밸런싱" button
3. Configure and generate recommendations

**Expected Results**:
- Modal opens
- Strategy selection works
- Threshold input accepts values
- API call succeeds
- BUY/SELL/HOLD recommendations display
- Transaction costs calculated

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-021: Backtest Lab Page [HIGH]
**Priority**: High
**Prerequisites**: At least 1 portfolio
**Steps**:
1. Navigate to `/lab`
2. Select portfolio
3. Configure date range
4. Run backtest

**Expected Results**:
- Page loads without errors
- Portfolio dropdown populated
- Date pickers work
- Backtest executes
- Results display with 7 metrics
- Charts render

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-022: Market Page [MEDIUM]
**Priority**: Medium
**Prerequisites**: None
**Steps**:
1. Navigate to `/market`
2. Observe index data loading

**Expected Results**:
- Market indices load (S&P500, NASDAQ, DOW, KOSPI)
- No errors from recent changes

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

### TC-023: Holding Edit/Delete [LOW]
**Priority**: Low
**Prerequisites**: Portfolio with holdings
**Steps**:
1. Click edit icon on holding
2. Modify values
3. Save
4. Delete a holding

**Expected Results**:
- Edit modal opens
- Changes persist
- Delete confirmation works
- List updates

**Actual Results**: [To be filled]
**Status**: [ ] Pass [ ] Fail [ ] Blocked
**Notes**:

---

## 5. Cross-Browser Testing (Optional)

### TC-024: Chrome [MEDIUM]
**Priority**: Medium
**Key Scenarios**: TC-001, TC-002, TC-003, TC-013
**Status**: [ ] Pass [ ] Fail

### TC-025: Firefox [LOW]
**Priority**: Low
**Key Scenarios**: TC-001, TC-002, TC-013
**Status**: [ ] Pass [ ] Fail

### TC-026: Edge [LOW]
**Priority**: Low
**Key Scenarios**: TC-001, TC-002, TC-013
**Status**: [ ] Pass [ ] Fail

---

## 6. Bugs & Issues Found

### Bug Report Template
```
BUG-XXX: [Short Description]
Severity: [Critical/High/Medium/Low]
Found in: [Test Case ID]
Steps to Reproduce:
1.
2.
3.

Expected:
Actual:
Screenshots/Logs:
Recommendation:
```

---

## 7. Test Execution Summary

**Date Executed**: [To be filled]
**Tester**: Claude QA Engineer
**Environment**: Local Development

| Priority | Total | Pass | Fail | Blocked | Pass Rate |
|----------|-------|------|------|---------|-----------|
| Critical | 8     |      |      |         |           |
| High     | 9     |      |      |         |           |
| Medium   | 4     |      |      |         |           |
| Low      | 2     |      |      |         |           |
| **Total**| **23**|      |      |         |           |

---

## 8. Recommendations for Improvement

### High Priority Recommendations
1. **Add Loading States**: Unified modal should show loading during API calls
2. **Error Messages**: Display user-friendly error messages when holdings fail to add
3. **Success Feedback**: Toast notification on successful holding addition
4. **Symbol Validation**: Real-time validation against available stocks/crypto

### Medium Priority Recommendations
5. **Auto-complete**: Symbol search with suggestions
6. **Price Preview**: Show current price while entering symbol
7. **Duplicate Check**: Warn if adding duplicate holding
8. **Keyboard Navigation**: Tab order and Enter to submit

### Low Priority Recommendations
9. **Modal Animation**: Smooth open/close transitions
10. **Mobile Responsiveness**: Test on mobile viewport
11. **Accessibility**: ARIA labels, screen reader support
12. **Analytics**: Track modal usage and conversion rates

---

## 9. Test Automation Candidates

**High Value for Automation**:
- TC-001: Modal open/close
- TC-002: Stock addition happy path
- TC-003: Crypto addition happy path
- TC-013: Dev login flow
- TC-019-021: Regression tests for existing features

**Framework Suggestion**: Playwright E2E tests
**Location**: `frontend/e2e/unified-modal.spec.ts`

---

## Appendix A: API Endpoints Reference

### Dev Login
```
POST /api/v1/auth/dev-login
Response: { accessToken, refreshToken, user }
```

### Add Holding
```
POST /api/v1/portfolios/{id}/holdings
Body: {
  symbol: string,
  quantity: number,
  averageCost: number,
  assetType: "STOCK" | "CRYPTO",
  baseCurrency?: "KRW" | "USD"  // Required for CRYPTO
}
```

### Get Portfolio
```
GET /api/v1/portfolios/{id}
Authorization: Bearer {accessToken}
```

---

## Appendix B: Known Issues (Pre-existing)

1. **E2E Test Timeout**: Portfolio creation step times out (auth state propagation)
2. **Circuit Breaker**: Not implemented (deferred to Phase 6)
3. **Redis**: Not used in dev environment

---

**Document Version**: 1.0
**Last Updated**: 2025-10-19
**Next Review**: After test execution
**Status**: Ready for Testing
