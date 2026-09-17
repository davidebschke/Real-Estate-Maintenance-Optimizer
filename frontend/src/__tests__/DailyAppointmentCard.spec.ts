import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import DailyAppointmentCard from '@/components/overview/DailyAppointmentCard.vue'
import type { Appointment } from '@/types/appointment'

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
    materials: ['Filterpatrone 2x', 'Dichtungsset'],
    history: [],
    travelDistanceKm: 6.4,
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('DailyAppointmentCard', () => {
  it('renders the position, time, title, property, materials and travel summary', () => {
    const wrapper = mount(DailyAppointmentCard, {
      props: { appointment: createAppointment(), position: 1 },
      global: { plugins: [i18n] },
    })

    expect(wrapper.find('.daily-appointment-card__position').text()).toBe('1')
    expect(wrapper.find('.daily-appointment-card__time').text()).toBe('08:30')
    expect(wrapper.find('.daily-appointment-card__title').text()).toBe('Heizungswartung')
    expect(wrapper.find('.daily-appointment-card__property').text()).toBe(
      'Wohnanlage Sonnenhof · Aachener Str. 512, 50933 Köln-Braunsenfeld',
    )
    expect(wrapper.find('.daily-appointment-card__materials').text()).toBe(
      'Filterpatrone 2x, Dichtungsset',
    )
    expect(wrapper.find('.daily-appointment-card__travel').text()).toBe('6.4 km · 1 Std 30 Min')
  })

  it('does not render a materials tag when the appointment has no materials', () => {
    const wrapper = mount(DailyAppointmentCard, {
      props: { appointment: createAppointment({ materials: [] }), position: 1 },
      global: { plugins: [i18n] },
    })

    expect(wrapper.find('.daily-appointment-card__materials').exists()).toBe(false)
  })

  it('marks a completed appointment with the completed modifier class', () => {
    const wrapper = mount(DailyAppointmentCard, {
      props: { appointment: createAppointment({ completed: true }), position: 1 },
      global: { plugins: [i18n] },
    })

    expect(wrapper.classes()).toContain('daily-appointment-card--completed')
  })

  it('emits the appointment id when clicked', async () => {
    const wrapper = mount(DailyAppointmentCard, {
      props: { appointment: createAppointment({ id: '42' }), position: 1 },
      global: { plugins: [i18n] },
    })

    await wrapper.trigger('click')

    expect(wrapper.emitted('open-appointment')).toEqual([['42']])
  })
})
