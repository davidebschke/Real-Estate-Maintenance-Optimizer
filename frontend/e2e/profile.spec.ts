import { test, expect } from '@playwright/test'

test.describe('profile and settings', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  test('opens the profile dialog with username, password and buffer settings from the account menu', async ({ page }) => {
    await page.goto('/')

    await page.locator('.user-account-dropdown__trigger').click()
    await page.getByRole('button', { name: 'Profil & Einstellungen' }).click()

    const dialog = page.locator('.profile-settings-dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByRole('heading', { name: 'Benutzername' })).toBeVisible()
    await expect(dialog.getByRole('heading', { name: 'Passwort ändern' })).toBeVisible()
    await expect(dialog.getByRole('heading', { name: 'Pufferzeit zwischen Terminen' })).toBeVisible()
    await expect(dialog.getByRole('button', { name: 'Passwort ändern' })).toBeDisabled()

    await dialog.getByRole('button', { name: 'Schließen' }).click()
    await expect(dialog).toBeHidden()
  })
})
