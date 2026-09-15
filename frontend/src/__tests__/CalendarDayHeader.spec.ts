import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import CalendarDayHeader from '@/components/calendar/CalendarDayHeader.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('CalendarDayHeader', () => {
  it('renders the weekday label and day number without a summary line when none is given', () => {
    const wrapper = mount(CalendarDayHeader, {
      props: { date: new Date(2026, 7, 10), label: 'Mo', summary: null },
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toContain('Mo 10.')
    expect(wrapper.find('.calendar-day-header__summary').exists()).toBe(false)
  })

  it('renders the plural appointment count and travel distance in German', () => {
    const wrapper = mount(CalendarDayHeader, {
      props: {
        date: new Date(2026, 7, 10),
        label: 'Mo',
        summary: { appointmentCount: 4, travelDistanceKm: 38 },
      },
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toContain('4 Termine · 38 km')
  })

  it('renders the singular appointment count in German', () => {
    const wrapper = mount(CalendarDayHeader, {
      props: {
        date: new Date(2026, 7, 14),
        label: 'Fr',
        summary: { appointmentCount: 1, travelDistanceKm: 18 },
      },
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toContain('1 Termin · 18 km')
  })

  it('renders the summary in English, without a trailing period after the day number, when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(CalendarDayHeader, {
      props: {
        date: new Date(2026, 7, 10),
        label: 'Mon',
        summary: { appointmentCount: 2, travelDistanceKm: 21 },
      },
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toContain('Mon 10')
    expect(wrapper.text()).not.toContain('Mon 10.')
    expect(wrapper.text()).toContain('2 appointments · 21 km')
  })
})
