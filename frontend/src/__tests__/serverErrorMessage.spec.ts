import { describe, expect, it } from 'vitest'
import { AxiosError, type AxiosResponse } from 'axios'
import { getServerErrorMessage } from '@/utils/serverErrorMessage'

/** Builds the axios error a rejected request carries for the given response body. */
function createAxiosError(data: unknown): AxiosError {
  const error = new AxiosError('Request failed with status code 409')
  error.response = { status: 409, data } as AxiosResponse
  return error
}

describe('getServerErrorMessage', () => {
  it('returns the message of the backend error response', () => {
    expect(getServerErrorMessage(createAxiosError({ message: 'An apartment can have at most 10 tenants.' }))).toBe(
      'An apartment can have at most 10 tenants.',
    )
  })

  it('returns null when the response has no usable message', () => {
    expect(getServerErrorMessage(createAxiosError({}))).toBeNull()
    expect(getServerErrorMessage(createAxiosError({ message: '' }))).toBeNull()
    expect(getServerErrorMessage(createAxiosError({ message: 42 }))).toBeNull()
    expect(getServerErrorMessage(createAxiosError(undefined))).toBeNull()
  })

  it('returns null for an error without response and for anything that is not an axios error', () => {
    expect(getServerErrorMessage(new AxiosError('Network Error'))).toBeNull()
    expect(getServerErrorMessage(new Error('boom'))).toBeNull()
    expect(getServerErrorMessage('boom')).toBeNull()
  })
})
