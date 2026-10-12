import { expect, type APIRequestContext, type Page } from '@playwright/test'
import { BACKEND_BASE_URL, csrfHeaders } from './apiSession.js'

/** The tenant created for a test property, as far as the payment mocks need it. */
export interface TestTenant {
  tenantId: string
  apartmentId: string
  firstName: string
  lastName: string
  floor: number
  monthlyRent: number
}

/** A payment as the scan endpoint returns it. */
export interface ScannedPaymentJson {
  paymentDate: string
  amount: number
  payerName: string
  reference: string
  periodYear: number
  periodMonth: number
  tenantId: string | null
  match: 'MATCH' | 'POSSIBLE' | 'NONE'
  alreadyRecorded: boolean
}

/** A payment of a former tenant as the year endpoint returns it. */
export interface FormerTenantPaymentJson {
  apartmentId: string
  floor: number
  formerTenantName: string
  paymentDate: string
  amount: number
  reference: string
  periodYear: number
  periodMonth: number
}

/** What the payment mocks recorded about the requests of a test. */
export interface PaymentMockLog {
  scanCount: number
  activationCount: number
  savedBodies: { payments: Record<string, unknown>[] }[]
  requestedYears: number[]
}

/** The year the e2e payments refer to, so the tests do not depend on a fixed calendar date. */
export const CURRENT_YEAR = new Date().getFullYear()

/** Creates an apartment with Erika Mustermann as its only tenant in the given property via the backend API. */
export async function createTestTenant(request: APIRequestContext, propertyId: string): Promise<TestTenant> {
  const monthlyRent = 950
  const floor = 2
  const response = await request.post(`${BACKEND_BASE_URL}/api/properties/${propertyId}/apartments`, {
    data: {
      apartment: { floor, areaSquareMeters: 64.5, totalRent: monthlyRent, coldRent: 750, additionalCosts: 200 },
      tenant: { firstName: 'Erika', lastName: 'Mustermann' },
    },
    headers: await csrfHeaders(request),
  })
  expect(response.ok()).toBe(true)
  const apartment = (await response.json()) as { id: string; tenants: { id: string }[] }
  return {
    tenantId: apartment.tenants[0]!.id,
    apartmentId: apartment.id,
    firstName: 'Erika',
    lastName: 'Mustermann',
    floor,
    monthlyRent,
  }
}

/** A scanned payment of the tenant for October of the current year, confidently matched. */
export function scannedPaymentJson(tenantId: string | null, overrides: Partial<ScannedPaymentJson> = {}): ScannedPaymentJson {
  return {
    paymentDate: `${CURRENT_YEAR}-10-02`,
    amount: 950,
    payerName: 'Erika Mustermann',
    reference: `Miete Oktober ${CURRENT_YEAR}`,
    periodYear: CURRENT_YEAR,
    periodMonth: 10,
    tenantId,
    match: 'MATCH',
    alreadyRecorded: false,
    ...overrides,
  }
}

/** The first tracked month the mocked backend reports once the check is active. */
export const TRACKING_START = `${CURRENT_YEAR}-01`

/** A payment of a former tenant for March of the current year. */
export function formerTenantPaymentJson(tenant: TestTenant, overrides: Partial<FormerTenantPaymentJson> = {}): FormerTenantPaymentJson {
  return {
    apartmentId: tenant.apartmentId,
    floor: tenant.floor,
    formerTenantName: 'Meier, Max',
    paymentDate: `${CURRENT_YEAR}-03-02`,
    amount: tenant.monthlyRent,
    reference: `Miete März ${CURRENT_YEAR}`,
    periodYear: CURRENT_YEAR,
    periodMonth: 3,
    ...overrides,
  }
}

/** The year table of one tenant as the backend returns it, with the given months paid and the other tracked months untracked; a null tracking start means the check is not activated. */
export function yearJson(
  tenant: TestTenant,
  year: number,
  paidMonths: number[],
  alert: 'NONE' | 'PENDING' | 'OVERDUE' = 'NONE',
  trackingStart: string | null = TRACKING_START,
  formerTenantPayments: FormerTenantPaymentJson[] = [],
) {
  return {
    year,
    trackingStart,
    formerTenantPayments,
    tenants: [
      {
        tenantId: tenant.tenantId,
        apartmentId: tenant.apartmentId,
        firstName: tenant.firstName,
        lastName: tenant.lastName,
        floor: tenant.floor,
        monthlyRent: tenant.monthlyRent,
        alert,
        months: Array.from({ length: 12 }, (_, index) => ({
          month: index + 1,
          status: paidMonths.includes(index + 1) ? 'PAID' : 'UNTRACKED',
          paidAmount: paidMonths.includes(index + 1) ? tenant.monthlyRent : 0,
          expectedAmount: tenant.monthlyRent,
        })),
      },
    ],
  }
}

/** Answers the account-wide alerts request with the given alerts instead of the real backend. */
export async function mockPaymentAlerts(page: Page, alerts: { propertyId: string; apartmentId: string; tenantId: string; alert: 'PENDING' | 'OVERDUE' }[]) {
  await page.route('**/api/rent-payments/alerts', async (route) => {
    await route.fulfill({ json: alerts })
  })
}

/**
 * Answers the scan, save and year requests of one tenant's rent payments instead of the real backend, so no AI run is needed.
 * The year table reports every saved October payment as paid, which lets a test watch the table refresh after saving, and the rent payment check is active unless told otherwise until the activation request arrives.
 */
export async function mockRentPayments(
  page: Page,
  tenant: TestTenant,
  options: {
    scanned?: ScannedPaymentJson[]
    scanFailure?: { status: number; message: string }
    isTrackingActive?: boolean
    activationFailure?: { status: number; message: string }
    formerTenantPayments?: FormerTenantPaymentJson[]
  } = {},
): Promise<PaymentMockLog> {
  const log: PaymentMockLog = { scanCount: 0, activationCount: 0, savedBodies: [], requestedYears: [] }
  const paidMonths: number[] = []
  let isTrackingActive = options.isTrackingActive ?? true

  await page.route((url) => url.pathname.endsWith('/rent-payments/tracking'), async (route) => {
    if (route.request().method() === 'DELETE') {
      isTrackingActive = false
      await route.fulfill({ status: 204 })
      return
    }
    log.activationCount++
    if (options.activationFailure) {
      await route.fulfill({ status: options.activationFailure.status, json: { message: options.activationFailure.message } })
      return
    }
    isTrackingActive = true
    await route.fulfill({ json: { trackingStart: TRACKING_START } })
  })

  await page.route((url) => url.pathname.endsWith('/rent-payments/scan'), async (route) => {
    log.scanCount++
    if (options.scanFailure) {
      await route.fulfill({ status: options.scanFailure.status, json: { message: options.scanFailure.message } })
      return
    }
    await route.fulfill({ json: { payments: options.scanned ?? [] } })
  })

  await page.route((url) => url.pathname.endsWith('/rent-payments'), async (route) => {
    const request = route.request()
    if (request.method() === 'POST') {
      const body = request.postDataJSON() as { payments: { periodMonth: number }[] }
      log.savedBodies.push(body as PaymentMockLog['savedBodies'][number])
      paidMonths.push(...body.payments.map((payment) => payment.periodMonth))
      await route.fulfill({ status: 201, json: { savedCount: body.payments.length, skippedCount: 0 } })
      return
    }
    const year = Number(new URL(request.url()).searchParams.get('year'))
    log.requestedYears.push(year)
    const trackingStart = isTrackingActive ? TRACKING_START : null
    const formerTenantPayments = (options.formerTenantPayments ?? []).filter((payment) => payment.periodYear === year)
    await route.fulfill({ json: yearJson(tenant, year, year === CURRENT_YEAR ? paidMonths : [], 'NONE', trackingStart, formerTenantPayments) })
  })

  return log
}
