import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useAppointmentsStore } from '@/stores/appointments'
import { usePropertiesStore } from '@/stores/properties'
import { useTenantsStore } from '@/stores/tenants'
import * as authService from '@/services/authService'
import * as accountService from '@/services/accountService'
import type { CurrentUser } from '@/types/auth'

vi.mock('@/services/authService')
vi.mock('@/services/accountService')

/** Builds a sample account for tests, with overridable fields. */
function createUser(overrides: Partial<CurrentUser> = {}): CurrentUser {
  return {
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

const demoUser = createUser({
  username: 'demo-1',
  displayName: 'Demo',
  demoAccount: true,
  expiresAt: new Date('2026-09-29T16:00:00Z'),
  remainingPropertyCreations: 3,
  remainingAppointmentCreations: 3,
  remainingTenantCreations: 10,
  remainingAiOptimizations: 1,
  appointmentBufferMinutes: 15,
})

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(authService.fetchCurrentUser).mockReset()
  vi.mocked(authService.login).mockReset()
  vi.mocked(authService.createDemoAccount).mockReset()
  vi.mocked(authService.logout).mockReset()
  vi.mocked(accountService.changeUsername).mockReset()
  vi.mocked(accountService.changePassword).mockReset()
  vi.mocked(accountService.changeAppointmentBuffer).mockReset()
})

/** Fills every data store as if a previous account had loaded its data. */
function fillDataStoresOfPreviousAccount() {
  useAppointmentsStore().appointments = [{ id: 'old' } as never]
  usePropertiesStore().properties = [{ id: 'old' } as never]
  useTenantsStore().apartmentsByProperty = { old: [] }
}

describe('useAuthStore', () => {
  it('starts without account and without having checked the session', () => {
    const store = useAuthStore()

    expect(store.currentUser).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    expect(store.isSessionChecked).toBe(false)
  })

  it('restores a still-valid session', async () => {
    vi.mocked(authService.fetchCurrentUser).mockResolvedValue(createUser())
    const store = useAuthStore()

    await store.restoreSession()

    expect(store.isAuthenticated).toBe(true)
    expect(store.currentUser?.displayName).toBe('David Ebschke')
    expect(store.isSessionChecked).toBe(true)
  })

  it('treats a failing session check as logged out instead of throwing', async () => {
    vi.mocked(authService.fetchCurrentUser).mockRejectedValue(new Error('offline'))
    const store = useAuthStore()

    await store.restoreSession()

    expect(store.isAuthenticated).toBe(false)
    expect(store.isSessionChecked).toBe(true)
  })

  it('logs in and drops data cached for a previous account', async () => {
    vi.mocked(authService.login).mockResolvedValue(createUser())
    fillDataStoresOfPreviousAccount()
    const store = useAuthStore()

    await store.login('debschke', 'geheim')

    expect(authService.login).toHaveBeenCalledWith('debschke', 'geheim')
    expect(store.isAuthenticated).toBe(true)
    expect(useAppointmentsStore().appointments).toEqual([])
    expect(usePropertiesStore().properties).toEqual([])
    expect(useTenantsStore().apartmentsByProperty).toEqual({})
  })

  it('keeps the user logged out when the login is rejected', async () => {
    vi.mocked(authService.login).mockRejectedValue(new Error('401'))
    const store = useAuthStore()

    await expect(store.login('debschke', 'falsch')).rejects.toThrow('401')
    expect(store.isAuthenticated).toBe(false)
  })

  it('starts a demo session', async () => {
    vi.mocked(authService.createDemoAccount).mockResolvedValue(demoUser)
    const store = useAuthStore()

    await store.startDemoSession()

    expect(store.isDemoAccount).toBe(true)
  })

  it('logs out and forgets the account and every cached data', async () => {
    vi.mocked(authService.login).mockResolvedValue(createUser())
    vi.mocked(authService.logout).mockResolvedValue()
    const store = useAuthStore()
    await store.login('debschke', 'geheim')
    fillDataStoresOfPreviousAccount()

    await store.logout()

    expect(store.isAuthenticated).toBe(false)
    expect(useAppointmentsStore().appointments).toEqual([])
    expect(usePropertiesStore().properties).toEqual([])
    expect(useTenantsStore().apartmentsByProperty).toEqual({})
  })

  it('forgets the account locally even when the logout request fails', async () => {
    vi.mocked(authService.login).mockResolvedValue(createUser())
    vi.mocked(authService.logout).mockRejectedValue(new Error('offline'))
    const store = useAuthStore()
    await store.login('debschke', 'geheim')

    await expect(store.logout()).rejects.toThrow('offline')
    expect(store.isAuthenticated).toBe(false)
  })

  it('refreshes the remaining creation limits of a demo account', async () => {
    vi.mocked(authService.createDemoAccount).mockResolvedValue(demoUser)
    vi.mocked(authService.fetchCurrentUser).mockResolvedValue({ ...demoUser, remainingPropertyCreations: 2 })
    const store = useAuthStore()
    await store.startDemoSession()

    await store.refreshDemoQuota()

    expect(store.currentUser?.remainingPropertyCreations).toBe(2)
  })

  it('does not refresh anything for a regular account', async () => {
    vi.mocked(authService.login).mockResolvedValue(createUser())
    const store = useAuthStore()
    await store.login('debschke', 'geheim')

    await store.refreshDemoQuota()

    expect(authService.fetchCurrentUser).not.toHaveBeenCalled()
  })

  it('keeps the last known limits when refreshing them fails', async () => {
    vi.mocked(authService.createDemoAccount).mockResolvedValue(demoUser)
    vi.mocked(authService.fetchCurrentUser).mockRejectedValue(new Error('offline'))
    const store = useAuthStore()
    await store.startDemoSession()

    await store.refreshDemoQuota()

    expect(store.currentUser?.remainingPropertyCreations).toBe(3)
  })

  it('clears an expired session without any backend request', () => {
    const store = useAuthStore()
    store.currentUser = createUser()
    fillDataStoresOfPreviousAccount()

    store.clearSession()

    expect(store.isAuthenticated).toBe(false)
    expect(store.isSessionChecked).toBe(true)
    expect(usePropertiesStore().properties).toEqual([])
    expect(useTenantsStore().apartmentsByProperty).toEqual({})
    expect(authService.logout).not.toHaveBeenCalled()
  })

  it('takes over the renamed account without dropping its cached data', async () => {
    vi.mocked(accountService.changeUsername).mockResolvedValue(createUser({ username: 'neuer-name' }))
    const store = useAuthStore()
    store.currentUser = createUser()
    fillDataStoresOfPreviousAccount()

    await store.changeUsername('neuer-name', 'geheim')

    expect(accountService.changeUsername).toHaveBeenCalledWith('neuer-name', 'geheim')
    expect(store.currentUser?.username).toBe('neuer-name')
    expect(usePropertiesStore().properties).toHaveLength(1)
  })

  it('keeps the account unchanged when renaming is rejected', async () => {
    vi.mocked(accountService.changeUsername).mockRejectedValue(new Error('taken'))
    const store = useAuthStore()
    store.currentUser = createUser()

    await expect(store.changeUsername('vergeben', 'geheim')).rejects.toThrow('taken')

    expect(store.currentUser?.username).toBe('debschke')
  })

  it('stays logged in after changing the password', async () => {
    vi.mocked(accountService.changePassword).mockResolvedValue(createUser())
    const store = useAuthStore()
    store.currentUser = createUser()

    await store.changePassword('alt', 'neu')

    expect(accountService.changePassword).toHaveBeenCalledWith('alt', 'neu')
    expect(store.isAuthenticated).toBe(true)
  })

  it('takes over the changed appointment buffer', async () => {
    vi.mocked(accountService.changeAppointmentBuffer).mockResolvedValue(createUser({ appointmentBufferMinutes: 40 }))
    const store = useAuthStore()
    store.currentUser = createUser()

    await store.changeAppointmentBuffer(40)

    expect(accountService.changeAppointmentBuffer).toHaveBeenCalledWith(40)
    expect(store.currentUser?.appointmentBufferMinutes).toBe(40)
  })
})
