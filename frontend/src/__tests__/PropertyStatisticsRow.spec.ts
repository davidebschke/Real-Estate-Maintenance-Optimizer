import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import PropertyStatisticsRow from '@/components/statistics/PropertyStatisticsRow.vue'
import type { PropertyAppointmentSummary } from '@/composables/usePropertyAppointmentSummaries'
import type { Property } from '@/types/property'
import type { Appointment } from '@/types/appointment'

const property: Property = {
  id: '1',
  name: 'Wohnanlage Sonnenhof',
  address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
  icon: 'pi-building',
  latitude: 50.94,
  longitude: 6.88,
  tenantCount: 3,
}

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: '42',
    seriesId: null,
    title: 'Heizungswartung',
    propertyId: '1',
    propertyName: property.name,
    propertyAddress: property.address,
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

/** Mounts the row inside a table body, since a table row is only valid markup there. */
function mountRow(summary: PropertyAppointmentSummary) {
  return mount(PropertyStatisticsRow, {
    props: { property, summary },
    attachTo: document.createElement('tbody'),
    global: { plugins: [i18n] },
  })
}

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('PropertyStatisticsRow', () => {
  it('renders the property with its open and completed appointment counts', () => {
    const wrapper = mountRow({ openCount: 3, completedCount: 12, nextAppointment: null })

    expect(wrapper.find('.property-statistics-row__name').text()).toBe('Wohnanlage Sonnenhof')
    expect(wrapper.find('.property-statistics-row__address').text()).toBe(
      'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    )
    expect(wrapper.find('.property-statistics-row__open').text()).toBe('3')
    expect(wrapper.find('.property-statistics-row__completed').text()).toBe('12')
  })

  it('shows a placeholder when there is no next appointment', () => {
    const wrapper = mountRow({ openCount: 0, completedCount: 2, nextAppointment: null })

    expect(wrapper.find('.property-statistics-row__next-link').exists()).toBe(false)
    expect(wrapper.find('.property-statistics-row__no-next').text()).toBe('Kein anstehender Auftrag')
  })

  it('shows the next appointment as a link with its title and date and emits its id when clicked', async () => {
    const wrapper = mountRow({
      openCount: 1,
      completedCount: 0,
      nextAppointment: createAppointment({ id: '42', title: 'Heizungswartung' }),
    })

    const link = wrapper.find('.property-statistics-row__next-link')
    expect(link.find('.property-statistics-row__next-title').text()).toBe('Heizungswartung')
    expect(link.find('.property-statistics-row__next-date').text()).toBe('20.08.2026, 09:00')

    await link.trigger('click')

    expect(wrapper.emitted('open-appointment')).toEqual([['42']])
  })
})
