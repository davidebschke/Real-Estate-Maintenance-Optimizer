import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useCalendarAppointments } from '@/composables/useCalendarAppointments'
import { useAppointmentsStore } from '@/stores/appointments'
import type { Appointment } from '@/types/appointment'

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '1',
    seriesId: null,
    title: 'Heizungswartung',
    propertyId: 'property-1',
    propertyName: 'Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: '',
    category: 'maintenance',
    start: new Date(2026, 7, 10, 8, 30),
    end: new Date(2026, 7, 10, 10, 0),
    locked: false,
    recurring: false,
    recurrenceIntervalMonths: null,
    materials: [],
    history: [],
    travelDistanceKm: 12,
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
})

describe('useCalendarAppointments', () => {
  it('starts with no appointments and no calendar events', () => {
    const { appointments, events } = useCalendarAppointments()

    expect(appointments.value).toEqual([])
    expect(events.value).toEqual([])
  })

  it('converts an appointment into a vue-cal event with formatted date-times and a category class', () => {
    const store = useAppointmentsStore()
    store.appointments = [createAppointment()]

    const { events } = useCalendarAppointments()

    expect(events.value).toEqual([
      {
        appointmentId: '1',
        start: '2026-08-10 08:30',
        end: '2026-08-10 10:00',
        title: 'Heizungswartung',
        content: 'Sonnenhof',
        class: 'calendar-event--maintenance',
      },
    ])
  })

  it('shortens a completed event to its actual end and adds the completed class', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ completed: true, actualEnd: new Date(2026, 7, 10, 9, 15) }),
    ]

    const { events } = useCalendarAppointments()

    expect(events.value).toEqual([
      expect.objectContaining({
        end: '2026-08-10 09:15',
        class: 'calendar-event--maintenance calendar-event--completed',
      }),
    ])
  })

  it('adds the locked class for a fixed appointment', () => {
    const store = useAppointmentsStore()
    store.appointments = [createAppointment({ locked: true })]

    const { events } = useCalendarAppointments()

    expect(events.value).toEqual([
      expect.objectContaining({ class: 'calendar-event--maintenance calendar-event--locked' }),
    ])
  })

  it('extends a completed event when the actual end is later than the planned end', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ completed: true, actualEnd: new Date(2026, 7, 10, 11, 0) }),
    ]

    const { events } = useCalendarAppointments()

    expect(events.value[0]!.end).toBe('2026-08-10 11:00')
  })

  it('returns null for a day without scheduled appointments', () => {
    const { getDaySummary } = useCalendarAppointments()

    expect(getDaySummary(new Date(2026, 7, 10))).toBeNull()
  })

  it('aggregates the appointment count and travel distance for a given day only', () => {
    const store = useAppointmentsStore()
    store.appointments = [
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

    const { getDaySummary } = useCalendarAppointments()

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
