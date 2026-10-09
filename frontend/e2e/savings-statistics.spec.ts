import { test, expect } from '@playwright/test'
import { mockSavings } from './support/optimizationMocks'

test.describe('AI savings statistics', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  test('opens the savings from the statistics menu and draws the line chart with the totals', async ({ page }) => {
    await mockSavings(page)
    await page.goto('/statistics')

    await page.getByRole('link', { name: /Einsparungen durch KI/ }).click()

    await expect(page).toHaveURL('/statistics/savings')
    await expect(page.getByRole('heading', { name: 'Einsparungen durch KI-Optimierung' })).toBeVisible()
    await expect(page.locator('[data-tile="distance"] .savings-summary__value')).toHaveText('12,5 km')
    await expect(page.locator('[data-tile="accepted"] .savings-summary__value')).toHaveText('3')
    await expect(page.locator('.savings-line-chart canvas')).toBeVisible()
  })

  test('requests the monthly savings when the period is switched', async ({ page }) => {
    await mockSavings(page)
    await page.goto('/statistics/savings')
    await expect(page.locator('.savings-line-chart canvas')).toBeVisible()

    const monthlyRequest = page.waitForRequest((request) => request.url().includes('granularity=MONTH'))
    await page.getByRole('button', { name: 'Monate' }).click()

    await monthlyRequest
  })

  test('invites to the optimization while nothing was saved yet', async ({ page }) => {
    await mockSavings(page, true)
    await page.route('**/api/optimizations/proposals', async (route) => {
      await route.fulfill({ json: [] })
    })
    await page.goto('/statistics/savings')

    await expect(page.locator('.savings-line-chart')).toHaveCount(0)
    await page.getByRole('link', { name: 'Zur KI-Optimierung' }).click()

    await expect(page).toHaveURL('/optimization')
  })

  test('the savings page and its chart fit a phone screen without horizontal scrolling', async ({ page }) => {
    await mockSavings(page)
    await page.setViewportSize({ width: 360, height: 740 })
    await page.goto('/statistics/savings')

    const canvas = page.locator('.savings-line-chart canvas')
    await expect(canvas).toBeVisible()
    const canvasBox = await canvas.boundingBox()
    expect(canvasBox!.width).toBeLessThanOrEqual(360)
    const overflows = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)
    expect(overflows).toBe(false)
  })
})
