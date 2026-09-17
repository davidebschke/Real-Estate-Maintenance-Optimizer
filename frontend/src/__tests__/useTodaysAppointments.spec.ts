import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useTodaysAppointments } from '@/composables/useTodaysAppointments'
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
    travelDistanceKm: 0,
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.useFakeTimers()
  vi.setSystemTime(new Date(2026, 7, 10, 9, 0))
})

afterEach(() => {
  vi.useRealTimers()
})

describe('useTodaysAppointments', () => {
  it('returns an empty list when there are no appointments', () => {
    const { todaysAppointments } = useTodaysAppointments()

    expect(todaysAppointments.value).toEqual([])
  })

  it('excludes appointments scheduled on other days', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ id: '1', start: new Date(2026, 7, 9, 9, 0) }),
      createAppointment({ id: '2', start: new Date(2026, 7, 11, 9, 0) }),
    ]

    const { todaysAppointments } = useTodaysAppointments()

    expect(todaysAppointments.value).toEqual([])
  })

  it('sorts today\'s appointments from the earliest to the latest start time', () => {
    const store = useAppointmentsStore()
    const later = createAppointment({ id: '1', start: new Date(2026, 7, 10, 15, 30) })
    const earlier = createAppointment({ id: '2', start: new Date(2026, 7, 10, 8, 30) })
    store.appointments = [later, earlier]

    const { todaysAppointments } = useTodaysAppointments()

    expect(todaysAppointments.value.map((appointment) => appointment.id)).toEqual(['2', '1'])
  })

  it('includes both completed and not-yet-completed appointments scheduled for today', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ id: '1', completed: true }),
      createAppointment({ id: '2', completed: false }),
    ]

    const { todaysAppointments } = useTodaysAppointments()

    expect(todaysAppointments.value).toHaveLength(2)
  })
})
