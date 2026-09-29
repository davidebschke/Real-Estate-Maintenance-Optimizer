import { expect, type APIRequestContext } from '@playwright/test'
import { BACKEND_BASE_URL, csrfHeaders } from './apiSession.js'

export { BACKEND_BASE_URL } from './apiSession.js'

/** Address of every test property, split into the fields of the property form so tests can re-enter it. */
export const TEST_PROPERTY_ADDRESS = {
  street: 'Aachener Str.',
  houseNumber: '512',
  postalCode: '50933',
  city: 'Köln-Braunsenfeld',
} as const

/** A property created via the backend API for the duration of a single test. */
export interface TestProperty {
  id: string
  name: string
  address: string
}

/** Creates a uniquely named property with stored coordinates via the backend API for the logged-in e2e account. */
export async function createTestProperty(request: APIRequestContext): Promise<TestProperty> {
  const response = await request.post(`${BACKEND_BASE_URL}/api/properties`, {
    data: {
      name: `E2E Sonnenhof ${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      address: `${TEST_PROPERTY_ADDRESS.street} ${TEST_PROPERTY_ADDRESS.houseNumber}, ${TEST_PROPERTY_ADDRESS.postalCode} ${TEST_PROPERTY_ADDRESS.city}`,
      latitude: 50.937634,
      longitude: 6.8922212,
    },
    headers: await csrfHeaders(request),
  })
  expect(response.ok()).toBe(true)
  return (await response.json()) as TestProperty
}

/** Deletes the property with the given id via the backend API, cascading to every appointment still referencing it. */
export async function deleteTestProperty(request: APIRequestContext, id: string) {
  await request.delete(`${BACKEND_BASE_URL}/api/properties/${id}`, { headers: await csrfHeaders(request) })
}
