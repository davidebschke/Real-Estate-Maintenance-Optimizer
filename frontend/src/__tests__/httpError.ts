import { AxiosError, AxiosHeaders } from 'axios'

/** Builds the axios error a request rejects with for the given HTTP status and response body. */
export function httpError(status: number, data: unknown = {}): AxiosError {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('failed', 'ERR_BAD_REQUEST', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data,
  })
}
