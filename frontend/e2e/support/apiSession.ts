import process from 'node:process'
import type { APIRequestContext } from '@playwright/test'

export const BACKEND_BASE_URL = 'http://localhost:8080'

/** Where the setup project stores the logged-in browser state every other test starts from. */
export const AUTH_STATE_PATH = 'e2e/.auth/user.json'

/** Credentials of the regular account the e2e tests run as, from E2E_USERNAME (default "debschke") and E2E_PASSWORD. */
export function e2eCredentials(): { username: string; password: string } {
  const password = process.env.E2E_PASSWORD
  if (!password) {
    throw new Error(
      'E2E_PASSWORD must be set to the password of the e2e account (the backend sets it from REMO_AUTH_INITIAL_USER_PASSWORD)',
    )
  }
  return { username: process.env.E2E_USERNAME ?? 'debschke', password }
}

/** Returns the CSRF header matching the XSRF-TOKEN cookie the given request context holds, required for every state-changing API call. */
export async function csrfHeaders(request: APIRequestContext): Promise<Record<string, string>> {
  const { cookies } = await request.storageState()
  const csrfCookie = cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')
  return csrfCookie ? { 'X-XSRF-TOKEN': csrfCookie.value } : {}
}
