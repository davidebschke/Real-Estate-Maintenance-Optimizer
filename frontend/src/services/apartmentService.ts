import axios from 'axios'
import type { Apartment, ApartmentUpdatePayload, NewApartmentPayload, TenantDetails } from '@/types/apartment'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

/** Fetches every apartment of the given property together with its tenants. */
export async function fetchApartments(propertyId: string): Promise<Apartment[]> {
  const { data } = await axios.get<Apartment[]>(`${apiBaseUrl}/api/properties/${propertyId}/apartments`)
  return data
}

/** Creates a new apartment in the given property, optionally together with its first tenant. */
export async function createApartment(propertyId: string, payload: NewApartmentPayload): Promise<Apartment> {
  const { data } = await axios.post<Apartment>(`${apiBaseUrl}/api/properties/${propertyId}/apartments`, payload)
  return data
}

/** Updates the data of the given apartment, applying a changed total rent from the payload's month. */
export async function updateApartment(apartmentId: string, payload: ApartmentUpdatePayload): Promise<Apartment> {
  const { data } = await axios.put<Apartment>(`${apiBaseUrl}/api/apartments/${apartmentId}`, payload)
  return data
}

/** Deletes the given apartment together with its tenants, rent payments and rent history. */
export async function deleteApartment(apartmentId: string): Promise<void> {
  await axios.delete(`${apiBaseUrl}/api/apartments/${apartmentId}`)
}

/** Adds a further tenant to the given apartment. */
export async function addTenant(apartmentId: string, payload: TenantDetails): Promise<Apartment> {
  const { data } = await axios.post<Apartment>(`${apiBaseUrl}/api/apartments/${apartmentId}/tenants`, payload)
  return data
}

/** Renames the given tenant. */
export async function updateTenant(tenantId: string, payload: TenantDetails): Promise<Apartment> {
  const { data } = await axios.put<Apartment>(`${apiBaseUrl}/api/tenants/${tenantId}`, payload)
  return data
}

/** Deletes the given tenant, whose apartment stays vacant if they were its last tenant. */
export async function deleteTenant(tenantId: string): Promise<void> {
  await axios.delete(`${apiBaseUrl}/api/tenants/${tenantId}`)
}
