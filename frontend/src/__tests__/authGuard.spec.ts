import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { routes } from '@/router'
import { installAuthGuard } from '@/router/authGuard'
import { useAuthStore } from '@/stores/auth'
import * as authService from '@/services/authService'

vi.mock('@/services/authService')

const loggedInUser = {
  username: 'debschke',
  displayName: 'David Ebschke',
  demoAccount: false,
  expiresAt: null,
  remainingPropertyCreations: null,
  remainingAppointmentCreations: null,
  remainingTenantCreations: null,
  appointmentBufferMinutes: 15,
}

/** Creates a router with the app's routes and the auth guard, backed by in-memory history. */
function createGuardedRouter() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  installAuthGuard(router)
  return router
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(authService.fetchCurrentUser).mockReset()
})

describe('auth guard', () => {
  it('sends a visitor without session to the login screen, remembering the requested page', async () => {
    vi.mocked(authService.fetchCurrentUser).mockResolvedValue(null)
    const router = createGuardedRouter()

    await router.push('/calendar')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/calendar')
  })

  it('does not add a redirect for the start page', async () => {
    vi.mocked(authService.fetchCurrentUser).mockResolvedValue(null)
    const router = createGuardedRouter()

    await router.push('/')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBeUndefined()
  })

  it('lets a logged-in user through to every page', async () => {
    vi.mocked(authService.fetchCurrentUser).mockResolvedValue(loggedInUser)
    const router = createGuardedRouter()

    await router.push('/properties')

    expect(router.currentRoute.value.name).toBe('properties')
  })

  it('checks the session only once across navigations', async () => {
    vi.mocked(authService.fetchCurrentUser).mockResolvedValue(loggedInUser)
    const router = createGuardedRouter()

    await router.push('/')
    await router.push('/calendar')

    expect(authService.fetchCurrentUser).toHaveBeenCalledOnce()
  })

  it('sends a logged-in user opening the login screen on to the requested page', async () => {
    vi.mocked(authService.fetchCurrentUser).mockResolvedValue(loggedInUser)
    const router = createGuardedRouter()

    await router.push({ name: 'login', query: { redirect: '/calendar' } })

    expect(router.currentRoute.value.name).toBe('calendar')
  })

  it('never follows a redirect to another site after login', async () => {
    vi.mocked(authService.fetchCurrentUser).mockResolvedValue(loggedInUser)
    const router = createGuardedRouter()

    await router.push({ name: 'login', query: { redirect: '//evil.example/phish' } })

    expect(router.currentRoute.value.name).toBe('overview')
  })

  it('blocks the app again right after the session was cleared', async () => {
    vi.mocked(authService.fetchCurrentUser).mockResolvedValue(loggedInUser)
    const router = createGuardedRouter()
    await router.push('/calendar')

    useAuthStore().clearSession()
    await router.push('/properties')

    expect(router.currentRoute.value.name).toBe('login')
  })
})
