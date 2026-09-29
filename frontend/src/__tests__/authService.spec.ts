import { afterEach, describe, expect, it, vi } from 'vitest'
import axios, { AxiosError, AxiosHeaders } from 'axios'
import {
  createDemoAccount,
  fetchCurrentUser,
  login,
  logout,
  toAuthFailureReason,
} from '@/services/authService'

vi.mock('axios', async (importOriginal) => {
  const actual = await importOriginal<typeof import('axios')>()
  return {
    ...actual,
    default: {
      ...actual.default,
      get: vi.fn<typeof actual.default.get>(),
      post: vi.fn<typeof actual.default.post>(),
      isAxiosError: actual.isAxiosError,
    },
  }
})

/** Builds the axios error a request rejects with for the given HTTP status. */
function httpError(status: number): AxiosError {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('failed', 'ERR_BAD_REQUEST', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data: {},
  })
}

const regularUserDto = {
  username: 'debschke',
  displayName: 'David Ebschke',
  demoAccount: false,
  expiresAt: null,
  remainingPropertyCreations: null,
  remainingAppointmentCreations: null,
}

afterEach(() => {
  vi.mocked(axios.get).mockReset()
  vi.mocked(axios.post).mockReset()
})

describe('authService', () => {
  it('fetches the logged-in account', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: regularUserDto })

    const user = await fetchCurrentUser()

    expect(user).toEqual({ ...regularUserDto, expiresAt: null })
    expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('/api/auth/me'))
  })

  it('returns null instead of failing when there is no valid session', async () => {
    vi.mocked(axios.get).mockRejectedValue(httpError(401))

    expect(await fetchCurrentUser()).toBeNull()
  })

  it('rethrows any other failure while fetching the logged-in account', async () => {
    vi.mocked(axios.get).mockRejectedValue(httpError(500))

    await expect(fetchCurrentUser()).rejects.toBeInstanceOf(AxiosError)
  })

  it('logs in with the given credentials', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: regularUserDto })

    const user = await login('debschke', 'geheim')

    expect(user.displayName).toBe('David Ebschke')
    expect(axios.post).toHaveBeenCalledWith(expect.stringContaining('/api/auth/login'), {
      username: 'debschke',
      password: 'geheim',
    })
  })

  it('creates a demo account and converts its expiry to a date', async () => {
    vi.mocked(axios.post).mockResolvedValue({
      data: {
        ...regularUserDto,
        username: 'demo-1',
        displayName: 'Demo',
        demoAccount: true,
        expiresAt: '2026-09-29T16:00:00Z',
        remainingPropertyCreations: 3,
        remainingAppointmentCreations: 3,
      },
    })

    const user = await createDemoAccount()

    expect(user.demoAccount).toBe(true)
    expect(user.expiresAt).toEqual(new Date('2026-09-29T16:00:00Z'))
    expect(user.remainingPropertyCreations).toBe(3)
    expect(axios.post).toHaveBeenCalledWith(expect.stringContaining('/api/auth/demo-account'))
  })

  it('logs out', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: undefined })

    await logout()

    expect(axios.post).toHaveBeenCalledWith(expect.stringContaining('/api/auth/logout'))
  })

  it.each([
    [401, 'invalidCredentials'],
    [429, 'tooManyRequests'],
    [500, 'generic'],
    [403, 'generic'],
  ])('maps an HTTP %i failure to the reason %s', (status, reason) => {
    expect(toAuthFailureReason(httpError(status))).toBe(reason)
  })

  it('maps a failure without HTTP response, e.g. a network error, to the generic reason', () => {
    expect(toAuthFailureReason(new Error('offline'))).toBe('generic')
  })
})
