import { describe, it, expect } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { routes, type NavigationKey } from '@/router'

function createTestRouter() {
  return createRouter({ history: createMemoryHistory(), routes })
}

describe('router', () => {
  it.each<[NavigationKey | 'statistics-properties', string]>([
    ['overview', '/'],
    ['calendar', '/calendar'],
    ['statistics', '/statistics'],
    ['properties', '/properties'],
    ['statistics-properties', '/statistics/properties'],
  ])('resolves the %s route to path %s', async (name, path) => {
    const router = createTestRouter()
    await router.push({ name })

    expect(router.currentRoute.value.path).toBe(path)
  })

  it('resolves the login route as the only public page, rendered without the app shell', async () => {
    const router = createTestRouter()
    await router.push({ name: 'login' })

    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.meta).toEqual({ public: true, layout: 'auth' })
    expect(routes.filter((route) => route.meta?.public)).toHaveLength(1)
  })
})
