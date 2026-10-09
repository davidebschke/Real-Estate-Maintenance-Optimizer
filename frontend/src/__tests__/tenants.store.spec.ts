import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { AxiosError, type AxiosResponse } from 'axios'
import { useTenantsStore } from '@/stores/tenants'
import * as apartmentService from '@/services/apartmentService'
import type { Apartment, ApartmentWithTenantPayload } from '@/types/apartment'

vi.mock('@/services/apartmentService')

/** Builds a sample apartment with one tenant, with overridable fields. */
function createApartment(overrides: Partial<Apartment> = {}): Apartment {
  return {
    id: 'apartment-1',
    propertyId: 'property-1',
    floor: 2,
    areaSquareMeters: 64.5,
    totalRent: 850,
    coldRent: 650,
    additionalCosts: 200,
    tenants: [{ id: 'tenant-1', firstName: 'Erika', lastName: 'Mustermann' }],
    ...overrides,
  }
}

const payload: ApartmentWithTenantPayload = {
  apartment: { floor: 2, areaSquareMeters: 64.5, totalRent: 850, coldRent: 650, additionalCosts: 200 },
  tenant: { firstName: 'Erika', lastName: 'Mustermann' },
}

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(apartmentService.fetchApartments).mockReset().mockResolvedValue([])
  vi.mocked(apartmentService.createApartment).mockReset().mockResolvedValue(createApartment())
  vi.mocked(apartmentService.addTenant).mockReset().mockResolvedValue(createApartment())
  vi.mocked(apartmentService.updateTenant).mockReset().mockResolvedValue(createApartment())
  vi.mocked(apartmentService.deleteTenant).mockReset().mockResolvedValue()
})

describe('useTenantsStore', () => {
  it('starts without apartments, errors or an open form', () => {
    const store = useTenantsStore()

    expect(store.apartmentsByProperty).toEqual({})
    expect(store.hasLoadError).toBe(false)
    expect(store.hasSaveError).toBe(false)
    expect(store.hasDeleteError).toBe(false)
    expect(store.isFormDialogOpen).toBe(false)
  })

  it('fetches the apartments of a property into its own entry', async () => {
    vi.mocked(apartmentService.fetchApartments).mockResolvedValue([createApartment()])
    const store = useTenantsStore()

    await store.fetchApartments('property-1')

    expect(apartmentService.fetchApartments).toHaveBeenCalledWith('property-1')
    expect(store.apartmentsByProperty['property-1']).toEqual([createApartment()])
    expect(store.hasLoadError).toBe(false)
  })

  it('records a load error and keeps the previous apartments when fetching fails', async () => {
    const store = useTenantsStore()
    store.apartmentsByProperty = { 'property-1': [createApartment()] }
    vi.mocked(apartmentService.fetchApartments).mockRejectedValue(new Error('Network Error'))

    await store.fetchApartments('property-1')

    expect(store.hasLoadError).toBe(true)
    expect(store.apartmentsByProperty['property-1']).toEqual([createApartment()])
  })

  it('ignores a fetch that was overtaken by a newer one', async () => {
    let resolveFirst!: (apartments: Apartment[]) => void
    vi.mocked(apartmentService.fetchApartments)
      .mockImplementationOnce(() => new Promise((resolve) => (resolveFirst = resolve)))
      .mockResolvedValueOnce([createApartment({ id: 'newer' })])
    const store = useTenantsStore()

    const first = store.fetchApartments('property-1')
    await store.fetchApartments('property-1')
    resolveFirst([createApartment({ id: 'older' })])
    await first

    expect(store.apartmentsByProperty['property-1']?.map((apartment) => apartment.id)).toEqual(['newer'])
  })

  it('creates an apartment and reloads the property afterwards', async () => {
    vi.mocked(apartmentService.fetchApartments).mockResolvedValue([createApartment()])
    const store = useTenantsStore()

    const succeeded = await store.createApartment('property-1', payload)

    expect(succeeded).toBe(true)
    expect(apartmentService.createApartment).toHaveBeenCalledWith('property-1', payload)
    expect(store.apartmentsByProperty['property-1']).toEqual([createApartment()])
    expect(store.hasSaveError).toBe(false)
  })

  it('adds a tenant to an apartment and reloads the property afterwards', async () => {
    const store = useTenantsStore()

    const succeeded = await store.addTenant('property-1', 'apartment-1', payload.tenant)

    expect(succeeded).toBe(true)
    expect(apartmentService.addTenant).toHaveBeenCalledWith('apartment-1', payload.tenant)
    expect(apartmentService.fetchApartments).toHaveBeenCalledWith('property-1')
  })

  it('updates a tenant together with their apartment', async () => {
    const store = useTenantsStore()

    const succeeded = await store.updateTenant('property-1', 'tenant-1', payload)

    expect(succeeded).toBe(true)
    expect(apartmentService.updateTenant).toHaveBeenCalledWith('tenant-1', payload)
  })

  it('records a save error and skips the reload when a save fails', async () => {
    vi.mocked(apartmentService.createApartment).mockRejectedValue(new Error('409'))
    const store = useTenantsStore()

    const succeeded = await store.createApartment('property-1', payload)

    expect(succeeded).toBe(false)
    expect(store.hasSaveError).toBe(true)
    expect(apartmentService.fetchApartments).not.toHaveBeenCalled()
  })

  it('deletes a tenant and reloads the property afterwards', async () => {
    const store = useTenantsStore()

    const succeeded = await store.deleteTenant('property-1', 'tenant-1')

    expect(succeeded).toBe(true)
    expect(apartmentService.deleteTenant).toHaveBeenCalledWith('tenant-1')
    expect(apartmentService.fetchApartments).toHaveBeenCalledWith('property-1')
    expect(store.hasDeleteError).toBe(false)
  })

  it('records a delete error when deleting fails and clears it with the next fetch', async () => {
    vi.mocked(apartmentService.deleteTenant).mockRejectedValue(new Error('500'))
    const store = useTenantsStore()

    const succeeded = await store.deleteTenant('property-1', 'tenant-1')
    expect(succeeded).toBe(false)
    expect(store.hasDeleteError).toBe(true)

    await store.fetchApartments('property-1')
    expect(store.hasDeleteError).toBe(false)
  })

  it('discards a save answered only after the account changed', async () => {
    let resolveCreate!: (apartment: Apartment) => void
    vi.mocked(apartmentService.createApartment).mockImplementation(
      () => new Promise((resolve) => (resolveCreate = resolve)),
    )
    const store = useTenantsStore()

    const pending = store.createApartment('property-1', payload)
    store.reset()
    resolveCreate(createApartment())
    const succeeded = await pending

    expect(succeeded).toBe(false)
    expect(apartmentService.fetchApartments).not.toHaveBeenCalled()
  })

  it('opens the form for a new tenant with a new apartment', () => {
    const store = useTenantsStore()

    store.openCreateDialog('property-1')

    expect(store.formTarget).toEqual({ mode: 'create', propertyId: 'property-1' })
    expect(store.isFormDialogOpen).toBe(true)
  })

  it('opens the form for a further tenant of an apartment', () => {
    const store = useTenantsStore()
    const apartment = createApartment()

    store.openAddTenantDialog(apartment)

    expect(store.formTarget).toEqual({ mode: 'add', apartment })
  })

  it('opens the form for editing a tenant', () => {
    const store = useTenantsStore()
    const apartment = createApartment()
    const tenant = apartment.tenants[0]!

    store.openEditDialog(apartment, tenant)

    expect(store.formTarget).toEqual({ mode: 'edit', apartment, tenant })
  })

  it('clears the save error when a form is opened', async () => {
    vi.mocked(apartmentService.createApartment).mockRejectedValue(new Error('409'))
    const store = useTenantsStore()
    await store.createApartment('property-1', payload)

    store.openCreateDialog('property-1')

    expect(store.hasSaveError).toBe(false)
  })

  it('closes the form when its visibility is set to false', () => {
    const store = useTenantsStore()
    store.openCreateDialog('property-1')

    store.isFormDialogOpen = false

    expect(store.formTarget).toBeNull()
  })

  it('keeps the localized message of a failed save and clears it with the next successful one', async () => {
    const limitError = new AxiosError('Request failed with status code 409')
    limitError.response = { status: 409, data: { message: 'An apartment can have at most 10 tenants.' } } as AxiosResponse
    vi.mocked(apartmentService.addTenant).mockRejectedValueOnce(limitError)
    const store = useTenantsStore()

    await store.addTenant('property-1', 'apartment-1', payload.tenant)
    expect(store.lastChangeErrorMessage).toBe('An apartment can have at most 10 tenants.')

    await store.addTenant('property-1', 'apartment-1', payload.tenant)
    expect(store.lastChangeErrorMessage).toBeNull()
  })

  it('has no message for a failure without a backend response', async () => {
    vi.mocked(apartmentService.createApartment).mockRejectedValue(new Error('Network Error'))
    const store = useTenantsStore()

    await store.createApartment('property-1', payload)

    expect(store.hasSaveError).toBe(true)
    expect(store.lastChangeErrorMessage).toBeNull()
  })

  it('clears a stale load error as soon as another property is loaded', async () => {
    vi.mocked(apartmentService.fetchApartments).mockRejectedValueOnce(new Error('Network Error'))
    const store = useTenantsStore()
    await store.fetchApartments('property-1')
    expect(store.hasLoadError).toBe(true)

    await store.fetchApartments('property-2')

    expect(store.hasLoadError).toBe(false)
  })

  it('forgets the cached apartments of a deleted property only', () => {
    const store = useTenantsStore()
    store.apartmentsByProperty = { 'property-1': [createApartment()], 'property-2': [createApartment({ id: 'other' })] }

    store.forgetProperty('property-1')

    expect(Object.keys(store.apartmentsByProperty)).toEqual(['property-2'])
  })

  it('forgets everything on reset', async () => {
    const store = useTenantsStore()
    store.apartmentsByProperty = { 'property-1': [createApartment()] }
    store.openCreateDialog('property-1')
    vi.mocked(apartmentService.fetchApartments).mockRejectedValue(new Error('x'))
    await store.fetchApartments('property-1')

    store.reset()

    expect(store.apartmentsByProperty).toEqual({})
    expect(store.hasLoadError).toBe(false)
    expect(store.formTarget).toBeNull()
  })
})
