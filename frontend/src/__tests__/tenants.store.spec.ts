import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { AxiosError, type AxiosResponse } from 'axios'
import { useTenantsStore } from '@/stores/tenants'
import { useAuthStore } from '@/stores/auth'
import { usePaymentsStore } from '@/stores/payments'
import * as apartmentService from '@/services/apartmentService'
import * as propertyService from '@/services/propertyService'
import type { Apartment, ApartmentUpdatePayload, NewApartmentPayload } from '@/types/apartment'

vi.mock('@/services/apartmentService')
vi.mock('@/services/propertyService')

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

const tenant = { firstName: 'Erika', lastName: 'Mustermann' }

const payload: NewApartmentPayload = {
  apartment: { floor: 2, areaSquareMeters: 64.5, totalRent: 850, coldRent: 650, additionalCosts: 200 },
  tenant,
}

const updatePayload: ApartmentUpdatePayload = { apartment: payload.apartment, rentEffectiveMonth: null }

beforeEach(() => {
  setActivePinia(createPinia())
  vi.mocked(apartmentService.fetchApartments).mockReset().mockResolvedValue([])
  vi.mocked(propertyService.fetchProperties).mockReset().mockResolvedValue([])
  vi.mocked(apartmentService.createApartment).mockReset().mockResolvedValue(createApartment())
  vi.mocked(apartmentService.updateApartment).mockReset().mockResolvedValue(createApartment())
  vi.mocked(apartmentService.deleteApartment).mockReset().mockResolvedValue()
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
    expect(store.hasApartmentDeleteError).toBe(false)
    expect(store.isApartmentFormDialogOpen).toBe(false)
    expect(store.isTenantFormDialogOpen).toBe(false)
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

  it('creates a vacant apartment without a tenant', async () => {
    const store = useTenantsStore()

    const succeeded = await store.createApartment('property-1', { apartment: payload.apartment })

    expect(succeeded).toBe(true)
    expect(apartmentService.createApartment).toHaveBeenCalledWith('property-1', { apartment: payload.apartment })
  })

  it('reloads the properties after a change so their tenant counts stay current', async () => {
    const store = useTenantsStore()

    await store.addTenant('property-1', 'apartment-1', tenant)

    expect(propertyService.fetchProperties).toHaveBeenCalledTimes(1)
  })

  it('does not reload the properties when a change fails', async () => {
    vi.mocked(apartmentService.deleteTenant).mockRejectedValue(new Error('500'))
    const store = useTenantsStore()

    await store.deleteTenant('property-1', 'tenant-1')

    expect(propertyService.fetchProperties).not.toHaveBeenCalled()
  })

  it('updates an apartment and reloads the property afterwards', async () => {
    vi.mocked(apartmentService.fetchApartments).mockResolvedValue([createApartment({ totalRent: 900 })])
    const store = useTenantsStore()

    const succeeded = await store.updateApartment('property-1', 'apartment-1', updatePayload)

    expect(succeeded).toBe(true)
    expect(apartmentService.updateApartment).toHaveBeenCalledWith('apartment-1', updatePayload)
    expect(store.apartmentsByProperty['property-1']?.[0]?.totalRent).toBe(900)
    expect(propertyService.fetchProperties).toHaveBeenCalledTimes(1)
  })

  it('records a save error when updating an apartment fails', async () => {
    vi.mocked(apartmentService.updateApartment).mockRejectedValue(new Error('400'))
    const store = useTenantsStore()

    const succeeded = await store.updateApartment('property-1', 'apartment-1', updatePayload)

    expect(succeeded).toBe(false)
    expect(store.hasSaveError).toBe(true)
    expect(apartmentService.fetchApartments).not.toHaveBeenCalled()
  })

  it('deletes an apartment, drops it from the cache even if the reload fails and reloads the properties', async () => {
    vi.mocked(apartmentService.fetchApartments).mockRejectedValue(new Error('Network Error'))
    const store = useTenantsStore()
    store.apartmentsByProperty = { 'property-1': [createApartment(), createApartment({ id: 'apartment-2' })] }

    const succeeded = await store.deleteApartment('property-1', 'apartment-1')

    expect(succeeded).toBe(true)
    expect(apartmentService.deleteApartment).toHaveBeenCalledWith('apartment-1')
    expect(store.apartmentsByProperty['property-1']?.map((apartment) => apartment.id)).toEqual(['apartment-2'])
    expect(propertyService.fetchProperties).toHaveBeenCalledTimes(1)
    expect(store.hasApartmentDeleteError).toBe(false)
  })

  it('keeps the cached apartment and records an apartment delete error when deleting it fails', async () => {
    vi.mocked(apartmentService.deleteApartment).mockRejectedValue(new Error('500'))
    const store = useTenantsStore()
    store.apartmentsByProperty = { 'property-1': [createApartment()] }

    const succeeded = await store.deleteApartment('property-1', 'apartment-1')

    expect(succeeded).toBe(false)
    expect(store.hasApartmentDeleteError).toBe(true)
    expect(store.hasDeleteError).toBe(false)
    expect(store.apartmentsByProperty['property-1']).toHaveLength(1)
    expect(propertyService.fetchProperties).not.toHaveBeenCalled()
  })

  it('clears an apartment delete error with the next fetch', async () => {
    vi.mocked(apartmentService.deleteApartment).mockRejectedValue(new Error('500'))
    const store = useTenantsStore()
    await store.deleteApartment('property-1', 'apartment-1')

    await store.fetchApartments('property-1')

    expect(store.hasApartmentDeleteError).toBe(false)
  })

  it('adds a tenant to an apartment and reloads the property afterwards', async () => {
    const store = useTenantsStore()

    const succeeded = await store.addTenant('property-1', 'apartment-1', tenant)

    expect(succeeded).toBe(true)
    expect(apartmentService.addTenant).toHaveBeenCalledWith('apartment-1', tenant)
    expect(apartmentService.fetchApartments).toHaveBeenCalledWith('property-1')
  })

  it('renames a tenant', async () => {
    const store = useTenantsStore()

    const succeeded = await store.updateTenant('property-1', 'tenant-1', tenant)

    expect(succeeded).toBe(true)
    expect(apartmentService.updateTenant).toHaveBeenCalledWith('tenant-1', tenant)
  })

  it('reloads an already loaded rent payment table after a tenant change, since it can alter what the table shows', async () => {
    const store = useTenantsStore()
    const paymentsStore = usePaymentsStore()
    paymentsStore.overviewsByProperty = { 'property-1': { year: 2026, trackingStart: '2026-01', tenants: [], formerTenantPayments: [] } }
    const fetchYear = vi.spyOn(paymentsStore, 'fetchYear').mockResolvedValue()
    const fetchAlerts = vi.spyOn(paymentsStore, 'fetchAlerts').mockResolvedValue()

    await store.updateTenant('property-1', 'tenant-1', tenant)

    expect(fetchYear).toHaveBeenCalledWith('property-1')
    expect(fetchAlerts).toHaveBeenCalled()
  })

  it('reloads an already loaded rent payment table after an apartment was changed or deleted', async () => {
    const store = useTenantsStore()
    const paymentsStore = usePaymentsStore()
    paymentsStore.overviewsByProperty = { 'property-1': { year: 2026, trackingStart: '2026-01', tenants: [], formerTenantPayments: [] } }
    const fetchYear = vi.spyOn(paymentsStore, 'fetchYear').mockResolvedValue()
    const fetchAlerts = vi.spyOn(paymentsStore, 'fetchAlerts').mockResolvedValue()

    await store.updateApartment('property-1', 'apartment-1', updatePayload)
    await store.deleteApartment('property-1', 'apartment-1')

    expect(fetchYear).toHaveBeenCalledTimes(2)
    expect(fetchAlerts).toHaveBeenCalledTimes(2)
  })

  it('does not load a rent payment table that was never opened after a tenant update', async () => {
    const store = useTenantsStore()
    const fetchYear = vi.spyOn(usePaymentsStore(), 'fetchYear').mockResolvedValue()

    await store.updateTenant('property-1', 'tenant-1', tenant)

    expect(fetchYear).not.toHaveBeenCalled()
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

  it('opens the apartment form for a new apartment', () => {
    const store = useTenantsStore()

    store.openCreateApartmentDialog('property-1')

    expect(store.apartmentFormTarget).toEqual({ mode: 'create', propertyId: 'property-1' })
    expect(store.isApartmentFormDialogOpen).toBe(true)
    expect(store.isTenantFormDialogOpen).toBe(false)
  })

  it('opens the apartment form for editing an apartment', () => {
    const store = useTenantsStore()
    const apartment = createApartment()

    store.openEditApartmentDialog(apartment)

    expect(store.apartmentFormTarget).toEqual({ mode: 'edit', apartment })
  })

  it('opens the tenant form for a further tenant of an apartment', () => {
    const store = useTenantsStore()
    const apartment = createApartment()

    store.openAddTenantDialog(apartment)

    expect(store.tenantFormTarget).toEqual({ mode: 'add', apartment })
    expect(store.isTenantFormDialogOpen).toBe(true)
    expect(store.isApartmentFormDialogOpen).toBe(false)
  })

  it('opens the tenant form for renaming a tenant', () => {
    const store = useTenantsStore()
    const apartment = createApartment()
    const existingTenant = apartment.tenants[0]!

    store.openEditTenantDialog(apartment, existingTenant)

    expect(store.tenantFormTarget).toEqual({ mode: 'edit', apartment, tenant: existingTenant })
  })

  it('clears the save error when either form is opened', async () => {
    vi.mocked(apartmentService.createApartment).mockRejectedValue(new Error('409'))
    vi.mocked(apartmentService.addTenant).mockRejectedValue(new Error('409'))
    const store = useTenantsStore()

    await store.createApartment('property-1', payload)
    expect(store.hasSaveError).toBe(true)
    store.openCreateApartmentDialog('property-1')
    expect(store.hasSaveError).toBe(false)

    await store.addTenant('property-1', 'apartment-1', tenant)
    expect(store.hasSaveError).toBe(true)
    store.openAddTenantDialog(createApartment())
    expect(store.hasSaveError).toBe(false)
  })

  it('closes the forms when their visibility is set to false', () => {
    const store = useTenantsStore()
    store.openCreateApartmentDialog('property-1')
    store.openAddTenantDialog(createApartment())

    store.isApartmentFormDialogOpen = false
    store.isTenantFormDialogOpen = false

    expect(store.apartmentFormTarget).toBeNull()
    expect(store.tenantFormTarget).toBeNull()
  })

  it('refreshes the remaining tenant creations of a demo account only after a successful creation of a tenant', async () => {
    const refreshDemoQuota = vi.spyOn(useAuthStore(), 'refreshDemoQuota').mockResolvedValue()
    const store = useTenantsStore()
    vi.mocked(apartmentService.createApartment).mockRejectedValueOnce(new Error('403'))
    await store.createApartment('property-1', payload)
    expect(refreshDemoQuota).not.toHaveBeenCalled()

    await store.createApartment('property-1', payload)
    await store.addTenant('property-1', 'apartment-1', tenant)
    expect(refreshDemoQuota).toHaveBeenCalledTimes(2)

    await store.createApartment('property-1', { apartment: payload.apartment })
    await store.updateApartment('property-1', 'apartment-1', updatePayload)
    await store.deleteApartment('property-1', 'apartment-1')
    await store.updateTenant('property-1', 'tenant-1', tenant)
    await store.deleteTenant('property-1', 'tenant-1')
    expect(refreshDemoQuota).toHaveBeenCalledTimes(2)
  })

  it('keeps the localized message of a failed save and clears it with the next successful one', async () => {
    const limitError = new AxiosError('Request failed with status code 409')
    limitError.response = { status: 409, data: { message: 'An apartment can have at most 10 tenants.' } } as AxiosResponse
    vi.mocked(apartmentService.addTenant).mockRejectedValueOnce(limitError)
    const store = useTenantsStore()

    await store.addTenant('property-1', 'apartment-1', tenant)
    expect(store.lastChangeErrorMessage).toBe('An apartment can have at most 10 tenants.')

    await store.addTenant('property-1', 'apartment-1', tenant)
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
    store.openCreateApartmentDialog('property-1')
    store.openAddTenantDialog(createApartment())
    vi.mocked(apartmentService.fetchApartments).mockRejectedValue(new Error('x'))
    await store.fetchApartments('property-1')

    store.reset()

    expect(store.apartmentsByProperty).toEqual({})
    expect(store.hasLoadError).toBe(false)
    expect(store.apartmentFormTarget).toBeNull()
    expect(store.tenantFormTarget).toBeNull()
  })
})
