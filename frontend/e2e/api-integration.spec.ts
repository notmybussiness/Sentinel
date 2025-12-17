import { test, expect } from '@playwright/test';

test.describe('API Integration Tests', () => {
    test.beforeEach(async ({ page }) => {
        // Backend API가 실행 중인지 확인
        const apiHealth = await page.request.get('http://localhost:8080/api/v1/market/indices');
        expect(apiHealth.ok()).toBeTruthy();
    });

    test('Dashboard loads with real market indices from Backend API', async ({ page }) => {
        // 1. 페이지 로드
        await page.goto('/');

        // 2. Market Indices 섹션이 표시될 때까지 대기
        await expect(page.getByText('Market Indices')).toBeVisible();

        // 3. 로딩이 완료될 때까지 대기 (skeleton이 사라짐)
        await expect(page.locator('.animate-pulse')).toHaveCount(0, { timeout: 10000 });

        // 4. 실제 Backend API 데이터 확인
        // Backend에서 S&P 500, NASDAQ, DOW, KOSPI를 반환함
        await expect(page.getByText('S&P 500')).toBeVisible();
        await expect(page.getByText('NASDAQ')).toBeVisible();
        await expect(page.getByText('DOW')).toBeVisible();
        await expect(page.getByText('KOSPI')).toBeVisible();

        // 5. 가격이 숫자로 표시되는지 확인 (실제 API 값이 있음)
        // S&P 500 가격 카드의 숫자 값이 있는지 확인
        const sp500Card = page.locator('text=S&P 500').locator('..').locator('..');
        await expect(sp500Card.locator('h3')).toContainText(/[\d,]+/);
    });

    test('Dashboard loads with real crypto data from Backend API', async ({ page }) => {
        // 1. 페이지 로드
        await page.goto('/');

        // 2. Top Crypto 섹션이 표시될 때까지 대기
        await expect(page.getByText('Top Crypto')).toBeVisible();

        // 3. 로딩이 완료될 때까지 대기
        await expect(page.locator('.animate-pulse')).toHaveCount(0, { timeout: 10000 });

        // 4. 암호화폐 데이터가 표시되는지 확인
        // Backend /api/v1/crypto/trending은 XRP, BTC, ETH 등을 반환
        // 최소 하나 이상의 암호화폐가 표시되어야 함
        // Top Crypto 섹션의 제목 바로 다음 grid에서 카드 확인
        const cryptoHeading = page.locator('h2:has-text("Top Crypto")');
        const cryptoGrid = cryptoHeading.locator('~ div').first();

        // 암호화폐 심볼이 포함된 카드가 있는지 확인 (BTC, ETH, XRP 등)
        // 여러 개가 있으므로 first()로 하나만 확인
        await expect(cryptoGrid.getByText(/BTC|ETH|XRP|SOL/).first()).toBeVisible();
    });

    test('API response contains expected data structure', async ({ page }) => {
        // Market Indices API 직접 호출
        const indicesResponse = await page.request.get('http://localhost:8080/api/v1/market/indices');
        expect(indicesResponse.ok()).toBeTruthy();

        const indices = await indicesResponse.json();
        expect(Array.isArray(indices)).toBeTruthy();
        expect(indices.length).toBeGreaterThan(0);

        // 첫 번째 항목 구조 확인
        const firstIndex = indices[0];
        expect(firstIndex).toHaveProperty('symbol');
        expect(firstIndex).toHaveProperty('name');
        expect(firstIndex).toHaveProperty('value');
        expect(firstIndex).toHaveProperty('change');
        expect(firstIndex).toHaveProperty('changePercent');
    });

    test('Crypto API response contains expected data structure', async ({ page }) => {
        // Crypto Trending API 직접 호출
        const cryptoResponse = await page.request.get('http://localhost:8080/api/v1/crypto/trending');
        expect(cryptoResponse.ok()).toBeTruthy();

        const cryptos = await cryptoResponse.json();
        expect(Array.isArray(cryptos)).toBeTruthy();
        expect(cryptos.length).toBeGreaterThan(0);

        // 첫 번째 항목 구조 확인
        const firstCrypto = cryptos[0];
        expect(firstCrypto).toHaveProperty('symbol');
        expect(firstCrypto).toHaveProperty('name');
        expect(firstCrypto).toHaveProperty('price');
        expect(firstCrypto).toHaveProperty('changePercent');
        expect(firstCrypto).toHaveProperty('volume');
    });

    test('Network requests go to Backend API, not mock', async ({ page }) => {
        // 네트워크 요청 모니터링
        const apiRequests: string[] = [];

        page.on('request', request => {
            if (request.url().includes('localhost:8080')) {
                apiRequests.push(request.url());
            }
        });

        // 페이지 로드
        await page.goto('/');

        // 로딩 완료 대기
        await expect(page.locator('.animate-pulse')).toHaveCount(0, { timeout: 10000 });

        // Backend API 호출이 있었는지 확인
        expect(apiRequests.some(url => url.includes('/api/v1/market/indices'))).toBeTruthy();
        expect(apiRequests.some(url => url.includes('/api/v1/crypto/trending'))).toBeTruthy();
    });
});

test.describe('Authentication UI Tests', () => {
    test('Header shows Kakao login button when not authenticated', async ({ page }) => {
        // 로그인 안 된 상태에서 페이지 로드
        await page.goto('/');

        // 로딩 완료 대기
        await expect(page.locator('.animate-pulse')).toHaveCount(0, { timeout: 10000 });

        // 카카오 로그인 버튼이 표시되어야 함
        await expect(page.getByRole('button', { name: /카카오 로그인/ })).toBeVisible();
    });

    test('Kakao login button click triggers login flow', async ({ page }) => {
        // 페이지 로드
        await page.goto('/');

        // 로딩 완료 대기
        await expect(page.locator('.animate-pulse')).toHaveCount(0, { timeout: 10000 });

        // 네트워크 요청 모니터링
        const loginApiCalled = page.waitForRequest(
            request => request.url().includes('/api/v1/auth/kakao'),
            { timeout: 5000 }
        );

        // 카카오 로그인 버튼 클릭
        await page.getByRole('button', { name: /카카오 로그인/ }).click();

        // 카카오 로그인 URL 요청이 발생했는지 확인
        const request = await loginApiCalled;
        expect(request.url()).toContain('/api/v1/auth/kakao');
    });

    test('OAuth callback page handles missing code', async ({ page }) => {
        // code 없이 callback 페이지 접근
        await page.goto('/auth/kakao/callback');

        // 에러 메시지 표시 확인
        await expect(page.getByText(/인증 코드가 없습니다/)).toBeVisible({ timeout: 5000 });
    });

    test('Kakao login redirects to Kakao auth page', async ({ page }) => {
        // 페이지 로드
        await page.goto('/');
        await expect(page.locator('.animate-pulse')).toHaveCount(0, { timeout: 10000 });

        // 카카오 로그인 버튼 클릭 후 리다이렉트 확인
        await Promise.all([
            page.waitForURL(/kakao\.com/, { timeout: 10000 }),
            page.getByRole('button', { name: /카카오 로그인/ }).click(),
        ]);

        // 카카오 인증 페이지로 리다이렉트됐는지 확인
        const currentUrl = page.url();
        expect(currentUrl).toMatch(/kakao\.com/);
        // redirect_uri가 URL에 포함되어 있는지 확인 (이중 인코딩: %253A = %3A = :)
        expect(currentUrl).toContain('localhost%253A3000');
        expect(currentUrl).toContain('callback');
    });
});
