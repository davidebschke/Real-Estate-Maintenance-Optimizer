import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { ref } from 'vue'
import { i18n } from '@/i18n'
import DailyAppointmentList from '@/components/overview/DailyAppointmentList.vue'
import OptimizationBanner from '@/components/optimization/OptimizationBanner.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import * as appointmentService from '@/services/appointmentService'
import { useAppointmentMapMarkers } from '@/composables/useAppointmentMapMarkers'
import { useAppointmentRoute } from '@/composables/useAppointmentRoute'
import type { RouteLeg } from '@/types/route'
import type { Appointment } from '@/types/appointment'

vi.mock('@/services/appointmentService')
vi.mock('@/composables/useAppointmentMapMarkers')
vi.mock('@/composables/useAppointmentRoute')

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
    actualEnd: null,
    completed: false,
    ...overrides,
  }
}

/** Makes the route composable report the given legs by appointment id. */
function mockRouteLegs(legsByAppointmentId: Map<string, RouteLeg>) {
  vi.mocked(useAppointmentRoute).mockReturnValue({
    legsByAppointmentId: ref(legsByAppointmentId),
  } as never)
}

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
  vi.useFakeTimers()
  vi.setSystemTime(new Date(2026, 7, 10, 9, 0))
  vi.mocked(appointmentService.fetchAppointments).mockReset().mockResolvedValue([])
  vi.mocked(useAppointmentMapMarkers).mockReturnValue({ markers: ref([]) } as never)
  mockRouteLegs(new Map())
})

afterEach(() => {
  vi.useRealTimers()
})

describe('DailyAppointmentList', () => {
  it('shows an empty-state message when there are no appointments today', async () => {
    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()

    expect(wrapper.find('.daily-appointment-list__empty').text()).toBe(
      'Heute stehen keine weiteren Termine an.',
    )
  })

  it('shows the travel leg to each appointment next to its duration', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '1', title: 'Erster' }),
      createAppointment({
        id: '2',
        title: 'Zweiter',
        start: new Date(2026, 7, 10, 11, 0),
        end: new Date(2026, 7, 10, 12, 0),
      }),
    ])
    mockRouteLegs(new Map([['2', { distanceMeters: 6400, durationSeconds: 721 }]]))

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()

    const cards = wrapper.findAll('.daily-appointment-card')
    expect(cards[0]?.find('.daily-appointment-card__travel').exists()).toBe(false)
    expect(cards[1]?.find('.daily-appointment-card__travel').text()).toBe('6,4 km · 13 Min Anfahrt')
  })

  it("offers a Google Maps route link under each of today's appointments but not under tomorrow's preview", async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '1', title: 'Heute' }),
    ])
    const todayWrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()

    expect(todayWrapper.findAll('.daily-appointment-card__route-link')).toHaveLength(1)

    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '1', title: 'Erledigt', completed: true }),
      createAppointment({
        id: '2',
        title: 'Morgen',
        start: new Date(2026, 7, 11, 9, 0),
        end: new Date(2026, 7, 11, 10, 0),
      }),
    ])
    const tomorrowWrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()

    expect(tomorrowWrapper.findAll('.daily-appointment-card')).toHaveLength(1)
    expect(tomorrowWrapper.find('.daily-appointment-card__route-link').exists()).toBe(false)
  })

  it('numbers the open appointments like their markers on the map, counting completed ones too', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '1', title: 'Erledigt', completed: true }),
      createAppointment({
        id: '2',
        title: 'Offen',
        start: new Date(2026, 7, 10, 11, 0),
        end: new Date(2026, 7, 10, 12, 0),
      }),
    ])

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()

    expect(wrapper.findAll('.daily-appointment-card__position').map((node) => node.text())).toEqual(
      ['2'],
    )
  })

  it('shows the "all completed" preview of tomorrow once every appointment today is completed', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '1', title: 'Erledigt', completed: true }),
    ])

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()

    expect(wrapper.findAll('.daily-appointment-card')).toHaveLength(0)
    expect(wrapper.find('.daily-appointment-list__date').text()).toBe(
      'Termine für morgen, den 11. August',
    )
    expect(wrapper.find('.daily-appointment-list__count').text()).toBe(
      'Alle Termine heute erledigt · Vorschau auf morgen',
    )
    expect(wrapper.find('.daily-appointment-list__empty').text()).toBe(
      'Für morgen stehen noch keine Termine an.',
    )
  })

  it("lists tomorrow's open appointments once every appointment today is completed", async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '1', title: 'Erledigt', completed: true }),
      createAppointment({
        id: '2',
        title: 'Morgiger Termin',
        completed: false,
        start: new Date(2026, 7, 11, 9, 0),
        end: new Date(2026, 7, 11, 10, 0),
      }),
    ])

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()

    const titles = wrapper.findAll('.daily-appointment-card__title').map((node) => node.text())
    expect(titles).toEqual(['Morgiger Termin'])
    expect(wrapper.find('.daily-appointment-list__empty').exists()).toBe(false)
  })

  it('excludes completed appointments from the list and the count while keeping open ones', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({
        id: '1',
        title: 'Erledigt',
        completed: true,
        start: new Date(2026, 7, 10, 8, 0),
        end: new Date(2026, 7, 10, 9, 0),
      }),
      createAppointment({
        id: '2',
        title: 'Offen',
        completed: false,
        start: new Date(2026, 7, 10, 10, 0),
        end: new Date(2026, 7, 10, 11, 0),
      }),
    ])

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()

    const titles = wrapper.findAll('.daily-appointment-card__title').map((node) => node.text())
    expect(titles).toEqual(['Offen'])
    expect(wrapper.find('.daily-appointment-list__count').text()).toContain('1 Termin')
  })

  it("renders today's appointments as numbered cards, ordered from first to last", async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({
        id: '1',
        title: 'Später dran',
        start: new Date(2026, 7, 10, 15, 0),
        end: new Date(2026, 7, 10, 16, 0),
      }),
      createAppointment({
        id: '2',
        title: 'Zuerst dran',
        start: new Date(2026, 7, 10, 8, 30),
        end: new Date(2026, 7, 10, 9, 30),
      }),
      createAppointment({
        id: '3',
        title: 'Anderer Tag',
        start: new Date(2026, 7, 11, 8, 0),
        end: new Date(2026, 7, 11, 9, 0),
      }),
    ])

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()

    const titles = wrapper.findAll('.daily-appointment-card__title').map((node) => node.text())
    expect(titles).toEqual(['Zuerst dran', 'Später dran'])
    expect(wrapper.find('.daily-appointment-list__count').text()).toContain('2 Termine')
  })

  it('opens the appointment detail view when a card is clicked', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ id: '10' }),
    ])

    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()
    const appointmentsStore = useAppointmentsStore()

    await wrapper.find('.daily-appointment-card').trigger('click')

    expect(appointmentsStore.activeDetailAppointmentId).toBe('10')
  })

  it('ends with the AI optimization banner', async () => {
    const wrapper = mount(DailyAppointmentList, { global: { plugins: [i18n], stubs: { OptimizationBanner: true } } })
    await flushPromises()

    expect(wrapper.findComponent(OptimizationBanner).exists()).toBe(true)
  })
})
