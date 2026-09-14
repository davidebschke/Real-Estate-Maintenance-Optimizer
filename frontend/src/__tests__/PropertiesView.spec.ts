import { afterEach, describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import PropertiesView from '@/views/PropertiesView.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('PropertiesView', () => {
  it('renders the localized properties page text', () => {
    const wrapper = mount(PropertiesView, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toBe('Hier ist die Immobilienseite')
  })

  it('renders the English text when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(PropertiesView, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toBe('Here is the properties page')
  })
})
