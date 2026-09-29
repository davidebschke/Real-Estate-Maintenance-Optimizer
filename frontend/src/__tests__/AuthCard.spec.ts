import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import { i18n } from '@/i18n'
import AuthCard from '@/components/auth/AuthCard.vue'
import LoginForm from '@/components/auth/LoginForm.vue'
import DemoAccountPanel from '@/components/auth/DemoAccountPanel.vue'

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
})

/** Mounts the card with both of its panels stubbed, so only its own switching and success logic is under test. */
function mountCard() {
  return mount(AuthCard, {
    global: { plugins: [i18n, PrimeVue], stubs: { LoginForm: true, DemoAccountPanel: true } },
  })
}

describe('AuthCard', () => {
  it('asks the user to log in and starts with the login form', () => {
    const wrapper = mountCard()

    expect(wrapper.find('h1').text()).toBe('Willkommen bei Remo')
    expect(wrapper.text()).toContain('Bitte melden Sie sich an')
    expect(wrapper.findComponent(LoginForm).exists()).toBe(true)
    expect(wrapper.findComponent(DemoAccountPanel).exists()).toBe(false)
    expect(wrapper.find('#auth-mode-login').attributes('aria-selected')).toBe('true')
  })

  it('switches to the demo account panel and back', async () => {
    const wrapper = mountCard()

    await wrapper.find('#auth-mode-demo').trigger('click')
    expect(wrapper.findComponent(DemoAccountPanel).exists()).toBe(true)
    expect(wrapper.find('#auth-mode-demo').attributes('aria-selected')).toBe('true')

    await wrapper.find('#auth-mode-login').trigger('click')
    expect(wrapper.findComponent(LoginForm).exists()).toBe(true)
  })

  it('turns green, locks the other mode and passes the success on once a session started', async () => {
    const wrapper = mountCard()

    await wrapper.findComponent(LoginForm).vm.$emit('authenticated')

    expect(wrapper.find('.auth-card').classes()).toContain('auth-card--success')
    expect(wrapper.find('#auth-mode-demo').attributes('disabled')).toBeDefined()
    expect(wrapper.emitted('authenticated')).toHaveLength(1)

    await wrapper.find('#auth-mode-demo').trigger('click')
    expect(wrapper.findComponent(LoginForm).exists()).toBe(true)
  })

  it('also passes on the success of a created demo account', async () => {
    const wrapper = mountCard()
    await wrapper.find('#auth-mode-demo').trigger('click')

    await wrapper.findComponent(DemoAccountPanel).vm.$emit('authenticated')

    expect(wrapper.emitted('authenticated')).toHaveLength(1)
  })
})
