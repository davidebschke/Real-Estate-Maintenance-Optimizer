import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { i18n } from '@/i18n'
import DemoAccountBanner from '@/components/auth/DemoAccountBanner.vue'
import { useAuthStore } from '@/stores/auth'
import type { CurrentUser } from '@/types/auth'

/** Logs in the given account directly in the store. */
function logInAs(overrides: Partial<CurrentUser>) {
  useAuthStore().currentUser = {
    username: 'demo-1',
    displayName: 'Demo',
    demoAccount: true,
    expiresAt: new Date(2026, 8, 29, 16, 30),
    remainingPropertyCreations: 3,
    remainingAppointmentCreations: 3,
    remainingTenantCreations: 10,
    appointmentBufferMinutes: 15,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
})

describe('DemoAccountBanner', () => {
  it('reminds a demo account when its data gets deleted and what it may still create', () => {
    logInAs({ remainingPropertyCreations: 2, remainingAppointmentCreations: 1 })

    const text = mount(DemoAccountBanner, { global: { plugins: [i18n] } }).text()

    expect(text).toContain('Demo-Account')
    expect(text).toContain('beim Abmelden gelöscht, spätestens um 16:30 Uhr')
    expect(text).toContain('2 weitere Objekte')
    expect(text).toContain('ein weiterer Termin')
    expect(text).toContain('10 weitere Mieter')
  })

  it('says so once nothing more can be created', () => {
    logInAs({ remainingPropertyCreations: 0, remainingAppointmentCreations: 0, remainingTenantCreations: 0 })

    const text = mount(DemoAccountBanner, { global: { plugins: [i18n] } }).text()

    expect(text).toContain('kein weiteres Objekt')
    expect(text).toContain('kein weiterer Termin')
    expect(text).toContain('kein weiterer Mieter')
  })

  it('is not shown for a regular account', () => {
    logInAs({ demoAccount: false, expiresAt: null, remainingPropertyCreations: null })

    const wrapper = mount(DemoAccountBanner, { global: { plugins: [i18n] } })

    expect(wrapper.find('.demo-account-banner').exists()).toBe(false)
  })
})
