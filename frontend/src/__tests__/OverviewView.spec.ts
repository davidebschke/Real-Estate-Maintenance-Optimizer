import { afterEach, describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { i18n } from '@/i18n'
import OverviewView from '@/views/OverviewView.vue'

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('OverviewView', () => {
  it('renders the localized overview page text', () => {
    const wrapper = mount(OverviewView, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toBe('Hier ist die Übersichtsseite')
  })

  it('renders the English text when the locale is switched', () => {
    i18n.global.locale.value = 'en'
    const wrapper = mount(OverviewView, {
      global: { plugins: [i18n] },
    })

    expect(wrapper.text()).toBe('Here is the overview page')
  })
})
