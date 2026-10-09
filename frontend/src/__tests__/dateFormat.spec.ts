import { describe, expect, it } from 'vitest'
import {
  formatDurationMinutes,
  formatLocalizedDateTime,
  formatLocalizedTime,
  isSameDay,
} from '@/utils/dateFormat'

describe('formatLocalizedTime', () => {
  it('formats the time of day with two-digit hours and minutes in German', () => {
    expect(formatLocalizedTime(new Date(2026, 8, 29, 8, 5), 'de')).toBe('08:05')
  })

  it('formats the time of day in English', () => {
    expect(formatLocalizedTime(new Date(2026, 8, 29, 16, 30), 'en')).toMatch(/04:30\s?PM/)
  })
})

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

describe('formatLocalizedDateTime', () => {
  it('formats the date and time with two-digit parts in German', () => {
    expect(formatLocalizedDateTime(new Date(2026, 7, 20, 9, 5), 'de')).toBe('20.08.2026, 09:05')
  })

  it('formats the date and time in English', () => {
    expect(formatLocalizedDateTime(new Date(2026, 7, 20, 16, 30), 'en')).toMatch(/08\/20\/2026,\s*04:30\s?PM/)
  })
})
