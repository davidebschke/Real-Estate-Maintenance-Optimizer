import { test as setup, expect } from '@playwright/test'
import { AUTH_STATE_PATH, BACKEND_BASE_URL, csrfHeaders, e2eCredentials } from './support/apiSession.js'

setup('log in as the e2e account', async ({ request }) => {
  const versionResponse = await request.get(`${BACKEND_BASE_URL}/api/version`)
  expect(versionResponse.ok()).toBe(true)

  const loginResponse = await request.post(`${BACKEND_BASE_URL}/api/auth/login`, {
    data: e2eCredentials(),
    headers: await csrfHeaders(request),
  })
  expect(loginResponse.ok(), 'the e2e account must be able to log in, check E2E_USERNAME/E2E_PASSWORD').toBe(true)

  await request.storageState({ path: AUTH_STATE_PATH })
})
