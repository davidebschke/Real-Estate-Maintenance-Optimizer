import { beforeEach, describe, it, expect } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import DemoAccountBanner from '@/components/auth/DemoAccountBanner.vue'
import { routes } from '@/router'
import { useAuthStore } from '@/stores/auth'
import App from '../App.vue'

/** Mounts the app shallowly at the given location. */
async function mountAppAt(location: string) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(location)
  return shallowMount(App, { global: { plugins: [router] } })
}

/** Logs in an account directly in the store. */
function logIn() {
  useAuthStore().currentUser = {
    username: 'debschke',
    displayName: 'David Ebschke',
    demoAccount: false,
    expiresAt: null,
    remainingPropertyCreations: null,
    remainingAppointmentCreations: null,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
})

describe('App', () => {
  it('renders the application header, demo banner slot and footer for a logged-in account', async () => {
    logIn()

    const wrapper = await mountAppAt('/')

    expect(wrapper.findComponent(AppHeader).exists()).toBe(true)
    expect(wrapper.findComponent(DemoAccountBanner).exists()).toBe(true)
    expect(wrapper.findComponent(AppFooter).exists()).toBe(true)
  })

  it('shows nothing of the app shell on the login screen', async () => {
    const wrapper = await mountAppAt('/login')

    expect(wrapper.findComponent(RouterView).exists()).toBe(true)
    expect(wrapper.findComponent(AppHeader).exists()).toBe(false)
    expect(wrapper.findComponent(AppFooter).exists()).toBe(false)
  })

  it('renders neither the app shell nor the protected page without a session until the guard redirected', async () => {
    const wrapper = await mountAppAt('/calendar')

    expect(wrapper.findComponent(AppHeader).exists()).toBe(false)
    expect(wrapper.findComponent(RouterView).exists()).toBe(false)
  })
})
