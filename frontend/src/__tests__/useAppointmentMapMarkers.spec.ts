import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAppointmentMapMarkers } from '@/composables/useAppointmentMapMarkers'
import { useAppointmentsStore } from '@/stores/appointments'
import * as geocodingService from '@/services/geocodingService'
import type { Appointment } from '@/types/appointment'

vi.mock('@/services/geocodingService')

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '1',
    seriesId: null,
    title: 'Heizungswartung',
    propertyId: 'property-1',
    propertyName: 'Wohnanlage Sonnenhof',
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
    travelDistanceKm: 6.4,
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.useFakeTimers()
  vi.setSystemTime(new Date(2026, 7, 10, 9, 0))
  vi.mocked(geocodingService.geocodeAddress).mockReset()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('useAppointmentMapMarkers', () => {
  it('resolves appointments to numbered markers in chronological order', async () => {
    vi.mocked(geocodingService.geocodeAddress).mockImplementation(async (address) =>
      address.includes('Sonnenhof') ? { lat: 50.9, lng: 6.9 } : { lat: 50.8, lng: 6.8 },
    )
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({
        id: '1',
        title: 'Später dran',
        propertyAddress: 'Sonnenhof-Straße 1',
        start: new Date(2026, 7, 10, 15, 0),
      }),
      createAppointment({
        id: '2',
        title: 'Zuerst dran',
        propertyAddress: 'Andere Straße 2',
        start: new Date(2026, 7, 10, 8, 30),
      }),
    ]

    const { markers, hasGeocodingError } = useAppointmentMapMarkers()
    await flushPromises()

    expect(markers.value).toEqual([
      { appointment: store.appointments[1], position: 1, lat: 50.8, lng: 6.8 },
      { appointment: store.appointments[0], position: 2, lat: 50.9, lng: 6.9 },
    ])
    expect(hasGeocodingError.value).toBe(false)
  })

  it('resolves the same address only once for multiple appointments', async () => {
    vi.mocked(geocodingService.geocodeAddress).mockResolvedValue({ lat: 50.9, lng: 6.9 })
    const store = useAppointmentsStore()
    store.appointments = [
      createAppointment({ id: '1', start: new Date(2026, 7, 10, 8, 0) }),
      createAppointment({ id: '2', start: new Date(2026, 7, 10, 9, 0) }),
    ]

    useAppointmentMapMarkers()
    await flushPromises()

    expect(geocodingService.geocodeAddress).toHaveBeenCalledTimes(1)
  })

  it('flags a geocoding error and skips markers whose address could not be resolved', async () => {
    vi.mocked(geocodingService.geocodeAddress).mockResolvedValue(null)
    const store = useAppointmentsStore()
    store.appointments = [createAppointment({ id: '1' })]

    const { markers, hasGeocodingError } = useAppointmentMapMarkers()
    await flushPromises()

    expect(markers.value).toEqual([])
    expect(hasGeocodingError.value).toBe(true)
  })
})
