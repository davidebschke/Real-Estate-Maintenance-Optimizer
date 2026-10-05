import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { i18n } from '@/i18n'
import CalendarListView from '@/components/calendar/CalendarListView.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import type { Appointment } from '@/types/appointment'

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  return {
    id: 'a1',
    seriesId: null,
    title: 'Heizungswartung',
    propertyId: 'property-1',
    propertyName: 'Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: '',
    category: 'maintenance',
    start: new Date(2026, 7, 11, 9, 0),
    end: new Date(2026, 7, 11, 10, 0),
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

function mountView(appointments: Appointment[], visibleStart: Date, visibleEnd: Date) {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(CalendarListView, {
    props: { appointments, visibleStart, visibleEnd },
    global: { plugins: [i18n, pinia] },
  })
}

describe('CalendarListView', () => {
  it('shows an empty hint when no appointments fall into the visible range', () => {
    const wrapper = mountView([], new Date(2026, 7, 10), new Date(2026, 7, 14))

    expect(wrapper.find('.calendar-list-view__empty').text()).toBe(
      'Keine Termine im angezeigten Zeitraum.',
    )
  })

  it('groups appointments by day within the visible range, sorted by start time', () => {
    const appointments = [
      createAppointment({
        id: 'late',
        title: 'Spätdienst',
        start: new Date(2026, 7, 11, 15, 0),
        end: new Date(2026, 7, 11, 16, 0),
      }),
      createAppointment({
        id: 'early',
        title: 'Frühdienst',
        start: new Date(2026, 7, 11, 8, 0),
        end: new Date(2026, 7, 11, 9, 0),
      }),
      createAppointment({
        id: 'other-day',
        title: 'Anderer Tag',
        start: new Date(2026, 7, 13, 9, 0),
        end: new Date(2026, 7, 13, 10, 0),
      }),
    ]

    const wrapper = mountView(appointments, new Date(2026, 7, 10), new Date(2026, 7, 14))

    const days = wrapper.findAll('.calendar-list-view__day')
    expect(days).toHaveLength(2)

    const firstDayTitles = days[0]!.findAll('.calendar-list-view__title').map((node) => node.text())
    expect(firstDayTitles).toEqual(['Frühdienst', 'Spätdienst'])
  })

  it('excludes appointments outside the visible range', () => {
    const appointments = [
      createAppointment({ id: 'outside', start: new Date(2026, 7, 1, 9, 0), end: new Date(2026, 7, 1, 10, 0) }),
    ]

    const wrapper = mountView(appointments, new Date(2026, 7, 10), new Date(2026, 7, 14))

    expect(wrapper.find('.calendar-list-view__empty').exists()).toBe(true)
  })

  it('opens the appointment detail view when an item is clicked', async () => {
    const appointments = [createAppointment({ id: 'a1' })]
    const wrapper = mountView(appointments, new Date(2026, 7, 10), new Date(2026, 7, 14))

    await wrapper.find('.calendar-list-view__item').trigger('click')

    expect(useAppointmentsStore().activeDetailAppointmentId).toBe('a1')
  })
})
