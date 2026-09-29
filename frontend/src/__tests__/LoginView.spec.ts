import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import { routes } from '@/router'
import LoginView from '@/views/LoginView.vue'
import AuthCard from '@/components/auth/AuthCard.vue'
import LanguageSwitch from '@/components/layout/LanguageSwitch.vue'

beforeEach(() => {
  setActivePinia(createPinia())
  vi.useFakeTimers()
  i18n.global.locale.value = 'de'
})

afterEach(() => {
  vi.useRealTimers()
})

/** Mounts the login view at the given location, with the auth card stubbed. */
async function mountViewAt(location: string) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(location)
  const wrapper = mount(LoginView, {
    global: { plugins: [i18n, PrimeVue, router], stubs: { AuthCard: true, OverviewView: true } },
  })
  return { wrapper, router }
}

describe('LoginView', () => {
  it('shows the brand, the tagline, the language switch and the auth card', async () => {
    const { wrapper } = await mountViewAt('/login')

    expect(wrapper.text()).toContain('Remo')
    expect(wrapper.text()).toContain('Wartung planen. Wege sparen.')
    expect(wrapper.findComponent(LanguageSwitch).exists()).toBe(true)
    expect(wrapper.findComponent(AuthCard).exists()).toBe(true)
  })

  it('keeps the green success state visible for a moment before opening the requested page', async () => {
    const { wrapper, router } = await mountViewAt('/login?redirect=/calendar')

    wrapper.findComponent(AuthCard).vm.$emit('authenticated')
    vi.advanceTimersByTime(799)
    await flushPromises()
    expect(router.currentRoute.value.name).toBe('login')

    vi.advanceTimersByTime(1)
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/calendar')
  })

  it('opens the overview instead of an external redirect target', async () => {
    const { wrapper, router } = await mountViewAt('/login?redirect=https://evil.example')

    wrapper.findComponent(AuthCard).vm.$emit('authenticated')
    vi.advanceTimersByTime(800)
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('overview')
  })
})
