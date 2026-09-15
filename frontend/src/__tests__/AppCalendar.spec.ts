import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { i18n } from '@/i18n'
import AppCalendar from '@/components/calendar/AppCalendar.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('AppCalendar', () => {
  it('renders the toolbar defaulting to the week view', () => {
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
})
