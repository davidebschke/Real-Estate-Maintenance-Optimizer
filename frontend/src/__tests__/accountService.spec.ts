import { afterEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import {
  changeAppointmentBuffer,
  changePassword,
  changeUsername,
  toAccountFailureReason,
} from '@/services/accountService'
import { httpError } from '@/__tests__/httpError'

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

    const user = await changeUsername('neuer-name', 'geheimes passwort')

    expect(user.username).toBe('neuer-name')
    expect(axios.put).toHaveBeenCalledWith(expect.stringContaining('/api/account/username'), {
      username: 'neuer-name',
      currentPassword: 'geheimes passwort',
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
    [409, 'generic'],
    [422, 'currentPasswordIncorrect'],
    [400, 'invalidInput'],
    [429, 'tooManyRequests'],
    [403, 'generic'],
    [500, 'generic'],
  ])('maps an HTTP %i failure to the reason %s', (status, reason) => {
    expect(toAccountFailureReason(httpError(status))).toBe(reason)
  })

  it('maps a 409 to the conflict reason of the form that sent the request', () => {
    expect(toAccountFailureReason(httpError(409), 'usernameTaken')).toBe('usernameTaken')
  })

  it('maps a failure without HTTP response to a generic reason', () => {
    expect(toAccountFailureReason(new Error('offline'))).toBe('generic')
  })
})
