import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { i18n } from '@/i18n'
import DailyAppointmentList from '@/components/overview/DailyAppointmentList.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import * as appointmentService from '@/services/appointmentService'
import type { Appointment } from '@/types/appointment'

vi.mock('@/services/appointmentService')

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
  i18n.global.locale.value = 'de'
  vi.useFakeTimers()
  vi.setSystemTime(new Date(2026, 7, 10, 9, 0))
  vi.mocked(appointmentService.fetchAppointments).mockReset().mockResolvedValue([])
})

afterEach(() => {
  vi.useRealTimers()
})

describe('DailyAppointmentList', () => {
  it('shows an empty-state message when there are no appointments today', async () => {
    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n] } })
    await flushPromises()

    expect(wrapper.find('.daily-appointment-list__empty').text()).toBe(
      'Heute stehen keine weiteren Termine an.',
    )
  })

  it('hides completed appointments and shows the empty state once none are left', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '1', title: 'Erledigt', completed: true }),
    ])

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n] } })
    await flushPromises()

    expect(wrapper.findAll('.daily-appointment-card')).toHaveLength(0)
    expect(wrapper.find('.daily-appointment-list__empty').text()).toBe(
      'Heute stehen keine weiteren Termine an.',
    )
  })

  it('excludes completed appointments from the list and the count while keeping open ones', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '1', title: 'Erledigt', completed: true, start: new Date(2026, 7, 10, 8, 0), end: new Date(2026, 7, 10, 9, 0) }),
      createAppointment({ id: '2', title: 'Offen', completed: false, start: new Date(2026, 7, 10, 10, 0), end: new Date(2026, 7, 10, 11, 0) }),
    ])

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n] } })
    await flushPromises()

    const titles = wrapper.findAll('.daily-appointment-card__title').map((node) => node.text())
    expect(titles).toEqual(['Offen'])
    expect(wrapper.find('.daily-appointment-list__count').text()).toContain('1 Termin')
  })

  it('renders today\'s appointments as numbered cards, ordered from first to last', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '1', title: 'Später dran', start: new Date(2026, 7, 10, 15, 0), end: new Date(2026, 7, 10, 16, 0) }),
      createAppointment({ id: '2', title: 'Zuerst dran', start: new Date(2026, 7, 10, 8, 30), end: new Date(2026, 7, 10, 9, 30) }),
      createAppointment({ id: '3', title: 'Anderer Tag', start: new Date(2026, 7, 11, 8, 0), end: new Date(2026, 7, 11, 9, 0) }),
    ])

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n] } })
    await flushPromises()

    const titles = wrapper.findAll('.daily-appointment-card__title').map((node) => node.text())
    expect(titles).toEqual(['Zuerst dran', 'Später dran'])
    expect(wrapper.find('.daily-appointment-list__count').text()).toContain('2 Termine')
  })

  it('opens the appointment detail view when a card is clicked', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '10' }),
    ])

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n] } })
    await flushPromises()
    const appointmentsStore = useAppointmentsStore()

    await wrapper.find('.daily-appointment-card').trigger('click')

    expect(appointmentsStore.activeDetailAppointmentId).toBe('10')
  })
})
