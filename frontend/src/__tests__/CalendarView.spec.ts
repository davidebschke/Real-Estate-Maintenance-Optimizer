import { afterEach, describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import CalendarView from '@/views/CalendarView.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('CalendarView', () => {
  it('renders the localized calendar page text', () => {
    const wrapper = mount(CalendarView, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toBe('Hier ist die Kalenderseite')
  })

  it('renders the English text when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(CalendarView, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toBe('Here is the calendar page')
  })
})
