import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import VueCal from 'vue-cal'
import { useToast } from 'primevue/usetoast'
import { i18n } from '@/i18n'
import AppCalendar from '@/components/calendar/AppCalendar.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import * as appointmentService from '@/services/appointmentService'
import type { Appointment } from '@/types/appointment'

vi.mock('@/services/appointmentService')
vi.mock('primevue/usetoast')

/** Builds a bubbling mouse event carrying the pointer-event fields the drag composable reads, sidestepping jsdom's limited PointerEvent support. */
function pointerEvent(type: string, position: { x: number; y: number }): PointerEvent {
  const event = new MouseEvent(type, {
    clientX: position.x,
    clientY: position.y,
    button: 0,
    bubbles: true,
    cancelable: true,
  })
  return Object.assign(event, { pointerId: 1, pointerType: 'mouse' }) as unknown as PointerEvent
}

/** Builds a sample appointment for tests, with overridable fields. */
function createAppointment(overrides: Partial<Appointment> = {}): Appointment {
  const today = new Date()
  return {
    id: 'a1',
    seriesId: null,
    title: 'Heizungswartung',
    propertyId: 'property-1',
    propertyName: 'Sonnenhof',
    propertyAddress: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    description: '',
    category: 'maintenance',
    start: today,
    end: new Date(today.getTime() + 60 * 60 * 1000),
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
  vi.mocked(appointmentService.fetchAppointments).mockReset()
})

describe('AppCalendar', () => {
  it('renders the toolbar defaulting to the week view', () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
    const wrapper = mount(AppCalendar, {
      global: { plugins: [i18n, createPinia()] },
    })

    expect(wrapper.find('.calendar-toolbar').exists()).toBe(true)
    const activeButton = wrapper
      .findAll('.calendar-toolbar__view-button')
      .find((button) => button.classes('calendar-toolbar__view-button--active'))
    expect(activeButton?.text()).toBe('Woche')
  })

  it('switches the active view when a different view button is clicked', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
    const wrapper = mount(AppCalendar, {
      global: { plugins: [i18n, createPinia()] },
    })

    const dayButton = wrapper
      .findAll('.calendar-toolbar__view-button')
      .find((button) => button.text() === 'Tag')
    await dayButton?.trigger('click')

    const activeButton = wrapper
      .findAll('.calendar-toolbar__view-button')
      .find((button) => button.classes('calendar-toolbar__view-button--active'))
    expect(activeButton?.text()).toBe('Tag')
  })

  it('fetches appointments once mounted', () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
    mount(AppCalendar, {
      global: { plugins: [i18n, createPinia()] },
    })

    expect(appointmentService.fetchAppointments).toHaveBeenCalled()
  })

  it("overrides vue-cal's default empty-state label with the app's own appointment terminology", () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
    const wrapper = mount(AppCalendar, {
      global: { plugins: [i18n, createPinia()] },
    })

    expect(wrapper.findComponent(VueCal).props('locale')).toMatchObject({
      noEvent: 'Keine Termine',
    })
  })

  it('hides Sundays from the calendar grid', () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
    const wrapper = mount(AppCalendar, {
      global: { plugins: [i18n, createPinia()] },
    })

    expect(wrapper.findComponent(VueCal).props('hideWeekdays')).toEqual([7])
  })

  it('exposes each rendered appointment card as a drag-and-drop source via its appointment id', async () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([createAppointment()])
    const wrapper = mount(AppCalendar, {
      global: { plugins: [i18n, createPinia()] },
    })
    await flushPromises()

    expect(wrapper.find('[data-appointment-id="a1"]').exists()).toBe(true)
  })

  it('shows a toast when the user drags a locked appointment', async () => {
    const add = vi.fn<(message: Record<string, unknown>) => void>()
    vi.mocked(useToast).mockReturnValue({ add } as never)
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([
      createAppointment({ locked: true }),
    ])
    const wrapper = mount(AppCalendar, {
      global: { plugins: [i18n, createPinia()] },
    })
    await flushPromises()

    const card = wrapper.get('[data-appointment-id="a1"]').element
    card.dispatchEvent(pointerEvent('pointerdown', { x: 0, y: 0 }))
    window.dispatchEvent(pointerEvent('pointermove', { x: 0, y: 50 }))

    expect(add).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'warn',
        detail: 'Dieser Termin ist unverschiebbar und kann nicht verlegt werden.',
      }),
    )
  })

  it('opens the detail view for the clicked appointment', () => {
    vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])
    const pinia = createPinia()
    setActivePinia(pinia)
    const wrapper = mount(AppCalendar, {
      global: { plugins: [i18n, pinia] },
    })

    const onEventClick = wrapper.findComponent(VueCal).props('onEventClick') as (event: {
      appointmentId: string
    }) => void
    onEventClick({ appointmentId: '42' })

    expect(useAppointmentsStore().activeDetailAppointmentId).toBe('42')
  })
})
