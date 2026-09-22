import { rm } from 'node:fs/promises'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { test, expect, type Page } from '@playwright/test'

const propertyStorageDirectory = join(fileURLToPath(import.meta.url), '..', '..', '..', 'backend', 'ExampleObjects')

/** Opens the pinned create dialog, fills in the given fields and submits it. */
async function createProperty(
  page: Page,
  options: {
    name: string
    street: string
    houseNumber: string
    addressSupplement?: string
    postalCode: string
    city: string
  },
) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/api/properties') && response.request().method() === 'POST',
  )

  await page.locator('.property-create-card').click()
  await page.getByLabel('Name').fill(options.name)
  await page.getByLabel('Straße').fill(options.street)
  await page.getByLabel('Hausnummer').fill(options.houseNumber)
  if (options.addressSupplement) {
    await page.getByLabel('Zusatz').fill(options.addressSupplement)
  }
  await page.getByLabel('Postleitzahl').fill(options.postalCode)
  await page.getByLabel('Ort').fill(options.city)
  await page.getByRole('button', { name: 'Objekt anlegen', exact: true }).click()

  const response = await responsePromise
  return (await response.json()) as { id: string; address: string }
}

/** Removes the JSON file the file-backed repository persisted for the given property, so tests don't leave data behind. */
async function deletePropertyFile(id: string) {
  await rm(join(propertyStorageDirectory, `${id}.json`), { force: true })
}

test.describe('properties', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  test('shows a pinned card with a plus icon at the start of the property list', async ({
    page,
  }) => {
    await page.goto('/properties')

    const firstCard = page.locator('.property-list > *').first()
    await expect(firstCard).toHaveClass(/property-create-card/)
    await expect(firstCard.locator('.pi-plus')).toBeVisible()
  })

  test('creating a property via the dialog adds it to the list', async ({ page }) => {
    await page.goto('/properties')
    const name = `E2E Testobjekt ${Date.now()}`

    const { id } = await createProperty(page, {
      name,
      street: 'Aachener Str.',
      houseNumber: '512',
      postalCode: '50933',
      city: 'Köln',
    })

    try {
      await expect(page.getByRole('heading', { name })).toBeVisible()
    } finally {
      await deletePropertyFile(id)
    }
  })

  test('combines street, house number and address supplement into a single address', async ({
    page,
  }) => {
    await page.goto('/properties')
    const name = `E2E Testobjekt ${Date.now()}`

    const { id, address } = await createProperty(page, {
      name,
      street: 'Aachener Str.',
      houseNumber: '512',
      addressSupplement: 'a',
      postalCode: '50933',
      city: 'Köln',
    })

    try {
      expect(address).toBe('Aachener Str. 512a, 50933 Köln')
    } finally {
      await deletePropertyFile(id)
    }
  })

  test('keeps the submit button disabled until every field is valid', async ({ page }) => {
    await page.goto('/properties')

    await page.locator('.property-create-card').click()
    const submitButton = page.getByRole('button', { name: 'Objekt anlegen', exact: true })
    await expect(submitButton).toBeDisabled()

    await page.getByLabel('Name').fill('E2E Testobjekt')
    await page.getByLabel('Straße').fill('Aachener Str.')
    await page.getByLabel('Hausnummer').fill('512')
    await page.getByLabel('Postleitzahl').fill('123')
    await page.getByLabel('Ort').fill('Köln')
    await expect(submitButton).toBeDisabled()

    await page.getByLabel('Postleitzahl').fill('50933')
    await expect(submitButton).toBeEnabled()
  })
})
