import { describe, it, expect } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { i18n } from '@/i18n'
import { routes } from '@/router'
import AppHeader from '@/components/layout/AppHeader.vue'
import NavigationMenu from '@/components/layout/NavigationMenu.vue'
import LanguageSwitch from '@/components/layout/LanguageSwitch.vue'
import UserAccountDropdown from '@/components/layout/UserAccountDropdown.vue'
import AppointmentButton from '@/components/layout/AppointmentButton.vue'

function createTestRouter() {
  return createRouter({ history: createMemoryHistory(), routes })
}

describe('AppHeader', () => {
  it('renders the brand name and composes every header section', async () => {
    const router = createTestRouter()
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [i18n, router],
        stubs: { NavigationMenu: true, LanguageSwitch: true, UserAccountDropdown: true, AppointmentButton: true },
      },
    })
    await router.isReady()

    expect(wrapper.text()).toContain('Remo')
    expect(wrapper.findComponent(NavigationMenu).exists()).toBe(true)
    expect(wrapper.findComponent(LanguageSwitch).exists()).toBe(true)
    expect(wrapper.findComponent(UserAccountDropdown).exists()).toBe(true)
    expect(wrapper.findComponent(AppointmentButton).exists()).toBe(true)
  })

  it('navigates to the overview page when the brand is clicked', async () => {
    const router = createTestRouter()
    await router.push({ name: 'calendar' })
    const wrapper = mount(AppHeader, {
      global: {
        plugins: [i18n, router],
        stubs: { LanguageSwitch: true, UserAccountDropdown: true, AppointmentButton: true },
      },
    })
    await router.isReady()

    await wrapper.find('.app-header__brand').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('overview')
  })
})
