import { describe, expect, it } from 'vitest'
import {
  findNewPasswordProblem,
  isValidAppointmentBuffer,
  isValidUsername,
} from '@/utils/accountFieldValidation'

describe('isValidUsername', () => {
  it.each(['abc', 'David.Ebschke', 'user_name-1', 'a'.repeat(50)])('accepts %s', (username) => {
    expect(isValidUsername(username)).toBe(true)
  })

  it.each(['ab', '', 'with space', 'ümlaut', 'a'.repeat(51), 'semi;colon'])('rejects "%s"', (username) => {
    expect(isValidUsername(username)).toBe(false)
  })
})

describe('findNewPasswordProblem', () => {
  it('accepts a long enough password that differs from the current one', () => {
    expect(findNewPasswordProblem('another passphrase', 'old passphrase')).toBeNull()
  })

  it('reports a password shorter than 8 characters', () => {
    expect(findNewPasswordProblem('short', 'old passphrase')).toBe('tooShort')
  })

  it('counts UTF-8 bytes against the 72 byte limit of BCrypt', () => {
    expect(findNewPasswordProblem('a'.repeat(72), 'old passphrase')).toBeNull()
    expect(findNewPasswordProblem('ä'.repeat(37), 'old passphrase')).toBe('tooLong')
  })

  it('reports a password equal to the current one', () => {
    expect(findNewPasswordProblem('same passphrase', 'same passphrase')).toBe('unchanged')
  })
})

describe('isValidAppointmentBuffer', () => {
  it.each([0, 15, 1440])('accepts %i minutes', (minutes) => {
    expect(isValidAppointmentBuffer(minutes)).toBe(true)
  })

  it.each([null, -1, 1441, 1.5])('rejects %s', (minutes) => {
    expect(isValidAppointmentBuffer(minutes)).toBe(false)
  })
})
