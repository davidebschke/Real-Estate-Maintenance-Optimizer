import { test, expect, type Page } from '@playwright/test'
import { createTestProperty, deleteTestProperty, type TestProperty } from './support/testProperty.js'
import {
  CURRENT_YEAR,
  createTestTenant,
  mockPaymentAlerts,
  mockRentPayments,
  scannedPaymentJson,
  type TestTenant,
} from './support/paymentMocks.js'

const MAX_STATEMENT_BYTES = 5 * 1024 * 1024

/** A tiny file the browser treats as a PDF; the AI scan itself is always mocked. */
const statementPdf = { name: 'kontoauszug.pdf', mimeType: 'application/pdf', buffer: Buffer.from('%PDF-1.7 test') }

/** Opens the detail drawer of the test property from the property list. */
async function openPropertyDetails(page: Page, property: TestProperty) {
  await page.goto('/properties')
  await page.locator('.property-card', { hasText: property.name }).locator('.property-card__details').click()
  await expect(page.getByRole('heading', { name: 'Mietzahlungen' })).toBeVisible()
}

test.describe('rent payment check', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  let property: TestProperty
  let tenant: TestTenant

  test.beforeEach(async ({ request, page }) => {
    property = await createTestProperty(request)
    tenant = await createTestTenant(request, property.id)
    await mockPaymentAlerts(page, [])
  })

  test.afterEach(async ({ request }) => {
    await deleteTestProperty(request, property.id)
  })

  test('shows the year table of the property with the tenant below the apartments', async ({ page }) => {
    await mockRentPayments(page, tenant)

    await openPropertyDetails(page, property)

    const table = page.locator('.tenant-payment-table')
    await expect(table.locator('.tenant-payment-table__tenant-name')).toHaveText('Erika Mustermann')
    await expect(table.locator('.tenant-payment-table__year')).toHaveText(String(CURRENT_YEAR))
    await expect(table.locator('.tenant-payment-table__cell')).toHaveCount(12)
    await expect(page.getByRole('button', { name: 'Kontoauszug einlesen' })).toBeVisible()
  })

  test('navigates the year table to the previous year', async ({ page }) => {
    const log = await mockRentPayments(page, tenant)
    await openPropertyDetails(page, property)

    await page.getByRole('button', { name: 'Vorheriges Jahr' }).click()

    await expect(page.locator('.tenant-payment-table__year')).toHaveText(String(CURRENT_YEAR - 1))
    await expect.poll(() => log.requestedYears).toContain(CURRENT_YEAR - 1)
  })

  test('reads a statement, previews the payments, saves them and refreshes the table', async ({ page }) => {
    const log = await mockRentPayments(page, tenant, { scanned: [scannedPaymentJson(tenant.tenantId)] })
    await openPropertyDetails(page, property)
    const paidCells = page.locator('.tenant-payment-table__cell').getByRole('img', { name: /^Bezahlt/ })
    await expect(paidCells).toHaveCount(0)

    await page.locator('.payment-upload-button input[type="file"]').setInputFiles(statementPdf)

    const dialog = page.locator('.payment-scan-preview-dialog')
    await expect(dialog.getByText('Erkannte Zahlungen prüfen')).toBeVisible()
    await expect(dialog.locator('.payment-scan-preview-dialog__payer')).toHaveText('Erika Mustermann')
    await expect(dialog.locator('.payment-scan-preview-dialog__amount')).toContainText('950,00')
    await expect(dialog.locator('.payment-scan-preview-dialog__tenant-select')).toContainText('Erika Mustermann')
    await expect(dialog.locator('.payment-scan-preview-dialog__match')).toHaveText('Sichere Zuordnung')

    await dialog.getByRole('button', { name: '1 Zahlung(en) speichern' }).click()

    await expect(page.getByText('Gespeichert: 1, übersprungen: 0')).toBeVisible()
    await expect(dialog).toBeHidden()
    expect(log.savedBodies).toHaveLength(1)
    expect(log.savedBodies[0]!.payments).toEqual([
      {
        tenantId: tenant.tenantId,
        paymentDate: `${CURRENT_YEAR}-10-02`,
        amount: 950,
        reference: `Miete Oktober ${CURRENT_YEAR}`,
        periodYear: CURRENT_YEAR,
        periodMonth: 10,
      },
    ])
    await expect(paidCells).toHaveCount(1)
  })

  test('deselects an already recorded payment and highlights an unassigned one', async ({ page }) => {
    await mockRentPayments(page, tenant, {
      scanned: [
        scannedPaymentJson(tenant.tenantId, { alreadyRecorded: true }),
        scannedPaymentJson(null, { payerName: 'Stadtwerke', reference: 'Abschlag', amount: 40, match: 'NONE' }),
      ],
    })
    await openPropertyDetails(page, property)

    await page.locator('.payment-upload-button input[type="file"]').setInputFiles(statementPdf)

    const dialog = page.locator('.payment-scan-preview-dialog')
    const rows = dialog.locator('.payment-scan-preview-dialog__row')
    await expect(rows).toHaveCount(2)
    await expect(rows.nth(0).locator('.payment-scan-preview-dialog__recorded')).toContainText('Bereits erfasst')
    await expect(rows.nth(0).getByRole('checkbox')).not.toBeChecked()
    await expect(rows.nth(1)).toHaveClass(/payment-scan-preview-dialog__row--review/)
    await expect(rows.nth(1).locator('.payment-scan-preview-dialog__match')).toHaveText('Keine Zuordnung')
    await expect(dialog.getByRole('button', { name: '0 Zahlung(en) speichern' })).toBeDisabled()
  })

  test('discards the scanned payments without saving when the preview is cancelled', async ({ page }) => {
    const log = await mockRentPayments(page, tenant, { scanned: [scannedPaymentJson(tenant.tenantId)] })
    await openPropertyDetails(page, property)
    await page.locator('.payment-upload-button input[type="file"]').setInputFiles(statementPdf)
    const dialog = page.locator('.payment-scan-preview-dialog')
    await expect(dialog).toBeVisible()

    await dialog.getByRole('button', { name: 'Abbrechen' }).click()

    await expect(dialog).toBeHidden()
    expect(log.savedBodies).toHaveLength(0)
  })

  test('shows a hint when the statement contains no payments', async ({ page }) => {
    await mockRentPayments(page, tenant, { scanned: [] })
    await openPropertyDetails(page, property)

    await page.locator('.payment-upload-button input[type="file"]').setInputFiles(statementPdf)

    await expect(page.getByText('Im Kontoauszug wurden keine Zahlungen erkannt.')).toBeVisible()
  })

  test('shows the backend message when the AI is unavailable and opens no preview', async ({ page }) => {
    await mockRentPayments(page, tenant, {
      scanFailure: { status: 503, message: 'Die KI ist gerade nicht erreichbar. Bitte später erneut versuchen.' },
    })
    await openPropertyDetails(page, property)

    await page.locator('.payment-upload-button input[type="file"]').setInputFiles(statementPdf)

    await expect(page.locator('.payment-upload-button__error')).toHaveText(
      'Die KI ist gerade nicht erreichbar. Bitte später erneut versuchen.',
    )
    await expect(page.locator('.payment-scan-preview-dialog')).toHaveCount(0)
  })

  test('rejects a file that is too large or not a PDF without sending it', async ({ page }) => {
    const log = await mockRentPayments(page, tenant)
    await openPropertyDetails(page, property)
    const input = page.locator('.payment-upload-button input[type="file"]')

    await input.setInputFiles({ name: 'gross.pdf', mimeType: 'application/pdf', buffer: Buffer.alloc(MAX_STATEMENT_BYTES + 1) })
    await expect(page.locator('.payment-upload-button__error')).toHaveText('Die Datei ist größer als 5 MB.')

    await input.setInputFiles({ name: 'foto.png', mimeType: 'image/png', buffer: Buffer.from('png') })
    await expect(page.locator('.payment-upload-button__error')).toHaveText('Bitte eine PDF-Datei auswählen.')

    expect(log.scanCount).toBe(0)
  })

  test('marks the property and its tenant when a payment is overdue', async ({ page }) => {
    await mockRentPayments(page, tenant)
    await mockPaymentAlerts(page, [
      { propertyId: property.id, apartmentId: tenant.apartmentId, tenantId: tenant.tenantId, alert: 'OVERDUE' },
    ])
    await page.goto('/properties')

    const card = page.locator('.property-card', { hasText: property.name })
    await expect(card.locator('.property-card__payment-alert')).toContainText('Zahlung überfällig')

    await card.locator('.property-card__details').click()
    await expect(page.locator('.apartment-card__tenant .payment-alert-badge')).toContainText('Zahlung überfällig')
  })

  test('fits a phone screen: the year table scrolls inside its own region and the preview needs no page scrolling', async ({ page }) => {
    await mockRentPayments(page, tenant, { scanned: [scannedPaymentJson(tenant.tenantId)] })
    await page.setViewportSize({ width: 360, height: 740 })
    await openPropertyDetails(page, property)

    await expect(page.locator('.tenant-payment-table__scroll')).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)).toBe(false)

    await page.locator('.payment-upload-button input[type="file"]').setInputFiles(statementPdf)
    const dialog = page.locator('.payment-scan-preview-dialog')
    await expect(dialog.locator('.payment-scan-preview-dialog__row')).toBeVisible()
    const save = dialog.getByRole('button', { name: '1 Zahlung(en) speichern' })
    await save.scrollIntoViewIfNeeded()
    await expect(save).toBeInViewport({ ratio: 1 })
    expect(await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)).toBe(false)
  })
})
