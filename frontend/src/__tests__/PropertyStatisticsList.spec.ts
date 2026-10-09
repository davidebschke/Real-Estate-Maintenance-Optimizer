import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { i18n } from '@/i18n'
import PropertyStatisticsList from '@/components/statistics/PropertyStatisticsList.vue'
import { usePropertiesStore } from '@/stores/properties'
import { useAppointmentsStore } from '@/stores/appointments'
import type { Property } from '@/types/property'
import type { Appointment } from '@/types/appointment'

/** Builds a sample property for tests, with overridable fields. */
function createProperty(overrides: Partial<Property> = {}): Property {
  return {
    id: '1',
    name: 'Wohnanlage Sonnenhof',
    address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    icon: 'pi-building',
    latitude: 50.94,
    longitude: 6.88,
    tenantCount: 0,
    ...overrides,
  }
}

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '10',
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
  i18n.global.locale.value = 'de'
})

describe('PropertyStatisticsList', () => {
  it('shows an empty-state message when there are no properties', () => {
    const wrapper = mount(PropertyStatisticsList, { global: { plugins: [i18n] } })

    expect(wrapper.find('.property-statistics-list__empty').text()).toBe(
      'Es sind noch keine Objekte hinterlegt.',
    )
    expect(wrapper.find('table').exists()).toBe(false)
  })

  it('shows an error message when loading the properties failed', () => {
    usePropertiesStore().hasLoadError = true
    const wrapper = mount(PropertyStatisticsList, { global: { plugins: [i18n] } })

    expect(wrapper.find('.property-statistics-list__error').exists()).toBe(true)
    expect(wrapper.find('.property-statistics-list__empty').exists()).toBe(false)
  })

  it('renders the column headers in the selected language', () => {
    usePropertiesStore().properties = [createProperty()]
    i18n.global.locale.value = 'en'
    const wrapper = mount(PropertyStatisticsList, { global: { plugins: [i18n] } })

    expect(wrapper.findAll('thead th').map((header) => header.text())).toEqual([
      'Property',
      'Open appointments',
      'Completed appointments',
      'Next appointment',
    ])
  })

  it('renders one row per property with its own appointment summary', () => {
    usePropertiesStore().properties = [
      createProperty({ id: '1', name: 'Wohnanlage Sonnenhof' }),
      createProperty({ id: '2', name: 'Wohnpark Lindenthal' }),
    ]
    useAppointmentsStore().appointments = [
      createAppointment({ id: '10', propertyId: '1', completed: true }),
      createAppointment({ id: '11', propertyId: '1', start: new Date(2026, 7, 25, 9, 0) }),
      createAppointment({ id: '12', propertyId: '1', start: new Date(2026, 7, 22, 9, 0), title: 'Dachcheck' }),
    ]
    const wrapper = mount(PropertyStatisticsList, { global: { plugins: [i18n] } })

    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)
    expect(rows[0]!.find('.property-statistics-row__open').text()).toBe('2')
    expect(rows[0]!.find('.property-statistics-row__completed').text()).toBe('1')
    expect(rows[0]!.find('.property-statistics-row__next-title').text()).toBe('Dachcheck')
    expect(rows[1]!.find('.property-statistics-row__open').text()).toBe('0')
    expect(rows[1]!.find('.property-statistics-row__no-next').exists()).toBe(true)
  })

  it('opens the appointment detail view when a next-appointment link is clicked', async () => {
    usePropertiesStore().properties = [createProperty({ id: '1' })]
    const appointmentsStore = useAppointmentsStore()
    appointmentsStore.appointments = [createAppointment({ id: '10', propertyId: '1' })]
    const wrapper = mount(PropertyStatisticsList, { global: { plugins: [i18n] } })

    await wrapper.find('.property-statistics-row__next-link').trigger('click')

    expect(appointmentsStore.activeDetailAppointmentId).toBe('10')
  })
})
