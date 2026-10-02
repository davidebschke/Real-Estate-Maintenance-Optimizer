import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import { AxiosError, AxiosHeaders } from 'axios'
import { i18n } from '@/i18n'
import DemoAccountPanel from '@/components/auth/DemoAccountPanel.vue'
import { useAuthStore } from '@/stores/auth'
import * as authService from '@/services/authService'

vi.mock('@/services/authService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/services/authService')>()
  return { ...actual, createDemoAccount: vi.fn<typeof actual.createDemoAccount>() }
})

/** Builds the axios error a request rejects with for the given HTTP status. */
function httpError(status: number): AxiosError {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('failed', 'ERR_BAD_REQUEST', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data: {},
  })
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(authService.createDemoAccount).mockReset()
  i18n.global.locale.value = 'de'
})

/** Mounts the demo account panel with the plugins it needs. */
function mountPanel() {
  return mount(DemoAccountPanel, { global: { plugins: [i18n, PrimeVue] } })
}

describe('DemoAccountPanel', () => {
  it('tells the user up front what the demo account contains, allows and when it is deleted', () => {
    const text = mountPanel().text()

    expect(text).toContain('5 Beispiel-Objekte')
    expect(text).toContain('30 Termine in den nächsten 3 Wochen')
    expect(text).toContain('bis zu 3 Objekte und 3 Termine')
    expect(text).toContain('Beim Abmelden')
    expect(text).toContain('8 Stunden')
  })

  it('creates the demo account, turns green and reports the success', async () => {
    vi.mocked(authService.createDemoAccount).mockResolvedValue({
      username: 'demo-1',
      displayName: 'Demo',
      demoAccount: true,
      expiresAt: new Date('2026-09-29T16:00:00Z'),
      remainingPropertyCreations: 3,
      remainingAppointmentCreations: 3,
      appointmentBufferMinutes: 15,
    })
    const wrapper = mountPanel()

    await wrapper.findComponent(Button).trigger('click')
    await flushPromises()

    expect(useAuthStore().isDemoAccount).toBe(true)
    expect(wrapper.findComponent(Button).classes()).toContain('demo-account-panel__submit--success')
    expect(wrapper.findComponent(Button).text()).toBe('Demo-Account bereit')
    expect(wrapper.emitted('authenticated')).toHaveLength(1)
  })

  it('explains that no more demo accounts can be created when rate-limited', async () => {
    vi.mocked(authService.createDemoAccount).mockRejectedValue(httpError(429))
    const wrapper = mountPanel()

    await wrapper.findComponent(Button).trigger('click')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('keine weiteren Demo-Accounts')
    expect(wrapper.findComponent(Button).attributes('disabled')).toBeUndefined()
    expect(wrapper.emitted('authenticated')).toBeUndefined()
  })

  it('shows a generic error for any other failure', async () => {
    vi.mocked(authService.createDemoAccount).mockRejectedValue(httpError(500))
    const wrapper = mountPanel()

    await wrapper.findComponent(Button).trigger('click')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('konnte nicht erstellt werden')
  })
})
