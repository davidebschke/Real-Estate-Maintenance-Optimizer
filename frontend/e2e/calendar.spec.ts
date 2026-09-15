import { test, expect } from '@playwright/test'

test.describe('calendar', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  test('defaults to the week view showing the current week', async ({ page }) => {
    await page.goto('/calendar')

    await expect(page.getByRole('button', { name: 'Woche' })).toHaveClass(
      /calendar-toolbar__view-button--active/,
    )
  })

  test('switches between day, month and year views', async ({ page }) => {
    await page.goto('/calendar')

    await page.getByRole('button', { name: 'Tag', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Tag', exact: true })).toHaveClass(
      /calendar-toolbar__view-button--active/,
    )

    await page.getByRole('button', { name: 'Monat' }).click()
    await expect(page.getByRole('button', { name: 'Monat' })).toHaveClass(
      /calendar-toolbar__view-button--active/,
    )

    await page.getByRole('button', { name: 'Jahr' }).click()
    await expect(page.getByRole('button', { name: 'Jahr' })).toHaveClass(
      /calendar-toolbar__view-button--active/,
    )
  })

  test('always shows today in the week view again after leaving and re-entering via the main navigation', async ({
    page,
  }) => {
    await page.goto('/calendar')

    await page.getByRole('button', { name: 'Monat' }).click()
    await expect(page.getByRole('button', { name: 'Monat' })).toHaveClass(
      /calendar-toolbar__view-button--active/,
    )

    await page.getByRole('link', { name: 'Übersicht' }).click()
    await page.getByRole('link', { name: 'Kalender' }).click()

    await expect(page.getByRole('button', { name: 'Woche' })).toHaveClass(
      /calendar-toolbar__view-button--active/,
    )
  })
})
