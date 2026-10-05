import { afterEach, describe, expect, it } from 'vitest'
import { useRouteMode } from '@/composables/useRouteMode'

afterEach(() => {
  useRouteMode().routeMode.value = 'car'
})

describe('useRouteMode', () => {
  it('defaults to travelling by car', () => {
    expect(useRouteMode().routeMode.value).toBe('car')
  })

  it('shares the selected mode between every caller', () => {
    useRouteMode().routeMode.value = 'walking'

    expect(useRouteMode().routeMode.value).toBe('walking')
  })
})
