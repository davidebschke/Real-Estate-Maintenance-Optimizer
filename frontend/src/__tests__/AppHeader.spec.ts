import { afterEach, describe, it, expect } from 'vitest'
import { mount, shallowMount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import { useNavigation } from '@/composables/useNavigation'
import AppHeader from '@/components/layout/AppHeader.vue'
import NavigationMenu from '@/components/layout/NavigationMenu.vue'
import LanguageSwitch from '@/components/layout/LanguageSwitch.vue'
import UserAccountDropdown from '@/components/layout/UserAccountDropdown.vue'
import AppointmentButton from '@/components/layout/AppointmentButton.vue'
import ComingSoonDialog from '@/components/layout/ComingSoonDialog.vue'

afterEach(() => {
  useNavigation().closePopup()
})

describe('AppHeader', () => {
  it('renders the brand name and composes every header section', () => {
    // PrimeVue must be installed here too: otherwise resolving its components without the
    // plugin once in this file leaves Dialog unable to find $primevue in the next test below.
    const wrapper = shallowMount(AppHeader, {
      global: { plugins: [i18n, PrimeVue] },
    })

    expect(wrapper.text()).toContain('Remo')
    expect(wrapper.findComponent(NavigationMenu).exists()).toBe(true)
    expect(wrapper.findComponent(LanguageSwitch).exists()).toBe(true)
    expect(wrapper.findComponent(UserAccountDropdown).exists()).toBe(true)
    expect(wrapper.findComponent(AppointmentButton).exists()).toBe(true)
    expect(wrapper.findComponent(ComingSoonDialog).exists()).toBe(true)
  })

  it('opens the coming soon dialog when a navigation entry is clicked', async () => {
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [i18n, PrimeVue],
        stubs: { LanguageSwitch: true, UserAccountDropdown: true, AppointmentButton: true },
      },
      attachTo: document.body,
    })

    await wrapper.find('.navigation-menu__link').trigger('click')

    expect(document.body.textContent).toContain('Bald verfügbar')

    wrapper.unmount()
  })
})
