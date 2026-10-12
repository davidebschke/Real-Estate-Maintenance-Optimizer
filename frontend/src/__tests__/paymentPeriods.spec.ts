import { describe, expect, it } from 'vitest'
import {
  formatMonthYear,
  formatNumericMonthYear,
  formatShortMonth,
  generatePeriodOptions,
  parsePeriodKey,
  periodKey,
} from '@/utils/paymentPeriods'

describe('paymentPeriods', () => {
  it('builds a zero-padded period key and parses it back', () => {
    expect(periodKey(2026, 3)).toBe('2026-03')
    expect(parsePeriodKey('2026-03')).toEqual({ year: 2026, month: 3 })
  })

  it('formats a month with its year in German and English', () => {
    expect(formatMonthYear(2026, 10, 'de')).toBe('Oktober 2026')
    expect(formatMonthYear(2026, 10, 'en')).toBe('October 2026')
  })

  it('formats a period key as numeric month and year', () => {
    expect(formatNumericMonthYear('2026-01')).toBe('01/2026')
    expect(formatNumericMonthYear('2026-12')).toBe('12/2026')
  })

  it('formats the short month name in German and English', () => {
    expect(formatShortMonth(10, 'de')).toBe('Okt')
    expect(formatShortMonth(10, 'en')).toBe('Oct')
  })

  it('lists the months from two years before to one year after the anchor year in order', () => {
    const options = generatePeriodOptions(2026, 'de')

    expect(options).toHaveLength(48)
    expect(options[0]).toEqual({ value: '2024-01', label: 'Januar 2024' })
    expect(options[options.length - 1]).toEqual({ value: '2027-12', label: 'Dezember 2027' })
  })

  it('adds an extra month outside the range once and keeps the order chronological', () => {
    const options = generatePeriodOptions(2026, 'en', ['2019-05', '2026-10'])

    expect(options).toHaveLength(49)
    expect(options[0]).toEqual({ value: '2019-05', label: 'May 2019' })
    expect(options.filter((option) => option.value === '2026-10')).toHaveLength(1)
  })
})
