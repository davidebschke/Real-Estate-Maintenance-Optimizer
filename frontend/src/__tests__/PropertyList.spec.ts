import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { i18n } from '@/i18n'
import PropertyList from '@/components/properties/PropertyList.vue'
import { usePropertiesStore } from '@/stores/properties'
import { useAppointmentsStore } from '@/stores/appointments'
import * as propertyService from '@/services/propertyService'
import * as appointmentService from '@/services/appointmentService'
import type { Property } from '@/types/property'
import type { Appointment } from '@/types/appointment'

vi.mock('@/services/propertyService')
vi.mock('@/services/appointmentService')

/** Builds a sample property for tests, with overridable fields. */
function createProperty(overrides: Partial<Property> = {}): Property {
  return {
    id: '1',
    name: 'Wohnanlage Sonnenhof',
    address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    icon: 'pi-building',
    latitude: 50.94,
    longitude: 6.88,
    ...overrides,
  }
}

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '1',
    seriesId: null,
    title: 'Heizungswartung',
    propertyId: '1',
    propertyName: 'Wohnanlage Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: '',
    category: 'maintenance',
    start: new Date(2026, 7, 20, 9, 0),
    end: new Date(2026, 7, 20, 10, 0),
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
  vi.mocked(propertyService.fetchProperties).mockReset()
  vi.mocked(appointmentService.fetchAppointments).mockReset().mockResolvedValue([])
})

afterEach(() => {
  vi.useRealTimers()
})

describe('PropertyList', () => {
  it('shows an empty-state message when there are no properties', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([])
    const wrapper = mount(PropertyList, { global: { plugins: [i18n] } })
    await flushPromises()

    expect(wrapper.find('.property-list__empty').exists()).toBe(true)
  })

  it('shows an error message when loading properties fails', async () => {
    vi.mocked(propertyService.fetchProperties).mockRejectedValue(new Error('network error'))
    const wrapper = mount(PropertyList, { global: { plugins: [i18n] } })
    await flushPromises()

    expect(wrapper.find('.property-list__error').exists()).toBe(true)
    expect(wrapper.find('.property-list__empty').exists()).toBe(false)
  })

  it('renders one card per property with its appointment summary', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([
      createProperty({ id: '1', name: 'Wohnanlage Sonnenhof' }),
      createProperty({ id: '2', name: 'Wohnpark Lindenthal' }),
    ])
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '10', propertyId: '1', completed: true }),
    ])

    const wrapper = mount(PropertyList, { global: { plugins: [i18n] } })
    await flushPromises()

    const cards = wrapper.findAll('.property-card')
    expect(cards).toHaveLength(2)
    expect(cards[0]!.find('.property-card__name').text()).toBe('Wohnanlage Sonnenhof')
  })

  it('opens the appointment detail view when a next-appointment link is clicked', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([createProperty({ id: '1' })])
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '10', propertyId: '1', completed: false }),
    ])

    const wrapper = mount(PropertyList, { global: { plugins: [i18n] } })
    await flushPromises()
    const appointmentsStore = useAppointmentsStore()

    await wrapper.find('.property-card__next-appointment').trigger('click')

    expect(appointmentsStore.activeDetailAppointmentId).toBe('10')
  })

  it('opens the property creation dialog when the pinned create card is clicked', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([])
    const wrapper = mount(PropertyList, { global: { plugins: [i18n] } })
    await flushPromises()
    const propertiesStore = usePropertiesStore()

    await wrapper.find('.property-create-card').trigger('click')

    expect(propertiesStore.isCreateDialogOpen).toBe(true)
  })
})
