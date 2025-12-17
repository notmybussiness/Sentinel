import { test, expect } from '@playwright/test';

test.describe('Portfolio CRUD with Dev Login', () => {
    test.beforeEach(async ({ page }) => {
        // Go to homepage
        await page.goto('/');
        await page.waitForLoadState('networkidle');

        // Dev login
        const devLoginButton = page.getByRole('button', { name: 'Dev 로그인' });
        if (await devLoginButton.isVisible({ timeout: 3000 }).catch(() => false)) {
            await devLoginButton.click();
            await expect(page.getByText('로그아웃')).toBeVisible({ timeout: 5000 });
        }
    });

    test('should create a new portfolio', async ({ page }) => {
        // Navigate to portfolio page
        await page.click('a[href="/portfolio"]');
        await page.waitForURL('**/portfolio');
        await page.waitForTimeout(1000);

        // Click create new portfolio card
        await page.click('text=Create New Portfolio');

        // Wait for modal - correct title
        await expect(page.getByText('새 포트폴리오 생성')).toBeVisible();

        // Fill in the form - use modal context
        const modal = page.locator('.fixed.inset-0').locator('visible=true');
        const nameInput = modal.locator('input[type="text"]');
        await nameInput.fill('E2E Test Portfolio ' + Date.now());

        const descInput = modal.locator('textarea');
        await descInput.fill('E2E test description');

        // Submit
        await page.click('button:has-text("생성하기")');

        // Wait for modal to close
        await expect(page.getByText('새 포트폴리오 생성')).not.toBeVisible({ timeout: 5000 });

        // Wait for list refresh
        await page.waitForTimeout(1000);

        // Verify at least one portfolio card exists
        await expect(page.locator('text=Total Value').first()).toBeVisible({ timeout: 5000 });
    });

    test('should view portfolio details', async ({ page }) => {
        // Navigate to portfolio page
        await page.click('a[href="/portfolio"]');
        await page.waitForURL('**/portfolio');
        await page.waitForTimeout(1500);

        // Find portfolio link (Link component wraps the card)
        const portfolioLink = page.locator('a[href^="/portfolio/"]').first();

        if (await portfolioLink.isVisible({ timeout: 3000 }).catch(() => false)) {
            // Click on portfolio link
            await portfolioLink.click();

            // Should navigate to detail page
            await expect(page).toHaveURL(/\/portfolio\/\d+/, { timeout: 5000 });

            // Wait for content to load
            await page.waitForTimeout(2000);

            // Should see portfolio details
            await expect(page.getByText('총 가치')).toBeVisible({ timeout: 10000 });
            await expect(page.getByRole('heading', { name: '보유 종목' })).toBeVisible();
        } else {
            // Create a portfolio first
            await page.click('text=Create New Portfolio');
            await page.locator('.fixed.inset-0 input[type="text"]').fill('View Test Portfolio');
            await page.click('button:has-text("생성하기")');
            await page.waitForTimeout(2000);

            // Click the created portfolio link
            await page.locator('a[href^="/portfolio/"]').filter({ hasText: 'View Test Portfolio' }).click();
            await expect(page).toHaveURL(/\/portfolio\/\d+/, { timeout: 5000 });
        }
    });

    test('should edit portfolio', async ({ page }) => {
        // Navigate to portfolio page
        await page.click('a[href="/portfolio"]');
        await page.waitForURL('**/portfolio');
        await page.waitForTimeout(1500);

        // Find portfolio link
        const portfolioLink = page.locator('a[href^="/portfolio/"]').first();

        if (await portfolioLink.isVisible({ timeout: 3000 }).catch(() => false)) {
            await portfolioLink.click();
            await expect(page).toHaveURL(/\/portfolio\/\d+/, { timeout: 5000 });

            // Wait for content to load
            await page.waitForTimeout(2000);

            // Click edit button
            await page.click('button:has-text("수정")');

            // Edit modal should be visible
            await expect(page.getByText('포트폴리오 수정')).toBeVisible();

            // Modify name - get input inside modal
            const modal = page.locator('.fixed.inset-0');
            const nameInput = modal.locator('input[type="text"]');
            const currentName = await nameInput.inputValue();
            await nameInput.clear();
            await nameInput.fill(currentName + ' Updated');

            // Save
            await page.click('button:has-text("저장하기")');

            // Modal should close
            await expect(page.getByText('포트폴리오 수정')).not.toBeVisible({ timeout: 5000 });

            // Verify name changed
            await expect(page.getByText(currentName + ' Updated')).toBeVisible({ timeout: 5000 });
        }
    });

    test('should add holding to portfolio', async ({ page }) => {
        // Navigate to portfolio page first (after login in beforeEach)
        await page.click('a[href="/portfolio"]');
        await page.waitForURL('**/portfolio');
        await page.waitForTimeout(1500);

        // Find portfolio link
        const portfolioLink = page.locator('a[href^="/portfolio/"]').first();

        if (await portfolioLink.isVisible({ timeout: 3000 }).catch(() => false)) {
            // Click on link
            await portfolioLink.click();

            // Wait for navigation
            await expect(page).toHaveURL(/\/portfolio\/\d+/, { timeout: 10000 });

            // Wait for content to load
            await page.waitForTimeout(3000);

            // Check if add holding button exists
            const addButton = page.getByRole('button', { name: '종목 추가' });
            await expect(addButton).toBeVisible({ timeout: 10000 });

            // Click add holding button
            await addButton.click();

            // Modal should appear
            await expect(page.getByRole('heading', { name: '종목 추가' })).toBeVisible();

            // Fill in holding details
            const modal = page.locator('.fixed.inset-0');
            await modal.locator('input[placeholder*="AAPL"]').fill('GOOG');
            await modal.locator('input[placeholder="10"]').fill('2');
            await modal.locator('input[placeholder="150.00"]').fill('180.00');

            // Submit
            await page.click('button:has-text("추가하기")');

            // Wait for API response
            await page.waitForTimeout(2000);

            // Reload page to see updated data
            await page.reload();
            await page.waitForLoadState('networkidle');
            await page.waitForTimeout(2000);

            // Verify holding was added
            await expect(page.getByText('GOOG')).toBeVisible({ timeout: 10000 });
        }
    });

    test('should delete portfolio', async ({ page }) => {
        // Navigate to portfolio page
        await page.click('a[href="/portfolio"]');
        await page.waitForURL('**/portfolio');
        await page.waitForTimeout(1500);

        // First, create a portfolio to delete
        await page.click('text=Create New Portfolio');
        await expect(page.getByText('새 포트폴리오 생성')).toBeVisible();

        const uniqueName = 'Delete Me ' + Date.now();
        const modal = page.locator('.fixed.inset-0');
        await modal.locator('input[type="text"]').fill(uniqueName);
        await page.click('button:has-text("생성하기")');

        // Wait for modal to close and list to update
        await expect(page.getByText('새 포트폴리오 생성')).not.toBeVisible({ timeout: 5000 });
        await page.waitForTimeout(2000);

        // Find the created portfolio link
        const createdLink = page.locator('a[href^="/portfolio/"]').filter({ hasText: uniqueName });
        await expect(createdLink).toBeVisible({ timeout: 5000 });

        // Handle the confirm dialog
        page.on('dialog', dialog => dialog.accept());

        // Click the trash button within the link
        await createdLink.locator('button').click();

        // Wait for deletion
        await page.waitForTimeout(2000);

        // Verify portfolio was deleted
        await expect(createdLink).not.toBeVisible({ timeout: 5000 });
    });
});
