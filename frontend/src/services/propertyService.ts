import axios from 'axios'
import type { Property } from '@/types/property'

/** Shape of a property as returned by the backend. */
export type PropertyResponseDto = Property

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/** Fetches every property, sorted by name. */
export async function fetchProperties(): Promise<Property[]> {
  const { data } = await axios.get<PropertyResponseDto[]>(`${apiBaseUrl}/api/properties`)
  return data
}
