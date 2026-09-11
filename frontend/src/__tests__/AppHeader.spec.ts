import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import AppHeader from '@/components/layout/AppHeader.vue'

describe('AppHeader', () => {
  it('renders the brand name and all navigation entries', () => {
    const wrapper = mount(AppHeader, {
      global: { plugins: [i18n, PrimeVue] },
    })

    expect(wrapper.text()).toContain('Remo')
    expect(wrapper.text()).toContain('Übersicht')
    expect(wrapper.text()).toContain('Kalender')
    expect(wrapper.text()).toContain('Statistik')
    expect(wrapper.text()).toContain('Immobilien')
  })

  it('opens the coming soon dialog when a navigation entry is clicked', async () => {
    const wrapper = mount(AppHeader, {
      global: { plugins: [i18n, PrimeVue] },
      attachTo: document.body,
    })

    await wrapper.find('.navigation-menu__link').trigger('click')

    expect(document.body.textContent).toContain('Bald verfügbar')
  })
})
