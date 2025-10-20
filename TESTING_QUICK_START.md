# Testing Quick Start Guide - Sentinel
**Last Updated**: 2025-10-19
**For**: QA Team, Developers, Product Owners

---

## Prerequisites (5 min)

### 1. Environment Running
```bash
# Terminal 1 - Backend (CMD/PowerShell, NOT Git Bash)
cd C:\Users\zetto\Desktop\Sentinel\backend
gradlew bootRun

# Wait for: "Started SentinelApplication in X.XXX seconds"
# Verify: http://localhost:8080/actuator/health
```

```bash
# Terminal 2 - Frontend
cd C:\Users\zetto\Desktop\Sentinel\frontend
npm run dev

# Wait for: "ready - started server on 0.0.0.0:3000"
# Verify: http://localhost:3000
```

### 2. Dependencies Installed
```bash
cd frontend
npm install                    # Install dependencies
npx playwright install         # Install browser drivers
```

---

## Option A: Automated E2E Tests (30 min)

**Best for**: CI/CD, regression testing, quick validation

### Run All Tests
```bash
cd frontend
npm run test:e2e
```

**Expected Output**:
```
Running 21 tests using 3 workers

  ✓ TC-001: Should open unified holding modal (5s)
  ✓ TC-002: Should add stock holding successfully (8s)
  ✓ TC-003: Should add crypto holding with KRW (8s)
  ...

  21 passed (3m)
```

### Run Specific Test Suite
```bash
# Unified modal tests only
npx playwright test unified-holding-modal

# Specific test case
npx playwright test -g "TC-002"

# Debug mode (interactive)
npx playwright test --ui

# Headed mode (see browser)
npx playwright test --headed
```

### View Test Report
```bash
npx playwright show-report
```

**Opens**: HTML report in browser with:
- Test results summary
- Screenshots on failure
- Network requests
- Console logs
- Video recordings (if enabled)

### Interpret Results

✅ **All Green (21/21 passed)**: Ready for production
- Review report for any warnings
- Check response times (<3s per test)
- Proceed to manual validation (critical path only)

⚠️ **Some Failures (18-20 passed)**: Investigate
- Review failed test screenshots
- Check console errors
- Verify API responses
- Fix bugs and re-run

❌ **Many Failures (<18 passed)**: Stop deployment
- Critical regression detected
- Review recent changes
- Run manual tests to confirm
- Fix all CRITICAL bugs before proceeding

---

## Option B: Manual Testing (60 min)

**Best for**: User acceptance, exploratory testing, UX validation

### Step 1: Open Checklist (2 min)
```bash
# Open in text editor
code C:\Users\zetto\Desktop\Sentinel\MANUAL_TEST_CHECKLIST.md

# Or in browser
start C:\Users\zetto\Desktop\Sentinel\MANUAL_TEST_CHECKLIST.md
```

### Step 2: Critical Path Only (20 min)
**If time is limited, test these 5 critical scenarios**:

1. ✅ **TC-001**: Open unified modal (2 min)
   - Navigate to portfolio detail
   - Click "+ 종목 추가"
   - Verify modal structure

2. ✅ **TC-002**: Add stock (5 min)
   - Add AAPL with quantity 10, cost 150
   - Verify API call succeeds
   - Verify holding appears

3. ✅ **TC-003**: Add crypto KRW (5 min)
   - Add BTC with quantity 0.5, cost 80000000, KRW
   - Verify API call includes baseCurrency
   - Verify price displays

4. ✅ **TC-009**: Null price handling (3 min)
   - Add crypto that may fail (SOL, ADA, DOGE)
   - Verify "-" displays, not "NaN"
   - Check console for errors

5. ✅ **TC-013**: Dev login (5 min)
   - Log out (clear localStorage)
   - Click "개발자 로그인"
   - Verify immediate redirect

**Pass Criteria**: 5/5 pass → Proceed to deployment
**Fail Criteria**: Any failures → Run full manual test suite

### Step 3: Full Manual Suite (60 min)
**Run all 16 test scenarios if**:
- Critical path has failures
- Major feature release
- Pre-production validation required

**Follow checklist**:
- Print or open `MANUAL_TEST_CHECKLIST.md`
- Check boxes as you complete tests
- Note any issues in "Notes" sections
- Fill out summary scorecard

---

## Option C: Hybrid Approach (40 min)

**Recommended for most deployments**:

### 1. Run Automated Tests (30 min)
```bash
cd frontend
npm run test:e2e
```

### 2. Manual Critical Path (10 min)
If automated tests pass, manually verify:
- ✅ Dev login UX (does it feel smooth?)
- ✅ Modal interaction (is it intuitive?)
- ✅ Error messages (are they user-friendly?)
- ✅ Visual design (any UI glitches?)

### 3. Decision Tree
```
Automated Tests Pass (21/21)?
├─ YES → Manual critical path pass?
│  ├─ YES → ✅ DEPLOY TO PRODUCTION
│  └─ NO  → 🔍 Investigate UX issues → Fix → Re-test
└─ NO  → ❌ STOP DEPLOYMENT
         → 🐛 File bugs using BUG_REPORT_TEMPLATE.md
         → 🔧 Fix bugs
         → 🔄 Re-run full test suite
```

---

## Found a Bug? (5 min)

### 1. Use Bug Template
```bash
# Open template
code C:\Users\zetto\Desktop\Sentinel\BUG_REPORT_TEMPLATE.md

# Copy blank template
# Fill in details
# Save as BUG-001.md, BUG-002.md, etc.
```

### 2. Bug Severity Guide
- **CRITICAL**: App crash, data loss, security issue → Fix immediately
- **HIGH**: Major feature broken, workaround exists → Fix before production
- **MEDIUM**: Minor feature broken → Fix in next sprint
- **LOW**: Cosmetic issue → Add to backlog

### 3. Report Bug
- Save filled template as `BUG-XXX.md`
- Share with development team
- Create GitHub issue (if using issue tracker)
- Link to test case that failed (e.g., "Found in TC-009")

---

## Test Scenarios by Priority

### CRITICAL (Must Pass Before Production)
- TC-001: Open unified modal
- TC-002: Stock addition
- TC-003: Crypto addition KRW
- TC-009: PriceDisplay null handling
- TC-013: Dev login flow
- TC-014: JWT token validation
- TC-015: Portfolio creation after login
- TC-016: Holding addition after login

**Pass Rate Required**: 8/8 (100%)

### HIGH (Should Pass Before Production)
- TC-004: Crypto USD
- TC-005: Asset type switching
- TC-006: Form validation - stock
- TC-007: Form validation - crypto
- TC-010: NaN price handling
- TC-011: Undefined price handling
- TC-017: Session persistence
- TC-019: AI analysis regression
- TC-020: Rebalancing regression
- TC-021: Backtest regression

**Pass Rate Required**: >90% (9/10)

### MEDIUM (Nice to Have)
- TC-008: Modal close behavior
- TC-012: Network failure handling
- TC-022: Market page regression
- TC-024: Cross-browser (Chrome)

**Pass Rate Required**: >75% (3/4)

### LOW (Enhancement Opportunities)
- TC-018: Logout flow
- TC-023: Holding edit/delete
- TC-025: Cross-browser (Firefox)
- TC-026: Cross-browser (Edge)

**Pass Rate Required**: >50% (1/2)

---

## Troubleshooting

### Automated Tests Failing

#### Problem: "Timeout waiting for element"
**Solution**:
```bash
# Increase timeout
npx playwright test --timeout=60000

# Run in headed mode to see what's happening
npx playwright test --headed

# Check if servers are running
curl http://localhost:8080/actuator/health
curl http://localhost:3000/api/health
```

#### Problem: "Browser not found"
**Solution**:
```bash
npx playwright install
npx playwright install-deps
```

#### Problem: "Network request failed"
**Solution**:
- Verify backend is running on port 8080
- Check API keys configured in `application-secret.yml`
- Check network tab in Playwright report

---

### Manual Tests Failing

#### Problem: Dev login not working
**Solution**:
1. Clear browser cache and localStorage
2. Verify `NEXT_PUBLIC_DEV_MODE=true` in `.env.local`
3. Check backend logs for errors
4. Verify `/api/v1/auth/dev-login` endpoint exists

#### Problem: Modal not opening
**Solution**:
1. Check browser console for errors
2. Verify React Query is initialized
3. Check if button click handler is attached
4. Try force refresh (Ctrl+F5)

#### Problem: Prices showing "-" for all holdings
**Solution**:
1. Check API keys configured (AlphaVantage, Upbit)
2. Check rate limits (AlphaVantage: 5/min, 100/day)
3. Verify network requests in browser DevTools
4. Check backend logs for API errors

---

## Performance Benchmarks

### Automated Test Suite
- **Total Duration**: 3-5 minutes (21 tests)
- **Average per Test**: 8-12 seconds
- **Success Rate**: Target >95% (20/21)

### Manual Testing
- **Critical Path**: 20 minutes (5 tests)
- **Full Suite**: 60 minutes (16 tests)
- **Success Rate**: Target >95% (15/16)

### API Response Times
- **Dev Login**: <500ms
- **Add Holding**: <1000ms
- **Fetch Portfolio**: <500ms
- **Crypto Price**: <2000ms (external API)

---

## Daily Testing Workflow

### Before Starting Work
```bash
# 1. Pull latest code
git pull origin dev

# 2. Install dependencies
cd frontend && npm install
cd backend && ./gradlew build

# 3. Start servers
# Backend: CMD/PowerShell
cd backend && gradlew bootRun

# Frontend: Git Bash/Terminal
cd frontend && npm run dev

# 4. Run smoke tests (5 min)
cd frontend
npx playwright test -g "TC-001|TC-002|TC-013"
```

### Before Committing Code
```bash
# 1. Lint and type check
npm run lint
npm run type-check

# 2. Run affected tests
npx playwright test unified-holding-modal

# 3. Manual spot check (2 min)
# - Open modal
# - Add one stock
# - Verify no console errors
```

### Before Merging to Main
```bash
# 1. Full E2E test suite
npm run test:e2e

# 2. Critical path manual (20 min)
# Follow MANUAL_TEST_CHECKLIST.md sections 1-5

# 3. Review test report
npx playwright show-report

# 4. Document any issues
# Use BUG_REPORT_TEMPLATE.md if needed
```

---

## CI/CD Integration (Future)

### GitHub Actions Workflow
```yaml
# .github/workflows/e2e-tests.yml
name: E2E Tests

on:
  pull_request:
    branches: [main, dev]
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - name: Install dependencies
        run: npm ci
      - name: Install Playwright
        run: npx playwright install --with-deps
      - name: Run E2E tests
        run: npm run test:e2e
      - name: Upload test report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: playwright-report/
```

---

## Quick Reference Card

### Test Commands
```bash
# All E2E tests
npm run test:e2e

# Specific test
npx playwright test -g "TC-002"

# UI mode (debugging)
npx playwright test --ui

# View report
npx playwright show-report
```

### Server Status
```bash
# Backend health
curl http://localhost:8080/actuator/health

# Frontend health
curl http://localhost:3000/api/health
```

### Test Files
- **Test Plan**: `QA_TEST_PLAN.md`
- **Manual Checklist**: `MANUAL_TEST_CHECKLIST.md`
- **Bug Template**: `BUG_REPORT_TEMPLATE.md`
- **E2E Tests**: `frontend/e2e/unified-holding-modal.spec.ts`

### Pass Criteria
- **Critical**: 100% (8/8)
- **High**: >90% (9/10)
- **Medium**: >75% (3/4)
- **Overall**: >95% (22/23)

---

**Questions?** Review `QA_DELIVERABLES_SUMMARY.md` for complete documentation.

**Ready to test?** Start with automated tests, then manual critical path.

**Found bugs?** Use `BUG_REPORT_TEMPLATE.md` to document.

---

**Last Updated**: 2025-10-19
**Maintainer**: QA Team
**Status**: Ready for Use
