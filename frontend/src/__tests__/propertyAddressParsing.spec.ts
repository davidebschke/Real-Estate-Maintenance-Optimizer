import { describe, expect, it } from 'vitest'
import { parsePropertyAddress } from '@/utils/propertyAddressParsing'

describe('parsePropertyAddress', () => {
  it('parses a simple address without a supplement', () => {
    expect(parsePropertyAddress('Aachener Str. 512, 50933 Köln')).toEqual({
      street: 'Aachener Str.',
      houseNumber: '512',
      addressSupplement: '',
      postalCode: '50933',
      city: 'Köln',
    })
  })

  it('parses an address with a house number supplement', () => {
    expect(parsePropertyAddress('Nordparkstr. 3a, 50733 Köln')).toEqual({
      street: 'Nordparkstr.',
      houseNumber: '3',
      addressSupplement: 'a',
      postalCode: '50733',
      city: 'Köln',
    })
  })

  it('parses a multi-word street and city', () => {
    expect(parsePropertyAddress('Aachener Str. 512, 50933 Köln-Braunsenfeld')).toEqual({
      street: 'Aachener Str.',
      houseNumber: '512',
      addressSupplement: '',
      postalCode: '50933',
      city: 'Köln-Braunsenfeld',
    })
  })

  it('returns null for an address that does not match the expected format', () => {
    expect(parsePropertyAddress('Irgendwas ohne Struktur')).toBeNull()
  })
})
