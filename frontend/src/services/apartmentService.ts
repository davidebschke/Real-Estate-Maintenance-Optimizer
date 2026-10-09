import axios from 'axios'
import type { Apartment, ApartmentWithTenantPayload, TenantDetails } from '@/types/apartment'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

/** Fetches every apartment of the given property together with its tenants. */
export async function fetchApartments(propertyId: string): Promise<Apartment[]> {
  const { data } = await axios.get<Apartment[]>(`${apiBaseUrl}/api/properties/${propertyId}/apartments`)
  return data
}

/** Creates a new apartment in the given property together with its first tenant. */
export async function createApartment(propertyId: string, payload: ApartmentWithTenantPayload): Promise<Apartment> {
  const { data } = await axios.post<Apartment>(`${apiBaseUrl}/api/properties/${propertyId}/apartments`, payload)
  return data
}

/** Adds a further tenant to the given apartment. */
export async function addTenant(apartmentId: string, payload: TenantDetails): Promise<Apartment> {
  const { data } = await axios.post<Apartment>(`${apiBaseUrl}/api/apartments/${apartmentId}/tenants`, payload)
  return data
}

/** Updates the given tenant together with the data of their apartment. */
export async function updateTenant(tenantId: string, payload: ApartmentWithTenantPayload): Promise<Apartment> {
  const { data } = await axios.put<Apartment>(`${apiBaseUrl}/api/tenants/${tenantId}`, payload)
  return data
}

/** Deletes the given tenant, together with their apartment if they were its last tenant. */
export async function deleteTenant(tenantId: string): Promise<void> {
  await axios.delete(`${apiBaseUrl}/api/tenants/${tenantId}`)
}
