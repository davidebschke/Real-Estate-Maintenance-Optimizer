import { test, expect } from '@playwright/test'

test.describe('main navigation', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  test('shows the overview page by default', async ({ page }) => {
    await page.goto('/')

    await expect(page.getByText('Hier ist die Übersichtsseite')).toBeVisible()
  })

  test('navigates to the page matching each clicked navigation entry', async ({ page }) => {
    await page.goto('/')

    await page.getByRole('link', { name: 'Kalender' }).click()
    await expect(page.getByText('Hier ist die Kalenderseite')).toBeVisible()
    await expect(page).toHaveURL('/calendar')

    await page.getByRole('link', { name: 'Statistik' }).click()
    await expect(page.getByText('Hier ist die Statistikseite')).toBeVisible()
    await expect(page).toHaveURL('/statistics')

    await page.getByRole('link', { name: 'Immobilien' }).click()
    await expect(page.getByText('Hier ist die Immobilienseite')).toBeVisible()
    await expect(page).toHaveURL('/properties')
  })

  test('returns to the overview page when the logo or brand name is clicked', async ({ page }) => {
    await page.goto('/calendar')

    await page.getByRole('link', { name: 'Remo' }).click()

    await expect(page.getByText('Hier ist die Übersichtsseite')).toBeVisible()
    await expect(page).toHaveURL('/')
  })
})
