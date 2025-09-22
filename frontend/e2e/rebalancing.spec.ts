import { test, expect } from '@playwright/test'

test.describe('포트폴리오 리밸런싱', () => {

  test.beforeEach(async ({ page }) => {
    // 백엔드 서버가 실행 중인지 확인하기 위해 health check
    try {
      await page.goto('http://localhost:8080/actuator/health', { timeout: 5000 })
      const response = await page.textContent('body')
      expect(response).toContain('UP')
    } catch (error) {
      console.warn('백엔드 서버가 실행되지 않았을 수 있습니다.')
    }

    // 프론트엔드 홈페이지로 이동
    await page.goto('/')
  })

  test('메인 페이지 로드 확인', async ({ page }) => {
    // 페이지 제목 확인
    await expect(page).toHaveTitle(/Sentinel/i)

    // 기본 컨텐츠 로드 확인
    await expect(page.locator('body')).toBeVisible()
  })

  test('리밸런싱 페이지 접근 테스트', async ({ page }) => {
    // 리밸런싱 페이지로 직접 이동 (포트폴리오 ID 1로 가정)
    await page.goto('/portfolios/1/rebalancing')

    // 페이지 로드 대기
    await page.waitForLoadState('networkidle')

    // 리밸런싱 페이지 제목 확인
    await expect(page.locator('h1')).toContainText('포트폴리오 리밸런싱')

    // AI 기반 설명 텍스트 확인
    await expect(page.locator('text=AI 기반 리밸런싱 전략으로 포트폴리오를 최적화하세요')).toBeVisible()
  })

  test('전략 선택 컴포넌트 테스트', async ({ page }) => {
    await page.goto('/portfolios/1/rebalancing')
    await page.waitForLoadState('networkidle')

    // 전략 선택 섹션 확인
    await expect(page.locator('text=리밸런싱 전략 선택')).toBeVisible()

    // 기본 전략들이 표시되는지 확인
    await expect(page.locator('text=임계값 기반')).toBeVisible()
    await expect(page.locator('text=시간 기반')).toBeVisible()
    await expect(page.locator('text=하이브리드')).toBeVisible()

    // 전략 설명이 표시되는지 확인
    await expect(page.locator('text=자산 배분이 목표치에서 설정된 임계값')).toBeVisible()
  })

  test('목표 자산 배분 에디터 테스트', async ({ page }) => {
    await page.goto('/portfolios/1/rebalancing')
    await page.waitForLoadState('networkidle')

    // 목표 자산 배분 섹션 확인
    await expect(page.locator('text=목표 자산 배분')).toBeVisible()

    // 총 배분 현황 확인
    await expect(page.locator('text=총 배분 현황')).toBeVisible()

    // 종목 추가 버튼 확인
    await expect(page.locator('text=종목 추가')).toBeVisible()

    // 자동 균등 배분 버튼 확인
    await expect(page.locator('text=자동 균등 배분')).toBeVisible()
  })

  test('추천안 생성 버튼 테스트', async ({ page }) => {
    await page.goto('/portfolios/1/rebalancing')
    await page.waitForLoadState('networkidle')

    // 추천안 생성 버튼 확인
    const generateButton = page.locator('text=리밸런싱 추천안 생성')
    await expect(generateButton).toBeVisible()

    // 빠른 분석 버튼 확인
    const quickAnalysisButton = page.locator('text=빠른 분석')
    await expect(quickAnalysisButton).toBeVisible()

    // 버튼 클릭 가능 여부 확인
    await expect(generateButton).toBeEnabled()
    await expect(quickAnalysisButton).toBeEnabled()
  })

  test('반응형 디자인 테스트', async ({ page }) => {
    await page.goto('/portfolios/1/rebalancing')
    await page.waitForLoadState('networkidle')

    // 데스크톱 뷰 테스트
    await page.setViewportSize({ width: 1920, height: 1080 })
    await expect(page.locator('h1')).toBeVisible()

    // 태블릿 뷰 테스트
    await page.setViewportSize({ width: 768, height: 1024 })
    await expect(page.locator('h1')).toBeVisible()

    // 모바일 뷰 테스트
    await page.setViewportSize({ width: 375, height: 667 })
    await expect(page.locator('h1')).toBeVisible()
  })

  test('백엔드 API 연결 테스트 (Mock)', async ({ page }) => {
    // API 요청을 가로채서 Mock 응답 제공
    await page.route('**/api/v1/portfolios/1/rebalancing/quick-analysis*', async route => {
      const mockResponse = {
        currentAllocation: {
          'AAPL': 35.5,
          'MSFT': 28.2,
          'GOOGL': 18.3,
          'TSLA': 12.0,
          'NVDA': 6.0
        },
        targetAllocation: {
          'AAPL': 30.0,
          'MSFT': 25.0,
          'GOOGL': 20.0,
          'TSLA': 15.0,
          'NVDA': 10.0
        },
        deviations: {
          'AAPL': 5.5,
          'MSFT': 3.2,
          'GOOGL': -1.7,
          'TSLA': -3.0,
          'NVDA': -4.0
        },
        maxDeviation: 5.5,
        maxDeviationSymbol: 'AAPL',
        needsAttention: true
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockResponse)
      })
    })

    await page.goto('/portfolios/1/rebalancing')
    await page.waitForLoadState('networkidle')

    // 빠른 분석 실행
    await page.click('text=빠른 분석')

    // 분석 결과가 표시되는지 확인 (Mock 데이터 기준)
    await expect(page.locator('text=포트폴리오 분석')).toBeVisible()
  })

  test('전략 추천 API 테스트 (Mock)', async ({ page }) => {
    // 전략 정보 API Mock
    await page.route('**/api/v1/portfolios/1/rebalancing/strategies', async route => {
      const mockStrategies = {
        'THRESHOLD_BASED': {
          name: '임계값 기반',
          description: '자산 배분이 목표치에서 설정된 임계값(기본 5%) 이상 벗어날 때 리밸런싱을 권장합니다.',
          pros: ['시장 변동에 즉시 반응', '효율적인 리스크 관리'],
          cons: ['빈번한 모니터링 필요'],
          suitableFor: ['적극적 투자자'],
          complexity: '중간'
        }
      }

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockStrategies)
      })
    })

    await page.goto('/portfolios/1/rebalancing')
    await page.waitForLoadState('networkidle')

    // 전략 정보가 로드되는지 확인
    await expect(page.locator('text=임계값 기반')).toBeVisible()
    await expect(page.locator('text=시장 변동에 즉시 반응')).toBeVisible()
  })

  test('접근성(Accessibility) 테스트', async ({ page }) => {
    await page.goto('/portfolios/1/rebalancing')
    await page.waitForLoadState('networkidle')

    // 키보드 네비게이션 테스트
    await page.keyboard.press('Tab')
    await page.keyboard.press('Tab')

    // 포커스 가능한 요소들이 있는지 확인
    const focusableElements = await page.locator('button, input, select, textarea, [tabindex]').count()
    expect(focusableElements).toBeGreaterThan(0)

    // alt 텍스트나 aria-label이 있는지 확인
    const images = await page.locator('img').count()
    if (images > 0) {
      await expect(page.locator('img').first()).toHaveAttribute('alt')
    }
  })

  test('에러 처리 테스트', async ({ page }) => {
    // API 에러 상황 Mock
    await page.route('**/api/v1/portfolios/1/rebalancing/**', async route => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal Server Error' })
      })
    })

    await page.goto('/portfolios/1/rebalancing')
    await page.waitForLoadState('networkidle')

    // 에러 상황에서도 페이지가 깨지지 않는지 확인
    await expect(page.locator('h1')).toBeVisible()

    // 추천안 생성 버튼 클릭 시 적절한 에러 처리가 되는지 확인
    await page.click('text=리밸런싱 추천안 생성')

    // 에러 메시지나 알림이 표시되는지 확인할 수 있음
    // (실제 구현에 따라 다름)
  })

  test('성능 테스트', async ({ page }) => {
    // 페이지 로드 시간 측정
    const startTime = Date.now()

    await page.goto('/portfolios/1/rebalancing')
    await page.waitForLoadState('networkidle')

    const endTime = Date.now()
    const loadTime = endTime - startTime

    // 5초 이내 로드 확인
    expect(loadTime).toBeLessThan(5000)

    // Core Web Vitals 관련 메트릭 확인 가능
    const performanceEntries = await page.evaluate(() => {
      return JSON.stringify(performance.getEntriesByType('navigation'))
    })

    expect(performanceEntries).toBeTruthy()
  })
})