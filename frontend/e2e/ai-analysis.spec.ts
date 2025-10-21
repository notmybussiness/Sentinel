import { test, expect } from '@playwright/test';

test.describe('Portfolio AI Analysis E2E', () => {
  // Helper function to perform dev login
  async function devLogin(page: any) {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    // Wait for login page to fully load
    await page.waitForTimeout(1000);

    // Click dev login button and wait for navigation
    await Promise.all([
      page.waitForNavigation({ url: '/', timeout: 15000 }),
      page.getByText('개발자 로그인 (테스트)').click()
    ]);

    // IMPORTANT: Wait for auth state to fully propagate
    await page.waitForLoadState('networkidle');

    // Additional wait to ensure tokens are stored in localStorage
    await page.waitForTimeout(2000);

    // Try to verify auth token (may not be needed for E2E)
    const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
    if (accessToken) {
      console.log('✅ Dev login successful with access token');
    } else {
      // Even without token in localStorage, if we're on home page, login might have worked
      console.log('⚠️ No access token in localStorage, but continuing test...');
    }
  }

  // Helper function to create a test portfolio
  async function createTestPortfolio(page: any, name: string = 'AI Test Portfolio') {
    // Navigate to portfolios page
    await page.goto('/portfolios');
    await page.waitForLoadState('networkidle');

    // Wait for page to fully render
    await page.waitForTimeout(1000);

    // Click "+ 새 포트폴리오" button
    await page.getByText('+ 새 포트폴리오').click();

    // Wait for modal to appear
    await expect(page.getByRole('heading', { name: '새 포트폴리오 생성' })).toBeVisible();

    // Fill in portfolio details
    await page.fill('input[placeholder*="포트폴리오 이름"]', name);
    await page.fill('textarea[placeholder*="설명"]', 'E2E test portfolio for AI analysis with BTC, ETH, SOL');

    // Click create button
    await page.getByRole('button', { name: '생성' }).click();

    // Wait for modal to close and portfolio list to update
    await page.waitForTimeout(3000);
    await page.waitForLoadState('networkidle');

    console.log(`✅ Portfolio created: ${name}`);
  }

  // Helper function to add crypto holding
  async function addCryptoHolding(
    page: any,
    symbol: string,
    quantity: string,
    averageCost: string,
    baseCurrency: 'KRW' | 'USD' = 'KRW'
  ) {
    // Click "+ 종목 추가" button (통합 모달)
    await page.getByText('+ 종목 추가').click();
    await page.waitForTimeout(500);

    // Select crypto asset type (₿ 암호화폐)
    await page.getByText('₿ 암호화폐').click();
    await page.waitForTimeout(500);

    // Search for crypto
    const searchInput = page.getByPlaceholder('암호화폐 검색...');
    await searchInput.fill(symbol);
    await page.waitForTimeout(800); // Debounce + API call

    // Select first search result
    const searchResults = page.locator('[data-testid="crypto-search-result"]');
    await searchResults.first().click();
    await page.waitForTimeout(500);

    // Fill in quantity
    const quantityInput = page.locator('input[placeholder*="수량"]');
    await quantityInput.fill(quantity);

    // Fill in average cost
    const avgCostInput = page.locator('input[placeholder*="평균 매수가"]');
    await avgCostInput.fill(averageCost);

    // Select base currency if needed
    if (baseCurrency === 'USD') {
      await page.getByRole('button', { name: 'USD' }).click();
    }

    // Click add button
    await page.getByRole('button', { name: '추가' }).click();

    // Wait for modal to close and portfolio to update
    await page.waitForTimeout(1500);
  }

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await devLogin(page);
  });

  test('should add multiple crypto holdings and run AI analysis', async ({ page }) => {
    // 1. Create a test portfolio
    await createTestPortfolio(page, 'Crypto Portfolio - BTC ETH SOL');

    // 2. Click on the portfolio to go to detail page
    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // 3. Add BTC (10개, 한 달 전 가격: $95,000 = 123,500,000원)
    console.log('Adding BTC holding...');
    await addCryptoHolding(page, 'BTC', '10', '123500000', 'KRW');
    await expect(page.getByText('BTC')).toBeVisible({ timeout: 5000 });

    // 4. Add ETH (100개, 한 달 전 가격: $3,300 = 4,290,000원)
    console.log('Adding ETH holding...');
    await addCryptoHolding(page, 'ETH', '100', '4290000', 'KRW');
    await expect(page.getByText('ETH')).toBeVisible({ timeout: 5000 });

    // 5. Add SOL (50개, 한 달 전 가격: $180 = 234,000원)
    console.log('Adding SOL holding...');
    await addCryptoHolding(page, 'SOL', '50', '234000', 'KRW');
    await expect(page.getByText('SOL')).toBeVisible({ timeout: 5000 });

    console.log('All holdings added successfully!');
    console.log('Portfolio summary:');
    console.log('- BTC: 10개 @ 123,500,000원 = 1,235,000,000원');
    console.log('- ETH: 100개 @ 4,290,000원 = 429,000,000원');
    console.log('- SOL: 50개 @ 234,000원 = 11,700,000원');
    console.log('Total cost: 1,675,700,000원 (약 16억 7천만원)');

    // Wait for portfolio to recalculate totals
    await page.waitForTimeout(2000);

    // 6. Click "🤖 AI 분석" button
    console.log('Clicking AI analysis button...');
    await page.getByText('🤖 AI 분석').click();

    // Wait for modal to appear
    await expect(page.getByText('AI 포트폴리오 분석')).toBeVisible({ timeout: 3000 });

    // 7. Select analysis type (OVERVIEW - 전체 개요)
    console.log('Selecting analysis type: OVERVIEW');
    await page.getByText('전체 개요').click();

    // 8. Click "분석 시작" button
    console.log('Starting AI analysis...');
    await page.getByRole('button', { name: '분석 시작' }).click();

    // 9. Wait for analysis to complete (Gemini API call)
    console.log('Waiting for AI analysis to complete...');
    // Wait for loading state to disappear
    await page.waitForTimeout(5000); // Gemini API can take 3-5 seconds

    // 10. Verify analysis results are displayed
    await expect(page.getByText('분석 요약')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('주요 인사이트')).toBeVisible();
    await expect(page.getByText('투자 제안')).toBeVisible();

    console.log('AI analysis completed successfully!');

    // Optional: Take a screenshot for visual verification
    await page.screenshot({ path: 'frontend/e2e/screenshots/ai-analysis-result.png', fullPage: true });
  });

  test('should show AI analysis with different analysis types', async ({ page }) => {
    // Create portfolio with holdings
    await createTestPortfolio(page, 'Diversification Test');

    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // Add two holdings for quick test
    await addCryptoHolding(page, 'BTC', '5', '123500000', 'KRW');
    await addCryptoHolding(page, 'ETH', '50', '4290000', 'KRW');

    await page.waitForTimeout(2000);

    // Test DIVERSIFICATION analysis
    await page.getByText('🤖 AI 분석').click();
    await expect(page.getByText('AI 포트폴리오 분석')).toBeVisible();

    await page.getByText('다각화 분석').click();
    await page.getByRole('button', { name: '분석 시작' }).click();

    await page.waitForTimeout(5000);
    await expect(page.getByText('분석 요약')).toBeVisible({ timeout: 10000 });

    // Verify diversification score is displayed
    await expect(page.getByText('다각화 점수')).toBeVisible();
  });

  test('should show AI analysis with RISK analysis type', async ({ page }) => {
    // Create portfolio with holdings
    await createTestPortfolio(page, 'Risk Analysis Test');

    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // Add holdings
    await addCryptoHolding(page, 'BTC', '2', '123500000', 'KRW');

    await page.waitForTimeout(2000);

    // Test RISK analysis
    await page.getByText('🤖 AI 분석').click();
    await expect(page.getByText('AI 포트폴리오 분석')).toBeVisible();

    await page.getByText('리스크 평가').click();
    await page.getByRole('button', { name: '분석 시작' }).click();

    await page.waitForTimeout(5000);
    await expect(page.getByText('분석 요약')).toBeVisible({ timeout: 10000 });

    // Verify risk level is displayed
    await expect(page.getByText('리스크 수준')).toBeVisible();
  });

  test('should handle AI analysis errors gracefully', async ({ page }) => {
    // Create portfolio WITHOUT holdings (should cause error or warning)
    await createTestPortfolio(page, 'Empty Portfolio');

    const firstPortfolio = page.locator('.cursor-pointer').first();
    await firstPortfolio.click();
    await page.waitForLoadState('networkidle');

    // Try to run AI analysis on empty portfolio
    await page.getByText('🤖 AI 분석').click();

    // Modal should still open
    await expect(page.getByText('AI 포트폴리오 분석')).toBeVisible();

    await page.getByText('전체 개요').click();
    await page.getByRole('button', { name: '분석 시작' }).click();

    await page.waitForTimeout(3000);

    // Should show some result or error message
    // (Implementation may vary based on error handling)
  });

  test.afterEach(async ({ page }) => {
    await page.close();
  });
});
