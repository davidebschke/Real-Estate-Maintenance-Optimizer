import { test, expect, type Page } from '@playwright/test'
import { createTestProperty, deleteTestProperty, type TestProperty } from './support/testProperty.js'
import { createTestApartment } from './support/testApartment.js'

/** Opens the detail drawer of the test property from the property list. */
async function openPropertyDetails(page: Page, property: TestProperty) {
  await page.goto('/properties')
  await page.locator('.property-card', { hasText: property.name }).locator('.property-card__details').click()
  await expect(page.getByRole('heading', { name: 'Wohnungen & Mieter' })).toBeVisible()
}

/** Fills the apartment form with the data of a 64.5 m² apartment on the second floor. */
async function fillApartmentForm(page: Page) {
  await page.locator('#apartment-floor').fill('2')
  await page.locator('#apartment-area').fill('64,5')
  await page.locator('#apartment-totalRent').fill('850')
  await page.locator('#apartment-coldRent').fill('650')
  await page.locator('#apartment-additionalCosts').fill('200')
}

/** Confirms the open delete confirmation. */
async function confirmDeletion(page: Page) {
  await page.getByRole('alertdialog').getByRole('button', { name: 'Löschen' }).click()
}

test.describe('apartments and tenants', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  let property: TestProperty

  test.beforeEach(async ({ request }) => {
    property = await createTestProperty(request)
  })

  test.afterEach(async ({ request }) => {
    await deleteTestProperty(request, property.id)
  })

  test('creates a vacant apartment without a tenant', async ({ page }) => {
    await openPropertyDetails(page, property)

    await page.getByRole('dialog').getByRole('button', { name: 'Wohnung hinzufügen' }).click()
    await fillApartmentForm(page)
    await page.getByRole('button', { name: 'Wohnung anlegen', exact: true }).click()

    const card = page.locator('.apartment-card')
    await expect(card).toHaveCount(1)
    await expect(card.locator('.apartment-card__floor')).toHaveText('2. Stockwerk')
    await expect(card.locator('.apartment-card__vacancy')).toHaveText('Leerstand – noch kein Mieter')
  })

  test('creates an apartment together with its first tenant', async ({ page }) => {
    await openPropertyDetails(page, property)

    await page.getByRole('dialog').getByRole('button', { name: 'Wohnung hinzufügen' }).click()
    await fillApartmentForm(page)
    await page.locator('#apartment-tenant-first-name').fill('Erika')
    await page.locator('#apartment-tenant-last-name').fill('Mustermann')
    await page.getByRole('button', { name: 'Wohnung anlegen', exact: true }).click()

    const card = page.locator('.apartment-card')
    await expect(card.locator('.apartment-card__tenant-name')).toHaveText('Erika Mustermann')
    await expect(card.locator('.apartment-card__vacancy')).toHaveCount(0)
  })

  test('refuses a first tenant with only one name filled', async ({ page }) => {
    await openPropertyDetails(page, property)

    await page.getByRole('dialog').getByRole('button', { name: 'Wohnung hinzufügen' }).click()
    await fillApartmentForm(page)
    await page.locator('#apartment-tenant-first-name').fill('Erika')

    await expect(page.getByRole('button', { name: 'Wohnung anlegen', exact: true })).toBeDisabled()
    await expect(page.getByText('Bitte einen Nachnamen eingeben.')).toBeVisible()
  })

  test('adds a tenant to a vacant apartment and renames them', async ({ page, request }) => {
    await createTestApartment(request, property.id)
    await openPropertyDetails(page, property)

    await page.getByRole('button', { name: 'Mieter hinzufügen' }).first().click()
    await page.locator('#tenant-first-name').fill('Max')
    await page.locator('#tenant-last-name').fill('Meier')
    await page.getByRole('dialog', { name: 'Mieter hinzufügen' }).getByRole('button', { name: 'Mieter hinzufügen' }).click()

    const card = page.locator('.apartment-card')
    await expect(card.locator('.apartment-card__tenant-name')).toHaveText('Max Meier')
    await expect(card.locator('.apartment-card__vacancy')).toHaveCount(0)

    await card.getByRole('button', { name: 'Mieter Max Meier bearbeiten' }).click()
    await page.locator('#tenant-last-name').fill('Müller')
    await page.getByRole('dialog').getByRole('button', { name: 'Speichern' }).click()

    await expect(card.locator('.apartment-card__tenant-name')).toHaveText('Max Müller')
  })

  test('keeps the apartment as vacant after its last tenant was deleted', async ({ page, request }) => {
    await createTestApartment(request, property.id, { firstName: 'Erika', lastName: 'Mustermann' })
    await openPropertyDetails(page, property)

    await page.getByRole('button', { name: 'Mieter Erika Mustermann löschen' }).click()
    await expect(page.getByRole('alertdialog')).toContainText('Zahlungen des vorherigen Mieters erhalten')
    await confirmDeletion(page)

    const card = page.locator('.apartment-card')
    await expect(card).toHaveCount(1)
    await expect(card.locator('.apartment-card__vacancy')).toHaveText('Leerstand – noch kein Mieter')
  })

  test('edits an apartment without asking for a month while the rent stays the same', async ({ page, request }) => {
    await createTestApartment(request, property.id)
    await openPropertyDetails(page, property)

    await page.getByRole('button', { name: 'Wohnung 2. Stockwerk bearbeiten' }).click()
    await page.locator('#apartment-floor').fill('3')

    await expect(page.locator('#apartment-rent-effective-month')).toHaveCount(0)
    await page.getByRole('dialog').getByRole('button', { name: 'Speichern' }).click()

    await expect(page.locator('.apartment-card__floor')).toHaveText('3. Stockwerk')
  })

  test('asks from which month a changed rent applies and saves the new rent', async ({ page, request }) => {
    await createTestApartment(request, property.id)
    await openPropertyDetails(page, property)

    await page.getByRole('button', { name: 'Wohnung 2. Stockwerk bearbeiten' }).click()
    await page.locator('#apartment-totalRent').fill('900')

    await expect(page.locator('label[for="apartment-rent-effective-month"]')).toBeVisible()
    await expect(page.getByText('frühere Monate behalten den bisherigen Mietpreis')).toBeVisible()
    await page.getByRole('dialog').getByRole('button', { name: 'Speichern' }).click()

    await expect(page.locator('.apartment-card__figure').first()).toContainText('900,00')
  })

  test('deletes an apartment only after the confirmation', async ({ page, request }) => {
    await createTestApartment(request, property.id, { firstName: 'Erika', lastName: 'Mustermann' })
    await openPropertyDetails(page, property)

    await page.getByRole('button', { name: 'Wohnung 2. Stockwerk löschen' }).click()
    await expect(page.getByRole('alertdialog')).toContainText('Mietzahlungen und der Mietverlauf werden mitgelöscht')
    await page.getByRole('alertdialog').getByRole('button', { name: 'Abbrechen' }).click()
    await expect(page.locator('.apartment-card')).toHaveCount(1)

    await page.getByRole('button', { name: 'Wohnung 2. Stockwerk löschen' }).click()
    await confirmDeletion(page)

    await expect(page.locator('.apartment-card')).toHaveCount(0)
    await expect(page.locator('.property-detail-drawer__empty')).toHaveText(
      'Für dieses Objekt sind noch keine Wohnungen hinterlegt.',
    )
  })
})
