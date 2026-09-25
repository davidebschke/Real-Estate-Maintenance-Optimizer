import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { i18n } from '@/i18n'
import CalendarView from '@/views/CalendarView.vue'
import AppCalendar from '@/components/calendar/AppCalendar.vue'
import * as appointmentService from '@/services/appointmentService'

vi.mock('@/services/appointmentService')
vi.mock('primevue/usetoast')
vi.mocked(appointmentService.fetchAppointments).mockResolvedValue([])

describe('CalendarView', () => {
  it('renders the calendar', () => {
    const wrapper = mount(CalendarView, {
      global: { plugins: [i18n, createPinia()] },
    })

    expect(wrapper.findComponent(AppCalendar).exists()).toBe(true)
  })
})
