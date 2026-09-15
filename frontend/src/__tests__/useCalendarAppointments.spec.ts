import { describe, expect, it } from 'vitest'
import { useCalendarAppointments } from '@/composables/useCalendarAppointments'
import type { Appointment } from '@/types/appointment'

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '1',
    title: 'Heizungswartung',
    propertyName: 'Sonnenhof',
    category: 'maintenance',
    start: new Date(2026, 7, 10, 8, 30),
    end: new Date(2026, 7, 10, 10, 0),
    travelDistanceKm: 12,
    ...overrides,
  }
}

describe('useCalendarAppointments', () => {
  it('starts with no appointments and no calendar events', () => {
    const { appointments, events } = useCalendarAppointments()

    expect(appointments.value).toEqual([])
    expect(events.value).toEqual([])
  })

  it('converts an appointment into a vue-cal event with formatted date-times and a category class', () => {
    const { appointments, events } = useCalendarAppointments()

    appointments.value = [createAppointment()]

    expect(events.value).toEqual([
      {
        start: '2026-08-10 08:30',
        end: '2026-08-10 10:00',
        title: 'Heizungswartung',
        content: 'Sonnenhof',
        class: 'calendar-event--maintenance',
      },
    ])
  })

  it('returns null for a day without scheduled appointments', () => {
    const { getDaySummary } = useCalendarAppointments()

    expect(getDaySummary(new Date(2026, 7, 10))).toBeNull()
  })

  it('aggregates the appointment count and travel distance for a given day only', () => {
    const { appointments, getDaySummary } = useCalendarAppointments()

    appointments.value = [
      createAppointment({ id: '1', travelDistanceKm: 12 }),
      createAppointment({
        id: '2',
        start: new Date(2026, 7, 10, 13, 15),
        end: new Date(2026, 7, 10, 14, 45),
        travelDistanceKm: 26,
      }),
      createAppointment({
        id: '3',
        start: new Date(2026, 7, 11, 9, 0),
        end: new Date(2026, 7, 11, 10, 0),
        travelDistanceKm: 5,
      }),
    ]

    expect(getDaySummary(new Date(2026, 7, 10))).toEqual({
      appointmentCount: 2,
      travelDistanceKm: 38,
    })
    expect(getDaySummary(new Date(2026, 7, 11))).toEqual({
      appointmentCount: 1,
      travelDistanceKm: 5,
    })
  })
})
