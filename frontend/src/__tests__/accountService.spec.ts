import { afterEach, describe, expect, it, vi } from 'vitest'
import axios, { AxiosError, AxiosHeaders } from 'axios'
import {
  changeAppointmentBuffer,
  changePassword,
  changeUsername,
  toAccountFailureReason,
} from '@/services/accountService'

vi.mock('axios', async (importOriginal) => {
  const actual = await importOriginal<typeof import('axios')>()
  return {
    ...actual,
    default: {
      ...actual.default,
      put: vi.fn<typeof actual.default.put>(),
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

const userDto = {
  username: 'neuer-name',
  displayName: 'David Ebschke',
  demoAccount: false,
  expiresAt: null,
  remainingPropertyCreations: null,
  remainingAppointmentCreations: null,
  appointmentBufferMinutes: 30,
}

afterEach(() => {
  vi.mocked(axios.put).mockReset()
})

describe('accountService', () => {
  it('changes the username and returns the updated account', async () => {
    vi.mocked(axios.put).mockResolvedValue({ data: userDto })

    const user = await changeUsername('neuer-name')

    expect(user.username).toBe('neuer-name')
    expect(axios.put).toHaveBeenCalledWith(expect.stringContaining('/api/account/username'), {
      username: 'neuer-name',
    })
  })

  it('changes the password by sending the current and the new one', async () => {
    vi.mocked(axios.put).mockResolvedValue({ data: userDto })

    await changePassword('alt-passwort', 'neues-passwort')

    expect(axios.put).toHaveBeenCalledWith(expect.stringContaining('/api/account/password'), {
      currentPassword: 'alt-passwort',
      newPassword: 'neues-passwort',
    })
  })

  it('changes the appointment buffer and returns the updated account', async () => {
    vi.mocked(axios.put).mockResolvedValue({ data: userDto })

    const user = await changeAppointmentBuffer(30)

    expect(user.appointmentBufferMinutes).toBe(30)
    expect(axios.put).toHaveBeenCalledWith(expect.stringContaining('/api/account/appointment-buffer'), {
      appointmentBufferMinutes: 30,
    })
  })

  it.each([
    [409, 'usernameTaken'],
    [422, 'currentPasswordIncorrect'],
    [400, 'invalidInput'],
    [429, 'tooManyRequests'],
    [403, 'generic'],
    [500, 'generic'],
  ])('maps an HTTP %i failure to the reason %s', (status, reason) => {
    expect(toAccountFailureReason(httpError(status))).toBe(reason)
  })

  it('maps a failure without HTTP response to a generic reason', () => {
    expect(toAccountFailureReason(new Error('offline'))).toBe('generic')
  })
})
