import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import CalendarView from '@/views/CalendarView.vue'
import AppCalendar from '@/components/calendar/AppCalendar.vue'

describe('CalendarView', () => {
  it('renders the calendar', () => {
    const wrapper = mount(CalendarView, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.findComponent(AppCalendar).exists()).toBe(true)
  })
})
