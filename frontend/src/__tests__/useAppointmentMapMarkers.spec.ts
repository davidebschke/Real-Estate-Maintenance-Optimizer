import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAppointmentMapMarkers } from '@/composables/useAppointmentMapMarkers'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertiesStore } from '@/stores/properties'
import * as geocodingService from '@/services/geocodingService'
import type { Appointment } from '@/types/appointment'
import type { Property } from '@/types/property'

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

/** Builds a sample property for tests, with overridable fields. */
function createProperty(overrides: Partial<Property> = {}): Property {
  return {
    id: 'property-1',
    name: 'Wohnanlage Sonnenhof',
    address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    icon: 'pi-building',
    latitude: 50.9,
    longitude: 6.9,
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
  it("uses the property's stored coordinates without geocoding its address", async () => {
    const appointmentsStore = useAppointmentsStore()
    const propertiesStore = usePropertiesStore()
    appointmentsStore.appointments = [createAppointment({ id: '1' })]
    propertiesStore.properties = [createProperty({ latitude: 50.9, longitude: 6.9 })]

    const { markers, hasGeocodingError } = useAppointmentMapMarkers()
    await flushPromises()

    expect(markers.value).toEqual([
      { appointment: appointmentsStore.appointments[0], position: 1, lat: 50.9, lng: 6.9 },
    ])
    expect(hasGeocodingError.value).toBe(false)
    expect(geocodingService.geocodeAddress).not.toHaveBeenCalled()
  })

  it('falls back to geocoding the address when the property has no stored coordinates', async () => {
    vi.mocked(geocodingService.geocodeAddress).mockResolvedValue({ lat: 50.8, lng: 6.8 })
    const appointmentsStore = useAppointmentsStore()
    const propertiesStore = usePropertiesStore()
    appointmentsStore.appointments = [createAppointment({ id: '1' })]
    propertiesStore.properties = [createProperty({ latitude: null, longitude: null })]

    const { markers } = useAppointmentMapMarkers()
    await flushPromises()

    expect(geocodingService.geocodeAddress).toHaveBeenCalledWith(
      'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    )
    expect(markers.value).toEqual([
      { appointment: appointmentsStore.appointments[0], position: 1, lat: 50.8, lng: 6.8 },
    ])
  })

  it('resolves appointments to numbered markers in chronological order', async () => {
    const appointmentsStore = useAppointmentsStore()
    const propertiesStore = usePropertiesStore()
    appointmentsStore.appointments = [
      createAppointment({
        id: '1',
        title: 'Später dran',
        propertyId: 'property-1',
        start: new Date(2026, 7, 10, 15, 0),
      }),
      createAppointment({
        id: '2',
        title: 'Zuerst dran',
        propertyId: 'property-2',
        start: new Date(2026, 7, 10, 8, 30),
      }),
    ]
    propertiesStore.properties = [
      createProperty({ id: 'property-1', latitude: 50.9, longitude: 6.9 }),
      createProperty({ id: 'property-2', latitude: 50.8, longitude: 6.8 }),
    ]

    const { markers } = useAppointmentMapMarkers()
    await flushPromises()

    expect(markers.value).toEqual([
      { appointment: appointmentsStore.appointments[1], position: 1, lat: 50.8, lng: 6.8 },
      { appointment: appointmentsStore.appointments[0], position: 2, lat: 50.9, lng: 6.9 },
    ])
  })

  it('resolves the same property only once for multiple appointments', async () => {
    const appointmentsStore = useAppointmentsStore()
    const propertiesStore = usePropertiesStore()
    appointmentsStore.appointments = [
      createAppointment({ id: '1', start: new Date(2026, 7, 10, 8, 0) }),
      createAppointment({ id: '2', start: new Date(2026, 7, 10, 9, 0) }),
    ]
    propertiesStore.properties = [createProperty({ latitude: null, longitude: null })]
    vi.mocked(geocodingService.geocodeAddress).mockResolvedValue({ lat: 50.9, lng: 6.9 })

    useAppointmentMapMarkers()
    await flushPromises()

    expect(geocodingService.geocodeAddress).toHaveBeenCalledTimes(1)
  })

  it('flags a geocoding error and skips markers whose address could not be resolved', async () => {
    const appointmentsStore = useAppointmentsStore()
    const propertiesStore = usePropertiesStore()
    appointmentsStore.appointments = [createAppointment({ id: '1' })]
    propertiesStore.properties = [createProperty({ latitude: null, longitude: null })]
    vi.mocked(geocodingService.geocodeAddress).mockResolvedValue(null)

    const { markers, hasGeocodingError } = useAppointmentMapMarkers()
    await flushPromises()

    expect(markers.value).toEqual([])
    expect(hasGeocodingError.value).toBe(true)
  })
})
