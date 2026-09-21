import axios from 'axios'

/** A resolved geographic position (WGS 84). */
export interface GeocodedPosition {
  lat: number
  lng: number
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

const cache = new Map<string, GeocodedPosition | null>()

/** Resolves a postal address to geographic coordinates via the backend, caching results per address for the session. */
export async function geocodeAddress(address: string): Promise<GeocodedPosition | null> {
  if (cache.has(address)) {
    return cache.get(address) ?? null
  }

  let position: GeocodedPosition | null = null
  try {
    const { data } = await axios.get<{ latitude: number | null; longitude: number | null }>(
      `${apiBaseUrl}/api/geocode`,
      { params: { address } },
    )
    position =
      data.latitude !== null && data.longitude !== null
        ? { lat: data.latitude, lng: data.longitude }
        : null
  } catch {
    position = null
  }

  cache.set(address, position)
  return position
}
