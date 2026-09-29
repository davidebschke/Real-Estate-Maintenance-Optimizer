import { describe, expect, it } from 'vitest'
import { toInitials } from '@/utils/initials'

describe('toInitials', () => {
  it.each([
    ['David Ebschke', 'DE'],
    ['Demo', 'D'],
    ['anna maria braun', 'AB'],
    ['Demo-Account', 'DA'],
    ['-Demo-', 'D'],
    ['  Özlem   Yılmaz  ', 'ÖY'],
    ['', ''],
    ['   ', ''],
  ])('returns the initials of "%s" as "%s"', (displayName, initials) => {
    expect(toInitials(displayName)).toBe(initials)
  })
})
