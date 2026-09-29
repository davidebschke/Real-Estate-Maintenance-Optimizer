import { describe, expect, it } from 'vitest'
import { DEFAULT_REDIRECT_PATH, resolveSafeRedirect } from '@/utils/safeRedirect'

describe('resolveSafeRedirect', () => {
  it.each(['/', '/calendar', '/properties?view=list', '/calendar#today'])('keeps the in-app path %s', (path) => {
    expect(resolveSafeRedirect(path)).toBe(path)
  })

  it.each([
    'https://evil.example',
    '//evil.example',
    '/\\evil.example',
    'javascript:alert(1)',
    'calendar',
    '',
    '/login',
    '/login?redirect=/calendar',
  ])('falls back to the start page for the unsafe or pointless target %s', (target) => {
    expect(resolveSafeRedirect(target)).toBe(DEFAULT_REDIRECT_PATH)
  })

  it('uses the first value of a repeated query parameter', () => {
    expect(resolveSafeRedirect(['/calendar', 'https://evil.example'])).toBe('/calendar')
  })

  it.each([undefined, null, 42])('falls back to the start page for the non-string value %s', (value) => {
    expect(resolveSafeRedirect(value)).toBe(DEFAULT_REDIRECT_PATH)
  })
})
