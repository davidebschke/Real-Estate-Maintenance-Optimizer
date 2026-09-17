import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { usePropertiesStore } from '@/stores/properties'
import * as propertyService from '@/services/propertyService'
import type { Property } from '@/types/property'

vi.mock('@/services/propertyService')

/** Builds a sample property for tests, with overridable fields. */
function createProperty(overrides: Partial<Property> = {}): Property {
  return {
    id: '1',
    name: 'Wohnanlage Sonnenhof',
    address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    icon: 'pi-building',
    latitude: 50.94,
    longitude: 6.88,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(propertyService.fetchProperties).mockReset()
})

describe('usePropertiesStore', () => {
  it('starts with no properties and no load error', () => {
    const store = usePropertiesStore()

    expect(store.properties).toEqual([])
    expect(store.hasLoadError).toBe(false)
  })

  it('fetches properties from the backend', async () => {
    vi.mocked(propertyService.fetchProperties).mockResolvedValue([createProperty()])
    const store = usePropertiesStore()

    await store.fetchProperties()

    expect(store.properties).toEqual([createProperty()])
    expect(store.hasLoadError).toBe(false)
  })

  it('records a load error when the backend request fails', async () => {
    vi.mocked(propertyService.fetchProperties).mockRejectedValue(new Error('network error'))
    const store = usePropertiesStore()

    await store.fetchProperties()

    expect(store.properties).toEqual([])
    expect(store.hasLoadError).toBe(true)
  })

  it('clears a previous load error once a later fetch succeeds', async () => {
    vi.mocked(propertyService.fetchProperties).mockRejectedValueOnce(new Error('network error'))
    const store = usePropertiesStore()
    await store.fetchProperties()
    expect(store.hasLoadError).toBe(true)

    vi.mocked(propertyService.fetchProperties).mockResolvedValueOnce([createProperty()])
    await store.fetchProperties()

    expect(store.hasLoadError).toBe(false)
    expect(store.properties).toEqual([createProperty()])
  })
})
