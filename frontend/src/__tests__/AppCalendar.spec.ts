import { afterEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import VueCal from 'vue-cal'
import { i18n } from '@/i18n'
import AppCalendar from '@/components/calendar/AppCalendar.vue'
import { useAppointmentsStore } from '@/stores/appointments'
import * as appointmentService from '@/services/appointmentService'

vi.mock('@/services/appointmentService')

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
