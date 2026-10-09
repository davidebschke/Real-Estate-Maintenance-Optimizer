import { afterEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import {
  addTenant,
  createApartment,
  deleteTenant,
  fetchApartments,
  updateTenant,
} from '@/services/apartmentService'
import type { Apartment, ApartmentWithTenantPayload } from '@/types/apartment'

vi.mock('axios')

afterEach(() => {
  vi.mocked(axios.get).mockReset()
  vi.mocked(axios.post).mockReset()
  vi.mocked(axios.put).mockReset()
  vi.mocked(axios.delete).mockReset()
})

/** Builds a sample apartment with one tenant, with overridable fields. */
function createApartmentFixture(overrides: Partial<Apartment> = {}): Apartment {
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

describe('apartmentService', () => {
  it('fetches the apartments of a property', async () => {
    vi.mocked(axios.get).mockResolvedValue({ data: [createApartmentFixture()] })

    const apartments = await fetchApartments('property-1')

    expect(axios.get).toHaveBeenCalledWith('/api/properties/property-1/apartments')
    expect(apartments).toEqual([createApartmentFixture()])
  })

  it('creates an apartment with its first tenant', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: createApartmentFixture() })

    const created = await createApartment('property-1', payload)

    expect(axios.post).toHaveBeenCalledWith('/api/properties/property-1/apartments', payload)
    expect(created).toEqual(createApartmentFixture())
  })

  it('adds a further tenant to an apartment', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: createApartmentFixture() })

    await addTenant('apartment-1', payload.tenant)

    expect(axios.post).toHaveBeenCalledWith('/api/apartments/apartment-1/tenants', payload.tenant)
  })

  it('updates a tenant together with their apartment', async () => {
    vi.mocked(axios.put).mockResolvedValue({ data: createApartmentFixture() })

    const updated = await updateTenant('tenant-1', payload)

    expect(axios.put).toHaveBeenCalledWith('/api/tenants/tenant-1', payload)
    expect(updated).toEqual(createApartmentFixture())
  })

  it('deletes a tenant', async () => {
    vi.mocked(axios.delete).mockResolvedValue({})

    await deleteTenant('tenant-1')

    expect(axios.delete).toHaveBeenCalledWith('/api/tenants/tenant-1')
  })

  it('lets a failing request propagate to the caller', async () => {
    vi.mocked(axios.get).mockRejectedValue(new Error('Network Error'))

    await expect(fetchApartments('property-1')).rejects.toThrow('Network Error')
  })
})
