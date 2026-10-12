import { expect, type APIRequestContext } from '@playwright/test'
import { BACKEND_BASE_URL, csrfHeaders } from './apiSession.js'

/** An apartment created via the backend API for the duration of a single test. */
export interface TestApartment {
  id: string
  floor: number
  totalRent: number
  tenants: { id: string; firstName: string; lastName: string }[]
}

/** Creates an apartment on the second floor in the given property via the backend API, vacant unless a first tenant is given. */
export async function createTestApartment(
  request: APIRequestContext,
  propertyId: string,
  tenant?: { firstName: string; lastName: string },
): Promise<TestApartment> {
  const response = await request.post(`${BACKEND_BASE_URL}/api/properties/${propertyId}/apartments`, {
    data: {
      apartment: { floor: 2, areaSquareMeters: 64.5, totalRent: 850, coldRent: 650, additionalCosts: 200 },
      ...(tenant ? { tenant } : {}),
    },
    headers: await csrfHeaders(request),
  })
  expect(response.ok()).toBe(true)
  return (await response.json()) as TestApartment
}
