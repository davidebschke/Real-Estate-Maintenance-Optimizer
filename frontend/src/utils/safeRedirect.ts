/** Target used whenever a requested redirect is missing or not a safe in-app path. */
export const DEFAULT_REDIRECT_PATH = '/'

/**
 * Returns the given post-login redirect target only if it is a path inside this app, so a crafted
 * `?redirect=https://evil.example` or `//evil.example` link can never send a freshly logged-in user elsewhere.
 */
export function resolveSafeRedirect(redirect: unknown): string {
  const candidate = Array.isArray(redirect) ? redirect[0] : redirect
  if (typeof candidate !== 'string') return DEFAULT_REDIRECT_PATH
  if (!candidate.startsWith('/') || candidate.startsWith('//') || candidate.includes('\\')) {
    return DEFAULT_REDIRECT_PATH
  }
  if (candidate.startsWith('/login')) return DEFAULT_REDIRECT_PATH
  return candidate
}
