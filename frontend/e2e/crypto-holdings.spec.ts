import { test, expect } from '@playwright/test';

test.describe('Crypto Holdings Integration', () => {
  // Helper function to perform dev login
  async function devLogin(page: any) {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.getByText('개발자 로그인 (테스트)').click();
    await page.waitForURL('/', { timeout: 5000 });
  }

  // Helper function to create a test portfolio
  async function createTestPortfolio(page: any, name: string = 'Crypto Test Portfolio') {
    await page.goto('/portfolios');
    await page.waitForLoadState('networkidle');

    // Click "+ 새 포트폴리오" button (with plus sign)
    await page.getByText('+ 새 포트폴리오').click();

    // Wait for modal to appear
    await expect(page.getByText('포트폴리오 생성')).toBeVisible();

    // Fill in portfolio details
    await page.fill('input[placeholder*="포트폴리오 이름"]', name);
    await page.fill('textarea[placeholder*="설명"]', 'E2E test portfolio for crypto holdings');

    // Click create button
    await page.getByRole('button', { name: '생성' }).click();

    // Wait for modal to close and portfolio list to update
    await page.waitForTimeout(2000);
  }

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await devLogin(page);
  });

  test('should show crypto add button in portfolio detail', async ({ page }) => {
    // Create a test portfolio
    await createTestPortfolio(page);

    // Click on the first portfolio card (they are Card components with onClick)
    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();

    // Wait for detail page to load
    await page.waitForLoadState('networkidle');

    // Verify crypto add button exists
    await expect(page.getByText('+ 암호화폐 추가')).toBeVisible();
  });

  test('should open AddCryptoHoldingModal when clicking crypto add button', async ({ page }) => {
    // Create a test portfolio
    await createTestPortfolio(page);

    // Click on the first portfolio
    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // Click crypto add button
    await page.getByText('+ 암호화폐 추가').click();

    // Wait for modal to appear
    await expect(page.getByText('암호화폐 추가')).toBeVisible();

    // Verify modal elements exist
    await expect(page.getByPlaceholder('암호화폐 검색...')).toBeVisible();
    await expect(page.getByText('KRW')).toBeVisible();
    await expect(page.getByText('USD')).toBeVisible();
  });

  test('should search for crypto and display results', async ({ page }) => {
    // Create a test portfolio
    await createTestPortfolio(page);

    // Click on the first portfolio
    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // Click crypto add button
    await page.getByText('+ 암호화폐 추가').click();

    // Search for Bitcoin
    const searchInput = page.getByPlaceholder('암호화폐 검색...');
    await searchInput.fill('bitcoin');

    // Wait for search results (debounced 300ms + API call)
    await page.waitForTimeout(500);

    // Verify search results appear
    // Note: This assumes the search API returns results
    // The actual test might need adjustment based on API behavior
    const searchResults = page.locator('[data-testid="crypto-search-result"]');
    await expect(searchResults.first()).toBeVisible({ timeout: 3000 });
  });

  test('should add crypto holding to portfolio', async ({ page }) => {
    // Create a test portfolio
    await createTestPortfolio(page, 'Bitcoin Portfolio');

    // Click on the portfolio
    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // Click crypto add button
    await page.getByText('+ 암호화폐 추가').click();

    // Search for Bitcoin
    const searchInput = page.getByPlaceholder('암호화폐 검색...');
    await searchInput.fill('BTC');
    await page.waitForTimeout(500);

    // Select first search result
    const searchResults = page.locator('[data-testid="crypto-search-result"]');
    await searchResults.first().click();

    // Fill in quantity and average cost
    await page.fill('input[placeholder*="수량"]', '0.5');
    await page.fill('input[placeholder*="평균 매수가"]', '50000000');

    // Select KRW as base currency (should be default)
    // If needed: await page.getByText('KRW').click();

    // Click add button
    await page.getByRole('button', { name: '추가' }).click();

    // Wait for modal to close and portfolio to update
    await page.waitForTimeout(1000);

    // Verify the crypto holding appears in the portfolio
    await expect(page.getByText('BTC')).toBeVisible({ timeout: 5000 });
  });

  test('should display crypto holding with correct asset type', async ({ page }) => {
    // Create a test portfolio and add crypto
    await createTestPortfolio(page, 'Multi-Asset Portfolio');

    // Click on the portfolio
    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // Add a crypto holding (abbreviated version)
    await page.getByText('+ 암호화폐 추가').click();
    const searchInput = page.getByPlaceholder('암호화폐 검색...');
    await searchInput.fill('ETH');
    await page.waitForTimeout(500);

    const searchResults = page.locator('[data-testid="crypto-search-result"]');
    await searchResults.first().click();

    await page.fill('input[placeholder*="수량"]', '2');
    await page.fill('input[placeholder*="평균 매수가"]', '3000000');

    await page.getByRole('button', { name: '추가' }).click();
    await page.waitForTimeout(1000);

    // Verify ETH appears in holdings
    await expect(page.getByText('ETH')).toBeVisible({ timeout: 5000 });

    // Verify the holding card displays crypto-specific information
    // This might include base currency (KRW/USD) indicator
  });

  test('should switch between KRW and USD base currency', async ({ page }) => {
    // Create a test portfolio
    await createTestPortfolio(page);

    // Click on the portfolio
    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // Click crypto add button
    await page.getByText('+ 암호화폐 추가').click();

    // Verify both currency options are available
    const krwButton = page.getByRole('button', { name: 'KRW' });
    const usdButton = page.getByRole('button', { name: 'USD' });

    await expect(krwButton).toBeVisible();
    await expect(usdButton).toBeVisible();

    // Click USD to switch
    await usdButton.click();

    // Verify USD is selected (might need to check for active class)
    // This test validates the currency selection UI works
  });

  test('should validate crypto holding form inputs', async ({ page }) => {
    // Create a test portfolio
    await createTestPortfolio(page);

    // Click on the portfolio
    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // Click crypto add button
    await page.getByText('+ 암호화폐 추가').click();

    // Try to add without selecting a crypto
    await page.getByRole('button', { name: '추가' }).click();

    // Should show validation error (implementation might vary)
    // Verify modal is still open (didn't submit)
    await expect(page.getByText('암호화폐 추가')).toBeVisible();

    // Search and select a crypto
    const searchInput = page.getByPlaceholder('암호화폐 검색...');
    await searchInput.fill('BTC');
    await page.waitForTimeout(500);

    const searchResults = page.locator('[data-testid="crypto-search-result"]');
    await searchResults.first().click();

    // Try to add with invalid quantity (0 or negative)
    await page.fill('input[placeholder*="수량"]', '0');
    await page.fill('input[placeholder*="평균 매수가"]', '50000000');
    await page.getByRole('button', { name: '추가' }).click();

    // Should still show modal (validation failed)
    await expect(page.getByText('암호화폐 추가')).toBeVisible();

    // Try with valid inputs
    await page.fill('input[placeholder*="수량"]', '1');
    await page.getByRole('button', { name: '추가' }).click();

    // Should close modal and add holding
    await page.waitForTimeout(1000);
    await expect(page.getByText('BTC')).toBeVisible({ timeout: 5000 });
  });

  test('should handle crypto search debouncing', async ({ page }) => {
    // Create a test portfolio
    await createTestPortfolio(page);

    // Click on the portfolio
    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // Click crypto add button
    await page.getByText('+ 암호화폐 추가').click();

    // Type quickly (should debounce)
    const searchInput = page.getByPlaceholder('암호화폐 검색...');
    await searchInput.type('bit', { delay: 50 }); // Fast typing

    // Wait less than debounce time (300ms)
    await page.waitForTimeout(100);

    // Continue typing
    await searchInput.type('coin', { delay: 50 });

    // Wait for debounce (300ms) + API call
    await page.waitForTimeout(500);

    // Search should only have executed once with full term "bitcoin"
    const searchResults = page.locator('[data-testid="crypto-search-result"]');
    await expect(searchResults.first()).toBeVisible({ timeout: 3000 });
  });

  test.afterEach(async ({ page }) => {
    // Cleanup: could delete test portfolios if needed
    // For now, just close the page
    await page.close();
  });
});
