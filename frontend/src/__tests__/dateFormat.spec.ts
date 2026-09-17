import { describe, expect, it } from 'vitest'
import { formatDurationMinutes, isSameDay } from '@/utils/dateFormat'

describe('isSameDay', () => {
  it('returns true for two dates on the same calendar day at different times', () => {
    expect(isSameDay(new Date(2026, 7, 10, 8, 0), new Date(2026, 7, 10, 23, 30))).toBe(true)
  })

  it('returns false for dates on different calendar days', () => {
    expect(isSameDay(new Date(2026, 7, 10, 23, 59), new Date(2026, 7, 11, 0, 0))).toBe(false)
  })
})

describe('formatDurationMinutes', () => {
  it('formats a whole-hour duration in German without a minutes part', () => {
    expect(formatDurationMinutes(120, 'de')).toBe('2 Std')
  })

  it('formats a duration with both hours and minutes in German', () => {
    expect(formatDurationMinutes(90, 'de')).toBe('1 Std 30 Min')
  })

  it('formats a sub-hour duration in English', () => {
    expect(formatDurationMinutes(45, 'en')).toBe('45 min')
  })

  it('formats a zero-minute duration', () => {
    expect(formatDurationMinutes(0, 'de')).toBe('0 Min')
  })
})
