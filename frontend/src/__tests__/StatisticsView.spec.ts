import { afterEach, describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import StatisticsView from '@/views/StatisticsView.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('StatisticsView', () => {
  it('renders the localized statistics page text', () => {
    const wrapper = mount(StatisticsView, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toBe('Hier ist die Statistikseite')
  })

  it('renders the English text when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(StatisticsView, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toBe('Here is the statistics page')
  })
})
