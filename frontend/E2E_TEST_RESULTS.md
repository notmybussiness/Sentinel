# E2E Test Results - Crypto Holdings Integration

**Date**: 2025-10-17
**Test Environment**: localhost:8080 (backend), localhost:3000 (frontend)
**Framework**: Playwright

## Summary

**Total Tests**: 12 (4 login, 8 crypto holdings)
**Passed**: 4 (all login tests)
**Failed**: 8 (all crypto holdings tests)
**Success Rate**: 33% (4/12)

## Test Results

### ✅ Login Tests (4/4 Passed)
All basic authentication and login functionality tests pass successfully:

1. ✅ **should load login page** (674ms)
   - Verifies login page loads with correct title
   - Checks for presence of Kakao and Dev login buttons

2. ✅ **should perform dev mode login successfully** (1.2s)
   - Tests dev mode login functionality
   - Verifies successful redirect to homepage

3. ✅ **should have auth token after dev login** (912ms)
   - Confirms JWT tokens are stored in localStorage
   - Validates user data structure (email, name)

4. ✅ **should redirect to login page when not authenticated** (720ms)
   - Tests protected route behavior
   - Verifies redirect for unauthenticated access

### ❌ Crypto Holdings Tests (0/8 Passed)
All crypto holdings integration tests timeout at portfolio creation:

1. ❌ **should show crypto add button in portfolio detail** (30.0s timeout)
   - Error: Timeout waiting for "+ 새 포트폴리오" button

2. ❌ **should open AddCryptoHoldingModal when clicking crypto add button** (30.0s timeout)
   - Error: Timeout at portfolio creation step

3. ❌ **should search for crypto and display results** (30.0s timeout)
   - Error: Timeout at portfolio creation step

4. ❌ **should add crypto holding to portfolio** (30.0s timeout)
   - Error: Timeout at portfolio creation step

5. ❌ **should display crypto holding with correct asset type** (30.0s timeout)
   - Error: Timeout at portfolio creation step

6. ❌ **should switch between KRW and USD base currency** (30.0s timeout)
   - Error: Timeout at portfolio creation step

7. ❌ **should validate crypto holding form inputs** (30.0s timeout)
   - Error: Timeout at portfolio creation step

8. ❌ **should handle crypto search debouncing** (30.0s timeout)
   - Error: Timeout at portfolio creation step

## Issues Identified

### Root Cause
All crypto holdings tests fail at the `createTestPortfolio()` helper function, specifically when trying to click the "+ 새 포트폴리오" button on the `/portfolios` page.

**Error Pattern**:
```
Test timeout of 30000ms exceeded.
Error: locator.click: Target page, context or browser has been closed
Call log:
  - waiting for getByText('+ 새 포트폴리오')
```

### Possible Causes

1. **API Response Delay**
   - The portfolios API (`GET /api/v1/portfolios`) may be slow or failing
   - Frontend may be waiting indefinitely for portfolio data

2. **Authentication State**
   - JWT token from dev login may not persist correctly between page navigations
   - Auth context may not propagate quickly enough after `beforeEach` login

3. **Page Rendering Issues**
   - The "+ 새 포트폴리오" button may not render if portfolios API fails
   - React Query may be preventing render until API response

4. **Selector Issues**
   - Button selector `getByText('+ 새 포트폴리오')` may need adjustment
   - Portfolio card selector `.cursor-pointer` may match unintended elements

## Test Coverage

### Implemented Test Scenarios

The crypto holdings test file (`crypto-holdings.spec.ts`) covers:

1. **UI Visibility**
   - Crypto add button presence in portfolio detail
   - AddCryptoHoldingModal opening/closing

2. **Search Functionality**
   - Crypto search API integration
   - Search results display
   - Search debouncing (300ms)

3. **Form Validation**
   - Symbol selection requirement
   - Quantity validation (> 0)
   - Average cost validation

4. **Currency Selection**
   - KRW/USD base currency toggle
   - Currency state persistence

5. **Data Integration**
   - Adding crypto holding to portfolio
   - Displaying crypto with correct asset type
   - Real-time price updates (implied)

## Recommendations

### Short-term Fixes

1. **Increase Timeouts**
   - Current: 30s default timeout
   - Recommended: 60s for API-dependent operations

2. **Add Explicit Waits**
   - Wait for network idle after login
   - Wait for portfolio list API response before clicking
   - Add retry logic for flaky selectors

3. **Mock API Responses**
   - Use Playwright's `route()` to mock `/api/v1/portfolios` response
   - Reduce dependency on backend availability

4. **Simplify Test Setup**
   - Use fixture data instead of creating portfolios dynamically
   - Pre-populate database with test portfolio

### Long-term Improvements

1. **Add Test IDs**
   - Add `data-testid` attributes to key UI elements:
     - Portfolio cards: `data-testid="portfolio-card-{id}"`
     - Crypto add button: `data-testid="crypto-add-button"`
     - Search results: `data-testid="crypto-search-result"`

2. **API Testing**
   - Separate E2E tests from API integration tests
   - Add backend E2E tests for crypto endpoints
   - Verify API responses before running frontend tests

3. **Test Database**
   - Use separate test database for E2E tests
   - Reset state between test runs
   - Seed with predictable fixture data

4. **CI/CD Integration**
   - Run tests in headless mode
   - Generate HTML reports automatically
   - Set up test environment in CI pipeline

## Files Created

- `frontend/e2e/crypto-holdings.spec.ts` - Comprehensive crypto holdings E2E tests (270 lines)

## Next Steps

1. **Debug Portfolio Creation**
   - Manually test portfolio creation flow in browser
   - Check browser DevTools console for API errors
   - Verify JWT token is sent with `/portfolios` request

2. **Adjust Test Selectors**
   - Inspect actual DOM structure of portfolios page
   - Use more specific selectors or add test IDs
   - Consider using `page.getByRole()` for better accessibility

3. **Run Individual Test**
   - Isolate one test case and debug step-by-step
   - Add console.log statements to understand failure point
   - Use Playwright's `--debug` flag for visual debugging

4. **Consider Test Strategy**
   - May need to restructure tests to avoid portfolio creation overhead
   - Consider component-level tests for crypto modal
   - Reserve E2E for critical user journeys only

## Server Status

✅ **Backend**: Running on port 8080 (Spring Boot 3.5.5)
✅ **Frontend**: Running on port 3000 (Next.js 14)

Both servers started successfully and remained stable throughout test execution.
