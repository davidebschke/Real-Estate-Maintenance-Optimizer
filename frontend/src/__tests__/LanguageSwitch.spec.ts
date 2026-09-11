import { afterEach, describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import LanguageSwitch from '@/components/layout/LanguageSwitch.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('LanguageSwitch', () => {
  it('marks the current locale as active and the other as inactive', () => {
    const wrapper = mount(LanguageSwitch, {
      global: { plugins: [i18n] },
    })
    const [deOption, enOption] = wrapper.findAll('.language-switch__option')

    expect(deOption!.attributes('aria-pressed')).toBe('true')
    expect(enOption!.attributes('aria-pressed')).toBe('false')
  })

  it('switches the active locale when a different option is clicked', async () => {
    const wrapper = mount(LanguageSwitch, {
      global: { plugins: [i18n] },
    })
    const [, enOption] = wrapper.findAll('.language-switch__option')

    await enOption!.trigger('click')

    expect(i18n.global.locale.value).toBe('en')
    expect(enOption!.attributes('aria-pressed')).toBe('true')
  })
})
