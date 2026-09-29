import axios, { type AxiosError } from 'axios'

/** Endpoints whose 401 answer is an expected outcome (wrong password, no session yet) rather than an expired session. */
const SESSION_INDEPENDENT_PATHS = ['/api/auth/login', '/api/auth/demo-account', '/api/auth/logout', '/api/auth/me']

/** Options for wiring the shared axios defaults to the session handling. */
export interface HttpClientOptions {
  onSessionExpired: () => void
}

/**
 * Configures the shared axios defaults every service uses: sends the HttpOnly session cookie, echoes the CSRF
 * cookie as header (also cross-origin in local development), and reports an expired session exactly once per 401.
 * Returns a function that removes the installed response interceptor again.
 */
export function configureHttpClient(options: HttpClientOptions): () => void {
  axios.defaults.withCredentials = true
  axios.defaults.withXSRFToken = true
  axios.defaults.xsrfCookieName = 'XSRF-TOKEN'
  axios.defaults.xsrfHeaderName = 'X-XSRF-TOKEN'

  const interceptorId = axios.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
      if (error.response?.status === 401 && !isSessionIndependentRequest(error.config?.url)) {
        options.onSessionExpired()
      }
      return Promise.reject(error)
    },
  )

  return () => axios.interceptors.response.eject(interceptorId)
}

/** Whether the given request URL targets an endpoint that answers 401 without meaning the session expired. */
export function isSessionIndependentRequest(url: string | undefined): boolean {
  if (!url) return false
  return SESSION_INDEPENDENT_PATHS.some((path) => url.endsWith(path))
}
