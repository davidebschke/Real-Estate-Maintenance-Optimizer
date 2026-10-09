import axios from 'axios'

/** Returns the localized message the backend put into an error response body, or null if the error carries none. */
export function getServerErrorMessage(error: unknown): string | null {
  if (!axios.isAxiosError(error)) return null
  const message = (error.response?.data as { message?: unknown } | undefined)?.message
  return typeof message === 'string' && message.length > 0 ? message : null
}
