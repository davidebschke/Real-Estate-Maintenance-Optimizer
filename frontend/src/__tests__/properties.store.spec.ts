import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { usePropertiesStore } from '@/stores/properties'
import { useAuthStore } from '@/stores/auth'
import { useTenantsStore } from '@/stores/tenants'
import * as propertyService from '@/services/propertyService'
import * as appointmentService from '@/services/appointmentService'
import type { Property } from '@/types/property'

vi.mock('@/services/propertyService')
vi.mock('@/services/appointmentService')

/** Builds a sample property for tests, with overridable fields. */
function createProperty(overrides: Partial<Property> = {}): Property {
  return {
    id: '1',
    name: 'Wohnanlage Sonnenhof',
    address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
    icon: 'pi-building',
    latitude: 50.94,
    longitude: 6.88,
    tenantCount: 0,
    ...overrides,
  }
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(propertyService.fetchProperties).mockReset()
  vi.mocked(propertyService.createProperty).mockReset()
  vi.mocked(propertyService.updateProperty).mockReset()
  vi.mocked(propertyService.deleteProperty).mockReset()
  vi.mocked(appointmentService.fetchAppointments).mockReset().mockResolvedValue([])
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

  it('opens and closes the edit dialog', () => {
    const store = usePropertiesStore()
    const property = createProperty()

    store.openEditDialog(property)
    expect(store.editingProperty).toEqual(property)
    expect(store.isFormDialogOpen).toBe(true)

    store.closeEditDialog()
    expect(store.editingProperty).toBeNull()
    expect(store.isFormDialogOpen).toBe(false)
  })

  it('opens and closes the detail view of a property', () => {
    const store = usePropertiesStore()

    expect(store.activeDetailPropertyId).toBeNull()

    store.openDetail('1')
    expect(store.activeDetailPropertyId).toBe('1')

    store.closeDetail()
    expect(store.activeDetailPropertyId).toBeNull()
  })

  it('closes the detail view of a property when that property is deleted, but not that of another one', async () => {
    vi.mocked(propertyService.deleteProperty).mockResolvedValue(undefined)
    const store = usePropertiesStore()
    store.properties = [createProperty({ id: '1' }), createProperty({ id: '2', name: 'Aachener Hof' })]
    store.openDetail('2')

    await store.deleteProperty('1')
    expect(store.activeDetailPropertyId).toBe('2')

    await store.deleteProperty('2')
    expect(store.activeDetailPropertyId).toBeNull()
  })

  it('forgets the cached apartments of a deleted property', async () => {
    vi.mocked(propertyService.deleteProperty).mockResolvedValue(undefined)
    const store = usePropertiesStore()
    store.properties = [createProperty({ id: '1' })]
    useTenantsStore().apartmentsByProperty = { '1': [], '2': [] }

    await store.deleteProperty('1')

    expect(Object.keys(useTenantsStore().apartmentsByProperty)).toEqual(['2'])
  })

  it('closes both the create and edit dialog when the shared form dialog is closed', () => {
    const store = usePropertiesStore()
    store.openCreateDialog()
    store.openEditDialog(createProperty())

    store.isFormDialogOpen = false

    expect(store.isCreateDialogOpen).toBe(false)
    expect(store.editingProperty).toBeNull()
  })

  it('updates a property, replaces it in the list and re-sorts by name', async () => {
    vi.mocked(propertyService.updateProperty).mockResolvedValue(
      createProperty({ id: '1', name: 'Aachener Hof' }),
    )
    const store = usePropertiesStore()
    store.properties = [
      createProperty({ id: '1', name: 'Wohnanlage Sonnenhof' }),
      createProperty({ id: '2', name: 'Beispielhof' }),
    ]

    const result = await store.updateProperty('1', {
      name: 'Aachener Hof',
      address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
      latitude: 50.94,
      longitude: 6.88,
    })

    expect(result).toBe(true)
    expect(store.properties.map((property) => property.name)).toEqual(['Aachener Hof', 'Beispielhof'])
    expect(store.hasUpdateError).toBe(false)
  })

  it('records an update error when the backend request fails', async () => {
    vi.mocked(propertyService.updateProperty).mockRejectedValue(new Error('network error'))
    const store = usePropertiesStore()
    store.properties = [createProperty({ id: '1' })]

    const result = await store.updateProperty('1', {
      name: 'Aachener Hof',
      address: 'Aachener Str. 512, 50933 Köln-Braunsenfeld',
      latitude: null,
      longitude: null,
    })

    expect(result).toBe(false)
    expect(store.properties).toEqual([createProperty({ id: '1' })])
    expect(store.hasUpdateError).toBe(true)
  })

  it('deletes a property, removes it from the list and refreshes the appointments', async () => {
    vi.mocked(propertyService.deleteProperty).mockResolvedValue(undefined)
    const store = usePropertiesStore()
    store.properties = [createProperty({ id: '1' }), createProperty({ id: '2', name: 'Aachener Hof' })]

    const result = await store.deleteProperty('1')

    expect(result).toBe(true)
    expect(store.properties.map((property) => property.id)).toEqual(['2'])
    expect(store.hasDeleteError).toBe(false)
    expect(appointmentService.fetchAppointments).toHaveBeenCalled()
  })

  it('records a delete error and keeps the property when the backend request fails', async () => {
    vi.mocked(propertyService.deleteProperty).mockRejectedValue(new Error('network error'))
    const store = usePropertiesStore()
    store.properties = [createProperty({ id: '1' })]

    const result = await store.deleteProperty('1')

    expect(result).toBe(false)
    expect(store.properties).toEqual([createProperty({ id: '1' })])
    expect(store.hasDeleteError).toBe(true)
  })

  it('still reports a successful delete when only the post-delete appointments refresh fails', async () => {
    vi.mocked(propertyService.deleteProperty).mockResolvedValue(undefined)
    vi.mocked(appointmentService.fetchAppointments).mockRejectedValue(new Error('network error'))
    const store = usePropertiesStore()
    store.properties = [createProperty({ id: '1' })]

    const result = await store.deleteProperty('1')

    expect(result).toBe(true)
    expect(store.properties).toEqual([])
    expect(store.hasDeleteError).toBe(false)
  })

  it('refreshes the remaining creation limit of a demo account only after a successful create', async () => {
    const refreshDemoQuota = vi.spyOn(useAuthStore(), 'refreshDemoQuota').mockResolvedValue()
    const store = usePropertiesStore()
    vi.mocked(propertyService.createProperty).mockRejectedValueOnce(new Error('403'))
    await store.createProperty({ name: 'Objekt', address: 'Str. 1', latitude: null, longitude: null })
    expect(refreshDemoQuota).not.toHaveBeenCalled()

    vi.mocked(propertyService.createProperty).mockResolvedValue(createProperty())
    await store.createProperty({ name: 'Objekt', address: 'Str. 1', latitude: null, longitude: null })

    expect(refreshDemoQuota).toHaveBeenCalledOnce()
  })

  it('forgets every property, error and open form on reset and ignores a fetch still in flight', async () => {
    let resolveFetch: (properties: Property[]) => void = () => {}
    vi.mocked(propertyService.fetchProperties).mockReturnValue(new Promise((resolve) => (resolveFetch = resolve)))
    const store = usePropertiesStore()
    store.properties = [createProperty()]
    store.hasCreateError = true
    store.openEditDialog(createProperty())
    store.openDetail('1')
    const pendingFetch = store.fetchProperties()

    store.reset()
    resolveFetch([createProperty({ id: 'from-previous-account' })])
    await pendingFetch

    expect(store.properties).toEqual([])
    expect(store.hasCreateError).toBe(false)
    expect(store.isFormDialogOpen).toBe(false)
    expect(store.activeDetailPropertyId).toBeNull()
  })

  it('does not add a property to the list when its creation is answered only after an account change', async () => {
    let resolveCreate: (property: Property) => void = () => {}
    vi.mocked(propertyService.createProperty).mockReturnValue(new Promise((resolve) => (resolveCreate = resolve)))
    const store = usePropertiesStore()
    const pendingCreate = store.createProperty({ name: 'Objekt', address: 'Str. 1', latitude: null, longitude: null })

    store.reset()
    resolveCreate(createProperty({ id: 'from-previous-account' }))

    expect(await pendingCreate).toBe(false)
    expect(store.properties).toEqual([])
  })

  it('ignores an update or delete answered only after an account change', async () => {
    vi.mocked(propertyService.updateProperty).mockResolvedValue(createProperty({ name: 'Umbenannt' }))
    vi.mocked(propertyService.deleteProperty).mockResolvedValue()
    const store = usePropertiesStore()
    const pendingUpdate = store.updateProperty('1', { name: 'Umbenannt', address: 'Str. 1', latitude: null, longitude: null })
    const pendingDelete = store.deleteProperty('1')

    store.reset()
    store.properties = [createProperty()]

    expect(await pendingUpdate).toBe(false)
    expect(await pendingDelete).toBe(false)
    expect(store.properties).toEqual([createProperty()])
  })
})
