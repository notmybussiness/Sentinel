# Manual Testing Checklist - Sentinel UX Improvements
**Date**: 2025-10-19
**Tester**: _____________
**Environment**: Local Development

---

## Pre-Test Setup

- [ ] Backend running on http://localhost:8080
- [ ] Frontend running on http://localhost:3000
- [ ] Browser DevTools open (Console + Network tabs)
- [ ] Clear browser cache and localStorage
- [ ] Test browser: Chrome/Edge/Firefox (circle one)

---

## 1. CRITICAL: Dev Login Flow (5 min)

**URL**: http://localhost:3000/login

### Steps:
1. [ ] Click "개발자 로그인 (테스트)" button
2. [ ] Observe redirect (should be **immediate**, no refresh)
3. [ ] Check URL = http://localhost:3000/
4. [ ] Open DevTools → Application → Local Storage
5. [ ] Verify `accessToken` exists
6. [ ] Verify `refreshToken` exists

### Expected Results:
- [ ] No page refresh/flicker
- [ ] Redirect happens in <1 second
- [ ] Tokens are valid JWT format
- [ ] Console shows no errors

**Status**: ⬜ PASS ⬜ FAIL
**Notes**:
```


```

---

## 2. CRITICAL: Unified Holding Modal - Stock (10 min)

**Prerequisites**: Logged in, create a test portfolio

### Steps:
1. [ ] Click portfolio to open detail page
2. [ ] Click "+ 종목 추가" button
3. [ ] Modal opens with title "종목 추가"
4. [ ] Verify two tabs: "📈 Stock" and "₿ Crypto"
5. [ ] Stock tab is selected by default (blue background)
6. [ ] Form shows: Symbol, Quantity, Average Cost
7. [ ] **No baseCurrency dropdown** visible

**Add Stock**:
8. [ ] Enter Symbol: `AAPL`
9. [ ] Enter Quantity: `10`
10. [ ] Enter Average Cost: `150.50`
11. [ ] Click "추가" button
12. [ ] Modal closes
13. [ ] AAPL appears in holdings list
14. [ ] Price loads (or shows "-")

### Expected Results:
- [ ] Form validation works
- [ ] API call to `/api/v1/portfolios/{id}/holdings` succeeds
- [ ] Request body includes `"assetType": "STOCK"`
- [ ] No baseCurrency in request
- [ ] Holdings list updates automatically

**Status**: ⬜ PASS ⬜ FAIL
**Network Request**:
```json
{
  "symbol": "AAPL",
  "quantity": 10,
  "averageCost": 150.50,
  "assetType": "STOCK"
}
```

**Console Errors**: ⬜ None ⬜ Found: _______________

---

## 3. CRITICAL: Unified Holding Modal - Crypto KRW (10 min)

**Prerequisites**: Same portfolio as above

### Steps:
1. [ ] Click "+ 종목 추가" again
2. [ ] Click "₿ Crypto" tab
3. [ ] **baseCurrency dropdown appears**
4. [ ] Default selection: KRW
5. [ ] Enter Symbol: `BTC`
6. [ ] Enter Quantity: `0.5`
7. [ ] Enter Average Cost: `80000000`
8. [ ] Click "추가"
9. [ ] Modal closes
10. [ ] BTC appears in list

### Expected Results:
- [ ] baseCurrency dropdown only visible on Crypto tab
- [ ] KRW selected by default
- [ ] API request includes `"baseCurrency": "KRW"`
- [ ] BTC price displays in KRW (₩) or "-"

**Status**: ⬜ PASS ⬜ FAIL
**Network Request**:
```json
{
  "symbol": "BTC",
  "quantity": 0.5,
  "averageCost": 80000000,
  "assetType": "CRYPTO",
  "baseCurrency": "KRW"
}
```

**Console Errors**: ⬜ None ⬜ Found: _______________

---

## 4. HIGH: Crypto USD (5 min)

**Prerequisites**: Same portfolio

### Steps:
1. [ ] Click "+ 종목 추가"
2. [ ] Click "₿ Crypto" tab
3. [ ] **Change baseCurrency to USD**
4. [ ] Enter Symbol: `ETH`
5. [ ] Enter Quantity: `2.5`
6. [ ] Enter Average Cost: `2500`
7. [ ] Click "추가"

### Expected Results:
- [ ] Request body has `"baseCurrency": "USD"`
- [ ] ETH price displays with $ symbol
- [ ] No errors

**Status**: ⬜ PASS ⬜ FAIL
**Notes**:
```


```

---

## 5. HIGH: Asset Type Switching (3 min)

**Prerequisites**: Modal open

### Steps:
1. [ ] Start on Stock tab
2. [ ] Enter Symbol: `TSLA`, Quantity: `5`
3. [ ] Switch to Crypto tab
4. [ ] **Verify form fields are EMPTY**
5. [ ] baseCurrency dropdown is visible
6. [ ] Enter Symbol: `BTC`
7. [ ] Switch back to Stock tab
8. [ ] **Verify form fields are EMPTY again**
9. [ ] baseCurrency dropdown is HIDDEN

### Expected Results:
- [ ] Form resets on every tab switch
- [ ] No data persists between switches
- [ ] baseCurrency only shows for Crypto

**Status**: ⬜ PASS ⬜ FAIL
**Notes**:
```


```

---

## 6. CRITICAL: PriceDisplay Null Handling (5 min)

**Prerequisites**: Portfolio with holdings

### Scenario A: Valid Price
1. [ ] AAPL holding shows real price (e.g., $150.25)
2. [ ] No console errors

### Scenario B: Failed Price (Try adding these cryptos)
1. [ ] Add crypto: `SOL` (Solana)
2. [ ] Add crypto: `ADA` (Cardano)
3. [ ] Add crypto: `DOGE` (Dogecoin)

### Expected Results:
- [ ] If price fails to load, display shows **"-"**
- [ ] **Never shows "NaN"**
- [ ] **Never shows "undefined"**
- [ ] **Never shows "null"**
- [ ] No console error: "Cannot read property of null"
- [ ] Page remains functional

**Status**: ⬜ PASS ⬜ FAIL
**Console Errors**:
```


```

---

## 7. HIGH: Form Validation - Stock (3 min)

**Prerequisites**: Modal open, Stock tab

### Test Cases:
1. [ ] Leave Symbol empty → Click "추가" → Error shown
2. [ ] Symbol: `AAPL`, Quantity: `0` → Error shown
3. [ ] Symbol: `AAPL`, Quantity: `-5` → Error shown
4. [ ] Symbol: `AAPL`, Quantity: `10`, Cost: `0` → Error shown
5. [ ] Symbol: `AAPL`, Quantity: `10`, Cost: `-100` → Error shown
6. [ ] Valid inputs → No errors, modal closes

**Status**: ⬜ PASS ⬜ FAIL
**Notes**:
```


```

---

## 8. HIGH: Form Validation - Crypto (3 min)

**Prerequisites**: Modal open, Crypto tab

### Test Cases:
1. [ ] Leave Symbol empty → Error shown
2. [ ] Symbol: `BTC`, Quantity: `0` → Error shown
3. [ ] Symbol: `BTC`, Quantity: `0.00001` → **ACCEPTS** (crypto allows tiny fractions)
4. [ ] Symbol: `BTC`, Quantity: `1`, Cost: `0` → Error shown

**Status**: ⬜ PASS ⬜ FAIL
**Notes**:
```


```

---

## 9. MEDIUM: Modal Close Behavior (2 min)

**Prerequisites**: Modal open

### Steps:
1. [ ] Enter Symbol: `AAPL`, Quantity: `10`
2. [ ] Click **outside modal** (on backdrop)
3. [ ] Modal closes
4. [ ] Reopen modal
5. [ ] Form is **empty** (data not saved)

**Status**: ⬜ PASS ⬜ FAIL
**Notes**:
```


```

---

## 10. HIGH: Session Persistence (2 min)

**Prerequisites**: Logged in, portfolio created

### Steps:
1. [ ] Refresh page (F5)
2. [ ] Still logged in (no redirect to /login)
3. [ ] Portfolio still visible
4. [ ] Navigate to `/market` → Still logged in
5. [ ] Navigate to `/lab` → Still logged in

**Status**: ⬜ PASS ⬜ FAIL
**Notes**:
```


```

---

## 11. REGRESSION: AI Analysis Modal (3 min)

**Prerequisites**: Portfolio with at least 1 holding

### Steps:
1. [ ] Click "🤖 AI 분석" button
2. [ ] Modal opens
3. [ ] 5 analysis types visible:
   - [ ] 전체 개요
   - [ ] 다각화 분석
   - [ ] 리스크 평가
   - [ ] 성과 분석
   - [ ] 투자 제안
4. [ ] Select one and click "분석 시작"
5. [ ] Analysis runs (or shows error if API key missing)

**Status**: ⬜ PASS ⬜ FAIL ⬜ BLOCKED (API key issue)
**Notes**:
```


```

---

## 12. REGRESSION: Rebalancing Modal (3 min)

**Prerequisites**: Portfolio with at least 2 holdings

### Steps:
1. [ ] Click "리밸런싱" button
2. [ ] Modal opens
3. [ ] Strategy dropdown visible
4. [ ] Threshold input visible
5. [ ] Tax checkbox visible
6. [ ] Click "추천 생성"
7. [ ] Results display (or error if not enough data)

**Status**: ⬜ PASS ⬜ FAIL
**Notes**:
```


```

---

## 13. REGRESSION: Backtest Lab Page (3 min)

**Prerequisites**: At least 1 portfolio exists

### Steps:
1. [ ] Navigate to `/lab`
2. [ ] Page loads
3. [ ] Portfolio dropdown populated
4. [ ] Date range inputs visible
5. [ ] Rebalancing frequency buttons visible
6. [ ] No console errors on load

**Status**: ⬜ PASS ⬜ FAIL
**Console Errors**: ⬜ None ⬜ Found: _______________

---

## 14. REGRESSION: Market Page (1 min)

### Steps:
1. [ ] Navigate to `/market`
2. [ ] Market indices load (S&P500, NASDAQ, DOW, KOSPI)
3. [ ] No errors

**Status**: ⬜ PASS ⬜ FAIL

---

## 15. EDGE CASE: Rapid Tab Switching (2 min)

### Steps:
1. [ ] Open modal
2. [ ] **Rapidly switch between Stock ↔ Crypto tabs** (10 times fast)
3. [ ] Form should reset each time
4. [ ] No console errors
5. [ ] No UI glitches

**Status**: ⬜ PASS ⬜ FAIL
**Console Errors**:
```


```

---

## 16. EDGE CASE: Multiple Holdings Same Symbol (2 min)

### Steps:
1. [ ] Add AAPL with Quantity: 10, Cost: 150
2. [ ] Add AAPL again with Quantity: 5, Cost: 160
3. [ ] Both holdings appear in list
4. [ ] No errors

**Status**: ⬜ PASS ⬜ FAIL
**Notes**:
```


```

---

## Summary

**Total Tests**: 16
**Passed**: _____
**Failed**: _____
**Blocked**: _____

**Critical Issues Found**:
```
1.

2.

3.
```

**Recommendations**:
```
1.

2.

3.
```

**Overall Assessment**: ⬜ Ready for Production ⬜ Needs Fixes ⬜ Major Issues

**Tester Signature**: _____________ **Date**: _____________
