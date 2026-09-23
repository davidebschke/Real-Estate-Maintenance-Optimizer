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

  test('flags a duplicate name against an existing property with a red-bordered hint and disables submit', async ({
    page,
  }) => {
    await page.goto('/properties')

    await page.locator('.property-create-card').click()
    await page.getByLabel('Name').fill('Wohnanlage Sonnenhof')
    await page.getByLabel('Straße').fill('Ganz andere Str.')
    await page.getByLabel('Hausnummer').fill('99')
    await page.getByLabel('Postleitzahl').fill('12345')
    await page.getByLabel('Ort').fill('Musterstadt')

    await expect(page.getByText('Ein Objekt mit diesem Namen existiert bereits.')).toBeVisible()
    await expect(page.getByLabel('Name')).toHaveClass(/p-invalid/)
    await expect(page.getByLabel('Straße')).not.toHaveClass(/p-invalid/)
    await expect(page.getByRole('button', { name: 'Objekt anlegen', exact: true })).toBeDisabled()
  })

  test('flags a duplicate address against an existing property with a red-bordered hint and disables submit', async ({
    page,
  }) => {
    await page.goto('/properties')

    await page.locator('.property-create-card').click()
    await page.getByLabel('Name').fill('Ganz anderer Name')
    await page.getByLabel('Straße').fill('Aachener Str.')
    await page.getByLabel('Hausnummer').fill('512')
    await page.getByLabel('Postleitzahl').fill('50933')
    await page.getByLabel('Ort').fill('Köln-Braunsenfeld')

    await expect(page.getByText('Ein Objekt mit dieser Adresse existiert bereits.')).toBeVisible()
    await expect(page.getByLabel('Straße')).toHaveClass(/p-invalid/)
    await expect(page.getByLabel('Name')).not.toHaveClass(/p-invalid/)
    await expect(page.getByRole('button', { name: 'Objekt anlegen', exact: true })).toBeDisabled()
  })

  test('flags a house number that is not purely digits with a red-bordered hint and disables submit', async ({
    page,
  }) => {
    await page.goto('/properties')

    await page.locator('.property-create-card').click()
    await page.getByLabel('Name').fill('E2E Testobjekt')
    await page.getByLabel('Straße').fill('Aachener Str.')
    await page.getByLabel('Hausnummer').fill('512b')
    await page.getByLabel('Postleitzahl').fill('50933')
    await page.getByLabel('Ort').fill('Köln')

    await expect(page.getByText('Die Hausnummer darf nur aus Zahlen bestehen.')).toBeVisible()
    await expect(page.getByLabel('Hausnummer')).toHaveClass(/p-invalid/)
    await expect(page.getByRole('button', { name: 'Objekt anlegen', exact: true })).toBeDisabled()

    await page.getByLabel('Hausnummer').fill('512')
    await expect(page.getByText('Die Hausnummer darf nur aus Zahlen bestehen.')).toHaveCount(0)
    await expect(page.getByLabel('Hausnummer')).not.toHaveClass(/p-invalid/)
  })

  test('flags a postal code that is not exactly 5 digits with a red-bordered hint and disables submit', async ({
    page,
  }) => {
    await page.goto('/properties')

    await page.locator('.property-create-card').click()
    await page.getByLabel('Name').fill('E2E Testobjekt')
    await page.getByLabel('Straße').fill('Aachener Str.')
    await page.getByLabel('Hausnummer').fill('512')
    await page.getByLabel('Postleitzahl').fill('123')
    await page.getByLabel('Ort').fill('Köln')
    const submitButton = page.getByRole('button', { name: 'Objekt anlegen', exact: true })

    await expect(page.getByText('Die Postleitzahl muss aus genau 5 Ziffern bestehen.')).toBeVisible()
    await expect(page.getByLabel('Postleitzahl')).toHaveClass(/p-invalid/)
    await expect(submitButton).toBeDisabled()

    await page.getByLabel('Postleitzahl').fill('50933')
    await expect(
      page.getByText('Die Postleitzahl muss aus genau 5 Ziffern bestehen.'),
    ).toHaveCount(0)
    await expect(page.getByLabel('Postleitzahl')).not.toHaveClass(/p-invalid/)
    await expect(submitButton).toBeEnabled()
  })

  test('shows a correction suggestion for an address with a wrong postal code and applies it on click', async ({
    page,
  }) => {
    await page.route('**/api/geocode/validate**', async (route) => {
      await route.fulfill({
        json: {
          status: 'SUGGESTION',
          suggestedStreet: 'Aachener Str.',
          suggestedHouseNumber: '512',
          suggestedPostalCode: '50933',
          suggestedCity: 'Köln',
        },
      })
    })
    await page.goto('/properties')

    await page.locator('.property-create-card').click()
    await page.getByLabel('Name').fill('E2E Testobjekt')
    await page.getByLabel('Straße').fill('Aachener Str.')
    await page.getByLabel('Hausnummer').fill('512')
    await page.getByLabel('Postleitzahl').fill('99999')
    await page.getByLabel('Ort').fill('Köln')
    const submitButton = page.getByRole('button', { name: 'Objekt anlegen', exact: true })

    await submitButton.click()

    await expect(
      page.getByText('Meinten Sie folgende Adresse: Aachener Str. 512, 50933 Köln?'),
    ).toBeVisible()
    await expect(submitButton).toBeDisabled()

    await page.getByRole('button', { name: 'Vorschlag übernehmen' }).click()

    await expect(page.getByLabel('Postleitzahl')).toHaveValue('50933')
    await expect(submitButton).toBeEnabled()
  })

  test('blocks submission with a generic error when the address cannot be resolved at all', async ({
    page,
  }) => {
    await page.route('**/api/geocode/validate**', async (route) => {
      await route.fulfill({
        json: {
          status: 'NOT_FOUND',
          suggestedStreet: null,
          suggestedHouseNumber: null,
          suggestedPostalCode: null,
          suggestedCity: null,
        },
      })
    })
    await page.goto('/properties')

    await page.locator('.property-create-card').click()
    await page.getByLabel('Name').fill('E2E Testobjekt')
    await page.getByLabel('Straße').fill('Nirgendwostr.')
    await page.getByLabel('Hausnummer').fill('1')
    await page.getByLabel('Postleitzahl').fill('99999')
    await page.getByLabel('Ort').fill('Nirgendwo')
    const submitButton = page.getByRole('button', { name: 'Objekt anlegen', exact: true })

    await submitButton.click()

    await expect(
      page.getByText('Diese Adresse konnte nicht gefunden werden. Bitte prüfen Sie Ihre Eingabe.'),
    ).toBeVisible()
    await expect(submitButton).toBeDisabled()
  })
})
