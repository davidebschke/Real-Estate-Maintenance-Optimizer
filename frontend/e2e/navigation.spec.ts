import { test, expect } from '@playwright/test'

test.describe('main navigation', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  test('shows the overview page by default', async ({ page }) => {
    await page.goto('/')

    await expect(page.locator('.daily-appointment-list')).toBeVisible()
  })

  test('navigates to the page matching each clicked navigation entry', async ({ page }) => {
    await page.goto('/')

    await page.getByRole('link', { name: 'Kalender' }).click()
    await expect(page.locator('.calendar-toolbar')).toBeVisible()
    await expect(page).toHaveURL('/calendar')

    await page.getByRole('link', { name: 'Immobilien' }).click()
    await expect(page.getByRole('heading', { name: 'Objekte' })).toBeVisible()
    await expect(page).toHaveURL('/properties')
  })

  test('shows the statistics nav entry and opens the property statistics list from it', async ({ page }) => {
    await page.goto('/')

    await page.getByRole('link', { name: 'Statistik' }).click()
    await expect(page.getByRole('heading', { name: 'Statistik' })).toBeVisible()
    await expect(page).toHaveURL('/statistics')

    await page.getByRole('link', { name: /Objektstatistik/ }).click()
    await expect(page.getByRole('heading', { name: 'Objektstatistik' })).toBeVisible()
    await expect(page).toHaveURL('/statistics/properties')
  })

  test('keeps the property statistics route directly reachable by URL', async ({ page }) => {
    await page.goto('/statistics/properties')

    await expect(page.getByRole('heading', { name: 'Objektstatistik' })).toBeVisible()
  })

  test('returns to the overview page when the logo or brand name is clicked', async ({ page }) => {
    await page.goto('/calendar')

    await page.getByRole('link', { name: 'Remo' }).click()

    await expect(page.locator('.daily-appointment-list')).toBeVisible()
    await expect(page).toHaveURL('/')
  })
})
