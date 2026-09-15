import { describe, expect, it } from 'vitest'
import {
  DURATION_OPTIONS,
  RECURRENCE_INTERVAL_OPTIONS,
  generateTimeSlotOptions,
  generateUpcomingDayOptions,
} from '@/utils/appointmentSchedulingOptions'

describe('appointmentSchedulingOptions', () => {
  it('generates the requested number of upcoming days, starting from the reference date', () => {
    const options = generateUpcomingDayOptions(new Date(2026, 7, 11), 'de-DE', 3)

    expect(options).toEqual([
      { value: '2026-08-11', label: expect.stringContaining('11.08.2026') },
      { value: '2026-08-12', label: expect.stringContaining('12.08.2026') },
      { value: '2026-08-13', label: expect.stringContaining('13.08.2026') },
    ])
  })

  it('generates half-hour time slots within business hours by default', () => {
    const options = generateTimeSlotOptions()

    expect(options[0]).toEqual({ value: '07:00', label: '07:00' })
    expect(options).toContainEqual({ value: '13:00', label: '13:00' })
    expect(options[options.length - 1]).toEqual({ value: '19:00', label: '19:00' })
  })

  it('exposes a fixed, non-empty set of duration and recurrence-interval options', () => {
    expect(DURATION_OPTIONS.length).toBeGreaterThan(0)
    expect(DURATION_OPTIONS).toContainEqual({ key: 'allDay', minutes: 1440 })
    expect(RECURRENCE_INTERVAL_OPTIONS).toContainEqual({ key: 'quarterly', months: 3 })
  })
})
