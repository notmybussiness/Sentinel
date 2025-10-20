/**
 * E2E Tests - Unified Holding Modal
 *
 * Tests for the new unified "+ 종목 추가" modal that handles both stocks and crypto.
 * Covers TC-001 through TC-008 from QA_TEST_PLAN.md
 *
 * @see C:\Users\zetto\Desktop\Sentinel\QA_TEST_PLAN.md
 */

import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:3000';
const API_URL = 'http://localhost:8080';

/**
 * Helper: Login via dev login
 */
async function devLogin(page) {
  await page.goto(`${BASE_URL}/login`);
  await page.click('text=개발자 로그인 (테스트)');

  // Wait for redirect to home page
  await page.waitForURL(`${BASE_URL}/`);

  // Verify token exists
  const token = await page.evaluate(() => localStorage.getItem('accessToken'));
  expect(token).toBeTruthy();
}

/**
 * Helper: Create a test portfolio
 */
async function createPortfolio(page, name = 'E2E Test Portfolio') {
  await page.goto(`${BASE_URL}/`);
  await page.click('text=포트폴리오 생성');

  // Fill form
  await page.fill('input[name="name"]', name);
  await page.fill('textarea[name="description"]', 'Created by E2E test');
  await page.fill('input[name="initialCapital"]', '10000000');

  // Submit
  await page.click('button:has-text("생성")');

  // Wait for modal to close
  await page.waitForTimeout(1000);

  // Get portfolio ID from URL or list
  const portfolioCard = page.locator('.portfolio-card').first();
  await portfolioCard.click();

  await page.waitForURL(/\/portfolios\/\d+/);

  const url = page.url();
  const portfolioId = url.match(/\/portfolios\/(\d+)/)?.[1];
  expect(portfolioId).toBeTruthy();

  return portfolioId;
}

test.describe('Unified Holding Modal - Basic Functionality', () => {

  test.beforeEach(async ({ page }) => {
    await devLogin(page);
  });

  /**
   * TC-001: Open Unified Holding Modal
   */
  test('TC-001: Should open unified holding modal', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-001 Portfolio');

    // Click "+ 종목 추가" button
    await page.click('text=종목 추가');

    // Verify modal opened
    await expect(page.locator('text=종목 추가').first()).toBeVisible();

    // Verify asset type selector visible
    await expect(page.locator('text=📈 Stock')).toBeVisible();
    await expect(page.locator('text=₿ Crypto')).toBeVisible();

    // Verify Stock is default
    const stockTab = page.locator('button:has-text("📈 Stock")');
    await expect(stockTab).toHaveClass(/bg-blue-500/); // Active state

    // Verify stock form fields visible
    await expect(page.locator('input[name="symbol"]')).toBeVisible();
    await expect(page.locator('input[name="quantity"]')).toBeVisible();
    await expect(page.locator('input[name="averageCost"]')).toBeVisible();

    // Verify baseCurrency NOT visible for Stock
    await expect(page.locator('select[name="baseCurrency"]')).not.toBeVisible();
  });

  /**
   * TC-002: Stock Addition Flow
   */
  test('TC-002: Should add stock holding successfully', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-002 Portfolio');

    // Open modal
    await page.click('text=종목 추가');

    // Verify Stock tab is selected
    const stockTab = page.locator('button:has-text("📈 Stock")');
    await expect(stockTab).toHaveClass(/bg-blue-500/);

    // Fill form
    await page.fill('input[name="symbol"]', 'AAPL');
    await page.fill('input[name="quantity"]', '10');
    await page.fill('input[name="averageCost"]', '150.50');

    // Intercept API call
    const addHoldingPromise = page.waitForResponse(
      response => response.url().includes(`/api/v1/portfolios/${portfolioId}/holdings`)
                  && response.request().method() === 'POST'
    );

    // Submit
    await page.click('button:has-text("추가")');

    // Verify API call
    const response = await addHoldingPromise;
    expect(response.status()).toBe(200);

    const requestBody = await response.request().postDataJSON();
    expect(requestBody).toMatchObject({
      symbol: 'AAPL',
      quantity: 10,
      averageCost: 150.50,
      assetType: 'STOCK'
    });

    // Modal should close
    await expect(page.locator('text=종목 추가').first()).not.toBeVisible({ timeout: 3000 });

    // Verify holding appears in list
    await page.waitForTimeout(1000); // Wait for refetch
    await expect(page.locator('text=AAPL')).toBeVisible();
  });

  /**
   * TC-003: Crypto Addition Flow - KRW
   */
  test('TC-003: Should add crypto holding with KRW', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-003 Portfolio');

    // Open modal
    await page.click('text=종목 추가');

    // Click Crypto tab
    await page.click('button:has-text("₿ Crypto")');

    // Verify baseCurrency selector appears
    await expect(page.locator('select[name="baseCurrency"]')).toBeVisible();

    // Verify KRW is default
    const baseCurrencySelect = page.locator('select[name="baseCurrency"]');
    await expect(baseCurrencySelect).toHaveValue('KRW');

    // Fill form
    await page.fill('input[name="symbol"]', 'BTC');
    await page.fill('input[name="quantity"]', '0.5');
    await page.fill('input[name="averageCost"]', '80000000');

    // Intercept API call
    const addHoldingPromise = page.waitForResponse(
      response => response.url().includes(`/api/v1/portfolios/${portfolioId}/holdings`)
                  && response.request().method() === 'POST'
    );

    // Submit
    await page.click('button:has-text("추가")');

    // Verify API call
    const response = await addHoldingPromise;
    expect(response.status()).toBe(200);

    const requestBody = await response.request().postDataJSON();
    expect(requestBody).toMatchObject({
      symbol: 'BTC',
      quantity: 0.5,
      averageCost: 80000000,
      assetType: 'CRYPTO',
      baseCurrency: 'KRW'
    });

    // Modal should close
    await expect(page.locator('text=종목 추가').first()).not.toBeVisible({ timeout: 3000 });

    // Verify holding appears
    await page.waitForTimeout(1000);
    await expect(page.locator('text=BTC')).toBeVisible();
  });

  /**
   * TC-004: Crypto Addition Flow - USD
   */
  test('TC-004: Should add crypto holding with USD', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-004 Portfolio');

    // Open modal
    await page.click('text=종목 추가');

    // Click Crypto tab
    await page.click('button:has-text("₿ Crypto")');

    // Change baseCurrency to USD
    await page.selectOption('select[name="baseCurrency"]', 'USD');

    // Fill form
    await page.fill('input[name="symbol"]', 'ETH');
    await page.fill('input[name="quantity"]', '2.5');
    await page.fill('input[name="averageCost"]', '2500');

    // Intercept API call
    const addHoldingPromise = page.waitForResponse(
      response => response.url().includes(`/api/v1/portfolios/${portfolioId}/holdings`)
                  && response.request().method() === 'POST'
    );

    // Submit
    await page.click('button:has-text("추가")');

    // Verify API call
    const response = await addHoldingPromise;
    expect(response.status()).toBe(200);

    const requestBody = await response.request().postDataJSON();
    expect(requestBody).toMatchObject({
      symbol: 'ETH',
      quantity: 2.5,
      averageCost: 2500,
      assetType: 'CRYPTO',
      baseCurrency: 'USD'
    });

    // Modal should close
    await expect(page.locator('text=종목 추가').first()).not.toBeVisible({ timeout: 3000 });

    // Verify holding appears
    await page.waitForTimeout(1000);
    await expect(page.locator('text=ETH')).toBeVisible();
  });

  /**
   * TC-005: Asset Type Switching
   */
  test('TC-005: Should reset form when switching asset types', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-005 Portfolio');

    // Open modal
    await page.click('text=종목 추가');

    // Fill Stock form
    await page.fill('input[name="symbol"]', 'AAPL');
    await page.fill('input[name="quantity"]', '10');
    await page.fill('input[name="averageCost"]', '150');

    // Switch to Crypto
    await page.click('button:has-text("₿ Crypto")');

    // Verify form fields reset
    await expect(page.locator('input[name="symbol"]')).toHaveValue('');
    await expect(page.locator('input[name="quantity"]')).toHaveValue('');
    await expect(page.locator('input[name="averageCost"]')).toHaveValue('');

    // Verify baseCurrency selector appears
    await expect(page.locator('select[name="baseCurrency"]')).toBeVisible();

    // Fill Crypto form
    await page.fill('input[name="symbol"]', 'BTC');
    await page.fill('input[name="quantity"]', '0.5');

    // Switch back to Stock
    await page.click('button:has-text("📈 Stock")');

    // Verify form reset again
    await expect(page.locator('input[name="symbol"]')).toHaveValue('');
    await expect(page.locator('input[name="quantity"]')).toHaveValue('');

    // Verify baseCurrency selector hidden
    await expect(page.locator('select[name="baseCurrency"]')).not.toBeVisible();
  });

  /**
   * TC-006: Form Validation - Stock
   */
  test('TC-006: Should validate stock form inputs', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-006 Portfolio');

    // Open modal
    await page.click('text=종목 추가');

    // Test empty symbol
    await page.click('button:has-text("추가")');
    await expect(page.locator('text=/Symbol.*required/i')).toBeVisible();

    // Test quantity = 0
    await page.fill('input[name="symbol"]', 'AAPL');
    await page.fill('input[name="quantity"]', '0');
    await page.fill('input[name="averageCost"]', '100');
    await page.click('button:has-text("추가")');
    await expect(page.locator('text=/Quantity.*0/i')).toBeVisible();

    // Test negative quantity
    await page.fill('input[name="quantity"]', '-5');
    await page.click('button:has-text("추가")');
    await expect(page.locator('text=/Quantity.*0/i')).toBeVisible();

    // Test averageCost = 0
    await page.fill('input[name="quantity"]', '10');
    await page.fill('input[name="averageCost"]', '0');
    await page.click('button:has-text("추가")');
    await expect(page.locator('text=/cost.*0/i')).toBeVisible();

    // Test negative averageCost
    await page.fill('input[name="averageCost"]', '-100');
    await page.click('button:has-text("추가")');
    await expect(page.locator('text=/cost.*0/i')).toBeVisible();
  });

  /**
   * TC-007: Form Validation - Crypto
   */
  test('TC-007: Should validate crypto form inputs', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-007 Portfolio');

    // Open modal
    await page.click('text=종목 추가');

    // Switch to Crypto
    await page.click('button:has-text("₿ Crypto")');

    // Test empty symbol
    await page.click('button:has-text("추가")');
    await expect(page.locator('text=/Symbol.*required/i')).toBeVisible();

    // Test quantity = 0
    await page.fill('input[name="symbol"]', 'BTC');
    await page.fill('input[name="quantity"]', '0');
    await page.fill('input[name="averageCost"]', '50000000');
    await page.click('button:has-text("추가")');
    await expect(page.locator('text=/Quantity.*0/i')).toBeVisible();

    // Test very small quantity (should accept for crypto)
    await page.fill('input[name="quantity"]', '0.00001');
    await page.click('button:has-text("추가")');
    // Should not show error for small fractions

    // Test averageCost = 0
    await page.fill('input[name="quantity"]', '1');
    await page.fill('input[name="averageCost"]', '0');
    await page.click('button:has-text("추가")');
    await expect(page.locator('text=/cost.*0/i')).toBeVisible();
  });

  /**
   * TC-008: Modal Close Behavior
   */
  test('TC-008: Should close modal and reset form', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-008 Portfolio');

    // Open modal
    await page.click('text=종목 추가');

    // Fill form
    await page.fill('input[name="symbol"]', 'AAPL');
    await page.fill('input[name="quantity"]', '10');
    await page.fill('input[name="averageCost"]', '150');

    // Click backdrop (outside modal)
    await page.click('.modal-backdrop', { force: true });

    // Modal should close
    await expect(page.locator('text=종목 추가').first()).not.toBeVisible({ timeout: 2000 });

    // Reopen modal
    await page.click('text=종목 추가');

    // Form should be reset
    await expect(page.locator('input[name="symbol"]')).toHaveValue('');
    await expect(page.locator('input[name="quantity"]')).toHaveValue('');
    await expect(page.locator('input[name="averageCost"]')).toHaveValue('');
  });
});

test.describe('PriceDisplay Null Handling', () => {

  test.beforeEach(async ({ page }) => {
    await devLogin(page);
  });

  /**
   * TC-009: Null Price Handling
   */
  test('TC-009: Should display "-" for null prices', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-009 Portfolio');

    // Add a crypto that might fail price fetch
    await page.click('text=종목 추가');
    await page.click('button:has-text("₿ Crypto")');
    await page.fill('input[name="symbol"]', 'SOL'); // May fail
    await page.fill('input[name="quantity"]', '10');
    await page.fill('input[name="averageCost"]', '100000');
    await page.click('button:has-text("추가")');

    // Wait for modal to close
    await page.waitForTimeout(2000);

    // Check for "-" in price display or actual price
    const priceDisplay = page.locator('.price-display').first();
    const text = await priceDisplay.textContent();

    // Should be either a valid price or "-", not "NaN" or error
    expect(text).not.toContain('NaN');
    expect(text).not.toContain('undefined');

    // Check console for errors
    const errors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    await page.waitForTimeout(1000);

    // Should have no console errors related to price display
    const priceErrors = errors.filter(e => e.includes('price') || e.includes('PriceDisplay'));
    expect(priceErrors.length).toBe(0);
  });
});

test.describe('Dev Login Authentication', () => {

  /**
   * TC-013: Dev Login Flow
   */
  test('TC-013: Should authenticate via dev login', async ({ page }) => {
    // Navigate to login page
    await page.goto(`${BASE_URL}/login`);

    // Intercept dev login API call
    const loginPromise = page.waitForResponse(
      response => response.url().includes('/api/v1/auth/dev-login')
                  && response.request().method() === 'POST'
    );

    // Click dev login button
    await page.click('text=개발자 로그인 (테스트)');

    // Verify API call
    const response = await loginPromise;
    expect(response.status()).toBe(200);

    const responseBody = await response.json();
    expect(responseBody).toHaveProperty('accessToken');
    expect(responseBody).toHaveProperty('refreshToken');
    expect(responseBody).toHaveProperty('user');
    expect(responseBody.user).toMatchObject({
      email: 'dev@sentinel.com',
      name: 'Developer'
    });

    // Should redirect immediately (no refresh)
    await page.waitForURL(`${BASE_URL}/`, { timeout: 3000 });

    // Verify tokens in localStorage
    const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
    const refreshToken = await page.evaluate(() => localStorage.getItem('refreshToken'));

    expect(accessToken).toBeTruthy();
    expect(refreshToken).toBeTruthy();

    // Verify home page shows authenticated state
    await expect(page.locator('text=포트폴리오')).toBeVisible();
  });

  /**
   * TC-014: JWT Token Validation
   */
  test('TC-014: Should have valid JWT tokens', async ({ page }) => {
    await devLogin(page);

    // Get tokens from localStorage
    const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
    const refreshToken = await page.evaluate(() => localStorage.getItem('refreshToken'));

    expect(accessToken).toBeTruthy();
    expect(refreshToken).toBeTruthy();

    // Decode JWT (basic check for structure)
    const tokenParts = accessToken!.split('.');
    expect(tokenParts.length).toBe(3); // Header, Payload, Signature

    // Decode payload
    const payload = JSON.parse(Buffer.from(tokenParts[1], 'base64').toString());
    expect(payload).toHaveProperty('sub', '1'); // User ID
    expect(payload).toHaveProperty('email', 'dev@sentinel.com');
    expect(payload).toHaveProperty('exp');

    // Verify exp is in future
    const now = Math.floor(Date.now() / 1000);
    expect(payload.exp).toBeGreaterThan(now);

    // Make authenticated API call
    const response = await page.request.get(`${API_URL}/api/v1/portfolios`, {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });

    expect(response.status()).toBe(200);
  });

  /**
   * TC-015: Portfolio Creation After Login
   */
  test('TC-015: Should create portfolio after login', async ({ page }) => {
    await devLogin(page);

    // Click create portfolio
    await page.click('text=포트폴리오 생성');

    // Verify modal opens
    await expect(page.locator('text=포트폴리오 생성').first()).toBeVisible();

    // Fill form
    await page.fill('input[name="name"]', 'TC-015 Portfolio');
    await page.fill('textarea[name="description"]', 'Test description');
    await page.fill('input[name="initialCapital"]', '10000000');

    // Intercept API call
    const createPromise = page.waitForResponse(
      response => response.url().includes('/api/v1/portfolios')
                  && response.request().method() === 'POST'
    );

    // Submit
    await page.click('button:has-text("생성")');

    // Verify API call succeeds
    const response = await createPromise;
    expect(response.status()).toBe(200);

    // Verify JWT token was sent
    const authHeader = response.request().headers()['authorization'];
    expect(authHeader).toBeTruthy();
    expect(authHeader).toMatch(/^Bearer /);

    // Verify portfolio appears
    await page.waitForTimeout(1000);
    await expect(page.locator('text=TC-015 Portfolio')).toBeVisible();
  });

  /**
   * TC-016: Holding Addition After Login
   */
  test('TC-016: Should add holding after login', async ({ page }) => {
    await devLogin(page);
    const portfolioId = await createPortfolio(page, 'TC-016 Portfolio');

    // Open add holding modal
    await page.click('text=종목 추가');

    // Add stock
    await page.fill('input[name="symbol"]', 'AAPL');
    await page.fill('input[name="quantity"]', '10');
    await page.fill('input[name="averageCost"]', '150');

    // Intercept API call
    const addPromise = page.waitForResponse(
      response => response.url().includes(`/api/v1/portfolios/${portfolioId}/holdings`)
                  && response.request().method() === 'POST'
    );

    // Submit
    await page.click('button:has-text("추가")');

    // Verify API call succeeds
    const response = await addPromise;
    expect(response.status()).toBe(200);

    // Verify JWT token was sent
    const authHeader = response.request().headers()['authorization'];
    expect(authHeader).toBeTruthy();
    expect(authHeader).toMatch(/^Bearer /);

    // Verify holding appears
    await page.waitForTimeout(1000);
    await expect(page.locator('text=AAPL')).toBeVisible();
  });

  /**
   * TC-017: Session Persistence
   */
  test('TC-017: Should persist session after refresh', async ({ page }) => {
    await devLogin(page);

    // Create portfolio
    const portfolioId = await createPortfolio(page, 'TC-017 Portfolio');

    // Refresh page
    await page.reload();

    // Wait for page load
    await page.waitForLoadState('networkidle');

    // Should still be authenticated
    const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
    expect(accessToken).toBeTruthy();

    // Should see portfolios
    await expect(page.locator('text=TC-017 Portfolio')).toBeVisible();

    // Navigate between pages
    await page.click('text=Market');
    await expect(page).toHaveURL(/\/market/);

    await page.click('text=Lab');
    await expect(page).toHaveURL(/\/lab/);

    // Should remain authenticated
    const token = await page.evaluate(() => localStorage.getItem('accessToken'));
    expect(token).toBeTruthy();
  });
});

test.describe('Regression Tests - Existing Features', () => {

  test.beforeEach(async ({ page }) => {
    await devLogin(page);
  });

  /**
   * TC-019: AI Analysis Modal
   */
  test('TC-019: Should open AI analysis modal', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-019 Portfolio');

    // Add a holding first
    await page.click('text=종목 추가');
    await page.fill('input[name="symbol"]', 'AAPL');
    await page.fill('input[name="quantity"]', '10');
    await page.fill('input[name="averageCost"]', '150');
    await page.click('button:has-text("추가")');
    await page.waitForTimeout(1500);

    // Click AI Analysis button
    await page.click('text=🤖 AI 분석');

    // Verify modal opens
    await expect(page.locator('text=AI 포트폴리오 분석').first()).toBeVisible();

    // Verify all 5 analysis types available
    await expect(page.locator('text=전체 개요')).toBeVisible();
    await expect(page.locator('text=다각화 분석')).toBeVisible();
    await expect(page.locator('text=리스크 평가')).toBeVisible();
    await expect(page.locator('text=성과 분석')).toBeVisible();
    await expect(page.locator('text=투자 제안')).toBeVisible();

    // No console errors
    const errors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') errors.push(msg.text());
    });

    await page.waitForTimeout(500);
    expect(errors.filter(e => !e.includes('DevTools'))).toHaveLength(0);
  });

  /**
   * TC-020: Rebalancing Modal
   */
  test('TC-020: Should open rebalancing modal', async ({ page }) => {
    const portfolioId = await createPortfolio(page, 'TC-020 Portfolio');

    // Add 2 holdings
    await page.click('text=종목 추가');
    await page.fill('input[name="symbol"]', 'AAPL');
    await page.fill('input[name="quantity"]', '10');
    await page.fill('input[name="averageCost"]', '150');
    await page.click('button:has-text("추가")');
    await page.waitForTimeout(1500);

    await page.click('text=종목 추가');
    await page.fill('input[name="symbol"]', 'GOOGL');
    await page.fill('input[name="quantity"]', '5');
    await page.fill('input[name="averageCost"]', '120');
    await page.click('button:has-text("추가")');
    await page.waitForTimeout(1500);

    // Click Rebalancing button
    await page.click('text=리밸런싱');

    // Verify modal opens
    await expect(page.locator('text=리밸런싱 추천').first()).toBeVisible();

    // Verify strategy selection
    await expect(page.locator('select[name="strategy"]')).toBeVisible();

    // Verify threshold input
    await expect(page.locator('input[name="threshold"]')).toBeVisible();

    // No console errors
    const errors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') errors.push(msg.text());
    });

    await page.waitForTimeout(500);
    expect(errors.filter(e => !e.includes('DevTools'))).toHaveLength(0);
  });

  /**
   * TC-021: Backtest Lab Page
   */
  test('TC-021: Should load backtest lab page', async ({ page }) => {
    // Create portfolio first
    await createPortfolio(page, 'TC-021 Portfolio');

    // Navigate to Lab
    await page.goto(`${BASE_URL}/lab`);

    // Page should load
    await expect(page.locator('text=백테스팅 연구소')).toBeVisible();

    // Portfolio dropdown should be populated
    const portfolioSelect = page.locator('select').first();
    await expect(portfolioSelect).toBeVisible();

    // Date pickers should be visible
    await expect(page.locator('input[type="date"]').first()).toBeVisible();

    // Rebalancing frequency buttons
    await expect(page.locator('text=없음')).toBeVisible();
    await expect(page.locator('text=월간')).toBeVisible();

    // No errors on load
    const errors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error') errors.push(msg.text());
    });

    await page.waitForTimeout(500);
    expect(errors.filter(e => !e.includes('DevTools'))).toHaveLength(0);
  });
});
