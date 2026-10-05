import { describe, expect, it } from 'vitest'
import { formatKilometers, formatTravelDuration } from '@/utils/travelFormat'

describe('travelFormat', () => {
  it('formats meters as kilometers with one decimal place in the given locale', () => {
    expect(formatKilometers(6432, 'de')).toBe('6,4')
    expect(formatKilometers(6432, 'en')).toBe('6.4')
    expect(formatKilometers(0, 'de')).toBe('0')
  })

  it('formats a driving time in seconds rounded up to the next full minute', () => {
    expect(formatTravelDuration(721, 'de')).toBe('13 Min')
    expect(formatTravelDuration(3900, 'en')).toBe('1 hr 5 min')
    expect(formatTravelDuration(0, 'de')).toBe('0 Min')
  })
})
