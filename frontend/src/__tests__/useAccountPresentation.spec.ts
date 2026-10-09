import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { i18n } from '@/i18n'
import { useAccountPresentation } from '@/composables/useAccountPresentation'
import { useAuthStore } from '@/stores/auth'
import type { CurrentUser } from '@/types/auth'

/** Runs the composable inside a component, since it needs the i18n instance. */
function presentAccount() {
  let presentation!: ReturnType<typeof useAccountPresentation>
  mount(
    defineComponent({
      setup() {
        presentation = useAccountPresentation()
        return () => h('div')
      },
    }),
    { global: { plugins: [i18n] } },
  )
  return presentation
}

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
    remainingAiOptimizations: null,
    appointmentBufferMinutes: 15,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  i18n.global.locale.value = 'de'
})

afterEach(() => {
  i18n.global.locale.value = 'de'
})

describe('useAccountPresentation', () => {
  it('presents a regular account by its display name, initials and username', () => {
    logInAs()

    const presentation = presentAccount()

    expect(presentation.displayName.value).toBe('David Ebschke')
    expect(presentation.initials.value).toBe('DE')
    expect(presentation.roleLabel.value).toBe('Angemeldet als debschke')
    expect(presentation.sessionEndTime.value).toBe('')
  })

  it('presents a demo account under its localized name with the end of its session', () => {
    logInAs({ displayName: 'Demo', demoAccount: true, expiresAt: new Date(2026, 8, 29, 16, 30) })

    const presentation = presentAccount()

    expect(presentation.displayName.value).toBe('Demo-Account')
    expect(presentation.initials.value).toBe('DA')
    expect(presentation.sessionEndTime.value).toBe('16:30')
    expect(presentation.roleLabel.value).toBe('Testzugang · endet 16:30 Uhr')
  })

  it('follows the active language', () => {
    logInAs({ displayName: 'Demo', demoAccount: true, expiresAt: new Date(2026, 8, 29, 16, 30) })
    const presentation = presentAccount()

    i18n.global.locale.value = 'en'

    expect(presentation.displayName.value).toBe('Demo account')
    expect(presentation.roleLabel.value).toContain('Trial access')
  })

  it('presents nothing while logged out', () => {
    const presentation = presentAccount()

    expect(presentation.displayName.value).toBe('')
    expect(presentation.initials.value).toBe('')
    expect(presentation.roleLabel.value).toBe('')
  })
})
