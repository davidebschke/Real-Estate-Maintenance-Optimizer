import { describe, expect, it } from 'vitest'
import {
  isValidHouseNumber,
  isValidPlaceName,
  isValidPostalCode,
} from '@/utils/addressFieldValidation'

describe('addressFieldValidation', () => {
  it('accepts place names with diacritics, digits and common punctuation', () => {
    expect(isValidPlaceName('Aachener Str.')).toBe(true)
    expect(isValidPlaceName('Müllerstraße 2-4')).toBe(true)
    expect(isValidPlaceName("Frankfurt a. M. / O'Brien")).toBe(true)
    expect(isValidPlaceName('O’Brien Platz – Nord')).toBe(true)
  })

  it('rejects empty place names and ones with disallowed characters', () => {
    expect(isValidPlaceName('')).toBe(false)
    expect(isValidPlaceName('   ')).toBe(false)
    expect(isValidPlaceName('Hauptstr<script>')).toBe(false)
  })

  it('accepts a house number of one to five digits only', () => {
    expect(isValidHouseNumber('7')).toBe(true)
    expect(isValidHouseNumber('12345')).toBe(true)
    expect(isValidHouseNumber('123456')).toBe(false)
    expect(isValidHouseNumber('7a')).toBe(false)
    expect(isValidHouseNumber('')).toBe(false)
  })

  it('accepts a postal code of exactly five digits only', () => {
    expect(isValidPostalCode('52062')).toBe(true)
    expect(isValidPostalCode('5206')).toBe(false)
    expect(isValidPostalCode('520620')).toBe(false)
    expect(isValidPostalCode('5206a')).toBe(false)
  })
})
