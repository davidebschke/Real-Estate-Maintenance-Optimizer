import axios from 'axios'
import type { CreatePropertyPayload, Property } from '@/types/property'

/** Shape of a property as returned by the backend. */
export type PropertyResponseDto = Property

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

/** Fetches every property, sorted by name. */
export async function fetchProperties(): Promise<Property[]> {
  const { data } = await axios.get<PropertyResponseDto[]>(`${apiBaseUrl}/api/properties`)
  return data
}

/** Creates a new property. */
export async function createProperty(payload: CreatePropertyPayload): Promise<Property> {
  const { data } = await axios.post<PropertyResponseDto>(`${apiBaseUrl}/api/properties`, payload)
  return data
}
