import { afterEach, describe, expect, it, vi } from 'vitest'
import axios, { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { configureHttpClient, isSessionIndependentRequest } from '@/services/httpClient'

/** Runs the rejection handler of the most recently installed response interceptor, like axios would for a failed request. */
async function rejectThroughInterceptor(status: number, url: string) {
  const handlers = (
    axios.interceptors.response as unknown as {
      handlers: Array<{ rejected: (error: AxiosError) => Promise<never> } | null>
    }
  ).handlers.filter(Boolean)
  const config = { url, headers: new AxiosHeaders() } as InternalAxiosRequestConfig
  const error = new AxiosError('failed', 'ERR_BAD_REQUEST', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data: {},
  })
  await expect(handlers[handlers.length - 1]!.rejected(error)).rejects.toBe(error)
}

let removeInterceptor: (() => void) | undefined

afterEach(() => {
  removeInterceptor?.()
  removeInterceptor = undefined
})

describe('configureHttpClient', () => {
  it('sends cookies with every request and echoes the CSRF cookie as header, also cross-origin', () => {
    removeInterceptor = configureHttpClient({ onSessionExpired: vi.fn<() => void>() })

    expect(axios.defaults.withCredentials).toBe(true)
    expect(axios.defaults.withXSRFToken).toBe(true)
    expect(axios.defaults.xsrfCookieName).toBe('XSRF-TOKEN')
    expect(axios.defaults.xsrfHeaderName).toBe('X-XSRF-TOKEN')
  })

  it('reports an expired session when a data request is answered with 401', async () => {
    const onSessionExpired = vi.fn<() => void>()
    removeInterceptor = configureHttpClient({ onSessionExpired })

    await rejectThroughInterceptor(401, 'http://localhost:8080/api/properties')

    expect(onSessionExpired).toHaveBeenCalledOnce()
  })

  it('does not report an expired session for a wrong password or a missing session at startup', async () => {
    const onSessionExpired = vi.fn<() => void>()
    removeInterceptor = configureHttpClient({ onSessionExpired })

    await rejectThroughInterceptor(401, 'http://localhost:8080/api/auth/login')
    await rejectThroughInterceptor(401, '/api/auth/me')

    expect(onSessionExpired).not.toHaveBeenCalled()
  })

  it('does not report an expired session for any other failure', async () => {
    const onSessionExpired = vi.fn<() => void>()
    removeInterceptor = configureHttpClient({ onSessionExpired })

    await rejectThroughInterceptor(403, '/api/properties')
    await rejectThroughInterceptor(500, '/api/appointments')

    expect(onSessionExpired).not.toHaveBeenCalled()
  })
})

describe('isSessionIndependentRequest', () => {
  it.each(['/api/auth/login', '/api/auth/demo-account', '/api/auth/logout', 'http://x/api/auth/me'])(
    'treats %s as session independent',
    (url) => {
      expect(isSessionIndependentRequest(url)).toBe(true)
    },
  )

  it.each(['/api/properties', '/api/appointments/1', undefined])('treats %s as session bound', (url) => {
    expect(isSessionIndependentRequest(url)).toBe(false)
  })
})
