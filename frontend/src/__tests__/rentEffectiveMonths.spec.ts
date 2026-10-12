import { describe, expect, it } from 'vitest'
import { currentMonthKey, listRentEffectiveMonths } from '@/utils/rentEffectiveMonths'

describe('currentMonthKey', () => {
  it('builds the zero-padded YYYY-MM key of the date', () => {
    expect(currentMonthKey(new Date(2026, 2, 31))).toBe('2026-03')
    expect(currentMonthKey(new Date(2026, 11, 1))).toBe('2026-12')
  })
})

describe('listRentEffectiveMonths', () => {
  it('lists the months from the current one back to January 2000, newest first', () => {
    const options = listRentEffectiveMonths(new Date(2026, 9, 12), 'de')

    expect(options[0]).toEqual({ value: '2026-10', label: 'Oktober 2026' })
    expect(options[1]).toEqual({ value: '2026-09', label: 'September 2026' })
    expect(options[10]).toEqual({ value: '2025-12', label: 'Dezember 2025' })
    expect(options[options.length - 1]).toEqual({ value: '2000-01', label: 'Januar 2000' })
    expect(options).toHaveLength(26 * 12 + 10)
  })

  it('offers no month after the current one', () => {
    const values = listRentEffectiveMonths(new Date(2026, 0, 15), 'en').map((option) => option.value)

    expect(values[0]).toBe('2026-01')
    expect(values).not.toContain('2026-02')
  })

  it('localizes the month names', () => {
    expect(listRentEffectiveMonths(new Date(2026, 9, 12), 'en')[0]!.label).toBe('October 2026')
  })

  it('contains every month exactly once', () => {
    const values = listRentEffectiveMonths(new Date(2026, 9, 12), 'de').map((option) => option.value)

    expect(new Set(values).size).toBe(values.length)
  })
})
