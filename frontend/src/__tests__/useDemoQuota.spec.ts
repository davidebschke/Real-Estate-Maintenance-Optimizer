import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useDemoQuota } from '@/composables/useDemoQuota'
import { useAuthStore } from '@/stores/auth'
import type { CurrentUser } from '@/types/auth'

/** Logs in the given account directly in the store. */
function logInAs(overrides: Partial<CurrentUser>) {
  useAuthStore().currentUser = {
    username: 'demo-1',
    displayName: 'Demo',
    demoAccount: true,
    expiresAt: new Date(),
    remainingPropertyCreations: 3,
    remainingAppointmentCreations: 3,
    appointmentBufferMinutes: 15,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
})

describe('useDemoQuota', () => {
  it('reports the remaining creations of a demo account per resource', () => {
    logInAs({ remainingPropertyCreations: 2, remainingAppointmentCreations: 1 })

    expect(useDemoQuota('properties').remaining.value).toBe(2)
    expect(useDemoQuota('appointments').remaining.value).toBe(1)
    expect(useDemoQuota('properties').isLimited.value).toBe(true)
    expect(useDemoQuota('properties').isExhausted.value).toBe(false)
  })

  it('reports an exhausted limit', () => {
    logInAs({ remainingAppointmentCreations: 0 })

    expect(useDemoQuota('appointments').isExhausted.value).toBe(true)
  })

  it('reacts when the remaining creations change', () => {
    logInAs({ remainingPropertyCreations: 1 })
    const quota = useDemoQuota('properties')

    useAuthStore().currentUser!.remainingPropertyCreations = 0

    expect(quota.isExhausted.value).toBe(true)
  })

  it('treats a regular account and a logged-out visitor as unlimited', () => {
    logInAs({ demoAccount: false, remainingPropertyCreations: null, remainingAppointmentCreations: null })
    expect(useDemoQuota('properties').isLimited.value).toBe(false)
    expect(useDemoQuota('properties').isExhausted.value).toBe(false)

    useAuthStore().currentUser = null
    expect(useDemoQuota('appointments').remaining.value).toBeNull()
  })
})
