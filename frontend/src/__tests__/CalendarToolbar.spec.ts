import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import CalendarToolbar from '@/components/calendar/CalendarToolbar.vue'

describe('CalendarToolbar', () => {
  it('renders the given range label', () => {
    const wrapper = mount(CalendarToolbar, {
      props: { rangeLabel: '10. – 14. August 2026', activeView: 'week' },
      global: { plugins: [i18n] },
    })

    expect(wrapper.find('.calendar-toolbar__title').text()).toBe('10. – 14. August 2026')
  })

  it('emits previous/next/today when the corresponding buttons are clicked', async () => {
    const wrapper = mount(CalendarToolbar, {
      props: { rangeLabel: '10. – 14. August 2026', activeView: 'week' },
      global: { plugins: [i18n] },
    })

    const [previousButton, nextButton] = wrapper.findAll('.calendar-toolbar__nav-button')
    await previousButton?.trigger('click')
    await nextButton?.trigger('click')
    await wrapper.find('.calendar-toolbar__today-button').trigger('click')

    expect(wrapper.emitted('previous')).toHaveLength(1)
    expect(wrapper.emitted('next')).toHaveLength(1)
    expect(wrapper.emitted('today')).toHaveLength(1)
  })

  it('marks the active view button and emits an updated model when another view is clicked', async () => {
    const wrapper = mount(CalendarToolbar, {
      props: { rangeLabel: '10. – 14. August 2026', activeView: 'week' },
      global: { plugins: [i18n] },
    })

    const viewButtons = wrapper.findAll('.calendar-toolbar__view-button')
    const activeButton = viewButtons.find((button) =>
      button.classes('calendar-toolbar__view-button--active'),
    )
    expect(activeButton?.text()).toBe('Woche')

    const monthButton = viewButtons.find((button) => button.text() === 'Monat')
    await monthButton?.trigger('click')

    expect(wrapper.emitted('update:activeView')).toEqual([['month']])
  })
})
