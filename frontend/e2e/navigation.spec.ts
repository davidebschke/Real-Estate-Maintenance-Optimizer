import process from 'node:process'
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

  test('hides the statistics nav entry in the production build', async ({ page }) => {
    test.skip(!process.env.CI, 'only CI runs against the production preview build')

    await page.goto('/')

    await expect(page.getByRole('link', { name: 'Statistik' })).toHaveCount(0)
  })

  test('shows the statistics nav entry when developing locally', async ({ page }) => {
    test.skip(!!process.env.CI, 'CI runs the production build, where this entry is hidden')

    await page.goto('/')

    await expect(page.getByRole('link', { name: 'Statistik' })).toBeVisible()
  })

  test('keeps the statistics route directly reachable by URL', async ({ page }) => {
    await page.goto('/statistics')

    await expect(page.getByText('Hier ist die Statistikseite')).toBeVisible()
  })

  test('returns to the overview page when the logo or brand name is clicked', async ({ page }) => {
    await page.goto('/calendar')

    await page.getByRole('link', { name: 'Remo' }).click()

    await expect(page.locator('.daily-appointment-list')).toBeVisible()
    await expect(page).toHaveURL('/')
  })
})
