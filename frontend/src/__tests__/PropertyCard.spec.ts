import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import PropertyCard from '@/components/properties/PropertyCard.vue'
import type { Property } from '@/types/property'
import type { Appointment } from '@/types/appointment'

/** Builds a sample property for tests, with overridable fields. */
function createProperty(overrides: Partial<Property> = {}): Property {
  return {
    id: '1',
    name: 'Wohnanlage Sonnenhof',
    address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    icon: 'pi-building',
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

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('PropertyCard', () => {
  it('renders the icon, name, address and appointment counts', () => {
    const wrapper = mount(PropertyCard, {
      props: {
        property: createProperty(),
        openCount: 3,
        completedCount: 12,
        nextAppointment: null,
      },
      global: { plugins: [i18n] },
    })

    expect(wrapper.find('.property-card__icon').classes()).toContain('pi-building')
    expect(wrapper.find('.property-card__name').text()).toBe('Wohnanlage Sonnenhof')
    expect(wrapper.find('.property-card__address').text()).toBe(
      'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    )
    const statValues = wrapper.findAll('.property-card__stat-value')
    expect(statValues[0]!.text()).toBe('3')
    expect(statValues[1]!.text()).toBe('12')
  })

  it('renders a very long name and address in full without truncating the content', () => {
    const longName = 'Wohnanlage '.repeat(20).trim()
    const longAddress = 'Sehr lange Musterstraße '.repeat(10).trim()
    const wrapper = mount(PropertyCard, {
      props: {
        property: createProperty({ name: longName, address: longAddress }),
        openCount: 0,
        completedCount: 0,
        nextAppointment: null,
      },
      global: { plugins: [i18n] },
    })

    expect(wrapper.find('.property-card__name').text()).toBe(longName)
    expect(wrapper.find('.property-card__address').text()).toBe(longAddress)
  })

  it('shows a placeholder when there is no next appointment', () => {
    const wrapper = mount(PropertyCard, {
      props: {
        property: createProperty(),
        openCount: 0,
        completedCount: 2,
        nextAppointment: null,
      },
      global: { plugins: [i18n] },
    })

    expect(wrapper.find('.property-card__next-appointment').exists()).toBe(false)
    expect(wrapper.find('.property-card__no-next-appointment').exists()).toBe(true)
  })

  it('shows the next appointment as a clickable link and emits its id when clicked', async () => {
    const wrapper = mount(PropertyCard, {
      props: {
        property: createProperty(),
        openCount: 1,
        completedCount: 0,
        nextAppointment: createAppointment({ id: '42' }),
      },
      global: { plugins: [i18n] },
    })

    const link = wrapper.find('.property-card__next-appointment')
    expect(link.exists()).toBe(true)
    expect(link.text()).toContain('20.08.2026')

    await link.trigger('click')

    expect(wrapper.emitted('open-appointment')).toEqual([['42']])
  })
})
