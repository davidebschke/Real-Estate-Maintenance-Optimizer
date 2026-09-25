import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import CalendarEventCard from '@/components/calendar/CalendarEventCard.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('CalendarEventCard', () => {
  it('renders the title, property name and a 24h German time range', () => {
    const wrapper = mount(CalendarEventCard, {
      props: {
        appointmentId: 'a1',
        title: 'Heizungswartung',
        propertyName: 'Sonnenhof',
        start: new Date(2026, 7, 10, 8, 30),
        end: new Date(2026, 7, 10, 10, 0),
      },
      global: { plugins: [i18n] },
    })

    expect(wrapper.find('.calendar-event-card__title').text()).toBe('Heizungswartung')
    expect(wrapper.find('.calendar-event-card__meta').text()).toBe('Sonnenhof · 08:30–10:00')
  })

  it('exposes the appointment id as a data attribute for the drag-and-drop composable to find', () => {
    const wrapper = mount(CalendarEventCard, {
      props: {
        appointmentId: 'a1',
        title: 'Heizungswartung',
        propertyName: 'Sonnenhof',
        start: new Date(2026, 7, 10, 8, 30),
        end: new Date(2026, 7, 10, 10, 0),
      },
      global: { plugins: [i18n] },
    })

    expect(wrapper.find('.calendar-event-card').attributes('data-appointment-id')).toBe('a1')
  })

  it('formats the time range for the English locale', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(CalendarEventCard, {
      props: {
        appointmentId: 'a1',
        title: 'Boiler service',
        propertyName: 'Sonnenhof',
        start: new Date(2026, 7, 10, 8, 30),
        end: new Date(2026, 7, 10, 10, 0),
      },
      global: { plugins: [i18n] },
    })

    expect(wrapper.find('.calendar-event-card__meta').text()).toContain('Sonnenhof')
  })
})
