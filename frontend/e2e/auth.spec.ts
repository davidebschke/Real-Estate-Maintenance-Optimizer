import process from 'node:process'
import { test, expect, type Page } from '@playwright/test'
import { e2eCredentials } from './support/apiSession.js'

/** The green the login button and card turn to once a session started (Tailwind green-500, the app's "completed" color). */
const SUCCESS_GREEN = 'rgb(34, 197, 94)'

/** Enters the given credentials into the login form and submits it. */
async function submitLogin(page: Page, username: string, password: string) {
  await page.getByLabel('Benutzername').fill(username)
  await page.locator('#login-password').fill(password)
  await page.getByRole('button', { name: 'Anmelden', exact: true }).click()
}

test.describe('authentication', () => {
  test.use({ storageState: { cookies: [], origins: [] }, viewport: { width: 1440, height: 900 } })

  test('shows only the login screen before logging in and remembers the requested page', async ({ page }) => {
    await page.goto('/calendar')

    await expect(page).toHaveURL('/login?redirect=/calendar')
    await expect(page.getByRole('heading', { name: 'Willkommen bei Remo' })).toBeVisible()
    await expect(page.locator('.app-header')).toHaveCount(0)
    await expect(page.locator('.app-footer')).toHaveCount(0)
  })

  test('rejects wrong credentials with a message and stays on the login screen', async ({ page }) => {
    await page.goto('/login')

    await submitLogin(page, `e2e-unknown-${Date.now()}`, 'definitely-wrong')

    await expect(page.getByRole('alert')).toHaveText('Benutzername oder Passwort ist falsch.')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('turns the login green, opens the requested page and shows the account name in the header', async ({
    page,
  }) => {
    const { username, password } = e2eCredentials()
    await page.goto('/calendar')

    await submitLogin(page, username, password)

    await expect(page.locator('.login-form__submit')).toHaveCSS('background-color', SUCCESS_GREEN)
    await expect(page.locator('.auth-card')).toHaveClass(/auth-card--success/)
    await expect(page).toHaveURL('/calendar')
    await expect(page.locator('.calendar-toolbar')).toBeVisible()
    await expect(page.locator('.user-account-dropdown__trigger')).toContainText(
      process.env.E2E_DISPLAY_NAME ?? 'Account_Default',
    )
  })

  test('a demo account starts with example data, explains its limits and is gone after logging out', async ({
    page,
  }) => {
    await page.goto('/login')
    await page.getByRole('tab', { name: 'Demo-Account' }).click()
    await expect(page.getByText('30 Termine in den nächsten 3 Wochen')).toBeVisible()
    await expect(page.getByText('bis zu 3 Objekte, 3 Termine und 10 Mieter')).toBeVisible()

    await page.getByRole('button', { name: 'Demo-Account erstellen' }).click()

    await expect(page.locator('.demo-account-panel__submit')).toHaveCSS('background-color', SUCCESS_GREEN)
    await expect(page).toHaveURL('/')
    await expect(page.locator('.demo-account-banner')).toContainText('3 weitere Objekte')
    await expect(page.locator('.user-account-dropdown__trigger')).toContainText('Demo-Account')

    await page.getByRole('link', { name: 'Immobilien' }).click()
    await expect(page.locator('.property-card')).toHaveCount(5)

    await page.locator('.user-account-dropdown__trigger').click()
    await page.getByRole('button', { name: 'Abmelden' }).click()

    await expect(page).toHaveURL('/login')
    await page.goto('/properties')
    await expect(page).toHaveURL('/login?redirect=/properties')
  })

  test('the login screen fits a phone screen without horizontal scrolling', async ({ page }) => {
    await page.setViewportSize({ width: 360, height: 740 })
    await page.goto('/login')

    await expect(page.getByRole('button', { name: 'Anmelden', exact: true })).toBeVisible()
    const overflows = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)
    expect(overflows).toBe(false)

    await page.getByRole('tab', { name: 'Demo-Account' }).click()
    await expect(page.getByRole('button', { name: 'Demo-Account erstellen' })).toBeInViewport()
  })
})
