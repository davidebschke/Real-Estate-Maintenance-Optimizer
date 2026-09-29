import type { RouteLocationNormalized, RouteLocationRaw, Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resolveSafeRedirect } from '@/utils/safeRedirect'

/**
 * Decides where a navigation may go: every page except public ones (the login screen) requires a session,
 * and a logged-in user opening the login screen is sent on to the app instead.
 */
export async function resolveAuthNavigation(to: RouteLocationNormalized): Promise<true | RouteLocationRaw> {
  const authStore = useAuthStore()
  if (!authStore.isSessionChecked) {
    await authStore.restoreSession()
  }

  const isPublic = to.meta.public === true
  if (!isPublic && !authStore.isAuthenticated) {
    return { name: 'login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  if (to.name === 'login' && authStore.isAuthenticated) {
    return resolveSafeRedirect(to.query.redirect)
  }
  return true
}

/** Installs {@link resolveAuthNavigation} as a global guard on the given router. */
export function installAuthGuard(router: Router): void {
  router.beforeEach((to) => resolveAuthNavigation(to))
}
