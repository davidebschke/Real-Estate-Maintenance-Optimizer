import { afterEach, beforeEach, describe, it, expect, vi } from 'vitest'
import { DOMWrapper, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import { routes } from '@/router'
import UserAccountDropdown from '@/components/layout/UserAccountDropdown.vue'
import { useAuthStore } from '@/stores/auth'
import * as authService from '@/services/authService'
import type { CurrentUser } from '@/types/auth'

vi.mock('@/services/authService')

let wrapper: ReturnType<typeof mount> | undefined

/** Logs in the given account directly in the store. */
function logInAs(overrides: Partial<CurrentUser> = {}) {
  useAuthStore().currentUser = {
    username: 'debschke',
    displayName: 'David Ebschke',
    demoAccount: false,
    expiresAt: null,
    remainingPropertyCreations: null,
    remainingAppointmentCreations: null,
    remainingTenantCreations: null,
    appointmentBufferMinutes: 15,
    ...overrides,
  }
}

/** Mounts the dropdown with a router positioned on the overview page. */
async function mountDropdown() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/')
  wrapper = mount(UserAccountDropdown, {
    global: { plugins: [i18n, PrimeVue, router] },
    attachTo: document.body,
  })
  return { wrapper, router }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(authService.logout).mockReset().mockResolvedValue()
  i18n.global.locale.value = 'de'
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
})

describe('UserAccountDropdown', () => {
  it('shows the name and initials of the logged-in account on the trigger', async () => {
    logInAs()

    const { wrapper } = await mountDropdown()

    const trigger = wrapper.find('.user-account-dropdown__trigger')
    expect(trigger.text()).toContain('David Ebschke')
    expect(trigger.text()).toContain('DE')
    expect(trigger.text()).toContain('Angemeldet als debschke')
  })

  it('shows a demo account under its localized name together with the end of its session', async () => {
    logInAs({ displayName: 'Demo', demoAccount: true, expiresAt: new Date(2026, 8, 29, 16, 30) })

    const { wrapper } = await mountDropdown()

    expect(wrapper.find('.user-account-dropdown__trigger').text()).toContain('Demo-Account')
    expect(wrapper.find('.user-account-dropdown__trigger').text()).toContain('endet 16:30 Uhr')
  })

  it('no longer offers switching to example accounts', async () => {
    logInAs()
    const { wrapper } = await mountDropdown()

    await wrapper.find('.user-account-dropdown__trigger').trigger('click')

    expect(document.body.textContent).not.toContain('Marco Keller')
    expect(document.body.querySelector('.user-account-dropdown__account-option')).toBeNull()
    expect(document.body.textContent).toContain('Abmelden')
  })

  it('logs out and returns to the login screen', async () => {
    logInAs()
    const { wrapper, router } = await mountDropdown()
    await wrapper.find('.user-account-dropdown__trigger').trigger('click')

    await new DOMWrapper(document.body.querySelector('.user-account-dropdown__logout') as Element).trigger('click')
    await flushPromises()

    expect(authService.logout).toHaveBeenCalledOnce()
    expect(useAuthStore().isAuthenticated).toBe(false)
    expect(router.currentRoute.value.name).toBe('login')
  })

  it('still returns to the login screen when the logout request fails', async () => {
    vi.mocked(authService.logout).mockRejectedValue(new Error('offline'))
    logInAs()
    const { wrapper, router } = await mountDropdown()
    await wrapper.find('.user-account-dropdown__trigger').trigger('click')

    await new DOMWrapper(document.body.querySelector('.user-account-dropdown__logout') as Element).trigger('click')
    await flushPromises()

    expect(useAuthStore().isAuthenticated).toBe(false)
    expect(router.currentRoute.value.name).toBe('login')
  })

  it('opens the profile and settings dialog from the menu for a regular account', async () => {
    logInAs()
    const { wrapper } = await mountDropdown()
    await wrapper.find('.user-account-dropdown__trigger').trigger('click')

    await new DOMWrapper(document.body.querySelector('.user-account-dropdown__menu-item') as Element).trigger('click')
    await flushPromises()

    expect(document.body.querySelector('.profile-settings-dialog')).not.toBeNull()
    expect(document.body.textContent).toContain('Pufferzeit zwischen Terminen')
  })

  it('makes the profile and settings entry unreachable for a demo account', async () => {
    logInAs({ displayName: 'Demo', demoAccount: true, expiresAt: new Date(2026, 8, 29, 16, 30) })
    const { wrapper } = await mountDropdown()
    await wrapper.find('.user-account-dropdown__trigger').trigger('click')

    const menuItem = document.body.querySelector('.user-account-dropdown__menu-item') as HTMLButtonElement
    await new DOMWrapper(menuItem).trigger('click')
    await flushPromises()

    expect(menuItem.disabled).toBe(true)
    expect(menuItem.textContent).toContain('Im Demo-Account nicht verfügbar')
    expect(document.body.querySelector('.profile-settings-dialog')).toBeNull()
  })
})
