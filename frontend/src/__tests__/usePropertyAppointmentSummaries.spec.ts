import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { usePropertyAppointmentSummaries } from '@/composables/usePropertyAppointmentSummaries'
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

describe('usePropertyAppointmentSummaries', () => {
  it('returns zero counts and no next appointment for a property without any appointments', () => {
    const { getSummaryFor } = usePropertyAppointmentSummaries()

    expect(getSummaryFor('property-1')).toEqual({
      openCount: 0,
      completedCount: 0,
      nextAppointment: null,
    })
  })

  it('counts open and completed appointments separately, ignoring other properties', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ id: '1', propertyId: 'property-1', completed: false }),
      createAppointment({ id: '2', propertyId: 'property-1', completed: true }),
      createAppointment({ id: '3', propertyId: 'property-2', completed: false }),
    ]

    const { getSummaryFor } = usePropertyAppointmentSummaries()

    expect(getSummaryFor('property-1').openCount).toBe(1)
    expect(getSummaryFor('property-1').completedCount).toBe(1)
  })

  it('returns null as the next appointment when every appointment is already completed', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({
        id: '1',
        propertyId: 'property-1',
        completed: true,
        start: new Date(2026, 7, 20),
      }),
    ]

    const { getSummaryFor } = usePropertyAppointmentSummaries()

    expect(getSummaryFor('property-1').nextAppointment).toBeNull()
  })

  it('returns null as the next appointment when the only open appointment already started in the past', () => {
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({
        id: '1',
        propertyId: 'property-1',
        completed: false,
        start: new Date(2026, 7, 10, 8, 0),
      }),
    ]

    const { getSummaryFor } = usePropertyAppointmentSummaries()

    expect(getSummaryFor('property-1').nextAppointment).toBeNull()
  })

  it('picks the earliest upcoming, not-yet-completed appointment as the next appointment', () => {
    const store = useAppointmentsStore()
    const later = createAppointment({
      id: '1',
      propertyId: 'property-1',
      start: new Date(2026, 7, 20),
    })
    const sooner = createAppointment({
      id: '2',
      propertyId: 'property-1',
      start: new Date(2026, 7, 15),
    })
    store.appointments = [later, sooner]

    const { getSummaryFor } = usePropertyAppointmentSummaries()

    expect(getSummaryFor('property-1').nextAppointment?.id).toBe('2')
  })
})
