import { describe, it, expect } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { routes, type NavigationKey } from '@/router'

function createTestRouter() {
  return createRouter({ history: createMemoryHistory(), routes })
}

describe('router', () => {
  it.each<[NavigationKey, string]>([
    ['overview', '/'],
    ['calendar', '/calendar'],
    ['statistics', '/statistics'],
    ['properties', '/properties'],
  ])('resolves the %s route to path %s', async (name, path) => {
    const router = createTestRouter()
    await router.push({ name })

    expect(router.currentRoute.value.path).toBe(path)
  })
})
