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
  vi.mocked(propertyService.createProperty).mockReset()
})

describe('usePropertiesStore', () => {
  it('starts with no properties, no errors and the create dialog closed', () => {
    const store = usePropertiesStore()

    expect(store.properties).toEqual([])
    expect(store.hasLoadError).toBe(false)
    expect(store.isCreateDialogOpen).toBe(false)
  })

  it('opens and closes the create dialog', () => {
    const store = usePropertiesStore()

    store.openCreateDialog()
    expect(store.isCreateDialogOpen).toBe(true)

    store.closeCreateDialog()
    expect(store.isCreateDialogOpen).toBe(false)
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

  it('creates a property and appends it to the list', async () => {
    vi.mocked(propertyService.createProperty).mockResolvedValue(createProperty({ id: '2' }))
    const store = usePropertiesStore()
    store.properties = [createProperty({ id: '1' })]

    const result = await store.createProperty({
      name: 'Wohnanlage Sonnenhof',
      address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
      latitude: 50.94,
      longitude: 6.88,
    })

    expect(result).toBe(true)
    expect(store.properties).toEqual([createProperty({ id: '1' }), createProperty({ id: '2' })])
    expect(store.hasCreateError).toBe(false)
  })

  it('records a create error when the backend request fails', async () => {
    vi.mocked(propertyService.createProperty).mockRejectedValue(new Error('network error'))
    const store = usePropertiesStore()

    const result = await store.createProperty({
      name: 'Wohnanlage Sonnenhof',
      address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
      latitude: null,
      longitude: null,
    })

    expect(result).toBe(false)
    expect(store.properties).toEqual([])
    expect(store.hasCreateError).toBe(true)
  })

  it('inserts a created property alphabetically rather than always at the end', async () => {
    vi.mocked(propertyService.createProperty).mockResolvedValue(
      createProperty({ id: '2', name: 'Aachener Hof' }),
    )
    const store = usePropertiesStore()
    store.properties = [createProperty({ id: '1', name: 'Wohnanlage Sonnenhof' })]

    await store.createProperty({
      name: 'Aachener Hof',
      address: 'Beispielstr. 1',
      latitude: null,
      longitude: null,
    })

    expect(store.properties.map((property) => property.id)).toEqual(['2', '1'])
  })

  it('ignores a fetchProperties response that resolves after a newer create already happened', async () => {
    const store = usePropertiesStore()
    let resolveStaleFetch!: (properties: Property[]) => void
    vi.mocked(propertyService.fetchProperties).mockReturnValue(
      new Promise((resolve) => {
        resolveStaleFetch = resolve
      }),
    )
    const staleFetch = store.fetchProperties()

    vi.mocked(propertyService.createProperty).mockResolvedValue(createProperty({ id: '2' }))
    await store.createProperty({
      name: 'Wohnanlage Sonnenhof',
      address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
      latitude: 50.94,
      longitude: 6.88,
    })

    resolveStaleFetch([createProperty({ id: '1' })])
    await staleFetch

    expect(store.properties.map((property) => property.id)).toEqual(['2'])
  })
})
